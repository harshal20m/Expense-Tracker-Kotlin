package com.example.paisatracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One salary entry = one month's income.
 *
 * [receivedAt] is the timestamp when the user logged the salary.
 * Balance = salaryAmount - sum of all expenses with date >= receivedAt
 * that belong to the same calendar month.
 *
 * Resets monthly: each month the user adds a new SalaryRecord.
 * Previous months' records are kept for history.
 */
@Entity(tableName = "salary_records")
data class SalaryRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val amount: Double,                         // e.g. 75000.0
    val receivedAt: Long = System.currentTimeMillis(), // epoch ms — start of tracking
    val month: Int,                             // 1-12 (Calendar.MONTH + 1)
    val year: Int,                              // e.g. 2025
    val note: String = "",                      // optional label e.g. "April salary"
    val currency: String = "INR"
)
