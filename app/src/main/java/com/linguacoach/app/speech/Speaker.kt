package com.linguacoach.app.speech

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

/** One selectable TTS voice. */
data class VoiceOption(val id: String, val label: String, val quality: Int)

/**
 * Android TextToSpeech wrapper. Reads the tutor's English replies (and, right
 * after, short spoken corrections) aloud, exposes a [speaking] flag for the avatar
 * animation, and lists the English voices installed on the device so the user can
 * pick a more natural one.
 */
class Speaker(context: Context) {
    private var ready = false
    private var pendingVoiceId: String? = null

    private val _speaking = MutableStateFlow(false)
    val speaking: StateFlow<Boolean> = _speaking

    private val _voices = MutableStateFlow<List<VoiceOption>>(emptyList())
    val voices: StateFlow<List<VoiceOption>> = _voices

    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
            ready = true
            buildVoiceList()
            pendingVoiceId?.let { applyVoice(it) }
        }
    }

    init {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) { _speaking.value = true }
            override fun onDone(utteranceId: String?) { if (utteranceId == "last") _speaking.value = false }
            @Deprecated("deprecated") override fun onError(utteranceId: String?) { _speaking.value = false }
            override fun onError(utteranceId: String?, errorCode: Int) { _speaking.value = false }
            override fun onStop(utteranceId: String?, interrupted: Boolean) { _speaking.value = false }
        })
    }

    private fun buildVoiceList() {
        val list = runCatching {
            tts.voices
                ?.filter { it.locale.language == "en" && !it.isNetworkConnectionRequired }
                ?.sortedByDescending { it.quality }
                ?.map { v ->
                    val country = v.locale.country.ifBlank { "" }
                    val accent = when (country) {
                        "US" -> "Americano"; "GB" -> "Britannico"; "AU" -> "Australiano"
                        "IN" -> "Indiano"; "CA" -> "Canadese"; "IE" -> "Irlandese"; else -> country
                    }
                    val gender = when {
                        v.name.contains("female", true) -> "donna"
                        v.name.contains("male", true) -> "uomo"
                        else -> ""
                    }
                    val stars = if (v.quality >= Voice.QUALITY_VERY_HIGH) " ⭐" else ""
                    VoiceOption(v.name, listOf("Inglese", accent, gender).filter { it.isNotBlank() }.joinToString(" ") + stars, v.quality)
                } ?: emptyList()
        }.getOrDefault(emptyList())
        _voices.value = list.distinctBy { it.id }
    }

    fun setVoice(id: String?) {
        if (id == null) { tts.language = Locale.US; return }
        if (ready) applyVoice(id) else pendingVoiceId = id
    }

    private fun applyVoice(id: String) {
        val v = runCatching { tts.voices?.firstOrNull { it.name == id } }.getOrNull()
        if (v != null) tts.voice = v
    }

    /** Speak the reply, then (optionally) a short correction right after it. */
    fun speak(text: String, followUp: String? = null) {
        if (!ready || text.isBlank()) return
        val last = if (followUp.isNullOrBlank()) "last" else "mid"
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, Bundle(), last)
        if (!followUp.isNullOrBlank()) {
            tts.speak(followUp, TextToSpeech.QUEUE_ADD, Bundle(), "last")
        }
    }

    fun stop() { if (ready) tts.stop(); _speaking.value = false }

    fun shutdown() { tts.stop(); tts.shutdown(); _speaking.value = false }
}
