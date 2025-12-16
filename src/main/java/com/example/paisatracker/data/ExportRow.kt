package com.example.paisatracker.data

data class ExportRow(
    val projectName: String?,      // optional in CSV
    val projectEmoji: String?,     // optional
    val categoryName: String,
    val categoryEmoji: String?,    // optional
    val description: String,
    val amount: Double,
    val date: Long,
    val paymentMethod: String?     // optional
)
