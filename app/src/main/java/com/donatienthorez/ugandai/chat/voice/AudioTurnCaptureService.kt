package com.donatienthorez.ugandai.chat.voice

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.donatienthorez.ugandai.chat.data.audio.VoiceRecorder
import java.io.File
import java.util.concurrent.atomic.AtomicLong

interface AudioTurnCaptureListener {
    fun onReady(captureSessionId: Long) = Unit
    fun onSpeechStarted(captureSessionId: Long) = Unit
    fun onTurnComplete(file: File, captureSessionId: Long) = Unit
    fun onNoSpeech(captureSessionId: Long) = Unit
    fun onError(captureSessionId: Long, message: String) = Unit
}

interface AudioTurnCaptureService {
    fun start(listener: AudioTurnCaptureListener)
    fun finishTurn()
    fun stop()
    fun destroy()
}

/** Records one natural utterance and ends it after sustained silence.
 *
 * AudioRecord captures auditable PCM instead of using SpeechRecognizer. Transcription remains
 * server-side and explicitly English, and every capture has isolated session ownership. */
class AndroidAudioTurnCaptureService(context: Context) : AudioTurnCaptureService {
    private val recorder = VoiceRecorder(context.applicationContext)
    private val handler = Handler(Looper.getMainLooper())
    private var generation = 0L
    private var activeListener: AudioTurnCaptureListener? = null
    private var activeCaptureSessionId = 0L

    override fun start(listener: AudioTurnCaptureListener) {
        stop()
        val activeGeneration = ++generation
        val captureSessionId = nextCaptureSessionId.incrementAndGet()
        activeCaptureSessionId = captureSessionId
        activeListener = listener
        try {
            recorder.start(captureSessionId)
        } catch (error: Exception) {
            Log.e(TAG, "Microphone initialization failed", error)
            listener.onError(captureSessionId, "The microphone could not be started: ${error.message ?: "unknown error"}")
            return
        }
        Log.i(TAG, "VOICE session=$captureSessionId ready")
        listener.onReady(captureSessionId)
        val startedAt = System.currentTimeMillis()
        var speechStartedAt: Long? = null
        var lastLoudAt: Long? = null
        var consecutiveLoudSamples = 0

        fun poll() {
            if (activeGeneration != generation || !recorder.isRecording()) return
            val now = System.currentTimeMillis()
            val level = recorder.audioLevel()
            if (level.rms >= SPEECH_RMS) {
                consecutiveLoudSamples += 1
                lastLoudAt = now
                if (speechStartedAt == null && consecutiveLoudSamples >= REQUIRED_LOUD_SAMPLES) {
                    speechStartedAt = now
                    Log.i(TAG, "VOICE session=$captureSessionId beginningSpeech rms=${level.rms} peak=${level.peak}")
                    listener.onSpeechStarted(captureSessionId)
                }
            } else {
                consecutiveLoudSamples = 0
            }

            val speechStart = speechStartedAt
            val shouldFinish = speechStart != null &&
                now - speechStart >= MINIMUM_SPEECH_MILLIS &&
                now - (lastLoudAt ?: speechStart) >= END_OF_TURN_SILENCE_MILLIS
            val reachedMaximum = now - startedAt >= MAXIMUM_TURN_MILLIS
            val noSpeechTimeout = speechStart == null && now - startedAt >= NO_SPEECH_TIMEOUT_MILLIS

            when {
                shouldFinish || (reachedMaximum && speechStart != null) -> {
                    VoiceLatencyTrace.mark(captureSessionId, "speech_end")
                    val file = recorder.stop()
                    VoiceLatencyTrace.mark(captureSessionId, "wav_ready")
                    activeListener = null
                    Log.i(TAG, "VOICE session=$captureSessionId endSpeech reason=${if (shouldFinish) "silence" else "maximum"}")
                    if (file != null && isUsableCapture(file, captureSessionId)) listener.onTurnComplete(file, captureSessionId)
                    else listener.onError(captureSessionId, "No usable audio was recorded")
                }
                noSpeechTimeout -> {
                    val level = recorder.audioLevel()
                    Log.e(TAG, "No speech detected; stopping before transcription. rms=${level.rms} peak=${level.peak} samples=${level.samples}. Check host microphone routing.")
                    recorder.cancel()
                    activeListener = null
                    Log.i(TAG, "VOICE session=$captureSessionId END reason=no_speech")
                    listener.onNoSpeech(captureSessionId)
                }
                else -> handler.postDelayed(::poll, POLL_INTERVAL_MILLIS)
            }
        }
        handler.postDelayed(::poll, POLL_INTERVAL_MILLIS)
    }

