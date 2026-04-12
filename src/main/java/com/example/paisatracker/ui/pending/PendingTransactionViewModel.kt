package com.example.paisatracker.ui.pending

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.paisatracker.data.Category
import com.example.paisatracker.data.Expense
import com.example.paisatracker.data.PaisaTrackerRepository
import com.example.paisatracker.data.PendingTransaction
import com.example.paisatracker.data.Project
import com.example.paisatracker.data.UpiStatus
import com.example.paisatracker.data.UpiTransaction
import com.example.paisatracker.service.UpiNotificationListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PendingTransactionViewModel(
    private val repository: PaisaTrackerRepository,
    private val prefs: SharedPreferences
) : ViewModel() {

    // ── Settings state ────────────────────────────────────────────────────────
    val isListenerEnabled  = MutableStateFlow(prefs.getBoolean(UpiNotificationListener.KEY_ENABLED,         false))
    val reviewBeforeSave   = MutableStateFlow(prefs.getBoolean(UpiNotificationListener.KEY_REVIEW_FIRST,    true))
    val skipDuplicates     = MutableStateFlow(prefs.getBoolean(UpiNotificationListener.KEY_SKIP_DUPLICATES, true))
    val filterGPay         = MutableStateFlow(prefs.getBoolean(UpiNotificationListener.KEY_FILTER_GPAY,     true))
    val filterPhonePe      = MutableStateFlow(prefs.getBoolean(UpiNotificationListener.KEY_FILTER_PHONEPE,  true))
    val filterPaytm        = MutableStateFlow(prefs.getBoolean(UpiNotificationListener.KEY_FILTER_PAYTM,    true))
    val filterBhim         = MutableStateFlow(prefs.getBoolean(UpiNotificationListener.KEY_FILTER_BHIM,     false))
    val filterAmazon       = MutableStateFlow(prefs.getBoolean(UpiNotificationListener.KEY_FILTER_AMAZON,   false))
    val filterBankSms      = MutableStateFlow(prefs.getBoolean(UpiNotificationListener.KEY_FILTER_BANK_SMS, true))

    // ── Pending list ──────────────────────────────────────────────────────────
    val pendingList: StateFlow<List<PendingTransaction>> = repository.getAllPendingTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreviewedCount: StateFlow<Int> = repository.getUnreviewedPendingCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // ── Project / category data ───────────────────────────────────────────────
    val allProjects: StateFlow<List<Project>> = repository.getAllProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories: StateFlow<List<Category>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Settings toggles ──────────────────────────────────────────────────────

    fun setListenerEnabled(enabled: Boolean) {
        isListenerEnabled.value = enabled
        prefs.edit().putBoolean(UpiNotificationListener.KEY_ENABLED, enabled).apply()
    }

    fun setReviewBeforeSave(v: Boolean) {
        reviewBeforeSave.value = v
        prefs.edit().putBoolean(UpiNotificationListener.KEY_REVIEW_FIRST, v).apply()
    }

    fun setSkipDuplicates(v: Boolean) {
        skipDuplicates.value = v
        prefs.edit().putBoolean(UpiNotificationListener.KEY_SKIP_DUPLICATES, v).apply()
    }

    fun setFilter(key: String, flow: MutableStateFlow<Boolean>, v: Boolean) {
        flow.value = v
        prefs.edit().putBoolean(key, v).apply()
    }

    // ── Actions on individual pending transactions ────────────────────────────

    fun discard(txn: PendingTransaction) {
        viewModelScope.launch { repository.deletePendingTransaction(txn) }
    }

    fun discardAll() {
        viewModelScope.launch {
            pendingList.value.forEach { repository.deletePendingTransaction(it) }
        }
    }

    /**
     * Save a single pending transaction as an Expense (+ UpiTransaction if VPA exists).
     * The pending record is deleted on success.
     */
    fun saveAsExpense(
        txn: PendingTransaction,
        categoryId: Long,
        projectId: Long,
        note: String,
        amount: Double,
        payeeName: String
    ) {
        viewModelScope.launch {
            try {
                val payIcon = when (txn.sourceApp) {
                    "GPay"     -> "gpay"
                    "PhonePe"  -> "phonepe"
                    "Paytm"    -> "paytm"
                    else       -> "upi"
                }
                val description = buildString {
                    append(payeeName.ifBlank { txn.payeeName }.ifBlank { "UPI Payment" })
                    if (note.isNotBlank()) append(" — $note")
                    if (txn.utrNumber.isNotBlank()) append(" · UTR ${txn.utrNumber}")
                }
                val expense = Expense(
                    amount        = amount,
                    description   = description,
                    date          = txn.capturedAt,
                    categoryId    = categoryId,
                    paymentMethod = txn.sourceApp,
                    paymentIcon   = payIcon
                )
                val expenseId = repository.insertExpense(expense)

                if (txn.payeeVpa.isNotBlank() || txn.utrNumber.isNotBlank()) {
                    repository.insertUpiTransaction(
                        UpiTransaction(
                            expenseId       = expenseId,
                            vpa             = txn.payeeVpa.ifBlank { "unknown@upi" },
                            payeeName       = payeeName.ifBlank { txn.payeeName },
                            amount          = amount,
                            transactionNote = txn.transactionDate,
                            status          = UpiStatus.SUCCESS,
                            transactionId   = txn.utrNumber.ifBlank { null },
                            rawResponse     = txn.rawNotificationText.take(400)
                        )
                    )
                }

                repository.deletePendingTransaction(txn)

            } catch (e: Exception) {
                android.util.Log.e("PENDING_VM", "saveAsExpense failed: ${e.message}")
            }
        }
    }

    /** Save all pending transactions that already have a categoryId assigned */
    fun saveAll() {
        viewModelScope.launch {
            pendingList.value
                .filter { it.categoryId != null }
                .forEach { txn ->
                    saveAsExpense(
                        txn        = txn,
                        categoryId = txn.categoryId!!,
                        projectId  = txn.projectId ?: 0L,
                        note       = txn.note,
                        amount     = txn.amount,
                        payeeName  = txn.payeeName
                    )
                }
        }
    }

    /** Update the local copy of a pending record (category, note, amount) */
    fun updatePending(txn: PendingTransaction) {
        viewModelScope.launch { repository.updatePendingTransaction(txn) }
    }
}

class PendingTransactionViewModelFactory(
    private val repository: PaisaTrackerRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val prefs = context.getSharedPreferences(
            UpiNotificationListener.PREFS_NAME, Context.MODE_PRIVATE
        )
        @Suppress("UNCHECKED_CAST")
        return PendingTransactionViewModel(repository, prefs) as T
    }
}
