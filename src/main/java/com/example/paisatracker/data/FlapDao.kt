package com.example.paisatracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FlapDao {

    /** Observe the single flap row — emits on every update */
    @Query("SELECT * FROM flap_data WHERE id = 1")
    fun getFlapData(): Flow<FlapData?>

    /** Upsert — inserts row 1 or replaces it if it exists */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFlapData(flapData: FlapData)

    /** Convenience: read once (not reactive) */
    @Query("SELECT * FROM flap_data WHERE id = 1")
    suspend fun getFlapDataOnce(): FlapData?
}