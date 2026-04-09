package com.example.paisatracker.ui.scanner

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.paisatracker.data.Asset
import com.example.paisatracker.data.Category
import com.example.paisatracker.data.Expense
import com.example.paisatracker.data.PaisaTrackerRepository
import com.example.paisatracker.data.Project
import com.example.paisatracker.data.UpiStatus
import com.example.paisatracker.data.UpiTransaction
import com.example.paisatracker.util.ParsedReceipt
import com.example.paisatracker.util.UpiCallbackResult
import com.example.paisatracker.util.UpiQrData
import com.example.paisatracker.util.extractTextFromImage
import com.example.paisatracker.util.parseReceiptText
import com.example.paisatracker.util.parseUpiQr
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// ─── UI State ─────────────────────────────────────────────────────────────────

sealed class ScannerUiState {
    /** Initial screen: two buttons — Scan QR and Share Receipt */
    object Idle : ScannerUiState()

    /** ZXing camera is open / QR is being processed */
    object Processing : ScannerUiState()

    /** P2M merchant QR — show confirm + categorise form */
    data class P2mConfirm(val qrData: UpiQrData) : ScannerUiState()

    /** P2P personal QR — VPA copied to clipboard, prompt user to open UPI app */
    data class P2pClipboard(val qrData: UpiQrData) : ScannerUiState()

    /** P2M intent is being fired */
    object PaymentLaunching : ScannerUiState()

    /** P2M payment came back with a result */
    data class P2mResult(
        val success: Boolean,
        val qrData: UpiQrData,
        val amount: Double,
        val txnId: String?,
        val expenseId: Long
    ) : ScannerUiState()

    /** ML Kit OCR is running on a shared screenshot */
    object ReceiptProcessing : ScannerUiState()

    /**
     * OCR finished — show pre-filled form for user to verify before saving.
     * [imageUri] is kept so we can attach it as an Asset after save.
     */
    data class ReceiptConfirm(
        val parsed: ParsedReceipt,
        val imageUri: Uri
    ) : ScannerUiState()

    data class Error(val message: String) : ScannerUiState()
}

