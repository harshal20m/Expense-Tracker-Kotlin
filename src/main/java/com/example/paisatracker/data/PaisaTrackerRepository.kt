package com.example.paisatracker.data

import kotlinx.coroutines.flow.Flow

class PaisaTrackerRepository(
    private val projectDao: ProjectDao,
    private val categoryDao: CategoryDao,
    private val expenseDao: ExpenseDao,
    private val assetDao: AssetDao,
    private val backupDao: BackupDao,
    private val budgetDao: BudgetDao,
    private val flapDao: FlapDao,
    private val upiTransactionDao: UpiTransactionDao,
    private val pendingTransactionDao: PendingTransactionDao,
    private val salaryRecordDao: SalaryRecordDao

) {

    //pending transactions
    fun getAllPendingTransactions() = pendingTransactionDao.getAllPending()
 fun getUnreviewedPendingCount() = pendingTransactionDao.getUnreviewedCount()
 suspend fun insertPendingTransaction(t: PendingTransaction): Long = pendingTransactionDao.insert(t)
 suspend fun updatePendingTransaction(t: PendingTransaction) = pendingTransactionDao.update(t)
 suspend fun deletePendingTransaction(t: PendingTransaction) = pendingTransactionDao.delete(t)
 suspend fun countPendingByUtr(utr: String): Int = pendingTransactionDao.countByUtr(utr)
 suspend fun countExpenseByUtr(utr: String): Int = pendingTransactionDao.countExpenseByUtr(utr)

 // ── Salary records ────────────────────────────────────────────
 fun getCurrentMonthSalary(month: Int, year: Int) = salaryRecordDao.getCurrentMonthRecord(month, year)
 fun getAllSalaryRecords() = salaryRecordDao.getAllRecords()
 fun getTotalSpentSince(ts: Long) = salaryRecordDao.getTotalSpentSince(ts)
 fun getCategoryBreakdownSince(ts: Long) = salaryRecordDao.getCategoryBreakdownSince(ts)
 suspend fun insertSalaryRecord(r: SalaryRecord): Long = salaryRecordDao.insert(r)
 suspend fun updateSalaryRecord(r: SalaryRecord) = salaryRecordDao.update(r)
 suspend fun deleteSalaryRecord(r: SalaryRecord) = salaryRecordDao.delete(r)

    //upi scanner
    suspend fun insertUpiTransaction(txn: UpiTransaction): Long =
        upiTransactionDao.insert(txn)

    suspend fun updateUpiTransactionStatus(
        id: Long, status: UpiStatus,
        txnId: String?, code: String?, raw: String?
    ) = upiTransactionDao.updateStatus(id, status, txnId, code, raw)

    fun getAllUpiTransactions() = upiTransactionDao.getAllTransactions()

    fun getUpiTransactionByExpenseId(expenseId: Long): Flow<UpiTransaction?> = upiTransactionDao.getByExpenseIdAsFlow(expenseId)

    suspend fun deleteExpenseById(expenseId: Long) {
        expenseDao.deleteById(expenseId)
    }

    //flapmethods
    // b) Add these methods:

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

    fun getAllProjects(): Flow<List<Project>> = projectDao.getAllProjects()

    // Add this method for seeding
    suspend fun getAllProjectsList(): List<Project> {
        return projectDao.getAllProjectsList()
    }

    fun getProjectById(projectId: Long): Flow<Project> = projectDao.getProjectById(projectId)

    fun getCategoryById(categoryId: Long): Flow<Category> = categoryDao.getCategoryById(categoryId)

     suspend fun getCategoriesForProjectList(projectId: Long): List<Category> {
         return categoryDao.getCategoriesForProjectList(projectId)
     }
    fun getAllProjectsWithTotal(): Flow<List<ProjectWithTotal>> = projectDao.getAllProjectsWithTotal()

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

    fun getExpenseById(id: Long): Flow<Expense?> = expenseDao.getExpenseById(id)

    suspend fun insertExpense(expense: Expense): Long {
        return expenseDao.insert(expense)
    }

    suspend fun updateExpense(expense: Expense) {
        expenseDao.updateExpense(expense)
    }

    suspend fun deleteExpense(expense: Expense) {
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

    fun searchExpensesByAmount(minAmount: Double?, maxAmount: Double?, projectId: Long?): Flow<List<RecentExpense>> {
        return expenseDao.searchExpensesByAmount(minAmount, maxAmount, projectId)
    }

    fun searchExpensesByDateRange(startDate: Long?, endDate: Long?, projectId: Long?): Flow<List<RecentExpense>> {
        return expenseDao.searchExpensesByDateRange(startDate, endDate, projectId)
    }
}