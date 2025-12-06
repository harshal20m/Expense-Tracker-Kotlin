package com.example.paisatracker.data

import androidx.room.Embedded

data class ProjectWithTotal(
    @Embedded
    val project: Project,
    val totalAmount: Double,
    val categoryCount: Int = 0,    // Add this
    val expenseCount: Int = 0      // Add this
)
