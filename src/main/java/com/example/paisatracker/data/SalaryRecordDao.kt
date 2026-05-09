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

    // ══════════════════════════════════════════════════════════════════════════
    // MULTI-SALARY QUERIES
    // ══════════════════════════════════════════════════════════════════════════

    /** Get ALL active salaries for current month (supports multiple income sources) */
    @Query("""
        SELECT * FROM salary_records
        WHERE month = :month AND year = :year AND isActive = 1
        ORDER BY receivedAt DESC
    """)
    fun getCurrentMonthSalaries(month: Int, year: Int): Flow<List<SalaryRecord>>

    /** Get salaries for a specific account */
    @Query("""
        SELECT * FROM salary_records
        WHERE linkedAccountId = :accountId AND month = :month AND year = :year AND isActive = 1
        ORDER BY receivedAt DESC
    """)
    fun getSalariesForAccount(accountId: Long, month: Int, year: Int): Flow<List<SalaryRecord>>

    /** Get total income for current month (sum of all active salaries) */
    @Query("""
        SELECT COALESCE(SUM(amount), 0.0)
        FROM salary_records
        WHERE month = :month AND year = :year AND isActive = 1
    """)
    fun getTotalMonthlyIncome(month: Int, year: Int): Flow<Double>

    /** Get all recurring salaries (for auto-generation) */
    @Query("""
        SELECT * FROM salary_records
        WHERE isRecurring = 1 AND isActive = 1
        GROUP BY linkedAccountId, sourceType
        HAVING MAX(year * 12 + month)
        ORDER BY receivedAt DESC
    """)
    suspend fun getActiveRecurringSalaries(): List<SalaryRecord>

    /** Check if a salary already exists for account and source type in current month */
    @Query("""
        SELECT COUNT(*) > 0
        FROM salary_records
        WHERE month = :month AND year = :year
        AND linkedAccountId = :accountId
        AND sourceType = :sourceType
        AND isActive = 1
    """)
    suspend fun salaryExistsForAccountAndType(
        month: Int,
        year: Int,
        accountId: Long,
        sourceType: String
    ): Boolean

    // ══════════════════════════════════════════════════════════════════════════
    // LEGACY QUERIES (for backward compatibility)
    // ══════════════════════════════════════════════════════════════════════════

    /** Current month's salary record (most recently added for this month/year)
     *  @deprecated Use getCurrentMonthSalaries() for multi-salary support */
    @Query("""
        SELECT * FROM salary_records
        WHERE month = :month AND year = :year AND isActive = 1
        ORDER BY receivedAt DESC LIMIT 1
    """)
    fun getCurrentMonthRecord(month: Int, year: Int): Flow<SalaryRecord?>

    @Query("""
        SELECT * FROM salary_records
        WHERE month = :month AND year = :year AND isActive = 1
        ORDER BY receivedAt DESC LIMIT 1
    """)
    suspend fun getCurrentMonthRecordOnce(month: Int, year: Int): SalaryRecord?

    @Query("""
        SELECT * FROM salary_records
        WHERE isRecurring = 1 AND isActive = 1
        ORDER BY year DESC, month DESC, receivedAt DESC
        LIMIT 1
    """)
    suspend fun getLatestRecurringRecord(): SalaryRecord?

    // ══════════════════════════════════════════════════════════════════════════
    // COMMON QUERIES
    // ══════════════════════════════════════════════════════════════════════════

    /** All records ordered newest first — for history view */
    @Query("SELECT * FROM salary_records WHERE isActive = 1 ORDER BY year DESC, month DESC, receivedAt DESC")
    fun getAllRecords(): Flow<List<SalaryRecord>>

    /** Sum of all expenses since [startTimestamp] — used to compute balance
     *  Only includes expenses from projects where includeInSalary = true */
    @Query("""
        SELECT COALESCE(SUM(e.amount), 0.0)
        FROM expenses e
        JOIN categories c ON e.categoryId = c.id
        JOIN projects p ON c.projectId = p.id
        WHERE e.date >= :startTimestamp AND p.includeInSalary = 1
    """)
    fun getTotalSpentSince(startTimestamp: Long): Flow<Double>

    /** Sum of expenses per category since a timestamp — for category breakdown
     *  Only includes expenses from projects where includeInSalary = true */
    @Query("""
        SELECT c.name as categoryName, c.emoji as categoryEmoji,
               COALESCE(SUM(e.amount), 0.0) as total
        FROM expenses e
        JOIN categories c ON e.categoryId = c.id
        JOIN projects p ON c.projectId = p.id
        WHERE e.date >= :startTimestamp AND p.includeInSalary = 1
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
