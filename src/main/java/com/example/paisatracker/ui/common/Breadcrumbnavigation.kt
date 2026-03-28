package com.example.paisatracker.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.paisatracker.PaisaTrackerViewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

data class BreadcrumbItem(
    val label: String,
    val route: String
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BreadcrumbNavigation(
    navController: NavController,
    viewModel: PaisaTrackerViewModel
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val scope = rememberCoroutineScope()

    var breadcrumbs by remember { mutableStateOf<List<BreadcrumbItem>>(emptyList()) }

    LaunchedEffect(currentRoute) {
        scope.launch {
            breadcrumbs = buildBreadcrumbTrail(currentRoute, navBackStackEntry, viewModel)
        }
    }

    AnimatedVisibility(
        visible = breadcrumbs.size > 1,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)
        ) + fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                // Reduced top padding to pull it tighter to the UI above
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 0.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.wrapContentWidth(),
                shape = RoundedCornerShape(16.dp), // Tighter corner radius
                tonalElevation = 2.dp,
                shadowElevation = 4.dp, // Slightly reduced shadow
                color = Color.Transparent
            ) {
                FlowRow(
                    modifier = Modifier
                        .wrapContentWidth()
                        .defaultMinSize(minHeight = 36.dp) // Reduced from 52.dp
                        .background(
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp), // Tighter container padding
                    horizontalArrangement = Arrangement.spacedBy(2.dp), // Tighter horizontal spacing
                    verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically) // Tighter vertical wrapping
                ) {
                    breadcrumbs.forEachIndexed { index, breadcrumb ->
                        val isLast = index == breadcrumbs.size - 1

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp) // Tighter icon spacing
                        ) {
                            BreadcrumbChip(
                                label = breadcrumb.label,
                                isActive = isLast,
                                onClick = {
                                    if (!isLast) {
                                        navController.navigate(breadcrumb.route) {
                                            popUpTo(breadcrumb.route) {
                                                inclusive = false
                                            }
                                            launchSingleTop = true
                                        }
                                    }
                                },
                                showHomeIcon = index == 0
                            )

                            if (!isLast) {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(14.dp) // Smaller Chevron
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
private fun BreadcrumbChip(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    showHomeIcon: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }

    val containerColor by animateColorAsState(
        targetValue = if (isActive)
            MaterialTheme.colorScheme.primaryContainer
        else
            Color.Transparent,
        animationSpec = tween(300),
        label = "containerColor"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isActive)
            MaterialTheme.colorScheme.onPrimaryContainer
        else
            MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(300),
        label = "contentColor"
    )

    Box(
        modifier = Modifier
            .height(26.dp) // Reduced from 36.dp
            .clip(RoundedCornerShape(13.dp)) // Half of 26dp for a perfect pill shape
            .background(containerColor)
            .clickable(
                onClick = onClick,
                interactionSource = interactionSource,
                indication = ripple(
                    bounded = true,
                    color = MaterialTheme.colorScheme.primary
                ),
                enabled = !isActive
            )
            .padding(horizontal = 8.dp), // Reduced from 12.dp
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (showHomeIcon) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(14.dp) // Smaller Home Icon
                )
                Spacer(modifier = Modifier.width(2.dp))
            }

            Text(
                text = label,
                color = contentColor,
                style = MaterialTheme.typography.labelSmall, // Shifted to labelSmall
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium, // Softer weight for non-active
                fontSize = 11.sp, // Reduced from 13.sp
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// Logic remains untouched
private suspend fun buildBreadcrumbTrail(
    route: String?,
    navBackStackEntry: androidx.navigation.NavBackStackEntry?,
    viewModel: PaisaTrackerViewModel
): List<BreadcrumbItem> {
    if (route == null) return emptyList()

    val breadcrumbs = mutableListOf<BreadcrumbItem>()

    // Start with home
    breadcrumbs.add(BreadcrumbItem("Projects", "projects"))

    when {
        route.startsWith("project_details/") -> {
            val projectId = navBackStackEntry?.arguments?.getString("projectId")?.toLongOrNull()
            if (projectId != null) {
                val project = viewModel.getProjectById(projectId).firstOrNull()
                val projectName = project?.name ?: "Details"
                breadcrumbs.add(BreadcrumbItem(projectName, route))
            }
        }

        route.startsWith("project_insights/") -> {
            val projectId = navBackStackEntry?.arguments?.getString("projectId")?.toLongOrNull()
            if (projectId != null) {
                val project = viewModel.getProjectById(projectId).firstOrNull()
                val projectName = project?.name ?: "Project"
                breadcrumbs.add(BreadcrumbItem(projectName, "project_details/$projectId"))
                breadcrumbs.add(BreadcrumbItem("Insights", route))
            }
        }

        route.startsWith("expense_list/") -> {
            val categoryId = navBackStackEntry?.arguments?.getString("categoryId")?.toLongOrNull()
            if (categoryId != null) {
                val category = viewModel.getCategoryById(categoryId).firstOrNull()

                // Get project info
                val projectId = category?.projectId
                if (projectId != null) {
                    val project = viewModel.getProjectById(projectId).firstOrNull()
                    val projectName = project?.name ?: "Project"
                    breadcrumbs.add(BreadcrumbItem(projectName, "project_details/$projectId"))
                }

                val categoryName = category?.name ?: "Expenses"
                breadcrumbs.add(BreadcrumbItem(categoryName, route))
            }
        }

        route.startsWith("expense_details/") -> {
            val expenseId = navBackStackEntry?.arguments?.getString("expenseId")?.toLongOrNull()
            if (expenseId != null) {
                val expense = viewModel.getExpenseById(expenseId).firstOrNull()

                // Get category and project info
                val categoryId = expense?.categoryId
                if (categoryId != null) {
                    val category = viewModel.getCategoryById(categoryId).firstOrNull()
                    val projectId = category?.projectId

                    if (projectId != null) {
                        val project = viewModel.getProjectById(projectId).firstOrNull()
                        val projectName = project?.name ?: "Project"
                        breadcrumbs.add(BreadcrumbItem(projectName, "project_details/$projectId"))
                    }

                    val categoryName = category?.name ?: "Category"
                    breadcrumbs.add(BreadcrumbItem(categoryName, "expense_list/$categoryId"))
                }

                val expenseName = expense?.description?: "Expense"
                breadcrumbs.add(BreadcrumbItem(expenseName, route))
            }
        }

        route == "assets" || route == "export" || route == "settings" || route == "projects" -> {
            return emptyList()
        }
    }

    return breadcrumbs
}