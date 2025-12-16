package com.example.paisatracker.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.paisatracker.PaisaTrackerViewModel
import com.example.paisatracker.ui.assets.AssetsScreen

import com.example.paisatracker.ui.details.ProjectDetailsScreen
import com.example.paisatracker.ui.details.ProjectInsightsScreen
import com.example.paisatracker.ui.expense.ExpenseDetailScreen
import com.example.paisatracker.ui.expense.ExpenseListScreen
import com.example.paisatracker.ui.export.ExportScreen
import com.example.paisatracker.ui.main.ProjectListScreen
import com.example.paisatracker.ui.settings.SettingsScreen

@Composable
fun AppNavigation(navController: NavHostController, viewModel: PaisaTrackerViewModel, modifier: Modifier = Modifier) {
    NavHost(navController, startDestination = "projects", modifier = modifier) {
        composable("projects") {
            ProjectListScreen(viewModel = viewModel, navController = navController)
        }
        composable("export") {
            ExportScreen(viewModel = viewModel)
        }
        composable("assets") {
            AssetsScreen(viewModel = viewModel)
        }
        composable("project_details/{projectId}") { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId")?.toLongOrNull() ?: 0L
            ProjectDetailsScreen(viewModel = viewModel, projectId = projectId, navController = navController)
        }

        composable("expense_details/{expenseId}") { backStackEntry ->
            val id = backStackEntry.arguments?.getString("expenseId")?.toLongOrNull() ?: return@composable
            ExpenseDetailScreen(
                viewModel = viewModel,
                expenseId = id,
                navController = navController
            )
        }


        composable("expense_list/{categoryId}") { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString("categoryId")?.toLong() ?: return@composable
            ExpenseListScreen(
                viewModel = viewModel,
                categoryId = categoryId,
                navController = navController
            )
        }

        composable("settings") {
            SettingsScreen(viewModel = viewModel)
        }

        composable("project_insights/{projectId}") { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId")?.toLongOrNull()
            if (projectId != null) {
                ProjectInsightsScreen(
                    viewModel = viewModel,
                    projectId = projectId,
                    navController = navController
                )
            }
        }
    }
}
