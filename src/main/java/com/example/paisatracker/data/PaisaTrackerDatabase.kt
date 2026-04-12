package com.example.paisatracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Project::class,
        Category::class,
        Expense::class,
        Asset::class,
        BackupMetadata::class,
        Budget::class,
        FlapData::class,
        SalaryRecord::class
    ],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class PaisaTrackerDatabase : RoomDatabase() {

    abstract fun projectDao(): ProjectDao
    abstract fun categoryDao(): CategoryDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun assetDao(): AssetDao
    abstract fun backupDao(): BackupDao
    abstract fun budgetDao(): BudgetDao
    abstract fun flapDao(): FlapDao
    abstract fun salaryRecordDao(): SalaryRecordDao





    companion object {
        @Volatile
        private var INSTANCE: PaisaTrackerDatabase? = null

        fun getDatabase(context: Context): PaisaTrackerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PaisaTrackerDatabase::class.java,
                    "paisa_tracker_database_v1_2"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_4_5, MIGRATION_5_6)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
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

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 1. Update Assets table
                database.execSQL("ALTER TABLE assets ADD COLUMN expenseId INTEGER DEFAULT NULL")

                // 2. Update Categories table
                database.execSQL("ALTER TABLE categories ADD COLUMN emoji TEXT NOT NULL DEFAULT '▶️'")

                // 3. Update Expenses table
                database.execSQL("ALTER TABLE expenses ADD COLUMN paymentMethod TEXT DEFAULT NULL")
                database.execSQL("ALTER TABLE expenses ADD COLUMN paymentIcon TEXT DEFAULT NULL")

                // 4. Create Budgets table
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS budgets (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        emoji TEXT NOT NULL DEFAULT '💰',
                        limitAmount REAL NOT NULL,
                        period TEXT NOT NULL,
                        categoryId INTEGER,
                        projectId INTEGER,
                        createdAt INTEGER NOT NULL,
                        isActive INTEGER NOT NULL DEFAULT 1
                    )
                    """.trimIndent()
                )

                            database.execSQL("""
                CREATE TABLE IF NOT EXISTS `flap_data` (
                    `id` INTEGER NOT NULL PRIMARY KEY,
                    `notesText` TEXT NOT NULL DEFAULT '',
                    `calcHistorySerialized` TEXT NOT NULL DEFAULT '',
                    `calcDisplay` TEXT NOT NULL DEFAULT '0',
                    `calcExpression` TEXT NOT NULL DEFAULT '',
                    `lastUpdatedAt` INTEGER NOT NULL
                )
            """.trimIndent())
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `upi_transactions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `expenseId` INTEGER NOT NULL,
                        `vpa` TEXT NOT NULL,
                        `payeeName` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `transactionNote` TEXT NOT NULL DEFAULT '',
                        `status` TEXT NOT NULL DEFAULT 'PENDING',
                        `transactionId` TEXT,
                        `responseCode` TEXT,
                        `rawResponse` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        FOREIGN KEY(`expenseId`) REFERENCES `expenses`(`id`) ON DELETE CASCADE
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_upi_transactions_expenseId` ON `upi_transactions` (`expenseId`)")
            }
        }
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
               CREATE TABLE IF NOT EXISTS `pending_transactions` (
                   `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                   `amount` REAL NOT NULL,
                   `payeeName` TEXT NOT NULL,
                   `payeeVpa` TEXT NOT NULL DEFAULT '',
                   `utrNumber` TEXT NOT NULL DEFAULT '',
                   `sourceApp` TEXT NOT NULL DEFAULT 'Unknown',
                   `status` TEXT NOT NULL DEFAULT 'Success',
                   `transactionDate` TEXT NOT NULL DEFAULT '',
                   `categoryId` INTEGER,
                   `projectId` INTEGER,
                   `note` TEXT NOT NULL DEFAULT '',
                   `rawNotificationText` TEXT NOT NULL DEFAULT '',
                   `capturedAt` INTEGER NOT NULL,
                   `isReviewed` INTEGER NOT NULL DEFAULT 0
               )
           """
                )
                db.execSQL(
                    """
               CREATE TABLE IF NOT EXISTS `salary_records` (
                   `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                   `amount` REAL NOT NULL,
                   `receivedAt` INTEGER NOT NULL,
                   `month` INTEGER NOT NULL,
                   `year` INTEGER NOT NULL,
                   `note` TEXT NOT NULL DEFAULT '',
                   `currency` TEXT NOT NULL DEFAULT 'INR'
               )
           """
                )
            }
        }
}
}