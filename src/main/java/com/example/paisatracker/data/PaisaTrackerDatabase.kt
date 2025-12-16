package com.example.paisatracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Project::class, Category::class, Expense::class,  Asset::class  ], version = 2, exportSchema = false)
abstract class PaisaTrackerDatabase : RoomDatabase() {

    abstract fun projectDao(): ProjectDao
    abstract fun categoryDao(): CategoryDao
    abstract fun expenseDao(): ExpenseDao

    abstract fun assetDao(): AssetDao

    companion object {
        @Volatile
        private var INSTANCE: PaisaTrackerDatabase? = null

        fun getDatabase(context: Context): PaisaTrackerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PaisaTrackerDatabase::class.java,
                    "paisa_tracker_database_v1_2"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
