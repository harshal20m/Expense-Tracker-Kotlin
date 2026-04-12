package com.example.paisatracker.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.paisatracker.PaisaTrackerViewModel
import com.example.paisatracker.ui.common.CalendarTransactionView
import com.example.paisatracker.ui.details.ProjectDetailsScreen
import com.example.paisatracker.ui.details.ProjectInsightsScreen
import com.example.paisatracker.ui.expense.ExpenseDetailScreen
import com.example.paisatracker.ui.expense.ExpenseListScreen
import com.example.paisatracker.ui.export.ExportScreen
import com.example.paisatracker.ui.budget.BudgetScreen
import com.example.paisatracker.ui.main.projects.ProjectListScreen

import com.example.paisatracker.ui.settings.SettingsScreen

@Composable
fun AppNavigation(
    navController: NavHostController,
    viewModel: PaisaTrackerViewModel,
    modifier: Modifier = Modifier,
) {
    NavHost(navController, startDestination = "projects", modifier = modifier) {
        composable("projects") {
            ProjectListScreen(viewModel = viewModel, navController = navController)
        }
        composable("calendar") {
            val expenses by viewModel.getAllExpensesWithDetails().collectAsState(initial = emptyList())
            CalendarTransactionView(
                expenses = expenses,
                onTransactionClick = { expenseId ->
                    // Use your navController to jump to the details screen!
                    navController.navigate("expense_details/$expenseId")
                }
            )
        }

        composable("export") {
            ExportScreen(viewModel = viewModel, navController = navController)
        }
        composable("settings") {
            SettingsScreen(viewModel = viewModel, navController = navController)
        }
        composable("project_details/{projectId}") { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId")?.toLongOrNull() ?: 0L
            ProjectDetailsScreen(viewModel = viewModel, projectId = projectId, navController = navController)
        }
        composable("expense_details/{expenseId}") { backStackEntry ->
            val id = backStackEntry.arguments?.getString("expenseId")?.toLongOrNull() ?: return@composable
            ExpenseDetailScreen(viewModel = viewModel, expenseId = id, navController = navController)
        }
        composable("expense_list/{categoryId}") { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString("categoryId")?.toLongOrNull() ?: return@composable
            ExpenseListScreen(viewModel = viewModel, categoryId = categoryId, navController = navController)
        }
        composable("project_insights/{projectId}") { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId")?.toLongOrNull()
            if (projectId != null) {
                ProjectInsightsScreen(viewModel = viewModel, projectId = projectId, navController = navController)
            }
        }

        //budget
        composable("budget") {
            BudgetScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                currencySymbol = "₹"   // or read from your settings/DataStore
            )
        }


    }
}