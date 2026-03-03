//package com.example.paisatracker.ui.common
//
//import androidx.compose.animation.AnimatedVisibility
//import androidx.compose.animation.animateColorAsState
//import androidx.compose.animation.core.spring
//import androidx.compose.animation.core.tween
//import androidx.compose.animation.fadeIn
//import androidx.compose.animation.fadeOut
//import androidx.compose.animation.slideInVertically
//import androidx.compose.animation.slideOutVertically
//import androidx.compose.foundation.background
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.interaction.MutableInteractionSource
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.ChevronRight
//import androidx.compose.material.icons.filled.Home
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextOverflow
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.navigation.NavController
//import androidx.navigation.compose.currentBackStackEntryAsState
//
//data class BreadcrumbItem(
//    val label: String,
//    val route: String
//)
//
//@Composable
//fun BreadcrumbNavigation(navController: NavController) {
//    val navBackStackEntry by navController.currentBackStackEntryAsState()
//    val currentRoute = navBackStackEntry?.destination?.route
//
//    val breadcrumbs = remember(currentRoute) {
//        buildBreadcrumbTrail(currentRoute)
//    }
//
//    AnimatedVisibility(
//        visible = breadcrumbs.size > 1,
//        enter = slideInVertically(
//            initialOffsetY = { it },
//            animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)
//        ) + fadeIn(),
//        exit = slideOutVertically(
//            targetOffsetY = { it },
//            animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)
//        ) + fadeOut()
//    ) {
//        Box(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(horizontal = 16.dp, vertical = 8.dp),
//            contentAlignment = Alignment.Center
//        ) {
//            Surface(
//                modifier = Modifier.wrapContentWidth(),
//                shape = RoundedCornerShape(24.dp),
//                tonalElevation = 2.dp,
//                shadowElevation = 6.dp,
//                color = Color.Transparent
//            ) {
//                Row(
//                    modifier = Modifier
//                        .wrapContentWidth()
//                        .height(52.dp)
//                        .background(
//                            color = MaterialTheme.colorScheme.surfaceContainerLow,
//                            shape = RoundedCornerShape(24.dp)
//                        )
//                        .padding(horizontal = 12.dp, vertical = 8.dp),
//                    horizontalArrangement = Arrangement.spacedBy(4.dp),
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    breadcrumbs.forEachIndexed { index, breadcrumb ->
//                        val isLast = index == breadcrumbs.size - 1
//
//                        BreadcrumbChip(
//                            label = breadcrumb.label,
//                            isActive = isLast,
//                            onClick = {
//                                if (!isLast) {
//                                    navController.navigate(breadcrumb.route) {
//                                        popUpTo(breadcrumb.route) {
//                                            inclusive = false
//                                        }
//                                        launchSingleTop = true
//                                    }
//                                }
//                            },
//                            showHomeIcon = index == 0
//                        )
//
//                        if (!isLast) {
//                            Icon(
//                                imageVector = Icons.Default.ChevronRight,
//                                contentDescription = null,
//                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
//                                modifier = Modifier.size(16.dp)
//                            )
//                        }
//                    }
//                }
//            }
//        }
//    }
//}
//
//@Composable
//private fun BreadcrumbChip(
//    label: String,
//    isActive: Boolean,
//    onClick: () -> Unit,
//    showHomeIcon: Boolean = false
//) {
//    val interactionSource = remember { MutableInteractionSource() }
//
//    val containerColor by animateColorAsState(
//        targetValue = if (isActive)
//            MaterialTheme.colorScheme.primaryContainer
//        else
//            Color.Transparent,
//        animationSpec = tween(300),
//        label = "containerColor"
//    )
//
//    val contentColor by animateColorAsState(
//        targetValue = if (isActive)
//            MaterialTheme.colorScheme.onPrimaryContainer
//        else
//            MaterialTheme.colorScheme.onSurfaceVariant,
//        animationSpec = tween(300),
//        label = "contentColor"
//    )
//
//    Box(
//        modifier = Modifier
//            .height(36.dp)
//            .clip(RoundedCornerShape(18.dp))
//            .background(containerColor)
//            .clickable(
//                onClick = onClick,
//                interactionSource = interactionSource,
//                indication = ripple(
//                    bounded = true,
//                    color = MaterialTheme.colorScheme.primary
//                ),
//                enabled = !isActive
//            )
//            .padding(horizontal = 12.dp),
//        contentAlignment = Alignment.Center
//    ) {
//        Row(
//            verticalAlignment = Alignment.CenterVertically,
//            horizontalArrangement = Arrangement.Center
//        ) {
//            if (showHomeIcon) {
//                Icon(
//                    imageVector = Icons.Default.Home,
//                    contentDescription = null,
//                    tint = contentColor,
//                    modifier = Modifier.size(18.dp)
//                )
//                if (label.isNotEmpty()) {
//                    Spacer(modifier = Modifier.width(4.dp))
//                }
//            }
//
//            if (label.isNotEmpty()) {
//                Text(
//                    text = label,
//                    color = contentColor,
//                    style = MaterialTheme.typography.labelMedium,
//                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
//                    fontSize = 13.sp,
//                    maxLines = 1,
//                    overflow = TextOverflow.Ellipsis
//                )
//            }
//        }
//    }
//}
//
//private fun buildBreadcrumbTrail(route: String?): List<BreadcrumbItem> {
//    if (route == null) return emptyList()
//
//    val breadcrumbs = mutableListOf<BreadcrumbItem>()
//
//    breadcrumbs.add(BreadcrumbItem("Projects", "projects"))
//
//    when {
//        route.startsWith("project_details/") -> {
//            breadcrumbs.add(BreadcrumbItem("Details", route))
//        }
//        route.startsWith("project_insights/") -> {
//            val projectId = route.substringAfter("project_insights/")
//            breadcrumbs.add(BreadcrumbItem("Details", "project_details/$projectId"))
//            breadcrumbs.add(BreadcrumbItem("Insights", route))
//        }
//        route.startsWith("expense_list/") -> {
//            breadcrumbs.add(BreadcrumbItem("Expenses", route))
//        }
//        route.startsWith("expense_details/") -> {
//            breadcrumbs.add(BreadcrumbItem("Expense", route))
//        }
//        route == "assets" -> {
//            return emptyList()
//        }
//        route == "export" -> {
//            return emptyList()
//        }
//        route == "settings" -> {
//            return emptyList()
//        }
//        route == "projects" -> {
//            return emptyList()
//        }
//    }
//
//    return breadcrumbs
//}

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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.wrapContentWidth(),
                shape = RoundedCornerShape(24.dp),
                tonalElevation = 2.dp,
                shadowElevation = 6.dp,
                color = Color.Transparent
            ) {
                Row(
                    modifier = Modifier
                        .wrapContentWidth()
                        .height(52.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = RoundedCornerShape(24.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    breadcrumbs.forEachIndexed { index, breadcrumb ->
                        val isLast = index == breadcrumbs.size - 1

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
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(16.dp)
                            )
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
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
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
            .padding(horizontal = 12.dp),
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
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }

            Text(
                text = label,
                color = contentColor,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

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

                val expenseName = expense?.description ?: "Expense"
                breadcrumbs.add(BreadcrumbItem(expenseName, route))
            }
        }

        route == "assets" || route == "export" || route == "settings" || route == "projects" -> {
            return emptyList()
        }
    }

    return breadcrumbs
}