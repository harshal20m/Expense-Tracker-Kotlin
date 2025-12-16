package com.example.paisatracker.ui.details

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.paisatracker.PaisaTrackerViewModel
import com.example.paisatracker.data.Category
import com.example.paisatracker.data.CategoryExpense
import com.example.paisatracker.data.CategoryWithTotal
import com.example.paisatracker.ui.common.BarChart
import com.example.paisatracker.ui.common.PieChart
import com.example.paisatracker.ui.common.SortDropdown
import com.example.paisatracker.ui.common.SortOption
import com.example.paisatracker.util.formatCurrency
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieEntry
import kotlinx.coroutines.launch



enum class ViewType {
    GRID, LIST
}

private enum class CategorySheetType {
    ADD, EDIT, DELETE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailsScreen(
    viewModel: PaisaTrackerViewModel,
    projectId: Long,
    navController: NavController
) {
    val categoriesWithTotal by viewModel.getCategoriesWithTotalForProject(projectId)
        .collectAsState(initial = emptyList())
    val categoryExpenses: List<CategoryExpense> by viewModel.getCategoryExpenses(projectId)
        .collectAsState(initial = emptyList())

    var categorySortOption by remember { mutableStateOf(SortOption.AMOUNT_HIGH_LOW) }
    var currentChartType by remember { mutableStateOf(ChartType.BAR) }
    var currentViewType by remember { mutableStateOf(ViewType.GRID) }

    var currentSheetType by remember { mutableStateOf<CategorySheetType?>(null) }
    var categoryToEdit by remember { mutableStateOf<CategoryWithTotal?>(null) }
    var categoryToDelete by remember { mutableStateOf<CategoryWithTotal?>(null) }

    val sheetState: SheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    var showSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.03f),
            MaterialTheme.colorScheme.background
        )
    )

    val totalSpent = categoriesWithTotal.sumOf { it.totalAmount }
    val maxCategoryAmount = categoriesWithTotal.maxOfOrNull { it.totalAmount } ?: 0.0

    val sortedCategories = remember(categoriesWithTotal, categorySortOption) {
        when (categorySortOption) {
            SortOption.AMOUNT_LOW_HIGH ->
                categoriesWithTotal.sortedBy { it.totalAmount }

            SortOption.AMOUNT_HIGH_LOW ->
                categoriesWithTotal.sortedByDescending { it.totalAmount }

            SortOption.NAME_A_Z ->
                categoriesWithTotal.sortedBy { it.category.name.lowercase() }

            SortOption.NAME_Z_A ->
                categoriesWithTotal.sortedByDescending { it.category.name.lowercase() }

            SortOption.DATE_OLD_NEW ->
                categoriesWithTotal.sortedBy { it.latestExpenseTime ?: 0L }

            SortOption.DATE_NEW_OLD ->
                categoriesWithTotal.sortedByDescending { it.latestExpenseTime ?: 0L }
        }
    }



    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    currentSheetType = CategorySheetType.ADD
                    categoryToEdit = null
                    categoryToDelete = null
                    showSheet = true
                    scope.launch { sheetState.show() }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .padding(bottom = 80.dp)
                    .shadow(16.dp, RoundedCornerShape(16.dp))
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Category")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundGradient)
                .padding(paddingValues)
                .statusBarsPadding(),
            contentPadding = PaddingValues(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Section
            item {
                SummaryHeader(
                    totalSpent = totalSpent,
                    categoryCount = categoriesWithTotal.size,
                    onViewInsights = {
                        navController.navigate("project_insights/$projectId")
                    },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                )
            }
            // Categories Header with Sort
            if (categoriesWithTotal.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Category,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Categories (${categoriesWithTotal.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ViewTypeToggle(
                                currentViewType = currentViewType,
                                onViewTypeChange = { currentViewType = it }
                            )

                            SortDropdown(
                                current = categorySortOption,
                                onChange = { categorySortOption = it }
                            )
                        }
                    }
                }
            }



            // Empty State
            if (categoriesWithTotal.isEmpty()) {
                item {
                    EmptyStateView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 60.dp)
                    )
                }
            } else {
                // Category List/Grid
                when (currentViewType) {
                    ViewType.GRID -> {
                        // Grid View - 2 columns with heightened cards
                        items(
                            items = sortedCategories.chunked(2),
                            key = { row -> row.first().category.id }
                        ) { rowItems ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowItems.forEach { categoryWithTotal ->
                                    CategoryGridItem(
                                        categoryWithTotal = categoryWithTotal,
                                        maxCategoryAmount = maxCategoryAmount,
                                        totalAmountAllCategories = totalSpent,
                                        onCategoryClick = {
                                            navController.navigate("expense_list/${categoryWithTotal.category.id}")
                                        },
                                        onEditClick = {
                                            categoryToEdit = categoryWithTotal
                                            currentSheetType = CategorySheetType.EDIT
                                            showSheet = true
                                            scope.launch { sheetState.show() }
                                        },
                                        onDeleteClick = {
                                            categoryToDelete = categoryWithTotal
                                            currentSheetType = CategorySheetType.DELETE
                                            showSheet = true
                                            scope.launch { sheetState.show() }
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                // Add spacer if odd number of items in last row
                                if (rowItems.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                    ViewType.LIST -> {
                        // List View - Full width cards
                        items(sortedCategories, key = { it.category.id }) { categoryWithTotal ->
                            CategoryListItem(
                                categoryWithTotal = categoryWithTotal,
                                maxCategoryAmount = maxCategoryAmount,
                                totalAmountAllCategories = totalSpent,
                                onCategoryClick = {
                                    navController.navigate("expense_list/${categoryWithTotal.category.id}")
                                },
                                onEditClick = {
                                    categoryToEdit = categoryWithTotal
                                    currentSheetType = CategorySheetType.EDIT
                                    showSheet = true
                                    scope.launch { sheetState.show() }
                                },
                                onDeleteClick = {
                                    categoryToDelete = categoryWithTotal
                                    currentSheetType = CategorySheetType.DELETE
                                    showSheet = true
                                    scope.launch { sheetState.show() }
                                },
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSheet && currentSheetType != null) {
        ModalBottomSheet(
            onDismissRequest = {
                showSheet = false
                currentSheetType = null
                categoryToEdit = null
                categoryToDelete = null
            },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            when (currentSheetType) {
                CategorySheetType.ADD -> {
                    AddCategorySheetContent(
                        onCancel = {
                            showSheet = false
                            currentSheetType = null
                        },
                        onConfirm = { newCategoryName, emoji ->
                            if (newCategoryName.isNotBlank()) {
                                viewModel.insertCategory(
                                    Category(
                                        name = newCategoryName,
                                        projectId = projectId,
                                        emoji = emoji
                                    )
                                )
                                scope.launch { sheetState.hide() }.invokeOnCompletion {
                                    showSheet = false
                                    currentSheetType = null
                                }
                            }
                        }
                    )
                }

                CategorySheetType.EDIT -> {
                    val editing = categoryToEdit
                    if (editing != null) {
                        EditCategorySheetContent(
                            currentName = editing.category.name,
                            currentEmoji = editing.category.emoji,
                            onCancel = {
                                showSheet = false
                                currentSheetType = null
                                categoryToEdit = null
                            },
                            onConfirm = { newName, newEmoji ->
                                viewModel.updateCategory(
                                    editing.category.copy(
                                        name = newName,
                                        emoji = newEmoji
                                    )
                                )
                                scope.launch { sheetState.hide() }.invokeOnCompletion {
                                    showSheet = false
                                    currentSheetType = null
                                    categoryToEdit = null
                                }
                            }
                        )
                    }
                }

                CategorySheetType.DELETE -> {
                    val deleting = categoryToDelete
                    if (deleting != null) {
                        DeleteCategorySheetContent(
                            categoryName = deleting.category.name,
                            onCancel = {
                                showSheet = false
                                currentSheetType = null
                                categoryToDelete = null
                            },
                            onConfirm = {
                                viewModel.deleteCategory(deleting.category)
                                scope.launch { sheetState.hide() }.invokeOnCompletion {
                                    showSheet = false
                                    currentSheetType = null
                                    categoryToDelete = null
                                }
                            }
                        )
                    }
                }

                null -> Unit
            }
        }
    }
}

@Composable
fun SummaryHeader(
    totalSpent: Double,
    categoryCount: Int,
    onViewInsights: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Total Spending",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Text(
                            text = formatCurrency(totalSpent),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Divider(
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f),
                    thickness = 1.dp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    InfoChip(
                        label = "Categories",
                        value = categoryCount.toString(),
                        icon = "📁"
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        modifier = Modifier.clickable(onClick = onViewInsights)
                    ) {
                        Text(
                            text = "View insights →",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InfoChip(
    label: String,
    value: String,
    icon: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = icon,
                fontSize = 20.sp
            )
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun ChartSection(
    currentChartType: ChartType,
    onChartTypeChange: (ChartType) -> Unit,
    categoryExpenses: List<CategoryExpense>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(20.dp)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Spending Insights",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Distribution by category",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                ChartToggleButton(
                    currentSelection = currentChartType,
                    onSelectionChange = onChartTypeChange
                )
            }

            when (currentChartType) {
                ChartType.PIE -> {
                    val pieEntries = categoryExpenses.map {
                        PieEntry(it.totalAmount.toFloat(), it.categoryName)
                    }
                    PieChart(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .padding(vertical = 8.dp),
                        entries = pieEntries,
                        description = ""
                    )
                }

                ChartType.BAR -> {
                    val barEntries = categoryExpenses.mapIndexed { index, item ->
                        BarEntry(index.toFloat(), item.totalAmount.toFloat())
                    }
                    val labels = categoryExpenses.map { it.categoryName }
                    BarChart(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .padding(vertical = 8.dp),
                        entries = barEntries,
                        labels = labels,
                        description = ""
                    )
                }
            }
        }
    }
}

@Composable
fun ChartToggleButton(
    currentSelection: ChartType,
    onSelectionChange: (ChartType) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier
                .height(40.dp)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ChartType.values().forEach { chartType ->
                val isSelected = currentSelection == chartType
                val backgroundColor = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    Color.Transparent
                val textColor = if (isSelected)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant

                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSelectionChange(chartType) },
                    shape = RoundedCornerShape(8.dp),
                    color = backgroundColor
                ) {
                    Text(
                        text = chartType.name.lowercase().replaceFirstChar { it.uppercase() },
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = textColor,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ViewTypeToggle(
    currentViewType: ViewType,
    onViewTypeChange: (ViewType) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier
                .height(40.dp)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ViewType.values().forEach { viewType ->
                val isSelected = currentViewType == viewType
                val backgroundColor = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    Color.Transparent
                val iconColor = if (isSelected)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant

                Surface(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onViewTypeChange(viewType) },
                    shape = RoundedCornerShape(8.dp),
                    color = backgroundColor
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (viewType == ViewType.GRID) "⊞" else "☰",
                            fontSize = 18.sp,
                            color = iconColor,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateView(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                modifier = Modifier.size(80.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "📂",
                        fontSize = 40.sp
                    )
                }
            }
            Text(
                text = "No categories yet",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Start organizing your expenses by creating your first category",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun CategoryListItem(
    categoryWithTotal: CategoryWithTotal,
    maxCategoryAmount: Double,
    totalAmountAllCategories: Double,
    onCategoryClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val shareOfTotal =
        if (totalAmountAllCategories > 0) (categoryWithTotal.totalAmount / totalAmountAllCategories).toFloat()
        else 0f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            )
            .clickable(onClick = onCategoryClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = categoryWithTotal.category.emoji,
                                    fontSize = 24.sp
                                )
                            }
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = categoryWithTotal.category.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "${(shareOfTotal * 100).toInt()}% of budget",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "More Options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit category") },
                                onClick = {
                                    onEditClick()
                                    menuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete category") },
                                onClick = {
                                    onDeleteClick()
                                    menuExpanded = false
                                }
                            )
                        }
                    }
                }

                Divider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    thickness = 1.dp
                )

                // Footer Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Total Amount",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatCurrency(categoryWithTotal.totalAmount),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (categoryWithTotal.expenseCount == 1)
                                "1 expense"
                            else
                                "${categoryWithTotal.expenseCount} expenses",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "View →",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryGridItem(
    categoryWithTotal: CategoryWithTotal,
    maxCategoryAmount: Double,
    totalAmountAllCategories: Double,
    onCategoryClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val shareOfTotal =
        if (totalAmountAllCategories > 0) (categoryWithTotal.totalAmount / totalAmountAllCategories).toFloat()
        else 0f

    // Calculate dynamic height based on amount
    val heightFactor =
        if (maxCategoryAmount > 0) {
            (0.4f * (categoryWithTotal.totalAmount / maxCategoryAmount).toFloat() + 0.7f)
        } else 0.85f

    val cardHeight = (190.dp * heightFactor).coerceIn(200.dp, 350.dp)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(cardHeight)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            )
            .clickable(onClick = onCategoryClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header Section
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = categoryWithTotal.category.emoji,
                                    fontSize = 24.sp
                                )
                            }
                        }

                        Box {
                            IconButton(
                                onClick = { menuExpanded = true },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = "More Options",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Edit", style = MaterialTheme.typography.bodyMedium) },
                                    onClick = {
                                        onEditClick()
                                        menuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete", style = MaterialTheme.typography.bodyMedium) },
                                    onClick = {
                                        onDeleteClick()
                                        menuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = categoryWithTotal.category.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 17.sp,
                            lineHeight = 20.sp
                        )

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "${(shareOfTotal * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Footer Section
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = formatCurrency(categoryWithTotal.totalAmount),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 20.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (categoryWithTotal.expenseCount == 1)
                                "1 expense"
                            else
                                "${categoryWithTotal.expenseCount} expenses",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        )

                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "→",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
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
fun AddCategorySheetContent(
    onCancel: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var newCategoryName by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("📂") }

    val emojis = listOf(
        "💰", "🪙", "💵", "💴", "💶", "💷", "💳", "🏦", "📉", "📈", "🧾", "💱", "💲", "🏧",
        "🍽️", "☕", "🍔", "🍕", "🥗", "🍜", "🍲", "🍱", "🍣", "🍩", "🍿", "🧃", "🍻",
        "🛒", "🛍️",
        "🏠", "🏡", "🛏️", "🪑", "🛋️", "🧹", "🧽", "🧺",
        "🔧", "🛠️", "🧱", "💡", "🔌", "🚿", "🔥", "🧯",
        "📦", "🔑",
        "🚗", "🚕", "🚌", "🚆", "🚇", "🚲", "✈️", "⛽", "🛞",
        "🛣️", "🚦", "🛳️",
        "🧴", "🧼", "🪒", "💅", "💄", "🩺", "💊", "🛌",
        "🧘‍♂️", "🧘‍♀️",
        "👗", "👚", "👕", "👟", "🎒", "👜", "👛", "💍",
        "🎁", "🎀", "🌸", "🐾",
        "📂", "📁", "💼", "📅", "🗂️", "📝", "✏️", "📚",
        "📊", "📈", "📉", "📎", "🧾",
        "📱", "💻", "🖥️", "🎧", "🎮", "🖱️", "⌨️", "🔋",
        "🎨", "🎬", "🎮", "🎵", "🎤", "🎧", "📷", "🎞️",
        "🎭", "🎯", "🎉", "🎢", "🏟️",
        "🌍", "🌎", "🌏", "🧳", "🏝️", "🗺️", "🏨", "⛺",
        "🚐", "🚤",
        "👶", "🍼", "🧸", "🎒",
        "🏥", "🩺", "🩹", "🚑",
        "🌟", "⚡", "🔥", "💡", "🌈", "🔒", "🔓", "💬",
        "📢", "📦", "🔍"
    )

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "Create New Category",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Select an emoji",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    emojis.forEach { emoji ->
                        val isSelected = emoji == selectedEmoji
                        Surface(
                            modifier = Modifier
                                .size(56.dp)
                                .clickable { selectedEmoji = emoji },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            else
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                            border = if (isSelected)
                                BorderStroke(
                                    2.dp,
                                    MaterialTheme.colorScheme.primary
                                )
                            else null
                        ) {
                            Box(
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = emoji,
                                    fontSize = 28.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        TextField(
            value = newCategoryName,
            onValueChange = { newCategoryName = it },
            label = { Text("Category Name") },
            placeholder = { Text("e.g., Groceries, Transport") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            singleLine = true
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel", style = MaterialTheme.typography.labelLarge)
            }
            Button(
                onClick = { onConfirm(newCategoryName, selectedEmoji) },
                enabled = newCategoryName.isNotBlank(),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Create", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun EditCategorySheetContent(
    currentName: String,
    currentEmoji: String,
    onCancel: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var editedCategoryName by remember { mutableStateOf(currentName) }
    var selectedEmoji by remember { mutableStateOf(currentEmoji) }

    val emojis = listOf(
        "💰", "🪙", "💵", "💴", "💶", "💷", "💳", "🏦", "📉", "📈", "🧾", "💱", "💲", "🏧",
        "🍽️", "☕", "🍔", "🍕", "🥗", "🍜", "🍲", "🍱", "🍣", "🍩", "🍿", "🧃", "🍻",
        "🛒", "🛍️",
        "🏠", "🏡", "🛏️", "🪑", "🛋️", "🧹", "🧽", "🧺",
        "🔧", "🛠️", "🧱", "💡", "🔌", "🚿", "🔥", "🧯",
        "📦", "🔑",
        "🚗", "🚕", "🚌", "🚆", "🚇", "🚲", "✈️", "⛽", "🛞",
        "🛣️", "🚦", "🛳️",
        "🧴", "🧼", "🪒", "💅", "💄", "🩺", "💊", "🛌",
        "🧘‍♂️", "🧘‍♀️",
        "👗", "👚", "👕", "👟", "🎒", "👜", "👛", "💍",
        "🎁", "🎀", "🌸", "🐾",
        "📂", "📁", "💼", "📅", "🗂️", "📝", "✏️", "📚",
        "📊", "📈", "📉", "📎", "🧾",
        "📱", "💻", "🖥️", "🎧", "🎮", "🖱️", "⌨️", "🔋",
        "🎨", "🎬", "🎮", "🎵", "🎤", "🎧", "📷", "🎞️",
        "🎭", "🎯", "🎉", "🎢", "🏟️",
        "🌍", "🌎", "🌏", "🧳", "🏝️", "🗺️", "🏨", "⛺",
        "🚐", "🚤",
        "👶", "🍼", "🧸", "🎒",
        "🏥", "🩺", "🩹", "🚑",
        "🌟", "⚡", "🔥", "💡", "🌈", "🔒", "🔓", "💬",
        "📢", "📦", "🔍"
    )
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "Edit Category",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Select an emoji",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    emojis.forEach { emoji ->
                        val isSelected = emoji == selectedEmoji
                        Surface(
                            modifier = Modifier
                                .size(56.dp)
                                .clickable { selectedEmoji = emoji },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            else
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                            border = if (isSelected)
                                BorderStroke(
                                    2.dp,
                                    MaterialTheme.colorScheme.primary
                                )
                            else null
                        ) {
                            Box(
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = emoji,
                                    fontSize = 28.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        TextField(
            value = editedCategoryName,
            onValueChange = { editedCategoryName = it },
            label = { Text("Category Name") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            singleLine = true
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel", style = MaterialTheme.typography.labelLarge)
            }
            Button(
                onClick = { onConfirm(editedCategoryName, selectedEmoji) },
                enabled = editedCategoryName.isNotBlank() &&
                        (editedCategoryName != currentName || selectedEmoji != currentEmoji),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun DeleteCategorySheetContent(
    categoryName: String,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Delete Category?",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "⚠️ Warning",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "You're about to delete '$categoryName' and all its associated expenses. This action cannot be undone.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Keep it", style = MaterialTheme.typography.labelLarge)
            }
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Delete", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}