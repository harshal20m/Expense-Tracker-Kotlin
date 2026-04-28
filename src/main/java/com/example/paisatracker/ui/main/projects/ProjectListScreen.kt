package com.example.paisatracker.ui.main.projects

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.paisatracker.PaisaTrackerApplication
import com.example.paisatracker.PaisaTrackerViewModel
import com.example.paisatracker.data.Project
import com.example.paisatracker.data.ProjectWithTotal
import com.example.paisatracker.ui.assets.AssetsBottomSheet
import com.example.paisatracker.ui.common.WeeklyDashboardCalendar
import com.example.paisatracker.ui.quickadd.QuickAddSheet
import com.example.paisatracker.ui.search.SearchViewModel
import com.example.paisatracker.ui.search.SearchViewModelFactory
import com.example.paisatracker.ui.settings.UpdateRow
import com.example.paisatracker.util.CurrentCurrency
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch

private enum class SheetType { ADD, EDIT, DELETE }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalCoroutinesApi::class)
@Composable
fun ProjectListScreen(viewModel: PaisaTrackerViewModel, navController: NavController) {
    val context     = LocalContext.current
    val application = context.applicationContext as PaisaTrackerApplication
    val scope       = rememberCoroutineScope()

    // ── Search ────────────────────────────────────────────────────────────────
    val searchViewModel: SearchViewModel = viewModel(
        factory = SearchViewModelFactory(application.repository)
    )
    val searchQuery    by searchViewModel.searchQuery.collectAsState()
    val minAmount      by searchViewModel.minAmount.collectAsState()
    val maxAmount      by searchViewModel.maxAmount.collectAsState()
    val searchResults  by searchViewModel.searchResults.collectAsState()
    val isSearchActive by searchViewModel.isSearchActive.collectAsState()

    // ── Currency ──────────────────────────────────────────────────────────────
    val currency by viewModel.currentCurrency.collectAsState()
    LaunchedEffect(currency) { CurrentCurrency.set(currency) }

    // ── Panel toggle state ────────────────────────────────────────────────────
    var searchExpanded  by remember { mutableStateOf(false) }
    var recentExpanded  by remember { mutableStateOf(false) }
    var summaryExpanded by remember { mutableStateOf(false) }

    // ── Projects + custom ordering ────────────────────────────────────────────
    var showCompleted by remember { mutableStateOf(false) }
    val activeProjects by viewModel.getAllProjectsWithTotal().collectAsState(initial = emptyList())
    val completedProjects by viewModel.getCompletedProjectsWithTotal().collectAsState(initial = emptyList())
    
    val projects = if (showCompleted) completedProjects else activeProjects
    val sharedPrefs = remember {
        context.getSharedPreferences("project_order", android.content.Context.MODE_PRIVATE)
    }
    var customOrderMap by remember { mutableStateOf<Map<Long, Int>>(emptyMap()) }

    if (projects.isNotEmpty() && customOrderMap.isEmpty()) {
        val savedOrder = mutableMapOf<Long, Int>()
        projects.forEach { p ->
            val pos = sharedPrefs.getInt("project_${p.project.id}", -1)
            if (pos >= 0) savedOrder[p.project.id] = pos
        }
        customOrderMap = savedOrder.ifEmpty {
            projects.sortedByDescending { it.project.lastModified }
                .mapIndexed { i, p -> p.project.id to i }
                .toMap()
                .also { map ->
                    with(sharedPrefs.edit()) {
                        map.forEach { (id, pos) -> putInt("project_$id", pos) }
                        apply()
                    }
                }
        }
    }

    val orderedProjects = remember(projects, customOrderMap) {
        if (customOrderMap.isEmpty()) projects.sortedByDescending { it.project.lastModified }
        else projects.sortedBy { customOrderMap[it.project.id] ?: Int.MAX_VALUE }
    }

    // ── Scroll state ──────────────────────────────────────────────────────────
    val listState     = rememberLazyListState()
    val labelsVisible by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 200
        }
    }

    // ── Sheet state ───────────────────────────────────────────────────────────
    var currentSheetType by remember { mutableStateOf<SheetType?>(null) }
    var projectToEdit    by remember { mutableStateOf<ProjectWithTotal?>(null) }
    var projectToDelete  by remember { mutableStateOf<ProjectWithTotal?>(null) }
    val sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSheet        by remember { mutableStateOf(false) }
    var showAssetsSheet  by remember { mutableStateOf(false) }
    var showQuickAdd     by remember { mutableStateOf(false) }
    val quickAddState    = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // ── Totals ────────────────────────────────────────────────────────────────
    val totalSpent      = orderedProjects.sumOf { it.totalAmount }
    val totalCategories = orderedProjects.sumOf { it.categoryCount }
    val totalExpenses   = orderedProjects.sumOf { it.expenseCount }
    val recentExpenses  by viewModel.recentExpenses.collectAsState(initial = emptyList())
    val updateAvailable by viewModel.updateAvailable.collectAsState()

    if (showAssetsSheet) {
        AssetsBottomSheet(viewModel = viewModel, onDismiss = { showAssetsSheet = false })
    }

    // ── UI ────────────────────────────────────────────────────────────────────
    Column(modifier = Modifier.fillMaxSize()) {
        ProjectListHeader(
            onAddProjectClick = {
                currentSheetType = SheetType.ADD
                projectToEdit    = null
                projectToDelete  = null
                showSheet        = true
                scope.launch { sheetState.show() }
            },
            onQuickAddClick = {
                showQuickAdd = true
                scope.launch { quickAddState.show() }
            },
            labelsVisible = labelsVisible
        )

        LazyColumn(
            state               = listState,
            modifier            = Modifier.fillMaxSize(),
            contentPadding      = PaddingValues(top = 0.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ── Update Notification ───────────────────────────────────────────
            item {
                AnimatedVisibility(
                    visible = updateAvailable != null,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    updateAvailable?.let { release ->
                        UpdateRow(
                            tagName = release.tag_name,
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(release.html_url))
                                context.startActivity(intent)
                            },
                            onDismiss = { viewModel.dismissUpdate() }
                        )
                    }
                }
            }

            // ── Dashboard Components (Calendar, Actions, Panels, Filter) ──────
            item {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    WeeklyDashboardCalendar(
                        expenses           = recentExpenses,
                        onTransactionClick = { navController.navigate("expense_details/$it") }
                    )

                    ProjectActionGrid(
                        summaryExpanded = summaryExpanded,
                        searchExpanded  = searchExpanded,
                        recentExpanded  = recentExpanded,
                        onSummaryClick  = {
                            summaryExpanded = !summaryExpanded
                            if (summaryExpanded) { searchExpanded = false; recentExpanded = false }
                        },
                        onSearchClick   = {
                            searchExpanded = !searchExpanded
                            if (searchExpanded) { summaryExpanded = false; recentExpanded = false }
                        },
                        onRecentClick   = {
                            recentExpanded = !recentExpanded
                            if (recentExpanded) { summaryExpanded = false; searchExpanded = false }
                        },
                        onAssetsClick   = { showAssetsSheet = true }
                    )

                    // ── Summary panel ─────────────────────────────────────────────────
                    AnimatedVisibility(
                        visible = summaryExpanded && orderedProjects.isNotEmpty(),
                        enter   = expandVertically(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow)) + fadeIn(tween(300)),
                        exit    = shrinkVertically(spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium)) + fadeOut(tween(200))
                    ) {
                        SummaryExpandedCard(
                            totalSpent      = totalSpent,
                            totalProjects   = orderedProjects.size,
                            totalCategories = totalCategories,
                            totalExpenses   = totalExpenses
                        )
                    }

                    // ── Search panel ──────────────────────────────────────────────────
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

                    // ── Recent panel ──────────────────────────────────────────────────
                    AnimatedVisibility(
                        visible = recentExpanded,
                        enter   = expandVertically(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow)) + fadeIn(tween(300)),
                        exit    = shrinkVertically(spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium)) + fadeOut(tween(200))
                    ) {
                        RecentTransactionsPanel(
                            expenses       = recentExpenses,
                            onExpenseClick = { navController.navigate("expense_details/${it.id}") }
                        )
                    }

                    if (activeProjects.isNotEmpty() || completedProjects.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 8.dp, top = 2.dp, bottom = 0.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (showCompleted) "Completed Projects (${completedProjects.size})" else "Active Projects (${activeProjects.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            if (completedProjects.isNotEmpty() || showCompleted) {
                                TextButton(onClick = { showCompleted = !showCompleted }) {
                                    Text(if (showCompleted) "Show Active" else "Show Completed")
                                }
                            }
                        }
                    }
                }
            }

            // ── Project list / search results ─────────────────────────────────
            if (orderedProjects.isEmpty()) {
                item {
                    Box(
                        modifier         = Modifier.fillMaxWidth().height(400.dp),
                        contentAlignment = Alignment.Center
                    ) { EmptyProjectsState() }
                }
            } else if (isSearchActive) {
                item {
                    SearchResultsCard(results = searchResults, isActive = isSearchActive)
                }
            } else {
                itemsIndexed(orderedProjects, key = { _, it -> it.project.id }) { index, pwt ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        ProjectListItemWithReorder(
                            projectWithTotal = pwt,
                            currentIndex     = index,
                            totalItems       = orderedProjects.size,
                            onReorder        = { from, to ->
                                val newMap = mutableMapOf<Long, Int>()
                                orderedProjects.forEachIndexed { i, p ->
                                    newMap[p.project.id] = when {
                                        i == from            -> to
                                        i in to..<from       -> i + 1
                                        i in (from + 1)..to  -> i - 1
                                        else                 -> i
                                    }
                                }
                                customOrderMap = newMap
                                with(sharedPrefs.edit()) {
                                    newMap.forEach { (id, pos) -> putInt("project_$id", pos) }
                                    apply()
                                }
                            },
                            onProjectClick   = {
                                viewModel.updateProject(pwt.project.copy(lastModified = System.currentTimeMillis()), notify = false)
                                val newMap = mutableMapOf<Long, Int>()
                                newMap[pwt.project.id] = 0
                                orderedProjects.forEachIndexed { i, p ->
                                    if (p.project.id != pwt.project.id) newMap[p.project.id] = i + 1
                                }
                                customOrderMap = newMap
                                with(sharedPrefs.edit()) {
                                    newMap.forEach { (id, pos) -> putInt("project_$id", pos) }
                                    apply()
                                }
                                navController.navigate("project_details/${pwt.project.id}")
                            },
                            onEditClick      = {
                                projectToEdit    = pwt
                                currentSheetType = SheetType.EDIT
                                showSheet        = true
                                scope.launch { sheetState.show() }
                            },
                            onDeleteClick    = {
                                projectToDelete  = pwt
                                currentSheetType = SheetType.DELETE
                                showSheet        = true
                                scope.launch { sheetState.show() }
                            },
                            onCompleteToggleClick = {
                                viewModel.updateProjectStatus(
                                    pwt.project.id,
                                    !pwt.project.isCompleted,
                                    pwt.project.name
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    // ── Quick Add sheet ───────────────────────────────────────────────────────
    if (showQuickAdd) {
        ModalBottomSheet(
            onDismissRequest = { showQuickAdd = false },
            sheetState       = quickAddState,
            shape            = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle       = { BottomSheetDefaults.DragHandle() }
        ) {
            QuickAddSheet(
                onDismiss      = {
                    scope.launch { quickAddState.hide() }.invokeOnCompletion { showQuickAdd = false }
                },
                viewModel      = viewModel,
                currencySymbol = currency.symbol
            )
        }
    }

    // ── Project sheets ────────────────────────────────────────────────────────
    if (showSheet && currentSheetType != null) {
        ModalBottomSheet(
            onDismissRequest = {
                showSheet = false; currentSheetType = null
                projectToEdit = null; projectToDelete = null
            },
            sheetState = sheetState,
            shape      = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            when (currentSheetType) {
                SheetType.ADD -> AddProjectSheetContent(
                    viewModel = viewModel,
                    onCancel  = { showSheet = false; currentSheetType = null },
                    onConfirm = { name, emoji ->
                        if (name.isNotBlank()) {
                            viewModel.insertProject(Project(name = name, emoji = emoji))
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                showSheet = false; currentSheetType = null
                            }
                        }
                    }
                )
                SheetType.EDIT -> projectToEdit?.let { pwt ->
                    EditProjectSheetContent(
                        currentName  = pwt.project.name,
                        currentEmoji = pwt.project.emoji,
                        viewModel    = viewModel,
                        onCancel     = { showSheet = false; currentSheetType = null; projectToEdit = null },
                        onConfirm    = { name, emoji ->
                            viewModel.updateProject(pwt.project.copy(name = name, emoji = emoji))
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                showSheet = false; currentSheetType = null; projectToEdit = null
                            }
                        }
                    )
                }
                SheetType.DELETE -> projectToDelete?.let { pwt ->
                    DeleteProjectSheetContent(
                        projectName  = pwt.project.name,
                        projectEmoji = pwt.project.emoji,
                        onCancel     = { showSheet = false; currentSheetType = null; projectToDelete = null },
                        onConfirm    = {
                            viewModel.deleteProject(pwt.project)
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                showSheet = false; currentSheetType = null; projectToDelete = null
                            }
                        }
                    )
                }
                null -> {}
            }
        }
    }
}

// ── Original 2×2 toggle grid ──────────────────────────────────────────────────

@Composable
private fun ProjectActionGrid(
    summaryExpanded: Boolean,
    searchExpanded:  Boolean,
    recentExpanded:  Boolean,
    onSummaryClick:  () -> Unit,
    onSearchClick:   () -> Unit,
    onRecentClick:   () -> Unit,
    onAssetsClick:   () -> Unit
) {
    androidx.compose.foundation.layout.Box(
        modifier         = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier            = Modifier
                .widthIn(max = 500.dp)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ActionToggleCard(
                    title      = "Summary",
                    icon       = Icons.Default.KeyboardArrowDown,
                    isExpanded = summaryExpanded,
                    onClick    = onSummaryClick,
                    modifier   = Modifier.weight(1f),
                    emojiLabel = "📊"
                )
                ActionToggleCard(
                    title      = "Search",
                    icon       = Icons.Default.Search,
                    isExpanded = searchExpanded,
                    onClick    = onSearchClick,
                    modifier   = Modifier.weight(1f)
                )
            }
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ActionToggleCard(
                    title      = "Recent",
                    icon       = Icons.Default.DateRange,
                    isExpanded = recentExpanded,
                    onClick    = onRecentClick,
                    modifier   = Modifier.weight(1f)
                )
                ActionToggleCard(
                    title        = "Assets",
                    icon         = Icons.Default.Image,
                    isExpanded   = false,
                    onClick      = onAssetsClick,
                    modifier     = Modifier.weight(1f),
                    isNavigation = true
                )
            }
        }
    }
}