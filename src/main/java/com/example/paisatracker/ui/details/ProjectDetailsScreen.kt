package com.example.paisatracker.ui.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.paisatracker.PaisaTrackerViewModel
import com.example.paisatracker.data.Category
import com.example.paisatracker.data.CategoryWithTotal
import com.example.paisatracker.ui.common.SortDropdown
import com.example.paisatracker.ui.common.SortOption
import com.example.paisatracker.ui.details.category.AddCategoryGridItem
import com.example.paisatracker.ui.details.category.AddCategoryListItem
import com.example.paisatracker.ui.details.category.AddCategorySheetContent
import com.example.paisatracker.ui.details.category.CategoryGridItem
import com.example.paisatracker.ui.details.category.CategoryListItem
import com.example.paisatracker.ui.details.category.DeleteCategorySheetContent
import com.example.paisatracker.ui.details.category.EditCategorySheetContent
import kotlinx.coroutines.launch
import androidx.compose.material3.Text

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

private sealed interface CategorySheetMode {
    data object Add : CategorySheetMode
    data class Edit(val category: CategoryWithTotal) : CategorySheetMode
    data class Delete(val category: CategoryWithTotal) : CategorySheetMode
}

enum class ViewType { GRID, LIST }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailsScreen(
    viewModel: PaisaTrackerViewModel,
    projectId: Long,
    navController: NavController
) {
    val categoriesWithTotal by viewModel
        .getCategoriesWithTotalForProject(projectId)
        .collectAsState(initial = emptyList())

    var sortOption    by remember { mutableStateOf(SortOption.DATE_NEW_OLD) }
    var viewType      by remember { mutableStateOf(ViewType.GRID) }
    var sheetMode     by remember { mutableStateOf<CategorySheetMode?>(null) }

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
                        ViewTypeToggle(currentViewType = viewType, onViewTypeChange = { viewType = it })
                        SortDropdown(current = sortOption, onChange = { sortOption = it })
                    }
                }
            }

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