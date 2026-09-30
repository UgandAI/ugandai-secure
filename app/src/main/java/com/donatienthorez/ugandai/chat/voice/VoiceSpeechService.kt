package com.donatienthorez.ugandai.chat.voice

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.UUID

interface VoiceSpeechListener {
    fun onStarted() = Unit
    fun onFinished() = Unit
    fun onError(message: String) = Unit
}

interface VoiceSpeechService {
    fun initialize(onReady: () -> Unit, onError: (String) -> Unit)
    fun speak(text: String, listener: VoiceSpeechListener)
    fun stop()
    fun destroy()
}

class AndroidVoiceSpeechService(context: Context) : VoiceSpeechService {
    private val appContext = context.applicationContext
    private var textToSpeech: TextToSpeech? = null
    private var ready = false
    private var initializationStarted = false

    override fun initialize(onReady: () -> Unit, onError: (String) -> Unit) {
        if (ready) {
            onReady()
            return
        }
        if (initializationStarted) return
        initializationStarted = true
        textToSpeech = TextToSpeech(appContext) { status ->
            if (status != TextToSpeech.SUCCESS) {
                initializationStarted = false
                onError("Text-to-speech could not be initialized")
                return@TextToSpeech
            }
            val languageResult = textToSpeech?.setLanguage(Locale.US) ?: TextToSpeech.LANG_NOT_SUPPORTED
            if (languageResult == TextToSpeech.LANG_MISSING_DATA || languageResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                onError("English text-to-speech is unavailable")
            } else {
                ready = true
                onReady()
            }
        }
    }

    override fun speak(text: String, listener: VoiceSpeechListener) {
        val engine = textToSpeech
        if (!ready || engine == null) {
            listener.onError("Text-to-speech is not ready")
            return
        }
        stop()
        val utteranceId = "ugandai-${UUID.randomUUID()}"
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) { if (id == utteranceId) listener.onStarted() }
            override fun onDone(id: String?) { if (id == utteranceId) listener.onFinished() }
            @Deprecated("Deprecated by Android")
            override fun onError(id: String?) { if (id == utteranceId) listener.onError("Speech playback failed") }
            override fun onError(id: String?, errorCode: Int) {
                if (id == utteranceId) listener.onError("Speech playback failed ($errorCode)")
            }
        })
        val result = engine.speak(text, TextToSpeech.QUEUE_FLUSH, Bundle(), utteranceId)
        if (result == TextToSpeech.ERROR) listener.onError("Speech playback could not start")
    }

    override fun stop() {
        textToSpeech?.stop()
    }

    override fun destroy() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        ready = false
        initializationStarted = false
    }
}
