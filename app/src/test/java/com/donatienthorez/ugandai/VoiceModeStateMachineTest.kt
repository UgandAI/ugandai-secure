package com.donatienthorez.ugandai

import com.donatienthorez.ugandai.chat.voice.FinalTranscriptValidator
import com.donatienthorez.ugandai.chat.voice.VoiceModeEvent
import com.donatienthorez.ugandai.chat.voice.VoiceModePhase
import com.donatienthorez.ugandai.chat.voice.VoiceModeStateMachine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VoiceModeStateMachineTest {
    @Test
    fun `successful turn follows explicit phases and loops to listening`() {
        val machine = VoiceModeStateMachine()
        machine.dispatch(VoiceModeEvent.Enter(1, 42))
        assertEquals(VoiceModePhase.Initializing, machine.state.phase)
        machine.dispatch(VoiceModeEvent.Ready)
        machine.dispatch(VoiceModeEvent.SpeechStarted)
        assertEquals(VoiceModePhase.UserSpeaking, machine.state.phase)
        machine.dispatch(VoiceModeEvent.SpeechEnded)
        assertEquals(VoiceModePhase.Transcribing, machine.state.phase)
        machine.dispatch(VoiceModeEvent.FinalTranscript("What should I plant?"))
        assertEquals(VoiceModePhase.Thinking, machine.state.phase)
        machine.dispatch(VoiceModeEvent.AssistantResponse("Plant maize after the rains begin."))
        assertEquals(VoiceModePhase.AssistantSpeaking, machine.state.phase)
        machine.dispatch(VoiceModeEvent.SpeechPlaybackFinished)
        assertEquals(VoiceModePhase.Listening, machine.state.phase)
    }

    @Test
    fun `partial transcript never advances to thinking`() {
        val machine = listeningMachine()
        machine.dispatch(VoiceModeEvent.PartialTranscript("My farm is five acres"))
        assertEquals(VoiceModePhase.Listening, machine.state.phase)
        assertEquals("My farm is five acres", machine.state.partialTranscript)
    }

    @Test
    fun `interrupt stops speaking state and returns to listening`() {
        val machine = listeningMachine()
        machine.dispatch(VoiceModeEvent.AssistantResponse("A long answer"))
        machine.dispatch(VoiceModeEvent.Interrupt)
        assertEquals(VoiceModePhase.Listening, machine.state.phase)
        assertEquals("", machine.state.assistantText)
    }

    @Test
    fun `recognition and backend failures are recoverable`() {
        val machine = listeningMachine()
        machine.dispatch(VoiceModeEvent.Fail("Network unavailable"))
        assertEquals(VoiceModePhase.Error, machine.state.phase)
        machine.dispatch(VoiceModeEvent.Retry)
        assertEquals(VoiceModePhase.Listening, machine.state.phase)
        assertNull(machine.state.errorMessage)
    }

    @Test
    fun `exit during speaking clears active mode`() {
        val machine = listeningMachine()
        machine.dispatch(VoiceModeEvent.AssistantResponse("Answer"))
        machine.dispatch(VoiceModeEvent.Exit)
        assertEquals(VoiceModePhase.Idle, machine.state.phase)
    }

    @Test
    fun `entering another conversation creates a new scoped session`() {
        val machine = listeningMachine()
        machine.dispatch(VoiceModeEvent.Enter(2, 99))
        assertEquals(VoiceModePhase.Initializing, machine.state.phase)
        assertEquals(2, machine.state.sessionId)
        assertEquals(99, machine.state.conversationId)
    }

    @Test
    fun `empty punctuation and noise fragments are rejected but short answers remain valid`() {
        listOf("", "   ", ".", "...", "?!").forEach { assertNull(FinalTranscriptValidator.normalize(it)) }
        assertEquals("yes", FinalTranscriptValidator.normalize(" yes "))
        assertEquals("no", FinalTranscriptValidator.normalize("no"))
        assertEquals("maize", FinalTranscriptValidator.normalize("maize"))
        assertEquals("why?", FinalTranscriptValidator.normalize("why?"))
    }

    @Test
    fun `duplicate final event does not leave thinking phase`() {
        val machine = listeningMachine()
        machine.dispatch(VoiceModeEvent.FinalTranscript("Plant maize"))
        machine.dispatch(VoiceModeEvent.FinalTranscript("Plant maize"))
        assertEquals(VoiceModePhase.Thinking, machine.state.phase)
    }

    private fun listeningMachine() = VoiceModeStateMachine().apply {
        dispatch(VoiceModeEvent.Enter(1, 42))
        dispatch(VoiceModeEvent.Ready)
    }
}
