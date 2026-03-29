package com.example.paisatracker.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.paisatracker.PaisaTrackerApplication
import com.example.paisatracker.PaisaTrackerViewModel
import com.example.paisatracker.R
import com.example.paisatracker.data.Project
import com.example.paisatracker.data.ProjectWithTotal
import com.example.paisatracker.data.RecentExpense
import com.example.paisatracker.ui.assets.AssetsBottomSheet
import com.example.paisatracker.ui.common.WeeklyDashboardCalendar
import com.example.paisatracker.ui.search.SearchViewModel
import com.example.paisatracker.ui.search.SearchViewModelFactory
import com.example.paisatracker.util.CurrentCurrency
import com.example.paisatracker.util.formatCurrency
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val projectEmojis = listOf(
    "📁", "💼", "🏠", "🚗", "✈️", "🎓", "💰", "🏥", "🛒", "🎯",
    "📱", "💻", "🎨", "🎬", "🎮", "📚", "☕", "🍕", "🎉", "💡",
    "🔧", "🏃", "🎵", "📷", "🌟", "🔥", "💎", "🎁", "🌈", "⚡",
    "🧾", "💳", "🏦", "📈", "📉", "💱", "🪙",
    "🍔", "🥗", "🍱", "🍻", "🧃",
    "👗", "👟", "💄", "👜", "🎒",
    "🔌", "🚿", "🧹", "🪑",
    "🚌", "🚕", "🚆", "⛽", "🛞",
    "✏️", "📝", "🧠",
    "🧴", "🧼", "💊", "🩺", "🛌",
    "🎧", "🏟️", "🎢", "🎤",
    "🛠️", "🧾", "🧯",
    "🧳", "📅", "📊",
    "🧘", "🌿", "🐾", "🎈", "👶", "🎀", "🔑"
)

