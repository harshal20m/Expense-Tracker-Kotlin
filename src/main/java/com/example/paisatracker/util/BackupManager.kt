package com.example.paisatracker.util

import android.content.Context
import android.net.Uri
import com.example.paisatracker.data.BackupMetadata
import com.example.paisatracker.data.PaisaTrackerDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupManager(private val context: Context) {

    private val database = PaisaTrackerDatabase.getDatabase(context)

    /**
     * Create a full database backup (ZIP file containing database + assets)
     * Returns the BackupMetadata if successful, null otherwise
     */
    suspend fun createFullBackup(destinationUri: Uri): BackupMetadata? = withContext(Dispatchers.IO) {
        try {
            // Get database file - MUST match your actual database name
            val dbPath = context.getDatabasePath("paisa_tracker_database_v1_2").absolutePath
            val dbFile = File(dbPath)

            if (!dbFile.exists()) {
                return@withContext null
            }

            // Get asset files directory
            val assetDir = File(context.filesDir, "expense_assets")

            // Get current database instance for stats (don't close it)
            val db = PaisaTrackerDatabase.getDatabase(context)
            val projectCount = db.projectDao().getProjectCount()
            val categoryCount = db.categoryDao().getCategoryCount()
            val expenseCount = db.expenseDao().getExpenseCount()
            val totalAmount = db.expenseDao().getTotalAmount() ?: 0.0

            // Create ZIP file
            context.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
                ZipOutputStream(outputStream).use { zipOut ->

                    // Checkpoint WAL file before copying (ensures data is written to main db file)
                    try {
                        db.query("PRAGMA wal_checkpoint(FULL)", null)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    // Add database file to ZIP
                    FileInputStream(dbFile).use { fis ->
                        zipOut.putNextEntry(ZipEntry("database.db"))
                        fis.copyTo(zipOut)
                        zipOut.closeEntry()
                    }

                    // Add WAL file if exists
                    val walFile = File("${dbFile.absolutePath}-wal")
                    if (walFile.exists()) {
                        FileInputStream(walFile).use { fis ->
                            zipOut.putNextEntry(ZipEntry("database.db-wal"))
                            fis.copyTo(zipOut)
                            zipOut.closeEntry()
                        }
                    }

                    // Add SHM file if exists
                    val shmFile = File("${dbFile.absolutePath}-shm")
                    if (shmFile.exists()) {
                        FileInputStream(shmFile).use { fis ->
                            zipOut.putNextEntry(ZipEntry("database.db-shm"))
                            fis.copyTo(zipOut)
                            zipOut.closeEntry()
                        }
                    }

                    // Add asset files to ZIP if directory exists
                    if (assetDir.exists() && assetDir.isDirectory) {
                        assetDir.listFiles()?.forEach { assetFile ->
                            if (assetFile.isFile) {
                                FileInputStream(assetFile).use { fis ->
                                    zipOut.putNextEntry(ZipEntry("assets/${assetFile.name}"))
                                    fis.copyTo(zipOut)
                                    zipOut.closeEntry()
                                }
                            }
                        }
                    }
                }
            }

            // Get file size
            val fileSize = getFileSize(destinationUri)

            // Create metadata
            val timestamp = System.currentTimeMillis()
            val fileName = "PaisaTracker_Backup_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date(timestamp))}.zip"

            val metadata = BackupMetadata(
                fileName = fileName,
                filePath = destinationUri.toString(),
                fileSize = fileSize,
                timestamp = timestamp,
                projectCount = projectCount,
                categoryCount = categoryCount,
                expenseCount = expenseCount,
                totalAmount = totalAmount
            )

            // Save metadata to database
            db.backupDao().insertBackup(metadata)

            metadata

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Restore database from backup ZIP file
     * Returns true if successful, false otherwise
     */
    suspend fun restoreFromBackup(sourceUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            // Close current database
            database.close()

            val dbPath = context.getDatabasePath("paisa_tracker_database_v1_2").absolutePath
            val dbFile = File(dbPath)
            val assetDir = File(context.filesDir, "expense_assets")

            // Create asset directory if it doesn't exist
            if (!assetDir.exists()) {
                assetDir.mkdirs()
            }

            // Extract ZIP file
            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zipIn ->
                    var entry: ZipEntry? = zipIn.nextEntry

                    while (entry != null) {
                        when {
                            entry.name == "database.db" -> {
                                // Restore database file
                                FileOutputStream(dbFile).use { fos ->
                                    zipIn.copyTo(fos)
                                }
                            }
                            entry.name == "database.db-wal" -> {
                                // Restore WAL file
                                val walFile = File("${dbFile.absolutePath}-wal")
                                FileOutputStream(walFile).use { fos ->
                                    zipIn.copyTo(fos)
                                }
                            }
                            entry.name == "database.db-shm" -> {
                                // Restore SHM file
                                val shmFile = File("${dbFile.absolutePath}-shm")
                                FileOutputStream(shmFile).use { fos ->
                                    zipIn.copyTo(fos)
                                }
                            }
                            entry.name.startsWith("assets/") -> {
                                // Restore asset file
                                val fileName = entry.name.substringAfter("assets/")
                                val assetFile = File(assetDir, fileName)
                                FileOutputStream(assetFile).use { fos ->
                                    zipIn.copyTo(fos)
                                }
                            }
                        }

                        zipIn.closeEntry()
                        entry = zipIn.nextEntry
                    }
                }
            }

            // Reopen database with restored data
            PaisaTrackerDatabase.getDatabase(context)
            true

        } catch (e: Exception) {
            e.printStackTrace()
            // Reopen database
            PaisaTrackerDatabase.getDatabase(context)
            false
        }
    }

    /**
     * Delete backup file from storage
     */
    suspend fun deleteBackupFile(backup: BackupMetadata): Boolean =
        withContext(Dispatchers.IO) {
            val uri = Uri.parse(backup.filePath)

            try {
                // STEP 1: Check if file exists
                val fileExists = try {
                    context.contentResolver.openFileDescriptor(uri, "r")?.use {
                        true
                    } ?: false
                } catch (e: Exception) {
                    false
                }

                // STEP 2: Delete file ONLY if it exists
                if (fileExists) {
                    try {
                        context.contentResolver.delete(uri, null, null)
                    } catch (e: Exception) {
                        // Ignore — file may already be gone
                        e.printStackTrace()
                    }
                }

                // STEP 3: Always delete DB record
                database.backupDao().deleteBackup(backup)

                true
            } catch (e: Exception) {
                e.printStackTrace()

                // LAST RESORT: still remove DB entry to avoid stuck history
                try {
                    database.backupDao().deleteBackup(backup)
                } catch (_: Exception) {}

                false
            }
        }


    /**
     * Get file size from URI
     */
    private fun getFileSize(uri: Uri): Long {
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                pfd.statSize
            } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Format file size for display
     */
    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
        }
    }
}