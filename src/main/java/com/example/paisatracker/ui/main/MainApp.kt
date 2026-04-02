package com.example.paisatracker.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.example.paisatracker.PaisaTrackerViewModel
import com.example.paisatracker.navigation.AppNavigation
import com.example.paisatracker.ui.common.BottomNavigationBar
import com.example.paisatracker.ui.common.BreadcrumbNavigation
import com.example.paisatracker.ui.flap.QuickAccessFlap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(viewModel: PaisaTrackerViewModel) {
    val navController = rememberNavController()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()) {

            // ── Layer 1: Screen content ──────────────────────────────────
            AppNavigation(
                navController = navController,
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize()
            )

            // ── Layer 2: Breadcrumb + Bottom Nav (always on top of screens) ──
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                Column {
                    BreadcrumbNavigation(
                        navController = navController,
                        viewModel = viewModel
                    )
                    BottomNavigationBar(navController = navController)
                }
            }

            // ── Layer 3: Quick Access Flap (floats above everything) ─────
            // It positions itself just above the bottom nav via bottomNavHeight.
            // bottomNavHeight = BottomNavItem height (72dp) + vertical padding (8dp top + 24dp bottom) = 104dp
            QuickAccessFlap(
                viewModel = viewModel,
                bottomNavHeight = 104.dp,
            )
        }
    }
}