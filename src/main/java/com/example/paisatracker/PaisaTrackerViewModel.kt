package com.example.paisatracker


 import android.content.Context
import android.net.Uri
 import androidx.compose.runtime.mutableFloatStateOf
 import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.paisatracker.data.Asset
import com.example.paisatracker.data.BackupMetadata
import com.example.paisatracker.data.Budget
import com.example.paisatracker.data.BudgetPeriod
import com.example.paisatracker.data.BudgetWithSpending
import com.example.paisatracker.data.Category
import com.example.paisatracker.data.CategoryExpense
import com.example.paisatracker.data.CategoryWithTotal
import com.example.paisatracker.data.Currency
import com.example.paisatracker.data.CurrencyList
import com.example.paisatracker.data.CurrencyPreferencesRepository
import com.example.paisatracker.data.Expense
import com.example.paisatracker.data.FlapData
import com.example.paisatracker.data.FlapNote
import com.example.paisatracker.data.PaisaTrackerRepository
import com.example.paisatracker.data.Project
import com.example.paisatracker.data.ProjectWithTotal
import com.example.paisatracker.data.RecentExpense

 import com.example.paisatracker.data.serializeHistory
import com.example.paisatracker.data.serializeNotes
import com.example.paisatracker.util.ImageUtils
import com.opencsv.CSVReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
 import kotlinx.coroutines.flow.asStateFlow
 import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class PaisaTrackerViewModel(
    private val repository: PaisaTrackerRepository,
    currencyPreferencesRepository: CurrencyPreferencesRepository
) : ViewModel() {

    //budget
    val budgetsWithSpending: StateFlow<List<BudgetWithSpending>> = combine(
        repository.getAllBudgets(),
        repository.getAllExpenses(),
        repository.getAllProjects(),
        repository.getAllCategories()
    ) { budgets, expenses, projects, categories ->
        budgets.map { budget ->
            val now = Calendar.getInstance()
            val periodStart = getPeriodStart(budget.period)

            val filtered = expenses.filter { expense ->
                val expenseDate = expense.date  // assuming Long timestamp
                val matchesPeriod = expenseDate >= periodStart
                val matchesProject = budget.projectId == null ||
                        categories.find { it.id == expense.categoryId }?.projectId == budget.projectId
                val matchesCategory = budget.categoryId == null ||
                        expense.categoryId == budget.categoryId
                matchesPeriod && matchesProject && matchesCategory
            }

            val categoryName = budget.categoryId?.let { catId ->
                categories.find { it.id == catId }?.name
            }
            val projectName = budget.projectId?.let { projId ->
                projects.find { it.id == projId }?.name
            }

            BudgetWithSpending(
                budget = budget,
                spent = filtered.sumOf { it.amount },
                categoryName = categoryName,
                projectName = projectName
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())



    // ✅ With this (lifecycle-aware StateFlow):
    private val _flapButtonOffsetY = MutableStateFlow<Float>(Float.NaN)
    val flapButtonOffsetY: StateFlow<Float> = _flapButtonOffsetY.asStateFlow()

    // ✅ Helper function for cleaner UI updates
    fun updateFlapButtonOffsetY(offsetDp: Float) {
        _flapButtonOffsetY.value = offsetDp
    }


    fun addBudget(budget: Budget) {
        viewModelScope.launch {
            repository.insertBudget(budget)
        }
    }

    fun deleteBudget(budget: Budget) {
        viewModelScope.launch {
            repository.deleteBudget(budget)
        }
    }

    fun updateBudget(budget: Budget) {
        viewModelScope.launch {
            repository.updateBudget(budget)
        }
    }


    fun toggleBudgetActive(budgetId: Long, isActive: Boolean) {
        viewModelScope.launch {
            repository.toggleBudgetActive(budgetId, isActive)
        }
    }

    private fun getPeriodStart(period: BudgetPeriod): Long {
        val cal = Calendar.getInstance()
        return when (period) {
            BudgetPeriod.DAILY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            BudgetPeriod.WEEKLY -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            BudgetPeriod.MONTHLY -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            BudgetPeriod.YEARLY -> {
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
        }
    }

    //flap
// ================================================================

    // ── UI-only (not persisted) ───────────────────────────────────────────────
    val isFlapExpanded    = MutableStateFlow(false)
    val flapSelectedTab   = MutableStateFlow(0)       // 0 = Calculator, 1 = Notes
    val calcShowHistory   = MutableStateFlow(false)   // calc history sub-screen

    // ── Calculator ────────────────────────────────────────────────────────────
    val calcDisplay    = MutableStateFlow("0")
    val calcExpression = MutableStateFlow("")
    val calcHistory    = MutableStateFlow<List<String>>(emptyList())

    // ── Notes (list of FlapNote) ──────────────────────────────────────────────
    val flapNotes = MutableStateFlow<List<FlapNote>>(emptyList())

    // ── Startup load ──────────────────────────────────────────────────────────
    init {
        viewModelScope.launch {
            val saved = repository.getFlapDataOnce()
            if (saved != null) {
                calcDisplay.value    = saved.calcDisplay
                calcExpression.value = saved.calcExpression
                calcHistory.value    = saved.calcHistoryList()
                flapNotes.value      = saved.notesList()
            }
            startFlapPersistence()
        }
    }

    // ── Notes CRUD ────────────────────────────────────────────────────────────

    fun addFlapNote(text: String) {
        if (text.isBlank()) return
        val note = FlapNote(id = UUID.randomUUID().toString(), text = text.trim())
        flapNotes.value = listOf(note) + flapNotes.value   // newest first
    }

    fun editFlapNote(id: String, newText: String) {
        if (newText.isBlank()) { deleteFlapNote(id); return }
        flapNotes.value = flapNotes.value.map { if (it.id == id) it.copy(text = newText.trim()) else it }
    }

    fun deleteFlapNote(id: String) {
        flapNotes.value = flapNotes.value.filter { it.id != id }
    }

    // ── Debounced persistence ─────────────────────────────────────────────────
    @OptIn(kotlinx.coroutines.FlowPreview::class)
    private fun startFlapPersistence() {
        combine(
            calcDisplay,
            calcExpression,
            calcHistory,
            flapNotes
        ) { display, expr, history, notes ->
            FlapData(
                id = 1,
                notesSerialized = notes.serializeNotes(),
                calcHistorySerialized = history.serializeHistory(),
                calcDisplay = display,
                calcExpression = expr,
                lastUpdatedAt = System.currentTimeMillis()
            )
        }
            .drop(1)
            .debounce(600L)
            .distinctUntilChanged()
            .onEach { repository.upsertFlapData(it) }
            .launchIn(viewModelScope)
    }

    // Currency State - Add this
    val currentCurrency: StateFlow<Currency> = currencyPreferencesRepository.selectedCurrency
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CurrencyList.getCurrencyByCode("INR")
        )

    private val _recentExpensesLimit = MutableStateFlow(10)
    @OptIn(ExperimentalCoroutinesApi::class)
    val recentExpenses: Flow<List<RecentExpense>> = _recentExpensesLimit.flatMapLatest { limit ->
        repository.getRecentExpensesWithDetails(limit)
    }

    // Add this function for loading more
    fun loadMoreRecentExpenses() {
        _recentExpensesLimit.value += 10
    }

    fun getAllExpensesWithDetails(): Flow<List<RecentExpense>> {
        return repository.getAllExpensesWithDetails()
    }

    //-----------------Backup----------------
    fun getRecentBackups(): Flow<List<BackupMetadata>> {
        return repository.getRecentBackups()
    }

    // ---------------- Assets ----------------

    fun getAllAssets() = repository.getAllAssets()
    fun getAssetsForExpense(expenseId: Long) = repository.getAssetsForExpense(expenseId)

    fun deleteAsset(asset: Asset) {
        viewModelScope.launch(Dispatchers.IO) {
            ImageUtils.deleteImage(asset.imagePath)
            repository.deleteAsset(asset)
        }
    }

    private suspend fun saveAssetInternal(
        context: Context,
        uri: Uri,
        title: String,
        description: String,
        expenseId: Long?
    ) {
        val path = ImageUtils.saveImageToInternalStorage(context, uri) ?: return

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

    fun getAllProjects(): Flow<List<Project>> =
        repository.getAllProjects()

    fun getAllProjectsWithTotal(): Flow<List<ProjectWithTotal>> =
        repository.getAllProjectsWithTotal()

    fun getCategoryExpenses(projectId: Long): Flow<List<CategoryExpense>> =
        repository.getCategoryExpenses(projectId)

    fun getExpenseById(id: Long): Flow<Expense?> =
        repository.getExpenseById(id)

    fun getAllCategories(): Flow<List<Category>> =
        repository.getAllCategories()

    fun getCategoriesWithTotalForProject(projectId: Long): Flow<List<CategoryWithTotal>> =
        repository.getCategoriesWithTotalForProject(projectId)


    fun getProjectById(projectId: Long): Flow<Project> =
        repository.getProjectById(projectId)

    fun getCategoryById(categoryId: Long): Flow<Category> =
        repository.getCategoryById(categoryId)


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

        // Header (9 columns):
        sb.appendLine("Project,Project Emoji,Category,Category Emoji,Description,Amount,Date,Payment Method,Payment Method Emoji")


        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        var totalAmount = 0.0

        // Helper function to escape CSV values
        fun esc(value: String?): String {
            val v = value ?: ""
            return if (v.contains(Regex("[,\n\"]"))) {
                "\"${v.replace("\"", "\"\"")}\""
            } else v
        }

        // Write each expense row
        rows.forEach { r ->
            val dateStr = sdf.format(Date(r.date))
            totalAmount += r.amount  // ✅ Accumulate total

            sb.appendLine(
                listOf(
                    esc(r.projectName),
                    esc(r.projectEmoji),
                    esc(r.categoryName),
                    esc(r.categoryEmoji),
                    esc(r.description),
                    r.amount.toString(),
                    esc(dateStr),
                    esc(r.paymentMethod),
                    esc(r.paymentMethodEmoji)  // ✅ Added
                ).joinToString(",")
            )
        }

        val currency = currentCurrency.value.symbol
        val formattedTotal = String.format(Locale.US, "%.2f %s", totalAmount, currency)

        // ✅ Add TOTAL row at the bottom
        sb.appendLine(
            listOf(
                "TOTAL",
                "",
                "",
                "",
                "",
               formattedTotal,
                "",
                "",
                ""
            ).joinToString(",")
        )

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
                                indexMap["projectEmoji"] = index

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
                    val dateIdx = indexMap["date"]                      // optional
                    val paymentIdx = indexMap["paymentMethod"]          // optional

                    val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
                    val altDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

                    // ---------- 2. Read data rows ----------
                    var row: Array<String>?
                    while (reader.readNext().also { row = it } != null) {
                        val tokens = row ?: continue
                        if (tokens.size == 0) continue

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
                        val categoryId = getOrCreateCategoryAndGetId(categoryName, projectId, emoji)
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
    private val repository: PaisaTrackerRepository,
    private val currencyPreferencesRepository: CurrencyPreferencesRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PaisaTrackerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PaisaTrackerViewModel(repository, currencyPreferencesRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}