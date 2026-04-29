package com.example.paisatracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "action_history")
data class ActionHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val actionType: String, // "DELETE"
    val entityType: String, // "EXPENSE", "BUDGET", "PROJECT", "CATEGORY"
    val entityData: String, // Serialized JSON data
    val timestamp: Long = System.currentTimeMillis()
)
