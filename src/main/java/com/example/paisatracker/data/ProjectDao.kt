package com.example.paisatracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class CategoryExpense(val categoryName: String, val totalAmount: Double)

@Dao
interface ProjectDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertProject(project: Project): Long

    @Update
    suspend fun updateProject(project: Project)

    @Delete
    suspend fun deleteProject(project: Project)

    @Query("SELECT * FROM projects WHERE isCompleted = 0 ORDER BY name ASC")
    fun getActiveProjects(): Flow<List<Project>>

    @Query("SELECT * FROM projects WHERE isCompleted = 1 ORDER BY name ASC")
    fun getCompletedProjects(): Flow<List<Project>>

    @Query("UPDATE projects SET isCompleted = :isCompleted WHERE id = :projectId")
    suspend fun updateProjectStatus(projectId: Long, isCompleted: Boolean)

    @Query("""
    SELECT p.*, 
           COALESCE(SUM(e.amount), 0) as totalAmount,
           COUNT(DISTINCT c.id) as categoryCount,
           COUNT(DISTINCT e.id) as expenseCount
    FROM projects p
    LEFT JOIN categories c ON c.projectId = p.id
    LEFT JOIN expenses e ON e.categoryId = c.id
    WHERE p.isCompleted = 0
    GROUP BY p.id
    ORDER BY p.lastModified DESC
""")
    fun getActiveProjectsWithTotal(): Flow<List<ProjectWithTotal>>

    @Query("""
    SELECT p.*, 
           COALESCE(SUM(e.amount), 0) as totalAmount,
           COUNT(DISTINCT c.id) as categoryCount,
           COUNT(DISTINCT e.id) as expenseCount
    FROM projects p
    LEFT JOIN categories c ON c.projectId = p.id
    LEFT JOIN expenses e ON e.categoryId = c.id
    WHERE p.isCompleted = 1
    GROUP BY p.id
    ORDER BY p.lastModified DESC
""")
    fun getCompletedProjectsWithTotal(): Flow<List<ProjectWithTotal>>


    @Query("""
        SELECT c.name as categoryName, COALESCE(SUM(e.amount), 0.0) as totalAmount
        FROM categories c
        LEFT JOIN expenses e ON c.id = e.categoryId
        WHERE c.projectId = :projectId
        GROUP BY c.id
        ORDER BY c.name ASC
    """)
    fun getCategoryExpenses(projectId: Long): Flow<List<CategoryExpense>>

    @Query("SELECT * FROM projects WHERE id = :projectId")
    fun getProjectById(projectId: Long): Flow<Project>

    @Query("SELECT * FROM projects WHERE name = :name LIMIT 1")
    suspend fun getProjectByName(name: String): Project?

    @Query("SELECT * FROM projects ORDER BY name ASC")
    suspend fun getAllProjectsList(): List<Project>

    @Query("SELECT COUNT(*) FROM projects")
    suspend fun getProjectCount(): Int
}