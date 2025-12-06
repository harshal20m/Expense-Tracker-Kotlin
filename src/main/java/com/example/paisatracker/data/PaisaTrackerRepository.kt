package com.example.paisatracker.data

import kotlinx.coroutines.flow.Flow

class PaisaTrackerRepository(
    private val projectDao: ProjectDao,
    private val categoryDao: CategoryDao,
    private val expenseDao: ExpenseDao,
    private val assetDao: AssetDao
) {
    fun getAllProjects(): Flow<List<Project>> = projectDao.getAllProjects()

    fun getProjectById(projectId: Long): Flow<Project> = projectDao.getProjectById(projectId)

    fun getCategoryById(categoryId: Long): Flow<Category> = categoryDao.getCategoryById(categoryId)

    fun getAllProjectsWithTotal(): Flow<List<ProjectWithTotal>> = projectDao.getAllProjectsWithTotal()

    fun getCategoryExpenses(projectId: Long): Flow<List<CategoryExpense>> = projectDao.getCategoryExpenses(projectId)

    fun getCategoriesWithTotalForProject(projectId: Long): Flow<List<CategoryWithTotal>> = categoryDao.getCategoriesWithTotalForProject(projectId)

    fun getAllExpenses(): Flow<List<Expense>> = expenseDao.getAllExpenses()

    suspend fun getExpensesForExport(projectId: Long): List<ExpenseExport> = expenseDao.getExpensesForExport(projectId)

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




    // Asset methods latest added ***
    // Add these methods
//    fun getAllAssets(): Flow<List<Asset>> {
//        return assetDao.getAllAssets()
//    }
//
//    suspend fun insertAsset(asset: Asset): Long {
//        return assetDao.insertAsset(asset)
//    }
//
//    suspend fun updateAsset(asset: Asset) {
//        assetDao.updateAsset(asset)
//    }
//
//    suspend fun deleteAsset(asset: Asset) {
//        assetDao.deleteAsset(asset)
//    }
//
//    suspend fun deleteAllAssets() {
//        assetDao.deleteAllAssets()
//    }
//



    // Asset functions
    fun getAllAssets(): Flow<List<Asset>> = assetDao.getAllAssets()
    fun getAssetsForExpense(expenseId: Long): Flow<List<Asset>> = assetDao.getAssetsForExpense(expenseId)
    fun getIndependentAssets(): Flow<List<Asset>> = assetDao.getIndependentAssets()

    suspend fun insertAsset(asset: Asset) = assetDao.insertAsset(asset)
    suspend fun deleteAsset(asset: Asset) = assetDao.deleteAsset(asset)

}
