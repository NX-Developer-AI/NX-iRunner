package com.nxteam.irunner.ui.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nxteam.irunner.data.library.LibraryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LogViewModel(private val repository: LibraryRepository) : ViewModel() {
    private val logState = MutableStateFlow<String?>(null)
    val log: StateFlow<String?> = logState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch { logState.value = repository.readLog() }
    }

    fun clear() {
        viewModelScope.launch {
            repository.clearLog()
            logState.value = ""
        }
    }
}
