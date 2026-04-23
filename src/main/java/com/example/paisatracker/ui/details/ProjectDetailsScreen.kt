package com.example.paisatracker.ui.details

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.paisatracker.PaisaTrackerApplication
import com.example.paisatracker.PaisaTrackerViewModel
import com.example.paisatracker.data.Category
import com.example.paisatracker.data.CategoryWithTotal
import com.example.paisatracker.ui.common.SortDropdown
import com.example.paisatracker.ui.common.SortOption
import com.example.paisatracker.ui.details.category.*
import com.example.paisatracker.ui.main.projects.SearchFilterCard
import com.example.paisatracker.ui.main.projects.SearchResultsCard
import com.example.paisatracker.ui.search.SearchViewModel
import com.example.paisatracker.ui.search.SearchViewModelFactory
import kotlinx.coroutines.launch

/**
 * ProjectDetailsScreen is the host screen responsible for:
 * - Collecting state from the ViewModel
 * - Delegating UI to focused child composables
 * - Managing bottom sheet state for add/edit/delete
 *
 * Business logic decisions:
 * - Sheet type is modeled as a sealed class for exhaustive `when` coverage
 * - `categoryToEdit` and `categoryToDelete` are nullable; UI composables are only
 *   called when they are non-null, satisfying Kotlin null safety without force-unwrapping.
 * - Sort is local UI state — not persisted — for simplicity and rotation safety.
 */

sealed class CategorySheetMode {
    object Add : CategorySheetMode()
    data class Edit(val category: CategoryWithTotal) : CategorySheetMode()
    data class Delete(val category: CategoryWithTotal) : CategorySheetMode()
}

