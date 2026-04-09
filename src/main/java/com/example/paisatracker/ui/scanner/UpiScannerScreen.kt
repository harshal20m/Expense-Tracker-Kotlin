package com.example.paisatracker.ui.scanner

import android.content.pm.PackageManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.paisatracker.data.Category
import com.example.paisatracker.data.Project
import com.example.paisatracker.util.ParsedReceipt
import com.example.paisatracker.util.UpiQrData
import android.net.Uri
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.Launch
import androidx.compose.ui.platform.LocalContext

private val PAYMENT_METHODS = listOf("UPI", "GPay", "PhonePe", "Paytm", "Cash", "Card")

// ─────────────────────────────────────────────────────────────────────────────
//  Root screen — routes between states
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpiScannerScreen(
    viewModel: UpiScannerViewModel,
    onStartScan: () -> Unit,
    onPickReceipt: () -> Unit,
    onClose: () -> Unit,
    onP2mPayAndRecord: (UpiQrData) -> Unit,
    onReceiptSave: (ParsedReceipt, Uri) -> Unit
) {
    val uiState         by viewModel.uiState.collectAsState()
    val allProjects     by viewModel.allProjects.collectAsState()
    val filteredCats    by viewModel.filteredCategories.collectAsState()
    val isFormValid     by viewModel.isFormValid.collectAsState()
    val selectedProject by viewModel.selectedProject.collectAsState()
    val selectedCat     by viewModel.selectedCategory.collectAsState()
    val amountText      by viewModel.amountText.collectAsState()
    val note            by viewModel.transactionNote.collectAsState()
    val isCreatingCat   by viewModel.isCreatingCategory.collectAsState()
    val selectedPayment by viewModel.selectedPaymentMethod.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (uiState) {
                            is ScannerUiState.ReceiptConfirm    -> "Verify Receipt"
                            is ScannerUiState.P2pClipboard      -> "Pay Manually"
                            is ScannerUiState.ReceiptProcessing -> "Reading Receipt…"
                            else -> "Scan & Pay"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        // Back from sub-states goes to Idle, otherwise close
                        when (uiState) {
                            is ScannerUiState.P2mConfirm,
                            is ScannerUiState.P2pClipboard,
                            is ScannerUiState.ReceiptConfirm,
                            is ScannerUiState.Error -> viewModel.reset()
                            else -> onClose()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->

        AnimatedContent(
            targetState = uiState,
            transitionSpec = {
                (slideInHorizontally { it / 3 } + fadeIn(tween(220))) togetherWith
                        (slideOutHorizontally { -it / 3 } + fadeOut(tween(160)))
            },
            label = "scanner_state"
        ) { state ->
            when (state) {

                // ── Idle: two entry points ────────────────────────────────────
                is ScannerUiState.Idle,
                is ScannerUiState.Processing -> {
                    IdleScreen(
                        modifier      = Modifier.padding(padding),
                        isProcessing  = state is ScannerUiState.Processing,
                        onScanClick   = onStartScan,
                        onPickReceipt = onPickReceipt,
                        viewModel     = viewModel
                    )
                }

                // ── P2M confirm & categorise ──────────────────────────────────
                is ScannerUiState.P2mConfirm -> {
                    ConfirmAndCategoriseScreen(
                        modifier        = Modifier.padding(padding),
                        headerEmoji     = state.qrData.payeeName.firstOrNull()?.uppercase() ?: "M",
                        headerTitle     = state.qrData.payeeName.ifBlank { state.qrData.vpa },
                        headerSubtitle  = state.qrData.vpa,
                        badgeText       = "Merchant · Auto-pay",
                        badgeIsPositive = true,
                        amountText      = amountText,
                        amountEditable  = !state.qrData.isAmountLocked,
                        amountLocked    = state.qrData.isAmountLocked,
                        onAmountChange  = { viewModel.amountText.value = it },
                        note            = note,
                        onNoteChange    = { viewModel.transactionNote.value = it },
                        allProjects     = allProjects,
                        selectedProject = selectedProject,
                        onProjectSelect = { viewModel.onProjectSelected(it) },
                        filteredCats    = filteredCats,
                        selectedCat     = selectedCat,
                        onCatSelect     = { viewModel.selectedCategory.value = it },
                        isCreatingCat   = isCreatingCat,
                        newCatName      = viewModel.newCategoryName.collectAsState().value,
                        newCatEmoji     = viewModel.newCategoryEmoji.collectAsState().value,
                        onNewCatName    = { viewModel.newCategoryName.value = it },
                        onNewCatEmoji   = { viewModel.newCategoryEmoji.value = it },
                        onStartCreate   = { viewModel.isCreatingCategory.value = true },
                        onCancelCreate  = { viewModel.isCreatingCategory.value = false },
                        onConfirmCreate = { viewModel.confirmNewCategory() },
                        selectedPayment = selectedPayment,
                        onPaymentSelect = { viewModel.selectedPaymentMethod.value = it },
                        isFormValid     = isFormValid,
                        ctaText         = amountText.toDoubleOrNull()
                            ?.let { "Pay ₹${"%.2f".format(it)} via ${selectedPayment} →" }
                            ?: "Pay via UPI →",
                        onCta           = { onP2mPayAndRecord(state.qrData) }
                    )
                }

                // ── P2P clipboard screen ──────────────────────────────────────
                is ScannerUiState.P2pClipboard -> {
                    P2pScreen(
                        modifier  = Modifier.padding(padding),
                        qrData    = state.qrData,
                        onReset   = { viewModel.reset() }
                    )
                }

                // ── UPI intent launching ──────────────────────────────────────
                is ScannerUiState.PaymentLaunching -> {
                    Box(
                        modifier         = Modifier.fillMaxSize().padding(padding),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator()
                            Text("Opening UPI app…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }
                }

                // ── P2M result ────────────────────────────────────────────────
                is ScannerUiState.P2mResult -> {
                    ResultScreen(
                        modifier    = Modifier.padding(padding),
                        success     = state.success,
                        amount      = state.amount,
                        payeeName   = state.qrData.payeeName.ifBlank { state.qrData.vpa },
                        vpa         = state.qrData.vpa,
                        txnId       = state.txnId,
                        onScanAgain = { viewModel.reset() },
                        onClose     = onClose
                    )
                }

                // ── Receipt OCR running ───────────────────────────────────────
                is ScannerUiState.ReceiptProcessing -> {
                    Box(
                        modifier         = Modifier.fillMaxSize().padding(padding),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator()
                            Text("Reading receipt…", style = MaterialTheme.typography.bodyMedium)
                            Text("Extracting amount, name & UTR", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f))
                        }
                    }
                }

                // ── Receipt confirm ───────────────────────────────────────────
                is ScannerUiState.ReceiptConfirm -> {
                    ConfirmAndCategoriseScreen(
                        modifier        = Modifier.padding(padding),
                        headerEmoji     = state.parsed.paymentApp.first().toString(),
                        headerTitle     = state.parsed.payeeName.ifBlank { "Payment" },
                        headerSubtitle  = state.parsed.payeeVpa.ifBlank { "Scanned receipt" },
                        badgeText       = "${state.parsed.paymentApp} receipt",
                        badgeIsPositive = false,
                        amountText      = amountText,
                        amountEditable  = true,
                        amountLocked    = false,
                        onAmountChange  = { viewModel.amountText.value = it },
                        note            = note,
                        onNoteChange    = { viewModel.transactionNote.value = it },
                        allProjects     = allProjects,
                        selectedProject = selectedProject,
                        onProjectSelect = { viewModel.onProjectSelected(it) },
                        filteredCats    = filteredCats,
                        selectedCat     = selectedCat,
                        onCatSelect     = { viewModel.selectedCategory.value = it },
                        isCreatingCat   = isCreatingCat,
                        newCatName      = viewModel.newCategoryName.collectAsState().value,
                        newCatEmoji     = viewModel.newCategoryEmoji.collectAsState().value,
                        onNewCatName    = { viewModel.newCategoryName.value = it },
                        onNewCatEmoji   = { viewModel.newCategoryEmoji.value = it },
                        onStartCreate   = { viewModel.isCreatingCategory.value = true },
                        onCancelCreate  = { viewModel.isCreatingCategory.value = false },
                        onConfirmCreate = { viewModel.confirmNewCategory() },
                        selectedPayment = selectedPayment,
                        onPaymentSelect = { viewModel.selectedPaymentMethod.value = it },
                        isFormValid     = isFormValid,
                        ctaText         = "Save Expense ✓",
                        onCta           = { onReceiptSave(state.parsed, state.imageUri) },
                        receiptPreviewUri = state.imageUri,
                        utrNumber       = state.parsed.utrNumber
                    )
                }

                // ── Error ─────────────────────────────────────────────────────
                is ScannerUiState.Error -> {
                    ErrorScreen(
                        modifier = Modifier.padding(padding),
                        message  = state.message,
                        onRetry  = { viewModel.reset() }
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Idle screen — two big action cards
// ─────────────────────────────────────────────────────────────────────────────

// ── UPI app package names ─────────────────────────────────────────────────────
private data class UpiApp(val name: String, val packageName: String, val emoji: String)

private val KNOWN_UPI_APPS = listOf(
    UpiApp("GPay",     "com.google.android.apps.nbu.paisa.user", "G"),
    UpiApp("PhonePe",  "com.phonepe.app",                        "P"),
    UpiApp("Paytm",    "net.one97.paytm",                        "₽"),
    UpiApp("BHIM",     "in.org.npci.upiapp",                     "B"),
    UpiApp("Amazon",   "in.amazon.mShop.android.shopping",       "A"),
)

// ── Fixed IdleScreen — manual VPA now works via LocalContext ──────────────────

@Composable
internal fun IdleScreen(
    modifier: Modifier,
    isProcessing: Boolean,
    onScanClick: () -> Unit,
    onPickReceipt: () -> Unit,
    viewModel: UpiScannerViewModel   // ← pass viewModel directly so we can call onManualVpaEntered
) {
    val context = LocalContext.current
    var showManual by remember { mutableStateOf(false) }
    var manualVpa  by remember { mutableStateOf("") }
    var manualName by remember { mutableStateOf("") }

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(16.dp))

        Text(
            "How do you want to add this expense?",
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign  = TextAlign.Center
        )

        Spacer(Modifier.height(4.dp))

        // Scan QR
        Card(
            modifier  = Modifier.fillMaxWidth().clickable(enabled = !isProcessing, onClick = onScanClick),
            shape     = RoundedCornerShape(20.dp),
            colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    if (isProcessing) CircularProgressIndicator(modifier = Modifier.size(28.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    else Icon(Icons.Default.QrCodeScanner, null, modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.onPrimary)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Scan QR Code", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("For shops & merchants · Amount pre-filled", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.65f))
                }
            }
        }

        // Share receipt
        Card(
            modifier  = Modifier.fillMaxWidth().clickable(onClick = onPickReceipt),
            shape     = RoundedCornerShape(20.dp),
            colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.secondary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Image, null, modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.onSecondary)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Share Receipt", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    Text("Share screenshot from GPay / PhonePe / any UPI app", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.65f))
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        // Manual VPA toggle
        if (!showManual) {
            TextButton(onClick = { showManual = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Enter UPI ID manually", color = MaterialTheme.colorScheme.primary)
            }
        } else {
            AnimatedVisibility(visible = showManual, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value         = manualVpa,
                        onValueChange = { manualVpa = it.trim() },
                        label         = { Text("UPI ID") },
                        placeholder   = { Text("e.g. rahul@oksbi") },
                        singleLine    = true,
                        modifier      = Modifier.fillMaxWidth(),
                        shape         = RoundedCornerShape(12.dp),
                        isError       = manualVpa.isNotEmpty() && !manualVpa.contains("@")
                    )
                    OutlinedTextField(
                        value         = manualName,
                        onValueChange = { manualName = it },
                        label         = { Text("Name (optional)") },
                        singleLine    = true,
                        modifier      = Modifier.fillMaxWidth(),
                        shape         = RoundedCornerShape(12.dp)
                    )
                    Button(
                        // ← FIX: directly call viewModel.onManualVpaEntered with context
                        onClick  = { viewModel.onManualVpaEntered(manualVpa, manualName, context) },
                        enabled  = manualVpa.contains("@"),
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(12.dp)
                    ) { Text("Continue") }
                }
            }
        }
    }
}

// ── Fixed P2P Screen — share from app, direct launch buttons ─────────────────

@Composable
internal fun P2pScreen(
    modifier: Modifier,
    qrData: com.example.paisatracker.util.UpiQrData,
    onReset: () -> Unit
) {
    val context = LocalContext.current
    val pm = context.packageManager

    // Which UPI apps are actually installed on this device?
    val installedApps = remember {
        KNOWN_UPI_APPS.filter { app ->
            try { pm.getPackageInfo(app.packageName, 0); true }
            catch (_: PackageManager.NameNotFoundException) { false }
        }
    }

    Column(
        modifier              = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment   = Alignment.CenterHorizontally,
        verticalArrangement   = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        // ── Recipient card ────────────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(20.dp),
            colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Paying to", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f))
                Text(qrData.payeeName.ifBlank { qrData.vpa }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)

                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)).padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(qrData.vpa, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ContentCopy, "Copied", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                }

                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)) {
                    Text("✓ UPI ID copied to clipboard", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                }
            }
        }

        // ── Open UPI app directly ─────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(20.dp),
            colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Open your UPI app to pay", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    "The UPI ID is already copied. Open any app below, search by UPI ID or use the clipboard, and complete the payment. When done, share the success screen back here.",
                    style     = MaterialTheme.typography.bodySmall,
                    color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    lineHeight= 18.sp
                )

                // Direct launch buttons for installed UPI apps
                if (installedApps.isNotEmpty()) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        installedApps.forEach { app ->
                            OutlinedButton(
                                onClick  = {
                                    try {
                                        val intent = pm.getLaunchIntentForPackage(app.packageName)
                                        if (intent != null) context.startActivity(intent)
                                    } catch (_: Exception) { }
                                },
                                modifier = Modifier.weight(1f),
                                shape    = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(app.emoji, fontSize = 20.sp)
                                    Text(app.name, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }

                // How to share receipt back
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(12.dp),
                    color    = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment     = Alignment.Top
                    ) {
                        Icon(Icons.Default.Launch, null, modifier = Modifier.size(16.dp).padding(top = 2.dp), tint = MaterialTheme.colorScheme.primary)
                        Text(
                            "After paying: tap Share in your UPI app and choose PaisaTracker to auto-fill and save the expense.",
                            style     = MaterialTheme.typography.bodySmall,
                            color     = MaterialTheme.colorScheme.onPrimaryContainer,
                            lineHeight= 18.sp
                        )
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        OutlinedButton(onClick = onReset, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Text("Scan a different QR")
        }
    }
}
// ─────────────────────────────────────────────────────────────────────────────
//  Shared: Confirm & Categorise form  (used by both P2M and receipt flows)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ConfirmAndCategoriseScreen(
    modifier: Modifier,
    headerEmoji: String,
    headerTitle: String,
    headerSubtitle: String,
    badgeText: String,
    badgeIsPositive: Boolean,
    amountText: String,
    amountEditable: Boolean,
    amountLocked: Boolean,
    onAmountChange: (String) -> Unit,
    note: String,
    onNoteChange: (String) -> Unit,
    allProjects: List<Project>,
    selectedProject: Project?,
    onProjectSelect: (Project) -> Unit,
    filteredCats: List<Category>,
    selectedCat: Category?,
    onCatSelect: (Category) -> Unit,
    isCreatingCat: Boolean,
    newCatName: String,
    newCatEmoji: String,
    onNewCatName: (String) -> Unit,
    onNewCatEmoji: (String) -> Unit,
    onStartCreate: () -> Unit,
    onCancelCreate: () -> Unit,
    onConfirmCreate: () -> Unit,
    selectedPayment: String,
    onPaymentSelect: (String) -> Unit,
    isFormValid: Boolean,
    ctaText: String,
    onCta: () -> Unit,
    receiptPreviewUri: Uri? = null,
    utrNumber: String = ""
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── Header card ───────────────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(18.dp),
            colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            elevation= CardDefaults.cardElevation(0.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(headerEmoji, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(headerTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(headerSubtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.65f))
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = (if (badgeIsPositive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary).copy(alpha = 0.15f)
                ) {
                    Text(
                        badgeText,
                        style     = MaterialTheme.typography.labelSmall,
                        fontWeight= FontWeight.SemiBold,
                        color     = if (badgeIsPositive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary,
                        modifier  = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // ── Receipt image preview (receipt flow only) ─────────────────────────
        receiptPreviewUri?.let { uri ->
            Card(modifier = Modifier.fillMaxWidth().height(140.dp), shape = RoundedCornerShape(14.dp)) {
                AsyncImage(model = uri, contentDescription = "Receipt", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
        }

        // ── UTR number (receipt flow only) ────────────────────────────────────
        if (utrNumber.isNotBlank()) {
            Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
                Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("UTR / Ref", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Text(utrNumber, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        // ── Amount ────────────────────────────────────────────────────────────
        SLabel("Amount", trailing = if (!amountEditable && amountLocked) null else if (!amountEditable) null else null)
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)).padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("₹", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.W300, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                BasicTextField(
                    value         = amountText,
                    onValueChange = { if (amountEditable && it.matches(Regex("^\\d*\\.?\\d{0,2}\$"))) onAmountChange(it) },
                    enabled       = amountEditable,
                    modifier      = Modifier.weight(1f),
                    textStyle     = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, letterSpacing = (-0.5).sp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    cursorBrush   = SolidColor(MaterialTheme.colorScheme.primary),
                    singleLine    = true,
                    decorationBox = { inner ->
                        if (amountText.isEmpty()) Text("0.00", style = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)))
                        inner()
                    }
                )
                if (amountLocked) Text("🔒", fontSize = 16.sp)
            }
        }

        // ── Note ──────────────────────────────────────────────────────────────
        SLabel("Note")
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)).padding(horizontal = 16.dp, vertical = 12.dp)) {
            BasicTextField(value = note, onValueChange = onNoteChange, modifier = Modifier.fillMaxWidth(), textStyle = TextStyle(fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface), cursorBrush = SolidColor(MaterialTheme.colorScheme.primary), singleLine = true, decorationBox = { inner ->
                if (note.isEmpty()) Text("What's this for?", style = TextStyle(fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)))
                inner()
            })
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

        // ── Project ───────────────────────────────────────────────────────────
        SLabel("Project")
        PickerRow(displayValue = selectedProject?.let { "${it.emoji} ${it.name}" } ?: "Select project…", hasSelection = selectedProject != null, enabled = true, dropdownItems = allProjects, itemLabel = { "${it.emoji} ${it.name}" }, onItemSelected = onProjectSelect, pillItems = allProjects.take(4), isSelectedPill = { it.id == selectedProject?.id }, pillLabel = { "${it.emoji} ${it.name}" }, onPillSelected = onProjectSelect)

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

        // ── Category ──────────────────────────────────────────────────────────
        SLabel("Category", trailing = if (selectedProject == null) "select a project first" else null)

        AnimatedVisibility(visible = isCreatingCat, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            InlineCategoryCreate(name = newCatName, emoji = newCatEmoji, onNameChange = onNewCatName, onEmojiChange = onNewCatEmoji, onConfirm = onConfirmCreate, onCancel = onCancelCreate)
        }
        if (!isCreatingCat) {
            PickerRow(displayValue = selectedCat?.let { "${it.emoji} ${it.name}" } ?: if (selectedProject != null) "Select category…" else "—", hasSelection = selectedCat != null, enabled = selectedProject != null, dropdownItems = filteredCats, itemLabel = { "${it.emoji} ${it.name}" }, onItemSelected = onCatSelect, pillItems = filteredCats.take(4), isSelectedPill = { it.id == selectedCat?.id }, pillLabel = { "${it.emoji} ${it.name}" }, onPillSelected = onCatSelect, extraPill = if (selectedProject != null) "+ New" else null, onExtraPillClick = { onStartCreate() })
        }

        // ── Payment method ────────────────────────────────────────────────────
        SLabel("Payment method")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(end = 4.dp)) {
            items(PAYMENT_METHODS) { method ->
                val isSel = selectedPayment == method
                Box(
                    modifier = Modifier.height(34.dp).clip(RoundedCornerShape(20.dp)).background(if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onPaymentSelect(method) }.padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(method, style = MaterialTheme.typography.labelMedium, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal, color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // ── CTA button ────────────────────────────────────────────────────────
        Button(onClick = onCta, enabled = isFormValid, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))) {
            Text(ctaText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(16.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Result screen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ResultScreen(modifier: Modifier, success: Boolean, amount: Double, payeeName: String, vpa: String, txnId: String?, onScanAgain: () -> Unit, onClose: () -> Unit) {
    Column(modifier = modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Spacer(Modifier.height(24.dp))
        Box(modifier = Modifier.size(80.dp).clip(CircleShape).background(if (success) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.errorContainer), contentAlignment = Alignment.Center) { Text(if (success) "✅" else "❌", fontSize = 36.sp) }
        Text(if (success) "Payment Successful" else "Payment Failed", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = if (success) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error)
        Text(if (success) "Expense recorded" else "No expense was saved", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f), textAlign = TextAlign.Center)
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ResultDetailRow("Paid to", payeeName)
                ResultDetailRow("VPA",     vpa)
                ResultDetailRow("Amount",  "₹${"%.2f".format(amount)}")
                txnId?.let { ResultDetailRow("Transaction ID", it) }
            }
        }
        Spacer(Modifier.weight(1f))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onClose, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("Done") }
            Button(onClick = onScanAgain, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("Scan Again") }
        }
    }
}

@Composable private fun ResultDetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false).padding(start = 8.dp), textAlign = TextAlign.End)
    }
}

