package com.ugandai.ugandai.chat.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.asLiveData
import com.ugandai.ugandai.chat.data.ConversationRepository
import com.ugandai.ugandai.chat.data.Conversation
import com.ugandai.ugandai.chat.data.Message
import com.ugandai.ugandai.chat.data.MessageStatus
import com.donatienthorez.ugandai.chat.domain.usecase.ObserveMessagesUseCase
import com.ugandai.ugandai.chat.domain.usecase.ResendMessageUseCase
import com.ugandai.ugandai.chat.domain.usecase.SendChatRequestUseCase
import com.ugandai.ugandai.chat.domain.usecase.VoiceChatUseCase
import com.donatienthorez.ugandai.chat.voice.AudioTurnCaptureListener
import com.donatienthorez.ugandai.chat.voice.AudioTurnCaptureService
import com.donatienthorez.ugandai.chat.voice.VoiceModeEvent
import com.donatienthorez.ugandai.chat.voice.VoiceModePhase
import com.donatienthorez.ugandai.chat.voice.VoiceModeState
import com.donatienthorez.ugandai.chat.voice.VoiceModeStateMachine
import com.donatienthorez.ugandai.chat.voice.VoiceSpeechListener
import com.donatienthorez.ugandai.chat.voice.VoiceSpeechService
import com.donatienthorez.ugandai.chat.voice.VoiceLatencyTrace
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import android.util.Log

