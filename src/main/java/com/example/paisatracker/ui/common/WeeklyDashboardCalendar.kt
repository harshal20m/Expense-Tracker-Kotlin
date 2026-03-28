package com.example.paisatracker.ui.common

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.paisatracker.data.RecentExpense
import com.example.paisatracker.util.formatCurrency
import java.text.SimpleDateFormat
import androidx.compose.ui.text.style.TextAlign
import java.util.*

@Composable
fun WeeklyDashboardCalendar(
    expenses: List<RecentExpense>,
    onTransactionClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = remember { Calendar.getInstance() }
    var selectedDate by remember { mutableStateOf(today) }

    val startOfWeek = remember {
        Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.SUNDAY
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    val weekDates = remember(startOfWeek) {
        (0..6).map { i -> (startOfWeek.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, i) } }
    }

    val endOfWeek = remember(startOfWeek) {
        (startOfWeek.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 7) }
    }

    val weeklyExpenses = remember(expenses, startOfWeek, endOfWeek) {
        expenses.filter { it.date >= startOfWeek.timeInMillis && it.date < endOfWeek.timeInMillis }
    }

    val weeklyTotal = remember(weeklyExpenses) { weeklyExpenses.sumOf { it.amount } }

    val expensesByDate = remember(weeklyExpenses) {
        weeklyExpenses.groupBy {
            val cal = Calendar.getInstance().apply { timeInMillis = it.date }
            "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH)}-${cal.get(Calendar.DAY_OF_MONTH)}"
        }
    }

    val selectedDateKey = "${selectedDate.get(Calendar.YEAR)}-${selectedDate.get(Calendar.MONTH)}-${selectedDate.get(Calendar.DAY_OF_MONTH)}"
    val selectedExpenses = expensesByDate[selectedDateKey] ?: emptyList()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // --- HEADER ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("This Week", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "${formatDateToDayMonth(startOfWeek)} - ${formatDateToDayMonth(weekDates.last())}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Spent", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Text(formatCurrency(weeklyTotal), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // --- WEEK DAYS ---
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                val dayLabels = listOf("S", "M", "T", "W", "T", "F", "S")
                weekDates.forEachIndexed { index, date ->
                    val isSelected = isSameDay(date, selectedDate)
                    val isToday = isSameDay(date, today)
                    val dateKey = "${date.get(Calendar.YEAR)}-${date.get(Calendar.MONTH)}-${date.get(Calendar.DAY_OF_MONTH)}"

                    val bgColor by animateColorAsState(when {
                        isSelected -> MaterialTheme.colorScheme.primary
                        isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        else -> Color.Transparent
                    }, label = "bg")

                    val contentColor by animateColorAsState(when {
                        isSelected -> MaterialTheme.colorScheme.onPrimary
                        isToday -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurface
                    }, label = "txt")

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(bgColor)
                            .clickable { selectedDate = date }
                            .padding(vertical = 6.dp)
                            .weight(1f)
                    ) {
                        Text(dayLabels[index], style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = if (isSelected) contentColor.copy(0.8f) else MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(date.get(Calendar.DAY_OF_MONTH).toString(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = contentColor)
                        Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(if (expensesByDate.containsKey(dateKey)) contentColor else Color.Transparent))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)

            // --- TRANSACTIONS GRID (2 in a row) ---
            AnimatedContent(
                targetState = selectedExpenses,
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                label = "grid"
            ) { expensesForDay ->
                if (expensesForDay.isNotEmpty()) {
                    Box(modifier = Modifier.heightIn(max = 180.dp).padding(top = 8.dp)) {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(expensesForDay.chunked(2)) { rowItems ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    rowItems.forEach { expense ->
                                        GridTransactionItem(expense = expense, onClick = { onTransactionClick(expense.id) }, modifier = Modifier.weight(1f))
                                    }
                                    if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                } else {
                    Text("No transactions", modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun GridTransactionItem(
    expense: RecentExpense,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier.size(28.dp).background(MaterialTheme.colorScheme.primaryContainer.copy(0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(expense.categoryEmoji, fontSize = 12.sp)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.description,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatCurrency(expense.amount),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean =
    cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
            cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH)

private fun formatDateToDayMonth(calendar: Calendar): String =
    SimpleDateFormat("MMM d", Locale.getDefault()).format(calendar.time)