// ── Error ─────────────────────────────────────────────────────────────────────
@Composable private fun ErrorScreen(modifier: Modifier, message: String, onRetry: () -> Unit) {
    Column(modifier = modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("⚠️", fontSize = 48.sp)
        Spacer(Modifier.height(12.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRetry, shape = RoundedCornerShape(12.dp)) { Text("Try Again") }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Reusable sub-components
// ─────────────────────────────────────────────────────────────────────────────

@Composable private fun SLabel(text: String, trailing: String? = null) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f), letterSpacing = 0.8.sp)
        trailing?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)) }
    }
}

@Composable private fun <T : Any> PickerRow(displayValue: String, hasSelection: Boolean, enabled: Boolean, dropdownItems: List<T>, itemLabel: (T) -> String, onItemSelected: (T) -> Unit, pillItems: List<T>, isSelectedPill: (T) -> Boolean, pillLabel: (T) -> String, onPillSelected: (T) -> Unit, extraPill: String? = null, onExtraPillClick: (() -> Unit)? = null) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (enabled) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)).clickable(enabled = enabled && dropdownItems.isNotEmpty()) { expanded = true }.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(displayValue, style = MaterialTheme.typography.bodyMedium, fontWeight = if (hasSelection) FontWeight.SemiBold else FontWeight.Normal, color = when { !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f); hasSelection -> MaterialTheme.colorScheme.onSurface; else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) }, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Icon(Icons.Default.KeyboardArrowDown, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 0.4f else 0.15f))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) { dropdownItems.forEach { item -> DropdownMenuItem(text = { Text(itemLabel(item)) }, onClick = { onItemSelected(item); expanded = false }) } }
    }
    if (pillItems.isNotEmpty() && enabled) {
        Spacer(Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(end = 4.dp)) {
            items(pillItems) { item -> SPill(label = pillLabel(item), isSelected = isSelectedPill(item), onClick = { onPillSelected(item) }) }
            extraPill?.let { label -> item { SPill(label = label, isSelected = false, isNew = true, onClick = { onExtraPillClick?.invoke() }) } }
        }
    }
}

