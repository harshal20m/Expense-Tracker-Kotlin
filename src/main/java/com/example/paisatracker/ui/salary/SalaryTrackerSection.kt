package com.example.paisatracker.ui.salary

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.paisatracker.PaisaTrackerViewModel
import com.example.paisatracker.PaisaTrackerApplication
import com.example.paisatracker.data.SalaryRecord
import com.example.paisatracker.util.formatCurrency
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Self-contained salary tracker section.
 * Embed this directly inside the LazyColumn in ProjectListScreen as an `item { }` block.
 *
 * Usage:
 *   item { SalaryTrackerSection() }
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalaryTrackerSection(viewModel: PaisaTrackerViewModel) {
    val context = LocalContext.current
    val app     = context.applicationContext as PaisaTrackerApplication
    val vm: SalaryViewModel = viewModel(factory = SalaryViewModelFactory(app.repository, viewModel))

    val currentSalary    by vm.currentSalary.collectAsState()
    val totalSpent       by vm.totalSpentThisMonth.collectAsState()
    val remaining        by vm.remainingBalance.collectAsState()
    val spendPct         by vm.spendPercentage.collectAsState()
    val breakdown        by vm.categoryBreakdown.collectAsState()
    val history          by vm.allSalaryRecords.collectAsState()

    var expanded         by remember { mutableStateOf(true) }
    var showAddSheet     by remember { mutableStateOf(false) }
    var showHistory      by remember { mutableStateOf(false) }
    val editSheet        = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val animatedPct by animateFloatAsState(targetValue = spendPct, animationSpec = tween(800), label = "pct")
    val isOverBudget = remaining < 0

    Column(modifier = Modifier.padding(horizontal = 0.dp)) {

        // ── Section header ────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.AccountBalanceWallet, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Text("Monthly Budget", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                if (currentSalary == null) {
                    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.errorContainer) {
                        Text("Not set", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { showAddSheet = true }, modifier = Modifier.size(32.dp)) {
                    Icon(if (currentSalary != null) Icons.Default.Edit else Icons.Default.Add, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                }
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
            }
        }

        // ── Expandable content ────────────────────────────────────────────────
        AnimatedVisibility(visible = expanded, enter = expandVertically() + fadeIn(tween(200)), exit = shrinkVertically() + fadeOut(tween(150))) {
            if (currentSalary == null) {
                // No salary set — prompt card
                Card(
                    modifier  = Modifier.fillMaxWidth().clickable { showAddSheet = true },
                    shape     = RoundedCornerShape(18.dp),
                    colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(
                        modifier            = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("💰", fontSize = 32.sp)
                        Text("Set your salary to track spending", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text("PaisaTracker will show how much you've spent and what's left each month.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        Button(onClick = { showAddSheet = true }, shape = RoundedCornerShape(12.dp)) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Add salary for this month")
                        }
                    }
                }
            } else {
                Card(
                    modifier  = Modifier.fillMaxWidth(),
                    shape     = RoundedCornerShape(18.dp),
                    colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(3.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {

                        // ── Balance row ───────────────────────────────────────
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text("SALARY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f), letterSpacing = 0.6.sp)
                                Text(formatCurrency(currentSalary!!.amount), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                val df = SimpleDateFormat("dd MMM", Locale.getDefault())
                                Text("since ${df.format(Date(currentSalary!!.receivedAt))}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f))
                            }
                            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text("REMAINING", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f), letterSpacing = 0.6.sp)
                                Text(
                                    formatCurrency(remaining),
                                    style     = MaterialTheme.typography.headlineSmall,
                                    fontWeight= FontWeight.Bold,
                                    color     = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
                                )
                                Text(
                                    "spent ${formatCurrency(totalSpent)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                                )
                            }
                        }

                        // ── Progress bar ──────────────────────────────────────
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${(animatedPct * 100).toInt()}% spent", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
                                if (isOverBudget) Text("Over budget!", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
                            }
                            LinearProgressIndicator(
                                progress   = { animatedPct },
                                modifier   = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                color      = if (isOverBudget) MaterialTheme.colorScheme.error
                                             else if (animatedPct > 0.8f) MaterialTheme.colorScheme.secondary
                                             else MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                strokeCap  = StrokeCap.Round
                            )
                        }

                        // ── Category breakdown ────────────────────────────────
                        if (breakdown.isNotEmpty()) {
                            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Text("TOP SPENDING", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f), letterSpacing = 0.6.sp)
                            breakdown.take(4).forEach { cat ->
                                val pct = if (currentSalary!!.amount > 0) (cat.total / currentSalary!!.amount).toFloat().coerceIn(0f, 1f) else 0f
                                CategorySpendRow(cat.categoryEmoji, cat.categoryName, cat.total, pct)
                            }
                        }

                        // ── History toggle ────────────────────────────────────
                        if (history.size > 1) {
                            TextButton(
                                onClick  = { showHistory = !showHistory },
                                modifier = Modifier.fillMaxWidth(),
                                shape    = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    if (showHistory) "Hide history" else "View salary history (${history.size - 1} previous)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            AnimatedVisibility(visible = showHistory) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    history.drop(1).take(3).forEach { rec ->
                                        HistoryRow(rec, onDelete = { vm.deleteSalary(rec) })
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Add / edit salary sheet ───────────────────────────────────────────────
    if (showAddSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false },
            sheetState       = editSheet,
            shape            = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            AddSalarySheet(
                existing = currentSalary,
                onDismiss= { showAddSheet = false },
                onSave   = { amount, note ->
                    if (currentSalary != null) {
                        vm.updateSalary(currentSalary!!.copy(amount = amount, note = note))
                    } else {
                        vm.addSalary(amount, note)
                    }
                    showAddSheet = false
                }
            )
        }
    }
}

// ─── Sub-composables ──────────────────────────────────────────────────────────

@Composable
private fun CategorySpendRow(emoji: String, name: String, amount: Double, pct: Float) {
    val animated by animateFloatAsState(targetValue = pct, animationSpec = tween(600), label = "catPct")
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(emoji, fontSize = 14.sp)
        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text(formatCurrency(amount), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(3.dp))
            LinearProgressIndicator(progress = { animated }, modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.surfaceVariant, strokeCap = StrokeCap.Round)
        }
    }
}

@Composable
private fun HistoryRow(record: SalaryRecord, onDelete: () -> Unit) {
    val df = SimpleDateFormat("MMM yyyy", Locale.getDefault())
    val monthStr = try { df.format(java.util.GregorianCalendar(record.year, record.month - 1, 1).time) } catch (_: Exception) { "${record.month}/${record.year}" }
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)).padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text(monthStr, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            if (record.note.isNotBlank()) Text(record.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(formatCurrency(record.amount), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Delete, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
private fun AddSalarySheet(
    existing: SalaryRecord?,
    onDismiss: () -> Unit,
    onSave: (Double, String) -> Unit
) {
    var amountText by remember { mutableStateOf(existing?.amount?.let { "%.0f".format(it) } ?: "") }
    var note       by remember { mutableStateOf(existing?.note ?: "") }

    Column(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(if (existing != null) "Update salary" else "Add this month's salary", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
        OutlinedTextField(value = amountText, onValueChange = { if (it.matches(Regex("^\\d*\\.?\\d{0,2}\$"))) amountText = it }, label = { Text("Salary amount") }, leadingIcon = { Text("₹", modifier = Modifier.padding(start = 12.dp)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Note (e.g. April salary)") }, singleLine = true, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())

        // Info card
        Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)) {
            Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.Info, null, modifier = Modifier.size(16.dp).padding(top = 2.dp), tint = MaterialTheme.colorScheme.primary)
                Text("Tracking starts from today. All expenses added after this date will be deducted from this salary. ${if (existing != null) "Updating will replace this month's salary record." else "Resets when you add a new salary next month."}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer, lineHeight = 18.sp)
            }
        }

        Button(
            onClick  = {
                val a = amountText.toDoubleOrNull()
                if (a != null && a > 0) {
                    if (existing != null) {
                        onSave(a, note) // In this case onSave will handle the update
                    } else {
                        onSave(a, note)
                    }
                }
            },
            enabled  = amountText.toDoubleOrNull()?.let { it > 0 } == true,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = RoundedCornerShape(14.dp)
        ) {
            Text(if (existing != null) "Update Salary" else "Start Tracking", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
    }
}