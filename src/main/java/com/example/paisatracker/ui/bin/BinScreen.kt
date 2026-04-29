package com.example.paisatracker.ui.bin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.paisatracker.ui.common.DeleteConfirmationSheetContent
import com.example.paisatracker.PaisaTrackerViewModel
import com.example.paisatracker.data.ActionHistory
import com.example.paisatracker.data.Expense
import com.example.paisatracker.data.Project
import com.example.paisatracker.data.Category
import com.example.paisatracker.data.Budget
import com.example.paisatracker.data.SalaryRecord
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BinSheetContent(viewModel: PaisaTrackerViewModel, onDismiss: () -> Unit) {
    val history by viewModel.actionHistory.collectAsState()
    val deletedItems = history.filter { it.actionType == "DELETE" }
    var showClearConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Recycle Bin",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Items are deleted after 30 days",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            if (deletedItems.isNotEmpty()) {
                TextButton(
                    onClick = { showClearConfirm = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear All")
                }
            }
        }

        if (deletedItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Your bin is empty",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(deletedItems, key = { it.id }) { item ->
                    BinItemRow(
                        item = item,
                        onRestore = { viewModel.restoreAction(item) },
                        onDelete = { viewModel.deleteAction(item) }
                    )
                }
            }
        }
    }

    if (showClearConfirm) {
        ModalBottomSheet(
            onDismissRequest = { showClearConfirm = false }
        ) {
            DeleteConfirmationSheetContent(
                title = "Clear Bin?",
                message = "This will permanently delete all items in the recycle bin. This action cannot be undone.",
                onConfirm = {
                    viewModel.clearBin()
                    showClearConfirm = false
                },
                onDismiss = { showClearConfirm = false },
                confirmText = "Clear All"
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BinScreen(viewModel: PaisaTrackerViewModel, navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recycle Bin") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            BinSheetContent(viewModel = viewModel, onDismiss = { navController.popBackStack() })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BinItemRow(
    item: ActionHistory,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    val daysRemaining = 30 - TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - item.timestamp)
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val (title, subtitle, icon, emoji) = remember(item) {
        try {
            val gson = Gson()
            when (item.entityType) {
                "PROJECT" -> {
                    val data = gson.fromJson(item.entityData, JsonObject::class.java)
                    val project = gson.fromJson(data.getAsJsonObject("project"), Project::class.java)
                    val children = data.getAsJsonArray("children")
                    val childCount = children?.size() ?: 0
                    val totalExpenses = children?.sumOf { 
                        it.asJsonObject.getAsJsonArray("expenses")?.size() ?: 0 
                    } ?: 0
                    
                    Quadruple(
                        project.name, 
                        "Project • $childCount categories, $totalExpenses expenses", 
                        Icons.Default.Folder,
                        project.emoji
                    )
                }
                "CATEGORY" -> {
                    val data = gson.fromJson(item.entityData, JsonObject::class.java)
                    val category = gson.fromJson(data.getAsJsonObject("category"), Category::class.java)
                    val expenses = data.getAsJsonArray("expenses")
                    val expCount = expenses?.size() ?: 0
                    
                    Quadruple(
                        category.name, 
                        "Category • $expCount expenses", 
                        Icons.Default.Category,
                        category.emoji
                    )
                }
                "EXPENSE" -> {
                    val expense = gson.fromJson(item.entityData, Expense::class.java)
                    Quadruple(
                        expense.description.ifBlank { "Unlabeled Expense" }, 
                        "Expense • ${expense.amount}", 
                        Icons.Default.ReceiptLong,
                        null
                    )
                }
                "BUDGET" -> {
                    val budget = gson.fromJson(item.entityData, Budget::class.java)
                    Quadruple(
                        budget.name,
                        "Budget • ${budget.limitAmount} (${budget.period.displayName})",
                        Icons.Default.AccountBalanceWallet,
                        budget.emoji
                    )
                }
                "SALARY_RECORD" -> {
                    val record = gson.fromJson(item.entityData, SalaryRecord::class.java)
                    Quadruple(
                        "Salary: ${record.month}/${record.year}",
                        "Income • ${record.amount}",
                        Icons.Default.Payments,
                        "💰"
                    )
                }
                else -> Quadruple(item.entityType, "Deleted item", Icons.Default.Info, "🗑️")
            }
        } catch (e: Exception) {
            Quadruple(item.entityType, "Deleted item", Icons.Default.Info, "🗑️")
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (emoji != null) {
                        Text(text = emoji, fontSize = 20.sp)
                    } else {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = subtitle.substringBefore("•").trim(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = " • $daysRemaining days left",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            Row {
                IconButton(onClick = onRestore) {
                    Icon(
                        imageVector = Icons.Default.Restore,
                        contentDescription = "Restore",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete Permanently",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        ModalBottomSheet(
            onDismissRequest = { showDeleteConfirm = false }
        ) {
            DeleteConfirmationSheetContent(
                title = "Delete Permanently?",
                message = "Are you sure you want to delete this $title permanently? This cannot be undone.",
                onConfirm = {
                    onDelete()
                    showDeleteConfirm = false
                },
                onDismiss = { showDeleteConfirm = false }
            )
        }
    }
}

data class Quadruple<out A, out B, out C, out D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
