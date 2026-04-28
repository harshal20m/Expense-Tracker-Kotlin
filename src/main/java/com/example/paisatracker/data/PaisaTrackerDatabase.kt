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
        SalaryRecord::class,
    ],
    version = 9,
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
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_4_5,
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9
                    )
                    .fallbackToDestructiveMigration(false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
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
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE assets ADD COLUMN expenseId INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE categories ADD COLUMN emoji TEXT NOT NULL DEFAULT '▶️'")
                db.execSQL("ALTER TABLE expenses ADD COLUMN paymentMethod TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE expenses ADD COLUMN paymentIcon TEXT DEFAULT NULL")
                db.execSQL(
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
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `flap_data` (
                        `id` INTEGER NOT NULL PRIMARY KEY,
                        `notesText` TEXT NOT NULL DEFAULT '',
                        `calcHistorySerialized` TEXT NOT NULL DEFAULT '',
                        `calcDisplay` TEXT NOT NULL DEFAULT '0',
                        `calcExpression` TEXT NOT NULL DEFAULT '',
                        `lastUpdatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
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
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_upi_transactions_expenseId` ON `upi_transactions` (`expenseId`)")
            }
        }
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `upi_transactions`")
                db.execSQL("DROP TABLE IF EXISTS `pending_transactions`")
            }
        }
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE budgets ADD COLUMN trackingStartAt INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "UPDATE budgets SET trackingStartAt = createdAt WHERE trackingStartAt = 0"
                )
            }
        }
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE projects ADD COLUMN isCompleted INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}