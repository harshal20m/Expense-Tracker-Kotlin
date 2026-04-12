package com.example.paisatracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SalaryRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: SalaryRecord): Long

    @Update
    suspend fun update(record: SalaryRecord)

    @Delete
    suspend fun delete(record: SalaryRecord)

    /** Current month's salary record (most recently added for this month/year) */
    @Query("""
        SELECT * FROM salary_records 
        WHERE month = :month AND year = :year 
        ORDER BY receivedAt DESC LIMIT 1
    """)
    fun getCurrentMonthRecord(month: Int, year: Int): Flow<SalaryRecord?>

    /** All records ordered newest first — for history view */
    @Query("SELECT * FROM salary_records ORDER BY year DESC, month DESC, receivedAt DESC")
    fun getAllRecords(): Flow<List<SalaryRecord>>

    /** Sum of all expenses since [startTimestamp] — used to compute balance */
    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM expenses WHERE date >= :startTimestamp")
    fun getTotalSpentSince(startTimestamp: Long): Flow<Double>

    /** Sum of expenses per category since a timestamp — for category breakdown */
    @Query("""
        SELECT c.name as categoryName, c.emoji as categoryEmoji, 
               COALESCE(SUM(e.amount), 0.0) as total
        FROM expenses e 
        JOIN categories c ON e.categoryId = c.id
        WHERE e.date >= :startTimestamp
        GROUP BY e.categoryId
        ORDER BY total DESC
        LIMIT 6
    """)
    fun getCategoryBreakdownSince(startTimestamp: Long): Flow<List<CategorySpend>>
}

/** Lightweight projection for category spend breakdown */
data class CategorySpend(
    val categoryName: String,
    val categoryEmoji: String,
    val total: Double
)
