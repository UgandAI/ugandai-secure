package com.donatienthorez.ugandai.chat.voice

import android.os.SystemClock
import android.util.Log
import java.util.concurrent.ConcurrentHashMap

data class ServerVoiceTimings(
    val backendReceivedToSttStartMs: Long = 0,
    val sttMs: Long = 0,
    val llmFirstTokenMs: Long? = null,
    val llmCompletionMs: Long = 0,
    val ttsFirstAudioMs: Long? = null,
    val ttsCompletionMs: Long = 0,
    val backendTotalMs: Long = 0
)

/** Correlates one voice turn using elapsed realtime, which is immune to wall-clock changes. */
object VoiceLatencyTrace {
    private const val TAG = "UgandAIVoiceTiming"
    private val events = ConcurrentHashMap<Long, ConcurrentHashMap<String, Long>>()
    private val server = ConcurrentHashMap<Long, ServerVoiceTimings>()

    fun mark(sessionId: Long, event: String) {
        val now = SystemClock.elapsedRealtime()
        events.getOrPut(sessionId) { ConcurrentHashMap() }[event] = now
        Log.i(TAG, "VOICE_TIMING session=$sessionId event=$event elapsedMs=$now")
    }

    fun attachServer(sessionId: Long, timings: ServerVoiceTimings) {
        server[sessionId] = timings
        Log.i(TAG, "VOICE_TIMING session=$sessionId server=$timings")
    }

    fun report(sessionId: Long) {
        val values = events[sessionId] ?: return
        fun delta(start: String, end: String): Long? =
            values[start]?.let { startAt -> values[end]?.minus(startAt) }
        val backend = server[sessionId]
        val lines = listOfNotNull(
            delta("speech_end", "wav_ready")?.let { "speech_end -> wav_ready: $it ms" },
            delta("wav_ready", "http_upload_started")?.let { "wav_ready -> upload_started: $it ms" },
            delta("http_upload_started", "http_response_received")?.let { "HTTP round trip: $it ms" },
            backend?.let { "backend_received -> STT request: ${it.backendReceivedToSttStartMs} ms" },
            backend?.let { "STT: ${it.sttMs} ms" },
            backend?.llmFirstTokenMs?.let { "LLM first token: $it ms" }
                ?: "LLM first token: unavailable (non-streaming)",
            backend?.let { "LLM completion: ${it.llmCompletionMs} ms" },
            backend?.ttsFirstAudioMs?.let { "Backend TTS first audio: $it ms" }
                ?: "Backend TTS: skipped (Voice Mode uses Android TTS)",
            backend?.let { "TTS completion: ${it.ttsCompletionMs} ms" },
            delta("http_response_received", "tts_queued")?.let { "response_received -> local_TTS_queued: $it ms" },
            delta("tts_queued", "playback_started")?.let { "local_TTS_queued -> playback_started: $it ms" },
            delta("speech_end", "playback_started")?.let { "TOTAL speech_end -> first spoken assistant audio: $it ms" }
        )
        Log.i(TAG, "VOICE_TIMING_REPORT session=$sessionId\n${lines.joinToString("\n")}")
    }
}