private enum class SheetType { ADD, EDIT, DELETE }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalCoroutinesApi::class)
@Composable
fun ProjectListScreen(viewModel: PaisaTrackerViewModel, navController: NavController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val application = context.applicationContext as PaisaTrackerApplication

    val searchViewModel: SearchViewModel = viewModel(
        factory = SearchViewModelFactory(application.repository)
    )
    val searchQuery by searchViewModel.searchQuery.collectAsState()
    val minAmount by searchViewModel.minAmount.collectAsState()
    val maxAmount by searchViewModel.maxAmount.collectAsState()
    val searchResults by searchViewModel.searchResults.collectAsState()
    val isSearchActive by searchViewModel.isSearchActive.collectAsState()

    val currency by viewModel.currentCurrency.collectAsState()

    LaunchedEffect(currency) {
        CurrentCurrency.set(currency)
    }

    // UI Toggle States — only one can be expanded at a time
    var searchExpanded by remember { mutableStateOf(false) }
    var recentExpanded by remember { mutableStateOf(false) }
    var summaryExpanded by remember { mutableStateOf(false) }

    val projects by viewModel.getAllProjectsWithTotal().collectAsState(initial = emptyList())
    var currentSheetType by remember { mutableStateOf<SheetType?>(null) }
    var projectToEdit by remember { mutableStateOf<ProjectWithTotal?>(null) }
    var projectToDelete by remember { mutableStateOf<ProjectWithTotal?>(null) }

    val sharedPrefs = remember {
        context.getSharedPreferences("project_order", android.content.Context.MODE_PRIVATE)
    }

    var customOrderMap by remember { mutableStateOf<Map<Long, Int>>(emptyMap()) }

    if (projects.isNotEmpty() && customOrderMap.isEmpty()) {
        val savedOrder = mutableMapOf<Long, Int>()
        projects.forEach { project ->
            val savedPosition = sharedPrefs.getInt("project_${project.project.id}", -1)
            if (savedPosition >= 0) {
                savedOrder[project.project.id] = savedPosition
            }
        }
        if (savedOrder.isNotEmpty()) {
            customOrderMap = savedOrder
        } else {
            customOrderMap = projects
                .sortedByDescending { it.project.lastModified }
                .mapIndexed { index, project -> project.project.id to index }
                .toMap()
            with(sharedPrefs.edit()) {
                customOrderMap.forEach { (projectId, position) ->
                    putInt("project_$projectId", position)
                }
                apply()
            }
        }
    }

    val orderedProjects = remember(projects, customOrderMap) {
        if (customOrderMap.isEmpty()) {
            projects.sortedByDescending { it.project.lastModified }
        } else {
            projects.sortedBy { project ->
                customOrderMap[project.project.id] ?: Int.MAX_VALUE
            }
        }
    }

    val sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSheet by remember { mutableStateOf(false) }
    var showAssetsSheet by remember { mutableStateOf(false) }

    if (showAssetsSheet) {
        AssetsBottomSheet(
            viewModel = viewModel, // pass your viewmodel
            onDismiss = { showAssetsSheet = false } // Closes it when swiped down
        )
    }

    val scope = rememberCoroutineScope()

    val totalSpent = orderedProjects.sumOf { it.totalAmount }
    val totalCategories = orderedProjects.sumOf { it.categoryCount }
    val totalExpenses = orderedProjects.sumOf { it.expenseCount }
    val recentExpensesList by viewModel.recentExpenses.collectAsState(initial = emptyList())

    Column(modifier = Modifier.fillMaxSize()) {
        Header(
            onAddProjectClick = {
                currentSheetType = SheetType.ADD
                projectToEdit = null
                projectToDelete = null
                showSheet = true
                scope.launch { sheetState.show() }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Calendar Section
            item {
                WeeklyDashboardCalendar(
                    expenses = recentExpensesList,
                    onTransactionClick = { expenseId ->
                        navController.navigate("expense_details/$expenseId")
                    }
                )
            }

            // ── Action Toggles Grid (2x2: Summary, Search | Recent, Assets) ──
            item {
                // 1. Add a Box to center the content on large screens
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 500.dp) // Stops it from stretching on tablets
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Top Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ActionToggleCard(
                                title = "Summary",
                                icon = Icons.Default.KeyboardArrowDown,
                                isExpanded = summaryExpanded,
                                onClick = {
                                    summaryExpanded = !summaryExpanded
                                    if (summaryExpanded) {
                                        searchExpanded = false
                                        recentExpanded = false
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                emojiLabel = "📊"
                            )

                            ActionToggleCard(
                                title = "Search",
                                icon = Icons.Default.Search,
                                isExpanded = searchExpanded,
                                onClick = {
                                    searchExpanded = !searchExpanded
                                    if (searchExpanded) {
                                        summaryExpanded = false
                                        recentExpanded = false
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Bottom Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ActionToggleCard(
                                title = "Recent",
                                icon = Icons.Default.DateRange,
                                isExpanded = recentExpanded,
                                onClick = {
                                    recentExpanded = !recentExpanded
                                    if (recentExpanded) {
                                        summaryExpanded = false
                                        searchExpanded = false
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )

                            ActionToggleCard(
                                title = "Assets",
                                icon = Icons.Default.Image,
                                isExpanded = false,
                                onClick = { showAssetsSheet = true }, // Opens the sheet!
                                modifier = Modifier.weight(1f),
                                isNavigation = true
                            )
                        }
                    }
                }
            }




            // ── Summary Expanded Panel ──
            item {
                AnimatedVisibility(
                    visible = summaryExpanded && orderedProjects.isNotEmpty(),
                    enter = expandVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) + fadeIn(animationSpec = tween(300)),
                    exit = shrinkVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ) + fadeOut(animationSpec = tween(200))
                ) {
                    SummaryExpandedCard(
                        totalSpent = totalSpent,
                        totalProjects = orderedProjects.size,
                        totalCategories = totalCategories,
                        totalExpenses = totalExpenses
                    )
                }
            }

            // ── Search Filter Panel ──
            item {
                AnimatedVisibility(
                    visible = searchExpanded,
                    enter = expandVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) + fadeIn(animationSpec = tween(300)),
                    exit = shrinkVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ) + fadeOut(animationSpec = tween(200))
                ) {
                    SearchFilterCard(
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchViewModel.onSearchQueryChanged(it) },
                        minAmount = minAmount,
                        onMinAmountChange = { searchViewModel.onMinAmountChanged(it) },
                        maxAmount = maxAmount,
                        onMaxAmountChange = { searchViewModel.onMaxAmountChanged(it) },
                        onSearch = { searchViewModel.executeSearch() },
                        onClear = {
                            searchViewModel.clearSearch()
                            searchExpanded = false
                        },
                        isSearchActive = isSearchActive
                    )
                }
            }

            // ── Recent Transactions Panel — shows items directly ──
            item {
                AnimatedVisibility(
                    visible = recentExpanded,
                    enter = expandVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) + fadeIn(animationSpec = tween(300)),
                    exit = shrinkVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ) + fadeOut(animationSpec = tween(200))
                ) {
                    RecentTransactionsPanel(
                        expenses = recentExpensesList,
                        onExpenseClick = { expense ->
                            navController.navigate("expense_details/${expense.id}")
                        }
                    )
                }
            }

            // ── Main Content Area ──
            if (orderedProjects.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyProjectsState()
                    }
                }
            } else {
                if (isSearchActive) {
                    if (searchResults.isNotEmpty()) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Header
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(30.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.tertiaryContainer),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.Search,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp),
                                                    tint = MaterialTheme.colorScheme.tertiary
                                                )
                                            }
                                            Text(
                                                text = "Results",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                                        ) {
                                            Text(
                                                text = "${searchResults.size} found",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.tertiary,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                fontSize = 10.sp
                                            )
                                        }
                                    }

                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                        thickness = 0.5.dp
                                    )

                                    // Grid
                                    searchResults.chunked(2).forEach { rowItems ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            rowItems.forEach { expense ->
                                                SearchResultItemCard(
                                                    expense = expense,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                            if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 28.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("🔍", fontSize = 28.sp)
                                    Text(
                                        "No results found",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                } else {
                    itemsIndexed(
                        orderedProjects,
                        key = { _, item -> item.project.id }
                    ) { index, projectWithTotal ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            ProjectListItemWithReorder(
                                projectWithTotal = projectWithTotal,
                                currentIndex = index,
                                totalItems = orderedProjects.size,
                                onReorder = { fromIndex, toIndex ->
                                    val newOrderMap = mutableMapOf<Long, Int>()
                                    orderedProjects.forEachIndexed { idx, project ->
                                        newOrderMap[project.project.id] = when {
                                            idx == fromIndex -> toIndex
                                            idx in toIndex..<fromIndex -> idx + 1
                                            idx in (fromIndex + 1)..toIndex -> idx - 1
                                            else -> idx
                                        }
                                    }
                                    customOrderMap = newOrderMap
                                    with(sharedPrefs.edit()) {
                                        newOrderMap.forEach { (projectId, position) ->
                                            putInt("project_$projectId", position)
                                        }
                                        apply()
                                    }
                                },
                                onProjectClick = {
                                    viewModel.updateProject(
                                        projectWithTotal.project.copy(
                                            lastModified = System.currentTimeMillis()
                                        )
                                    )
                                    val newOrderMap = mutableMapOf<Long, Int>()
                                    newOrderMap[projectWithTotal.project.id] = 0
                                    orderedProjects.forEachIndexed { idx, project ->
                                        if (project.project.id != projectWithTotal.project.id) {
                                            newOrderMap[project.project.id] = idx + 1
                                        }
                                    }
                                    customOrderMap = newOrderMap
                                    with(sharedPrefs.edit()) {
                                        newOrderMap.forEach { (projectId, position) ->
                                            putInt("project_$projectId", position)
                                        }
                                        apply()
                                    }
                                    navController.navigate("project_details/${projectWithTotal.project.id}")
                                },
                                onEditClick = {
                                    projectToEdit = projectWithTotal
                                    currentSheetType = SheetType.EDIT
                                    showSheet = true
                                    scope.launch { sheetState.show() }
                                },
                                onDeleteClick = {
                                    projectToDelete = projectWithTotal
                                    currentSheetType = SheetType.DELETE
                                    showSheet = true
                                    scope.launch { sheetState.show() }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Bottom Sheet Logic
    if (showSheet && currentSheetType != null) {
        ModalBottomSheet(
            onDismissRequest = {
                showSheet = false
                currentSheetType = null
                projectToEdit = null
                projectToDelete = null
            },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            when (currentSheetType) {
                SheetType.ADD -> {
                    AddProjectSheetContent(
                        onCancel = {
                            showSheet = false
                            currentSheetType = null
                        },
                        onConfirm = { projectName, emoji ->
                            if (projectName.isNotBlank()) {
                                viewModel.insertProject(Project(name = projectName, emoji = emoji))
                                scope.launch { sheetState.hide() }.invokeOnCompletion {
                                    showSheet = false
                                    currentSheetType = null
                                }
                            }
                        }
                    )
                }
                SheetType.EDIT -> {
                    val editProject = projectToEdit
                    if (editProject != null) {
                        EditProjectSheetContent(
                            currentName = editProject.project.name,
                            currentEmoji = editProject.project.emoji,
                            onCancel = {
                                showSheet = false
                                currentSheetType = null
                                projectToEdit = null
                            },
                            onConfirm = { newName, newEmoji ->
                                viewModel.updateProject(
                                    editProject.project.copy(name = newName, emoji = newEmoji)
                                )
                                scope.launch { sheetState.hide() }.invokeOnCompletion {
                                    showSheet = false
                                    currentSheetType = null
                                    projectToEdit = null
                                }
                            }
                        )
                    }
                }
                SheetType.DELETE -> {
                    val deleteProject = projectToDelete
                    if (deleteProject != null) {
                        DeleteProjectSheetContent(
                            projectName = deleteProject.project.name,
                            onCancel = {
                                showSheet = false
                                currentSheetType = null
                                projectToDelete = null
                            },
                            onConfirm = {
                                viewModel.deleteProject(deleteProject.project)
                                scope.launch { sheetState.hide() }.invokeOnCompletion {
                                    showSheet = false
                                    currentSheetType = null
                                    projectToDelete = null
                                }
                            }
                        )
                    }
                }
                null -> {}
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Summary Expanded Card — full-width, rich detail, shown below the row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SummaryExpandedCard(
    totalSpent: Double,
    totalProjects: Int,
    totalCategories: Int,
    totalExpenses: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = "📊", fontSize = 18.sp)
                        }
                    }
                    Text(
                        text = "Overview",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f),
                    thickness = 1.dp
                )

                // Total spending — hero number
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Total Spending",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = formatCurrency(totalSpent),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 28.sp
                        )
                    }
                }

                // Stats row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SummaryStatTile(
                        emoji = "📁",
                        label = "Projects",
                        value = totalProjects.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    SummaryStatTile(
                        emoji = "🏷️",
                        label = "Categories",
                        value = totalCategories.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    SummaryStatTile(
                        emoji = "🧾",
                        label = "Expenses",
                        value = totalExpenses.toString(),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryStatTile(
    emoji: String,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = emoji, fontSize = 20.sp)
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 20.sp
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                fontSize = 10.sp
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Recent Transactions Panel — 2-column grid, same style as WeeklyDashboard
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun RecentTransactionsPanel(
    expenses: List<RecentExpense>,
    onExpenseClick: (RecentExpense) -> Unit
) {
    val displayExpenses = expenses.take(10)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ── Header ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🕐", fontSize = 14.sp)
                    }
                    Text(
                        text = "Recent",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (displayExpenses.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = "${displayExpenses.size} entries",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontSize = 10.sp
                        )
                    }
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                thickness = 0.5.dp
            )

            if (displayExpenses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("🧾", fontSize = 28.sp)
                        Text(
                            "No recent transactions",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // ── 2-column grid ──
                displayExpenses.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { expense ->
                            RecentGridTile(
                                expense = expense,
                                onClick = { onExpenseClick(expense) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentGridTile(
    expense: RecentExpense,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        tonalElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f),
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.08f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                // Top row: emoji + date pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = expense.categoryEmoji.ifBlank { "💸" },
                            fontSize = 15.sp
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                    ) {
                        Text(
                            text = SimpleDateFormat("d MMM", Locale.getDefault())
                                .format(Date(expense.date)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Description
                Text(
                    text = expense.description,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 12.sp
                )

                // Bottom row: category chip + amount
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(5.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Text(
                            text = expense.categoryName.ifBlank { "Other" },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            fontSize = 9.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formatCurrency(expense.amount),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Action Toggle Card — updated to support emoji label + navigation-only mode
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ActionToggleCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isExpanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emojiLabel: String? = null,
    isNavigation: Boolean = false
) {
    // Re-enabled your original rotation animation!
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "chevron_rotation"
    )

    Card(
        modifier = modifier
            .height(60.dp) // FIXED SIZE: Ensures all cards are perfectly uniform
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isExpanded) 4.dp else 0.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize() // Fills the 60.dp height
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            // Cute Left Icon / Emoji Container
            Surface(
                shape = CircleShape,
                color = if (isExpanded)
                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                else
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (emojiLabel != null) {
                        Text(
                            text = emojiLabel,
                            fontSize = 16.sp
                        )
                    } else {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            modifier = Modifier.size(16.dp),
                            tint = if (isExpanded)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Text with weight(1f) to push the trailing icon to the far right edge
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (isExpanded)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onPrimaryContainer,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            // Trailing Icon (Animated Dropdown OR Right Arrow)
            Box(
                modifier = Modifier.size(24.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isNavigation) {
                    // Right arrow for purely navigational cards (like Assets)
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Navigate to $title",
                        modifier = Modifier.size(20.dp),
                        tint = if (isExpanded)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                    )
                } else {
                    // Animated dropdown chevron for toggleable cards
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Toggle $title",
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(rotationAngle), // Applies the smooth 180-degree flip
                        tint = if (isExpanded)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Remaining composables — unchanged from original
// ─────────────────────────────────────────────────────────────────────────────

// ─────────────────────────────────────────────────────────────────────────────
// Search Result Grid Tile — compact, matches WeeklyDashboard grid style
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SearchResultItemCard(
    expense: RecentExpense,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = { /* Navigate to expense details */ },
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.15f),
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                // Top: emoji + date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = expense.categoryEmoji.ifBlank { "🔍" },
                            fontSize = 14.sp
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                    ) {
                        Text(
                            text = SimpleDateFormat("d MMM", Locale.getDefault())
                                .format(Date(expense.date)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Description
                Text(
                    text = expense.description,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 12.sp
                )

                // Bottom: category + amount
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(5.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Text(
                            text = expense.categoryName.ifBlank { "Other" },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            fontSize = 9.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formatCurrency(expense.amount),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun SearchFilterCard(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    minAmount: String,
    onMinAmountChange: (String) -> Unit,
    maxAmount: String,
    onMaxAmountChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    isSearchActive: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search descriptions...", fontSize = 14.sp) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (isSearchActive) {
                        IconButton(onClick = onClear, modifier = Modifier.size(24.dp)) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Clear",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                textStyle = androidx.compose.material3.LocalTextStyle.current.copy(fontSize = 14.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                )
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = minAmount,
                    onValueChange = onMinAmountChange,
                    placeholder = { Text("Min ₹", fontSize = 14.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    textStyle = androidx.compose.material3.LocalTextStyle.current.copy(fontSize = 14.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    )
                )
                OutlinedTextField(
                    value = maxAmount,
                    onValueChange = onMaxAmountChange,
                    placeholder = { Text("Max ₹", fontSize = 14.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    textStyle = androidx.compose.material3.LocalTextStyle.current.copy(fontSize = 14.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    )
                )
                Button(
                    onClick = onSearch,
                    modifier = Modifier.height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    Text("Apply", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun Header(onAddProjectClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        MaterialTheme.colorScheme.surface
                    ),
                    startY = 0f,
                    endY = 150f
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 8.dp,
                    shadowElevation = 4.dp,
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_project_icon_header),
                            contentDescription = null,
                            tint = androidx.compose.ui.graphics.Color.Unspecified,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "PaisaTracker",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 22.sp,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Track Your Projects",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )
                }
            }
            FloatingActionButton(
                onClick = onAddProjectClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.size(52.dp),
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 12.dp
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Project", modifier = Modifier.size(26.dp))
            }
        }
    }
}

@Composable
fun ProjectListItemWithReorder(
    projectWithTotal: ProjectWithTotal,
    currentIndex: Int,
    totalItems: Int,
    onReorder: (fromIndex: Int, toIndex: Int) -> Unit,
    onProjectClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var isAmountVisible by remember { mutableStateOf(false) }
    val canMoveUp = currentIndex > 0
    val canMoveDown = currentIndex < totalItems - 1
    val currency = CurrentCurrency.get()

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp, hoveredElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.1f),
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.05f)
                        )
                    )
                )
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = projectWithTotal.project.emoji, fontSize = 26.sp)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = projectWithTotal.project.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 17.sp,
                                lineHeight = 22.sp
                            )
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onProjectClick,
                            modifier = Modifier.height(40.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
                        ) {
                            Text(
                                text = "Explore",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                        IconButton(
                            onClick = { menuExpanded = !menuExpanded },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "More Options",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = menuExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            thickness = 1.dp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MenuActionButton(
                                icon = Icons.Default.Edit,
                                label = "Edit",
                                onClick = { onEditClick(); menuExpanded = false },
                                modifier = Modifier.weight(1f),
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                            MenuActionButton(
                                icon = Icons.Default.KeyboardArrowUp,
                                label = "To Top",
                                onClick = { if (canMoveUp) onReorder(currentIndex, 0); menuExpanded = false },
                                enabled = canMoveUp,
                                modifier = Modifier.weight(1f),
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                            MenuActionButton(
                                icon = Icons.Default.KeyboardArrowDown,
                                label = "To Bottom",
                                onClick = { if (canMoveDown) onReorder(currentIndex, totalItems - 1); menuExpanded = false },
                                enabled = canMoveDown,
                                modifier = Modifier.weight(1f),
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        }
                        MenuActionButton(
                            icon = Icons.Default.Delete,
                            label = "Delete Project",
                            onClick = { onDeleteClick(); menuExpanded = false },
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    }
                }

                AnimatedVisibility(
                    visible = !menuExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Spacer(modifier = Modifier.height(0.dp))
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            thickness = 0.5.dp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CompactStatBox(
                                label = "Categories",
                                value = "${projectWithTotal.categoryCount}",
                                modifier = Modifier.weight(1f)
                            )
                            CompactStatBox(
                                label = "Expenses",
                                value = "${projectWithTotal.expenseCount}",
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                DateChip(
                                    label = "Created",
                                    date = formatDateCompact(projectWithTotal.project.createdAt)
                                )
                                DateChip(
                                    label = "Updated",
                                    date = formatDateCompact(projectWithTotal.project.lastModified)
                                )
                            }
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            brush = Brush.linearGradient(
                                                colors = listOf(
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                                )
                                            )
                                        )
                                        .clickable { isAmountVisible = !isAmountVisible }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.End,
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = if (isAmountVisible)
                                                formatCurrency(projectWithTotal.totalAmount)
                                            else
                                                "${currency.symbol} ••••••",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 17.sp
                                        )
                                        Text(
                                            text = if (isAmountVisible) "Tap to hide" else "Tap to show",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                            fontSize = 9.sp
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
}

@Composable
private fun CompactStatBox(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontSize = 18.sp
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun DateChip(label: String, date: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            fontSize = 11.sp
        )
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Text(
                text = date,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                fontSize = 11.sp
            )
        }
    }
}

private fun formatDateCompact(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
fun AddProjectSheetContent(onCancel: () -> Unit, onConfirm: (String, String) -> Unit) {
    var projectName by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("📁") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            MaterialTheme.colorScheme.primaryContainer
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(text = selectedEmoji, style = MaterialTheme.typography.displaySmall, fontSize = 40.sp)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Create New Project", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Choose an emoji and name", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Select Emoji", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Card(
                modifier = Modifier.fillMaxWidth().height(120.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                LazyRow(
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(projectEmojis) { emoji ->
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (emoji == selectedEmoji) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                                .clickable { selectedEmoji = emoji },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = 24.sp)
                        }
                    }
                }
            }
        }
        OutlinedTextField(
            value = projectName,
            onValueChange = { projectName = it },
            label = { Text("Project Name") },
            placeholder = { Text("e.g., Home Renovation") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                focusedLabelColor = MaterialTheme.colorScheme.primary
            )
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Text("Cancel", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }
            Button(
                onClick = { onConfirm(projectName, selectedEmoji) },
                enabled = projectName.isNotBlank(),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 8.dp),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun EditProjectSheetContent(
    currentName: String,
    currentEmoji: String,
    onCancel: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var editedName by remember { mutableStateOf(currentName) }
    var selectedEmoji by remember { mutableStateOf(currentEmoji) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            MaterialTheme.colorScheme.primaryContainer
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(text = selectedEmoji, fontSize = 32.sp)
        }
        Text("Edit Project", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Update project details", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Select Emoji", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Card(
                modifier = Modifier.fillMaxWidth().height(100.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                LazyRow(
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(projectEmojis) { emoji ->
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(if (emoji == selectedEmoji) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                                .clickable { selectedEmoji = emoji },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = 22.sp)
                        }
                    }
                }
            }
        }
        OutlinedTextField(
            value = editedName,
            onValueChange = { editedName = it },
            label = { Text("Project Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                focusedLabelColor = MaterialTheme.colorScheme.primary
            )
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Text("Cancel", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }
            Button(
                onClick = { onConfirm(editedName, selectedEmoji) },
                enabled = editedName.isNotBlank() && (editedName != currentName || selectedEmoji != currentEmoji),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 6.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun DeleteProjectSheetContent(projectName: String, onCancel: () -> Unit, onConfirm: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                            MaterialTheme.colorScheme.errorContainer
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(32.dp))
        }
        Text("Delete Project?", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Are you sure you want to delete", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)) {
                Text(
                    text = "'$projectName'",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
            Text("This will permanently remove all categories and expenses.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                Text("Cancel", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text("Delete", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun EmptyProjectsState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Folder,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("No Projects Yet", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Create your first project to start\ntracking expenses",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun MenuActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (enabled) containerColor else containerColor.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) contentColor else contentColor.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) contentColor else contentColor.copy(alpha = 0.5f),
                fontSize = 13.sp
            )
        }
    }
}