package com.example.paisatracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UpiTransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(txn: UpiTransaction): Long

    @Query("UPDATE upi_transactions SET status = :status, transactionId = :txnId, responseCode = :code, rawResponse = :raw, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(
        id: Long,
        status: UpiStatus,
        txnId: String?,
        code: String?,
        raw: String?,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("SELECT * FROM upi_transactions ORDER BY createdAt DESC")
    fun getAllTransactions(): Flow<List<UpiTransaction>>

    @Query("SELECT * FROM upi_transactions WHERE status = 'PENDING' ORDER BY createdAt DESC")
    fun getPendingTransactions(): Flow<List<UpiTransaction>>

    @Query("SELECT * FROM upi_transactions WHERE id = :id")
    suspend fun getById(id: Long): UpiTransaction?

    @Query("SELECT * FROM upi_transactions WHERE expenseId = :expenseId LIMIT 1")
    suspend fun getByExpenseId(expenseId: Long): UpiTransaction?

    @Delete
    suspend fun delete(txn: UpiTransaction)

    @Query("SELECT * FROM upi_transactions WHERE expenseId = :expenseId LIMIT 1")
    fun getByExpenseIdAsFlow(expenseId: Long): Flow<UpiTransaction?>
}