package com.example.paisatracker.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.paisatracker.PaisaTrackerViewModel
import com.example.paisatracker.data.AppLockPreferences
import com.example.paisatracker.ui.applock.SetupPinDialog
import com.example.paisatracker.ui.applock.AppLockSettingsDialog
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.content.FileProvider
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: PaisaTrackerViewModel,
    navController: NavHostController
) {
    val context = LocalContext.current
    var showNotificationDialog by remember { mutableStateOf(false) }
    var showBatteryDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            MaterialTheme.colorScheme.surface
                        ),
                        startY = 0f,
                        endY = 250f
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp, bottom = 20.dp, start = 24.dp, end = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 8.dp,
                    shadowElevation = 4.dp,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = (-0.5).sp
                )

                Text(
                    text = "Customize your experience",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Notifications Section
            item {
                SectionHeader(title = "Notifications")
            }

            item {
                SettingsCard(
                    icon = Icons.Default.Notifications,
                    title = "Daily Reminders",
                    subtitle = "Configure notification time and frequency",
                    onClick = { showNotificationDialog = true }
                )
            }

            item {
                SettingsCard(
                    icon = Icons.Default.BatteryChargingFull,
                    title = "Battery Optimization",
                    subtitle = "Required for background notifications",
                    onClick = { showBatteryDialog = true }
                )
            }

            // App Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader(title = "App")
            }

            item {
                SettingsCard(
                    icon = Icons.Default.Info,
                    title = "About",
                    subtitle = "Version, developer info",
                    onClick = { showAboutDialog = true }
                )
            }

            item {
                SettingsCard(
                    icon = Icons.Default.Share,
                    title = "Share App",
                    subtitle = "Invite friends to track expenses",
                    onClick = {
                        try {
                            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                            val apkPath = packageInfo.applicationInfo?.sourceDir
                            val apkFile = File(apkPath ?: return@SettingsCard)
                            val packageName = context.packageName ?: run {
                                Toast.makeText(context, "Error: Package name not found", Toast.LENGTH_SHORT).show()
                                return@SettingsCard
                            }

                            val apkUri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                apkFile
                            )

                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/vnd.android.package-archive"
                                putExtra(Intent.EXTRA_STREAM, apkUri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                putExtra(Intent.EXTRA_SUBJECT, "PaisaTracker App")
                                putExtra(Intent.EXTRA_TEXT, "Check out PaisaTracker - Your Personal Expense Tracker!")
                            }

                            context.startActivity(Intent.createChooser(shareIntent, "Share PaisaTracker APK"))
                        } catch (e: Exception) {
                            Toast.makeText(context, "Unable to share APK: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            // Data & Privacy Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader(title = "Data & Privacy")
            }

            item {
                SettingsCard(
                    icon = Icons.Default.CloudUpload,
                    title = "Backup & Restore",
                    subtitle = "Export and import your data",
                    enabled = true,
                    onClick = {
                        navController.navigate("export") {
                            popUpTo("projects") {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            // App Lock Card
            item {
                val appLockPrefs = remember { AppLockPreferences.getInstance(context) }
                val isAppLockEnabled by appLockPrefs.isAppLockEnabled.collectAsState(initial = false)
                val isBiometricEnabled by appLockPrefs.isBiometricEnabled.collectAsState(initial = false)
                var showPinSetupDialog by remember { mutableStateOf(false) }
                var showAppLockSettingsDialog by remember { mutableStateOf(false) }
                val scope = rememberCoroutineScope()

                SettingsCard(
                    icon = Icons.Default.Lock,
                    title = "App Lock",
                    subtitle = if (isAppLockEnabled) "Enabled with ${if (isBiometricEnabled) "Biometric" else "PIN"}" else "Secure with PIN or Fingerprint",
                    enabled = true,
                    onClick = {
                        if (isAppLockEnabled) {
                            showAppLockSettingsDialog = true
                        } else {
                            showPinSetupDialog = true
                        }
                    }
                )

                // PIN Setup Dialog
                if (showPinSetupDialog) {
                    SetupPinDialog(
                        onDismiss = { showPinSetupDialog = false },
                        onPinSet = { pin ->
                            scope.launch {
                                appLockPrefs.setPinCode(pin)
                                appLockPrefs.setAppLockEnabled(true)
                                showPinSetupDialog = false
                            }
                        }
                    )
                }

                // App Lock Settings Dialog
                if (showAppLockSettingsDialog) {
                    AppLockSettingsDialog(
                        appLockPrefs = appLockPrefs,
                        isAppLockEnabled = isAppLockEnabled,
                        isBiometricEnabled = isBiometricEnabled,
                        onDismiss = { showAppLockSettingsDialog = false }
                    )
                }
            }
        }
    }

    // Bottom Sheets
    if (showNotificationDialog) {
        NotificationSettingsBottomSheet(
            viewModel = viewModel,
            onDismiss = { showNotificationDialog = false }
        )
    }

    if (showBatteryDialog) {
        BatteryOptimizationBottomSheet(
            onDismiss = { showBatteryDialog = false }
        )
    }

    if (showAboutDialog) {
        AboutBottomSheet(onDismiss = { showAboutDialog = false })
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
    )
}

@Composable
fun SettingsCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (enabled) 2.dp else 0.dp
        ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = if (!enabled)
            androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )
        else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (enabled)
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (enabled)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (enabled)
                            MaterialTheme.colorScheme.onSurface
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )

                    // "Coming Soon" badge for disabled items
                    if (!enabled) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = "Soon",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled)
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }

            if (enabled) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}