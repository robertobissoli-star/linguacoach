package com.linguacoach.app

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.linguacoach.app.ui.ChatScreen
import com.linguacoach.app.ui.OnboardingScreen
import com.linguacoach.app.ui.theme.LinguaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LinguaTheme {
                val vm: AppViewModel = viewModel()
                val settings by vm.settings.collectAsStateWithLifecycle()
                if (settings.onboarded) ChatScreen(vm)
                else OnboardingScreen(vm)
            }
        }
    }

    companion object {
        val MIC_PERMISSION = Manifest.permission.RECORD_AUDIO
    }
}
