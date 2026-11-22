package com.nxteam.irunner.data.library

import com.nxteam.irunner.data.database.InstalledAppEntity
import com.nxteam.irunner.emulator.LaunchOptions
import com.nxteam.irunner.ipa.CompatibilityLevel
import com.nxteam.irunner.ipa.CompatibilityReason

data class InstalledApp(
    val bundleIdentifier: String,
    val displayName: String,
    val version: String,
    val minimumOsVersion: String?,
    val architectures: List<String>,
    val deviceFamilies: List<Int>,
    val isEncrypted: Boolean,
    val compatibilityLevel: CompatibilityLevel,
    val compatibilityReasons: List<CompatibilityReason>,
    val ipaFilePath: String,
    val iconFilePath: String?,
    val fileSizeBytes: Long,
    val importedAtMillis: Long,
    val lastPlayedAtMillis: Long?,
    val launchOptions: LaunchOptions,
) {
    val supportsIphone: Boolean get() = deviceFamilies.isEmpty() || deviceFamilies.contains(1)
    val supportsIpad: Boolean get() = deviceFamilies.contains(2)
}

fun InstalledAppEntity.toModel(): InstalledApp = InstalledApp(
    bundleIdentifier = bundleIdentifier,
    displayName = displayName,
    version = version,
    minimumOsVersion = minimumOsVersion,
    architectures = architectures.split(',').filter { value -> value.isNotBlank() },
    deviceFamilies = deviceFamilies.split(',').mapNotNull { value -> value.trim().toIntOrNull() },
    isEncrypted = isEncrypted,
    compatibilityLevel = CompatibilityLevel.entries.firstOrNull { level -> level.name == compatibilityLevel }
        ?: CompatibilityLevel.UNLIKELY,
    compatibilityReasons = compatibilityReasons.split(',').mapNotNull { name ->
        CompatibilityReason.entries.firstOrNull { reason -> reason.name == name }
    },
    ipaFilePath = ipaFilePath,
    iconFilePath = iconFilePath,
    fileSizeBytes = fileSizeBytes,
    importedAtMillis = importedAtMillis,
    lastPlayedAtMillis = lastPlayedAtMillis,
    launchOptions = LaunchOptions.fromJson(launchOptionsJson),
)
