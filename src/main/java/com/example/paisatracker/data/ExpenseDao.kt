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

}
