package com.example.paisatracker.ui.budget

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.paisatracker.PaisaTrackerViewModel
import com.example.paisatracker.data.BudgetWithSpending
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    viewModel: PaisaTrackerViewModel,
    onNavigateBack: () -> Unit,
    currencySymbol: String = "₹"
) {
    val context = LocalContext.current
    val allBudgetsWithSpending by viewModel.budgetsWithSpending.collectAsState(initial = emptyList())

    // Separate active and inactive budgets
    val activeBudgets = allBudgetsWithSpending.filter { it.budget.isActive }
    val inactiveBudgets = allBudgetsWithSpending.filter { !it.budget.isActive }

    var showAddSheet by remember { mutableStateOf(false) }
    var showEditSheet by remember { mutableStateOf(false) }
    var budgetToDelete by remember { mutableStateOf<com.example.paisatracker.data.Budget?>(null) }
    var budgetToEdit by remember { mutableStateOf<com.example.paisatracker.data.Budget?>(null) }
    var showInactiveSection by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Budgets",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddSheet = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Budget")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        if (allBudgetsWithSpending.isEmpty()) {
            BudgetEmptyState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                onCreateClick = { showAddSheet = true }
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Summary card
                item {
                    BudgetSummaryCard(
                        budgets = activeBudgets,
                        currencySymbol = currencySymbol,
                        inactiveCount = inactiveBudgets.size
                    )
                }

                // Active Budgets Section
                if (activeBudgets.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Active Budgets",
                            count = activeBudgets.size,
                            icon = "🔥"
                        )
                    }

                    items(activeBudgets, key = { it.budget.id }) { budgetWithSpending ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + slideInVertically()
                        ) {
                            BudgetCard(
                                budgetWithSpending = budgetWithSpending,
                                currencySymbol = currencySymbol,
                                isActive = true,
                                onEdit = {
                                    budgetToEdit = it.budget
                                    showEditSheet = true
                                },
                                onDelete = {
                                    budgetToDelete = it.budget
                                },
                                onToggleActive = { budget ->
                                    try {
                                        viewModel.toggleBudgetActive(budget.id, !budget.isActive)
                                        android.widget.Toast.makeText(
                                            context,
                                            "Budget paused",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(
                                            context,
                                            "Failed to update budget status",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            )
                        }
                    }
                }

                // Inactive Budgets Section (Collapsible)
                if (inactiveBudgets.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Paused Budgets",
                            count = inactiveBudgets.size,
                            icon = "⏸️",
                            isExpanded = showInactiveSection,
                            onToggle = { showInactiveSection = !showInactiveSection }
                        )
                    }

                    if (showInactiveSection) {
                        items(inactiveBudgets, key = { it.budget.id }) { budgetWithSpending ->
                            AnimatedVisibility(
                                visible = showInactiveSection,
                                enter = fadeIn() + slideInVertically(),
                                exit = fadeOut() + slideOutVertically()
                            ) {
                                BudgetCard(
                                    budgetWithSpending = budgetWithSpending,
                                    currencySymbol = currencySymbol,
                                    isActive = false,
                                    onEdit = {
                                        budgetToEdit = it.budget
                                        showEditSheet = true
                                    },
                                    onDelete = {
                                        budgetToDelete = it.budget
                                    },
                                    onToggleActive = { budget ->
                                        try {
                                            viewModel.toggleBudgetActive(budget.id, !budget.isActive)
                                            android.widget.Toast.makeText(
                                                context,
                                                "Budget activated",
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                        } catch (e: Exception) {
                                            android.widget.Toast.makeText(
                                                context,
                                                "Failed to update budget status",
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }

    // Add Budget Bottom Sheet
    if (showAddSheet) {
        AddBudgetSheet(
            viewModel = viewModel,
            onDismiss = { showAddSheet = false },
            currencySymbol = currencySymbol
        )
    }

    // Edit Budget Bottom Sheet
    if (showEditSheet && budgetToEdit != null) {
        AddBudgetSheet(
            viewModel = viewModel,
            onDismiss = {
                showEditSheet = false
                budgetToEdit = null
            },
            currencySymbol = currencySymbol,
            budgetToEdit = budgetToEdit
        )
    }

    // Delete Confirmation Dialog
    budgetToDelete?.let { budget ->
        AlertDialog(
            onDismissRequest = { budgetToDelete = null },
            title = { Text("Delete Budget") },
            text = { Text("Are you sure you want to delete \"${budget.name}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        try {
                            viewModel.deleteBudget(budget)
                            android.widget.Toast.makeText(
                                context,
                                "Budget deleted successfully",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(
                                context,
                                "Failed to delete budget",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                        budgetToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { budgetToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    count: Int,
    icon: String,
    isExpanded: Boolean = true,
    onToggle: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        ),
        onClick = { onToggle?.invoke() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = icon,
                    fontSize = 20.sp
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = count.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (onToggle != null) {
                Icon(
                    if (isExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun BudgetSummaryCard(
    budgets: List<BudgetWithSpending>,
    currencySymbol: String,
    inactiveCount: Int = 0
) {
    val totalBudget = budgets.sumOf { it.budget.limitAmount }
    val totalSpent = budgets.sumOf { it.spent }
    val overBudgetCount = budgets.count { it.isOverBudget }
    val nearLimitCount = budgets.count { it.isNearLimit && !it.isOverBudget }
    val averageUtilization = if (budgets.isNotEmpty() && totalBudget > 0)
        (totalSpent / totalBudget * 100).coerceIn(0.0, 100.0) else 0.0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Budget Overview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                // Show paused count if any
                if (inactiveCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("⏸️", fontSize = 12.sp)
                            Text(
                                text = "$inactiveCount paused",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "${currencySymbol}${formatAmount(totalSpent)}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "spent of ${currencySymbol}${formatAmount(totalBudget)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${String.format("%.1f", averageUtilization)}%",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (averageUtilization > 80)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "average used",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar for overall budget
            val overallProgress = if (totalBudget > 0) (totalSpent / totalBudget).toFloat().coerceIn(0f, 1f) else 0f
            val overallColor = when {
                overallProgress >= 1f -> MaterialTheme.colorScheme.error
                overallProgress >= 0.8f -> Color(0xFFFF9800)
                else -> MaterialTheme.colorScheme.primary
            }

            LinearProgressIndicator(
                progress = { overallProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50)),
                color = overallColor,
                trackColor = MaterialTheme.colorScheme.surface,
                strokeCap = StrokeCap.Round
            )

            // Status badges
            if (overBudgetCount > 0 || nearLimitCount > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (overBudgetCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "⚠️ $overBudgetCount over budget",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    if (nearLimitCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFF9800).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "🎯 $nearLimitCount near limit",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFF9800),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BudgetCard(
    budgetWithSpending: BudgetWithSpending,
    currencySymbol: String,
    isActive: Boolean,
    onEdit: (BudgetWithSpending) -> Unit,
    onDelete: (BudgetWithSpending) -> Unit,
    onToggleActive: (com.example.paisatracker.data.Budget) -> Unit
) {
    val budget = budgetWithSpending.budget
    val progressAnim by animateFloatAsState(
        targetValue = budgetWithSpending.percentUsed,
        animationSpec = tween(durationMillis = 800),
        label = "budget_progress"
    )

    val progressColor = when {
        budgetWithSpending.isOverBudget -> MaterialTheme.colorScheme.error
        budgetWithSpending.isNearLimit -> Color(0xFFFF9800)
        else -> MaterialTheme.colorScheme.primary
    }

    val statusLabel = when {
        budgetWithSpending.isOverBudget -> "Over budget"
        budgetWithSpending.isNearLimit -> "Near limit"
        else -> "${(budgetWithSpending.percentUsed * 100).toInt()}% used"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (!isActive) it.alpha(0.85f) else it },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isActive) 2.dp else 1.dp,
            pressedElevation = if (isActive) 4.dp else 2.dp
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Emoji circle with status indicator
                    Box {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isActive)
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    else
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = budget.emoji, fontSize = 24.sp)
                        }
                        if (!isActive) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .align(Alignment.TopEnd)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("⏸️", fontSize = 8.sp)
                                }
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = budget.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (isActive)
                                    MaterialTheme.colorScheme.onSurface
                                else
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            if (!isActive) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                ) {
                                    Text(
                                        text = "Paused",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                        Text(
                            text = buildString {
                                append(budget.period.displayName)
                                budgetWithSpending.categoryName?.let { append(" • $it") }
                                budgetWithSpending.projectName?.let { append(" • $it") }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    // Status chip
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = progressColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = statusLabel,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = progressColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Action buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        // Edit button
                        IconButton(
                            onClick = { onEdit(budgetWithSpending) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = if (isActive)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Toggle Active button (Pause/Play)
                        IconButton(
                            onClick = { onToggleActive(budget) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                if (isActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isActive) "Pause budget" else "Activate budget",
                                tint = if (isActive)
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                else
                                    MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Delete button
                        IconButton(
                            onClick = { onDelete(budgetWithSpending) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { progressAnim },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isActive) 10.dp else 6.dp)
                    .clip(RoundedCornerShape(50)),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surface,
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Amount row with more details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Spent",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "$currencySymbol${formatAmount(budgetWithSpending.spent)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = progressColor
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Remaining",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Text(
                        text = if (budgetWithSpending.isOverBudget)
                            "-$currencySymbol${formatAmount(-budgetWithSpending.remaining)}"
                        else
                            "$currencySymbol${formatAmount(budgetWithSpending.remaining)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (budgetWithSpending.isOverBudget)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.primary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Limit",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "$currencySymbol${formatAmount(budget.limitAmount)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!isActive) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "⏸️ This budget is paused. Click ${if (isActive) "pause" else "play"} to resume tracking.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Daily Average",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "$currencySymbol${formatAmount(budgetWithSpending.spent / 30)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Period",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Text(
                            text = budget.period.displayName,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Created",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Text(
                            text = SimpleDateFormat("MMM dd", Locale.getDefault())
                                .format(Date(budget.createdAt)),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BudgetEmptyState(
    modifier: Modifier = Modifier,
    onCreateClick: () -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Outlined.AccountBalanceWallet,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No Budgets Yet",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tap + to create your first budget\nand start tracking your spending",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        FilledTonalButton(
            onClick = onCreateClick,
            modifier = Modifier.fillMaxWidth(0.6f)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create Budget")
        }
    }
}

fun formatAmount(amount: Double): String {
    return if (amount >= 1000) {
        NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
            maximumFractionDigits = if (amount % 1 == 0.0) 0 else 2
        }.format(amount)
    } else {
        String.format("%.2f", amount)
    }
}