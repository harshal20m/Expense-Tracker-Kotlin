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

    @Query("""
    SELECT
        p.name AS projectName,
        p.emoji AS projectEmoji,
        c.name AS categoryName,
        c.emoji AS categoryEmoji,
        e.description AS description,
        e.amount AS amount,
        e.date AS date,
        e.paymentMethod AS paymentMethod
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

}