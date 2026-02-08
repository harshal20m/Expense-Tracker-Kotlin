package com.example.paisatracker.ui.details

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.ViewList
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
    var currentViewType by remember { mutableStateOf(ViewType.GRID) }

    var currentSheetType by remember { mutableStateOf<CategorySheetType?>(null) }
    var categoryToEdit by remember { mutableStateOf<CategoryWithTotal?>(null) }
    var categoryToDelete by remember { mutableStateOf<CategoryWithTotal?>(null) }

    val sheetState: SheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    var showSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

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
                shape = CircleShape,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add Category",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 80.dp, top = 0.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Compact Summary Header
            item {
                CompactSummaryHeader(
                    totalSpent = totalSpent,
                    categoryCount = categoriesWithTotal.size,
                    onViewInsights = {
                        navController.navigate("project_insights/$projectId")
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            // Categories Header with Controls
            if (categoriesWithTotal.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 0.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Categories • ${categoriesWithTotal.size}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CompactViewTypeToggle(
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
                    CompactEmptyState(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 40.dp)
                    )
                }
            } else {
                // Category List/Grid
                when (currentViewType) {
                    ViewType.GRID -> {
                        items(
                            items = sortedCategories.chunked(2),
                            key = { row -> row.first().category.id }
                        ) { rowItems ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowItems.forEach { categoryWithTotal ->
                                    CompactCategoryGridItem(
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
                                if (rowItems.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                    ViewType.LIST -> {
                        items(sortedCategories, key = { it.category.id }) { categoryWithTotal ->
                            CompactCategoryListItem(
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
                                modifier = Modifier.padding(horizontal = 16.dp)
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
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
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
fun CompactSummaryHeader(
    totalSpent: Double,
    categoryCount: Int,
    onViewInsights: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            MaterialTheme.colorScheme.primaryContainer
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Total Spending",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            text = formatCurrency(totalSpent),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Surface(
                        onClick = onViewInsights,
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Insights",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "📁", fontSize = 16.sp)
                        Text(
                            text = "$categoryCount Categories",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CompactViewTypeToggle(
    currentViewType: ViewType,
    onViewTypeChange: (ViewType) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            ViewType.values().forEach { viewType ->
                val isSelected = currentViewType == viewType
                Surface(
                    modifier = Modifier
                        .size(32.dp)
                        .clickable { onViewTypeChange(viewType) },
                    shape = RoundedCornerShape(7.dp),
                    color = if (isSelected)
                        MaterialTheme.colorScheme.primary
                    else Color.Transparent
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (viewType == ViewType.GRID)
                                Icons.Outlined.GridView else Icons.Outlined.ViewList,
                            contentDescription = null,
                            tint = if (isSelected)
                                MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CompactEmptyState(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "📂", fontSize = 48.sp)
            Text(
                text = "No categories yet",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Tap + to create your first category",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun CompactCategoryListItem(
    categoryWithTotal: CategoryWithTotal,
    maxCategoryAmount: Double,
    totalAmountAllCategories: Double,
    onCategoryClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val shareOfTotal = if (totalAmountAllCategories > 0)
        (categoryWithTotal.totalAmount / totalAmountAllCategories).toFloat() else 0f

    val progress by animateFloatAsState(
        targetValue = shareOfTotal,
        animationSpec = spring(dampingRatio = 0.7f),
        label = "progress"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onCategoryClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = categoryWithTotal.category.emoji, fontSize = 20.sp)
                        }
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = categoryWithTotal.category.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${categoryWithTotal.expenseCount} expense${if (categoryWithTotal.expenseCount != 1) "s" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = formatCurrency(categoryWithTotal.totalAmount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "${(shareOfTotal * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Options",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = {
                                onEditClick()
                                menuExpanded = false
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Edit, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = {
                                onDeleteClick()
                                menuExpanded = false
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, contentDescription = null)
                            }
                        )
                    }
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }
}

@Composable
fun CompactCategoryGridItem(
    categoryWithTotal: CategoryWithTotal,
    maxCategoryAmount: Double,
    totalAmountAllCategories: Double,
    onCategoryClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val shareOfTotal = if (totalAmountAllCategories > 0)
        (categoryWithTotal.totalAmount / totalAmountAllCategories).toFloat() else 0f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .clickable(onClick = onCategoryClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = categoryWithTotal.category.emoji, fontSize = 20.sp)
                            }
                        }

                        Box {
                            IconButton(
                                onClick = { menuExpanded = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = "Options",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Edit", style = MaterialTheme.typography.bodySmall) },
                                    onClick = {
                                        onEditClick()
                                        menuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete", style = MaterialTheme.typography.bodySmall) },
                                    onClick = {
                                        onDeleteClick()
                                        menuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Text(
                        text = categoryWithTotal.category.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 18.sp
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = formatCurrency(categoryWithTotal.totalAmount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${categoryWithTotal.expenseCount} exp",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "${(shareOfTotal * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
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
        "💰", "💵", "💳", "🏦", "📊", "📈", "🧾",
        "🍔", "☕", "🍕", "🛒", "🍜",
        "🏠", "🔧", "💡", "🔑",
        "🚗", "⛽", "🚲", "✈️",
        "💊", "🩺", "💅",
        "👕", "👟", "💍",
        "📱", "💻", "🎮",
        "🎬", "🎵", "📷",
        "🌍", "🏨", "🧳",
        "🎁", "🌸", "⚡"
    )

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "New Category",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Pick an emoji",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    emojis.forEach { emoji ->
                        val isSelected = emoji == selectedEmoji
                        Surface(
                            modifier = Modifier
                                .size(48.dp)
                                .clickable { selectedEmoji = emoji },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                            border = if (isSelected)
                                BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                            else null
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = emoji, fontSize = 24.sp)
                            }
                        }
                    }
                }
            }
        }

        TextField(
            value = newCategoryName,
            onValueChange = { newCategoryName = it },
            label = { Text("Name") },
            placeholder = { Text("Groceries, Transport...") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            singleLine = true
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Cancel")
            }
            Button(
                onClick = { onConfirm(newCategoryName, selectedEmoji) },
                enabled = newCategoryName.isNotBlank(),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Create")
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
        "💰", "💵", "💳", "🏦", "📊", "📈", "🧾",
        "🍔", "☕", "🍕", "🛒", "🍜",
        "🏠", "🔧", "💡", "🔑",
        "🚗", "⛽", "🚲", "✈️",
        "💊", "🩺", "💅",
        "👕", "👟", "💍",
        "📱", "💻", "🎮",
        "🎬", "🎵", "📷",
        "🌍", "🏨", "🧳",
        "🎁", "🌸", "⚡"
    )
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Edit Category",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Pick an emoji",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    emojis.forEach { emoji ->
                        val isSelected = emoji == selectedEmoji
                        Surface(
                            modifier = Modifier
                                .size(48.dp)
                                .clickable { selectedEmoji = emoji },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                            border = if (isSelected)
                                BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                            else null
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = emoji, fontSize = 24.sp)
                            }
                        }
                    }
                }
            }
        }

        TextField(
            value = editedCategoryName,
            onValueChange = { editedCategoryName = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            singleLine = true
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Cancel")
            }
            Button(
                onClick = { onConfirm(editedCategoryName, selectedEmoji) },
                enabled = editedCategoryName.isNotBlank() &&
                        (editedCategoryName != currentName || selectedEmoji != currentEmoji),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save")
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
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Delete Category?",
            style = MaterialTheme.typography.headlineSmall,
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
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = "⚠️", fontSize = 18.sp)
                    Text(
                        text = "Warning",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Text(
                    text = "Deleting '$categoryName' will remove all expenses in this category. This can't be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Cancel")
            }
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Delete")
            }
        }
    }
}