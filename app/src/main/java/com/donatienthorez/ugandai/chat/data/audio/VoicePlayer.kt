package com.donatienthorez.ugandai.chat.data.audio

import android.content.Context
import android.media.MediaPlayer
import java.io.File
import java.io.FileOutputStream

/** Plays back the synthesized MP3 reply audio returned by POST /voice/chat. */
class VoicePlayer(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var playbackFile: File? = null

    fun play(audioBytes: ByteArray, audioFormat: String = "mp3", onCompletion: () -> Unit = {}) {
        stop()
        val file = File(context.cacheDir, "voice_reply_${System.currentTimeMillis()}.$audioFormat")
        require(audioBytes.isNotEmpty()) { "Audio response is empty" }
        try {
            FileOutputStream(file).use { it.write(audioBytes) }
            playbackFile = file
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnCompletionListener {
                    it.release()
                    mediaPlayer = null
                    playbackFile?.delete()
                    playbackFile = null
                    onCompletion()
                }
                setOnErrorListener { player, _, _ ->
                    player.release()
                    mediaPlayer = null
                    playbackFile?.delete()
                    playbackFile = null
                    true
                }
                prepare()
                start()
            }
        } catch (error: Exception) {
            mediaPlayer?.release()
            mediaPlayer = null
            playbackFile?.delete()
            playbackFile = null
            file.delete()
            throw error
        }
    }

    fun stop() {
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
        playbackFile?.delete()
        playbackFile = null
    }
}
