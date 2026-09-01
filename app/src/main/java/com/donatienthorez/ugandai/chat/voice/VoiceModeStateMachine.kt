package com.donatienthorez.ugandai.chat.voice

enum class VoiceModePhase {
    Idle,
    Initializing,
    Listening,
    UserSpeaking,
    Transcribing,
    Thinking,
    AssistantSpeaking,
    Error
}

data class VoiceModeState(
    val phase: VoiceModePhase = VoiceModePhase.Idle,
    val partialTranscript: String = "",
    val finalizedTranscript: String = "",
    val assistantText: String = "",
    val errorMessage: String? = null,
    val sessionId: Long = 0L,
    val conversationId: Int? = null
)

sealed interface VoiceModeEvent {
    data class Enter(val sessionId: Long, val conversationId: Int?) : VoiceModeEvent
    data object Ready : VoiceModeEvent
    data object SpeechStarted : VoiceModeEvent
    data class PartialTranscript(val text: String) : VoiceModeEvent
    data object SpeechEnded : VoiceModeEvent
    data class FinalTranscript(val text: String) : VoiceModeEvent
    data class AssistantResponse(val text: String) : VoiceModeEvent
    data object SpeechPlaybackFinished : VoiceModeEvent
    data object Interrupt : VoiceModeEvent
    data class Fail(val message: String) : VoiceModeEvent
    data object Retry : VoiceModeEvent
    data object Exit : VoiceModeEvent
}

/** Pure, deliberately small reducer so voice behavior can be tested without Android audio hardware. */
class VoiceModeStateMachine {
    var state: VoiceModeState = VoiceModeState()
        private set

    fun dispatch(event: VoiceModeEvent): VoiceModeState {
        state = reduce(state, event)
        return state
    }

    companion object {
        fun reduce(current: VoiceModeState, event: VoiceModeEvent): VoiceModeState = when (event) {
            is VoiceModeEvent.Enter -> VoiceModeState(
                phase = VoiceModePhase.Initializing,
                sessionId = event.sessionId,
                conversationId = event.conversationId
            )
            VoiceModeEvent.Ready -> current.copy(
                phase = VoiceModePhase.Listening,
                partialTranscript = "",
                errorMessage = null
            )
            VoiceModeEvent.SpeechStarted -> current.copy(phase = VoiceModePhase.UserSpeaking)
            is VoiceModeEvent.PartialTranscript -> current.copy(partialTranscript = event.text)
            VoiceModeEvent.SpeechEnded -> current.copy(phase = VoiceModePhase.Transcribing)
            is VoiceModeEvent.FinalTranscript -> current.copy(
                phase = VoiceModePhase.Thinking,
                finalizedTranscript = event.text,
                partialTranscript = event.text,
                errorMessage = null
            )
            is VoiceModeEvent.AssistantResponse -> current.copy(
                phase = VoiceModePhase.AssistantSpeaking,
                assistantText = event.text,
                errorMessage = null
            )
            VoiceModeEvent.SpeechPlaybackFinished -> current.copy(
                phase = VoiceModePhase.Listening,
                partialTranscript = "",
                finalizedTranscript = "",
                assistantText = ""
            )
            VoiceModeEvent.Interrupt -> current.copy(
                phase = VoiceModePhase.Listening,
                partialTranscript = "",
                assistantText = ""
            )
            is VoiceModeEvent.Fail -> current.copy(phase = VoiceModePhase.Error, errorMessage = event.message)
            VoiceModeEvent.Retry -> current.copy(
                phase = VoiceModePhase.Listening,
                partialTranscript = "",
                errorMessage = null
            )
            VoiceModeEvent.Exit -> VoiceModeState(phase = VoiceModePhase.Idle, sessionId = current.sessionId)
        }
    }
}

object FinalTranscriptValidator {
    private val punctuationOnly = Regex("^[\\p{P}\\p{S}\\s]+$")

    fun normalize(raw: String): String? {
        val text = raw.trim().replace(Regex("\\s+"), " ")
        if (text.isEmpty() || punctuationOnly.matches(text)) return null
        return text
    }
}