class ChatViewModel(
    private val sendChatRequestUseCase: SendChatRequestUseCase,
    private val resendChatRequestUseCase: ResendMessageUseCase,
    private val observeMessagesUseCase: ObserveMessagesUseCase,
    private val voiceChatUseCase: VoiceChatUseCase,
    private val conversationRepository: ConversationRepository,
    private val audioTurnCaptureService: AudioTurnCaptureService,
    private val voiceSpeechService: VoiceSpeechService,
) : ViewModel() {

    private val _conversation = MutableLiveData<Conversation>()
    val conversation: LiveData<Conversation> = _conversation

    private val _isSendingMessage = MutableLiveData<Boolean>()
    val isSendingMessage: LiveData<Boolean> = _isSendingMessage
    val conversationSummaries = conversationRepository.conversationSummaries.asLiveData()
    val selectedConversationId = conversationRepository.selectedConversationId.asLiveData()
    private val voiceMachine = VoiceModeStateMachine()
    private val _voiceModeState = MutableStateFlow(voiceMachine.state)
    val voiceModeState = _voiceModeState.asStateFlow()
    private var voiceTurnJob: Job? = null
    private var nextVoiceSessionId = 1L
    private var activeCaptureSessionId = 0L

    init {
        observeMessageList()
    }

    private fun observeMessageList() {
        viewModelScope.launch {
            observeMessagesUseCase.invoke().collect { conversation ->
                _conversation.postValue(conversation)

                _isSendingMessage.postValue(
                    conversation.list.any { it.messageStatus == MessageStatus.Sending }
                )
            }
        }
    }

    // ✅ CLEAN VERSION
    fun sendMessage(prompt: String) {
        viewModelScope.launch {
            sendChatRequestUseCase.invoke(prompt)
        }
    }

    fun resendMessage(message: Message) {
        viewModelScope.launch {
            resendChatRequestUseCase.invoke(message)
        }
    }

    fun sendVoiceMessage(audioFile: File) {
        viewModelScope.launch {
            voiceChatUseCase.invoke(audioFile)
        }
    }

    fun newConversation() {
        exitVoiceMode()
        viewModelScope.launch { conversationRepository.newConversation() }
    }

    fun selectConversation(id: Int) {
        exitVoiceMode()
        viewModelScope.launch { conversationRepository.selectConversation(id) }
    }

    fun enterVoiceMode() {
        if (_voiceModeState.value.phase != VoiceModePhase.Idle) return
        viewModelScope.launch {
            val conversationId = try {
                conversationRepository.selectedConversationOrCreate()
            } catch (error: Exception) {
                updateVoice(VoiceModeEvent.Enter(nextVoiceSessionId++, null))
                updateVoice(VoiceModeEvent.Fail("Unable to open this conversation"))
                return@launch
            }
            val sessionId = nextVoiceSessionId++
            updateVoice(VoiceModeEvent.Enter(sessionId, conversationId))
            voiceSpeechService.initialize(
                onReady = { viewModelScope.launch { startListening(sessionId) } },
                onError = { message -> viewModelScope.launch { updateVoice(VoiceModeEvent.Fail(message)) } }
            )
        }
    }

    fun interruptAssistant() {
        if (_voiceModeState.value.phase != VoiceModePhase.AssistantSpeaking) return
        voiceSpeechService.stop()
        updateVoice(VoiceModeEvent.Interrupt)
        startListening(_voiceModeState.value.sessionId)
    }

    fun finishVoiceTurn() {
        if (_voiceModeState.value.phase in setOf(VoiceModePhase.Listening, VoiceModePhase.UserSpeaking)) {
            audioTurnCaptureService.finishTurn()
        }
    }

    fun retryVoiceMode() {
        if (_voiceModeState.value.phase != VoiceModePhase.Error) return
        updateVoice(VoiceModeEvent.Retry)
        startListening(_voiceModeState.value.sessionId)
    }

    fun exitVoiceMode() {
        if (_voiceModeState.value.phase == VoiceModePhase.Idle) return
        voiceTurnJob?.cancel()
        audioTurnCaptureService.stop()
        voiceSpeechService.stop()
        updateVoice(VoiceModeEvent.Exit)
    }

    private fun startListening(sessionId: Long) {
        if (!isActiveVoiceSession(sessionId)) return
        audioTurnCaptureService.start(object : AudioTurnCaptureListener {
            override fun onReady(captureSessionId: Long) {
                activeCaptureSessionId = captureSessionId
                Log.i(TAG, "VOICE session=$captureSessionId viewModel=ready voiceModeSession=$sessionId")
                if (isActiveVoiceSession(sessionId)) updateVoice(VoiceModeEvent.Ready)
            }

            override fun onSpeechStarted(captureSessionId: Long) {
                if (captureSessionId != activeCaptureSessionId) return
                Log.i(TAG, "VOICE session=$captureSessionId viewModel=beginningSpeech")
                if (isActiveVoiceSession(sessionId)) updateVoice(VoiceModeEvent.SpeechStarted)
            }

            override fun onTurnComplete(file: File, captureSessionId: Long) {
                if (captureSessionId != activeCaptureSessionId) {
                    Log.w(TAG, "VOICE session=$captureSessionId stale=onTurnComplete active=$activeCaptureSessionId")
                    file.delete()
                    return
                }
                viewModelScope.launch { submitRecordedTurn(sessionId, captureSessionId, file) }
            }

            override fun onNoSpeech(captureSessionId: Long) {
                if (captureSessionId != activeCaptureSessionId) return
                if (isActiveVoiceSession(sessionId)) {
                    updateVoice(VoiceModeEvent.Fail("I didn't hear any speech. Check the microphone and try again."))
                }
            }

            override fun onError(captureSessionId: Long, message: String) {
                if (captureSessionId != activeCaptureSessionId) return
                if (isActiveVoiceSession(sessionId)) updateVoice(VoiceModeEvent.Fail(message))
            }
        })
    }

    private fun submitRecordedTurn(sessionId: Long, captureSessionId: Long, audioFile: File) {
        if (captureSessionId != activeCaptureSessionId) {
            Log.w(TAG, "VOICE session=$captureSessionId stale=submit active=$activeCaptureSessionId")
            audioFile.delete()
            return
        }
        if (!isActiveVoiceSession(sessionId)) {
            audioFile.delete()
            return
        }
        val phase = _voiceModeState.value.phase
        if (phase !in setOf(VoiceModePhase.Listening, VoiceModePhase.UserSpeaking)) {
            audioFile.delete()
            return
        }
        audioTurnCaptureService.stop()
        updateVoice(VoiceModeEvent.SpeechEnded)
        Log.i(TAG, "VOICE session=$captureSessionId viewModel=transcribing file=${audioFile.name}")
        voiceTurnJob = viewModelScope.launch {
            voiceChatUseCase(audioFile, captureSessionId, playReply = false).fold(
                onSuccess = { result ->
                    Log.i(TAG, "VOICE session=$captureSessionId viewModelFinal=${result.transcript.take(300)}")
                    if (captureSessionId != activeCaptureSessionId || !isActiveVoiceSession(sessionId)) {
                        Log.w(TAG, "VOICE session=$captureSessionId stale=final active=$activeCaptureSessionId")
                        return@fold
                    }
                    updateVoice(VoiceModeEvent.FinalTranscript(result.transcript))
                    if (result.content.isBlank()) {
                        updateVoice(VoiceModeEvent.Fail("UgandAI returned an empty response"))
                        return@fold
                    }
                    updateVoice(VoiceModeEvent.AssistantResponse(result.content))
                    VoiceLatencyTrace.mark(captureSessionId, "tts_queued")
                    voiceSpeechService.speak(result.content, object : VoiceSpeechListener {
                        override fun onStarted() {
                            VoiceLatencyTrace.mark(captureSessionId, "playback_started")
                            VoiceLatencyTrace.report(captureSessionId)
                        }
                        override fun onFinished() {
                            viewModelScope.launch {
                                if (isActiveVoiceSession(sessionId)) {
                                    updateVoice(VoiceModeEvent.SpeechPlaybackFinished)
                                    startListening(sessionId)
                                }
                            }
                        }

                        override fun onError(message: String) {
                            viewModelScope.launch {
                                if (isActiveVoiceSession(sessionId)) updateVoice(VoiceModeEvent.Fail(message))
                            }
                        }
                    })
                },
                onFailure = { error ->
                    if (isActiveVoiceSession(sessionId)) {
                        updateVoice(VoiceModeEvent.Fail(error.message ?: "Voice transcription failed"))
                    }
                }
            )
        }
    }

    private fun isActiveVoiceSession(sessionId: Long): Boolean =
        _voiceModeState.value.phase != VoiceModePhase.Idle && _voiceModeState.value.sessionId == sessionId

    private fun updateVoice(event: VoiceModeEvent) {
        _voiceModeState.value = voiceMachine.dispatch(event)
    }

    override fun onCleared() {
        audioTurnCaptureService.destroy()
        voiceSpeechService.destroy()
        super.onCleared()
    }

    companion object { private const val TAG = "UgandAIVoiceTrace" }
}