@Composable private fun SPill(label: String, isSelected: Boolean, isNew: Boolean = false, onClick: () -> Unit) {
    Box(modifier = Modifier.height(28.dp).clip(RoundedCornerShape(20.dp)).background(when { isSelected -> MaterialTheme.colorScheme.primary; isNew -> MaterialTheme.colorScheme.surface; else -> MaterialTheme.colorScheme.surfaceVariant }).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick).padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = if (isSelected || isNew) FontWeight.SemiBold else FontWeight.Normal, color = when { isSelected -> MaterialTheme.colorScheme.onPrimary; isNew -> MaterialTheme.colorScheme.primary; else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f) }, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable private fun InlineCategoryCreate(name: String, emoji: String, onNameChange: (String) -> Unit, onEmojiChange: (String) -> Unit, onConfirm: () -> Unit, onCancel: () -> Unit) {
    val emojiList = listOf("📂","🛒","🍔","🚗","💊","📚","🎮","☕","✈️","👗","🏋️","💡","🎬","🔧","🎵","📱")
    var showEmoji by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surface).clickable { showEmoji = !showEmoji }, contentAlignment = Alignment.Center) { Text(emoji, fontSize = 20.sp) }
            Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surface).padding(horizontal = 12.dp, vertical = 10.dp)) {
                BasicTextField(value = name, onValueChange = onNameChange, modifier = Modifier.fillMaxWidth(), textStyle = TextStyle(fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface), singleLine = true, cursorBrush = SolidColor(MaterialTheme.colorScheme.primary), decorationBox = { inner -> if (name.isEmpty()) Text("Category name", style = TextStyle(fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))); inner() })
            }
            Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surface).clickable(onClick = onCancel), contentAlignment = Alignment.Center) { Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) }
            Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(if (name.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)).clickable(enabled = name.isNotBlank(), onClick = onConfirm), contentAlignment = Alignment.Center) { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp), tint = if (name.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)) }
        }
        if (showEmoji) { LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { items(emojiList) { e -> Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(if (emoji == e) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface).clickable { onEmojiChange(e); showEmoji = false }, contentAlignment = Alignment.Center) { Text(e, fontSize = 18.sp) } } } }
    }
}