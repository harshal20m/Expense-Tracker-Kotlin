package com.example.paisatracker.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.paisatracker.PaisaTrackerViewModel
import com.example.paisatracker.navigation.AppNavigation
import com.example.paisatracker.ui.common.BottomNavigationBar
import com.example.paisatracker.ui.common.BreadcrumbTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(viewModel: PaisaTrackerViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                topBar = {
                    BreadcrumbTopAppBar(
                        navController = navController,
                        viewModel = viewModel,
                        navBackStackEntry = navBackStackEntry
                    )
                },
                containerColor = MaterialTheme.colorScheme.background
            ) { paddingValues ->
                // Only top padding, no bottom
                AppNavigation(
                    navController = navController,
                    viewModel = viewModel,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = paddingValues.calculateTopPadding())
                )
            }

            // Floating bottom nav (overlay)
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.BottomCenter
            ) {
                BottomNavigationBar(navController = navController)
            }
        }
    }
}
