package com.donatienthorez.ugandai.chat.data.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import java.io.File
import java.io.RandomAccessFile
import java.util.UUID
import kotlin.math.sqrt

data class AudioLevel(val rms: Int = 0, val peak: Int = 0, val samples: Long = 0)

/** Records a fresh 16 kHz mono PCM/WAV file and exposes measured input levels. */
class VoiceRecorder(private val context: Context) {
    @Volatile private var recorder: AudioRecord? = null
    @Volatile private var outputFile: File? = null
    @Volatile private var writerThread: Thread? = null
    @Volatile private var recording = false
    @Volatile private var latestLevel = AudioLevel()
    @Volatile private var maximumLevel = AudioLevel()
    @Volatile private var writerFailure: Throwable? = null

    @SuppressLint("MissingPermission")
    fun start(traceSessionId: Long): File {
        check(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            "RECORD_AUDIO permission is not granted"
        }
        check(!recording) { "Audio capture is already active" }

        val minBuffer = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        check(minBuffer > 0) { "AudioRecord returned invalid minimum buffer size: $minBuffer" }
        val bufferBytes = maxOf(minBuffer * 2, SAMPLE_RATE / 2)
        val audioRecord = AudioRecord.Builder()
            .setAudioSource(AUDIO_SOURCE)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AUDIO_FORMAT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(CHANNEL_CONFIG)
                    .build()
            )
            .setBufferSizeInBytes(bufferBytes)
            .build()
        check(audioRecord.state == AudioRecord.STATE_INITIALIZED) {
            audioRecord.release()
            "AudioRecord failed to initialize"
        }

        val file = File(context.cacheDir, "voice_session_${traceSessionId}_${System.currentTimeMillis()}_${UUID.randomUUID()}.wav")
        RandomAccessFile(file, "rw").use { writeWavHeader(it, 0) }
        latestLevel = AudioLevel()
        maximumLevel = AudioLevel()
        writerFailure = null
        outputFile = file
        recorder = audioRecord
        recording = true
        try {
            audioRecord.startRecording()
            check(audioRecord.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                "AudioRecord did not enter RECORDSTATE_RECORDING"
            }
        } catch (error: Throwable) {
            recording = false
            recorder = null
            outputFile = null
            audioRecord.release()
            file.delete()
            throw error
        }

