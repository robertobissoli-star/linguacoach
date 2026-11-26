package com.linguacoach.app.ui

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.linguacoach.app.AppViewModel
import com.linguacoach.app.MainActivity
import com.linguacoach.app.data.AVATAR_PRESETS
import com.linguacoach.app.data.ChatMessage
import com.linguacoach.app.data.Correction
import com.linguacoach.app.data.Level
import com.linguacoach.app.data.MODELS
import com.linguacoach.app.data.Role
import com.linguacoach.app.data.SCENARIOS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(vm: AppViewModel) {
    val ctx = LocalContext.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    val messages by vm.messages.collectAsStateWithLifecycle()
    val speaking by vm.speaker.speaking.collectAsStateWithLifecycle()
    val listening by vm.speech.listening.collectAsStateWithLifecycle()
    val partial by vm.speech.partial.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()

    var input by remember { mutableStateOf("") }
    var showSettings by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.speech.start(onFinal = { vm.sendText(it) }, onError = { })
    }
    fun toggleMic() {
        if (listening) { vm.speech.stop(); return }
        val granted = ContextCompat.checkSelfPermission(ctx, MainActivity.MIC_PERMISSION) == PackageManager.PERMISSION_GRANTED
        if (granted) vm.speech.start(onFinal = { vm.sendText(it) }, onError = { })
        else micPermission.launch(MainActivity.MIC_PERMISSION)
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }
    error?.let { LaunchedEffect(it) {} }

    Scaffold(
        topBar = {
            Surface(tonalElevation = 3.dp) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                        TutorAvatar(settings, speaking, sizeDp = 60)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(settings.tutorName, fontWeight = FontWeight.Bold)
                        Text(
                            when {
                                speaking -> "sta parlando…"
                                listening -> "ti ascolto…"
                                else -> "Tutor • livello ${settings.level}"
                            },
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                    IconButton(onClick = { vm.clearChat() }) { Icon(Icons.Default.AddComment, "nuova conversazione") }
                    IconButton(onClick = { showSettings = true }) { Icon(Icons.Default.Settings, "impostazioni") }
                }
            }
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Column {
                    error?.let {
                        Text(it, color = Color(0xFFF85149), fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp))
                    }
                    Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { toggleMic() }) {
                            Icon(if (listening) Icons.Default.Stop else Icons.Default.Mic,
                                "microfono", tint = if (listening) Color(0xFFF85149) else MaterialTheme.colorScheme.primary)
                        }
                        OutlinedTextField(
                            value = if (listening) partial else input,
                            onValueChange = { if (!listening) input = it },
                            placeholder = { Text(if (listening) "Parla in inglese…" else "Scrivi in inglese…") },
                            enabled = !listening,
                            modifier = Modifier.weight(1f),
                            maxLines = 4,
                            keyboardOptions = KeyboardOptions(autoCorrect = false),
                        )
                        IconButton(
                            onClick = { vm.sendText(input); input = "" },
                            enabled = input.isNotBlank() && !listening,
                        ) { Icon(Icons.Default.Send, "invia", tint = MaterialTheme.colorScheme.primary) }
                    }
                }
            }
        },
    ) { pad ->
        if (messages.isEmpty()) {
            EmptyState(settings.tutorName, Modifier.padding(pad)) { vm.startScenario(it) }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(pad),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(messages, key = { it.id }) { m -> MessageItem(m) { vm.replay(it) } }
            }
        }
    }

    if (showSettings) SettingsDialog(vm, onClose = { showSettings = false })
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmptyState(name: String, modifier: Modifier, onPick: (com.linguacoach.app.data.Scenario) -> Unit) {
    Column(modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(24.dp))
        Text("Ciao! Sono $name 👋", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text("Scegli un argomento per iniziare a conversare — o scrivi/parla pure liberamente.",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        Spacer(Modifier.height(20.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SCENARIOS.forEach { s ->
                FilterChip(selected = false, onClick = { onPick(s) }, label = { Text("${s.emoji} ${s.title}") })
            }
        }
    }
}

@Composable
private fun MessageItem(m: ChatMessage, onReplay: (String) -> Unit) {
    val isUser = m.role == Role.USER
    Column(Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
        Surface(
            color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.widthIn(max = 320.dp),
        ) {
            if (m.pending) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp)); Text("sta scrivendo…", fontSize = 13.sp)
                }
            } else {
                Column(Modifier.padding(12.dp)) {
                    Text(m.text, color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface)
                    if (!isUser) {
                        Spacer(Modifier.height(4.dp))
                        IconButton(onClick = { onReplay(m.text) }, modifier = Modifier.size(26.dp)) {
                            Icon(Icons.Default.VolumeUp, "ascolta", Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }
                }
            }
        }
        if (m.corrections.isNotEmpty() || m.encouragement != null) {
            Spacer(Modifier.height(4.dp))
            CorrectionCard(m.corrections, m.encouragement)
        }
    }
}

