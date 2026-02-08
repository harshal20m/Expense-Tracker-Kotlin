package com.example.paisatracker.ui.recent

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.paisatracker.PaisaTrackerViewModel
import com.example.paisatracker.R
import com.example.paisatracker.data.RecentExpense
import com.example.paisatracker.ui.expense.paymentIconRes
import com.example.paisatracker.util.formatCurrency
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RecentExpensesSection(viewModel: PaisaTrackerViewModel) {
    val allExpenses by viewModel.recentExpenses.collectAsState(initial = emptyList())
    var isExpanded by remember { mutableStateOf(false) }

    if (allExpenses.isEmpty()) return

    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            onClick = { isExpanded = !isExpanded }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f))))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), modifier = Modifier.size(32.dp)) {
                        Box(contentAlignment = Alignment.Center) { Text("📝", fontSize = 16.sp) }
                    }
                    Text(text = "Recent Expenses", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.rotate(if (isExpanded) 180f else 0f))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 2000.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    userScrollEnabled = false
                ) {
                    items(allExpenses) { expense ->
                        CompactRecentExpenseGridCard(expense = expense)
                    }
                }

                // Show button if the list is a multiple of 10 (potential for more pages)
                if (allExpenses.size >= 10 && allExpenses.size % 10 == 0) {
                    OutlinedButton(
                        onClick = { viewModel.loadMoreRecentExpenses() },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Load More")
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactRecentExpenseGridCard(expense: RecentExpense) {
    val iconRes = paymentIconRes(expense.paymentIcon)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Icon(
                    painter = painterResource(id = iconRes ?: R.drawable.ic_expense_icon),
                    contentDescription = null,
                    tint = if (iconRes != null) Color.Unspecified else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(formatDateShort(expense.date), style = MaterialTheme.typography.labelSmall, fontSize = 9.sp)
            }

            Text(formatCurrency(expense.amount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
            Text(expense.description, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TagItem(expense.projectEmoji, expense.projectName, MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f), Modifier.weight(1f))
                TagItem(expense.categoryEmoji, expense.categoryName, MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun TagItem(emoji: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(6.dp), color = color, modifier = modifier) {
        Row(modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 10.sp)
            Spacer(Modifier.width(2.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, fontSize = 8.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

private fun formatDateShort(timestamp: Long): String = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(timestamp))