package com.example.paisatracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AssetDao {
    // All assets (independent + linked)
    @Query("SELECT * FROM assets ORDER BY timestamp DESC")
    fun getAllAssets(): Flow<List<Asset>>

    // Assets for specific expense
    @Query("SELECT * FROM assets WHERE expenseId = :expenseId ORDER BY timestamp DESC")
    fun getAssetsForExpense(expenseId: Long): Flow<List<Asset>>

    // Independent assets only (no expense link)
    @Query("SELECT * FROM assets WHERE expenseId IS NULL ORDER BY timestamp DESC")
    fun getIndependentAssets(): Flow<List<Asset>>

    @Insert
    suspend fun insertAsset(asset: Asset): Long

    @Delete
    suspend fun deleteAsset(asset: Asset)

    @Query("DELETE FROM assets WHERE expenseId = :expenseId")
    suspend fun deleteAssetsForExpense(expenseId: Long)
}
