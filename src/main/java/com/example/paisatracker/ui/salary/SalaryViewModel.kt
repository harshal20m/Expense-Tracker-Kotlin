package com.example.paisatracker.ui.salary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.paisatracker.data.CategorySpend
import com.example.paisatracker.data.PaisaTrackerRepository
import com.example.paisatracker.data.SalaryRecord
import com.example.paisatracker.PaisaTrackerViewModel
import com.example.paisatracker.ui.common.ToastType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class SalaryViewModel(
    private val repository: PaisaTrackerRepository,
    private val globalViewModel: PaisaTrackerViewModel
) : ViewModel() {

    private val calendar get() = Calendar.getInstance()
    private val currentMonth get() = calendar.get(Calendar.MONTH) + 1  // 1-12
    private val currentYear  get() = calendar.get(Calendar.YEAR)

    // ── Current month salary record ───────────────────────────────────────────
    val currentSalary: StateFlow<SalaryRecord?> =
        repository.getCurrentMonthSalary(currentMonth, currentYear)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // ── Total spent since salary was received this month ──────────────────────
    val totalSpentThisMonth: StateFlow<Double> = currentSalary
        .flatMapLatest { salary ->
            if (salary == null) flowOf(0.0)
            else repository.getTotalSpentSince(salary.receivedAt)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // ── Remaining balance ─────────────────────────────────────────────────────
    val remainingBalance: StateFlow<Double> = combine(currentSalary, totalSpentThisMonth) { salary, spent ->
        (salary?.amount ?: 0.0) - spent
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // ── Spend percentage (0..1) ───────────────────────────────────────────────
    val spendPercentage: StateFlow<Float> = combine(currentSalary, totalSpentThisMonth) { salary, spent ->
        if (salary == null || salary.amount <= 0) 0f
        else (spent / salary.amount).toFloat().coerceIn(0f, 1f)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    // ── Category breakdown ────────────────────────────────────────────────────
    val categoryBreakdown: StateFlow<List<CategorySpend>> = currentSalary
        .flatMapLatest { salary ->
            if (salary == null) flowOf(emptyList())
            else repository.getCategoryBreakdownSince(salary.receivedAt)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Salary history ────────────────────────────────────────────────────────
    val allSalaryRecords: StateFlow<List<SalaryRecord>> = repository.getAllSalaryRecords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Add / update salary ───────────────────────────────────────────────────
    fun addSalary(amount: Double, note: String) {
        viewModelScope.launch {
            repository.insertSalaryRecord(
                SalaryRecord(
                    amount     = amount,
                    month      = currentMonth,
                    year       = currentYear,
                    note       = note.trim(),
                    receivedAt = System.currentTimeMillis()
                )
            )
            globalViewModel.showToast("Salary added successfully")
        }
    }

    fun updateSalary(record: SalaryRecord) {
        viewModelScope.launch {
            repository.updateSalaryRecord(record)
            globalViewModel.showToast("Salary updated")
        }
    }

    fun deleteSalary(record: SalaryRecord) {
        viewModelScope.launch {
            repository.deleteSalaryRecord(record)
            globalViewModel.showToast("Salary deleted", ToastType.INFO)
        }
    }
}

class SalaryViewModelFactory(
    private val repository: PaisaTrackerRepository,
    private val globalViewModel: PaisaTrackerViewModel
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return SalaryViewModel(repository, globalViewModel) as T
    }
}