        Log.i(TAG, "VOICE session=$traceSessionId START file=${file.name} source=MIC sourceId=${audioRecord.audioSource} " +
            "sampleRate=${audioRecord.sampleRate} channel=mono encoding=PCM_16BIT bufferBytes=$bufferBytes " +
            "audioSessionId=${audioRecord.audioSessionId} state=${audioRecord.state} recordingState=${audioRecord.recordingState}")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            audioRecord.routedDevice?.let {
                Log.i(TAG, "VOICE session=$traceSessionId route id=${it.id} type=${it.type} product=${it.productName} address=${it.address}")
            } ?: Log.w(TAG, "VOICE session=$traceSessionId route unavailable")
        }

        writerThread = Thread({ captureLoop(audioRecord, file, bufferBytes, traceSessionId) }, "UgandAI-PcmCapture").apply { start() }
        return file
    }

    fun stop(): File? {
        val file = outputFile
        finishCapture(deleteFile = false)
        val failure = writerFailure
        if (failure != null) {
            Log.e(TAG, "capture_failed file=${file?.name}", failure)
            file?.delete()
            return null
        }
        val dataBytes = ((file?.length() ?: 0L) - WAV_HEADER_BYTES).coerceAtLeast(0L)
        if (file != null && dataBytes > 0) {
            Log.i(TAG, "capture_stopped file=${file.name} bytes=${file.length()} pcmBytes=$dataBytes " +
                "durationMs=${dataBytes * 1000 / BYTES_PER_SECOND} lastRms=${latestLevel.rms} lastPeak=${latestLevel.peak}")
            return file
        }
        file?.delete()
        return null
    }

    fun cancel() = finishCapture(deleteFile = true)

    fun isRecording(): Boolean = recording && recorder?.recordingState == AudioRecord.RECORDSTATE_RECORDING

    fun maxAmplitude(): Int = latestLevel.peak

    fun audioLevel(): AudioLevel = latestLevel

    fun maximumAudioLevel(): AudioLevel = maximumLevel

    fun diagnosticDirectory(): File = context.getExternalFilesDir("voice_diagnostics")
        ?: File(context.filesDir, "voice_diagnostics")

    private fun finishCapture(deleteFile: Boolean) {
        recording = false
        val activeRecorder = recorder
        try {
            if (activeRecorder?.recordingState == AudioRecord.RECORDSTATE_RECORDING) activeRecorder.stop()
        } catch (error: IllegalStateException) {
            Log.w(TAG, "AudioRecord stop failed", error)
        }
        writerThread?.join(2_000)
        if (writerThread?.isAlive == true) Log.e(TAG, "PCM writer did not stop within 2 seconds")
        activeRecorder?.release()
        recorder = null
        writerThread = null
        val file = outputFile
        outputFile = null
        if (deleteFile) file?.delete()
    }

    private fun captureLoop(audioRecord: AudioRecord, file: File, bufferBytes: Int, traceSessionId: Long) {
        val samples = ShortArray(bufferBytes / 2)
        var totalSamples = 0L
        var windowSamples = 0L
        var sumSquares = 0.0
        var peak = 0
        var lastLevelAt = System.currentTimeMillis()
        try {
            RandomAccessFile(file, "rw").use { output ->
                output.seek(WAV_HEADER_BYTES.toLong())
                while (recording) {
                    val count = audioRecord.read(samples, 0, samples.size, AudioRecord.READ_BLOCKING)
                    if (count < 0) error("AudioRecord.read failed with code $count")
                    if (count == 0) continue
                    for (index in 0 until count) {
                        val value = samples[index].toInt()
                        output.write(value and 0xff)
                        output.write((value ushr 8) and 0xff)
                        val absolute = kotlin.math.abs(value)
                        if (absolute > peak) peak = absolute
                        sumSquares += value.toDouble() * value.toDouble()
                    }
                    totalSamples += count
                    windowSamples += count
                    val now = System.currentTimeMillis()
                    if (now - lastLevelAt >= LEVEL_LOG_INTERVAL_MILLIS && windowSamples > 0) {
                        val rms = sqrt(sumSquares / windowSamples).toInt()
                        latestLevel = AudioLevel(rms = rms, peak = peak, samples = totalSamples)
                        maximumLevel = AudioLevel(
                            rms = maxOf(maximumLevel.rms, rms),
                            peak = maxOf(maximumLevel.peak, peak),
                            samples = totalSamples
                        )
                        Log.d(TAG, "VOICE session=$traceSessionId rms=$rms peak=$peak samples=$totalSamples file=${file.name}")
                        windowSamples = 0
                        sumSquares = 0.0
                        peak = 0
                        lastLevelAt = now
                    }
                }
                writeWavHeader(output, totalSamples * 2)
            }
        } catch (error: Throwable) {
            writerFailure = error
        }
    }

    private fun writeWavHeader(output: RandomAccessFile, dataBytes: Long) {
        output.seek(0)
        output.writeBytes("RIFF")
        writeLittleEndianInt(output, (36 + dataBytes).toInt())
        output.writeBytes("WAVEfmt ")
        writeLittleEndianInt(output, 16)
        writeLittleEndianShort(output, 1)
        writeLittleEndianShort(output, 1)
        writeLittleEndianInt(output, SAMPLE_RATE)
        writeLittleEndianInt(output, BYTES_PER_SECOND)
        writeLittleEndianShort(output, 2)
        writeLittleEndianShort(output, 16)
        output.writeBytes("data")
        writeLittleEndianInt(output, dataBytes.toInt())
    }

    private fun writeLittleEndianInt(output: RandomAccessFile, value: Int) {
        output.write(value and 0xff); output.write((value ushr 8) and 0xff)
        output.write((value ushr 16) and 0xff); output.write((value ushr 24) and 0xff)
    }

    private fun writeLittleEndianShort(output: RandomAccessFile, value: Int) {
        output.write(value and 0xff); output.write((value ushr 8) and 0xff)
    }

    companion object {
        private const val TAG = "UgandAIVoiceCapture"
        private const val SAMPLE_RATE = 16_000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val AUDIO_SOURCE = MediaRecorder.AudioSource.MIC
        private const val WAV_HEADER_BYTES = 44
        private const val BYTES_PER_SECOND = SAMPLE_RATE * 2
        private const val LEVEL_LOG_INTERVAL_MILLIS = 250L
    }
}
