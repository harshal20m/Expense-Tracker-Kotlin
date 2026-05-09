package com.example.paisatracker.ui.common

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.paisatracker.data.RecentExpense
import com.example.paisatracker.util.formatCurrency
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

data class MonthData(
    val month: Calendar,
    val total: Double,
    val transactionCount: Int
)

// Helper function to format currency in compact form
fun formatCompactCurrency(amount: Double): String {
    return when {
        abs(amount) >= 100000 -> String.format("%.0fk", amount / 1000)
        abs(amount) >= 10000 -> String.format("%.1fk", amount / 1000)
        abs(amount) >= 1000 -> String.format("%.1fk", amount / 1000)
        else -> String.format("%.0f", amount)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CalendarTransactionView(
    expenses: List<RecentExpense>,
    onTransactionClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null
) {
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }
    var currentMonth by remember { mutableStateOf(Calendar.getInstance()) }

    val expensesByDate = remember(expenses) {
        expenses.groupBy {
            val cal = Calendar.getInstance()
            cal.timeInMillis = it.date
            "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH)}-${cal.get(Calendar.DAY_OF_MONTH)}"
        }
    }

    // Calculate monthly data for the sidebar
    val monthlyData = remember(expenses) {
        val grouped = expenses.groupBy {
            val cal = Calendar.getInstance()
            cal.timeInMillis = it.date
            "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH)}"
        }
        
        grouped.map { (key, monthExpenses) ->
            val parts = key.split("-")
            val cal = Calendar.getInstance()
            cal.set(Calendar.YEAR, parts[0].toInt())
            cal.set(Calendar.MONTH, parts[1].toInt())
            cal.set(Calendar.DAY_OF_MONTH, 1)
            
            MonthData(
                month = cal,
                total = monthExpenses.sumOf { it.amount },
                transactionCount = monthExpenses.size
            )
        }.sortedByDescending { it.month.timeInMillis }
    }

    val selectedDateKey = "${selectedDate.get(Calendar.YEAR)}-${selectedDate.get(Calendar.MONTH)}-${selectedDate.get(Calendar.DAY_OF_MONTH)}"
    val selectedExpenses = expensesByDate[selectedDateKey] ?: emptyList()
    val dailyTotal = selectedExpenses.sumOf { it.amount }
    val hasExpenses = selectedExpenses.isNotEmpty()

    val listState = rememberLazyListState()
    var isCalendarMinimized by remember { mutableStateOf(false) }
    var hideDailySummary by remember { mutableStateOf(false) }

    // Shrink on scroll, stay shrunk
    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        val scrollOffset = listState.firstVisibleItemScrollOffset
        val itemIndex = listState.firstVisibleItemIndex
        
        // Once user scrolls, minimize and keep it minimized
        if (itemIndex > 0 || scrollOffset > 0) {
            isCalendarMinimized = true
            hideDailySummary = itemIndex > 0
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // Standard ScreenHeader
            ScreenHeader(
                title = "Calendar",
                subtitle = "Track your daily expenses",
                icon = Icons.Default.CalendarMonth
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Side-by-side layout: Calendar (3/4) + Month Cards (1/4)
            // Both shrink together to same height
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(if (isCalendarMinimized) 80.dp else 300.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Calendar (3/4 width) - scroll to expand
                CompactCalendarView(
                    currentMonth = currentMonth,
                    selectedDate = selectedDate,
                    expensesByDate = expensesByDate,
                    onDateSelected = { selectedDate = it },
                    onMonthChange = { currentMonth = it },
                    isMinimized = isCalendarMinimized,
                    onExpandRequest = {
                        // Expand when user scrolls on calendar
                        isCalendarMinimized = false
                    },
                    modifier = Modifier.weight(0.72f)
                )

                // Month Cards Sidebar (1/4 width) - matches calendar height
                if (monthlyData.isNotEmpty()) {
                    MonthCardsSidebar(
                        monthlyData = monthlyData,
                        currentMonth = currentMonth,
                        onMonthClick = { month ->
                            currentMonth = month
                            selectedDate = month.clone() as Calendar
                        },
                        isMinimized = isCalendarMinimized,
                        modifier = Modifier.weight(0.28f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Transactions List
            AnimatedContent(
                targetState = hasExpenses,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(400)) +
                            slideInVertically(animationSpec = tween(400)) { it / 4 })
                        .togetherWith(
                            fadeOut(animationSpec = tween(200)) +
                                    slideOutVertically(animationSpec = tween(200)) { -it / 4 }
                        )
                },
                label = "transactions"
            ) { expensesExist ->
                if (expensesExist) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp, top = 4.dp),
                    ) {
                        // Daily Summary Header (item 0 - will be hidden on scroll)
                        item {
                            EnhancedDailySummaryHeader(
                                total = dailyTotal,
                                count = selectedExpenses.size,
                                date = selectedDate
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                        }

                        val groupedByProject = selectedExpenses.groupBy { expense ->
                            expense.projectName.ifBlank { "Personal" }
                        }

                        groupedByProject.entries.forEachIndexed { projectIndex, (projectName, projectExpenses) ->
                            // Sticky Project Header
                            stickyHeader(key = "project_$projectIndex") {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                                    shadowElevation = 4.dp
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Default.Folder,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        Spacer(Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = projectName,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "${projectExpenses.size} transactions",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = formatCurrency(projectExpenses.sumOf { it.amount }),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Project transactions
                            item(key = "project_content_$projectIndex") {
                                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)) {

                                    val groupedByCategory = projectExpenses.groupBy { it.categoryName }.entries.toList()
                                    val treeLineColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)

                                    groupedByCategory.forEachIndexed { cIndex, (catName, catExpenses) ->
                                        val isLastCat = cIndex == groupedByCategory.lastIndex

                                        BranchNode(isLast = isLastCat, branchY = 20.dp, lineColor = treeLineColor) {
                                            Column {
                                                // Enhanced Category Header
                                                Surface(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                                    ) {
                                                        Text(
                                                            text = catName,
                                                            style = MaterialTheme.typography.labelLarge,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                        Surface(
                                                            shape = RoundedCornerShape(6.dp),
                                                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                                                        ) {
                                                            Text(
                                                                text = "${catExpenses.size}",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.secondary,
                                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                            )
                                                        }
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(8.dp))

                                                catExpenses.forEachIndexed { tIndex, expense ->
                                                    val isLastTxn = tIndex == catExpenses.lastIndex
                                                    BranchNode(isLast = isLastTxn, branchY = 24.dp, lineColor = treeLineColor) {
                                                        TransactionItem(
                                                            expense = expense,
                                                            onClick = { onTransactionClick(expense.id) }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    EmptyStateView()
                }
            }
        }
    }
}

@Composable
private fun MonthCardsSidebar(
    monthlyData: List<MonthData>,
    currentMonth: Calendar,
    onMonthClick: (Calendar) -> Unit,
    isMinimized: Boolean,
    modifier: Modifier = Modifier
) {
    val sidebarListState = rememberLazyListState()
    
    // When minimized, show only the selected month
    val displayData = if (isMinimized) {
        monthlyData.filter { data ->
            data.month.get(Calendar.YEAR) == currentMonth.get(Calendar.YEAR) &&
            data.month.get(Calendar.MONTH) == currentMonth.get(Calendar.MONTH)
        }
    } else {
        monthlyData
    }
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        shape = if (isMinimized) RoundedCornerShape(8.dp) else RoundedCornerShape(16.dp),
        color = if (isMinimized) Color.Transparent else MaterialTheme.colorScheme.surfaceContainerLowest,
        tonalElevation = if (isMinimized) 0.dp else 1.dp
    ) {
        if (isMinimized) {
            // When minimized, use Box for true centering
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                displayData.firstOrNull()?.let { data ->
                    CompactMonthCard(
                        monthData = data,
                        isSelected = data.month.get(Calendar.YEAR) == currentMonth.get(Calendar.YEAR) &&
                                data.month.get(Calendar.MONTH) == currentMonth.get(Calendar.MONTH),
                        onClick = { onMonthClick(data.month) },
                        isMinimized = isMinimized
                    )
                }
            }
        } else {
            // When expanded, use LazyColumn for scrolling
            LazyColumn(
                state = sidebarListState,
                contentPadding = PaddingValues(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                userScrollEnabled = true
            ) {
                items(displayData) { data ->
                    CompactMonthCard(
                        monthData = data,
                        isSelected = data.month.get(Calendar.YEAR) == currentMonth.get(Calendar.YEAR) &&
                                data.month.get(Calendar.MONTH) == currentMonth.get(Calendar.MONTH),
                        onClick = { onMonthClick(data.month) },
                        isMinimized = isMinimized
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactMonthCard(
    monthData: MonthData,
    isSelected: Boolean,
    onClick: () -> Unit,
    isMinimized: Boolean = false
) {
    val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
    val yearFormat = SimpleDateFormat("yy", Locale.getDefault())

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() },
        color = if (isSelected)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = monthFormat.format(monthData.month.time),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "'${yearFormat.format(monthData.month.time)}",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = formatCompactCurrency(monthData.total),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        Icons.Default.Receipt,
                        contentDescription = null,
                        modifier = Modifier.size(8.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${monthData.transactionCount}",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactCalendarView(
    currentMonth: Calendar,
    selectedDate: Calendar,
    expensesByDate: Map<String, List<RecentExpense>>,
    onDateSelected: (Calendar) -> Unit,
    onMonthChange: (Calendar) -> Unit,
    isMinimized: Boolean,
    onExpandRequest: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(isMinimized) {
                if (isMinimized) {
                    detectDragGestures { _, dragAmount ->
                        // Expand on any drag gesture when minimized
                        if (dragAmount.y != 0f || dragAmount.x != 0f) {
                            onExpandRequest()
                        }
                    }
                }
            },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = if (isMinimized) 6.dp else 12.dp,
                vertical = if (isMinimized) 6.dp else 12.dp
            )
        ) {
            // Month navigation header (hides when minimized)
            AnimatedVisibility(
                visible = !isMinimized,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val newMonth = currentMonth.clone() as Calendar
                            newMonth.add(Calendar.MONTH, -1)
                            onMonthChange(newMonth)
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.ChevronLeft,
                            contentDescription = "Previous Month",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = monthFormat.format(currentMonth.time),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    IconButton(
                        onClick = {
                            val newMonth = currentMonth.clone() as Calendar
                            newMonth.add(Calendar.MONTH, 1)
                            onMonthChange(newMonth)
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = "Next Month",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Calendar Grid (full or week only)
            if (isMinimized) {
                CompactWeekRow(
                    currentMonth = currentMonth,
                    selectedDate = selectedDate,
                    expensesByDate = expensesByDate,
                    onDateSelected = onDateSelected
                )
            } else {
                CompactCalendarGrid(
                    currentMonth = currentMonth,
                    selectedDate = selectedDate,
                    expensesByDate = expensesByDate,
                    onDateSelected = onDateSelected
                )
            }
        }
    }
}

@Composable
private fun CompactCalendarGrid(
    currentMonth: Calendar,
    selectedDate: Calendar,
    expensesByDate: Map<String, List<RecentExpense>>,
    onDateSelected: (Calendar) -> Unit
) {
    val daysOfWeek = listOf("S", "M", "T", "W", "T", "F", "S")
    val calendar = currentMonth.clone() as Calendar
    calendar.set(Calendar.DAY_OF_MONTH, 1)

    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    val today = Calendar.getInstance()
    val oneMonthFromNow = Calendar.getInstance().apply { add(Calendar.MONTH, 1) }

    Column {
        // Days of week header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            daysOfWeek.forEach { day ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Calendar dates
        val totalCells = ((daysInMonth + firstDayOfWeek + 6) / 7) * 7
        for (i in 0 until totalCells step 7) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 2.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (j in 0 until 7) {
                    val dayIndex = i + j
                    val dayOfMonth = dayIndex - firstDayOfWeek + 1

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (dayOfMonth in 1..daysInMonth) {
                            val date = currentMonth.clone() as Calendar
                            date.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                            val isFutureDate = date.after(oneMonthFromNow)

                            if (!isFutureDate) {
                                CompactDateCell(
                                    date = date,
                                    selectedDate = selectedDate,
                                    today = today,
                                    expensesByDate = expensesByDate,
                                    onDateSelected = onDateSelected,
                                    isVeryCompact = false
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactWeekRow(
    currentMonth: Calendar,
    selectedDate: Calendar,
    expensesByDate: Map<String, List<RecentExpense>>,
    onDateSelected: (Calendar) -> Unit
) {
    val daysOfWeek = listOf("S", "M", "T", "W", "T", "F", "S")
    
    // Get the week containing the selected date
    val weekStart = selectedDate.clone() as Calendar
    weekStart.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
    
    val today = Calendar.getInstance()
    val oneMonthFromNow = Calendar.getInstance().apply { add(Calendar.MONTH, 1) }

    Column {
        // Compact days header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            daysOfWeek.forEach { day ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Week dates (very small)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            for (i in 0 until 7) {
                val date = weekStart.clone() as Calendar
                date.add(Calendar.DAY_OF_MONTH, i)
                
                val isFutureDate = date.after(oneMonthFromNow)
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(28.dp)
                        .padding(1.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (!isFutureDate) {
                        CompactDateCell(
                            date = date,
                            selectedDate = selectedDate,
                            today = today,
                            expensesByDate = expensesByDate,
                            onDateSelected = onDateSelected,
                            isVeryCompact = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactDateCell(
    date: Calendar,
    selectedDate: Calendar,
    today: Calendar,
    expensesByDate: Map<String, List<RecentExpense>>,
    onDateSelected: (Calendar) -> Unit,
    isVeryCompact: Boolean
) {
    val isSelected = date.get(Calendar.YEAR) == selectedDate.get(Calendar.YEAR) &&
            date.get(Calendar.MONTH) == selectedDate.get(Calendar.MONTH) &&
            date.get(Calendar.DAY_OF_MONTH) == selectedDate.get(Calendar.DAY_OF_MONTH)

    val dateKey = "${date.get(Calendar.YEAR)}-${date.get(Calendar.MONTH)}-${date.get(Calendar.DAY_OF_MONTH)}"
    val dayExpenses = expensesByDate[dateKey] ?: emptyList()
    val hasDayExpenses = dayExpenses.isNotEmpty()
    val dayTotal = dayExpenses.sumOf { it.amount }

    val isToday = date.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
            date.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
            date.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH)

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .scale(scale)
            .clip(RoundedCornerShape(if (isVeryCompact) 6.dp else 10.dp))
            .clickable { onDateSelected(date) },
        color = when {
            isSelected -> MaterialTheme.colorScheme.primary
            isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            hasDayExpenses -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
            else -> Color.Transparent
        },
        shape = RoundedCornerShape(if (isVeryCompact) 6.dp else 10.dp),
        border = if (isToday && !isSelected)
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
        else null,
        tonalElevation = if (isSelected) 3.dp else 0.dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(if (isVeryCompact) 2.dp else 3.dp)
        ) {
            Text(
                text = date.get(Calendar.DAY_OF_MONTH).toString(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                fontSize = if (isVeryCompact) 9.sp else 12.sp,
                color = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    isToday -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )

            if (hasDayExpenses && !isVeryCompact) {
                Spacer(modifier = Modifier.height(1.dp))

                val expenseColor = when {
                    dayTotal > 5000 -> MaterialTheme.colorScheme.error
                    dayTotal > 2000 -> Color(0xFFFF9800)
                    dayTotal > 500 -> Color(0xFFFFC107)
                    else -> MaterialTheme.colorScheme.tertiary
                }

                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                expenseColor
                        )
                )
            } else if (hasDayExpenses && isVeryCompact) {
                Box(
                    modifier = Modifier
                        .size(2.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.error
                        )
                )
            }
        }
    }
}

@Composable
private fun BranchNode(
    isLast: Boolean,
    branchY: Dp,
    lineColor: Color,
    content: @Composable () -> Unit
) {
    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
        Box(
            modifier = Modifier
                .width(24.dp)
                .fillMaxHeight()
                .drawBehind {
                    val stroke = 2.dp.toPx()
                    val branchYPx = branchY.toPx()
                    val startX = 12.dp.toPx()

                    val vLineEnd = if (isLast) branchYPx else size.height
                    drawLine(lineColor, Offset(startX, 0f), Offset(startX, vLineEnd), strokeWidth = stroke)
                    drawLine(lineColor, Offset(startX, branchYPx), Offset(size.width, branchYPx), strokeWidth = stroke)
                }
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else 4.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun EnhancedDailySummaryHeader(total: Double, count: Int, date: Calendar) {
    val dateFormat = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    dateFormat.format(date.time),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    formatCurrency(total),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Receipt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionItem(
    expense: RecentExpense,
    onClick: () -> Unit
) {
    val scale = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .scale(scale.value),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        tonalElevation = 1.dp,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(expense.categoryEmoji, fontSize = 18.sp)
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = expense.description,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (!expense.paymentMethod.isNullOrBlank()) {
                    Text(
                        text = expense.paymentMethod,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            Text(
                text = formatCurrency(expense.amount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun EmptyStateView() {
    val infiniteTransition = rememberInfiniteTransition(label = "float")
    val floatAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .padding(top = 60.dp)
                .offset(y = floatAnim.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("📅", fontSize = 40.sp)
                }
            }
            Text(
                "No transactions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "Start tracking your expenses",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// Made with Bob
