package com.example.paisatracker.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = BankAccount::class,
            parentColumns = ["id"],
            childColumns = ["bankAccountId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["bankAccountId"])
    ]
)
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val date: Long,
    val description: String,
    val categoryId: Long,
    val assetPath: String? = null,

    // Payment method info
    val paymentMethod: String? = null,   // "UPI", "PhonePe", "GPay", "Cash", "Card", ...
    val paymentIcon: String? = null,     // Icon identifier for payment method
    
    // Bank account link (nullable - expenses can exist without account assignment)
    val bankAccountId: Long? = null
)