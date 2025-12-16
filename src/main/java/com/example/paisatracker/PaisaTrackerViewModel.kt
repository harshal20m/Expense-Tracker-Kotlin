package com.example.paisatracker

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.paisatracker.data.Asset
import com.example.paisatracker.data.BackupMetadata
import com.example.paisatracker.data.Category
import com.example.paisatracker.data.CategoryExpense
import com.example.paisatracker.data.CategoryWithTotal
import com.example.paisatracker.data.Expense
import com.example.paisatracker.data.PaisaTrackerRepository
import com.example.paisatracker.data.Project
import com.example.paisatracker.data.ProjectWithTotal
import com.opencsv.CSVReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PaisaTrackerViewModel(private val repository: PaisaTrackerRepository) : ViewModel() {





    //-----------------Backup----------------
    fun getRecentBackups(): Flow<List<BackupMetadata>> {
        return repository.getRecentBackups()
    }

    fun getAllBackups(): Flow<List<BackupMetadata>> {
        return repository.getAllBackups()
    }

    suspend fun insertBackup(backup: BackupMetadata): Long {
        return repository.insertBackup(backup)
    }

    suspend fun deleteBackup(backup: BackupMetadata) {
        repository.deleteBackup(backup)
    }

    suspend fun getProjectCount(): Int {
        return repository.getProjectCount()
    }

    suspend fun getCategoryCount(): Int {
        return repository.getCategoryCount()
    }

    suspend fun getExpenseCount(): Int {
        return repository.getExpenseCount()
    }

    suspend fun getTotalAmount(): Double {
        return repository.getTotalAmount()
    }



    // ---------------- Assets ----------------

    fun getAllAssets() = repository.getAllAssets()
    fun getAssetsForExpense(expenseId: Long) = repository.getAssetsForExpense(expenseId)
    fun getIndependentAssets() = repository.getIndependentAssets()

    suspend fun insertAsset(asset: Asset) = repository.insertAsset(asset)

    fun deleteAsset(asset: Asset) {
        viewModelScope.launch { repository.deleteAsset(asset) }
    }

    private fun saveImageToInternalStorage(
        context: Context,
        uri: Uri
    ): String? {
        return try {
            val imagesDir = File(context.filesDir, "images")
            if (!imagesDir.exists()) {
                imagesDir.mkdirs()
            }

            val fileName = "IMG_${System.currentTimeMillis()}.jpg"
            val destFile = File(imagesDir, fileName)

            val input = context.contentResolver.openInputStream(uri) ?: return null

            input.use { ins ->
                destFile.outputStream().use { outs ->
                    ins.copyTo(outs)
                }
            }

            destFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun saveAssetInternal(
        context: Context,
        uri: Uri,
        title: String,
        description: String,
        expenseId: Long?
    ) {
        val path = saveImageToInternalStorage(context, uri) ?: return

        val asset = Asset(
            imagePath = path,
            title = title,
            description = description,
            expenseId = expenseId
        )
        repository.insertAsset(asset)
    }

    fun addIndependentAsset(
        context: Context,
        uri: Uri,
        title: String,
        description: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            saveAssetInternal(
                context = context,
                uri = uri,
                title = title,
                description = description,
                expenseId = null
            )
        }
    }

    fun addLinkedAsset(
        context: Context,
        uri: Uri,
        title: String,
        description: String,
        expenseId: Long
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            saveAssetInternal(
                context = context,
                uri = uri,
                title = title,
                description = description,
                expenseId = expenseId
            )
        }
    }

    // ---------------- Projects / Categories / Expenses basic APIs ----------------

    fun getAllProjectsWithTotal(): Flow<List<ProjectWithTotal>> =
        repository.getAllProjectsWithTotal()

    fun getCategoryExpenses(projectId: Long): Flow<List<CategoryExpense>> =
        repository.getCategoryExpenses(projectId)

    fun getExpenseById(id: Long): Flow<Expense?> =
        repository.getExpenseById(id)

    fun getCategoriesWithTotalForProject(projectId: Long): Flow<List<CategoryWithTotal>> =
        repository.getCategoriesWithTotalForProject(projectId)

    fun getAllExpenses(): Flow<List<Expense>> =
        repository.getAllExpenses()

    fun getProjectById(projectId: Long): Flow<Project> =
        repository.getProjectById(projectId)

    fun getCategoryById(categoryId: Long): Flow<Category> =
        repository.getCategoryById(categoryId)

    fun getCategoriesForProject(projectId: Long): Flow<List<Category>> =
        repository.getCategoriesForProject(projectId)

    fun insertProject(project: Project) {
        viewModelScope.launch { repository.insertProject(project) }
    }

    fun updateProject(project: Project) {
        viewModelScope.launch { repository.updateProject(project) }
    }

    fun deleteProject(project: Project) {
        viewModelScope.launch { repository.deleteProject(project) }
    }

    fun insertCategory(category: Category) {
        viewModelScope.launch { repository.insertCategory(category) }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch { repository.updateCategory(category) }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch { repository.deleteCategory(category) }
    }

    fun getExpensesForCategory(categoryId: Long): Flow<List<Expense>> =
        repository.getExpensesForCategory(categoryId)

    fun insertExpense(expense: Expense) {
        viewModelScope.launch { repository.insertExpense(expense) }
    }

    fun insertExpenseWithResult(expense: Expense, onInserted: (Long) -> Unit) {
        viewModelScope.launch {
            val newId = repository.insertExpense(expense)
            onInserted(newId)
        }
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch { repository.updateExpense(expense) }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch { repository.deleteExpense(expense) }
    }

    // ---------------- Export: CSV with emoji + paymentMethod ----------------

    suspend fun getExpensesForExport(projectId: Long): String {
        val rows = repository.getExportRows(projectId)
        val sb = StringBuilder()

        // Final CSV header – import bhi isi order ko use karega
        sb.appendLine("Project,Project Emoji,Category,Category Emoji,Description,Amount,Date,Payment Method")

        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        fun esc(value: String?): String {
            val v = value ?: ""
            return if (v.contains(Regex("[,\n\"]"))) {
                "\"${v.replace("\"", "\"\"")}\""
            } else v
        }

        rows.forEach { r ->
            val dateStr = sdf.format(Date(r.date))

            sb.appendLine(
                listOf(
                    esc(r.projectName),
                    esc(r.projectEmoji),
                    esc(r.categoryName),
                    esc(r.categoryEmoji),
                    esc(r.description),
                    r.amount.toString(),
                    esc(dateStr),
                    esc(r.paymentMethod)
                ).joinToString(",")
            )
        }

        return sb.toString()
    }

    // ---------------- Import: supports new 8‑column + old 4‑column CSV ----------------

    private suspend fun getOrCreateCategoryAndGetId(
        categoryName: String,
        projectId: Long,
        emoji: String?
    ): Long {
        if (categoryName.isBlank()) return -1

        val existing = repository.getCategoryByName(categoryName, projectId)

        return if (existing != null) {
            // Agar CSV me emoji diya gaya hai aur different hai, to update kar de
            if (!emoji.isNullOrBlank() && existing.emoji != emoji) {
                repository.updateCategory(existing.copy(emoji = emoji))
            }
            existing.id
        } else {
            // Nayi category create with emoji (ya default)
            val newCategory = Category(
                name = categoryName,
                projectId = projectId,
                emoji = emoji?.takeIf { it.isNotBlank() } ?: "▶️"
            )
            repository.insertCategory(newCategory)
        }
    }


    suspend fun importFromCsv(context: Context, uri: Uri, projectId: Long): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                CSVReader(InputStreamReader(inputStream)).use { reader ->

                    // ---------- 1. Read header & build index map ----------
                    // ---------- 1. Read header & build index map ----------
                    val header = reader.readNext() ?: return false

// Map: normalized column name -> index
                    val indexMap = mutableMapOf<String, Int>()

                    header.forEachIndexed { index, rawName ->
                        val name = rawName.trim().lowercase()

                        when {
                            // Simple "Emoji" column → treat as category emoji
                            name == "emoji" ->
                                indexMap["categoryEmoji"] = index

                            // "Project Emoji" column
                            name == "project emoji" ->
                                indexMap["projectEmoji"] = index   // abhi ignore karenge

                            // "Category Emoji" column
                            name == "category emoji" ->
                                indexMap["categoryEmoji"] = index

                            name in listOf("category", "category name") ->
                                indexMap["category"] = index

                            name in listOf("description", "details", "note") ->
                                indexMap["description"] = index

                            name in listOf("amount", "price", "value") ->
                                indexMap["amount"] = index

                            name in listOf("date", "txn date", "transaction date") ->
                                indexMap["date"] = index

                            name in listOf("payment method", "payment", "method") ->
                                indexMap["paymentMethod"] = index
                        }
                    }

// Category, description, amount at least hone chahiye
                    val catIdx = indexMap["category"] ?: return false
                    val descIdx = indexMap["description"] ?: return false
                    val amountIdx = indexMap["amount"] ?: return false

// Emoji indexes
                    val categoryEmojiIdx = indexMap["categoryEmoji"]    // optional
// projectEmojiIdx ko abhi use nahi kar rahe:
                    val dateIdx = indexMap["date"]                      // optional
                    val paymentIdx = indexMap["paymentMethod"]          // optional

                    val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
                    val altDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

                    // ---------- 2. Read data rows ----------
                    var row: Array<String>?
                    while (reader.readNext().also { row = it } != null) {
                        val tokens = row ?: continue
                        if (tokens.isEmpty()) continue

                        // Bounds safety
                        fun getSafe(i: Int?): String? =
                            if (i != null && i >= 0 && i < tokens.size) tokens[i].trim() else null

                        val categoryName = getSafe(catIdx)?.takeIf { it.isNotBlank() } ?: continue
                        val description = getSafe(descIdx) ?: ""
                        val amountStr = getSafe(amountIdx) ?: continue
                        val amount = amountStr.toDoubleOrNull() ?: continue

                        // Category Emoji column se emoji (agar present ho)
                        val emoji = getSafe(categoryEmojiIdx)

                        val dateStr = getSafe(dateIdx)
                        val millis = if (!dateStr.isNullOrBlank()) {
                            try {
                                // Try dd/MM/yy, fallback dd/MM/yyyy
                                (try {
                                    dateFormat.parse(dateStr)
                                } catch (_: Exception) {
                                    altDateFormat.parse(dateStr)
                                })?.time ?: System.currentTimeMillis()
                            } catch (_: Exception) {
                                System.currentTimeMillis()
                            }
                        } else {
                            System.currentTimeMillis()
                        }

                        val paymentMethod = getSafe(paymentIdx)?.takeIf { it.isNotBlank() }

                        // Category create / fetch
                        val categoryId = getOrCreateCategoryAndGetId(categoryName, projectId,emoji)
                        if (categoryId == -1L) continue

                        val expense = Expense(
                            description = description,
                            amount = amount,
                            date = millis,
                            categoryId = categoryId,
                            paymentMethod = paymentMethod
                        )

                        repository.insertExpense(expense)
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

}

class PaisaTrackerViewModelFactory(
    private val repository: PaisaTrackerRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PaisaTrackerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PaisaTrackerViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
