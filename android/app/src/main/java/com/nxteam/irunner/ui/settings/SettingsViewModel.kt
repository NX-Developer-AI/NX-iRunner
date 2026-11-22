package com.nxteam.irunner.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nxteam.irunner.data.settings.SettingsRepository
import com.nxteam.irunner.data.settings.ThemeMode
import com.nxteam.irunner.data.settings.UserSettings
import com.nxteam.irunner.emulator.LaunchOptions
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {
    val settings: StateFlow<UserSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserSettings())

    fun setThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch { repository.setThemeMode(themeMode) }
    }

    fun setDefaultLaunchOptions(options: LaunchOptions) {
        viewModelScope.launch { repository.setDefaultLaunchOptions(options) }
    }
}
