package com.example.paisatracker.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.paisatracker.PaisaTrackerApplication
import com.example.paisatracker.PaisaTrackerViewModel
import com.example.paisatracker.navigation.AppNavigation
import com.example.paisatracker.ui.common.BottomNavigationBar
import com.example.paisatracker.ui.common.BreadcrumbNavigation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(viewModel: PaisaTrackerViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val application = LocalContext.current.applicationContext as PaisaTrackerApplication

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        // Use safeDrawingPadding to respect system bars (status, nav, and cutouts)
        Box(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            // Navigation content
            AppNavigation(
                navController = navController,
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize(),
                application = application
            )

            // Floating breadcrumb and bottom nav (overlay)
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.BottomCenter
            ) {
                Column {
                    // Breadcrumb just above bottom nav
                    BreadcrumbNavigation(navController = navController, viewModel = PaisaTrackerViewModel(application.repository))

                    // Bottom navigation bar
                    BottomNavigationBar(navController = navController)
                }
            }
        }
    }
}
