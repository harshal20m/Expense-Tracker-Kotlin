package com.example.paisatracker.ui.main

import com.example.paisatracker.ui.recent.RecentExpensesSection
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.paisatracker.PaisaTrackerViewModel
import com.example.paisatracker.R
import com.example.paisatracker.data.Project
import com.example.paisatracker.data.ProjectWithTotal
import com.example.paisatracker.util.formatCurrency
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// Emoji List (same as before)
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

private enum class SheetType {
    ADD, EDIT, DELETE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListScreen(viewModel: PaisaTrackerViewModel, navController: NavController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val projects by viewModel.getAllProjectsWithTotal().collectAsState(initial = emptyList())
    var currentSheetType by remember { mutableStateOf<SheetType?>(null) }
    var projectToEdit by remember { mutableStateOf<ProjectWithTotal?>(null) }
    var projectToDelete by remember { mutableStateOf<ProjectWithTotal?>(null) }

    var showSummary by remember { mutableStateOf(false) }
    var summaryAtTop by remember { mutableStateOf(true) }

    // Persistent storage for project order
    val sharedPrefs = remember {
        context.getSharedPreferences("project_order", android.content.Context.MODE_PRIVATE)
    }

    var customOrderMap by remember { mutableStateOf<Map<Long, Int>>(emptyMap()) }

    // Load saved order on first composition
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
            // Initialize with default order (most recent first based on lastModified)
            customOrderMap = projects
                .sortedByDescending { it.project.lastModified }
                .mapIndexed { index, project ->
                    project.project.id to index
                }.toMap()

            // Save initial order
            with(sharedPrefs.edit()) {
                customOrderMap.forEach { (projectId, position) ->
                    putInt("project_$projectId", position)
                }
                apply()
            }
        }
    }

    // Apply custom order to projects
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
    val scope = rememberCoroutineScope()

    val totalSpent = orderedProjects.sumOf { it.totalAmount }
    val totalCategories = orderedProjects.sumOf { it.categoryCount }
    val totalExpenses = orderedProjects.sumOf { it.expenseCount }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Header
        Header(
            onAddProjectClick = {
                currentSheetType = SheetType.ADD
                projectToEdit = null
                projectToDelete = null
                showSheet = true
                scope.launch { sheetState.show() }
            }
        )

        // Content with conditional summary position
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Summary at top - only show if projects exist
                if (summaryAtTop && orderedProjects.isNotEmpty()) {
                    CollapsibleSummary(
                        totalSpent = totalSpent,
                        totalProjects = orderedProjects.size,
                        totalCategories = totalCategories,
                        totalExpenses = totalExpenses,
                        isExpanded = showSummary,
                        onToggleExpand = { showSummary = !showSummary },
                        onSwapPosition = { summaryAtTop = false }
                    )
                }

                this@Column.AnimatedVisibility(
                    visible = orderedProjects.isEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    EmptyProjectsState()
                }

                this@Column.AnimatedVisibility(
                    visible = orderedProjects.isNotEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 16.dp,
                            bottom = if (!summaryAtTop && orderedProjects.isNotEmpty()) 200.dp else 110.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Recent Expenses Section at the top
                        item {
                            RecentExpensesSection(viewModel = viewModel)
                        }

                        // Project items
                        itemsIndexed(orderedProjects, key = { _, item -> item.project.id }) { index, projectWithTotal ->
                            ProjectListItemWithReorder(
                                projectWithTotal = projectWithTotal,
                                currentIndex = index,
                                totalItems = orderedProjects.size,
                                onReorder = { fromIndex, toIndex ->
                                    // Update custom order map
                                    val newOrderMap = mutableMapOf<Long, Int>()
                                    orderedProjects.forEachIndexed { idx, project ->
                                        newOrderMap[project.project.id] = when {
                                            idx == fromIndex -> toIndex
                                            idx < fromIndex && idx >= toIndex -> idx + 1
                                            idx > fromIndex && idx <= toIndex -> idx - 1
                                            else -> idx
                                        }
                                    }
                                    customOrderMap = newOrderMap

                                    // Save order to SharedPreferences
                                    with(sharedPrefs.edit()) {
                                        newOrderMap.forEach { (projectId, position) ->
                                            putInt("project_$projectId", position)
                                        }
                                        apply()
                                    }
                                },
                                onProjectClick = {
                                    // Update lastModified timestamp to mark as recently used
                                    viewModel.updateProject(
                                        projectWithTotal.project.copy(
                                            lastModified = System.currentTimeMillis()
                                        )
                                    )

                                    // Move to top of list
                                    val newOrderMap = mutableMapOf<Long, Int>()
                                    newOrderMap[projectWithTotal.project.id] = 0
                                    orderedProjects.forEachIndexed { idx, project ->
                                        if (project.project.id != projectWithTotal.project.id) {
                                            newOrderMap[project.project.id] = idx + 1
                                        }
                                    }
                                    customOrderMap = newOrderMap

                                    // Save new order
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

                // Summary at bottom - only show if projects exist
                if (!summaryAtTop && orderedProjects.isNotEmpty()) {
                    CollapsibleSummary(
                        totalSpent = totalSpent,
                        totalProjects = orderedProjects.size,
                        totalCategories = totalCategories,
                        totalExpenses = totalExpenses,
                        isExpanded = showSummary,
                        onToggleExpand = { showSummary = !showSummary },
                        onSwapPosition = { summaryAtTop = true }
                    )
                }
            }
        }
    }

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
                                    editProject.project.copy(
                                        name = newName,
                                        emoji = newEmoji
                                    )
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
                null -> {
                    // Optional: empty content when currentSheetType is null
                }
            }
        }
    }
}