enum class ViewType { GRID, LIST }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailsScreen(
    viewModel: PaisaTrackerViewModel,
    projectId: Long,
    navController: NavController
) {
    val context = LocalContext.current
    val application = context.applicationContext as PaisaTrackerApplication

    // ── Search ────────────────────────────────────────────────────────────────
    val searchViewModel: SearchViewModel = viewModel(
        factory = SearchViewModelFactory(application.repository)
    )
    val searchQuery    by searchViewModel.searchQuery.collectAsState()
    val minAmount      by searchViewModel.minAmount.collectAsState()
    val maxAmount      by searchViewModel.maxAmount.collectAsState()
    val searchResults  by searchViewModel.searchResults.collectAsState()
    val isSearchActive by searchViewModel.isSearchActive.collectAsState()

    LaunchedEffect(projectId) {
        searchViewModel.setProjectId(projectId)
    }

    val categoriesWithTotal by viewModel
        .getCategoriesWithTotalForProject(projectId)
        .collectAsState(initial = emptyList())

    var sortOption    by remember { mutableStateOf(SortOption.DATE_NEW_OLD) }
    var viewType      by remember { mutableStateOf(ViewType.GRID) }
    var sheetMode     by remember { mutableStateOf<CategorySheetMode?>(null) }
    var searchExpanded by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope      = rememberCoroutineScope()

    val totalSpent = categoriesWithTotal.sumOf { it.totalAmount }

    val sortedCategories = remember(categoriesWithTotal, sortOption) {
        when (sortOption) {
            SortOption.AMOUNT_LOW_HIGH  -> categoriesWithTotal.sortedBy { it.totalAmount }
            SortOption.AMOUNT_HIGH_LOW  -> categoriesWithTotal.sortedByDescending { it.totalAmount }
            SortOption.NAME_A_Z         -> categoriesWithTotal.sortedBy { it.category.name.lowercase() }
            SortOption.NAME_Z_A         -> categoriesWithTotal.sortedByDescending { it.category.name.lowercase() }
            SortOption.DATE_OLD_NEW     -> categoriesWithTotal.sortedBy { it.latestExpenseTime ?: 0L }
            SortOption.DATE_NEW_OLD     -> categoriesWithTotal.sortedByDescending { it.latestExpenseTime ?: 0L }
        }
    }

    // null sentinel = "Add" button at head of list
    val listItems: List<CategoryWithTotal?> = remember(sortedCategories) {
        listOf(null) + sortedCategories
    }

    fun openSheet(mode: CategorySheetMode) {
        sheetMode = mode
        scope.launch { sheetState.show() }
    }

    fun closeSheet() {
        scope.launch { sheetState.hide() }.invokeOnCompletion { sheetMode = null }
    }

    Scaffold(containerColor = Color.Transparent) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues),
            contentPadding = PaddingValues(top = 0.dp, bottom = 180.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Summary header
            item {
                ProjectSummaryHeader(
                    totalSpent = totalSpent,
                    categoryCount = categoriesWithTotal.size,
                    onViewInsights = { navController.navigate("project_insights/$projectId") },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            // Controls row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Categories • ${categoriesWithTotal.size}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { searchExpanded = !searchExpanded }) {
                            Icon(
                                Icons.Outlined.Search,
                                contentDescription = "Search in project",
                                tint = if (searchExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        ViewTypeToggle(currentViewType = viewType, onViewTypeChange = { viewType = it })
                        SortDropdown(current = sortOption, onChange = { sortOption = it })
                    }
                }
            }

            // Search panel
            item {
                AnimatedVisibility(
                    visible = searchExpanded,
                    enter   = expandVertically(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow)) + fadeIn(tween(300)),
                    exit    = shrinkVertically(spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium)) + fadeOut(tween(200))
                ) {
                    SearchFilterCard(
                        searchQuery         = searchQuery,
                        onSearchQueryChange = { searchViewModel.onSearchQueryChanged(it) },
                        minAmount           = minAmount,
                        onMinAmountChange   = { searchViewModel.onMinAmountChanged(it) },
                        maxAmount           = maxAmount,
                        onMaxAmountChange   = { searchViewModel.onMaxAmountChanged(it) },
                        onSearch            = { searchViewModel.executeSearch() },
                        onClear             = { searchViewModel.clearSearch(); searchExpanded = false },
                        isSearchActive      = isSearchActive
                    )
                }
            }

            if (isSearchActive) {
                item {
                    SearchResultsCard(results = searchResults, isActive = isSearchActive)
                }
            } else {
                // Category grid / list
                when (viewType) {
                    ViewType.GRID -> {
                        items(
                            items = listItems.chunked(2),
                            key = { row -> row.firstOrNull()?.category?.id ?: -1L }
                        ) { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowItems.forEach { item ->
                                    if (item == null) {
                                        AddCategoryGridItem(
                                            onClick = { openSheet(CategorySheetMode.Add) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    } else {
                                        CategoryGridItem(
                                            categoryWithTotal = item,
                                            totalAmountAllCategories = totalSpent,
                                            onCategoryClick = { navController.navigate("expense_list/${item.category.id}") },
                                            onEditClick = { openSheet(CategorySheetMode.Edit(item)) },
                                            onDeleteClick = { openSheet(CategorySheetMode.Delete(item)) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }

                    ViewType.LIST -> {
                        items(listItems, key = { it?.category?.id ?: -1L }) { item ->
                            if (item == null) {
                                AddCategoryListItem(
                                    onClick = { openSheet(CategorySheetMode.Add) },
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            } else {
                                CategoryListItem(
                                    categoryWithTotal = item,
                                    totalAmountAllCategories = totalSpent,
                                    onCategoryClick = { navController.navigate("expense_list/${item.category.id}") },
                                    onEditClick = { openSheet(CategorySheetMode.Edit(item)) },
                                    onDeleteClick = { openSheet(CategorySheetMode.Delete(item)) },
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Bottom sheet — exhaustive `when` over sealed CategorySheetMode
    sheetMode?.let { mode ->
        ModalBottomSheet(
            onDismissRequest = { sheetMode = null },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            when (mode) {
                is CategorySheetMode.Add -> {
                    AddCategorySheetContent(
                        viewModel = viewModel,
                        onCancel = ::closeSheet,
                        onConfirm = { name, emoji ->
                            viewModel.insertCategory(
                                Category(name = name, projectId = projectId, emoji = emoji)
                            )
                            closeSheet()
                        }
                    )
                }

                is CategorySheetMode.Edit -> {
                    EditCategorySheetContent(
                        currentName = mode.category.category.name,
                        currentEmoji = mode.category.category.emoji,
                        viewModel = viewModel,
                        onCancel = ::closeSheet,
                        onConfirm = { name, emoji ->
                            viewModel.updateCategory(mode.category.category.copy(name = name, emoji = emoji))
                            closeSheet()
                        }
                    )
                }

                is CategorySheetMode.Delete -> {
                    DeleteCategorySheetContent(
                        categoryName = mode.category.category.name,
                        categoryEmoji = mode.category.category.emoji,
                        expenseCount = mode.category.expenseCount,
                        onCancel = ::closeSheet,
                        onConfirm = {
                            viewModel.deleteCategory(mode.category.category)
                            closeSheet()
                        }
                    )
                }
            }
        }
    }
}
