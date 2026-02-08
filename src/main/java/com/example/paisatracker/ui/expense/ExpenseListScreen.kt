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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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

// Move sealed class outside composable to avoid 'local class' sealed error
sealed class SheetState {
    object Add : SheetState()
    data class Edit(val expense: Expense) : SheetState()
    data class Delete(val expense: Expense) : SheetState()
}

enum class ExpenseViewType {
    GRID, LIST
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
    var currentViewType by remember { mutableStateOf(ExpenseViewType.LIST) }

    var currentSheet by remember { mutableStateOf<SheetState?>(null) }

    var newExpenseAmount by remember { mutableStateOf("") }
    var newExpenseDescription by remember { mutableStateOf("") }
    var newExpenseDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var newPaymentMethod by remember { mutableStateOf<String?>(null) }

    var editedExpenseAmount by remember { mutableStateOf("") }
    var editedExpenseDescription by remember { mutableStateOf("") }
    var editedExpenseDate by remember { mutableStateOf(0L) }
    var editedPaymentMethod by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var newExpenseImageUri by remember { mutableStateOf<Uri?>(null) }
    var editedExpenseImageUri by remember { mutableStateOf<Uri?>(null) }

    // ======= Gallery =======
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        debugLog(context, "Gallery result uri=$uri, sheet=$currentSheet")
        uri ?: return@rememberLauncherForActivityResult
        when (currentSheet) {
            is SheetState.Add -> newExpenseImageUri = uri
            is SheetState.Edit -> editedExpenseImageUri = uri
            else -> {}
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
                when (currentSheet) {
                    is SheetState.Add -> {
                        newExpenseImageUri = uri
                        debugLog(context, "Camera ADD uri set: $uri")
                    }
                    is SheetState.Edit -> {
                        editedExpenseImageUri = uri
                        debugLog(context, "Camera EDIT uri set: $uri")
                    }
                    else -> {}
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
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f),
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.18f),
            MaterialTheme.colorScheme.background
        )
    )

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

            SortOption.DATE_OLD_NEW ->
                expenses.sortedBy { it.date }

            SortOption.DATE_NEW_OLD ->
                expenses.sortedByDescending { it.date }
        }
    }


    // Bottom sheet state
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

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
                    currentSheet = SheetState.Add
                },
                modifier = Modifier
                    .padding(bottom = 80.dp)
                    .shadow(16.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(20.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
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

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ExpenseViewTypeToggle(
                            currentViewType = currentViewType,
                            onViewTypeChange = { currentViewType = it }
                        )

                        SortDropdown(current = expenseSortOption, onChange = { expenseSortOption = it })
                    }

                    when (currentViewType) {
                        ExpenseViewType.LIST -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 110.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(sortedExpenses, key = { it.id }) { expense ->
                                    ExpenseListItem(
                                        expense = expense,
                                        onClick = { navController.navigate("expense_details/${expense.id}") },
                                        onEditClick = {
                                            expenseToEditPrep(
                                                expense,
                                                onSetAmount = { editedExpenseAmount = it },
                                                onSetDesc = { editedExpenseDescription = it },
                                                onSetDate = { editedExpenseDate = it },
                                                onSetMethod = { editedPaymentMethod = it }
                                            )
                                            editedExpenseImageUri = null
                                            currentSheet = SheetState.Edit(expense)
                                        },
                                        onDeleteClick = { currentSheet = SheetState.Delete(expense) }
                                    )
                                }
                            }
                        }

                        ExpenseViewType.GRID -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 110.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(
                                    items = sortedExpenses.chunked(2),
                                    key = { row -> row.first().id }
                                ) { rowItems ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        rowItems.forEach { expense ->
                                            ExpenseGridItem(
                                                expense = expense,
                                                onClick = { navController.navigate("expense_details/${expense.id}") },
                                                onEditClick = {
                                                    expenseToEditPrep(
                                                        expense,
                                                        onSetAmount = { editedExpenseAmount = it },
                                                        onSetDesc = { editedExpenseDescription = it },
                                                        onSetDate = { editedExpenseDate = it },
                                                        onSetMethod = { editedPaymentMethod = it }
                                                    )
                                                    editedExpenseImageUri = null
                                                    currentSheet = SheetState.Edit(expense)
                                                },
                                                onDeleteClick = { currentSheet = SheetState.Delete(expense) },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        if (rowItems.size == 1) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Show ModalBottomSheet when currentSheet != null
            currentSheet?.let { sheet ->
                ModalBottomSheet(onDismissRequest = { currentSheet = null }, sheetState = sheetState, tonalElevation = 10.dp) {
                    // Add a visible drag handle and a rounded top container for better Pixel feel
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp), contentAlignment = Alignment.TopCenter) {
                        Box(modifier = Modifier
                            .width(48.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)))
                    }

                    when (sheet) {
                        is SheetState.Add -> {
                            ExpenseBottomSheetContent(
                                title = "New Expense",
                                amount = newExpenseAmount,
                                onAmountChange = { newExpenseAmount = it },
                                description = newExpenseDescription,
                                onDescriptionChange = { newExpenseDescription = it },
                                date = newExpenseDate,
                                onDateChange = { newExpenseDate = it },
                                paymentMethod = newPaymentMethod,
                                onPaymentMethodChange = { newPaymentMethod = it },
                                onPickFromGallery = { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                                onPickFromCamera = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                                previewImageUri = newExpenseImageUri,
                                onConfirm = {
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

                                        val pickedUri = newExpenseImageUri
                                        val pickedTitle = newExpenseDescription

                                        viewModel.insertExpenseWithResult(expense) { newExpenseId ->
                                            pickedUri?.let { uri ->
                                                val assetTitle = pickedTitle.ifBlank { "Expense #$newExpenseId" }
                                                viewModel.addLinkedAsset(context = context, uri = uri, title = assetTitle, description = "", expenseId = newExpenseId)
                                            }
                                        }

                                        newExpenseAmount = ""
                                        newExpenseDescription = ""
                                        newPaymentMethod = null
                                        newExpenseImageUri = null
                                        currentSheet = null
                                    } else {
                                        Toast.makeText(context, "Enter valid description & amount", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onDismiss = { currentSheet = null }
                            )
                        }

                        is SheetState.Edit -> {
                            ExpenseBottomSheetContent(
                                title = "Edit Expense",
                                amount = editedExpenseAmount,
                                onAmountChange = { editedExpenseAmount = it },
                                description = editedExpenseDescription,
                                onDescriptionChange = { editedExpenseDescription = it },
                                date = editedExpenseDate,
                                onDateChange = { editedExpenseDate = it },
                                paymentMethod = editedPaymentMethod,
                                onPaymentMethodChange = { editedPaymentMethod = it },
                                onPickFromGallery = { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                                onPickFromCamera = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                                previewImageUri = editedExpenseImageUri,
                                onConfirm = {
                                    val amount = editedExpenseAmount.toDoubleOrNull()
                                    if (editedExpenseDescription.isNotBlank() && amount != null) {
                                        val method = editedPaymentMethod?.takeIf { it.isNotBlank() }
                                        val pickedUri = editedExpenseImageUri
                                        val pickedTitle = editedExpenseDescription

                                        val old = sheet.expense
                                        viewModel.updateExpense(old.copy(
                                            amount = amount,
                                            description = editedExpenseDescription,
                                            date = editedExpenseDate,
                                            paymentMethod = method,
                                            paymentIcon = method.toPaymentIconKey()
                                        ))

                                        pickedUri?.let { uri ->
                                            val assetTitle = pickedTitle.ifBlank { "Expense #${old.id}" }
                                            viewModel.addLinkedAsset(context = context, uri = uri, title = assetTitle, description = "", expenseId = old.id)
                                        }

                                        editedExpenseImageUri = null
                                        currentSheet = null
                                    } else {
                                        Toast.makeText(context, "Enter valid description & amount", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onDismiss = { currentSheet = null }
                            )
                        }

                        is SheetState.Delete -> {
                            val expenseToDelete = sheet.expense

                            DeleteBottomSheetContent(
                                expense = expenseToDelete,
                                onCancel = { currentSheet = null },
                                onConfirm = {
                                    viewModel.deleteExpense(expenseToDelete)
                                    currentSheet = null
                                }
                            )
                        }

                    }
                }
            }
        }
    }
}

@Composable
fun ExpenseViewTypeToggle(
    currentViewType: ExpenseViewType,
    onViewTypeChange: (ExpenseViewType) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier
                .height(40.dp)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExpenseViewType.values().forEach { viewType ->
                val isSelected = currentViewType == viewType
                val backgroundColor = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    Color.Transparent
                val iconColor = if (isSelected)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant

                Surface(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onViewTypeChange(viewType) },
                    shape = RoundedCornerShape(8.dp),
                    color = backgroundColor
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (viewType == ExpenseViewType.GRID) "⊞" else "☰",
                            fontSize = 18.sp,
                            color = iconColor,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

// small helper to prefill edit fields
private fun expenseToEditPrep(
    expense: Expense,
    onSetAmount: (String) -> Unit,
    onSetDesc: (String) -> Unit,
    onSetDate: (Long) -> Unit,
    onSetMethod: (String?) -> Unit
) {
    onSetAmount(expense.amount.toString())
    onSetDesc(expense.description)
    onSetDate(expense.date)
    onSetMethod(expense.paymentMethod)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseBottomSheetContent(
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
    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, selectedYear: Int, selectedMonth: Int, selectedDay: Int ->
            val newCalendar = Calendar.getInstance().apply { set(selectedYear, selectedMonth, selectedDay) }
            onDateChange(newCalendar.timeInMillis)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(text = title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            TextButton(onClick = onDismiss) { Text("Close") }
        }

        Spacer(modifier = Modifier.height(8.dp))

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

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = { Text("Description") },
            singleLine = false,
            maxLines = 3,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        val methods = listOf("UPI", "PhonePe", "GPay", "Paytm", "Cash", "Card")
        var expanded by remember { mutableStateOf(false) }

        Text(text = "Payment via (optional)", style = MaterialTheme.typography.labelSmall)
        Spacer(modifier = Modifier.height(6.dp))
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = paymentMethod ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Select method") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                shape = RoundedCornerShape(12.dp)
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                methods.forEach { method ->
                    DropdownMenuItem(text = { Text(method) }, onClick = { onPaymentMethodChange(method); expanded = false })
                }
                DropdownMenuItem(text = { Text("None") }, onClick = { onPaymentMethodChange(null); expanded = false })
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(text = "Attach asset (optional)", style = MaterialTheme.typography.labelSmall)
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onPickFromGallery, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("From gallery") }
            OutlinedButton(onClick = onPickFromCamera, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("From camera") }
        }

        previewImageUri?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(shape = RoundedCornerShape(12.dp), tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth().height(160.dp)) {
                AsyncImage(model = it, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(onClick = { datePickerDialog.show() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = formatDate(date))
        }

        Spacer(modifier = Modifier.height(18.dp))

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), thickness = 1.dp)
        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = onConfirm, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                Text(text = if (title.startsWith("Edit")) "Save" else "Add")
            }
            OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("Cancel") }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
fun DeleteBottomSheetContent(expense: Expense, onCancel: () -> Unit, onConfirm: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
        Text(text = "Delete Expense", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Are you sure you want to delete '${expense.description}'?", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = onConfirm, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Delete") }
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancel") }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
fun EmptyExpenseState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Surface(modifier = Modifier.size(80.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(painter = painterResource(id = R.drawable.ic_expense_icon), contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "No expenses yet", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Add your first expense to get started!", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f), textAlign = TextAlign.Center)
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
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
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
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

@Composable
fun ExpenseListItem(
    expense: Expense,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val iconRes = paymentIconRes(expense.paymentIcon)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            hoveredElevation = 6.dp
        ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f),
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.08f),
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.04f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
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
                        // Use payment icon if available, otherwise use default expense icon
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (iconRes != null) {
                                Icon(
                                    painter = painterResource(id = iconRes),
                                    contentDescription = expense.paymentMethod,
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_expense_icon),
                                    contentDescription = "Expense Icon",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = expense.description,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 16.sp,
                                lineHeight = 19.sp
                            )
                        }
                    }

                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "More Options",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
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

                Spacer(modifier = Modifier.height(12.dp))

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    thickness = 0.5.dp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.36f),
                                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.18f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.DateRange,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = formatDate(expense.date),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                    )
                                )
                            )
                    ) {
                        Text(
                            text = formatCurrency(expense.amount),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExpenseGridItem(
    expense: Expense,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val iconRes = paymentIconRes(expense.paymentIcon)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Use payment icon if available
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (iconRes != null) {
                                Icon(
                                    painter = painterResource(id = iconRes),
                                    contentDescription = expense.paymentMethod,
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_expense_icon),
                                    contentDescription = "Expense",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Box {
                            IconButton(
                                onClick = { menuExpanded = true },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = "More",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Edit", style = MaterialTheme.typography.bodyMedium) },
                                    onClick = {
                                        onEditClick()
                                        menuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete", style = MaterialTheme.typography.bodyMedium) },
                                    onClick = {
                                        onDeleteClick()
                                        menuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Text(
                        text = expense.description,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 16.sp,
                        lineHeight = 19.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Footer
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = formatCurrency(expense.amount),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 19.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.DateRange,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = formatDate(expense.date),
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
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