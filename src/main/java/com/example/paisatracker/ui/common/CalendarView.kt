package com.example.paisatracker.ui.common

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
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

@Composable
fun CalendarTransactionView(
    expenses: List<RecentExpense>,
    onTransactionClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }
    var currentMonth by remember { mutableStateOf(Calendar.getInstance()) }
    var showYearPicker by remember { mutableStateOf(false) }

    val expensesByDate = remember(expenses) {
        expenses.groupBy {
            val cal = Calendar.getInstance()
            cal.timeInMillis = it.date
            "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH)}-${cal.get(Calendar.DAY_OF_MONTH)}"
        }
    }

    // Calculate month total
    val monthTotal = remember(expenses, currentMonth) {
        expenses.filter {
            val cal = Calendar.getInstance()
            cal.timeInMillis = it.date
            cal.get(Calendar.YEAR) == currentMonth.get(Calendar.YEAR) &&
                    cal.get(Calendar.MONTH) == currentMonth.get(Calendar.MONTH)
        }.sumOf { it.amount }
    }

    val selectedDateKey = "${selectedDate.get(Calendar.YEAR)}-${selectedDate.get(Calendar.MONTH)}-${selectedDate.get(Calendar.DAY_OF_MONTH)}"
    val selectedExpenses = expensesByDate[selectedDateKey] ?: emptyList()
    val dailyTotal = selectedExpenses.sumOf { it.amount }
    val hasExpenses = selectedExpenses.isNotEmpty()

    val listState = rememberLazyListState()
    var isCollapsed by remember { mutableStateOf(false) }

    LaunchedEffect(currentMonth, selectedDate) {
        if (!hasExpenses) isCollapsed = false
        listState.animateScrollToItem(0)
    }

    val surfaceElevation by animateDpAsState(
        targetValue = if (isCollapsed) 6.dp else 2.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "elevation"
    )

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .pointerInput(hasExpenses, isCollapsed) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        var dragAccumulator = 0f
                        do {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull()
                            if (change != null) {
                                dragAccumulator += change.position.y - change.previousPosition.y

                                if (dragAccumulator < -40f && !isCollapsed && hasExpenses) {
                                    isCollapsed = true
                                    dragAccumulator = 0f
                                } else if (dragAccumulator > 40f && isCollapsed &&
                                    listState.firstVisibleItemIndex == 0 &&
                                    listState.firstVisibleItemScrollOffset == 0) {
                                    isCollapsed = false
                                    dragAccumulator = 0f
                                }
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
        ) {
            // Compact Month Navigation with Total
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = surfaceElevation
            ) {
                CompactMonthHeader(
                    currentMonth = currentMonth,
                    monthTotal = monthTotal,
                    onMonthChange = { currentMonth = it },
                    onYearPickerClick = { showYearPicker = true },
                    isCollapsed = isCollapsed
                )
            }

            Spacer(modifier = Modifier.height(if (isCollapsed) 2.dp else 4.dp))

            // Calendar Grid
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 1.dp
            ) {
                CalendarGrid(
                    currentMonth = currentMonth,
                    selectedDate = selectedDate,
                    expensesByDate = expensesByDate,
                    onDateSelected = { selectedDate = it },
                    isCollapsed = isCollapsed
                )
            }

            Spacer(modifier = Modifier.height(if (isCollapsed) 6.dp else 12.dp))

            // Transactions Tree List
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
                        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 100.dp, top = 2.dp),
                    ) {
                        item {
                            EnhancedDailySummaryHeader(
                                total = dailyTotal,
                                count = selectedExpenses.size,
                                date = selectedDate
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        val groupedByProject = selectedExpenses.groupBy { expense ->
                            expense.projectName.ifBlank { "Personal" }
                        }

                        groupedByProject.entries.forEachIndexed { _, (projectName, projectExpenses) ->
                            item {
                                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                                    // Project Header
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Folder,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = projectName,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = formatCurrency(projectExpenses.sumOf { it.amount }),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    val groupedByCategory = projectExpenses.groupBy { it.categoryName }.entries.toList()
                                    val treeLineColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)

                                    groupedByCategory.forEachIndexed { cIndex, (catName, catExpenses) ->
                                        val isLastCat = cIndex == groupedByCategory.lastIndex

                                        BranchNode(isLast = isLastCat, branchY = 18.dp, lineColor = treeLineColor) {
                                            Column {
                                                Box(
                                                    modifier = Modifier.height(36.dp),
                                                    contentAlignment = Alignment.CenterStart
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Text(
                                                            text = catName,
                                                            style = MaterialTheme.typography.labelLarge,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                        Text(
                                                            text = "(${catExpenses.size})",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                                        )
                                                    }
                                                }

                                                catExpenses.forEachIndexed { tIndex, expense ->
                                                    val isLastTxn = tIndex == catExpenses.lastIndex
                                                    BranchNode(isLast = isLastTxn, branchY = 22.dp, lineColor = treeLineColor) {
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

        // Year Picker Dialog
        if (showYearPicker) {
            YearPickerDialog(
                currentYear = currentMonth.get(Calendar.YEAR),
                onYearSelected = { year ->
                    val newMonth = currentMonth.clone() as Calendar
                    newMonth.set(Calendar.YEAR, year)
                    currentMonth = newMonth
                    showYearPicker = false
                },
                onDismiss = { showYearPicker = false }
            )
        }
    }
}

@Composable
private fun YearPickerDialog(
    currentYear: Int,
    onYearSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val startYear = 2020
    val endYear = Calendar.getInstance().get(Calendar.YEAR)
    val years = (startYear..endYear).toList().reversed()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Year", style = MaterialTheme.typography.titleLarge) },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(years.size) { index ->
                    val year = years[index]
                    val isSelected = year == currentYear

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onYearSelected(year) },
                        color = if (isSelected)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            Color.Transparent
                    ) {
                        Text(
                            text = year.toString(),
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
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
                .width(20.dp)
                .fillMaxHeight()
                .drawBehind {
                    val stroke = 1.5.dp.toPx()
                    val branchYPx = branchY.toPx()
                    val startX = 10.dp.toPx()

                    val vLineEnd = if (isLast) branchYPx else size.height
                    drawLine(lineColor, Offset(startX, 0f), Offset(startX, vLineEnd), strokeWidth = stroke)
                    drawLine(lineColor, Offset(startX, branchYPx), Offset(size.width, branchYPx), strokeWidth = stroke)
                }
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else 2.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun CompactMonthHeader(
    currentMonth: Calendar,
    monthTotal: Double,
    onMonthChange: (Calendar) -> Unit,
    onYearPickerClick: () -> Unit,
    isCollapsed: Boolean
) {
    val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
    val yearFormat = SimpleDateFormat("yyyy", Locale.getDefault())
    val verticalPadding by animateDpAsState(
        if (isCollapsed) 6.dp else 12.dp,
        label = "padding"
    )

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = verticalPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Month Navigation
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledIconButton(
                    onClick = {
                        val newMonth = currentMonth.clone() as Calendar
                        newMonth.add(Calendar.MONTH, -1)
                        onMonthChange(newMonth)
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    modifier = Modifier.size(if (isCollapsed) 32.dp else 40.dp)
                ) {
                    Icon(
                        Icons.Default.ChevronLeft,
                        "Previous",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(if (isCollapsed) 18.dp else 20.dp)
                    )
                }

                // Month and Year (clickable for year picker)
                Surface(
                    onClick = onYearPickerClick,
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = monthFormat.format(currentMonth.time),
                            style = if (isCollapsed)
                                MaterialTheme.typography.titleMedium
                            else
                                MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = yearFormat.format(currentMonth.time),
                            style = if (isCollapsed)
                                MaterialTheme.typography.bodyMedium
                            else
                                MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = "Select year",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                FilledIconButton(
                    onClick = {
                        val newMonth = currentMonth.clone() as Calendar
                        newMonth.add(Calendar.MONTH, 1)
                        onMonthChange(newMonth)
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    modifier = Modifier.size(if (isCollapsed) 32.dp else 40.dp)
                ) {
                    Icon(
                        Icons.Default.ChevronRight,
                        "Next",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(if (isCollapsed) 18.dp else 20.dp)
                    )
                }
            }

            // Month Total
            AnimatedVisibility(
                visible = !isCollapsed && monthTotal > 0,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "Month Total",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            text = formatCurrency(monthTotal),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarGrid(
    currentMonth: Calendar,
    selectedDate: Calendar,
    expensesByDate: Map<String, List<RecentExpense>>,
    onDateSelected: (Calendar) -> Unit,
    isCollapsed: Boolean
) {
    val daysOfWeek = listOf("S", "M", "T", "W", "T", "F", "S")
    val calendar = currentMonth.clone() as Calendar
    calendar.set(Calendar.DAY_OF_MONTH, 1)

    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    val today = Calendar.getInstance()
    val oneMonthFromNow = Calendar.getInstance().apply { add(Calendar.MONTH, 1) }

    val isSameMonth = currentMonth.get(Calendar.YEAR) == selectedDate.get(Calendar.YEAR) &&
            currentMonth.get(Calendar.MONTH) == selectedDate.get(Calendar.MONTH)

    val selectedDayOfMonth = selectedDate.get(Calendar.DAY_OF_MONTH)
    val selectedRowIndex = if (isSameMonth) {
        (selectedDayOfMonth - 1 + firstDayOfWeek) / 7
    } else 0

    val padding by animateDpAsState(
        if (isCollapsed) 6.dp else 12.dp,
        label = "gridPadding"
    )

    Column(modifier = Modifier.padding(padding)) {
        // Days of week header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            daysOfWeek.forEach { day ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = if (isCollapsed) 3.dp else 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (!isCollapsed) Spacer(modifier = Modifier.height(4.dp))

        val totalCells = ((daysInMonth + firstDayOfWeek + 6) / 7) * 7
        for (i in 0 until totalCells step 7) {
            val rowIndex = i / 7
            val isSelectedRow = isSameMonth && rowIndex == selectedRowIndex

            AnimatedVisibility(
                visible = !isCollapsed || isSelectedRow,
                enter = expandVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) +
                        fadeIn(animationSpec = tween(300)),
                exit = shrinkVertically(animationSpec = tween(300)) +
                        fadeOut(animationSpec = tween(300))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = if (rowIndex == (totalCells / 7) - 1 || isCollapsed) 0.dp else 3.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (j in 0 until 7) {
                        val dayIndex = i + j
                        val dayOfMonth = dayIndex - firstDayOfWeek + 1

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(if (isCollapsed) 1.dp else 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (dayOfMonth in 1..daysInMonth) {
                                val date = currentMonth.clone() as Calendar
                                date.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                                // Check if date is in future (beyond 1 month)
                                val isFutureDate = date.after(oneMonthFromNow)

                                // Don't show future dates beyond 1 month
                                if (!isFutureDate) {
                                    DateCell(
                                        date = date,
                                        selectedDate = selectedDate,
                                        today = today,
                                        expensesByDate = expensesByDate,
                                        onDateSelected = onDateSelected,
                                        isCollapsed = isCollapsed
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

@Composable
private fun DateCell(
    date: Calendar,
    selectedDate: Calendar,
    today: Calendar,
    expensesByDate: Map<String, List<RecentExpense>>,
    onDateSelected: (Calendar) -> Unit,
    isCollapsed: Boolean
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

    // Animation for selection
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .scale(scale)
            .clip(RoundedCornerShape(10.dp))
            .clickable { onDateSelected(date) },
        color = when {
            isSelected -> MaterialTheme.colorScheme.primary
            isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            else -> Color.Transparent
        },
        shape = RoundedCornerShape(10.dp),
        border = if (isToday && !isSelected)
            BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
        else null
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(3.dp)
        ) {
            Text(
                text = date.get(Calendar.DAY_OF_MONTH).toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    isToday -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )

            if (hasDayExpenses) {
                Spacer(modifier = Modifier.height(2.dp))

                // Color-coded expense indicator
                val expenseColor = when {
                    dayTotal > 5000 -> MaterialTheme.colorScheme.error
                    dayTotal > 2000 -> Color(0xFFFF9800) // Orange
                    dayTotal > 500 -> Color(0xFFFFC107) // Amber
                    else -> MaterialTheme.colorScheme.tertiary
                }

                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.height(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected)
                                    MaterialTheme.colorScheme.onPrimary
                                else
                                    expenseColor
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun EnhancedDailySummaryHeader(total: Double, count: Int, date: Calendar) {
    val dateFormat = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    dateFormat.format(date.time),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    formatCurrency(total),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Receipt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.titleSmall,
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp, horizontal = 2.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(10.dp)
            .scale(scale.value),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            modifier = Modifier.size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(expense.categoryEmoji, fontSize = 14.sp)
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = expense.description,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (!expense.paymentMethod.isNullOrBlank()) {
                Text(
                    text = expense.paymentMethod,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }

        Text(
            text = formatCurrency(expense.amount),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error
        )
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .padding(top = 40.dp)
                .offset(y = floatAnim.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("📅", fontSize = 36.sp)
                }
            }
            Text(
                "No transactions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "Start tracking your expenses",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}