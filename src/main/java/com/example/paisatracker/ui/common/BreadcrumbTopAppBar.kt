package com.example.paisatracker.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
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
    val breadcrumbTextStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp)
    val activeBreadcrumbStyle = breadcrumbTextStyle.copy(fontWeight = FontWeight.Bold)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Always show Projects
        Text(
            text = "Projects",
            style = breadcrumbTextStyle,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable {
                navController.navigate("projects") { popUpTo("projects") { inclusive = true } }
            }
        )

        when (currentRoute) {
            "project_details/{projectId}" -> {
                val projectId = navBackStackEntry?.arguments?.getString("projectId")?.toLongOrNull()
                projectId?.let { id ->
                    val project by viewModel.getProjectById(id).collectAsState(initial = null)
                    BreadcrumbSeparator()
                    Text(
                        text = project?.name ?: "...",
                        style = activeBreadcrumbStyle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            "expense_list/{categoryId}" -> {
                val categoryId = navBackStackEntry?.arguments?.getString("categoryId")?.toLongOrNull()
                categoryId?.let { id ->
                    val category by viewModel.getCategoryById(id).collectAsState(initial = null)
                    // You could potentially fetch the project name via the ViewModel
                    // in a single state object to avoid nested collecting
                    BreadcrumbSeparator()
                    Text(
                        text = "Category", // Or fetch project name
                        style = breadcrumbTextStyle,
                        color = MaterialTheme.colorScheme.primary
                    )
                    BreadcrumbSeparator()
                    Text(
                        text = category?.name ?: "...",
                        style = activeBreadcrumbStyle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun BreadcrumbSeparator() {
    Text(
        text = "/",
        modifier = Modifier.padding(horizontal = 6.dp),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    )
}