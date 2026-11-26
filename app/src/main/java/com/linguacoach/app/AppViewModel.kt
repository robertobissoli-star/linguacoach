package com.linguacoach.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.linguacoach.app.ai.ClaudeClient
import com.linguacoach.app.ai.ClaudeException
import com.linguacoach.app.data.ChatMessage
import com.linguacoach.app.data.ChatRepository
import com.linguacoach.app.data.Level
import com.linguacoach.app.data.Role
import com.linguacoach.app.data.Scenario
import com.linguacoach.app.data.Settings
import com.linguacoach.app.speech.Speaker
import com.linguacoach.app.speech.SpeechManager
import com.linguacoach.app.update.UpdateInfo
import com.linguacoach.app.update.UpdateManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = ChatRepository.get(app)
    private val claude = ClaudeClient()
    private val updater = UpdateManager()
    val speaker = Speaker(app)
    val speech = SpeechManager(app)

    private val _update = MutableStateFlow<UpdateInfo?>(null)
    val update: StateFlow<UpdateInfo?> = _update
    private val _updateStatus = MutableStateFlow<String?>(null)
    val updateStatus: StateFlow<String?> = _updateStatus

    val versionName: String get() = updater.currentVersionName()

    val settings: StateFlow<Settings> = repo.settings
    val messages: StateFlow<List<ChatMessage>> = repo.messages

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    fun clearError() { _error.value = null }

    init {
        // Apply the saved voice whenever it loads or changes (Speaker queues if TTS not ready yet).
        viewModelScope.launch {
            settings.map { it.voiceId }.distinctUntilChanged().collect { speaker.setVoice(it) }
        }
        // Auto-check for updates on launch (once settings have loaded).
        viewModelScope.launch {
            settings.map { it.onboarded to it.autoUpdate }.distinctUntilChanged().collect { (onb, auto) ->
                if (onb && auto && _update.value == null) checkUpdates(silent = true)
            }
        }
    }

    // ---- updates --------------------------------------------------------

    fun checkUpdates(silent: Boolean) {
        viewModelScope.launch {
            if (!silent) _updateStatus.value = "Controllo aggiornamenti…"
            val info = runCatching { updater.check(repo.current.githubToken) }.getOrNull()
            _update.value = info
            if (!silent) _updateStatus.value =
                if (info != null) "Aggiornamento disponibile: ${info.name}"
                else "Sei già all'ultima versione (v${updater.currentVersionCode()})."
        }
    }

    fun installUpdate() {
        val info = _update.value ?: return
        viewModelScope.launch {
            _updateStatus.value = "Scarico l'aggiornamento…"
            try {
                updater.downloadAndInstall(getApplication(), info, repo.current.githubToken)
                _updateStatus.value = null
            } catch (e: Exception) {
                _updateStatus.value = "Errore aggiornamento: ${e.message}"
            }
        }
    }

    fun dismissUpdate() { _update.value = null }
    fun setGithubToken(t: String) = repo.updateSettings { it.copy(githubToken = t.trim()) }
    fun setAutoUpdate(on: Boolean) = repo.updateSettings { it.copy(autoUpdate = on) }

    private val level: Level
        get() = runCatching { Level.valueOf(repo.current.level) }.getOrDefault(Level.B1)

    // ---- settings -------------------------------------------------------

    fun completeOnboarding(apiKey: String, level: Level, tutorName: String) =
        repo.updateSettings { it.copy(apiKey = apiKey.trim(), level = level.name, tutorName = tutorName.trim().ifBlank { "Alex" }, onboarded = true) }

    fun setApiKey(key: String) = repo.updateSettings { it.copy(apiKey = key.trim()) }
    fun setLevel(l: Level) = repo.updateSettings { it.copy(level = l.name) }
    fun setModel(id: String) = repo.updateSettings { it.copy(model = id) }
    fun setSpeak(on: Boolean) = repo.updateSettings { it.copy(speakReplies = on) }
    fun setSpeakCorrections(on: Boolean) = repo.updateSettings { it.copy(speakCorrections = on) }
    fun setVoice(id: String?) { repo.updateSettings { it.copy(voiceId = id) }; speaker.setVoice(id) }
    fun setTutorName(n: String) = repo.updateSettings { it.copy(tutorName = n.trim().ifBlank { "Alex" }) }
    fun setAvatar(base64: String?) = repo.updateSettings { it.copy(avatar = base64) }
    fun setAvatarPreset(i: Int) = repo.updateSettings { it.copy(avatar = null, avatarPreset = i) }

    fun clearChat() { speaker.stop(); repo.clearChat() }

    // ---- conversation ---------------------------------------------------

    fun startScenario(scenario: Scenario) {
        repo.clearChat()
        // Seed with a hidden-style user instruction so the tutor opens the scene.
        val seed = ChatMessage(UUID.randomUUID().toString(), Role.USER, scenario.opener)
        send(seed, showUser = false)
    }

    fun sendText(text: String) {
        val t = text.trim()
        if (t.isEmpty()) return
        send(ChatMessage(UUID.randomUUID().toString(), Role.USER, t), showUser = true)
    }

    private fun send(userMsg: ChatMessage, showUser: Boolean) {
        val s = repo.current
        if (s.apiKey.isBlank()) { _error.value = "Imposta prima la tua chiave API nelle impostazioni."; return }

        val base = repo.messages.value
        val history = if (showUser) base + userMsg else base + userMsg.copy()
        val pending = ChatMessage(UUID.randomUUID().toString(), Role.TUTOR, "", pending = true)
        // Show the user bubble (unless seeded) plus a typing tutor bubble.
        repo.setMessages(if (showUser) history + pending else base + pending)

        viewModelScope.launch {
            try {
                val reply = claude.converse(s.apiKey, s.model, level, history)
                val tutorMsg = ChatMessage(
                    id = pending.id, role = Role.TUTOR, text = reply.reply,
                    corrections = reply.corrections, encouragement = reply.encouragement,
                )
                val shown = (if (showUser) history else base) + tutorMsg
                repo.setMessages(shown)
                if (s.speakReplies) {
                    // Speak the reply and, right after, a short spoken correction — like a real chat.
                    val followUp = if (s.speakCorrections) reply.corrections.firstOrNull()?.let { c ->
                        "Quick correction: say, ${c.fixed}. Instead of, ${c.original}."
                    } else null
                    speaker.speak(reply.reply, followUp)
                }
            } catch (e: ClaudeException) {
                repo.setMessages((if (showUser) history else base)) // drop pending
                _error.value = e.message
            } catch (e: Exception) {
                repo.setMessages((if (showUser) history else base))
                _error.value = "Errore di rete: ${e.message}"
            }
        }
    }

    /** Explicit tap on the speaker icon — always speaks, ignoring the auto-read toggle. */
    fun replay(text: String) { speaker.speak(text) }

    override fun onCleared() {
        speaker.shutdown()
        speech.stop()
        super.onCleared()
    }
}
