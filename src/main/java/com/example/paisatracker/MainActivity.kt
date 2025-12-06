package com.example.paisatracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.example.paisatracker.ui.main.MainApp
import com.example.paisatracker.ui.theme.PaisaTrackerTheme

class MainActivity : ComponentActivity() {
    private val viewModel: PaisaTrackerViewModel by viewModels {
        PaisaTrackerViewModelFactory((application as PaisaTrackerApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PaisaTrackerTheme {
                MainApp(viewModel = viewModel)
            }
        }
    }
}
