package com.nxteam.irunner.ui.library

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.PhoneIphone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nxteam.irunner.R
import com.nxteam.irunner.data.library.ImportResult
import com.nxteam.irunner.data.library.InstalledApp
import com.nxteam.irunner.ipa.IpaFormatException
import com.nxteam.irunner.ui.ContainerViewModelFactory
import com.nxteam.irunner.ui.components.AppIcon
import com.nxteam.irunner.ui.components.CompatibilityChip
import com.nxteam.irunner.ui.rememberAppContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    incomingUris: List<Uri>,
    onIncomingUrisConsumed: () -> Unit,
    onOpenApp: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenLogs: () -> Unit,
) {
    val container = rememberAppContainer()
    val viewModel: LibraryViewModel = viewModel(
        factory = ContainerViewModelFactory { LibraryViewModel(container.libraryRepository) },
    )
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val documentPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        viewModel.importUris(uris)
    }

    LaunchedEffect(incomingUris) {
        if (incomingUris.isNotEmpty()) {
            viewModel.importUris(incomingUris)
            onIncomingUrisConsumed()
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            val message = when (event) {
                is LibraryEvent.RescanFinished -> context.getString(R.string.rescan_result, event.importedCount)
                is LibraryEvent.ImportFinished -> when (val result = event.result) {
                    is ImportResult.Imported -> context.getString(
                        if (result.replacedExisting) R.string.import_replaced else R.string.import_success,
                        result.app.displayName,
                    )
                    is ImportResult.Failed -> context.getString(
                        R.string.import_failed,
                        describeImportError(context, result.error),
                    )
                }
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = viewModel::rescan) {
                        Icon(Icons.Rounded.Refresh, contentDescription = stringResource(R.string.action_rescan))
                    }
                    IconButton(onClick = onOpenLogs) {
                        Icon(Icons.Rounded.Terminal, contentDescription = stringResource(R.string.action_logs))
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.action_settings))
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { documentPicker.launch(arrayOf("*/*")) },
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.action_import)) },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isBusy) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    text = stringResource(R.string.importing),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            val currentApps = apps
            when {
                currentApps == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                currentApps.isEmpty() -> EmptyLibrary()
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 112.dp),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 96.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(currentApps, key = { app -> app.bundleIdentifier }) { app ->
                        AppCard(
                            app = app,
                            onClick = { viewModel.launch(app) },
                            onLongClick = { onOpenApp(app.bundleIdentifier) },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppCard(app: InstalledApp, onClick: () -> Unit, onLongClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(12.dp),
        ) {
            AppIcon(iconFilePath = app.iconFilePath, size = 64.dp)
            Spacer(Modifier.height(8.dp))
            Text(
                text = app.displayName,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Text(
                text = app.version,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            Spacer(Modifier.height(6.dp))
            CompatibilityChip(level = app.compatibilityLevel)
        }
    }
}

@Composable
private fun EmptyLibrary() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize().padding(32.dp),
    ) {
        Icon(
            Icons.Rounded.PhoneIphone,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(72.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.empty_library_title), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.empty_library_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private fun describeImportError(context: android.content.Context, error: Throwable): String =
    if (error is IpaFormatException) {
        context.getString(
            when (error.reason) {
                IpaFormatException.Reason.NOT_A_ZIP -> R.string.error_not_zip
                IpaFormatException.Reason.MISSING_APP_BUNDLE -> R.string.error_missing_bundle
                IpaFormatException.Reason.MISSING_INFO_PLIST -> R.string.error_missing_plist
                IpaFormatException.Reason.INVALID_INFO_PLIST -> R.string.error_invalid_plist
                IpaFormatException.Reason.MISSING_EXECUTABLE -> R.string.error_missing_executable
            },
        )
    } else {
        error.message ?: error.javaClass.simpleName
    }
