package com.example.paisatracker.ui.salary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.paisatracker.data.BankAccount
import com.example.paisatracker.data.CategorySpend
import com.example.paisatracker.data.MonthlySalarySummary
import com.example.paisatracker.data.PaisaTrackerRepository
import com.example.paisatracker.data.SalaryRecord
import com.example.paisatracker.data.SalarySourceType
import com.example.paisatracker.PaisaTrackerViewModel
import com.example.paisatracker.ui.common.ToastType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Enhanced SalaryViewModel with multi-salary support.
 *
 * Supports multiple income sources per month (e.g., primary job + freelance).
 * Each salary is linked to a specific bank account and can have different source types.
 */
class SalaryViewModel(
    private val repository: PaisaTrackerRepository,
    private val globalViewModel: PaisaTrackerViewModel
) : ViewModel() {

    private val calendar get() = Calendar.getInstance()
    private val currentMonth get() = calendar.get(Calendar.MONTH) + 1  // 1-12
    private val currentYear  get() = calendar.get(Calendar.YEAR)

    // ══════════════════════════════════════════════════════════════════════════
    // MULTI-SALARY STATE
    // ══════════════════════════════════════════════════════════════════════════

    /** All active salaries for current month */
    val currentMonthSalaries: StateFlow<List<SalaryRecord>> =
        repository.getCurrentMonthSalaries(currentMonth, currentYear)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Total income from all salaries this month */
    val totalMonthlyIncome: StateFlow<Double> =
        repository.getTotalMonthlyIncome(currentMonth, currentYear)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    /** Total spent since earliest salary was received this month */
    val totalSpentThisMonth: StateFlow<Double> = currentMonthSalaries
        .flatMapLatest { salaries ->
            if (salaries.isEmpty()) flowOf(0.0)
            else {
                val earliestTimestamp = salaries.minOf { it.receivedAt }
                repository.getTotalSpentSince(earliestTimestamp)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    /** Remaining balance across all salaries */
    val remainingBalance: StateFlow<Double> =
        combine(totalMonthlyIncome, totalSpentThisMonth) { income, spent ->
            income - spent
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    /** Spend percentage across all salaries (0..1) */
    val spendPercentage: StateFlow<Float> =
        combine(totalMonthlyIncome, totalSpentThisMonth) { income, spent ->
            if (income <= 0) 0f
            else (spent / income).toFloat().coerceIn(0f, 1f)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    /** Category breakdown since earliest salary */
    val categoryBreakdown: StateFlow<List<CategorySpend>> = currentMonthSalaries
        .flatMapLatest { salaries ->
            if (salaries.isEmpty()) flowOf(emptyList())
            else {
                val earliestTimestamp = salaries.minOf { it.receivedAt }
                repository.getCategoryBreakdownSince(earliestTimestamp)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Monthly summary with all salaries and spending */
    val monthlySummary: StateFlow<MonthlySalarySummary?> = flow {
        emit(repository.getMonthlySalarySummary(currentMonth, currentYear))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // ══════════════════════════════════════════════════════════════════════════
    // LEGACY STATE (for backward compatibility)
    // ══════════════════════════════════════════════════════════════════════════

    /** @deprecated Use currentMonthSalaries instead */
    val currentSalary: StateFlow<SalaryRecord?> =
        repository.getCurrentMonthSalary(currentMonth, currentYear)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // ══════════════════════════════════════════════════════════════════════════
    // COMMON STATE
    // ══════════════════════════════════════════════════════════════════════════

    val allSalaryRecords: StateFlow<List<SalaryRecord>> = repository.getAllSalaryRecords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeBankAccounts: StateFlow<List<BankAccount>> = repository.getActiveBankAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        ensureRecurringSalariesForCurrentMonth()
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ACTIONS
    // ══════════════════════════════════════════════════════════════════════════

    /** Auto-create all recurring salaries for current month */
    fun ensureRecurringSalariesForCurrentMonth() {
        viewModelScope.launch {
            try {
                val generatedSalaries = repository.autoCreateRecurringSalariesForMonth(
                    month = currentMonth,
                    year = currentYear
                )
                
                if (generatedSalaries.isNotEmpty()) {
                    val message = if (generatedSalaries.size == 1) {
                        val account = repository.getBankAccountByIdOnce(generatedSalaries[0].linkedAccountId)
                        "Recurring salary added to ${account?.name ?: "account"}"
                    } else {
                        "${generatedSalaries.size} recurring salaries added"
                    }
                    globalViewModel.showToast(message, ToastType.SUCCESS)
                }
            } catch (_: Exception) {
                // Silent fail
            }
        }
    }

    /** Add a new salary with required account link */
    fun addSalary(
        amount: Double,
        linkedAccountId: Long,
        sourceName: String = "",
        sourceType: String = SalarySourceType.PRIMARY,
        note: String = "",
        isRecurring: Boolean = false
    ) {
        viewModelScope.launch {
            val salary = SalaryRecord(
                amount = amount,
                month = currentMonth,
                year = currentYear,
                note = note.trim(),
                receivedAt = System.currentTimeMillis(),
                linkedAccountId = linkedAccountId,
                sourceName = sourceName.trim(),
                sourceType = sourceType,
                isRecurring = isRecurring,
                autoGenerated = false
            )
            
            repository.insertSalaryAndCreditAccount(salary)
            
            val account = repository.getBankAccountByIdOnce(linkedAccountId)
            val message = if (isRecurring) {
                "Recurring salary added to ${account?.name}"
            } else {
                "Salary added to ${account?.name}"
            }
            globalViewModel.showToast(message, ToastType.SUCCESS)
        }
    }

    /** Update an existing salary */
    fun updateSalary(record: SalaryRecord) {
        viewModelScope.launch {
            repository.updateSalaryRecord(record)
            globalViewModel.showToast("Salary updated")
        }
    }

    /** Delete a salary and deduct from account balance */
    fun deleteSalary(record: SalaryRecord) {
        viewModelScope.launch {
            // Deduct from account balance
            if (record.linkedAccountId > 0) {
                repository.incrementBankAccountBalance(record.linkedAccountId, -record.amount)
            }
            
            globalViewModel.recordDeletion("SALARY_RECORD", record)
            repository.deleteSalaryRecord(record)
            globalViewModel.showToast(
                "Salary deleted",
                ToastType.UNDO,
                onUndo = { globalViewModel.undoLastAction() }
            )
        }
    }

    /** Get salaries for a specific account */
    fun getSalariesForAccount(accountId: Long) =
        repository.getSalariesForAccount(accountId, currentMonth, currentYear)

    // ══════════════════════════════════════════════════════════════════════════
    // LEGACY METHODS (for backward compatibility)
    // ══════════════════════════════════════════════════════════════════════════

    /** @deprecated Use ensureRecurringSalariesForCurrentMonth() instead */
    fun ensureRecurringSalaryForCurrentMonth() {
        ensureRecurringSalariesForCurrentMonth()
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
