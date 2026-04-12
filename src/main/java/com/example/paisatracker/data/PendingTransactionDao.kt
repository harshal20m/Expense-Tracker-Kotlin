package com.example.paisatracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingTransactionDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(txn: PendingTransaction): Long

    @Update
    suspend fun update(txn: PendingTransaction)

    @Delete
    suspend fun delete(txn: PendingTransaction)

    @Query("DELETE FROM pending_transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM pending_transactions ORDER BY capturedAt DESC")
    fun getAllPending(): Flow<List<PendingTransaction>>

    @Query("SELECT COUNT(*) FROM pending_transactions WHERE isReviewed = 0")
    fun getUnreviewedCount(): Flow<Int>

    /** Duplicate check — same UTR number already exists */
    @Query("SELECT COUNT(*) FROM pending_transactions WHERE utrNumber = :utr AND utrNumber != ''")
    suspend fun countByUtr(utr: String): Int

    /** Also check Expense table for same UTR to avoid cross-table duplicates */
    @Query("SELECT COUNT(*) FROM upi_transactions WHERE transactionId = :utr AND transactionId != ''")
    suspend fun countExpenseByUtr(utr: String): Int
}