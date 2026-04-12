package com.example.paisatracker.ui.pending


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.paisatracker.PaisaTrackerApplication
import com.example.paisatracker.data.Category
import com.example.paisatracker.data.PendingTransaction
import com.example.paisatracker.data.Project

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


// ─────────────────────────────────────────────────────────────────────────────
//  Screen 2: Review Pending Transactions
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingTransactionsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val app     = context.applicationContext as PaisaTrackerApplication
    val vm: PendingTransactionViewModel = viewModel(
        factory = PendingTransactionViewModelFactory(app.repository, context)
    )

    val pending    by vm.pendingList.collectAsState()
    val projects   by vm.allProjects.collectAsState()
    val categories by vm.allCategories.collectAsState()

    var editingTxn  by remember { mutableStateOf<PendingTransaction?>(null) }
    val editSheet   = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Review", fontWeight = FontWeight.Bold)
                        if (pending.isNotEmpty()) Text("${pending.size} pending", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (pending.isNotEmpty()) {
                        TextButton(onClick = { vm.discardAll() }) { Text("Discard all", color = MaterialTheme.colorScheme.error) }
                        val saveable = pending.count { it.categoryId != null }
                        if (saveable > 0) {
                            TextButton(onClick = { vm.saveAll() }) { Text("Save $saveable", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { pad ->
        if (pending.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("🎉", fontSize = 48.sp)
                    Text("All caught up!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("No pending transactions to review.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(pad),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(pending, key = { it.id }) { txn ->
                    PendingTxnCard(
                        txn        = txn,
                        projects   = projects,
                        categories = categories,
                        onCategoryAssigned = { catId, projId ->
                            vm.updatePending(txn.copy(categoryId = catId, projectId = projId))
                        },
                        onDiscard  = { vm.discard(txn) },
                        onEdit     = { editingTxn = txn },
                        onSave     = { cat, proj ->
                            vm.saveAsExpense(
                                txn        = txn,
                                categoryId = cat,
                                projectId  = proj,
                                note       = txn.note,
                                amount     = txn.amount,
                                payeeName  = txn.payeeName
                            )
                        }
                    )
                }
            }
        }
    }

    // ── Edit bottom sheet ──────────────────────────────────────────────────────
    editingTxn?.let { txn ->
        ModalBottomSheet(
            onDismissRequest = { editingTxn = null },
            sheetState       = editSheet,
            shape            = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            EditPendingSheet(
                txn        = txn,
                projects   = projects,
                categories = categories,
                onDismiss  = { editingTxn = null },
                onSave     = { updated ->
                    vm.saveAsExpense(
                        txn        = updated,
                        categoryId = updated.categoryId ?: return@EditPendingSheet,
                        projectId  = updated.projectId  ?: 0L,
                        note       = updated.note,
                        amount     = updated.amount,
                        payeeName  = updated.payeeName
                    )
                    editingTxn = null
                },
                onDiscard  = { vm.discard(txn); editingTxn = null }
            )
        }
    }
}

// ─── Pending transaction card ─────────────────────────────────────────────────

@Composable
private fun PendingTxnCard(
    txn: PendingTransaction,
    projects: List<Project>,
    categories: List<Category>,
    onCategoryAssigned: (Long, Long) -> Unit,
    onDiscard: () -> Unit,
    onEdit: () -> Unit,
    onSave: (Long, Long) -> Unit
) {
    val assignedCat = categories.find { it.id == txn.categoryId }
    val assignedProj = projects.find { it.id == txn.projectId }
    val filteredCats = if (txn.projectId != null) categories.filter { it.projectId == txn.projectId } else emptyList()

    val appInitial = txn.sourceApp.firstOrNull()?.toString() ?: "?"
    val appColor   = when (txn.sourceApp) {
        "GPay"     -> MaterialTheme.colorScheme.primaryContainer
        "PhonePe"  -> MaterialTheme.colorScheme.tertiaryContainer
        "Paytm"    -> MaterialTheme.colorScheme.secondaryContainer
        else       -> MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // ── Header ────────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                    Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(appColor), contentAlignment = Alignment.Center) {
                        Text(appInitial, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(txn.payeeName.ifBlank { "Payment" }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            buildString {
                                append(txn.sourceApp)
                                if (txn.payeeVpa.isNotBlank()) append(" · ${txn.payeeVpa}")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("₹${"%.2f".format(txn.amount)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        txn.transactionDate.ifBlank {
                            SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(txn.capturedAt))
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    )
                }
            }

            // ── Status + UTR badges ───────────────────────────────────────────
            Row(modifier = Modifier.padding(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StatusBadge(txn.status)
                if (txn.utrNumber.isNotBlank()) {
                    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                        Text("UTR ${txn.utrNumber.take(8)}…", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // ── Quick category assign ─────────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 14.dp)) {
                Text("CATEGORY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f), letterSpacing = 0.6.sp)
                Spacer(Modifier.height(5.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Project pills first
                    items(projects.take(3)) { proj ->
                        val sel = proj.id == txn.projectId
                        Pill(label = "${proj.emoji} ${proj.name}", isSelected = sel) {
                            onCategoryAssigned(txn.categoryId ?: -1L, proj.id)
                        }
                    }
                    // Category pills (filtered by selected project, or first 4)
                    val catsToShow = if (filteredCats.isNotEmpty()) filteredCats.take(4) else categories.take(4)
                    items(catsToShow) { cat ->
                        val sel = cat.id == txn.categoryId
                        Pill(label = "${cat.emoji} ${cat.name}", isSelected = sel) {
                            onCategoryAssigned(cat.id, txn.projectId ?: cat.projectId)
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // ── Action buttons ────────────────────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onDiscard,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Discard", style = MaterialTheme.typography.labelMedium) }

                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("Edit", style = MaterialTheme.typography.labelMedium) }

                Button(
                    onClick  = { if (txn.categoryId != null) onSave(txn.categoryId, txn.projectId ?: 0L) else onEdit() },
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(10.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = if (txn.categoryId != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor   = if (txn.categoryId != null) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                ) { Text("Save", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

// ─── Edit pending sheet ───────────────────────────────────────────────────────

@Composable
private fun EditPendingSheet(
    txn: PendingTransaction,
    projects: List<Project>,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (PendingTransaction) -> Unit,
    onDiscard: () -> Unit
) {
    var amount    by remember { mutableStateOf("%.2f".format(txn.amount)) }
    var name      by remember { mutableStateOf(txn.payeeName) }
    var note      by remember { mutableStateOf(txn.note) }
    var selProjId by remember { mutableStateOf(txn.projectId) }
    var selCatId  by remember { mutableStateOf(txn.categoryId) }

    val filteredCats = if (selProjId != null) categories.filter { it.projectId == selProjId } else emptyList()

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
            Text("Edit transaction", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(txn.sourceApp, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
        }

        OutlinedTextField(value = amount, onValueChange = { if (it.matches(Regex("^\\d*\\.?\\d{0,2}\$"))) amount = it }, label = { Text("Amount") }, leadingIcon = { Text("₹", modifier = Modifier.padding(start = 12.dp)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Payee name") }, singleLine = true, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Note (optional)") }, singleLine = true, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())

        // Project pills
        Text("PROJECT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f), letterSpacing = 0.8.sp)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(projects) { proj ->
                PillLarge(label = "${proj.emoji} ${proj.name}", isSelected = selProjId == proj.id) {
                    selProjId = proj.id; selCatId = null
                }
            }
        }

        // Category pills (filtered by project)
        if (selProjId != null && filteredCats.isNotEmpty()) {
            Text("CATEGORY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f), letterSpacing = 0.8.sp)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(filteredCats) { cat ->
                    PillLarge(label = "${cat.emoji} ${cat.name}", isSelected = selCatId == cat.id) { selCatId = cat.id }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onDiscard, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Discard") }
            Button(
                onClick  = {
                    val a = amount.toDoubleOrNull() ?: txn.amount
                    val cat = selCatId ?: return@Button
                    onSave(txn.copy(amount = a, payeeName = name.trim().ifBlank { txn.payeeName }, note = note.trim(), categoryId = cat, projectId = selProjId))
                },
                enabled  = selCatId != null,
                modifier = Modifier.weight(2f),
                shape    = RoundedCornerShape(12.dp)
            ) { Text("Save Expense", fontWeight = FontWeight.Bold) }
        }
        Spacer(Modifier.height(8.dp))
    }
}

// ─── Shared sub-components ────────────────────────────────────────────────────

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.fillMaxWidth(), content = content)
    }
}

@Composable
private fun SettingsToggleRow(title: String, subtitle: String?, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) }
        }
        Switch(checked = checked, onCheckedChange = onChecked, modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun StatusBadge(status: String) {
    val (bg, fg) = when (status) {
        "Success" -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.tertiary
        "Failed"  -> MaterialTheme.colorScheme.errorContainer    to MaterialTheme.colorScheme.error
        else      -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.secondary
    }
    Surface(shape = RoundedCornerShape(20.dp), color = bg) {
        Text(status, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = fg, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
    }
}

@Composable
private fun Pill(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(modifier = Modifier.height(28.dp).clip(RoundedCornerShape(20.dp)).background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant).clickable(onClick = onClick).padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal, color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f), maxLines = 1)
    }
}

@Composable
private fun PillLarge(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(modifier = Modifier.height(34.dp).clip(RoundedCornerShape(20.dp)).background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant).clickable(onClick = onClick).padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal, color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f), maxLines = 1)
    }
}