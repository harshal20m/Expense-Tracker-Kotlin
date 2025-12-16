package com.example.paisatracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "backup_metadata")
data class BackupMetadata(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fileName: String,
    val filePath: String,
    val fileSize: Long, // in bytes
    val timestamp: Long = System.currentTimeMillis(),
    val projectCount: Int,
    val categoryCount: Int,
    val expenseCount: Int,
    val totalAmount: Double
)
