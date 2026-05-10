package com.example.paisatracker.ui.setup

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.paisatracker.PaisaTrackerViewModel
import com.example.paisatracker.ui.settings.DefaultDataSelectionBottomSheet
import com.example.paisatracker.ui.theme.PaisaTrackerTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirstTimeSetupSheet(
    viewModel: PaisaTrackerViewModel,
    onSetupComplete: (shouldSeedData: Boolean) -> Unit
) {
    var showCustomSelection by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    if (showCustomSelection) {
        DefaultDataSelectionBottomSheet(
            viewModel = viewModel,
            onDismiss = {
                showCustomSelection = false
                onSetupComplete(false)
            }
        )
    } else {
        ModalBottomSheet(
            onDismissRequest = {
                if (!isImporting) {
                    onSetupComplete(false)
                }
            },
            sheetState = rememberModalBottomSheetState(
                skipPartiallyExpanded = true,
                confirmValueChange = { !isImporting }
            ),
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 40.dp, top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🌱",
                    fontSize = 64.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Welcome to PaisaTracker!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Choose how you'd like to start your expense tracking journey",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Quick Start includes:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "8 common projects with 29 categories: Daily Living, Food & Dining, Transportation, Shopping, Entertainment, Bills & Utilities, Health & Wellness, and Education.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (isImporting) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Preparing your workspace...")
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                isImporting = true
                                coroutineScope.launch {
                                    onSetupComplete(true)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Quick Start (All Projects)")
                        }
                        
                        OutlinedButton(
                            onClick = { showCustomSelection = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Custom Selection")
                        }

                        TextButton(
                            onClick = { onSetupComplete(false) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Start Fresh (No Data)")
                        }
                    }
                }
            }
        }
    }
}
