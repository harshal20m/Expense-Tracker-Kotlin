package com.example.paisatracker

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.paisatracker.data.Asset
import com.example.paisatracker.data.Category
import com.example.paisatracker.data.CategoryExpense
import com.example.paisatracker.data.CategoryWithTotal
import com.example.paisatracker.data.Expense
import com.example.paisatracker.data.ExpenseExport
import com.example.paisatracker.data.PaisaTrackerRepository
import com.example.paisatracker.data.Project
import com.example.paisatracker.data.ProjectWithTotal
import com.opencsv.CSVReader
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale



//recently added for assets**
import com.example.paisatracker.util.ImageUtils
import kotlinx.coroutines.Dispatchers
import java.io.File

class PaisaTrackerViewModel(private val repository: PaisaTrackerRepository) : ViewModel() {

    fun insertExpenseWithResult(expense: Expense, onInserted: (Long) -> Unit) {
        viewModelScope.launch {
            val newId = repository.insertExpense(expense)
            onInserted(newId)
        }
    }


    // Asset functions latest added independent vs depenedent assets .
    fun getAllAssets() = repository.getAllAssets()
    fun getAssetsForExpense(expenseId: Long) = repository.getAssetsForExpense(expenseId)
    fun getIndependentAssets() = repository.getIndependentAssets()

    suspend fun insertAsset(asset: Asset) = repository.insertAsset(asset)
    fun deleteAsset(asset: Asset) {
        viewModelScope.launch { repository.deleteAsset(asset) }
    }

    // --------- NEW: image copy + helpers ---------

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

            val input = context.contentResolver.openInputStream(uri)
            if (input == null) {
                Log.e("PT_DEBUG", "saveImageToInternalStorage: inputStream null for uri=$uri")
                return null
            }

            input.use { ins ->
                destFile.outputStream().use { outs ->
                    ins.copyTo(outs)
                }
            }

            Log.d("PT_DEBUG", "Image saved to ${destFile.absolutePath}")
            destFile.absolutePath
        } catch (e: Exception) {
            Log.e("PT_DEBUG", "saveImageToInternalStorage error", e)
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
        val path = saveImageToInternalStorage(context, uri)
        if (path == null) {
            Log.e("PT_DEBUG", "saveAssetInternal: path null, asset not inserted")
            return
        }

        val asset = Asset(
            imagePath = path,
            title = title,
            description = description,
            expenseId = expenseId
        )
        repository.insertAsset(asset)
        Log.d("PT_DEBUG", "Asset inserted for expenseId=$expenseId, path=$path")
    }


    fun addIndependentAsset(
        context: Context,
        uri: Uri,
        title: String,
        description: String
    ) {
        viewModelScope.launch {
            saveAssetInternal(
                context = context,
                uri = uri,
                title = title,
                description = description,
                expenseId = null      // independent
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
        viewModelScope.launch {
            saveAssetInternal(
                context = context,
                uri = uri,
                title = title,
                description = description,
                expenseId = expenseId  // linked
            )
        }
    }

    //end


    fun getAllProjectsWithTotal(): Flow<List<ProjectWithTotal>> = repository.getAllProjectsWithTotal()

    fun getCategoryExpenses(projectId: Long): Flow<List<CategoryExpense>> = repository.getCategoryExpenses(projectId)

    fun getExpenseById(id: Long): Flow<Expense?> =
        repository.getExpenseById(id)

    fun getCategoriesWithTotalForProject(projectId: Long): Flow<List<CategoryWithTotal>> = repository.getCategoriesWithTotalForProject(projectId)

    fun getAllExpenses(): Flow<List<Expense>> = repository.getAllExpenses()

    fun getProjectById(projectId: Long): Flow<Project> = repository.getProjectById(projectId)

    fun getCategoryById(categoryId: Long): Flow<Category> = repository.getCategoryById(categoryId)



    suspend fun getExpensesForExport(projectId: Long): String {
        val expenses = repository.getExpensesForExport(projectId)
        return formatToCsv(expenses)
    }

    private fun formatToCsv(expenses: List<ExpenseExport>): String {
        val csvBuilder = StringBuilder()
        csvBuilder.append("Category Name,Description,Amount,Date\n")
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        for (expense in expenses) {
            csvBuilder.append("\"${expense.categoryName.replace("\"", "\"\"")}\",\"${expense.expenseDescription.replace("\"", "\"\"")}\",${expense.expenseAmount},${sdf.format(Date(expense.expenseDate))}\n")
        }
        return csvBuilder.toString()
    }

    private suspend fun getOrCreateCategoryAndGetId(categoryName: String, projectId: Long): Long {
        if (categoryName.isBlank()) return -1 // Invalid category
        val category = repository.getCategoryByName(categoryName, projectId)
        return category?.id ?: repository.insertCategory(Category(name = categoryName, projectId = projectId))
    }

    suspend fun importFromCsv(context: Context, uri: Uri, projectId: Long): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                CSVReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readNext() // Skip header

                    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    var line: Array<String>?
                    while (reader.readNext().also { line = it } != null) {
                        val tokens = line!!
                        if (tokens.size < 3) continue // Must have at least Category, Description, Amount

                        val categoryName = tokens[0].trim()
                        val expenseDescription = tokens[1].trim()
                        val expenseAmount = tokens[2].trim().toDoubleOrNull() ?: continue // Skip if amount is invalid

                        val expenseDate = if (tokens.size >= 4 && tokens[3].isNotBlank()) {
                            try {
                                dateFormat.parse(tokens[3].trim())?.time
                            } catch (e: Exception) {
                                System.currentTimeMillis() // Use today's date if parsing fails
                            }
                        } else {
                            System.currentTimeMillis() // Use today's date if date is missing
                        } ?: System.currentTimeMillis()

                        val categoryId = getOrCreateCategoryAndGetId(categoryName, projectId)
                        if (categoryId == -1L) continue // Skip if category is blank

                        repository.insertExpense(Expense(description = expenseDescription, amount = expenseAmount, date = expenseDate, categoryId = categoryId))
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun insertProject(project: Project) {
        viewModelScope.launch {
            repository.insertProject(project)
        }
    }

    fun updateProject(project: Project) {
        viewModelScope.launch {
            repository.updateProject(project)
        }
    }

    fun deleteProject(project: Project) {
        viewModelScope.launch {
            repository.deleteProject(project)
        }
    }

    fun getCategoriesForProject(projectId: Long): Flow<List<Category>> = repository.getCategoriesForProject(projectId)

    fun insertCategory(category: Category) {
        viewModelScope.launch {
            repository.insertCategory(category)
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            repository.updateCategory(category)
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            repository.deleteCategory(category)
        }
    }

    fun getExpensesForCategory(categoryId: Long): Flow<List<Expense>> = repository.getExpensesForCategory(categoryId)

    fun insertExpense(expense: Expense) {
        viewModelScope.launch {
            repository.insertExpense(expense)
        }
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch {
            repository.updateExpense(expense)
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
        }
    }
}

class PaisaTrackerViewModelFactory(private val repository: PaisaTrackerRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PaisaTrackerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PaisaTrackerViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
