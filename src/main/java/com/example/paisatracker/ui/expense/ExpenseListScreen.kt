package com.example.paisatracker.ui.expense

import android.Manifest
import android.app.DatePickerDialog
import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.DatePicker
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.paisatracker.PaisaTrackerViewModel
import com.example.paisatracker.R
import com.example.paisatracker.data.Expense
import com.example.paisatracker.ui.common.SortDropdown
import com.example.paisatracker.ui.common.SortOption
import com.example.paisatracker.util.formatCurrency
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private fun debugLog(context: Context, msg: String) {
    Log.d("PT_DEBUG", msg)
}

@DrawableRes
fun paymentIconRes(key: String?): Int? = when (key) {
    "upi" -> R.drawable.ic_upi_payment_icon
    "phonepe" -> R.drawable.ic_phonepe_icon
    "gpay" -> R.drawable.ic_google_pay_icon
    "paytm" -> R.drawable.ic_paytm_icon
    "cash" -> R.drawable.ic_cash_icon
    "card" -> R.drawable.ic_card_icon
    else -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseListScreen(
    viewModel: PaisaTrackerViewModel,
    categoryId: Long,
    navController: NavController
) {
    val expenses by viewModel.getExpensesForCategory(categoryId)
        .collectAsState(initial = emptyList())

    // sort state
    var expenseSortOption by remember { mutableStateOf(SortOption.AMOUNT_HIGH_LOW) }

    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var newExpenseAmount by remember { mutableStateOf("") }
    var newExpenseDescription by remember { mutableStateOf("") }
    var newExpenseDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var newPaymentMethod by remember { mutableStateOf<String?>(null) }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }

    var showEditDialog by remember { mutableStateOf(false) }
    var expenseToEdit by remember { mutableStateOf<Expense?>(null) }
    var editedExpenseAmount by remember { mutableStateOf("") }
    var editedExpenseDescription by remember { mutableStateOf("") }
    var editedExpenseDate by remember { mutableStateOf(0L) }
    var editedPaymentMethod by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current

    var newExpenseImageUri by remember { mutableStateOf<Uri?>(null) }
    var editedExpenseImageUri by remember { mutableStateOf<Uri?>(null) }

    // ======= Gallery =======
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        debugLog(context, "Gallery result uri=$uri, add=$showAddExpenseDialog edit=$showEditDialog")
        uri ?: return@rememberLauncherForActivityResult
        if (showAddExpenseDialog) {
            newExpenseImageUri = uri
        } else if (showEditDialog) {
            editedExpenseImageUri = uri
        }
    }

    // ======= Camera =======
    val cameraUri = remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        debugLog(context, "Camera callback success=$success, uri=${cameraUri.value}")
        if (success) {
            cameraUri.value?.let { uri ->
                if (showAddExpenseDialog) {
                    newExpenseImageUri = uri
                    debugLog(context, "Camera ADD uri set: $uri")
                } else if (showEditDialog) {
                    editedExpenseImageUri = uri
                    debugLog(context, "Camera EDIT uri set: $uri")
                }
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                val imagesDir = File(context.filesDir, "images")
                if (!imagesDir.exists()) imagesDir.mkdirs()

                val photoFile = File(
                    imagesDir,
                    "IMG_${System.currentTimeMillis()}.jpg"
                )

                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    photoFile
                )
                cameraUri.value = uri
                debugLog(context, "Camera uri created: $uri")
                cameraLauncher.launch(uri)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Camera error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(context, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f),
            MaterialTheme.colorScheme.background
        )
    )

    // sorted list yahi calculate karo
    val sortedExpenses = remember(expenses, expenseSortOption) {
        when (expenseSortOption) {
            SortOption.AMOUNT_LOW_HIGH ->
                expenses.sortedBy { it.amount }
            SortOption.AMOUNT_HIGH_LOW ->
                expenses.sortedByDescending { it.amount }
            SortOption.NAME_A_Z ->
                expenses.sortedBy { it.description.lowercase() }
            SortOption.NAME_Z_A ->
                expenses.sortedByDescending { it.description.lowercase() }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    newExpenseDate = System.currentTimeMillis()
                    newPaymentMethod = null
                    newExpenseAmount = ""
                    newExpenseDescription = ""
                    newExpenseImageUri = null
                    showAddExpenseDialog = true
                },
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Expense")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundGradient)
                .padding(paddingValues)
        ) {
            if (expenses.isEmpty()) {
                EmptyExpenseState()
            } else {
                Column {
                    ExpenseSummaryHeader(expenses = expenses)

                    // yahi sort filter chip
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        SortDropdown(
                            current = expenseSortOption,
                            onChange = { expenseSortOption = it }
                        )
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = 110.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(sortedExpenses, key = { it.id }) { expense ->
                            ExpenseListItem(
                                expense = expense,
                                onClick = {
                                    navController.navigate("expense_details/${expense.id}")
                                },
                                onEditClick = {
                                    expenseToEdit = expense
                                    editedExpenseAmount = expense.amount.toString()
                                    editedExpenseDescription = expense.description
                                    editedExpenseDate = expense.date
                                    editedPaymentMethod = expense.paymentMethod
                                    editedExpenseImageUri = null
                                    showEditDialog = true
                                },
                                onDeleteClick = {
                                    expenseToDelete = expense
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // ========== ADD dialog ==========
    if (showAddExpenseDialog) {
        ExpenseDialog(
            title = "New Expense",
            amount = newExpenseAmount,
            onAmountChange = { newExpenseAmount = it },
            description = newExpenseDescription,
            onDescriptionChange = { newExpenseDescription = it },
            date = newExpenseDate,
            onDateChange = { newExpenseDate = it },
            paymentMethod = newPaymentMethod,
            onPaymentMethodChange = { method -> newPaymentMethod = method },
            onPickFromGallery = {
                galleryLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onPickFromCamera = {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            },
            previewImageUri = newExpenseImageUri,
            onConfirm = {
                debugLog(context, "ADD onConfirm, uri=$newExpenseImageUri")
                val amount = newExpenseAmount.toDoubleOrNull()
                if (newExpenseDescription.isNotBlank() && amount != null) {
                    val method = newPaymentMethod?.takeIf { it.isNotBlank() }
                    val expense = Expense(
                        amount = amount,
                        description = newExpenseDescription,
                        date = newExpenseDate,
                        categoryId = categoryId,
                        paymentMethod = method,
                        paymentIcon = method.toPaymentIconKey(),
                        assetPath = null
                    )

                    // Uri + title dono ko local var me freeze karo
                    val pickedUri = newExpenseImageUri
                    val pickedTitle = newExpenseDescription

                    viewModel.insertExpenseWithResult(expense) { newExpenseId ->
                        debugLog(
                            context,
                            "insertExpenseWithResult callback, id=$newExpenseId, uri=$pickedUri, title=$pickedTitle"
                        )
                        pickedUri?.let { uri ->
                            val assetTitle =
                                pickedTitle.ifBlank { "Expense #$newExpenseId" }

                            viewModel.addLinkedAsset(
                                context = context,
                                uri = uri,
                                title = assetTitle,
                                description = "",
                                expenseId = newExpenseId
                            )

                            debugLog(context, "addLinkedAsset called for id=$newExpenseId")
                        }
                    }

                    newExpenseAmount = ""
                    newExpenseDescription = ""
                    newPaymentMethod = null
                    newExpenseImageUri = null
                    showAddExpenseDialog = false
                }
            },
            onDismiss = {
                showAddExpenseDialog = false
                newExpenseImageUri = null
            }
        )
    }


    // ========== DELETE dialog ==========
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Delete Expense", style = MaterialTheme.typography.headlineSmall) },
            text = {
                Text(
                    "Are you sure you want to delete '${expenseToDelete?.description}'?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        expenseToDelete?.let { viewModel.deleteExpense(it) }
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // ========== EDIT dialog ==========
    if (showEditDialog) {
        ExpenseDialog(
            title = "Edit Expense",
            amount = editedExpenseAmount,
            onAmountChange = { editedExpenseAmount = it },
            description = editedExpenseDescription,
            onDescriptionChange = { editedExpenseDescription = it },
            date = editedExpenseDate,
            onDateChange = { editedExpenseDate = it },
            paymentMethod = editedPaymentMethod,
            onPaymentMethodChange = { method -> editedPaymentMethod = method },
            onPickFromGallery = {
                galleryLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onPickFromCamera = {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            },
            previewImageUri = editedExpenseImageUri,
            onConfirm = {
                debugLog(context, "EDIT onConfirm, uri=$editedExpenseImageUri")
                val amount = editedExpenseAmount.toDoubleOrNull()
                if (editedExpenseDescription.isNotBlank() && amount != null) {
                    val method = editedPaymentMethod?.takeIf { it.isNotBlank() }

                    // Uri + title local var me freeze
                    val pickedUri = editedExpenseImageUri
                    val pickedTitle = editedExpenseDescription

                    expenseToEdit?.let { old ->
                        viewModel.updateExpense(
                            old.copy(
                                amount = amount,
                                description = editedExpenseDescription,
                                date = editedExpenseDate,
                                paymentMethod = method,
                                paymentIcon = method.toPaymentIconKey()
                            )
                        )
                        pickedUri?.let { uri ->
                            val assetTitle =
                                pickedTitle.ifBlank { "Expense #${old.id}" }

                            viewModel.addLinkedAsset(
                                context = context,
                                uri = uri,
                                title = assetTitle,
                                description = "",
                                expenseId = old.id
                            )
                            debugLog(context, "addLinkedAsset (edit) called for id=${old.id}")
                        }
                    }
                    editedExpenseImageUri = null
                    showEditDialog = false
                }
            },
            onDismiss = {
                showEditDialog = false
                editedExpenseImageUri = null
            }
        )
    }

}

@Composable
fun EmptyExpenseState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_expense_icon),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "No expenses yet",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Add your first expense to get started!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ExpenseSummaryHeader(expenses: List<Expense>) {
    val totalAmount = expenses.sumOf { it.amount }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Total Spent",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatCurrency(totalAmount),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "${expenses.size}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseDialog(
    title: String,
    amount: String,
    onAmountChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    date: Long,
    onDateChange: (Long) -> Unit,
    paymentMethod: String?,
    onPaymentMethodChange: (String?) -> Unit,
    onPickFromGallery: () -> Unit,
    onPickFromCamera: () -> Unit,
    previewImageUri: Uri?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance().apply { timeInMillis = date }
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)

    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, selectedYear: Int, selectedMonth: Int, selectedDay: Int ->
            val newCalendar = Calendar.getInstance().apply {
                set(selectedYear, selectedMonth, selectedDay)
            }
            onDateChange(newCalendar.timeInMillis)
        },
        year, month, day
    )

    val methods = listOf("UPI", "PhonePe", "GPay", "Paytm", "Cash", "Card")
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                if (title.startsWith("Edit")) Icons.Default.Edit else Icons.Default.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = { Text(title, style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = onAmountChange,
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    leadingIcon = { Text("₹", style = MaterialTheme.typography.titleMedium) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = onDescriptionChange,
                    label = { Text("Description") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Column {
                    Text(
                        text = "Payment via (optional)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = paymentMethod ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select method") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                            },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            methods.forEach { method ->
                                DropdownMenuItem(
                                    text = { Text(method) },
                                    onClick = {
                                        onPaymentMethodChange(method)
                                        expanded = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("None") },
                                onClick = {
                                    onPaymentMethodChange(null)
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Text(
                    text = "Attach asset (optional)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onPickFromGallery,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("From gallery")
                    }
                    OutlinedButton(
                        onClick = onPickFromCamera,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("From camera")
                    }
                }

                if (previewImageUri != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Selected image:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        tonalElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                    ) {
                        AsyncImage(
                            model = previewImageUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                OutlinedButton(
                    onClick = { datePickerDialog.show() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = formatDate(date), style = MaterialTheme.typography.bodyLarge)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (title.startsWith("Edit")) "Save" else "Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun ExpenseListItem(
    expense: Expense,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            hoveredElevation = 6.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.1f),
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.05f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_expense_icon),
                                contentDescription = "Expense Icon",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = expense.description,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 20.sp
                            )
                        }
                    }

                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "More Options",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("Edit", style = MaterialTheme.typography.bodyMedium)
                                    }
                                },
                                onClick = {
                                    onEditClick()
                                    menuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            "Delete",
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                },
                                onClick = {
                                    onDeleteClick()
                                    menuExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    thickness = 0.5.dp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
                                        )
                                    )
                                )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.DateRange,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = formatDate(expense.date),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }

                        expense.paymentMethod?.let { method ->
                            val iconRes = paymentIconRes(expense.paymentIcon)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                    )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (iconRes != null) {
                                        Icon(
                                            painter = painterResource(id = iconRes),
                                            contentDescription = method,
                                            modifier = Modifier.size(14.dp),
                                            tint = Color.Unspecified
                                        )
                                    }
                                    Text(
                                        text = method,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    )
                                )
                            )
                    ) {
                        Text(
                            text = formatCurrency(expense.amount),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun String?.toPaymentIconKey(): String? = when (this) {
    "UPI" -> "upi"
    "PhonePe" -> "phonepe"
    "GPay" -> "gpay"
    "Paytm" -> "paytm"
    "Cash" -> "cash"
    "Card" -> "card"
    else -> null
}
