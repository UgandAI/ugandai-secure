package com.donatienthorez.ugandai.chat.data.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

/** Records a single voice message to an AAC/M4A file for upload to POST /voice/chat. */
class VoiceRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    fun start(): File {
        val file = File(context.cacheDir, "voice_input_${System.currentTimeMillis()}.m4a")
        val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        try {
            mediaRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
        } catch (error: Exception) {
            mediaRecorder.release()
            file.delete()
            throw error
        }
        recorder = mediaRecorder
        outputFile = file
        return file
    }

    /** Stops the active recording and returns the recorded file, or null if nothing was captured. */
    fun stop(): File? {
        val file = outputFile
        return try {
            recorder?.stop()
            file
        } catch (e: Exception) {
            file?.delete()
            null
        } finally {
            recorder?.release()
            recorder = null
            outputFile = null
        }
    }

    fun cancel() {
        try {
            recorder?.stop()
        } catch (e: Exception) {
            // recorder may not have captured enough data to stop cleanly; discard either way
        }
        recorder?.release()
        recorder = null
        outputFile?.delete()
        outputFile = null
    }

    fun isRecording(): Boolean = recorder != null
}