@Composable
private fun CorrectionCard(corrections: List<Correction>, encouragement: String?) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp)) {
            if (corrections.isNotEmpty()) {
                Text("✏️ Correzioni", fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(6.dp))
                corrections.forEach { c ->
                    Row {
                        Text(c.original, color = Color(0xFFF85149),
                            textDecoration = TextDecoration.LineThrough, fontSize = 13.sp)
                        Text("  →  ", fontSize = 13.sp)
                        Text(c.fixed, color = Color(0xFF3FB950), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                    if (c.note.isNotBlank())
                        Text(c.note, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    Spacer(Modifier.height(6.dp))
                }
            } else {
                Text("✅ Nessun errore, ottimo!", fontSize = 13.sp, color = Color(0xFF3FB950))
            }
            encouragement?.let {
                Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsDialog(vm: AppViewModel, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val voices by vm.speaker.voices.collectAsStateWithLifecycle()
    var name by remember { mutableStateOf(settings.tutorName) }
    var key by remember { mutableStateOf(settings.apiKey) }

    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            val b64 = withContext(Dispatchers.IO) { AvatarUtil.encodeFromUri(ctx, uri) }
            vm.setAvatar(b64)
        }
    }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Impostazioni") },
        text = {
            Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
                Text("Avatar del tutor", fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TutorAvatar(settings, speaking = false, sizeDp = 56)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        TextButton(onClick = { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                            Text(if (settings.avatar == null) "Carica foto" else "Cambia foto")
                        }
                        if (settings.avatar != null) TextButton(onClick = { vm.setAvatar(null) }) { Text("Rimuovi foto") }
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AVATAR_PRESETS.forEachIndexed { i, (emoji, _) ->
                        FilterChip(selected = settings.avatar == null && settings.avatarPreset == i,
                            onClick = { vm.setAvatarPreset(i) }, label = { Text(emoji) })
                    }
                }
                Spacer(Modifier.height(10.dp))

                OutlinedTextField(value = name, onValueChange = { name = it.take(20); vm.setTutorName(it) },
                    label = { Text("Nome tutor") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))

                Text("Livello", fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Level.entries.forEach { l ->
                        FilterChip(selected = settings.level == l.name, onClick = { vm.setLevel(l) },
                            label = { Text(l.code) })
                    }
                }
                Spacer(Modifier.height(10.dp))

                Text("Modello AI", fontWeight = FontWeight.SemiBold)
                MODELS.forEach { (id, label) ->
                    FilterChip(selected = settings.model == id, onClick = { vm.setModel(id) },
                        label = { Text(label) }, modifier = Modifier.padding(vertical = 2.dp))
                }
                Spacer(Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Leggi le risposte ad alta voce", Modifier.weight(1f), fontSize = 14.sp)
                    Switch(checked = settings.speakReplies, onCheckedChange = { vm.setSpeak(it) })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Leggi anche le correzioni a voce", Modifier.weight(1f), fontSize = 14.sp)
                    Switch(checked = settings.speakCorrections, onCheckedChange = { vm.setSpeakCorrections(it) })
                }
                Spacer(Modifier.height(10.dp))

                Text("Voce del tutor", fontWeight = FontWeight.SemiBold)
                if (voices.isEmpty()) {
                    Text("Nessuna voce extra trovata. Per voci più naturali installa \"Google Speech Services\" dal Play Store e scaricane le voci inglesi.",
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(selected = settings.voiceId == null, onClick = { vm.setVoice(null) }, label = { Text("Predefinita") })
                        voices.take(12).forEach { v ->
                            FilterChip(selected = settings.voiceId == v.id, onClick = { vm.setVoice(v.id) }, label = { Text(v.label) })
                        }
                    }
                    TextButton(onClick = { vm.replay("Hello! This is how I sound. Let's practice English together.") }) {
                        Text("▶ Prova la voce")
                    }
                }
                Spacer(Modifier.height(10.dp))

                OutlinedTextField(value = key, onValueChange = { key = it; vm.setApiKey(it) },
                    label = { Text("Chiave API") }, singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                TextButton(onClick = { vm.clearChat(); onClose() }) { Text("🗑 Cancella conversazione") }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("Chiudi") } },
    )
}