    override fun finishTurn() {
        val listener = activeListener ?: return
        val captureSessionId = activeCaptureSessionId
        generation += 1
        handler.removeCallbacksAndMessages(null)
        activeListener = null
        VoiceLatencyTrace.mark(captureSessionId, "speech_end")
        val file = recorder.stop()
        VoiceLatencyTrace.mark(captureSessionId, "wav_ready")
        Log.i(TAG, "VOICE session=$captureSessionId endSpeech reason=manual")
        if (file != null && isUsableCapture(file, captureSessionId)) listener.onTurnComplete(file, captureSessionId)
        else {
            file?.delete()
            Log.i(TAG, "VOICE session=$captureSessionId END reason=no_speech_evidence")
            listener.onError(captureSessionId, "No clear speech was captured. Check the emulator host microphone input.")
        }
    }

    override fun stop() {
        generation += 1
        handler.removeCallbacksAndMessages(null)
        activeListener = null
        recorder.cancel()
    }

    override fun destroy() = stop()

    private fun isUsableCapture(file: File, captureSessionId: Long): Boolean {
        val level = recorder.maximumAudioLevel()
        val usable = file.length() > MINIMUM_FILE_BYTES &&
            level.samples >= MINIMUM_PCM_SAMPLES && level.rms >= SPEECH_RMS
        Log.i(TAG, "VOICE session=$captureSessionId captureGate file=${file.name} bytes=${file.length()} maxRms=${level.rms} maxPeak=${level.peak} samples=${level.samples} usable=$usable")
        if (!usable) file.delete()
        else preserveDiagnosticCopy(file, captureSessionId)
        return usable
    }

    private fun preserveDiagnosticCopy(file: File, captureSessionId: Long) {
        val directory = recorder.diagnosticDirectory().apply { mkdirs() }
        val copy = File(directory, file.name)
        file.copyTo(copy, overwrite = false)
        directory.listFiles()?.sortedByDescending(File::lastModified)?.drop(MAX_DIAGNOSTIC_FILES)?.forEach(File::delete)
        Log.i(TAG, "VOICE session=$captureSessionId diagnosticFile=${copy.absolutePath}")
    }

    companion object {
        // Observed emulator silence: RMS 47-620 (one transient 855); clear speech: RMS 1,647-7,148.
        internal const val SPEECH_RMS = 1_000
        internal const val REQUIRED_LOUD_SAMPLES = 1
        internal const val END_OF_TURN_SILENCE_MILLIS = 1_200L
        internal const val MINIMUM_SPEECH_MILLIS = 500L
        internal const val NO_SPEECH_TIMEOUT_MILLIS = 15_000L
        internal const val MAXIMUM_TURN_MILLIS = 30_000L
        internal const val POLL_INTERVAL_MILLIS = 100L
        internal const val MINIMUM_FILE_BYTES = 1_024L
        internal const val MINIMUM_PCM_SAMPLES = 8_000L
        private const val TAG = "UgandAIVoiceCapture"
        private const val MAX_DIAGNOSTIC_FILES = 10
        private val nextCaptureSessionId = AtomicLong(System.currentTimeMillis())
    }
}
