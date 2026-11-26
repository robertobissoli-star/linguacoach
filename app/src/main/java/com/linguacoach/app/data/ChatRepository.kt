package com.linguacoach.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "linguacoach")
private val SETTINGS = stringPreferencesKey("settings_json")
private val HISTORY = stringPreferencesKey("history_json")

/** Persists settings and the current conversation on device. */
class ChatRepository private constructor(appContext: Context) {

    private val ctx = appContext.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val _settings = MutableStateFlow(Settings())
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    init {
        scope.launch {
            val prefs = ctx.dataStore.data.first()
            prefs[SETTINGS]?.let { raw -> runCatching { json.decodeFromString<Settings>(raw) }.getOrNull()?.let { _settings.value = it } }
            prefs[HISTORY]?.let { raw ->
                runCatching { json.decodeFromString(ListSerializer(ChatMessage.serializer()), raw) }
                    .getOrNull()?.let { _messages.value = it }
            }
        }
    }

    val current: Settings get() = _settings.value

    fun updateSettings(block: (Settings) -> Settings) {
        val next = block(_settings.value)
        _settings.value = next
        scope.launch { ctx.dataStore.edit { it[SETTINGS] = json.encodeToString(Settings.serializer(), next) } }
    }

    fun setMessages(list: List<ChatMessage>) {
        _messages.value = list
        scope.launch {
            ctx.dataStore.edit {
                it[HISTORY] = json.encodeToString(ListSerializer(ChatMessage.serializer()), list.filter { m -> !m.pending })
            }
        }
    }

    fun clearChat() = setMessages(emptyList())

    companion object {
        @Volatile private var INSTANCE: ChatRepository? = null
        fun get(context: Context): ChatRepository =
            INSTANCE ?: synchronized(this) { INSTANCE ?: ChatRepository(context).also { INSTANCE = it } }
    }
}
