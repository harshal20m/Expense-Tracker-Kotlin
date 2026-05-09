package com.example.paisatracker.ui.main.home

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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.paisatracker.PaisaTrackerApplication
import com.example.paisatracker.PaisaTrackerViewModel
import com.example.paisatracker.ui.assets.AssetsBottomSheet
import com.example.paisatracker.ui.export.ExportBottomSheet
import com.example.paisatracker.ui.common.HeaderActionButton
import com.example.paisatracker.ui.common.ScreenHeader
import com.example.paisatracker.ui.common.WeeklyDashboardCalendar
import com.example.paisatracker.ui.main.projects.*
import com.example.paisatracker.ui.quickadd.QuickAddSheet
import com.example.paisatracker.ui.search.SearchBottomSheet
import com.example.paisatracker.ui.search.SearchViewModel
import com.example.paisatracker.ui.search.SearchViewModelFactory
import com.example.paisatracker.ui.settings.UpdateRow
import com.example.paisatracker.ui.bin.BinSheetContent
import com.example.paisatracker.data.Project
import com.example.paisatracker.util.CurrentCurrency
import com.example.paisatracker.viewmodel.AnalyticsViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalCoroutinesApi::class)
@Composable
fun HomeScreen(viewModel: PaisaTrackerViewModel, navController: NavController) {
    val context = LocalContext.current
    val application = context.applicationContext as PaisaTrackerApplication
    val scope = rememberCoroutineScope()

    // ── Search ────────────────────────────────────────────────────────────────
    val searchViewModel: SearchViewModel = viewModel(
        factory = SearchViewModelFactory(application.repository, context)
    )

    // ── Analytics ─────────────────────────────────────────────────────────────
    val analyticsViewModel = remember { AnalyticsViewModel(application.repository) }

    // ── Currency ──────────────────────────────────────────────────────────────
    val currency by viewModel.currentCurrency.collectAsState()
    LaunchedEffect(currency) { CurrentCurrency.set(currency) }

    // ── Panel toggle state ────────────────────────────────────────────────────
    var recentExpanded by remember { mutableStateOf(false) }

    // ── Totals ────────────────────────────────────────────────────────────────
    val activeProjects by viewModel.getAllProjectsWithTotal().collectAsState(initial = emptyList())
    val totalSpent = activeProjects.sumOf { it.totalAmount }
    val totalCategories = activeProjects.sumOf { it.categoryCount }
    val totalExpenses = activeProjects.sumOf { it.expenseCount }
    val recentExpenses by viewModel.recentExpenses.collectAsState(initial = emptyList())
    val updateAvailable by viewModel.updateAvailable.collectAsState()

    // ── Sheet state ───────────────────────────────────────────────────────────
    var showAssetsSheet by remember { mutableStateOf(false) }
    var showQuickAdd by remember { mutableStateOf(false) }
    var showBin by remember { mutableStateOf(false) }
    var showDataManagement by remember { mutableStateOf(false) }
    var showSearchSheet by remember { mutableStateOf(false) }
    var showSummarySheet by remember { mutableStateOf(false) }
    val quickAddState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val binSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val searchSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val summarySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // ── Sheet for Add Project ────────────────────────────────────────────────
    var showAddProjectSheet by remember { mutableStateOf(false) }
    val addProjectSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (showAssetsSheet) {
        AssetsBottomSheet(viewModel = viewModel, onDismiss = { showAssetsSheet = false })
    }

    if (showDataManagement) {
        ExportBottomSheet(viewModel = viewModel, navController = navController, onDismiss = { showDataManagement = false })
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
                    .padding(bottom = 96.dp) // Adjusted to be above bottom nav (84dp + 12dp)
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
                title = "PaisaTracker",
                subtitle = "Expense Tracker Dashboard",
                painter = painterResource(
                    id = com.example.paisatracker.R.drawable.expenses_5501391
                ),
                action = {
                    HeaderActionButton(
                        icon = Icons.Default.Add,
                        label = "Add Expense",
                        onClick = {
                            showQuickAdd = true
                            scope.launch { quickAddState.show() }
                        },
                        contentDescription = "Quick Add",
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                }
            )

            LazyColumn(
                state = rememberLazyListState(),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 0.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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

                // Search Card - Opens bottom sheet
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        onClick = {
                            showSearchSheet = true
                            scope.launch { searchSheetState.show() }
                        },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Search expenses...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        WeeklyDashboardCalendar(
                            expenses = recentExpenses,
                            onTransactionClick = { navController.navigate("expense_details/$it") }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (activeProjects.isEmpty()) {
                            DummyCreateProjectCard(onClick = {
                                showAddProjectSheet = true
                                scope.launch { addProjectSheetState.show() }
                            })
                        }

                        HomeActionGrid(
                            onSummaryClick = {
                                showSummarySheet = true
                                scope.launch { summarySheetState.show() }
                            },
                            onAssetsClick = { showAssetsSheet = true },
                            onBinClick = {
                                showBin = true
                                scope.launch { binSheetState.show() }
                            }
                        )

                        // Analytics Preview Card
                        Spacer(modifier = Modifier.height(8.dp))
                        AnalyticsPreviewCard(
                            viewModel = analyticsViewModel,
                            onViewFullAnalytics = { navController.navigate("analytics") },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        RecentTransactionsSlider(
                            expenses = recentExpenses,
                            onExpenseClick = { navController.navigate("expense_details/${it.id}") },
                            onMoreClick = {
                                recentExpanded = !recentExpanded
                            },
                            showMore = recentExpanded,
                            onLoadMore = { viewModel.loadMoreRecentExpenses() }
                        )
}
                }
            }
        }
    }

    if (showAddProjectSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddProjectSheet = false },
            sheetState = addProjectSheetState,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            AddProjectSheetContent(
                viewModel = viewModel,
                onCancel = { showAddProjectSheet = false },
                onConfirm = { name, emoji, includeInSalary ->
                    if (name.isNotBlank()) {
                        viewModel.insertProject(Project(name = name, emoji = emoji, includeInSalary = includeInSalary))
                        scope.launch { addProjectSheetState.hide() }.invokeOnCompletion {
                            showAddProjectSheet = false
                        }
                    }
                }
            )
        }
    }

    if (showQuickAdd) {
        ModalBottomSheet(
            onDismissRequest = { showQuickAdd = false },
            sheetState = quickAddState,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            QuickAddSheet(
                onDismiss = {
                    scope.launch { quickAddState.hide() }.invokeOnCompletion { showQuickAdd = false }
                },
                viewModel = viewModel,
                currencySymbol = currency.symbol
            )
        }
    }

    if (showBin) {
        ModalBottomSheet(
            onDismissRequest = { showBin = false },
            sheetState = binSheetState,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = { BottomSheetDefaults.DragHandle() },
            modifier = Modifier.fillMaxHeight()
        ) {
            BinSheetContent(
                viewModel = viewModel,
                onDismiss = {
                    scope.launch { binSheetState.hide() }.invokeOnCompletion { showBin = false }
                }
            )
        }
    }

    if (showSearchSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSearchSheet = false },
            sheetState = searchSheetState,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            SearchBottomSheet(
                searchViewModel = searchViewModel,
                onDismiss = {
                    scope.launch { searchSheetState.hide() }.invokeOnCompletion { showSearchSheet = false }
                },
                onExpenseClick = { expense ->
                    navController.navigate("expense_details/${expense.id}")
                    scope.launch { searchSheetState.hide() }.invokeOnCompletion { showSearchSheet = false }
                }
            )
        }
    }

    if (showSummarySheet) {
        ModalBottomSheet(
            onDismissRequest = { showSummarySheet = false },
            sheetState = summarySheetState,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            SummaryBottomSheet(
                analyticsViewModel = analyticsViewModel,
                totalSpent = totalSpent,
                totalProjects = activeProjects.size,
                totalCategories = totalCategories,
                totalExpenses = totalExpenses,
                onDismiss = {
                    scope.launch { summarySheetState.hide() }.invokeOnCompletion { showSummarySheet = false }
                }
            )
        }
    }
}


@Composable
private fun HomeActionGrid(
    onSummaryClick: () -> Unit,
    onAssetsClick: () -> Unit,
    onBinClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = 500.dp)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Summary - Takes full height on left
            OutlinedCard(
                onClick = onSummaryClick,
                modifier = Modifier
                    .weight(1f)
                    .height(116.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                ),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("📊", fontSize = 20.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Summary",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            // Gallery and Bin - Stacked on right
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Gallery
                OutlinedCard(
                    onClick = onAssetsClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(53.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                    ),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Collections,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                        Text(
                            "Gallery",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                
                // Bin
                OutlinedCard(
                    onClick = onBinClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(53.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                    ),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.DeleteSweep,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                        Text(
                            "Bin",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
