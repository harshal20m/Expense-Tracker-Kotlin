package com.example.paisatracker.ui.expense

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.paisatracker.PaisaTrackerViewModel
import com.example.paisatracker.R
import com.example.paisatracker.data.Asset
import com.example.paisatracker.data.Expense
import com.example.paisatracker.ui.common.ZoomableImageDialog
import com.example.paisatracker.util.formatCurrency
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseDetailScreen(
    viewModel: PaisaTrackerViewModel,
    expenseId: Long,
    navController: NavController
) {
    val context = LocalContext.current

    val expense by viewModel.getExpenseById(expenseId).collectAsState(initial = null)
    val assets  by viewModel.getAssetsForExpense(expenseId).collectAsState(initial = emptyList())

    // ── Edit state ────────────────────────────────────────────────────────────
    var showEditSheet   by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val editSheetState  = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val pickImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && expense != null) {
            viewModel.addLinkedAsset(context = context, uri = uri, title = expense!!.description, description = "", expenseId = expense!!.id)
        }
    }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f),
            MaterialTheme.colorScheme.background
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Expense Details", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    // ── Edit button ───────────────────────────────────────────
                    IconButton(onClick = { showEditSheet = true }) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit expense",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    // ── Delete button ─────────────────────────────────────────
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete expense",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().background(backgroundGradient).padding(innerPadding)
        ) {
            expense?.let { data ->
                ExpenseDetailContent(
                    expense       = data,
                    assets        = assets,
                    onAddAsset    = { pickImageLauncher.launch("image/*") },
                    onDeleteAsset = { asset -> viewModel.deleteAsset(asset) }
                )
            } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading expense…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
            }
        }
    }

    // ── Edit bottom sheet ─────────────────────────────────────────────────────
    if (showEditSheet && expense != null) {
        ModalBottomSheet(
            onDismissRequest = { showEditSheet = false },
            sheetState       = editSheetState,
            shape            = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle       = { BottomSheetDefaults.DragHandle() }
        ) {
            EditExpenseSheetContent(
                expense  = expense!!,
                onDismiss= { showEditSheet = false },
                onConfirm= { updated ->
                    viewModel.updateExpense(updated)
                    showEditSheet = false
                    Toast.makeText(context, "Expense updated", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    // ── Delete confirmation dialog ────────────────────────────────────────────
    if (showDeleteDialog && expense != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon  = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Expense?") },
            text  = {
                Text(
                    "\"${expense!!.description}\" will be permanently deleted along with all its assets.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteExpense(expense!!)
                        showDeleteDialog = false
                        navController.navigateUp()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

// ─── Edit expense sheet content ───────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditExpenseSheetContent(
    expense: Expense,
    onDismiss: () -> Unit,
    onConfirm: (Expense) -> Unit
) {
    var amount      by remember { mutableStateOf(expense.amount.toString()) }
    var description by remember { mutableStateOf(expense.description) }
    var date        by remember { mutableStateOf(expense.date) }
    var paymentMethod by remember { mutableStateOf(expense.paymentMethod ?: "") }
    var dropdownExpanded by remember { mutableStateOf(false) }

    val context  = LocalContext.current
    val calendar = Calendar.getInstance().apply { timeInMillis = date }

    val datePicker = android.app.DatePickerDialog(
        context,
        { _, y, m, d -> calendar.set(y, m, d); date = calendar.timeInMillis },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Edit Expense", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }

        OutlinedTextField(
            value         = amount,
            onValueChange = { if (it.matches(Regex("^\\d*\\.?\\d{0,2}\$"))) amount = it },
            label         = { Text("Amount") },
            leadingIcon   = { Text("₹", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 12.dp)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine    = true,
            shape         = RoundedCornerShape(14.dp),
            modifier      = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value         = description,
            onValueChange = { description = it },
            label         = { Text("Description") },
            singleLine    = false,
            maxLines      = 3,
            shape         = RoundedCornerShape(14.dp),
            modifier      = Modifier.fillMaxWidth()
        )

        // Payment method dropdown
        val paymentMethods = listOf("UPI", "GPay", "PhonePe", "Paytm", "Cash", "Card")
        ExposedDropdownMenuBox(expanded = dropdownExpanded, onExpandedChange = { dropdownExpanded = it }, modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value       = paymentMethod.ifBlank { "Not specified" },
                onValueChange = {},
                readOnly    = true,
                label       = { Text("Payment method") },
                trailingIcon= { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                modifier    = Modifier.menuAnchor().fillMaxWidth(),
                shape       = RoundedCornerShape(14.dp)
            )
            ExposedDropdownMenu(expanded = dropdownExpanded, onDismissRequest = { dropdownExpanded = false }) {
                paymentMethods.forEach { m ->
                    DropdownMenuItem(text = { Text(m) }, onClick = { paymentMethod = m; dropdownExpanded = false })
                }
                DropdownMenuItem(text = { Text("None") }, onClick = { paymentMethod = ""; dropdownExpanded = false })
            }
        }

        // Date picker
        OutlinedButton(onClick = { datePicker.show() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
            Icon(Icons.Default.DateRange, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(Date(date)))
        }

        // Save button
        Button(
            onClick  = {
                val parsedAmount = amount.toDoubleOrNull()
                if (description.isNotBlank() && parsedAmount != null && parsedAmount > 0) {
                    onConfirm(
                        expense.copy(
                            amount        = parsedAmount,
                            description   = description.trim(),
                            date          = date,
                            paymentMethod = paymentMethod.ifBlank { null },
                            paymentIcon   = paymentMethod.toIconKey()
                        )
                    )
                } else {
                    Toast.makeText(context, "Please enter a valid description and amount", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = RoundedCornerShape(14.dp)
        ) {
            Text("Save Changes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(8.dp))
    }
}

private fun String.toIconKey(): String? = when (this) {
    "GPay"    -> "gpay"
    "PhonePe" -> "phonepe"
    "Paytm"   -> "paytm"
    "Cash"    -> "cash"
    "Card"    -> "card"
    "UPI"     -> "upi"
    else      -> null
}

// ─── Detail content (unchanged from previous, included for completeness) ──────

@Composable
private fun ExpenseDetailContent(
    expense: Expense,
    assets: List<Asset>,
    onAddAsset: () -> Unit,
    onDeleteAsset: (Asset) -> Unit
) {
    var zoomImagePath by remember { mutableStateOf<String?>(null) }
    var assetToDelete by remember { mutableStateOf<Asset?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 110.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Amount card
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = RoundedCornerShape(24.dp), elevation = CardDefaults.cardElevation(6.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Amount", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                Text(formatCurrency(expense.amount), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DateRange, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(Modifier.width(6.dp))
                        Text(detailDate(expense.date), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
        }

        // Description + payment
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(4.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Description", style = MaterialTheme.typography.titleMedium)
                Text(expense.description, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 20.sp)
                Spacer(Modifier.height(4.dp))
                Text("Payment method", style = MaterialTheme.typography.titleMedium)
                if (expense.paymentMethod != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val iconRes = paymentIconRes(expense.paymentIcon)
                        if (iconRes != null) Icon(painter = painterResource(id = iconRes), contentDescription = expense.paymentMethod, modifier = Modifier.size(20.dp), tint = Color.Unspecified)
                        Text(expense.paymentMethod, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Text("Not specified", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Assets
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(0.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Assets (${assets.size})", style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = onAddAsset) { Icon(Icons.Default.Add, "Add asset", tint = MaterialTheme.colorScheme.primary) }
                }
                if (assets.isEmpty()) {
                    Text("No assets attached yet", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        assets.chunked(2).forEach { row ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { asset -> AssetGridTile(asset, Modifier.weight(1f).aspectRatio(1f), { zoomImagePath = asset.imagePath }, { assetToDelete = asset }) }
                                if (row.size == 1) Spacer(Modifier.weight(1f).aspectRatio(1f))
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(110.dp))
    }

    zoomImagePath?.let { ZoomableImageDialog(imageModel = it, onDismiss = { zoomImagePath = null }) }

    assetToDelete?.let { asset ->
        AlertDialog(
            onDismissRequest = { assetToDelete = null },
            icon    = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
            title   = { Text("Delete asset?") },
            text    = { Text("This image will be removed from this expense and from the assets gallery.") },
            confirmButton = { Button(onClick = { onDeleteAsset(asset); assetToDelete = null }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { assetToDelete = null }) { Text("Cancel") } },
            shape   = RoundedCornerShape(16.dp)
        )
    }
}

private fun detailDate(ts: Long): String = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(Date(ts))