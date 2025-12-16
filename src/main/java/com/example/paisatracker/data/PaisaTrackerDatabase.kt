package com.example.paisatracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Project::class,
        Category::class,
        Expense::class,
        Asset::class,
        BackupMetadata::class
    ],
    version = 2,
    exportSchema = false
)
abstract class PaisaTrackerDatabase : RoomDatabase() {

    abstract fun projectDao(): ProjectDao
    abstract fun categoryDao(): CategoryDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun assetDao(): AssetDao

    abstract fun backupDao(): BackupDao

    companion object {
        @Volatile
        private var INSTANCE: PaisaTrackerDatabase? = null

        fun getDatabase(context: Context): PaisaTrackerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PaisaTrackerDatabase::class.java,
                    "paisa_tracker_database_v1_2"  // Keep your current name
                )
                    .addMigrations(MIGRATION_1_2)  // Add proper migration
                    // Only fallback if migration fails (as backup)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * Migration from version 1 to 2
         * Adds the backup_metadata table for tracking backups
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create backup_metadata table
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS backup_metadata (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        fileName TEXT NOT NULL,
                        filePath TEXT NOT NULL,
                        fileSize INTEGER NOT NULL,
                        timestamp INTEGER NOT NULL,
                        projectCount INTEGER NOT NULL,
                        categoryCount INTEGER NOT NULL,
                        expenseCount INTEGER NOT NULL,
                        totalAmount REAL NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }
    }
}