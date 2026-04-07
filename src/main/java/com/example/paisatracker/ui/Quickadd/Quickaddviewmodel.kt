package com.example.paisatracker.ui.quickadd

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.paisatracker.data.Category
import com.example.paisatracker.data.Expense
import com.example.paisatracker.data.PaisaTrackerRepository
import com.example.paisatracker.data.Project
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// ── Submission result sealed class ────────────────────────────────────────────
sealed class QuickAddResult {
    object Idle : QuickAddResult()
    object Loading : QuickAddResult()
    object Success : QuickAddResult()
    data class Error(val message: String) : QuickAddResult()
}

// ── Payment methods ────────────────────────────────────────────────────────────
val PAYMENT_METHODS = listOf("Cash", "UPI" ,"Paytm","PhonePe","GPay","Card", "Other")



class QuickAddViewModel(
    private val repository: PaisaTrackerRepository
) : ViewModel() {

    // ── Form fields ───────────────────────────────────────────────────────────
    val amountText      = MutableStateFlow("")
    val description     = MutableStateFlow("")
    val selectedProject = MutableStateFlow<Project?>(null)
    val selectedCategory= MutableStateFlow<Category?>(null)
    val paymentMethod   = MutableStateFlow("UPI")
    val selectedDate    = MutableStateFlow(System.currentTimeMillis())

    // ── New-category inline creation ──────────────────────────────────────────
    val isCreatingCategory  = MutableStateFlow(false)
    val newCategoryName     = MutableStateFlow("")
    val newCategoryEmoji    = MutableStateFlow("📂")

    // ── Submission state ──────────────────────────────────────────────────────
    val submitResult = MutableStateFlow<QuickAddResult>(QuickAddResult.Idle)

    // ── Source data from DB ───────────────────────────────────────────────────
    val allProjects: StateFlow<List<Project>> = repository.getAllProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories: StateFlow<List<Category>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Categories filtered to selected project
    val filteredCategories: StateFlow<List<Category>> = combine(
        allCategories, selectedProject
    ) { cats, proj ->
        if (proj == null) emptyList()
        else cats.filter { it.projectId == proj.id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Recent projects/categories (last 5 unique, derived from allCategories ordering) ──
    // We use a simple in-memory recent list populated when the sheet opens.
    // Real recents would need a usage-log table — for now we show all projects
    // sorted by lastModified (already the natural DB order).
    val recentProjects: StateFlow<List<Project>> = repository.getAllProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Recent categories = last 5 categories across all projects (by id desc = newest created)
    val recentCategories: StateFlow<List<Category>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Auto-select logic: when project selected, auto-pick its first category ──
    fun onProjectSelected(project: Project) {
        selectedProject.value = project
        selectedCategory.value = null   // reset category when project changes
        isCreatingCategory.value = false
        newCategoryName.value = ""
    }

    fun onCategorySelected(category: Category) {
        selectedCategory.value = category
        isCreatingCategory.value = false
    }

    fun startCreatingCategory() {
        isCreatingCategory.value = true
        newCategoryName.value = ""
        newCategoryEmoji.value = "📂"
    }

    fun cancelCreatingCategory() {
        isCreatingCategory.value = false
        newCategoryName.value = ""
    }

    fun confirmNewCategory() {
        val project = selectedProject.value ?: return
        val name    = newCategoryName.value.trim()
        if (name.isBlank()) return

        viewModelScope.launch {
            val newCat = Category(
                name      = name,
                emoji     = newCategoryEmoji.value,
                projectId = project.id
            )
            val newId = repository.insertCategory(newCat)
            // Auto-select the just-created category
            selectedCategory.value = newCat.copy(id = newId)
            isCreatingCategory.value = false
            newCategoryName.value = ""
        }
    }

    // ── Form validation ───────────────────────────────────────────────────────
    val isFormValid: StateFlow<Boolean> = combine(
        amountText, selectedProject, selectedCategory
    ) { amount, project, category ->
        amount.toDoubleOrNull()?.let { it > 0 } == true
                && project != null
                && category != null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // ── Submit ────────────────────────────────────────────────────────────────
    fun submit(onSuccess: () -> Unit) {
        val amount   = amountText.value.toDoubleOrNull() ?: return
        val category = selectedCategory.value ?: return

        submitResult.value = QuickAddResult.Loading

        viewModelScope.launch {
            try {
                val expense = Expense(
                    amount        = amount,
                    description   = description.value.trim().ifBlank { "Quick expense" },
                    date          = selectedDate.value,
                    categoryId    = category.id,
                    paymentMethod = paymentMethod.value
                )
                repository.insertExpense(expense)
                submitResult.value = QuickAddResult.Success
                onSuccess()
            } catch (e: Exception) {
                submitResult.value = QuickAddResult.Error(e.message ?: "Failed to save")
            }
        }
    }

    // ── Reset form for next use ───────────────────────────────────────────────
    fun reset() {
        amountText.value       = ""
        description.value      = ""
        selectedCategory.value = null
        paymentMethod.value    = "UPI"
        selectedDate.value     = System.currentTimeMillis()
        submitResult.value     = QuickAddResult.Idle
        isCreatingCategory.value = false
        newCategoryName.value  = ""
        // Keep selectedProject — user likely adds to same project consecutively
    }
}

// ── Factory ───────────────────────────────────────────────────────────────────
class QuickAddViewModelFactory(
    private val repository: PaisaTrackerRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(QuickAddViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return QuickAddViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}