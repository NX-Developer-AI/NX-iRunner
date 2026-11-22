package com.nxteam.irunner.data.library

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.nxteam.irunner.data.database.InstalledAppDao
import com.nxteam.irunner.data.database.InstalledAppEntity
import com.nxteam.irunner.data.settings.SettingsRepository
import com.nxteam.irunner.emulator.EmulatorLauncher
import com.nxteam.irunner.emulator.LaunchOptions
import com.nxteam.irunner.ipa.CompatibilityEvaluator
import com.nxteam.irunner.ipa.IpaInspector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

sealed interface ImportResult {
    data class Imported(val app: InstalledApp, val replacedExisting: Boolean) : ImportResult
    data class Failed(val error: Throwable) : ImportResult
}

class LibraryRepository(
    private val context: Context,
    private val dao: InstalledAppDao,
    private val settingsRepository: SettingsRepository,
) {
    val appsDirectory: File
        get() = File(storageRoot, APPS_DIRECTORY_NAME).apply { mkdirs() }

    val logFile: File
        get() = File(storageRoot, LOG_FILE_NAME)

    private val storageRoot: File
        get() = context.getExternalFilesDir(null) ?: context.filesDir

    private val iconsDirectory: File
        get() = File(context.filesDir, "icons").apply { mkdirs() }

    private val importDirectory: File
        get() = File(context.cacheDir, "import").apply { mkdirs() }

    fun observeApps(): Flow<List<InstalledApp>> = dao.observeAll().map { entities -> entities.map { it.toModel() } }

    fun observeApp(bundleIdentifier: String): Flow<InstalledApp?> =
        dao.observe(bundleIdentifier).map { entity -> entity?.toModel() }

    suspend fun importFromUri(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        val temporaryFile = File(importDirectory, "incoming-${System.nanoTime()}.ipa")
        try {
            val input = context.contentResolver.openInputStream(uri)
                ?: throw IOException("Cannot open ${queryDisplayName(uri) ?: uri}")
            input.use { stream -> temporaryFile.outputStream().use { output -> stream.copyTo(output) } }
            importFile(temporaryFile)
        } catch (exception: Throwable) {
            ImportResult.Failed(exception)
        } finally {
            temporaryFile.delete()
        }
    }

    private suspend fun importFile(sourceFile: File): ImportResult {
        val metadata = IpaInspector.inspect(sourceFile, importDirectory)
        val report = CompatibilityEvaluator.evaluate(metadata)
        val safeName = metadata.bundleIdentifier.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val targetFile = File(appsDirectory, "$safeName.ipa")
        val existing = dao.find(metadata.bundleIdentifier)
        if (targetFile.exists()) {
            targetFile.delete()
        }
        if (!sourceFile.renameTo(targetFile)) {
            sourceFile.copyTo(targetFile, overwrite = true)
        }
        val iconFile = metadata.iconPng?.let { bytes ->
            File(iconsDirectory, "$safeName.png").apply { writeBytes(bytes) }
        }
        val launchOptions = existing?.launchOptionsJson
            ?: settingsRepository.current().defaultLaunchOptions.toJson()
        val entity = InstalledAppEntity(
            bundleIdentifier = metadata.bundleIdentifier,
            displayName = metadata.displayName,
            version = metadata.visibleVersion,
            minimumOsVersion = metadata.minimumOsVersion,
            architectures = metadata.machO?.architectures?.joinToString(",") ?: "",
            deviceFamilies = metadata.deviceFamilies.joinToString(","),
            isEncrypted = metadata.machO?.slices?.any { slice -> slice.isEncrypted } ?: false,
            compatibilityLevel = report.level.name,
            compatibilityReasons = report.reasons.joinToString(",") { reason -> reason.name },
            ipaFilePath = targetFile.absolutePath,
            iconFilePath = iconFile?.absolutePath,
            fileSizeBytes = targetFile.length(),
            importedAtMillis = System.currentTimeMillis(),
            lastPlayedAtMillis = existing?.lastPlayedAtMillis,
            launchOptionsJson = launchOptions,
        )
        dao.upsert(entity)
        return ImportResult.Imported(entity.toModel(), replacedExisting = existing != null)
    }

    suspend fun rescanAppsDirectory(): Int = withContext(Dispatchers.IO) {
        val knownPaths = dao.findAll().map { entity -> entity.ipaFilePath }.toSet()
        var importedCount = 0
        appsDirectory.listFiles { file -> file.isFile && file.extension.equals("ipa", ignoreCase = true) }
            ?.filter { file -> file.absolutePath !in knownPaths }
            ?.forEach { file ->
                val copy = File(importDirectory, "rescan-${System.nanoTime()}.ipa")
                runCatching {
                    file.copyTo(copy, overwrite = true)
                    val result = importFile(copy)
                    if (result is ImportResult.Imported) {
                        importedCount++
                        if (file.absolutePath != result.app.ipaFilePath) {
                            file.delete()
                        }
                    }
                }
                copy.delete()
            }
        dao.findAll().filter { entity -> !File(entity.ipaFilePath).exists() }.forEach { entity ->
            dao.delete(entity.bundleIdentifier)
        }
        importedCount
    }

    suspend fun updateLaunchOptions(bundleIdentifier: String, options: LaunchOptions) {
        dao.updateLaunchOptions(bundleIdentifier, options.toJson())
    }

    suspend fun delete(bundleIdentifier: String, deleteSaveData: Boolean) = withContext(Dispatchers.IO) {
        val entity = dao.find(bundleIdentifier) ?: return@withContext
        File(entity.ipaFilePath).delete()
        entity.iconFilePath?.let { path -> File(path).delete() }
        if (deleteSaveData) {
            File(File(storageRoot, SANDBOX_DIRECTORY_NAME), bundleIdentifier).deleteRecursively()
        }
        dao.delete(bundleIdentifier)
    }

    suspend fun launch(bundleIdentifier: String) {
        val entity = dao.find(bundleIdentifier) ?: return
        dao.updateLastPlayed(bundleIdentifier, System.currentTimeMillis())
        withContext(Dispatchers.Main) {
            EmulatorLauncher.launchApp(context, entity.ipaFilePath, LaunchOptions.fromJson(entity.launchOptionsJson))
        }
    }

    suspend fun readLog(): String = withContext(Dispatchers.IO) {
        val file = logFile
        if (!file.exists()) {
            return@withContext ""
        }
        val maximumBytes = 256 * 1024L
        if (file.length() <= maximumBytes) {
            file.readText()
        } else {
            file.inputStream().use { stream ->
                stream.skip(file.length() - maximumBytes)
                String(stream.readBytes())
            }
        }
    }

    suspend fun clearLog() = withContext(Dispatchers.IO) {
        logFile.takeIf { it.exists() }?.writeText("")
    }

    private fun queryDisplayName(uri: Uri): String? =
        runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull()

    companion object {
        const val APPS_DIRECTORY_NAME = "touchHLE_apps"
        const val SANDBOX_DIRECTORY_NAME = "touchHLE_sandbox"
        const val LOG_FILE_NAME = "touchHLE_log.txt"
    }
}
