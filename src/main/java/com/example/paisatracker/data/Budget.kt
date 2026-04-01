package com.example.paisatracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val emoji: String = "💰",
    val limitAmount: Double,
    val period: BudgetPeriod = BudgetPeriod.MONTHLY,
    val categoryId: Long? = null,      // null = applies to all categories (global budget)
    val projectId: Long? = null,       // null = applies to all projects
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)

enum class BudgetPeriod(val displayName: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    YEARLY("Yearly")
}