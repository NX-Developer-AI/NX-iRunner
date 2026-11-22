package com.nxteam.irunner.ui.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nxteam.irunner.BuildConfig
import com.nxteam.irunner.R
import com.nxteam.irunner.data.settings.ThemeMode
import com.nxteam.irunner.emulator.EmulatorLauncher
import com.nxteam.irunner.ui.ContainerViewModelFactory
import com.nxteam.irunner.ui.components.LaunchOptionsEditor
import com.nxteam.irunner.ui.rememberAppContainer

private const val COMPATIBILITY_DATABASE_URL = "https://appdb.touchhle.org/"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val container = rememberAppContainer()
    val viewModel: SettingsViewModel = viewModel(
        factory = ContainerViewModelFactory { SettingsViewModel(container.settingsRepository) },
    )
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionCard(title = stringResource(R.string.settings_appearance)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = settings.themeMode == mode,
                            onClick = { viewModel.setThemeMode(mode) },
                            label = {
                                Text(
                                    stringResource(
                                        when (mode) {
                                            ThemeMode.SYSTEM -> R.string.theme_system
                                            ThemeMode.LIGHT -> R.string.theme_light
                                            ThemeMode.DARK -> R.string.theme_dark
                                        },
                                    ),
                                )
                            },
                        )
                    }
                }
            }
            SectionCard(title = stringResource(R.string.settings_default_options)) {
                LaunchOptionsEditor(
                    options = settings.defaultLaunchOptions,
                    onOptionsChange = viewModel::setDefaultLaunchOptions,
                )
            }
            SectionCard(title = stringResource(R.string.settings_tools)) {
                ActionRow(
                    icon = Icons.Rounded.Apps,
                    title = stringResource(R.string.settings_core_picker),
                    summary = stringResource(R.string.settings_core_picker_summary),
                    onClick = { EmulatorLauncher.launchCorePicker(context) },
                )
                ActionRow(
                    icon = Icons.Rounded.Folder,
                    title = stringResource(R.string.settings_file_manager),
                    summary = stringResource(R.string.settings_file_manager_summary),
                    onClick = { openDocumentsProvider(context) },
                )
                ActionRow(
                    icon = Icons.Rounded.Public,
                    title = stringResource(R.string.settings_compatibility_database),
                    summary = stringResource(R.string.settings_compatibility_database_summary),
                    onClick = { openUrl(context, COMPATIBILITY_DATABASE_URL) },
                )
            }
            SectionCard(title = stringResource(R.string.settings_about)) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        stringResource(R.string.settings_about_summary, BuildConfig.VERSION_NAME, BuildConfig.CORE_VERSION),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

@Composable
private fun ActionRow(icon: ImageVector, title: String, summary: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun openDocumentsProvider(context: Context) {
    val rootUri = DocumentsContract.buildRootUri(BuildConfig.PROVIDER_AUTHORITY, "root")
    val documentUri = DocumentsContract.buildDocumentUri(BuildConfig.PROVIDER_AUTHORITY, "root")
    val intents = listOf(
        Intent(Intent.ACTION_VIEW, documentUri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION),
        Intent("android.provider.action.BROWSE", rootUri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
        Intent(Intent.ACTION_VIEW, rootUri).setType(DocumentsContract.Root.MIME_TYPE_ITEM),
    )
    for (intent in intents) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        } catch (_: ActivityNotFoundException) {
        } catch (_: SecurityException) {
        }
    }
    Toast.makeText(context, R.string.file_manager_unavailable, Toast.LENGTH_LONG).show()
}

private fun openUrl(context: Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}
