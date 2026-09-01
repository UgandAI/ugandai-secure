package com.ugandai.ugandai.chat.domain.usecase

import com.donatienthorez.ugandai.chat.data.api.OpenAIRepository
import com.donatienthorez.ugandai.chat.data.audio.VoicePlayer
import com.ugandai.ugandai.chat.data.ConversationRepository
import com.ugandai.ugandai.chat.data.Message
import com.ugandai.ugandai.chat.data.MessageStatus
import java.io.File
import android.util.Log
import java.security.MessageDigest
import com.donatienthorez.ugandai.chat.voice.FinalTranscriptValidator
import com.donatienthorez.ugandai.chat.data.api.VoiceChatResult

/** Uploads a recorded voice message to POST /voice/chat, then renders the transcript,
 * reply text, and citations into the conversation and plays the synthesized reply audio. */
class VoiceChatUseCase(
    private val openAIRepository: OpenAIRepository,
    private val conversationRepository: ConversationRepository,
    private val voicePlayer: VoicePlayer
) {

    suspend operator fun invoke(audioFile: File, captureSessionId: Long = 0, playReply: Boolean = true): Result<VoiceChatResult> {
        val conversationId = conversationRepository.selectedConversationOrCreate()
        return try {
            val audioBytes = audioFile.readBytes()
            val hash = MessageDigest.getInstance("SHA-256").digest(audioBytes).joinToString("") { "%02x".format(it) }
            Log.i(TAG, "VOICE session=$captureSessionId SEND_AUDIO file=${audioFile.name} bytes=${audioBytes.size} sha256=$hash")
            val result = openAIRepository.sendVoiceChat(
                audioBytes, conversationId, audioFile.name, captureSessionId, includeAudio = playReply
            )
            val transcript = FinalTranscriptValidator.normalize(result.transcript)
                ?: throw IllegalArgumentException("No clear speech was detected. The transcript was rejected.")
            Log.i(TAG, "VOICE session=$captureSessionId SEND=${transcript.take(300)} file=${audioFile.name}")
            // Only finalized server transcription becomes a real chat message.
            conversationRepository.addMessage(Message(
                text = transcript,
                isFromUser = true,
                messageStatus = MessageStatus.Sent
            ))
            conversationRepository.addMessage(Message(
                text = result.content,
                isFromUser = false,
                messageStatus = MessageStatus.Sent,
                citations = result.citations
            ))
            conversationRepository.refreshConversations()
            if (playReply) voicePlayer.play(result.audioBytes, result.audioFormat)
            Log.i(TAG, "VOICE session=$captureSessionId END result=success")
            Result.success(result)
        } catch (exception: Exception) {
            Result.failure(exception)
        } finally {
            audioFile.delete()
        }
    }

    companion object {
        private const val TAG = "UgandAIVoiceTrace"
    }
}
