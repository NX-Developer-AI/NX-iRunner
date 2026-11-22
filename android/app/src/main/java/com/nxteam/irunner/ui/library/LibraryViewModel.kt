package com.nxteam.irunner.ui.library

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nxteam.irunner.data.library.ImportResult
import com.nxteam.irunner.data.library.InstalledApp
import com.nxteam.irunner.data.library.LibraryRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface LibraryEvent {
    data class ImportFinished(val result: ImportResult) : LibraryEvent
    data class RescanFinished(val importedCount: Int) : LibraryEvent
}

class LibraryViewModel(private val repository: LibraryRepository) : ViewModel() {
    val apps: StateFlow<List<InstalledApp>?> = repository.observeApps()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val busyState = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = busyState.asStateFlow()

    private val eventFlow = MutableSharedFlow<LibraryEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<LibraryEvent> = eventFlow.asSharedFlow()

    fun importUris(uris: List<Uri>) {
        if (uris.isEmpty()) {
            return
        }
        viewModelScope.launch {
            busyState.value = true
            try {
                uris.forEach { uri -> eventFlow.emit(LibraryEvent.ImportFinished(repository.importFromUri(uri))) }
            } finally {
                busyState.value = false
            }
        }
    }

    fun rescan() {
        viewModelScope.launch {
            busyState.value = true
            try {
                eventFlow.emit(LibraryEvent.RescanFinished(repository.rescanAppsDirectory()))
            } finally {
                busyState.value = false
            }
        }
    }

    fun launch(app: InstalledApp) {
        viewModelScope.launch { repository.launch(app.bundleIdentifier) }
    }
}
