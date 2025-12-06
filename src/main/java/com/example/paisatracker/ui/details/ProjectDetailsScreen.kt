package com.example.paisatracker.ui.details

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.paisatracker.PaisaTrackerViewModel
import com.example.paisatracker.R
import com.example.paisatracker.data.Category
import com.example.paisatracker.data.CategoryWithTotal
import com.example.paisatracker.ui.common.BarChart
import com.example.paisatracker.ui.common.PieChart
import com.example.paisatracker.util.formatCurrency
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieEntry

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState

import com.example.paisatracker.ui.common.SortDropdown
import com.example.paisatracker.ui.common.SortOption




enum class ChartType {
    PIE, BAR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailsScreen(viewModel: PaisaTrackerViewModel, projectId: Long, navController: NavController) {
    val categoriesWithTotal by viewModel.getCategoriesWithTotalForProject(projectId).collectAsState(initial = emptyList())
    val categoryExpenses by viewModel.getCategoryExpenses(projectId).collectAsState(initial = emptyList())

    //new state added for filter
    var categorySortOption by remember { mutableStateOf(SortOption.AMOUNT_HIGH_LOW) }


    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<CategoryWithTotal?>(null) }

    var showEditDialog by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<CategoryWithTotal?>(null) }
    var editedCategoryName by remember { mutableStateOf("") }

    // Chart toggle state
    var currentChartType by remember { mutableStateOf(ChartType.BAR) }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
            MaterialTheme.colorScheme.background
        )
    )

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddCategoryDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Category")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundGradient)
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (categoryExpenses.isNotEmpty()) {
                // Chart Toggle Button
                ChartToggleButton(
                    currentSelection = currentChartType,
                    onSelectionChange = { currentChartType = it },
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .fillMaxWidth(0.5f)
                )

                // Display selected chart
                when (currentChartType) {
                    ChartType.PIE -> {
                        val pieEntries = categoryExpenses.map {
                            PieEntry(it.totalAmount.toFloat(), it.categoryName)
                        }
                        PieChart(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                                .padding(horizontal = 8.dp),
                            entries = pieEntries,
                            description = ""
                        )
                    }
                    ChartType.BAR -> {
                        val barEntries = categoryExpenses.mapIndexed { index, categoryExpense ->
                            BarEntry(index.toFloat(), categoryExpense.totalAmount.toFloat())
                        }
                        val labels = categoryExpenses.map { it.categoryName }
                        BarChart(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                                .padding(horizontal = 8.dp),
                            entries = barEntries,
                            labels = labels,
                            description = ""
                        )
                    }
                }
            }

            if (categoriesWithTotal.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    SortDropdown(
                        current = categorySortOption,
                        onChange = { categorySortOption = it }
                    )
                }
            }

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
                }
            }


            if (categoriesWithTotal.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No categories found.\nAdd one to get started!",
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        fontSize = 18.sp
                    )
                }
            } else {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    contentPadding = PaddingValues(8.dp, bottom = 110.dp  ),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(sortedCategories, key = { it.category.id }) { categoryWithTotal ->
                        CategoryListItem(
                            categoryWithTotal = categoryWithTotal,
                            onCategoryClick = { navController.navigate("expense_list/${categoryWithTotal.category.id}") },
                            onEditClick = {
                                categoryToEdit = categoryWithTotal
                                editedCategoryName = categoryWithTotal.category.name
                                showEditDialog = true
                            },
                            onDeleteClick = {
                                categoryToDelete = categoryWithTotal
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    // Add Category Dialog ke text block ko replace karo:

    if (showAddCategoryDialog) {
        var selectedEmoji by remember { mutableStateOf("📂") }
        val emojis = listOf(
            "📂","🛠️","🧾","🍽️","🏠","🚗","🎁","📦","💡","🎧",
            "💳","🛒","🧱","🛏️","🧹","🔧","📚","🎧","🧴","🎨"
        )
        val scrollState = rememberScrollState()

        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text("New Category") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    Text(
                        text = "Choose emoji (swipe →)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(scrollState)
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        emojis.forEach { emoji ->
                            val isSelected = emoji == selectedEmoji
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected)
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        else
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    )
                                    .clickable { selectedEmoji = emoji }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = emoji,
                                    fontSize = 24.sp
                                )
                            }
                        }
                    }

                    TextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        label = { Text("Category Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newCategoryName.isNotBlank()) {
                        viewModel.insertCategory(
                            Category(
                                name = newCategoryName,
                                projectId = projectId,
                                emoji = selectedEmoji
                            )
                        )
                        newCategoryName = ""
                        showAddCategoryDialog = false
                    }
                }) { Text("Add") }
            },
            dismissButton = {
                Button(onClick = { showAddCategoryDialog = false }) { Text("Cancel") }
            }
        )
    }


    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Category") },
            text = { Text("Are you sure you want to delete '${categoryToDelete?.category?.name}'? This will also delete all its expenses.") },
            confirmButton = {
                Button(onClick = {
                    categoryToDelete?.let { viewModel.deleteCategory(it.category) }
                    showDeleteDialog = false
                }) { Text("Delete") }
            },
            dismissButton = { Button(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
        )
    }

    if (showEditDialog) {
        var selectedEmoji by remember {
            mutableStateOf(categoryToEdit?.category?.emoji ?: "📂")
        }
        val emojis = listOf(
            "📂","🛠️","🧾","🍽️","🏠","🚗","🎁","📦","💡","🎧",
            "💳","🛒","🧱","🛏️","🧹","🔧","📚","🎧","🧴","🎨"
        )
        val scrollState = rememberScrollState()

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Category") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    Text(
                        text = "Choose emoji (swipe →)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(scrollState)
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        emojis.forEach { emoji ->
                            val isSelected = emoji == selectedEmoji
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected)
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        else
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    )
                                    .clickable { selectedEmoji = emoji }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = emoji,
                                    fontSize = 24.sp
                                )
                            }
                        }
                    }

                    TextField(
                        value = editedCategoryName,
                        onValueChange = { editedCategoryName = it },
                        label = { Text("Category Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (editedCategoryName.isNotBlank()) {
                        categoryToEdit?.let {
                            viewModel.updateCategory(
                                it.category.copy(
                                    name = editedCategoryName,
                                    emoji = selectedEmoji
                                )
                            )
                        }
                        showEditDialog = false
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                Button(onClick = { showEditDialog = false }) { Text("Cancel") }
            }
        )
    }


}

@Composable
fun ChartToggleButton(
    currentSelection: ChartType,
    onSelectionChange: (ChartType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        horizontalArrangement = Arrangement.Center,
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
                    .padding(4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onSelectionChange(chartType) },
                color = backgroundColor
            ) {
                Text(
                    text = chartType.name,
                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 20.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = textColor
                )
            }
        }
    }
}

@Composable
fun CategoryListItem(
    categoryWithTotal: CategoryWithTotal,
    onCategoryClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val cardGradient = Brush.horizontalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)
        )
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onCategoryClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(modifier = Modifier.background(cardGradient)) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Emoji + name stacked
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = categoryWithTotal.category.emoji,
                            style = MaterialTheme.typography.headlineSmall,
                            fontSize = 26.sp
                        )
                    }

                    // Right: menu
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More Options")
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
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                onClick = {
                                    onDeleteClick()
                                    menuExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = categoryWithTotal.category.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = formatCurrency(categoryWithTotal.totalAmount),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}
