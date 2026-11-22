package com.nxteam.irunner.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nxteam.irunner.data.library.InstalledApp
import com.nxteam.irunner.data.library.LibraryRepository
import com.nxteam.irunner.emulator.LaunchOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppDetailViewModel(
    private val repository: LibraryRepository,
    private val bundleIdentifier: String,
) : ViewModel() {
    val app: StateFlow<InstalledApp?> = repository.observeApp(bundleIdentifier)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val editedOptionsState = MutableStateFlow<LaunchOptions?>(null)
    val editedOptions: StateFlow<LaunchOptions?> = editedOptionsState.asStateFlow()

    fun editOptions(options: LaunchOptions) {
        editedOptionsState.value = options
    }

    fun saveOptions(onSaved: () -> Unit) {
        val options = editedOptionsState.value ?: return
        viewModelScope.launch {
            repository.updateLaunchOptions(bundleIdentifier, options)
            editedOptionsState.value = null
            onSaved()
        }
    }

    fun launch() {
        viewModelScope.launch {
            editedOptionsState.value?.let { options -> repository.updateLaunchOptions(bundleIdentifier, options) }
            editedOptionsState.value = null
            repository.launch(bundleIdentifier)
        }
    }

    fun delete(deleteSaveData: Boolean, onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.delete(bundleIdentifier, deleteSaveData)
            onDeleted()
        }
    }
}
