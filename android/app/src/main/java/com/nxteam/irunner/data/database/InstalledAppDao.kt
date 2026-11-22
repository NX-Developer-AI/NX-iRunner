package com.nxteam.irunner.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface InstalledAppDao {
    @Query("SELECT * FROM installed_apps ORDER BY COALESCE(lastPlayedAtMillis, importedAtMillis) DESC")
    fun observeAll(): Flow<List<InstalledAppEntity>>

    @Query("SELECT * FROM installed_apps WHERE bundleIdentifier = :bundleIdentifier")
    fun observe(bundleIdentifier: String): Flow<InstalledAppEntity?>

    @Query("SELECT * FROM installed_apps WHERE bundleIdentifier = :bundleIdentifier")
    suspend fun find(bundleIdentifier: String): InstalledAppEntity?

    @Query("SELECT * FROM installed_apps")
    suspend fun findAll(): List<InstalledAppEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: InstalledAppEntity)

    @Query("UPDATE installed_apps SET launchOptionsJson = :launchOptionsJson WHERE bundleIdentifier = :bundleIdentifier")
    suspend fun updateLaunchOptions(bundleIdentifier: String, launchOptionsJson: String)

    @Query("UPDATE installed_apps SET lastPlayedAtMillis = :timestamp WHERE bundleIdentifier = :bundleIdentifier")
    suspend fun updateLastPlayed(bundleIdentifier: String, timestamp: Long)

    @Query("DELETE FROM installed_apps WHERE bundleIdentifier = :bundleIdentifier")
    suspend fun delete(bundleIdentifier: String)
}
