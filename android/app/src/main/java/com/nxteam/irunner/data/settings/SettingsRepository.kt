package com.nxteam.irunner.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nxteam.irunner.emulator.LaunchOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val defaultLaunchOptions: LaunchOptions = LaunchOptions(),
)

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "nx_irunner_settings")

class SettingsRepository(private val context: Context) {
    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val defaultLaunchOptionsKey = stringPreferencesKey("default_launch_options")

    val settings: Flow<UserSettings> = context.settingsDataStore.data.map { preferences ->
        UserSettings(
            themeMode = ThemeMode.entries.firstOrNull { mode -> mode.name == preferences[themeModeKey] } ?: ThemeMode.SYSTEM,
            defaultLaunchOptions = LaunchOptions.fromJson(preferences[defaultLaunchOptionsKey]),
        )
    }

    suspend fun current(): UserSettings = settings.first()

    suspend fun setThemeMode(themeMode: ThemeMode) {
        context.settingsDataStore.edit { preferences -> preferences[themeModeKey] = themeMode.name }
    }

    suspend fun setDefaultLaunchOptions(options: LaunchOptions) {
        context.settingsDataStore.edit { preferences -> preferences[defaultLaunchOptionsKey] = options.toJson() }
    }
}
