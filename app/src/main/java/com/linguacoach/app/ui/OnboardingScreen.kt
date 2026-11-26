package com.linguacoach.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linguacoach.app.AppViewModel
import com.linguacoach.app.data.Level

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(vm: AppViewModel) {
    var key by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("Alex") }
    var level by remember { mutableStateOf(Level.B1) }

    Scaffold { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).padding(24.dp).verticalScroll(rememberScrollState()),
        ) {
            Text("LinguaCoach", fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text("Impara l'inglese parlato e scritto con un tutor AI che ti corregge all'istante.",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            Spacer(Modifier.height(20.dp))

            Text("Chiave API Anthropic", fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = key, onValueChange = { key = it },
                placeholder = { Text("sk-ant-…") },
                singleLine = true, visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )
            Text("La chiave resta sul tuo telefono. Ottienila su console.anthropic.com.",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Spacer(Modifier.height(16.dp))

            Text("Come vuoi chiamare il tutor?", fontWeight = FontWeight.SemiBold)
            OutlinedTextField(value = name, onValueChange = { name = it.take(20) }, singleLine = true,
                modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))

            Text("Il tuo livello", fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Level.entries.forEach { l ->
                    FilterChip(selected = level == l, onClick = { level = l },
                        label = { Text("${l.code} · ${l.label}") })
                }
            }
            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { vm.completeOnboarding(key, level, name) },
                enabled = key.trim().length > 10,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Inizia a parlare") }
            Spacer(Modifier.height(8.dp))
            Text("Potrai cambiare livello, modello e avatar nelle impostazioni.",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
    }
}
