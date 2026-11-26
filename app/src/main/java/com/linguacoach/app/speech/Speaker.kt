package com.linguacoach.app.speech

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

/**
 * Android TextToSpeech wrapper that reads the tutor's English replies aloud and
 * exposes a [speaking] flag so the UI can animate the avatar's mouth while it talks.
 */
class Speaker(context: Context) {
    private var ready = false
    private val _speaking = MutableStateFlow(false)
    val speaking: StateFlow<Boolean> = _speaking

    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
            ready = true
        }
    }

    init {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) { _speaking.value = true }
            override fun onDone(utteranceId: String?) { _speaking.value = false }
            @Deprecated("deprecated") override fun onError(utteranceId: String?) { _speaking.value = false }
            override fun onError(utteranceId: String?, errorCode: Int) { _speaking.value = false }
            override fun onStop(utteranceId: String?, interrupted: Boolean) { _speaking.value = false }
        })
    }

    fun speak(text: String) {
        if (!ready || text.isBlank()) return
        val params = Bundle()
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, "tutor")
    }

    fun stop() { if (ready) tts.stop(); _speaking.value = false }

    fun shutdown() { tts.stop(); tts.shutdown(); _speaking.value = false }
}
