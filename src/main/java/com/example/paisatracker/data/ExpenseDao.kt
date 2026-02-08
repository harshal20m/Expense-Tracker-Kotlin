package com.example.paisatracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// New data class for exporting
data class ExpenseExport(
    val projectName: String,
    val categoryName: String,
    val expenseDescription: String,
    val expenseAmount: Double,
    val expenseDate: Long
)


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

    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE categoryId = :categoryId ORDER BY date DESC")
    fun getExpensesForCategory(categoryId: Long): Flow<List<Expense>>

    // New query for exporting data for a specific project
    @Query("""
        SELECT p.name as projectName, c.name as categoryName, e.description as expenseDescription, e.amount as expenseAmount, e.date as expenseDate
        FROM expenses e
        INNER JOIN categories c ON e.categoryId = c.id
        INNER JOIN projects p ON c.projectId = p.id
        WHERE p.id = :projectId
        ORDER BY p.name, c.name, e.date
    """)
    suspend fun getExpensesForExport(projectId: Long): List<ExpenseExport>


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


    // Add this query to your ExpenseDao interface

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


}
