package com.nxteam.irunner.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "installed_apps")
data class InstalledAppEntity(
    @PrimaryKey val bundleIdentifier: String,
    val displayName: String,
    val version: String,
    val minimumOsVersion: String?,
    val architectures: String,
    val deviceFamilies: String,
    val isEncrypted: Boolean,
    val compatibilityLevel: String,
    val compatibilityReasons: String,
    val ipaFilePath: String,
    val iconFilePath: String?,
    val fileSizeBytes: Long,
    val importedAtMillis: Long,
    val lastPlayedAtMillis: Long?,
    val launchOptionsJson: String,
)
