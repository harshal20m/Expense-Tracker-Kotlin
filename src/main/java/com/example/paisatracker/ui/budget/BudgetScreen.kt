package com.example.paisatracker.ui.budget

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.paisatracker.PaisaTrackerViewModel
import com.example.paisatracker.data.BudgetWithSpending
import com.example.paisatracker.ui.salary.SalaryTrackerSection
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Orange = Color(0xFFFF9800)

// ... existing imports ...

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    viewModel: PaisaTrackerViewModel,
    onNavigateBack: () -> Unit,
    currencySymbol: String = "₹"
) {
    val context = LocalContext.current
    val allBudgets by viewModel.budgetsWithSpending.collectAsState()
    val activeBudgets = allBudgets.filter { it.budget.isActive }
    val inactiveBudgets = allBudgets.filter { !it.budget.isActive }

    var showAddSheet by remember { mutableStateOf(false) }
    var budgetToEdit by remember { mutableStateOf<com.example.paisatracker.data.Budget?>(null) }
    var budgetToDelete by remember { mutableStateOf<com.example.paisatracker.data.Budget?>(null) }
    var showPaused by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Budgets",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (allBudgets.isNotEmpty()) {
                            Text(
                                "${activeBudgets.size} active",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    FilledIconButton(
                        onClick = { showAddSheet = true },
                        modifier = Modifier.padding(end = 8.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp) // adjust dp for curvature
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Budget",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // ── Salary Tracker Section (Full width, always visible) ──────────
            item(span = { GridItemSpan(maxLineSpan) }) {
                SalaryTrackerSection()
            }

            // ── Add Normal Budget Heading ──────────────────────────────────
            item(span = { GridItemSpan(maxLineSpan) }) {
                NormalBudgetHeading()
            }

            // ── Overview card (Full width, only if active budgets exist) ─────
            if (activeBudgets.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    OverviewCard(budgets = activeBudgets, currencySymbol = currencySymbol)
                }
            }

            // ── Active budgets section header (Full width) ───────────────────
            if (activeBudgets.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SectionLabel(
                        text = "Active Budgets",
                        count = activeBudgets.size,
                        dotColor = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // ── Active budget cards (Grid items - masonry style) ─────────────
            items(activeBudgets, key = { it.budget.id }) { bws ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically { it / 2 }
                ) {
                    BudgetCard(
                        bws = bws,
                        currencySymbol = currencySymbol,
                        onEdit = { budgetToEdit = bws.budget },
                        onDelete = { budgetToDelete = bws.budget },
                        onToggle = {
                            viewModel.toggleBudgetActive(bws.budget.id, !bws.budget.isActive)
                            android.widget.Toast.makeText(context, "Budget paused", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            // ── Paused budgets header (Full width, collapsible) ──────────────
            if (inactiveBudgets.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    PausedSectionHeader(
                        count = inactiveBudgets.size,
                        expanded = showPaused,
                        onClick = { showPaused = !showPaused }
                    )
                }
            }

            // ── Paused budget cards (Grid items, shown when expanded) ────────
            if (inactiveBudgets.isNotEmpty() && showPaused) {
                items(inactiveBudgets, key = { it.budget.id }) { bws ->
                    BudgetCard(
                        bws = bws,
                        currencySymbol = currencySymbol,
                        isPaused = true,
                        onEdit = { budgetToEdit = bws.budget },
                        onDelete = { budgetToDelete = bws.budget },
                        onToggle = {
                            viewModel.toggleBudgetActive(bws.budget.id, !bws.budget.isActive)
                            android.widget.Toast.makeText(context, "Budget activated", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            // ── Empty state for budgets only (Full width) ────────────────────
            if (allBudgets.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    BudgetEmptyState(
                        modifier = Modifier.fillMaxWidth(),
                        onCreateClick = { showAddSheet = true }
                    )
                }
            }

            // Bottom spacer
            item(span = { GridItemSpan(maxLineSpan) }) {
                Spacer(Modifier.height(100.dp))
            }
        }
    }

    if (showAddSheet) {
        AddBudgetSheet(viewModel = viewModel, onDismiss = { showAddSheet = false }, currencySymbol = currencySymbol)
    }
    budgetToEdit?.let { budget ->
        AddBudgetSheet(
            viewModel = viewModel,
            onDismiss = { budgetToEdit = null },
            currencySymbol = currencySymbol,
            budgetToEdit = budget
        )
    }
    budgetToDelete?.let { budget ->
        AlertDialog(
            onDismissRequest = { budgetToDelete = null },
            title = { Text("Delete \"${budget.name}\"?") },
            text = { Text("This budget and its history will be permanently removed.") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.deleteBudget(budget); budgetToDelete = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { budgetToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

// ─── Normal Budget Heading ────────────────────────────────────────────────────
@Composable
private fun NormalBudgetHeading() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Horizontal divider
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Section header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.AccountBalance,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Project Wise Budget",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}



// ─── Overview Card ────────────────────────────────────────────────────────────
@Composable
private fun OverviewCard(budgets: List<BudgetWithSpending>, currencySymbol: String) {
    val totalLimit = budgets.sumOf { it.budget.limitAmount }
    val totalSpent = budgets.sumOf { it.spent }
    val overCount = budgets.count { it.isOverBudget }
    val nearCount = budgets.count { it.isNearLimit && !it.isOverBudget }
    val progress = if (totalLimit > 0) (totalSpent / totalLimit).toFloat().coerceIn(0f, 1f) else 0f
    val progressColor = when {
        progress >= 1f -> MaterialTheme.colorScheme.error
        progress >= 0.8f -> Orange
        else -> MaterialTheme.colorScheme.primary
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "$currencySymbol${formatAmount(totalSpent)}",
                        style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        lineHeight = 36.sp
                    )
                    Text(
                        text = "of $currencySymbol${formatAmount(totalLimit)} budgeted",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.65f)
                    )
                }

                // Big % circle
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = progressColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Segmented progress bar
            val animProgress by animateFloatAsState(progress, tween(900), label = "overview_progress")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animProgress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .background(progressColor)
                )
            }

            // Status badges
            if (overCount > 0 || nearCount > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (overCount > 0) StatusBadge(
                        label = "$overCount over limit",
                        color = MaterialTheme.colorScheme.error
                    )
                    if (nearCount > 0) StatusBadge(
                        label = "$nearCount near limit",
                        color = Orange
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

// ─── Section label ────────────────────────────────────────────────────────────
@Composable
private fun SectionLabel(text: String, count: Int, dotColor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
        )
    }
}

// ─── Paused section header ────────────────────────────────────────────────────
@Composable
private fun PausedSectionHeader(count: Int, expanded: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f))
            )
            Text(
                text = "Paused",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
        Icon(
            if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
            modifier = Modifier.size(18.dp)
        )
    }
}

// ─── Budget Card ─────────────────────────────────────────────────────────────
@Composable
private fun BudgetCard(
    bws: BudgetWithSpending,
    currencySymbol: String,
    isPaused: Boolean = false,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggle: () -> Unit
) {
    val budget = bws.budget
    val progressColor = when {
        bws.isOverBudget -> MaterialTheme.colorScheme.error
        bws.isNearLimit  -> Orange
        else             -> MaterialTheme.colorScheme.tertiary // Changed to tertiary
    }
    val progress by animateFloatAsState(
        targetValue = bws.percentUsed,
        animationSpec = tween(800),
        label = "card_progress"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (isPaused) it.alpha(0.65f) else it },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPaused)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            else
                MaterialTheme.colorScheme.surface // Changed from surfaceVariant to surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isPaused) 0.dp else 1.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {

            // ── Row 1: Emoji + Name + Badge + Actions ─────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Emoji
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(budget.emoji, fontSize = 20.sp)
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Name + metadata
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = budget.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isPaused) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                        else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = buildString {
                            append(budget.period.displayName)
                            bws.categoryName?.let { append("  ·  $it") }
                            bws.projectName?.let { append("  ·  $it") }
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Action icons
                Row {
                    SmallIconBtn(onClick = onEdit) {
                        Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.tertiary.copy(alpha = if (isPaused) 0.45f else 0.85f)) // Changed to tertiary
                    }
                    SmallIconBtn(onClick = onToggle) {
                        Icon(
                            if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            null,
                            modifier = Modifier.size(16.dp),
                            tint = if (isPaused) MaterialTheme.colorScheme.tertiary // Changed to tertiary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                        )
                    }
                    SmallIconBtn(onClick = onDelete) {
                        Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.45f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── Row 2: Amounts ────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // Spent
                Column {
                    Text(
                        text = "$currencySymbol${formatAmount(bws.spent)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = progressColor
                    )
                    Text(
                        text = "spent",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }

                // Remaining / over
                Column(horizontalAlignment = Alignment.End) {
                    val remText = if (bws.isOverBudget)
        "−$currencySymbol${formatAmount(-bws.remaining)}"
    else
        "$currencySymbol${formatAmount(bws.remaining)}"
                    Text(
                        text = remText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (bws.isOverBudget) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                    Text(
                        text = if (bws.isOverBudget) "over limit" else "of $currencySymbol${formatAmount(budget.limitAmount)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Row 3: Progress bar ───────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)) // Changed background color
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .background(progressColor)
                )
            }

            // ── Row 4: Footer stats (only when active) ────────────────────
            if (!isPaused) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    FooterStat(
                        label = "Daily avg",
                        value = "$currencySymbol${formatAmount(bws.spent / 30)}",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f) // Added color parameter
                    )
                    FooterStat(
                        label = "Period",
                        value = budget.period.displayName,
                        align = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f) // Added color parameter
                    )
                    FooterStat(
                        label = "Since",
                        value = SimpleDateFormat("MMM d", Locale.ROOT).format(Date(budget.createdAt)),
                        align = TextAlign.End,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f) // Added color parameter
                    )
                }
            }
        }
    }
}

@Composable
private fun FooterStat(label: String, value: String, align: TextAlign = TextAlign.Start, color: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)) {
    Column(horizontalAlignment = when (align) {
        TextAlign.Center -> Alignment.CenterHorizontally
        TextAlign.End -> Alignment.End
        else -> Alignment.Start
    }) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}

@Composable
private fun SmallIconBtn(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}

// ─── Empty state ──────────────────────────────────────────────────────────────
@Composable
fun BudgetEmptyState(modifier: Modifier = Modifier, onCreateClick: () -> Unit) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.AccountBalanceWallet,
                contentDescription = null,
                modifier = Modifier.size(44.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "No Budgets Yet",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Set spending limits to stay on track.\nTap the + button to get started.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onCreateClick,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.height(50.dp)
        ) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create Budget", fontWeight = FontWeight.SemiBold)
        }
    }
}

// ─── Shared format helper ─────────────────────────────────────────────────────
fun formatAmount(amount: Double): String = if (amount >= 1_000) {
    NumberFormat.getNumberInstance(Locale.ROOT).apply {
        maximumFractionDigits = if (amount % 1 == 0.0) 0 else 2
    }.format(amount)
} else String.format(Locale.ROOT, "%.2f", amount)
