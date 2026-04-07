// DataSeeder.kt
package com.example.paisatracker.data

import android.content.Context
import androidx.core.content.edit

class DataSeeder(private val repository: PaisaTrackerRepository) {

    companion object {
        private const val PREFS_NAME = "app_prefs"
        private const val KEY_DATA_SEEDED = "data_seeded"
        private const val KEY_USER_CHOICE = "user_choice"

        @Volatile
        private var INSTANCE: DataSeeder? = null

        fun getInstance(repository: PaisaTrackerRepository): DataSeeder {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DataSeeder(repository).also { INSTANCE = it }
            }
        }
    }

    fun shouldShowFirstTimeSetup(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return !prefs.getBoolean(KEY_DATA_SEEDED, false)
    }

    suspend fun seedInitialDataIfUserAccepts(context: Context, shouldSeed: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        if (shouldSeed) {
            seedProjects()
            seedCategories()
        }

        // Mark as seeded regardless of choice (so dialog doesn't show again)
        prefs.edit {
            putBoolean(KEY_DATA_SEEDED, true)
            putBoolean(KEY_USER_CHOICE, shouldSeed)
        }
    }

    private suspend fun seedProjects() {

        val existingProjects = repository.getAllProjectsList()
        val existingProjectNames = existingProjects.map { it.name }.toSet()

        val defaultProjects = listOf(
            Project(name = "Daily Living", emoji = "🏠", createdAt = System.currentTimeMillis()),
            Project(name = "Food & Dining", emoji = "🍔", createdAt = System.currentTimeMillis()),
            Project(name = "Transportation", emoji = "🚗", createdAt = System.currentTimeMillis()),
            Project(name = "Shopping", emoji = "🛍️", createdAt = System.currentTimeMillis()),
            Project(name = "Entertainment", emoji = "🎬", createdAt = System.currentTimeMillis()),
            Project(name = "Bills & Utilities", emoji = "💡", createdAt = System.currentTimeMillis()),
            Project(name = "Health & Wellness", emoji = "💊", createdAt = System.currentTimeMillis()),
            Project(name = "Education", emoji = "📚", createdAt = System.currentTimeMillis())
        )

        // Only insert projects that don't already exist
        defaultProjects.forEach { project ->
            if (project.name !in existingProjectNames) {
                repository.insertProject(project)
            }
        }
    }

    private suspend fun seedCategories() {
        // Get all projects (you'll need to add a suspend function to get all projects)
        val projects = repository.getAllProjectsList()
        val projectMap = projects.associateBy { it.name }

        // Get existing categories to avoid duplicates
        val existingCategories = mutableSetOf<String>()
        projects.forEach { project ->
            val categories = repository.getCategoriesForProjectList(project.id)
            existingCategories.addAll(categories.map { "${project.name}:${it.name}" })
        }
        val defaultCategories = mutableListOf<Category>()

        // Helper to add category if not exists
        fun addCategoryIfNotExists(projectName: String, categoryName: String, emoji: String) {
            val key = "$projectName:$categoryName"
            if (key !in existingCategories) {
                projectMap[projectName]?.let { project ->
                    defaultCategories.add(
                        Category(
                            projectId = project.id,
                            name = categoryName,
                            emoji = emoji,
                            createdAt = System.currentTimeMillis()
                        )
                    )
                }
            }
        }

        // Daily Living categories
        addCategoryIfNotExists("Daily Living", "Groceries", "🛒")
        addCategoryIfNotExists("Daily Living", "Household Items", "🧹")
        addCategoryIfNotExists("Daily Living", "Personal Care", "🧴")

        // Food & Dining categories
        addCategoryIfNotExists("Food & Dining", "Restaurants", "🍽️")
        addCategoryIfNotExists("Food & Dining", "Coffee & Snacks", "☕")
        addCategoryIfNotExists("Food & Dining", "Takeout", "🥡")

        // Transportation categories
        addCategoryIfNotExists("Transportation", "Fuel", "⛽")
        addCategoryIfNotExists("Transportation", "Public Transit", "🚌")
        addCategoryIfNotExists("Transportation", "Ride Share", "🚕")
        addCategoryIfNotExists("Transportation", "Parking", "🅿️")

        // Shopping categories
        addCategoryIfNotExists("Shopping", "Clothing", "👕")
        addCategoryIfNotExists("Shopping", "Electronics", "📱")
        addCategoryIfNotExists("Shopping", "Gifts", "🎁")

        // Entertainment categories
        addCategoryIfNotExists("Entertainment", "Movies", "🎬")
        addCategoryIfNotExists("Entertainment", "Streaming Services", "📺")
        addCategoryIfNotExists("Entertainment", "Games", "🎮")
        addCategoryIfNotExists("Entertainment", "Events & Concerts", "🎵")

        // Bills & Utilities categories
        addCategoryIfNotExists("Bills & Utilities", "Electricity", "⚡")
        addCategoryIfNotExists("Bills & Utilities", "Water", "💧")
        addCategoryIfNotExists("Bills & Utilities", "Internet", "🌐")
        addCategoryIfNotExists("Bills & Utilities", "Mobile", "📱")
        addCategoryIfNotExists("Bills & Utilities", "Rent", "🏠")

        // Health & Wellness categories
        addCategoryIfNotExists("Health & Wellness", "Pharmacy", "💊")
        addCategoryIfNotExists("Health & Wellness", "Doctor Visits", "👨‍⚕️")
        addCategoryIfNotExists("Health & Wellness", "Gym", "🏋️")
        addCategoryIfNotExists("Health & Wellness", "Insurance", "🛡️")

        // Education categories
        addCategoryIfNotExists("Education", "Books", "📖")
        addCategoryIfNotExists("Education", "Courses", "🎓")
        addCategoryIfNotExists("Education", "Supplies", "✏️")


        // Daily Living categories
        projectMap["Daily Living"]?.let { project ->
            defaultCategories.addAll(
                listOf(
                    Category(projectId = project.id, name = "Groceries", emoji = "🛒", createdAt = System.currentTimeMillis()),
                    Category(projectId = project.id, name = "Household Items", emoji = "🧹", createdAt = System.currentTimeMillis()),
                    Category(projectId = project.id, name = "Personal Care", emoji = "🧴", createdAt = System.currentTimeMillis())
                )
            )
        }

        // Food & Dining categories
        projectMap["Food & Dining"]?.let { project ->
            defaultCategories.addAll(
                listOf(
                    Category(projectId = project.id, name = "Restaurants", emoji = "🍽️", createdAt = System.currentTimeMillis()),
                    Category(projectId = project.id, name = "Coffee & Snacks", emoji = "☕", createdAt = System.currentTimeMillis()),
                    Category(projectId = project.id, name = "Takeout", emoji = "🥡", createdAt = System.currentTimeMillis())
                )
            )
        }

        // Transportation categories
        projectMap["Transportation"]?.let { project ->
            defaultCategories.addAll(
                listOf(
                    Category(projectId = project.id, name = "Fuel", emoji = "⛽", createdAt = System.currentTimeMillis()),
                    Category(projectId = project.id, name = "Public Transit", emoji = "🚌", createdAt = System.currentTimeMillis()),
                    Category(projectId = project.id, name = "Ride Share", emoji = "🚕", createdAt = System.currentTimeMillis()),
                    Category(projectId = project.id, name = "Parking", emoji = "🅿️", createdAt = System.currentTimeMillis())
                )
            )
        }

        // Shopping categories
        projectMap["Shopping"]?.let { project ->
            defaultCategories.addAll(
                listOf(
                    Category(projectId = project.id, name = "Clothing", emoji = "👕", createdAt = System.currentTimeMillis()),
                    Category(projectId = project.id, name = "Electronics", emoji = "📱", createdAt = System.currentTimeMillis()),
                    Category(projectId = project.id, name = "Gifts", emoji = "🎁", createdAt = System.currentTimeMillis())
                )
            )
        }

        // Entertainment categories
        projectMap["Entertainment"]?.let { project ->
            defaultCategories.addAll(
                listOf(
                    Category(projectId = project.id, name = "Movies", emoji = "🎬", createdAt = System.currentTimeMillis()),
                    Category(projectId = project.id, name = "Streaming Services", emoji = "📺", createdAt = System.currentTimeMillis()),
                    Category(projectId = project.id, name = "Games", emoji = "🎮", createdAt = System.currentTimeMillis()),
                    Category(projectId = project.id, name = "Events & Concerts", emoji = "🎵", createdAt = System.currentTimeMillis())
                )
            )
        }

        // Bills & Utilities categories
        projectMap["Bills & Utilities"]?.let { project ->
            defaultCategories.addAll(
                listOf(
                    Category(projectId = project.id, name = "Electricity", emoji = "⚡", createdAt = System.currentTimeMillis()),
                    Category(projectId = project.id, name = "Water", emoji = "💧", createdAt = System.currentTimeMillis()),
                    Category(projectId = project.id, name = "Internet", emoji = "🌐", createdAt = System.currentTimeMillis()),
                    Category(projectId = project.id, name = "Mobile", emoji = "📱", createdAt = System.currentTimeMillis()),
                    Category(projectId = project.id, name = "Rent", emoji = "🏠", createdAt = System.currentTimeMillis())
                )
            )
        }

        // Health & Wellness categories
        projectMap["Health & Wellness"]?.let { project ->
            defaultCategories.addAll(
                listOf(
                    Category(projectId = project.id, name = "Pharmacy", emoji = "💊", createdAt = System.currentTimeMillis()),
                    Category(projectId = project.id, name = "Doctor Visits", emoji = "👨‍⚕️", createdAt = System.currentTimeMillis()),
                    Category(projectId = project.id, name = "Gym", emoji = "🏋️", createdAt = System.currentTimeMillis()),
                    Category(projectId = project.id, name = "Insurance", emoji = "🛡️", createdAt = System.currentTimeMillis())
                )
            )
        }

        // Education categories
        projectMap["Education"]?.let { project ->
            defaultCategories.addAll(
                listOf(
                    Category(projectId = project.id, name = "Books", emoji = "📖", createdAt = System.currentTimeMillis()),
                    Category(projectId = project.id, name = "Courses", emoji = "🎓", createdAt = System.currentTimeMillis()),
                    Category(projectId = project.id, name = "Supplies", emoji = "✏️", createdAt = System.currentTimeMillis())
                )
            )
        }

        defaultCategories.forEach { category ->
            repository.insertCategory(category)
        }
    }
}