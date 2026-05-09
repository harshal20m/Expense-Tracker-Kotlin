package com.example.paisatracker.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class PaisaTrackerRepository(
    private val projectDao: ProjectDao,
    private val categoryDao: CategoryDao,
    private val expenseDao: ExpenseDao,
    private val assetDao: AssetDao,
    private val backupDao: BackupDao,
    private val budgetDao: BudgetDao,
    private val flapDao: FlapDao,
    private val salaryRecordDao: SalaryRecordDao,
    private val actionHistoryDao: ActionHistoryDao,
    private val bankAccountDao: BankAccountDao
) {
    // ── Bank Account Methods ──────────────────────────────────────────────────
    
    fun getAllBankAccounts(): Flow<List<BankAccount>> =
        bankAccountDao.getAllAccounts()
    
    fun getActiveBankAccounts(): Flow<List<BankAccount>> =
        bankAccountDao.getActiveAccounts()
    
    fun getBankAccountById(accountId: Long): Flow<BankAccount?> =
        bankAccountDao.getAccountById(accountId)
    
    suspend fun getBankAccountByIdOnce(accountId: Long): BankAccount? =
        bankAccountDao.getAccountByIdOnce(accountId)
    
    fun getBankAccountsByType(type: String): Flow<List<BankAccount>> =
        bankAccountDao.getAccountsByType(type)
    
    fun getTotalBankBalance(): Flow<Double> =
        bankAccountDao.getTotalBalance()
    
    fun getActiveBankAccountCount(): Flow<Int> =
        bankAccountDao.getActiveAccountCount()
    
    fun searchBankAccounts(query: String): Flow<List<BankAccount>> =
        bankAccountDao.searchAccounts(query)
    
    fun getAccountBalances(): Flow<List<AccountBalance>> =
        bankAccountDao.getAccountBalances()
    
    suspend fun insertBankAccount(account: BankAccount): Long =
        bankAccountDao.insert(account)
    
    suspend fun updateBankAccount(account: BankAccount) =
        bankAccountDao.update(account)
    
    suspend fun updateBankAccountBalance(accountId: Long, newBalance: Double) =
        bankAccountDao.updateBalance(accountId, newBalance)
    
    suspend fun incrementBankAccountBalance(accountId: Long, amount: Double) =
        bankAccountDao.incrementBalance(accountId, amount)
    
    suspend fun decrementBankAccountBalance(accountId: Long, amount: Double) =
        bankAccountDao.decrementBalance(accountId, amount)
    
    suspend fun setBankAccountActiveStatus(accountId: Long, isActive: Boolean) =
        bankAccountDao.setActiveStatus(accountId, isActive)
    
    suspend fun deleteBankAccount(account: BankAccount) =
        bankAccountDao.delete(account)
    
    suspend fun deleteBankAccountById(accountId: Long) =
        bankAccountDao.deleteById(accountId)
    
    suspend fun softDeleteBankAccount(accountId: Long) =
        bankAccountDao.softDelete(accountId)
    
    // Action History
    fun getActionHistory() = actionHistoryDao.getAllHistory()
    suspend fun insertAction(action: ActionHistory) = actionHistoryDao.insertAction(action)
    suspend fun getLatestAction() = actionHistoryDao.getLatestAction()
    suspend fun deleteAction(action: ActionHistory) = actionHistoryDao.deleteAction(action)
    suspend fun deleteActionById(id: Long) = actionHistoryDao.deleteById(id)
    suspend fun clearActionHistory() = actionHistoryDao.clearHistory()



 // ══════════════════════════════════════════════════════════════════════════
 // SALARY RECORDS - Multi-Salary Support
 // ══════════════════════════════════════════════════════════════════════════

 // ── Multi-Salary Methods ──────────────────────────────────────────────────
 
 /** Get all active salaries for current month (supports multiple income sources) */
 fun getCurrentMonthSalaries(month: Int, year: Int): Flow<List<SalaryRecord>> =
     salaryRecordDao.getCurrentMonthSalaries(month, year)

 /** Get salaries for a specific account */
 fun getSalariesForAccount(accountId: Long, month: Int, year: Int): Flow<List<SalaryRecord>> =
     salaryRecordDao.getSalariesForAccount(accountId, month, year)

 /** Get total monthly income from all active salaries */
 fun getTotalMonthlyIncome(month: Int, year: Int): Flow<Double> =
     salaryRecordDao.getTotalMonthlyIncome(month, year)

 /** Get all recurring salaries for auto-generation */
 suspend fun getActiveRecurringSalaries(): List<SalaryRecord> =
     salaryRecordDao.getActiveRecurringSalaries()

 /** Insert salary and credit to linked account */
 suspend fun insertSalaryAndCreditAccount(salary: SalaryRecord): Long {
     val salaryId = salaryRecordDao.insert(salary)
     if (salary.linkedAccountId > 0) {
         incrementBankAccountBalance(salary.linkedAccountId, salary.amount)
     }
     return salaryId
 }

 /** Get monthly salary summary with all salaries and spending */
 suspend fun getMonthlySalarySummary(month: Int, year: Int): MonthlySalarySummary {
     val salaries = getCurrentMonthSalaries(month, year).firstOrNull() ?: emptyList()
     val totalIncome = salaries.sumOf { it.amount }
     
     // Get earliest salary timestamp for the month
     val earliestTimestamp = salaries.minOfOrNull { it.receivedAt } ?: System.currentTimeMillis()
     val totalSpent = getTotalSpentSince(earliestTimestamp).firstOrNull() ?: 0.0
     
     val remainingBalance = totalIncome - totalSpent
     val spendPercentage = if (totalIncome > 0) (totalSpent / totalIncome).toFloat() else 0f
     
     return MonthlySalarySummary(
         month = month,
         year = year,
         totalIncome = totalIncome,
         salaries = salaries,
         totalSpent = totalSpent,
         remainingBalance = remainingBalance,
         spendPercentage = spendPercentage
     )
 }

 /** Auto-create recurring salaries for current month (supports multiple) */
 suspend fun autoCreateRecurringSalariesForMonth(
     month: Int,
     year: Int,
     timestamp: Long = System.currentTimeMillis()
 ): List<SalaryRecord> {
     val recurringSalaries = getActiveRecurringSalaries()
     val generatedSalaries = mutableListOf<SalaryRecord>()
     
     recurringSalaries.forEach { lastRecurringSalary ->
         // Check if this recurring salary already exists for current month
         val alreadyExists = salaryRecordDao.salaryExistsForAccountAndType(
             month = month,
             year = year,
             accountId = lastRecurringSalary.linkedAccountId,
             sourceType = lastRecurringSalary.sourceType
         )
         
         if (!alreadyExists) {
             val newSalary = lastRecurringSalary.copy(
                 id = 0,
                 month = month,
                 year = year,
                 receivedAt = timestamp,
                 autoGenerated = true
             )
             
             val insertedId = insertSalaryAndCreditAccount(newSalary)
             generatedSalaries.add(newSalary.copy(id = insertedId))
         }
     }
     
     return generatedSalaries
 }

 // ── Legacy Methods (for backward compatibility) ───────────────────────────
 
 /** @deprecated Use getCurrentMonthSalaries() for multi-salary support */
 fun getCurrentMonthSalary(month: Int, year: Int): Flow<SalaryRecord?> =
     salaryRecordDao.getCurrentMonthRecord(month, year)

 suspend fun getCurrentMonthSalaryOnce(month: Int, year: Int): SalaryRecord? =
     salaryRecordDao.getCurrentMonthRecordOnce(month, year)

 suspend fun getLatestRecurringSalaryRecord(): SalaryRecord? =
     salaryRecordDao.getLatestRecurringRecord()

 /** @deprecated Use autoCreateRecurringSalariesForMonth() for multi-salary support */
 suspend fun autoCreateRecurringSalaryIfNeeded(
     month: Int,
     year: Int,
     timestamp: Long = System.currentTimeMillis()
 ): SalaryRecord? {
     val existingCurrentMonth = getCurrentMonthSalaryOnce(month, year)
     if (existingCurrentMonth != null) return null

     val latestRecurring = getLatestRecurringSalaryRecord() ?: return null
     if (!latestRecurring.isRecurring) return null

     val generatedRecord = latestRecurring.copy(
         id = 0,
         month = month,
         year = year,
         receivedAt = timestamp,
         autoGenerated = true
     )

     val insertedId = salaryRecordDao.insert(generatedRecord)

     @Suppress("DEPRECATION")
     latestRecurring.recurringAccountId?.let { accountId ->
         bankAccountDao.incrementBalance(accountId, latestRecurring.amount, timestamp)
     }

     return generatedRecord.copy(id = insertedId)
 }

 // ── Common Methods ────────────────────────────────────────────────────────
 
 fun getAllSalaryRecords(): Flow<List<SalaryRecord>> = salaryRecordDao.getAllRecords()
 fun getTotalSpentSince(ts: Long): Flow<Double> = salaryRecordDao.getTotalSpentSince(ts)
 fun getCategoryBreakdownSince(ts: Long): Flow<List<CategorySpend>> = salaryRecordDao.getCategoryBreakdownSince(ts)
 suspend fun insertSalaryRecord(r: SalaryRecord): Long = salaryRecordDao.insert(r)
 suspend fun updateSalaryRecord(r: SalaryRecord) = salaryRecordDao.update(r)
 suspend fun deleteSalaryRecord(r: SalaryRecord) = salaryRecordDao.delete(r)


    //flapmethods


    fun getFlapData(): Flow<FlapData?> = flapDao.getFlapData()

    suspend fun upsertFlapData(flapData: FlapData) =
        flapDao.upsertFlapData(flapData)

    suspend fun getFlapDataOnce(): FlapData? =
        flapDao.getFlapDataOnce()

    // --- Budget Methods ---

    fun getAllActiveBudgets(): Flow<List<Budget>> =
        budgetDao.getAllActiveBudgets()

    fun getAllBudgets(): Flow<List<Budget>> =
        budgetDao.getAllBudgets()

    suspend fun insertBudget(budget: Budget): Long =
        budgetDao.insertBudget(budget)

    suspend fun deleteBudget(budget: Budget) =
        budgetDao.deleteBudget(budget)

    suspend fun updateBudget(budget: Budget) =
        budgetDao.updateBudget(budget)



    suspend fun toggleBudgetActive(budgetId: Long, isActive: Boolean) =
        budgetDao.toggleBudgetActive(budgetId, isActive)

    fun getRecentExpensesWithDetails(limit: Int): Flow<List<RecentExpense>> {
        return expenseDao.getRecentExpensesWithDetails(limit)
    }

    fun getAllExpensesWithDetails(): Flow<List<RecentExpense>> {
        return expenseDao.getAllExpensesWithDetails()
    }

    fun getAllProjects(): Flow<List<Project>> = projectDao.getActiveProjects()

    fun getCompletedProjects(): Flow<List<Project>> = projectDao.getCompletedProjects()

    // Add this method for seeding
    suspend fun getAllProjectsList(): List<Project> {
        return projectDao.getAllProjectsList()
    }

    fun getProjectById(projectId: Long): Flow<Project> = projectDao.getProjectById(projectId)

    fun getCategoryById(categoryId: Long): Flow<Category> = categoryDao.getCategoryById(categoryId)

     suspend fun getCategoriesForProjectList(projectId: Long): List<Category> {
         return categoryDao.getCategoriesForProjectList(projectId)
     }
    fun getAllProjectsWithTotal(): Flow<List<ProjectWithTotal>> = projectDao.getActiveProjectsWithTotal()

    fun getCompletedProjectsWithTotal(): Flow<List<ProjectWithTotal>> = projectDao.getCompletedProjectsWithTotal()

    fun getCategoryExpenses(projectId: Long): Flow<List<CategoryExpense>> = projectDao.getCategoryExpenses(projectId)

    fun getCategoriesWithTotalForProject(projectId: Long): Flow<List<CategoryWithTotal>> = categoryDao.getCategoriesWithTotalForProject(projectId)

    fun getAllCategories(): Flow<List<Category>> = categoryDao.getAllCategories()

    suspend fun getAllCategoriesList(): List<Category> {
        return categoryDao.getAllCategoriesList()
    }
    fun getAllExpenses(): Flow<List<Expense>> = expenseDao.getAllExpenses()

    suspend fun getExportRows(projectId: Long?): List<ExportRow> =
        expenseDao.getExportRows(projectId)

    suspend fun insertProject(project: Project): Long {
        return projectDao.insertProject(project)
    }

    suspend fun updateProject(project: Project) {
        projectDao.updateProject(project)
    }

    suspend fun updateProjectStatus(projectId: Long, isCompleted: Boolean) {
        projectDao.updateProjectStatus(projectId, isCompleted)
    }

    suspend fun deleteProject(project: Project) {
        projectDao.deleteProject(project)
    }

    fun getCategoriesForProject(projectId: Long): Flow<List<Category>> = categoryDao.getCategoriesForProject(projectId)

    suspend fun insertCategory(category: Category): Long {
        return categoryDao.insertCategory(category)
    }

    suspend fun updateCategory(category: Category) {
        categoryDao.updateCategory(category)
    }

    suspend fun deleteCategory(category: Category) {
        categoryDao.deleteCategory(category)
    }

    fun getExpensesForCategory(categoryId: Long): Flow<List<Expense>> = expenseDao.getExpensesForCategory(categoryId)
    suspend fun getExpensesForCategoryList(categoryId: Long): List<Expense> = expenseDao.getExpensesForCategoryList(categoryId)

    fun getExpenseById(id: Long): Flow<Expense?> = expenseDao.getExpenseById(id)

    suspend fun insertExpense(expense: Expense): Long {
        val expenseId = expenseDao.insert(expense)
        
        // Deduct expense amount from bank account if linked
        expense.bankAccountId?.let { accountId ->
            decrementBankAccountBalance(accountId, expense.amount)
        }
        
        return expenseId
    }

    suspend fun updateExpense(expense: Expense) {
        // Get the old expense to compare bank account changes
        val oldExpense = expenseDao.getExpenseByIdOnce(expense.id)
        
        // Update the expense
        expenseDao.updateExpense(expense)
        
        // Handle bank account balance adjustments
        if (oldExpense != null) {
            // If old expense had a bank account, refund the old amount
            oldExpense.bankAccountId?.let { oldAccountId ->
                incrementBankAccountBalance(oldAccountId, oldExpense.amount)
            }
            
            // If new expense has a bank account, deduct the new amount
            expense.bankAccountId?.let { newAccountId ->
                decrementBankAccountBalance(newAccountId, expense.amount)
            }
        }
    }

    suspend fun deleteExpense(expense: Expense) {
        // Refund the expense amount back to the bank account if linked
        expense.bankAccountId?.let { accountId ->
            incrementBankAccountBalance(accountId, expense.amount)
        }
        
        expenseDao.deleteExpense(expense)
    }

    suspend fun getProjectByName(name: String): Project? {
        return projectDao.getProjectByName(name)
    }

    suspend fun getCategoryByName(name: String, projectId: Long): Category? {
        return categoryDao.getCategoryByName(name, projectId)
    }

    // Asset functions
    fun getAllAssets(): Flow<List<Asset>> = assetDao.getAllAssets()
    fun getAssetsForExpense(expenseId: Long): Flow<List<Asset>> = assetDao.getAssetsForExpense(expenseId)
    fun getIndependentAssets(): Flow<List<Asset>> = assetDao.getIndependentAssets()

    suspend fun insertAsset(asset: Asset) = assetDao.insertAsset(asset)
    suspend fun deleteAsset(asset: Asset) = assetDao.deleteAsset(asset)

    //backup methods
    fun getRecentBackups(): Flow<List<BackupMetadata>> {
        return backupDao.getRecentBackups()
    }

    fun getAllBackups(): Flow<List<BackupMetadata>> {
        return backupDao.getAllBackups()
    }

    suspend fun insertBackup(backup: BackupMetadata): Long {
        return backupDao.insertBackup(backup)
    }

    suspend fun deleteBackup(backup: BackupMetadata) {
        backupDao.deleteBackup(backup)
    }

    suspend fun getProjectCount(): Int {
        return projectDao.getProjectCount()
    }

    suspend fun getCategoryCount(): Int {
        return categoryDao.getCategoryCount()
    }

    suspend fun getExpenseCount(): Int {
        return expenseDao.getExpenseCount()
    }

    suspend fun getTotalAmount(): Double {
        return expenseDao.getTotalAmount() ?: 0.0
    }

    // Search methods
    fun searchExpensesByDescription(query: String?, projectId: Long?): Flow<List<RecentExpense>> {
        return expenseDao.searchExpensesByDescription(query, projectId)
    }

    fun getExpensesByBankAccount(bankAccountId: Long): Flow<List<RecentExpense>> {
        return expenseDao.getExpensesByBankAccount(bankAccountId)
    }

    fun searchExpensesByAmount(minAmount: Double?, maxAmount: Double?, projectId: Long?): Flow<List<RecentExpense>> {
        return expenseDao.searchExpensesByAmount(minAmount, maxAmount, projectId)
    }

    fun searchExpensesByDateRange(startDate: Long?, endDate: Long?, projectId: Long?): Flow<List<RecentExpense>> {
        return expenseDao.searchExpensesByDateRange(startDate, endDate, projectId)
    }

    // ============================================================================
    // NEW ANALYTICS METHODS - Added for Sprint 2 (Time-based Analytics)
    // These methods support the new TimePeriod and analytics features
    // ============================================================================

    /**
     * Get expenses within a specific date range.
     * Used for filtering expenses by time period (week, month, year, custom).
     *
     * @param startDate Start timestamp (inclusive)
     * @param endDate End timestamp (inclusive)
     * @return Flow of expenses in the date range
     */
    fun getExpensesByDateRange(startDate: Long, endDate: Long): Flow<List<Expense>> =
        expenseDao.getExpensesByDateRange(startDate, endDate)

    /**
     * Get monthly aggregated totals for the last N months.
     * Returns month in format "YYYY-MM", total amount, and count of expenses.
     *
     * @param months Number of months to retrieve (default 12)
     * @return Flow of monthly totals
     */
    fun getMonthlyTotals(months: Int = 12): Flow<List<com.example.paisatracker.domain.models.MonthlyTotal>> =
        expenseDao.getMonthlyTotals(months)

    /**
     * Get yearly aggregated totals for all years with expenses.
     * Returns year, total amount, and count of expenses.
     *
     * @return Flow of yearly totals
     */
    fun getYearlyTotals(): Flow<List<com.example.paisatracker.domain.models.YearlyTotal>> =
        expenseDao.getYearlyTotals()

    /**
     * Get category-wise spending for a specific date range.
     * Includes category details and aggregated amounts.
     * Percentages are calculated automatically.
     *
     * @param startDate Start timestamp (inclusive)
     * @param endDate End timestamp (inclusive)
     * @return Flow of category spending data with percentages
     */
    fun getCategorySpendingByDateRange(
        startDate: Long,
        endDate: Long
    ): Flow<List<com.example.paisatracker.domain.models.CategorySpending>> =
        expenseDao.getCategorySpendingByDateRange(startDate, endDate)

    /**
     * Get total spending for a specific date range.
     * Used for calculating overall spending in a time period.
     *
     * @param startDate Start timestamp (inclusive)
     * @param endDate End timestamp (inclusive)
     * @return Total amount spent, or 0.0 if no expenses
     */
    suspend fun getTotalByDateRange(startDate: Long, endDate: Long): Double =
        expenseDao.getTotalByDateRange(startDate, endDate)

    /**
     * Get expense count for a specific date range.
     * Used for analytics and statistics.
     *
     * @param startDate Start timestamp (inclusive)
     * @param endDate End timestamp (inclusive)
     * @return Number of expenses in the date range
     */
    suspend fun getCountByDateRange(startDate: Long, endDate: Long): Int =
        expenseDao.getCountByDateRange(startDate, endDate)

    /**
     * Get monthly totals for a specific year.
     * Returns all 12 months with their totals (0 if no expenses).
     *
     * @param year Year to query (e.g., "2024")
     * @return Flow of monthly totals for the year
     */
    fun getMonthlyTotalsForYear(year: String): Flow<List<com.example.paisatracker.domain.models.MonthlyTotal>> =
        expenseDao.getMonthlyTotalsForYear(year)

    /**
     * Get top N categories by spending in a date range.
     * Useful for "Top Spending Categories" analytics.
     *
     * @param startDate Start timestamp (inclusive)
     * @param endDate End timestamp (inclusive)
     * @param limit Number of top categories to return (default 5)
     * @return Flow of top category spending data
     */
    fun getTopCategoriesByDateRange(
        startDate: Long,
        endDate: Long,
        limit: Int = 5
    ): Flow<List<com.example.paisatracker.domain.models.CategorySpending>> =
        expenseDao.getTopCategoriesByDateRange(startDate, endDate, limit)

    /**
     * Get average daily spending for a date range.
     * Useful for "Daily Average" analytics.
     *
     * @param startDate Start timestamp (inclusive)
     * @param endDate End timestamp (inclusive)
     * @return Average spending per day
     */
    suspend fun getAverageDailySpending(startDate: Long, endDate: Long): Double =
        expenseDao.getAverageDailySpending(startDate, endDate)

    // ============================================================================
    // BUDGET-SALARY INTEGRATION METHODS - Added for Sprint 7
    // These methods support financial health calculations and budget tracking
    // ============================================================================

    /**
     * Get total budget limit for a specific date range.
     * Sums up all active budgets that apply to the period.
     *
     * @param startDate Start timestamp (inclusive)
     * @param endDate End timestamp (inclusive)
     * @return Total budget limit for the period
     */
    suspend fun getTotalBudgetForPeriod(startDate: Long, endDate: Long): Double {
        // For now, sum all active monthly budgets
        // In a more sophisticated implementation, we'd filter by period type
        val budgets = budgetDao.getAllActiveBudgets().firstOrNull() ?: emptyList()
        return budgets
            .filter { it.period == BudgetPeriod.MONTHLY }
            .sumOf { it.limitAmount }
    }

    /**
     * Get spending against a specific budget.
     * Calculates how much has been spent in the budget's scope.
     *
     * @param budget The budget to check
     * @param startDate Start of the period
     * @param endDate End of the period
     * @return Amount spent against this budget
     */
    suspend fun getSpendingForBudget(budget: Budget, startDate: Long, endDate: Long): Double {
        return when {
            // Category-specific budget
            budget.categoryId != null -> {
                expenseDao.getTotalByCategoryAndDateRange(budget.categoryId, startDate, endDate)
            }
            // Project-specific budget
            budget.projectId != null -> {
                expenseDao.getTotalByProjectAndDateRange(budget.projectId, startDate, endDate)
            }
            // Global budget (all expenses)
            else -> {
                expenseDao.getTotalByDateRange(startDate, endDate)
            }
        }
    }

    /**
     * Calculate savings for a period.
     * Savings = Income (Salary) - Expenses
     *
     * @param month Month (1-12)
     * @param year Year (e.g., 2024)
     * @return Savings amount (can be negative if overspent)
     */
    suspend fun calculateSavingsForMonth(month: Int, year: Int): Double {
        val salaryRecord = getCurrentMonthSalary(month, year).firstOrNull()
        val salary = salaryRecord?.amount ?: 0.0
        
        // Get expenses for this month
        val calendar = java.util.Calendar.getInstance()
        calendar.set(year, month - 1, 1, 0, 0, 0)
        val startDate = calendar.timeInMillis
        
        calendar.set(year, month - 1, calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH), 23, 59, 59)
        val endDate = calendar.timeInMillis
        
        val expenses = getTotalByDateRange(startDate, endDate)
        
        return salary - expenses
    }
}