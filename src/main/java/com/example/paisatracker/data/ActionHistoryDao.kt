package com.example.paisatracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ActionHistoryDao {
    @Insert
    suspend fun insertAction(action: ActionHistory): Long

    @Query("SELECT * FROM action_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<ActionHistory>>

    @Query("SELECT * FROM action_history ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestAction(): ActionHistory?

    @Delete
    suspend fun deleteAction(action: ActionHistory)

    @Query("DELETE FROM action_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM action_history")
    suspend fun clearHistory()
}
