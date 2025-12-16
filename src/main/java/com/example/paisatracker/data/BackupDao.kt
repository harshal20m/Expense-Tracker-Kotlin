package com.example.paisatracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BackupDao {
    @Insert
    suspend fun insertBackup(backup: BackupMetadata): Long

    @Query("SELECT * FROM backup_metadata ORDER BY timestamp DESC")
    fun getAllBackups(): Flow<List<BackupMetadata>>

    @Query("SELECT * FROM backup_metadata ORDER BY timestamp DESC LIMIT 10")
    fun getRecentBackups(): Flow<List<BackupMetadata>>

    @Delete
    suspend fun deleteBackup(backup: BackupMetadata)

    @Query("DELETE FROM backup_metadata WHERE id = :backupId")
    suspend fun deleteBackupById(backupId: Long)

    @Query("DELETE FROM backup_metadata")
    suspend fun deleteAllBackups()

    @Query("SELECT COUNT(*) FROM backup_metadata")
    suspend fun getBackupCount(): Int
}