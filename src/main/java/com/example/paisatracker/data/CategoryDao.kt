package com.example.paisatracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class CategoryWithTotal(
    @Embedded
    val category: Category,
    val totalAmount: Double,
    val expenseCount: Int,
    val latestExpenseTime: Long? = null
)

@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategory(category: Category): Long

    @Update
    suspend fun updateCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)

    @Query("SELECT * FROM categories WHERE projectId = :projectId ORDER BY name ASC")
    fun getCategoriesForProject(projectId: Long): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE id = :categoryId")
    fun getCategoryById(categoryId: Long): Flow<Category>

    @Query("SELECT * FROM categories WHERE name = :name AND projectId = :projectId LIMIT 1")
    suspend fun getCategoryByName(name: String, projectId: Long): Category?

    @Query("""
    SELECT 
        c.*, 
        COALESCE(SUM(e.amount), 0.0) AS totalAmount, 
        COUNT(e.id) AS expenseCount,
        MAX(e.date) AS latestExpenseTime
    FROM categories c
    LEFT JOIN expenses e ON c.id = e.categoryId
    WHERE c.projectId = :projectId
    GROUP BY c.id
    ORDER BY c.name ASC
""")
    fun getCategoriesWithTotalForProject(projectId: Long): Flow<List<CategoryWithTotal>>



}
