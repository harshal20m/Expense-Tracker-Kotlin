package com.example.paisatracker.navigation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.paisatracker.PaisaTrackerApplication
import com.example.paisatracker.PaisaTrackerViewModel
import com.example.paisatracker.ui.analytics.AnalyticsScreen
import com.example.paisatracker.ui.bankaccount.BankAccountScreen
import com.example.paisatracker.ui.bankaccount.BankAccountTransactionScreen
import com.example.paisatracker.ui.bankaccount.BankAccountViewModel
import com.example.paisatracker.ui.bankaccount.BankAccountViewModelFactory
import com.example.paisatracker.ui.common.CalendarTransactionView
import com.example.paisatracker.ui.details.ProjectDetailsScreen
import com.example.paisatracker.ui.details.ProjectInsightsScreen
import com.example.paisatracker.ui.expense.ExpenseDetailScreen
import com.example.paisatracker.ui.expense.ExpenseListScreen
import com.example.paisatracker.ui.budget.BudgetScreen
import com.example.paisatracker.ui.finance.FinanceScreen
import com.example.paisatracker.ui.main.home.HomeScreen
import com.example.paisatracker.ui.main.projects.ProjectListScreen
import com.example.paisatracker.ui.management.ManagementScreen
import com.example.paisatracker.ui.settings.SettingsScreen
import com.example.paisatracker.ui.bin.BinScreen
import com.example.paisatracker.viewmodel.AnalyticsViewModel
import com.example.paisatracker.viewmodel.AnalyticsViewModelFactory

@Composable
fun AppNavigation(
    navController: NavHostController,
    viewModel: PaisaTrackerViewModel,
    modifier: Modifier = Modifier,
) {
    NavHost(navController, startDestination = "home", modifier = modifier) {
        composable("home") {
            HomeScreen(viewModel = viewModel, navController = navController)
        }
        composable("projects") {
            ProjectListScreen(viewModel = viewModel, navController = navController)
        }
        composable("calendar") {
            val expenses by viewModel.getAllExpensesWithDetails().collectAsState(initial = emptyList())
            CalendarTransactionView(
                expenses = expenses,
                onTransactionClick = { expenseId ->
                    navController.navigate("expense_details/$expenseId")
                },
                onBackClick = { navController.popBackStack() }
            )
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
        composable("budget") {
            BudgetScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onOpenProject = { projectId ->
                    navController.navigate("project_details/$projectId")
                },
                onOpenCategory = { categoryId ->
                    navController.navigate("expense_list/$categoryId")
                },
                currencySymbol = "₹"
            )
        }
        composable("bin") {
            BinScreen(viewModel = viewModel, navController = navController)
        }
        composable("analytics") {
            val analyticsViewModel = viewModel<AnalyticsViewModel>(
                factory = AnalyticsViewModelFactory(
                    (LocalContext.current.applicationContext as PaisaTrackerApplication).repository
                )
            )
            AnalyticsScreen(
                viewModel = analyticsViewModel,
                onNavigateBack = { navController.popBackStack() },
                onCategoryClick = { category ->
                    // Navigate to expense list for this category
                    navController.navigate("expense_list/${category.categoryId}")
                }
            )
        }
        composable("bank_accounts") {
            FinanceScreen(
                mainViewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onOpenProject = { projectId ->
                    navController.navigate("project_details/$projectId")
                },
                onOpenCategory = { categoryId ->
                    navController.navigate("expense_list/$categoryId")
                },
                onOpenBankAccount = { bankAccountId ->
                    navController.navigate("bank_account_transactions/$bankAccountId")
                },
                currencySymbol = "₹"
            )
        }
        composable("bank_account_transactions/{bankAccountId}") { backStackEntry ->
            val bankAccountId = backStackEntry.arguments?.getString("bankAccountId")?.toLongOrNull() ?: 0L
            BankAccountTransactionScreen(
                bankAccountId = bankAccountId,
                viewModel = viewModel,
                navController = navController
            )
        }
        composable("management") {
            ManagementScreen(
                viewModel = viewModel,
                navController = navController
            )
        }
    }
}