@Composable
private fun CollapsibleSummary(
    totalSpent: Double,
    totalProjects: Int,
    totalCategories: Int,
    totalExpenses: Int,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onSwapPosition: () -> Unit
) {
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "arrow rotation"
    )

    // Drag state for position swapping
    var isDragging by remember { mutableStateOf(false) }
    var dragOffsetY by remember { mutableStateOf(0f) }

    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.02f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "drag scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(if (isDragging) 8.dp else 4.dp, RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        isDragging = false
                        dragOffsetY = 0f
                    },
                    onDrag = { change, dragAmount ->
                        // Activate drag after threshold (long press detection)
                        if (!isDragging && kotlin.math.abs(dragOffsetY + dragAmount.y) > 50f) {
                            isDragging = true
                        }

                        if (isDragging) {
                            change.consume()
                            dragOffsetY += dragAmount.y
                        }
                    },
                    onDragEnd = {
                        // If dragged significantly, swap position
                        if (isDragging && kotlin.math.abs(dragOffsetY) > 100f) {
                            onSwapPosition()
                        }
                        isDragging = false
                        dragOffsetY = 0f
                    },
                    onDragCancel = {
                        isDragging = false
                        dragOffsetY = 0f
                    }
                )
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDragging)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
            else
                MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                        )
                    )
                )
        ) {
            // Header - Always visible
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isDragging) { onToggleExpand() }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
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
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "📊",
                                fontSize = 18.sp
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "Summary",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        if (!isExpanded) {
                            Text(
                                text = formatCurrency(totalSpent),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Only expand/collapse button
                IconButton(
                    onClick = onToggleExpand,
                    modifier = Modifier.size(32.dp),
                    enabled = !isDragging
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(rotationAngle)
                    )
                }
            }

            // Expanded content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    androidx.compose.material3.HorizontalDivider(
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
                        thickness = 1.dp
                    )

                    // Total Spent - Prominent
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Total Spending",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                            Text(
                                text = formatCurrency(totalSpent),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 24.sp
                            )
                        }
                    }

                    // Stats Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            label = "Projects",
                            value = totalProjects.toString(),
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "Categories",
                            value = totalCategories.toString(),
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "Expenses",
                            value = totalExpenses.toString(),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                fontSize = 11.sp
            )
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
                .padding(horizontal = 24.dp, vertical = 28.dp),
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
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_project_icon_header),
                            contentDescription = null,
                            tint = androidx.compose.ui.graphics.Color.Unspecified,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
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
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add Project",
                    modifier = Modifier.size(26.dp)
                )
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            hoveredElevation = 6.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
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
                            Text(
                                text = projectWithTotal.project.emoji,
                                fontSize = 26.sp
                            )
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
                        // Explore Button
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
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Explore",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )

                            }
                        }

                        // Three-dot menu button - toggles menu expansion
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

                // Animated expandable menu inside card
                AnimatedVisibility(
                    visible = menuExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        androidx.compose.material3.HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            thickness = 1.dp
                        )

                        // Menu Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Edit Button
                            MenuActionButton(
                                icon = Icons.Default.Edit,
                                label = "Edit",
                                onClick = {
                                    onEditClick()
                                    menuExpanded = false
                                },
                                modifier = Modifier.weight(1f),
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )

                            // Move to Top Button
                            MenuActionButton(
                                icon = Icons.Default.KeyboardArrowUp,
                                label = "To Top",
                                onClick = {
                                    if (canMoveUp) {
                                        onReorder(currentIndex, 0)
                                    }
                                    menuExpanded = false
                                },
                                enabled = canMoveUp,
                                modifier = Modifier.weight(1f),
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )

                            // Move to Bottom Button
                            MenuActionButton(
                                icon = Icons.Default.KeyboardArrowDown,
                                label = "To Bottom",
                                onClick = {
                                    if (canMoveDown) {
                                        onReorder(currentIndex, totalItems - 1)
                                    }
                                    menuExpanded = false
                                },
                                enabled = canMoveDown,
                                modifier = Modifier.weight(1f),
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        }

                        // Delete Button - Full Width
                        MenuActionButton(
                            icon = Icons.Default.Delete,
                            label = "Delete Project",
                            onClick = {
                                onDeleteClick()
                                menuExpanded = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    }
                }

                // Only show main content when menu is collapsed
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

                        androidx.compose.material3.HorizontalDivider(
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



                        // Dates Row with Explore Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
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
                                                "₹ ••••••",
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
private fun CompactStatBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
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
private fun DateChip(
    label: String,
    date: String
) {
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

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatDateCompact(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
fun AddProjectSheetContent(
    onCancel: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
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
            Text(
                text = selectedEmoji,
                style = MaterialTheme.typography.displaySmall,
                fontSize = 40.sp
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Create New Project",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Choose an emoji and name",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Select Emoji",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(projectEmojis) { emoji ->
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    if (emoji == selectedEmoji)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.surface
                                )
                                .clickable { selectedEmoji = emoji },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = emoji,
                                fontSize = 24.sp
                            )
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    1.5.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                ),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Text(
                    "Cancel",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Button(
                onClick = { onConfirm(projectName, selectedEmoji) },
                enabled = projectName.isNotBlank(),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 8.dp
                ),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Create",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
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
            Text(
                text = selectedEmoji,
                fontSize = 32.sp
            )
        }

        Text(
            "Edit Project",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Update project details",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Select Emoji",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(projectEmojis) { emoji ->
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(
                                    if (emoji == selectedEmoji)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.surface
                                )
                                .clickable { selectedEmoji = emoji },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = emoji,
                                fontSize = 22.sp
                            )
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    1.5.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            ) {
                Text(
                    "Cancel",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Button(
                onClick = { onConfirm(editedName, selectedEmoji) },
                enabled = editedName.isNotBlank() && (editedName != currentName || selectedEmoji != currentEmoji),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 2.dp,
                    pressedElevation = 6.dp
                ),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Save",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun DeleteProjectSheetContent(
    projectName: String,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
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
            Icon(
                Icons.Default.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(32.dp)
            )
        }

        Text(
            "Delete Project?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Are you sure you want to delete",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            ) {
                Text(
                    text = "'$projectName'",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
            Text(
                "This will permanently remove all categories and expenses.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
                Text(
                    "Cancel",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    "Delete",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun EmptyProjectsState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
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

            Text(
                text = "No Projects Yet",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Create your first project to start\ntracking expenses",
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
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
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