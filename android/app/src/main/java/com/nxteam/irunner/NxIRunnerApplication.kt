package com.nxteam.irunner

import android.app.Application
import com.nxteam.irunner.data.database.AppDatabase
import com.nxteam.irunner.data.library.LibraryRepository
import com.nxteam.irunner.data.settings.SettingsRepository

class NxIRunnerApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

class AppContainer(application: Application) {
    val database: AppDatabase by lazy { AppDatabase.create(application) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(application) }
    val libraryRepository: LibraryRepository by lazy {
        LibraryRepository(application, database.installedAppDao(), settingsRepository)
    }
}
