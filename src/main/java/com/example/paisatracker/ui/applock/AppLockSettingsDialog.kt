package com.example.paisatracker.ui.applock

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.paisatracker.data.AppLockPreferences
import com.example.paisatracker.util.BiometricHelper
import kotlinx.coroutines.launch

@Composable
fun AppLockSettingsDialog(
    appLockPrefs: AppLockPreferences,
    isAppLockEnabled: Boolean,
    isBiometricEnabled: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showChangePinDialog by remember { mutableStateOf(false) }
    var localAppLockEnabled by remember { mutableStateOf(isAppLockEnabled) }
    var localBiometricEnabled by remember { mutableStateOf(isBiometricEnabled) }

    // Check if biometric is available
    val isBiometricAvailable = remember { BiometricHelper.isBiometricAvailable(context) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                "App Lock Settings",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Enable/Disable App Lock
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "App Lock",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Require unlock on app launch",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = localAppLockEnabled,
                            onCheckedChange = {
                                localAppLockEnabled = it
                                scope.launch {
                                    appLockPrefs.setAppLockEnabled(it)
                                    if (!it) {
                                        // If app lock disabled, also disable biometric
                                        appLockPrefs.setBiometricEnabled(false)
                                        localBiometricEnabled = false
                                    }
                                }
                            }
                        )
                    }
                }

                // Biometric Toggle (only if app lock enabled AND biometric available)
                if (localAppLockEnabled) {
                    if (isBiometricAvailable) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Fingerprint,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Column {
                                        Text(
                                            "Biometric Unlock",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            "Use fingerprint/face",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Switch(
                                    checked = localBiometricEnabled,
                                    onCheckedChange = {
                                        localBiometricEnabled = it
                                        scope.launch {
                                            appLockPrefs.setBiometricEnabled(it)
                                            Toast.makeText(
                                                context,
                                                if (it) "Biometric enabled" else "Biometric disabled",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                )
                            }
                        }
                    } else {
                        // Biometric not available message
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Fingerprint,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    "Biometric authentication not available on this device",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                // Change PIN Button
                if (localAppLockEnabled) {
                    Button(
                        onClick = { showChangePinDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(
                            Icons.Default.VpnKey,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Change PIN")
                    }
                }

                Divider()

                Text(
                    "💡 Tip: App lock will activate when you open the app",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )

    // Change PIN Dialog
    if (showChangePinDialog) {
        SetupPinDialog(
            onDismiss = { showChangePinDialog = false },
            onPinSet = { newPin ->
                scope.launch {
                    appLockPrefs.setPinCode(newPin)
                    Toast.makeText(context, "PIN changed successfully", Toast.LENGTH_SHORT).show()
                    showChangePinDialog = false
                }
            }
        )
    }
}
