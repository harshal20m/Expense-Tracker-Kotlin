package com.example.paisatracker.ui.details

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.paisatracker.PaisaTrackerViewModel
import com.example.paisatracker.data.CategoryExpense
import com.example.paisatracker.ui.common.BarChart
import com.example.paisatracker.ui.common.PieChartWithLegend
import com.example.paisatracker.util.formatCurrency
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieEntry

enum class ChartType { PIE, BAR }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectInsightsScreen(
    viewModel: PaisaTrackerViewModel,
    projectId: Long,
    navController: NavController
) {
    val categoryExpenses: List<CategoryExpense> by viewModel.getCategoryExpenses(projectId)
        .collectAsState(initial = emptyList())

    var currentChartType by remember { mutableStateOf(ChartType.PIE) }
    val totalSpent = categoryExpenses.sumOf { it.totalAmount }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Project Insights",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp,
                end = 16.dp,
                top = 0.dp,
                bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (categoryExpenses.isEmpty()) {
                item { EmptyState() }
            } else {
                // Total header
                item { TotalCard(totalSpent) }

                // Chart toggle
                item {
                    ChartToggle(currentChartType) { currentChartType = it }
                }

                // Chart
                item {
                    ChartSection(currentChartType, categoryExpenses)
                }

                // Stats grid
                item { StatsGrid(categoryExpenses) }

                // Category list
                item { CategoryList(categoryExpenses) }
            }
        }
    }
}

@Composable
private fun TotalCard(totalSpent: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Total Spent",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    formatCurrency(totalSpent),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Text("💰", fontSize = 32.sp)
        }
    }
}

@Composable
private fun ChartToggle(current: ChartType, onChange: (ChartType) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ChartType.values().forEach { type ->
            val selected = current == type
            FilterChip(
                selected = selected,
                onClick = { onChange(type) },
                label = {
                    Text(
                        when (type) {
                            ChartType.PIE -> "Pie Chart"
                            ChartType.BAR -> "Bar Chart"
                        }
                    )
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ChartSection(type: ChartType, expenses: List<CategoryExpense>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                if (type == ChartType.PIE) "Distribution" else "By Category",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            when (type) {
                ChartType.PIE -> {
                    val entries = expenses.map { PieEntry(it.totalAmount.toFloat(), it.categoryName) }
                    PieChartWithLegend(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        entries = entries,
                        description = ""
                    )
                }
                ChartType.BAR -> {
                    val entries = expenses.mapIndexed { i, e -> BarEntry(i.toFloat(), e.totalAmount.toFloat()) }
                    val labels = expenses.map { it.categoryName }
                    BarChart(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        entries = entries,
                        labels = labels,
                        description = ""
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsGrid(expenses: List<CategoryExpense>) {
    val total = expenses.sumOf { it.totalAmount }
    val avg = if (expenses.isNotEmpty()) total / expenses.size else 0.0
    val top = expenses.maxByOrNull { it.totalAmount }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Average", formatCurrency(avg), "📊", Modifier.weight(1f))
            StatCard("Categories", "${expenses.size}", "📁", Modifier.weight(1f))
        }
        top?.let {
            StatCard("Top Spending", it.categoryName, "👑", Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, icon: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(icon, fontSize = 24.sp)
        }
    }
}

@Composable
private fun CategoryList(expenses: List<CategoryExpense>) {
    val total = expenses.sumOf { it.totalAmount }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Breakdown",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            expenses.sortedByDescending { it.totalAmount }.forEach { cat ->
                val percent = if (total > 0) ((cat.totalAmount / total) * 100).toInt() else 0
                CategoryRow(cat.categoryName, cat.totalAmount, percent)
            }
        }
    }
}

@Composable
private fun CategoryRow(name: String, amount: Double, percent: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(name, style = MaterialTheme.typography.bodyLarge)
            Text(
                formatCurrency(amount),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LinearProgressIndicator(
                progress = percent / 100f,
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            )
            Spacer(Modifier.width(12.dp))
            Text(
                "$percent%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun EmptyState() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("📊", fontSize = 48.sp)
            Text(
                "No Data Yet",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Add expenses to see insights",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
