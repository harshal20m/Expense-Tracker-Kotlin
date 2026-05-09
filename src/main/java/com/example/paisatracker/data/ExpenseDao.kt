package com.example.paisatracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// Add this data class to your data package
data class RecentExpense(
    val id: Long,
    val amount: Double,
    val description: String,
    val date: Long,
    val paymentMethod: String?,
    val paymentIcon: String?,
    val projectId: Long,
    val projectName: String,
    val projectEmoji: String,
    val categoryId: Long,
    val categoryName: String,
    val categoryEmoji: String
)

@Dao
interface ExpenseDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExpense(expense: Expense)

    @Insert
    suspend fun insert(expense: Expense): Long


    @Query("SELECT * FROM expenses WHERE id = :id LIMIT 1")
    fun getExpenseById(id: Long): Flow<Expense?>
    
    @Query("SELECT * FROM expenses WHERE id = :id LIMIT 1")
    suspend fun getExpenseByIdOnce(id: Long): Expense?

    @Update
    suspend fun updateExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)


     @Query("DELETE FROM expenses WHERE id = :id")
     suspend fun deleteById(id: Long)

    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE categoryId = :categoryId ORDER BY date DESC")
    fun getExpensesForCategory(categoryId: Long): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE categoryId = :categoryId")
    suspend fun getExpensesForCategoryList(categoryId: Long): List<Expense>

    @Query("""
    SELECT
        p.name AS projectName,
        p.emoji AS projectEmoji,
        c.name AS categoryName,
        c.emoji AS categoryEmoji,
        e.description AS description,
        e.amount AS amount,
        e.date AS date,
        e.paymentMethod AS paymentMethod,
        e.paymentIcon AS paymentMethodEmoji
    FROM expenses e
    INNER JOIN categories c ON e.categoryId = c.id
    INNER JOIN projects p ON c.projectId = p.id
    WHERE (:projectId IS NULL OR p.id = :projectId)
    ORDER BY p.name, c.name, e.date
""")
    suspend fun getExportRows(projectId: Long? = null): List<ExportRow>

    @Query("SELECT COUNT(*) FROM expenses")
    suspend fun getExpenseCount(): Int

    @Query("SELECT SUM(amount) FROM expenses")
    suspend fun getTotalAmount(): Double?

    @Query("""
    SELECT
        e.id as id,
        e.amount as amount,
        e.description as description,
        e.date as date,
        e.paymentMethod as paymentMethod,
        e.paymentIcon as paymentIcon,
        c.projectId as projectId,
        p.name as projectName,
        p.emoji as projectEmoji,
        c.id as categoryId,
        c.name as categoryName,
        c.emoji as categoryEmoji
    FROM expenses e
    INNER JOIN categories c ON e.categoryId = c.id
    INNER JOIN projects p ON c.projectId = p.id
    ORDER BY e.date DESC
    LIMIT :limit
""")
    fun getRecentExpensesWithDetails(limit: Int): Flow<List<RecentExpense>>

    @Query("""
    SELECT
        e.id as id,
        e.amount as amount,
        e.description as description,
        e.date as date,
        e.paymentMethod as paymentMethod,
        e.paymentIcon as paymentIcon,
        c.projectId as projectId,
        p.name as projectName,
        p.emoji as projectEmoji,
        c.id as categoryId,
        c.name as categoryName,
        c.emoji as categoryEmoji
    FROM expenses e
    INNER JOIN categories c ON e.categoryId = c.id
    INNER JOIN projects p ON c.projectId = p.id
    ORDER BY e.date DESC
""")
    fun getAllExpensesWithDetails(): Flow<List<RecentExpense>>

    // New search queries

    @Query("""
        SELECT
            e.id as id,
            e.amount as amount,
            e.description as description,
            e.date as date,
            e.paymentMethod as paymentMethod,
            e.paymentIcon as paymentIcon,
            c.projectId as projectId,
            p.name as projectName,
            p.emoji as projectEmoji,
            c.id as categoryId,
            c.name as categoryName,
            c.emoji as categoryEmoji
        FROM expenses e
        INNER JOIN categories c ON e.categoryId = c.id
        INNER JOIN projects p ON c.projectId = p.id
        WHERE (:query IS NULL OR e.description LIKE '%' || :query || '%')
        AND (:projectId IS NULL OR p.id = :projectId)
        ORDER BY e.date DESC
    """)
    fun searchExpensesByDescription(query: String?, projectId: Long?): Flow<List<RecentExpense>>

    @Query("""
        SELECT
            e.id as id,
            e.amount as amount,
            e.description as description,
            e.date as date,
            e.paymentMethod as paymentMethod,
            e.paymentIcon as paymentIcon,
            c.projectId as projectId,
            p.name as projectName,
            p.emoji as projectEmoji,
            c.id as categoryId,
            c.name as categoryName,
            c.emoji as categoryEmoji
        FROM expenses e
        INNER JOIN categories c ON e.categoryId = c.id
        INNER JOIN projects p ON c.projectId = p.id
        WHERE e.bankAccountId = :bankAccountId
        ORDER BY e.date DESC
    """)
    fun getExpensesByBankAccount(bankAccountId: Long): Flow<List<RecentExpense>>

    @Query("""
        SELECT
            e.id as id,
            e.amount as amount,
            e.description as description,
            e.date as date,
            e.paymentMethod as paymentMethod,
            e.paymentIcon as paymentIcon,
            c.projectId as projectId,
            p.name as projectName,
            p.emoji as projectEmoji,
            c.id as categoryId,
            c.name as categoryName,
            c.emoji as categoryEmoji
        FROM expenses e
        INNER JOIN categories c ON e.categoryId = c.id
        INNER JOIN projects p ON c.projectId = p.id
        WHERE (:minAmount IS NULL OR e.amount >= :minAmount)
        AND (:maxAmount IS NULL OR e.amount <= :maxAmount)
        AND (:projectId IS NULL OR p.id = :projectId)
        ORDER BY e.date DESC
    """)
    fun searchExpensesByAmount(minAmount: Double?, maxAmount: Double?, projectId: Long?): Flow<List<RecentExpense>>

    @Query("""
        SELECT
            e.id as id,
            e.amount as amount,
            e.description as description,
            e.date as date,
            e.paymentMethod as paymentMethod,
            e.paymentIcon as paymentIcon,
            c.projectId as projectId,
            p.name as projectName,
            p.emoji as projectEmoji,
            c.id as categoryId,
            c.name as categoryName,
            c.emoji as categoryEmoji
        FROM expenses e
        INNER JOIN categories c ON e.categoryId = c.id
        INNER JOIN projects p ON c.projectId = p.id
        WHERE (:startDate IS NULL OR e.date >= :startDate)
        AND (:endDate IS NULL OR e.date <= :endDate)
        AND (:projectId IS NULL OR p.id = :projectId)
        ORDER BY e.date DESC
    """)
    fun searchExpensesByDateRange(startDate: Long?, endDate: Long?, projectId: Long?): Flow<List<RecentExpense>>

    // ============================================================================
    // NEW ANALYTICS QUERIES - Added for Sprint 2 (Time-based Analytics)
    // These queries support the new TimePeriod and analytics features
    // ============================================================================

    /**
     * Get expenses within a specific date range.
     * Used for filtering expenses by time period (week, month, year, custom).
     *
     * @param startDate Start timestamp (inclusive)
     * @param endDate End timestamp (inclusive)
     * @return Flow of expenses in the date range
     */
    @Query("SELECT * FROM expenses WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getExpensesByDateRange(startDate: Long, endDate: Long): Flow<List<Expense>>

    /**
     * Get monthly aggregated totals for the last N months.
     * Returns month in format "YYYY-MM", total amount, and count of expenses.
     *
     * @param months Number of months to retrieve (default 12)
     * @return Flow of monthly totals
     */
    @Query("""
        SELECT 
            strftime('%Y-%m', date/1000, 'unixepoch') as month,
            SUM(amount) as total,
            COUNT(*) as count
        FROM expenses 
        GROUP BY month 
        ORDER BY month DESC 
        LIMIT :months
    """)
    fun getMonthlyTotals(months: Int = 12): Flow<List<com.example.paisatracker.domain.models.MonthlyTotal>>

    /**
     * Get yearly aggregated totals for all years with expenses.
     * Returns year, total amount, and count of expenses.
     *
     * @return Flow of yearly totals
     */
    @Query("""
        SELECT 
            strftime('%Y', date/1000, 'unixepoch') as year,
            SUM(amount) as total,
            COUNT(*) as count
        FROM expenses 
        GROUP BY year 
        ORDER BY year DESC
    """)
    fun getYearlyTotals(): Flow<List<com.example.paisatracker.domain.models.YearlyTotal>>

    /**
     * Get category-wise spending for a specific date range.
     * Includes category details and aggregated amounts.
     *
     * @param startDate Start timestamp (inclusive)
     * @param endDate End timestamp (inclusive)
     * @return Flow of category spending data
     */
    @Query("""
        SELECT 
            c.id as categoryId,
            c.name as categoryName,
            c.emoji as categoryIcon,
            SUM(e.amount) as total,
            COUNT(e.id) as count,
            0.0 as percentage
        FROM expenses e
        INNER JOIN categories c ON e.categoryId = c.id
        WHERE e.date BETWEEN :startDate AND :endDate
        GROUP BY c.id, c.name, c.emoji
        ORDER BY total DESC
    """)
    fun getCategorySpendingByDateRange(
        startDate: Long,
        endDate: Long
    ): Flow<List<com.example.paisatracker.domain.models.CategorySpending>>

    /**
     * Get total spending for a specific date range.
     * Used for calculating overall spending in a time period.
     *
     * @param startDate Start timestamp (inclusive)
     * @param endDate End timestamp (inclusive)
     * @return Total amount spent, or 0.0 if no expenses
     */
    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM expenses WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getTotalByDateRange(startDate: Long, endDate: Long): Double

    /**
     * Get expense count for a specific date range.
     * Used for analytics and statistics.
     *
     * @param startDate Start timestamp (inclusive)
     * @param endDate End timestamp (inclusive)
     * @return Number of expenses in the date range
     */
    @Query("SELECT COUNT(*) FROM expenses WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getCountByDateRange(startDate: Long, endDate: Long): Int

    /**
     * Get monthly totals for a specific year.
     * Returns all 12 months with their totals (0 if no expenses).
     *
     * @param year Year to query (e.g., 2024)
     * @return Flow of monthly totals for the year
     */
    @Query("""
        SELECT 
            strftime('%Y-%m', date/1000, 'unixepoch') as month,
            SUM(amount) as total,
            COUNT(*) as count
        FROM expenses 
        WHERE strftime('%Y', date/1000, 'unixepoch') = :year
        GROUP BY month 
        ORDER BY month ASC
    """)
    fun getMonthlyTotalsForYear(year: String): Flow<List<com.example.paisatracker.domain.models.MonthlyTotal>>

    /**
     * Get top N categories by spending in a date range.
     * Useful for "Top Spending Categories" analytics.
     *
     * @param startDate Start timestamp (inclusive)
     * @param endDate End timestamp (inclusive)
     * @param limit Number of top categories to return
     * @return Flow of top category spending data
     */
    @Query("""
        SELECT 
            c.id as categoryId,
            c.name as categoryName,
            c.emoji as categoryIcon,
            SUM(e.amount) as total,
            COUNT(e.id) as count,
            0.0 as percentage
        FROM expenses e
        INNER JOIN categories c ON e.categoryId = c.id
        WHERE e.date BETWEEN :startDate AND :endDate
        GROUP BY c.id, c.name, c.emoji
        ORDER BY total DESC
        LIMIT :limit
    """)
    fun getTopCategoriesByDateRange(
        startDate: Long,
        endDate: Long,
        limit: Int = 5
    ): Flow<List<com.example.paisatracker.domain.models.CategorySpending>>

    /**
     * Get average daily spending for a date range.
     * Useful for "Daily Average" analytics.
     *
     * @param startDate Start timestamp (inclusive)
     * @param endDate End timestamp (inclusive)
     * @return Average spending per day
     */
    @Query("""
        SELECT 
            COALESCE(SUM(amount) / 
                (CAST(((:endDate - :startDate) / 86400000) AS REAL) + 1), 0.0)
        FROM expenses 
        WHERE date BETWEEN :startDate AND :endDate
    """)
    suspend fun getAverageDailySpending(startDate: Long, endDate: Long): Double

    // ============================================================================
    // BUDGET-SALARY INTEGRATION QUERIES - Added for Sprint 7
    // These queries support budget tracking and financial health calculations
    // ============================================================================

    /**
     * Get total spending for a specific category within a date range.
     * Used for category-specific budget tracking.
     *
     * @param categoryId Category ID to filter by
     * @param startDate Start timestamp (inclusive)
     * @param endDate End timestamp (inclusive)
     * @return Total amount spent in the category
     */
    @Query("""
        SELECT COALESCE(SUM(amount), 0.0)
        FROM expenses
        WHERE categoryId = :categoryId
        AND date BETWEEN :startDate AND :endDate
    """)
    suspend fun getTotalByCategoryAndDateRange(
        categoryId: Long,
        startDate: Long,
        endDate: Long
    ): Double

    /**
     * Get total spending for a specific project within a date range.
     * Used for project-specific budget tracking.
     *
     * @param projectId Project ID to filter by
     * @param startDate Start timestamp (inclusive)
     * @param endDate End timestamp (inclusive)
     * @return Total amount spent in the project
     */
    @Query("""
        SELECT COALESCE(SUM(e.amount), 0.0)
        FROM expenses e
        INNER JOIN categories c ON e.categoryId = c.id
        WHERE c.projectId = :projectId
        AND e.date BETWEEN :startDate AND :endDate
    """)
    suspend fun getTotalByProjectAndDateRange(
        projectId: Long,
        startDate: Long,
        endDate: Long
    ): Double
}