package com.nxteam.irunner.ui.detail

import android.text.format.Formatter
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
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nxteam.irunner.R
import com.nxteam.irunner.data.library.InstalledApp
import com.nxteam.irunner.ui.ContainerViewModelFactory
import com.nxteam.irunner.ui.components.AppIcon
import com.nxteam.irunner.ui.components.CompatibilityChip
import com.nxteam.irunner.ui.components.LaunchOptionsEditor
import com.nxteam.irunner.ui.components.color
import com.nxteam.irunner.ui.components.messageResource
import com.nxteam.irunner.ui.rememberAppContainer
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailScreen(bundleIdentifier: String, onBack: () -> Unit) {
    val container = rememberAppContainer()
    val viewModel: AppDetailViewModel = viewModel(
        key = bundleIdentifier,
        factory = ContainerViewModelFactory { AppDetailViewModel(container.libraryRepository, bundleIdentifier) },
    )
    val app by viewModel.app.collectAsStateWithLifecycle()
    val editedOptions by viewModel.editedOptions.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(app?.displayName ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    if (editedOptions != null) {
                        IconButton(onClick = {
                            viewModel.saveOptions {
                                scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.options_saved)) }
                            }
                        }) {
                            Icon(Icons.Rounded.Save, contentDescription = stringResource(R.string.options_saved))
                        }
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.action_delete))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        val currentApp = app
        if (currentApp == null) {
            Column(
                modifier = Modifier.padding(padding).fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Header(currentApp, onLaunch = viewModel::launch)
            CompatibilityCard(currentApp)
            InformationCard(currentApp)
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.options_title), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    LaunchOptionsEditor(
                        options = editedOptions ?: currentApp.launchOptions,
                        onOptionsChange = viewModel::editOptions,
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
        if (showDeleteDialog) {
            DeleteDialog(
                appName = currentApp.displayName,
                onDismiss = { showDeleteDialog = false },
                onConfirm = { deleteSaveData ->
                    showDeleteDialog = false
                    viewModel.delete(deleteSaveData, onBack)
                },
            )
        }
    }
}

@Composable
private fun Header(app: InstalledApp, onLaunch: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AppIcon(iconFilePath = app.iconFilePath, size = 88.dp)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(app.displayName, style = MaterialTheme.typography.headlineSmall)
            Text(
                app.bundleIdentifier,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = onLaunch) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.action_launch))
            }
        }
    }
}

@Composable
private fun CompatibilityCard(app: InstalledApp) {
    Card(
        colors = CardDefaults.cardColors(containerColor = app.compatibilityLevel.color().copy(alpha = 0.10f)),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.detail_compatibility),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                CompatibilityChip(app.compatibilityLevel)
            }
            app.compatibilityReasons.forEach { reason ->
                Text(stringResource(reason.messageResource()), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun InformationCard(app: InstalledApp) {
    val context = LocalContext.current
    val unknown = stringResource(R.string.detail_unknown)
    val devices = buildList {
        if (app.supportsIphone) add(stringResource(R.string.device_iphone))
        if (app.supportsIpad) add(stringResource(R.string.device_ipad))
    }.joinToString(" · ")
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            InformationRow(stringResource(R.string.detail_bundle_identifier), app.bundleIdentifier)
            HorizontalDivider()
            InformationRow(stringResource(R.string.detail_version), app.version)
            HorizontalDivider()
            InformationRow(stringResource(R.string.detail_minimum_os), app.minimumOsVersion ?: unknown)
            HorizontalDivider()
            InformationRow(
                stringResource(R.string.detail_architectures),
                app.architectures.joinToString(", ").ifEmpty { unknown },
            )
            HorizontalDivider()
            InformationRow(stringResource(R.string.detail_devices), devices.ifEmpty { unknown })
            HorizontalDivider()
            InformationRow(stringResource(R.string.detail_size), Formatter.formatFileSize(context, app.fileSizeBytes))
        }
    }
}

@Composable
private fun InformationRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.45f),
        )
        Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(0.55f))
    }
}

@Composable
private fun DeleteDialog(appName: String, onDismiss: () -> Unit, onConfirm: (Boolean) -> Unit) {
    var deleteSaveData by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_title, appName)) },
        text = {
            Column {
                Text(stringResource(R.string.delete_message))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = deleteSaveData, onCheckedChange = { deleteSaveData = it })
                    Text(stringResource(R.string.delete_save_data))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(deleteSaveData) }) { Text(stringResource(R.string.action_delete)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
