package com.example.paisatracker.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expenses",
    foreignKeys = [ForeignKey(
        entity = Category::class,
        parentColumns = ["id"],
        childColumns = ["categoryId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["categoryId"])]
)
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val date: Long,
    val description: String,
    val categoryId: Long,
    val assetPath: String? = null,

    // NEW
    val paymentMethod: String? = null,   // "UPI", "PhonePe", "GPay", "Cash", "Card", ...
    val paymentIcon: String? = null      // "upi", "phonepe", "gpay", "paytm", "cash", "card" etc.
)
