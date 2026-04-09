package com.example.paisatracker.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tracks UPI payment attempts linked to an Expense record.
 * Created with PENDING status before the UPI intent fires.
 * Updated to SUCCESS or FAILED when ActivityResultLauncher returns.
 */
@Entity(
    tableName = "upi_transactions",
    foreignKeys = [ForeignKey(
        entity    = Expense::class,
        parentColumns = ["id"],
        childColumns  = ["expenseId"],
        onDelete  = ForeignKey.CASCADE
    )],
    indices = [Index("expenseId")]
)
data class UpiTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val expenseId: Long,            // linked Expense record

    // UPI payment fields
    val vpa: String,                // e.g.  "dmart@upi"
    val payeeName: String,          // e.g.  "DMart Store"
    val amount: Double,
    val transactionNote: String = "",

    // Payment result
    val status: UpiStatus = UpiStatus.PENDING,
    val transactionId: String? = null,      // txnId returned by UPI app
    val responseCode: String? = null,       // raw response code
    val rawResponse: String? = null,        // full raw UPI callback string

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class UpiStatus { PENDING, SUCCESS, FAILED, SUBMITTED }