class UpiScannerViewModel(
    private val repository: PaisaTrackerRepository
) : ViewModel() {

    val uiState = MutableStateFlow<ScannerUiState>(ScannerUiState.Idle)

    // ── Shared form state (used by both P2M confirm and receipt confirm) ───────
    val amountText            = MutableStateFlow("")
    val selectedProject       = MutableStateFlow<Project?>(null)
    val selectedCategory      = MutableStateFlow<Category?>(null)
    val transactionNote       = MutableStateFlow("")
    val selectedPaymentMethod = MutableStateFlow("UPI")

    // ── Inline category creation ──────────────────────────────────────────────
    val isCreatingCategory = MutableStateFlow(false)
    val newCategoryName    = MutableStateFlow("")
    val newCategoryEmoji   = MutableStateFlow("📂")

    // ── Pending DB IDs (P2M flow) ─────────────────────────────────────────────
    private var pendingExpenseId: Long = -1L
    private var pendingUpiTxnId: Long  = -1L

    // ── Source data ───────────────────────────────────────────────────────────
    val allProjects: StateFlow<List<Project>> = repository.getAllProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories: StateFlow<List<Category>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredCategories: StateFlow<List<Category>> = combine(
        allCategories, selectedProject
    ) { cats, proj ->
        if (proj == null) emptyList() else cats.filter { it.projectId == proj.id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isFormValid: StateFlow<Boolean> = combine(
        amountText, selectedProject, selectedCategory
    ) { amount, proj, cat ->
        amount.toDoubleOrNull()?.let { it > 0 } == true && proj != null && cat != null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // ─────────────────────────────────────────────────────────────────────────
    //  QR scan entry point
    // ─────────────────────────────────────────────────────────────────────────

    fun onQrScanned(rawQrString: String, context: Context) {
        uiState.value = ScannerUiState.Processing

        val qrData = parseUpiQr(rawQrString)
        if (qrData == null) {
            uiState.value = ScannerUiState.Error("Not a valid UPI QR code. Try again.")
            return
        }

        amountText.value      = qrData.amount?.let { "%.2f".format(it) } ?: ""
        transactionNote.value = qrData.note

        if (qrData.isMerchantQr) {
            // P2M: go to confirm sheet
            uiState.value = ScannerUiState.P2mConfirm(qrData)
        } else {
            // P2P: copy VPA to clipboard silently, show instructions screen
            copyVpaToClipboard(context, qrData.vpa)
            uiState.value = ScannerUiState.P2pClipboard(qrData)
        }
    }

    fun onManualVpaEntered(vpa: String, name: String, context: Context) {
        if (!vpa.contains("@")) {
            uiState.value = ScannerUiState.Error("Invalid UPI ID. Format: user@bank")
            return
        }
        onQrScanned("upi://pay?pa=${vpa.trim()}&pn=${name.trim()}", context)
    }

    private fun copyVpaToClipboard(context: Context, vpa: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("UPI ID", vpa)
        clipboard.setPrimaryClip(clip)
        Log.d("SCANNER", "VPA copied to clipboard: $vpa")
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Receipt share entry point (called by Activity when ACTION_SEND arrives)
    // ─────────────────────────────────────────────────────────────────────────

    fun onReceiptImageShared(context: Context, imageUri: Uri) {
        uiState.value = ScannerUiState.ReceiptProcessing

        viewModelScope.launch {
            try {
                val rawText = extractTextFromImage(context, imageUri)
                val parsed  = parseReceiptText(rawText)

                // Pre-fill amount from parsed receipt
                parsed.amount?.let { amountText.value = "%.2f".format(it) }

                // Note = payee name (who you paid) — most useful for expense records
                transactionNote.value = parsed.payeeName.ifBlank {
                    parsed.payeeVpa.substringBefore("@").ifBlank { "" }
                }

                // Auto-select payment method from detected app
                selectedPaymentMethod.value = when (parsed.paymentApp) {
                    "GPay"       -> "GPay"
                    "PhonePe"    -> "PhonePe"
                    "Paytm"      -> "Paytm"
                    "BHIM"       -> "UPI"
                    "SuperMoney" -> "UPI"
                    "Jupiter"    -> "UPI"
                    "Kotak"      -> "UPI"
                    else         -> "UPI"
                }

                uiState.value = ScannerUiState.ReceiptConfirm(parsed, imageUri)

            } catch (e: Exception) {
                android.util.Log.e("SCANNER", "OCR failed: ${e.message}")
                uiState.value = ScannerUiState.Error(
                    "Could not read the receipt image. Please try a clearer screenshot."
                )
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  P2M: record expense + fire intent
    // ─────────────────────────────────────────────────────────────────────────

    fun onP2mPayAndRecord(qrData: UpiQrData, onReadyToLaunch: (Long, Long) -> Unit) {
        val amount   = amountText.value.toDoubleOrNull() ?: return
        val category = selectedCategory.value ?: return
        val payment  = selectedPaymentMethod.value

        viewModelScope.launch {
            try {
                val expense = Expense(
                    amount        = amount,
                    description   = "Paid to ${qrData.payeeName.ifBlank { qrData.vpa }}",
                    date          = System.currentTimeMillis(),
                    categoryId    = category.id,
                    paymentMethod = payment,
                    paymentIcon   = payment.toPaymentIconKey()
                )
                pendingExpenseId = repository.insertExpense(expense)

                val upiTxn = UpiTransaction(
                    expenseId       = pendingExpenseId,
                    vpa             = qrData.vpa,
                    payeeName       = qrData.payeeName,
                    amount          = amount,
                    transactionNote = transactionNote.value.ifBlank { "PaisaTracker" },
                    status          = UpiStatus.PENDING
                )
                pendingUpiTxnId = repository.insertUpiTransaction(upiTxn)

                uiState.value = ScannerUiState.PaymentLaunching
                onReadyToLaunch(pendingExpenseId, pendingUpiTxnId)

            } catch (e: Exception) {
                uiState.value = ScannerUiState.Error("Failed to save: ${e.message}")
            }
        }
    }

    fun onP2mPaymentResult(callbackResult: UpiCallbackResult, qrData: UpiQrData) {
        if (pendingUpiTxnId < 0) return
        val upiStatus = when (callbackResult.status) {
            "SUCCESS"   -> UpiStatus.SUCCESS
            "SUBMITTED" -> UpiStatus.SUBMITTED
            else        -> UpiStatus.FAILED
        }
        viewModelScope.launch {
            repository.updateUpiTransactionStatus(
                id    = pendingUpiTxnId,
                status= upiStatus,
                txnId = callbackResult.transactionId,
                code  = callbackResult.responseCode,
                raw   = callbackResult.rawData
            )
            if (upiStatus == UpiStatus.FAILED && pendingExpenseId > 0) {
                repository.deleteExpenseById(pendingExpenseId)
            }
            uiState.value = ScannerUiState.P2mResult(
                success   = upiStatus != UpiStatus.FAILED,
                qrData    = qrData,
                amount    = amountText.value.toDoubleOrNull() ?: 0.0,
                txnId     = callbackResult.transactionId,
                expenseId = pendingExpenseId
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Receipt: save expense directly (no UPI intent — payment already done)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Called when user taps "Save Expense" on the receipt confirm screen.
     * The payment has already been completed in the external UPI app — we
     * are only recording the expense locally.
     *
     * [imageUri] is attached as an Asset linked to the new Expense so the
     * receipt screenshot is stored with the record.
     */
    fun onReceiptSave(
        context: Context,
        parsed: ParsedReceipt,
        imageUri: Uri,
        onSuccess: (Long) -> Unit
    ) {
        val amount   = amountText.value.toDoubleOrNull() ?: return
        val category = selectedCategory.value ?: return
        val payment  = selectedPaymentMethod.value

        viewModelScope.launch {
            try {
                // Build a rich description: "Paid Rahul Kumar · UTR 123456789012"
                val description = buildString {
                    val name = parsed.payeeName.ifBlank { parsed.payeeVpa.ifBlank { "UPI Payment" } }
                    append(name)
                    if (parsed.utrNumber.isNotBlank()) append(" · UTR ${parsed.utrNumber}")
                }

                val expense = Expense(
                    amount        = amount,
                    description   = description,
                    date          = System.currentTimeMillis(),
                    categoryId    = category.id,
                    paymentMethod = payment,
                    paymentIcon   = payment.toPaymentIconKey()
                )
                val expenseId = repository.insertExpense(expense)

                // Store UPI transaction with SUCCESS status and full parsed info
                if (parsed.payeeVpa.isNotBlank() || parsed.utrNumber.isNotBlank()) {
                    repository.insertUpiTransaction(
                        UpiTransaction(
                            expenseId       = expenseId,
                            vpa             = parsed.payeeVpa.ifBlank { "unknown@upi" },
                            payeeName       = parsed.payeeName,
                            amount          = amount,
                            // transactionNote stores app name + date for reference
                            transactionNote = buildString {
                                append(parsed.paymentApp)
                                if (parsed.transactionDate.isNotBlank()) append(" · ${parsed.transactionDate}")
                            },
                            status          = UpiStatus.SUCCESS,
                            transactionId   = parsed.utrNumber.ifBlank { null },
                            rawResponse     = parsed.rawText.take(500)
                        )
                    )
                }

                // Attach the receipt screenshot as a linked Asset
                repository.insertAsset(
                    Asset(
                        imagePath = imageUri.toString(),
                        title = "Payment receipt",
                        description = "${parsed.paymentApp} receipt${if (parsed.transactionDate.isNotBlank()) " · ${parsed.transactionDate}" else ""}",
                        expenseId = expenseId
                    )
                )

                onSuccess(expenseId)

            } catch (e: Exception) {
                uiState.value = ScannerUiState.Error("Failed to save: ${e.message}")
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Category creation
    // ─────────────────────────────────────────────────────────────────────────

    fun onProjectSelected(project: Project) {
        selectedProject.value = project
        selectedCategory.value = null
        isCreatingCategory.value = false
    }

    fun confirmNewCategory() {
        val project = selectedProject.value ?: return
        val name    = newCategoryName.value.trim().ifBlank { return }
        viewModelScope.launch {
            val newId = repository.insertCategory(
                Category(name = name, emoji = newCategoryEmoji.value, projectId = project.id)
            )
            selectedCategory.value = Category(
                id = newId, name = name, emoji = newCategoryEmoji.value, projectId = project.id
            )
            isCreatingCategory.value = false
            newCategoryName.value = ""
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Reset
    // ─────────────────────────────────────────────────────────────────────────

    fun reset() {
        uiState.value           = ScannerUiState.Idle
        amountText.value        = ""
        selectedCategory.value  = null
        transactionNote.value   = ""
        isCreatingCategory.value= false
        newCategoryName.value   = ""
        selectedPaymentMethod.value = "UPI"
        pendingExpenseId        = -1L
        pendingUpiTxnId         = -1L
    }
}

// ─── Helper ───────────────────────────────────────────────────────────────────

private fun String.toPaymentIconKey(): String = when (this) {
    "GPay"    -> "gpay"
    "PhonePe" -> "phonepe"
    "Paytm"   -> "paytm"
    "Cash"    -> "cash"
    "Card"    -> "card"
    else      -> "upi"
}

class UpiScannerViewModelFactory(
    private val repository: PaisaTrackerRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UpiScannerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UpiScannerViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}