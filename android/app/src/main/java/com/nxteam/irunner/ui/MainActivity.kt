package com.nxteam.irunner.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nxteam.irunner.NxIRunnerApplication
import com.nxteam.irunner.data.settings.UserSettings
import com.nxteam.irunner.ui.theme.NxIRunnerTheme

class MainActivity : ComponentActivity() {
    private var pendingUris by mutableStateOf<List<Uri>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) {
            pendingUris = extractUris(intent)
        }
        val settingsRepository = (application as NxIRunnerApplication).container.settingsRepository
        setContent {
            val settings by settingsRepository.settings.collectAsStateWithLifecycle(initialValue = UserSettings())
            NxIRunnerTheme(themeMode = settings.themeMode) {
                NxNavigation(
                    incomingUris = pendingUris,
                    onIncomingUrisConsumed = { pendingUris = emptyList() },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pendingUris = extractUris(intent)
    }

    private fun extractUris(intent: Intent?): List<Uri> {
        if (intent == null) {
            return emptyList()
        }
        return when (intent.action) {
            Intent.ACTION_VIEW -> listOfNotNull(intent.data)
            Intent.ACTION_SEND -> listOfNotNull(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                },
            )
            else -> emptyList()
        }
    }
}
