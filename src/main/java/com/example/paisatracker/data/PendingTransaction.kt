package com.example.paisatracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A transaction captured automatically from a UPI notification or SMS.
 * Lives in this table until the user reviews it on the Pending screen.
 *
 * On Save  → an Expense + optionally a UpiTransaction are inserted, this row deleted.
 * On Discard → this row deleted, nothing else saved.
 *
 * No FK to Expense — it's intentionally standalone until confirmed.
 */
@Entity(tableName = "pending_transactions")
data class PendingTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // ── Parsed fields ─────────────────────────────────────────────────────────
    val amount: Double,
    val payeeName: String,          // "Rahul Kumar", "DMart Store"
    val payeeVpa: String,           // "rahul@oksbi"  — blank for SMS
    val utrNumber: String,          // 12-digit UTR or txnId
    val sourceApp: String,          // "GPay" | "PhonePe" | "Paytm" | "Bank SMS" | "Unknown"
    val status: String,             // "Success" | "Failed" | "Pending"
    val transactionDate: String,    // human-readable date string from notification

    // ── User-assigned fields (filled on review screen) ─────────────────────
    val categoryId: Long? = null,
    val projectId: Long? = null,
    val note: String = "",

    // ── Raw notification payload ──────────────────────────────────────────
    val rawNotificationText: String = "",

    // ── Metadata ─────────────────────────────────────────────────────────
    val capturedAt: Long = System.currentTimeMillis(),
    val isReviewed: Boolean = false
)
