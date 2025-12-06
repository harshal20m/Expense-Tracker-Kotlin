package com.example.paisatracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "assets")
data class Asset(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val imagePath: String,
    val title: String = "",
    val description: String = "",
    val timestamp: Long = System.currentTimeMillis(),

    // NEW: optional link to an expense
    val expenseId: Long? = null       // null = old behaviour (independent asset)
)
