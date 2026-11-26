package com.linguacoach.app.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Wraps Android's on-device [SpeechRecognizer] to capture the learner's spoken
 * English. Create and call from the main thread.
 */
class SpeechManager(private val context: Context) {

    private var recognizer: SpeechRecognizer? = null
    private val _listening = MutableStateFlow(false)
    val listening: StateFlow<Boolean> = _listening
    private val _partial = MutableStateFlow("")
    val partial: StateFlow<String> = _partial

    val available: Boolean get() = SpeechRecognizer.isRecognitionAvailable(context)

    fun start(onFinal: (String) -> Unit, onError: (String) -> Unit) {
        if (!available) { onError("Riconoscimento vocale non disponibile su questo dispositivo."); return }
        stop()
        _partial.value = ""
        val sr = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = sr
        sr.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { _listening.value = true }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { _listening.value = false }
            override fun onPartialResults(partialResults: Bundle?) {
                partialResults?.stringList()?.firstOrNull()?.let { _partial.value = it }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
            override fun onError(error: Int) {
                _listening.value = false
                onError(errorText(error))
            }
            override fun onResults(results: Bundle?) {
                _listening.value = false
                val text = results?.stringList()?.firstOrNull().orEmpty()
                _partial.value = ""
                if (text.isNotBlank()) onFinal(text)
                else onError("Non ho capito, riprova.")
            }
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        sr.startListening(intent)
    }

    fun stop() {
        recognizer?.apply { stopListening(); destroy() }
        recognizer = null
        _listening.value = false
    }

    private fun Bundle.stringList(): List<String>? =
        getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)

    private fun errorText(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_NO_MATCH -> "Non ho capito, riprova."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Non ho sentito nulla."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permesso microfono negato."
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Problema di rete per il riconoscimento."
        else -> "Errore riconoscimento vocale ($error)."
    }
}
