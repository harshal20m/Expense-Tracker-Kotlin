package com.example.paisatracker.ui.main.projects

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.paisatracker.PaisaTrackerViewModel
import com.example.paisatracker.data.Project
import com.example.paisatracker.data.ProjectWithTotal
import com.example.paisatracker.ui.assets.AssetsBottomSheet
import com.example.paisatracker.ui.common.HeaderActionButton
import com.example.paisatracker.ui.common.ScreenHeader
import com.example.paisatracker.ui.quickadd.QuickAddSheet
import com.example.paisatracker.util.CurrentCurrency
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch

private enum class SheetType { ADD, EDIT, DELETE }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalCoroutinesApi::class)
@Composable
fun ProjectListScreen(viewModel: PaisaTrackerViewModel, navController: NavController) {
    val context     = LocalContext.current
    val scope       = rememberCoroutineScope()

    // ── Currency ──────────────────────────────────────────────────────────────
    val currency by viewModel.currentCurrency.collectAsState()
    LaunchedEffect(currency) { CurrentCurrency.set(currency) }

    // ── Projects + custom ordering ────────────────────────────────────────────
    var showCompleted by remember { mutableStateOf(false) }
    val activeProjects by viewModel.getAllProjectsWithTotal().collectAsState(initial = emptyList())
    val completedProjects by viewModel.getCompletedProjectsWithTotal().collectAsState(initial = emptyList())
    
    val projects = if (showCompleted) completedProjects else activeProjects
    val sharedPrefs = remember {
        context.getSharedPreferences("project_order", Context.MODE_PRIVATE)
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

    // ── Sheet state ───────────────────────────────────────────────────────────
    var currentSheetType by remember { mutableStateOf<SheetType?>(null) }
    var projectToEdit    by remember { mutableStateOf<ProjectWithTotal?>(null) }
    var projectToDelete  by remember { mutableStateOf<ProjectWithTotal?>(null) }
    val sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSheet        by remember { mutableStateOf(false) }
    var showAssetsSheet  by remember { mutableStateOf(false) }
    var showQuickAdd     by remember { mutableStateOf(false) }
    var showBin          by remember { mutableStateOf(false) }
    val quickAddState    = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val binSheetState    = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // ── UI ────────────────────────────────────────────────────────────────────
    if (showAssetsSheet) {
        AssetsBottomSheet(viewModel = viewModel, onDismiss = { showAssetsSheet = false })
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    showQuickAdd = true
                    scope.launch { quickAddState.show() }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .padding(bottom = 96.dp)
                    .size(56.dp)
            ) {
                Icon(Icons.Default.FlashOn, contentDescription = "Quick Add")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            ScreenHeader(
                title = "Projects",
                subtitle = if (activeProjects.isNotEmpty()) "${activeProjects.size} active" else "Track your goals",
                icon = Icons.Default.Folder,
                action = {
                    HeaderActionButton(
                        icon = Icons.Default.Add,
                        label = "Create Project",
                        onClick = {
                            currentSheetType = SheetType.ADD
                            projectToEdit = null
                            projectToDelete = null
                            showSheet = true
                            scope.launch { sheetState.show() }
                        },
                        contentDescription = "Add Project",
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                }
            )

            LazyColumn(
                state               = listState,
                modifier            = Modifier.fillMaxSize(),
                contentPadding      = PaddingValues(top = 0.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // ── Tabs (Browser-like) ───────────────────────────────────────────
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        BrowserTab(
                            text = "Active",
                            count = activeProjects.size,
                            isSelected = !showCompleted,
                            onClick = { showCompleted = false },
                            modifier = Modifier.weight(1f)
                        )
                        BrowserTab(
                            text = "Completed",
                            count = completedProjects.size,
                            isSelected = showCompleted,
                            onClick = { showCompleted = true },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // ── Project list ─────────────────────────────────
                if (orderedProjects.isEmpty()) {
                    item {
                        Box(
                            modifier         = Modifier.fillMaxWidth().height(400.dp),
                            contentAlignment = Alignment.Center
                        ) { EmptyProjectsState() }
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

    // ── Bin sheet ─────────────────────────────────────────────────────────────
    if (showBin) {
        ModalBottomSheet(
            onDismissRequest = { showBin = false },
            sheetState       = binSheetState,
            shape            = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle       = { BottomSheetDefaults.DragHandle() },
            modifier         = Modifier.fillMaxHeight()
        ) {
            com.example.paisatracker.ui.bin.BinSheetContent(
                viewModel = viewModel,
                onDismiss = {
                    scope.launch { binSheetState.hide() }.invokeOnCompletion { showBin = false }
                }
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
                    onConfirm = { name, emoji, includeInSalary ->
                        if (name.isNotBlank()) {
                            viewModel.insertProject(Project(name = name, emoji = emoji, includeInSalary = includeInSalary))
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
                        currentIncludeInSalary = pwt.project.includeInSalary,
                        viewModel    = viewModel,
                        onCancel     = { showSheet = false; currentSheetType = null; projectToEdit = null },
                        onConfirm    = { name, emoji, includeInSalary ->
                            viewModel.updateProject(pwt.project.copy(
                                name = name,
                                emoji = emoji,
                                includeInSalary = includeInSalary
                            ))
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

@Composable
fun BrowserTab(
    text: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(6.dp))
            Surface(
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                shape = CircleShape
            ) {
                Text(
                    text = count.toString(),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
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
    onAssetsClick:   () -> Unit,
    onBinClick:      () -> Unit
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                ActionToggleCard(
                    title        = "Assets",
                    icon         = Icons.Default.Image,
                    isExpanded   = false,
                    onClick      = onAssetsClick,
                    modifier     = Modifier.weight(1f),
                    isNavigation = true
                )
            }
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionToggleCard(
                    title      = "Recent",
                    icon       = Icons.Default.DateRange,
                    isExpanded = recentExpanded,
                    onClick    = onRecentClick,
                    modifier   = Modifier.weight(2f)
                )
                ActionToggleCard(
                    title        = "Bin",
                    icon         = Icons.Default.DeleteSweep,
                    isExpanded   = false,
                    onClick      = onBinClick,
                    modifier     = Modifier.weight(1f),
                    isNavigation = true
                )
            }
        }
    }
}
