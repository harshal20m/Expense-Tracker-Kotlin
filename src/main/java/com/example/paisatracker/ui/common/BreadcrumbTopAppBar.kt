package com.example.paisatracker.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import com.example.paisatracker.PaisaTrackerViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BreadcrumbTopAppBar(
    navController: NavController,
    viewModel: PaisaTrackerViewModel,
    navBackStackEntry: NavBackStackEntry?
) {
    val currentRoute = navBackStackEntry?.destination?.route

    // List of routes where breadcrumbs should be shown
    val projectRelatedRoutes = listOf(
        "projects",
        "project_details/{projectId}",
        "expense_list/{categoryId}"
    )

    // Hide breadcrumb bar if not a project-related route
    if (currentRoute !in projectRelatedRoutes) {
        return
    }

    // Also hide on main projects screen
    if (currentRoute == "projects") {
        return
    }

    TopAppBar(
        title = {
            BreadcrumbTitle(
                navController = navController,
                viewModel = viewModel,
                navBackStackEntry = navBackStackEntry
            )
        },
        navigationIcon = {
            IconButton(onClick = { navController.navigateUp() }) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}


@Composable
fun BreadcrumbTitle(
    navController: NavController,
    viewModel: PaisaTrackerViewModel,
    navBackStackEntry: NavBackStackEntry?
) {
    val currentRoute = navBackStackEntry?.destination?.route

    val breadcrumbTextStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp)
    val activeBreadcrumbStyle = breadcrumbTextStyle.copy(fontWeight = FontWeight.SemiBold)
    val separatorModifier = Modifier.padding(horizontal = 4.dp)

    when (currentRoute) {
        "project_details/{projectId}" -> {
            val projectId = navBackStackEntry?.arguments?.getString("projectId")?.toLongOrNull()
            if (projectId != null) {
                val project by viewModel.getProjectById(projectId).collectAsState(initial = null)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Projects",
                        style = breadcrumbTextStyle,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            navController.navigate("projects") {
                                popUpTo("projects") { inclusive = false }
                            }
                        }
                    )
                    Text(
                        " > ",
                        modifier = separatorModifier,
                        style = breadcrumbTextStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        project?.name ?: "Loading...",
                        style = activeBreadcrumbStyle,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        "expense_list/{categoryId}" -> {
            val categoryId = navBackStackEntry?.arguments?.getString("categoryId")?.toLongOrNull()
            if (categoryId != null) {
                val category by viewModel.getCategoryById(categoryId).collectAsState(initial = null)
                val project by if (category != null) {
                    viewModel.getProjectById(category!!.projectId).collectAsState(initial = null)
                } else {
                    remember { mutableStateOf(null) }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Projects",
                        style = breadcrumbTextStyle,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            navController.navigate("projects") {
                                popUpTo("projects") { inclusive = false }
                            }
                        }
                    )
                    Text(
                        " > ",
                        modifier = separatorModifier,
                        style = breadcrumbTextStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (project != null) {
                        Text(
                            project!!.name,
                            style = breadcrumbTextStyle,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable {
                                navController.navigate("project_details/${project!!.id}") {
                                    popUpTo("project_details/${project!!.id}") { inclusive = false }
                                }
                            }
                        )
                        Text(
                            " > ",
                            modifier = separatorModifier,
                            style = breadcrumbTextStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            category?.name ?: "Loading...",
                            style = activeBreadcrumbStyle,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    } else {
                        Text(
                            "Loading...",
                            style = breadcrumbTextStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        else -> {
            // Fallback - shouldn't reach here due to filter above
            Text(
                "Projects",
                style = activeBreadcrumbStyle,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
