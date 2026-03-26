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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.paisatracker.PaisaTrackerApplication
import com.example.paisatracker.data.AppTheme
import com.example.paisatracker.ui.assets.CompactHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: PaisaTrackerViewModel,
    navController: NavHostController
) {
    val context = LocalContext.current
    val application = context.applicationContext as PaisaTrackerApplication
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(application.themePreferencesRepository)
    )
    val currentTheme by settingsViewModel.currentTheme.collectAsState()

    var showNotificationDialog by remember { mutableStateOf(false) }
    var showBatteryDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showThemeSelectionDialog by remember { mutableStateOf(false) }


    Column(modifier = Modifier.fillMaxSize()) {
        CompactHeader(
            title = "Settings",
            subtitle = "Customize your experience",
            icon = Icons.Default.Settings
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { SectionHeader(title = "Notifications") }
            item { SettingsCard(icon = Icons.Default.Notifications, title = "Daily Reminders", subtitle = "Configure time and frequency", onClick = { showNotificationDialog = true }) }
            item { SettingsCard(icon = Icons.Default.BatteryChargingFull, title = "Battery Optimization", subtitle = "Required for notifications", onClick = { showBatteryDialog = true }) }

            item { Spacer(modifier = Modifier.height(8.dp)); SectionHeader(title = "App") }
            item { SettingsCard(icon = Icons.Default.Palette, title = "App Theme", subtitle = "Current: ${currentTheme.themeName}", onClick = { showThemeSelectionDialog = true }) }
            item { SettingsCard(icon = Icons.Default.Info, title = "About", subtitle = "Version, developer info", onClick = { showAboutDialog = true }) }
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
                            val apkUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/vnd.android.package-archive"
                                putExtra(Intent.EXTRA_STREAM, apkUri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share PaisaTracker APK"))
                        } catch (e: Exception) { Toast.makeText(context, "Unable to share APK", Toast.LENGTH_SHORT).show() }
                    }
                )
            }

            item { Spacer(modifier = Modifier.height(8.dp)); SectionHeader(title = "Data & Privacy") }
            item { SettingsCard(icon = Icons.Default.CloudSync, title = "Backup & Restore", subtitle = "Export and import your data", onClick = { navController.navigate("export") }) }

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
                    onClick = { if (isAppLockEnabled) showAppLockSettingsDialog = true else showPinSetupDialog = true }
                )

                if (showPinSetupDialog) {
                    SetupPinDialog(onDismiss = { showPinSetupDialog = false }, onPinSet = { pin ->
                        scope.launch {
                            appLockPrefs.setPinCode(pin)
                            appLockPrefs.setAppLockEnabled(true)
                            showPinSetupDialog = false
                        }
                    })
                }
                if (showAppLockSettingsDialog) {
                    AppLockSettingsDialog(appLockPrefs = appLockPrefs, isAppLockEnabled = isAppLockEnabled, isBiometricEnabled = isBiometricEnabled, onDismiss = { showAppLockSettingsDialog = false })
                }
            }
        }
    }

    if (showNotificationDialog) NotificationSettingsBottomSheet(viewModel = viewModel, onDismiss = { showNotificationDialog = false })
    if (showBatteryDialog) BatteryOptimizationBottomSheet(onDismiss = { showBatteryDialog = false })
    if (showAboutDialog) AboutBottomSheet(onDismiss = { showAboutDialog = false })
    if (showThemeSelectionDialog) {
        ThemeSelectionBottomSheet(currentTheme = currentTheme, onDismiss = { showThemeSelectionDialog = false }, onThemeSelected = { theme ->
            settingsViewModel.saveTheme(theme)
            showThemeSelectionDialog = false
        })
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(text = title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp))
}

@Composable
fun SettingsCard(icon: ImageVector, title: String, subtitle: String, enabled: Boolean = true, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
        }
    }
}
