package com.example.paisatracker.ui.settings

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.paisatracker.PaisaTrackerApplication
import com.example.paisatracker.PaisaTrackerViewModel
import com.example.paisatracker.data.AppLockPreferences
import com.example.paisatracker.data.CurrencyPreferencesRepository
import com.example.paisatracker.data.DataSeeder
import com.example.paisatracker.service.UpiNotificationListener
import com.example.paisatracker.ui.applock.AppLockSettingsDialog
import com.example.paisatracker.ui.applock.SetupPinDialog
import com.example.paisatracker.ui.assets.CompactHeader
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: PaisaTrackerViewModel,
    navController: NavHostController
) {
    val context     = LocalContext.current
    val application = context.applicationContext as PaisaTrackerApplication

    val currencyPreferencesRepository = remember { CurrencyPreferencesRepository(context) }
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(application.themePreferencesRepository, currencyPreferencesRepository)
    )

    val currentTheme     by settingsViewModel.currentTheme.collectAsState()
    val selectedCurrency by settingsViewModel.selectedCurrency.collectAsState()

    var showNotificationDialog    by remember { mutableStateOf(false) }
    var showBatteryDialog         by remember { mutableStateOf(false) }
    var showAboutDialog           by remember { mutableStateOf(false) }
    var showThemeDialog           by remember { mutableStateOf(false) }
    var showCurrencyDialog        by remember { mutableStateOf(false) }
    var showResetDialog           by remember { mutableStateOf(false) }
    var isResetting               by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    val appLockPrefs        = remember { AppLockPreferences.getInstance(context) }
    val isAppLockEnabled    by appLockPrefs.isAppLockEnabled.collectAsState(initial = false)
    val isBiometricEnabled  by appLockPrefs.isBiometricEnabled.collectAsState(initial = false)
    var showPinSetupDialog  by remember { mutableStateOf(false) }
    var showAppLockDialog   by remember { mutableStateOf(false) }

    val isNotifPermGranted = UpiNotificationListener.isPermissionGranted(context)

    Column(modifier = Modifier.fillMaxSize()) {
        CompactHeader(
            title    = "Settings",
            subtitle = "Customize your experience",
            icon     = Icons.Default.Settings
        )

        // ── Masonry / staggered grid ──────────────────────────────────────────
        LazyVerticalStaggeredGrid(
            columns             = StaggeredGridCells.Fixed(2),
            modifier            = Modifier.fillMaxSize(),
            contentPadding      = PaddingValues(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 110.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalItemSpacing = 12.dp
        ) {

            // ── Section label — full width span ───────────────────────────────
            item(span = StaggeredGridItemSpan.FullLine) {
                MasonryLabel("Appearance")
            }

            // Theme card — taller, shows colour dot preview
            item {
                MasonryCard(
                    icon     = Icons.Default.Palette,
                    title    = "Theme",
                    subtitle = currentTheme.themeName,
                    extra    = {
                        // Mini colour swatch row
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.padding(top = 8.dp)) {
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary,
                                MaterialTheme.colorScheme.tertiary,
                                MaterialTheme.colorScheme.primaryContainer
                            ).forEach { c ->
                                Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(c))
                            }
                        }
                    },
                    onClick  = { showThemeDialog = true }
                )
            }

            // Currency card
            item {
                MasonryCard(
                    icon     = Icons.Default.AttachMoney,
                    title    = "Currency",
                    subtitle = "${selectedCurrency.flag} ${selectedCurrency.code}",
                    badge    = selectedCurrency.symbol,
                    onClick  = { showCurrencyDialog = true }
                )
            }

            item(span = StaggeredGridItemSpan.FullLine) { MasonryLabel("Notifications") }

            item {
                MasonryCard(
                    icon     = Icons.Default.Notifications,
                    title    = "Reminders",
                    subtitle = "Daily expense reminders",
                    onClick  = { showNotificationDialog = true }
                )
            }

            item {
                MasonryCard(
                    icon     = Icons.Default.BatteryChargingFull,
                    title    = "Battery",
                    subtitle = "Optimization settings",
                    onClick  = { showBatteryDialog = true }
                )
            }

            // Auto-capture — full row, shows permission status badge
            item(span = StaggeredGridItemSpan.FullLine) {
                MasonryCardWide(
                    icon        = Icons.Default.NotificationsActive,
                    title       = "Auto-capture transactions",
                    subtitle    = if (isNotifPermGranted)
                        "Reading UPI notifications automatically"
                    else
                        "Tap to set up automatic expense tracking from notifications",
                    badgeText   = if (isNotifPermGranted) "Active" else "Setup",
                    badgeGreen  = isNotifPermGranted,
                    onClick     = { navController.navigate("auto_capture_settings") }
                )
            }

            item(span = StaggeredGridItemSpan.FullLine) { MasonryLabel("Security") }

            item {
                MasonryCard(
                    icon     = Icons.Default.Lock,
                    title    = "App Lock",
                    subtitle = if (isAppLockEnabled)
                        if (isBiometricEnabled) "Biometric" else "PIN"
                    else "Disabled",
                    badge    = if (isAppLockEnabled) "On" else null,
                    onClick  = {
                        if (isAppLockEnabled) showAppLockDialog = true
                        else showPinSetupDialog = true
                    }
                )
            }

            item {
                MasonryCard(
                    icon     = Icons.Default.CloudSync,
                    title    = "Backup",
                    subtitle = "Export & restore data",
                    onClick  = { navController.navigate("export") }
                )
            }

            item(span = StaggeredGridItemSpan.FullLine) { MasonryLabel("App") }

            item {
                MasonryCard(
                    icon     = Icons.Default.Share,
                    title    = "Share App",
                    subtitle = "Invite friends",
                    onClick  = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "PaisaTracker - Expense Tracker")
                            putExtra(Intent.EXTRA_TEXT,
                                "Track your expenses with PaisaTracker!\n" +
                                        "Download: https://play.google.com/store/apps/details?id=com.example.paisatracker"
                            )
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share PaisaTracker"))
                    }
                )
            }

            item {
                MasonryCard(
                    icon     = Icons.Default.Info,
                    title    = "About",
                    subtitle = "Version & developer info",
                    onClick  = { showAboutDialog = true }
                )
            }

            item(span = StaggeredGridItemSpan.FullLine) { MasonryLabel("Data") }

            // Sample data — full row with descriptive text
            item(span = StaggeredGridItemSpan.FullLine) {
                MasonryCardWide(
                    icon     = Icons.Default.Restore,
                    title    = "Add default data",
                    subtitle = "Add sample projects & categories (won't delete existing data)",
                    onClick  = { showResetDialog = true }
                )
            }
        }
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────

    if (showPinSetupDialog) {
        SetupPinDialog(
            onDismiss = { showPinSetupDialog = false },
            onPinSet  = { pin ->
                scope.launch {
                    appLockPrefs.setPinCode(pin)
                    appLockPrefs.setAppLockEnabled(true)
                    showPinSetupDialog = false
                }
            }
        )
    }

    if (showAppLockDialog) {
        AppLockSettingsDialog(
            appLockPrefs       = appLockPrefs,
            isAppLockEnabled   = isAppLockEnabled,
            isBiometricEnabled = isBiometricEnabled,
            onDismiss          = { showAppLockDialog = false }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title  = { Text("Add default data?") },
            text   = {
                Text(
                    "Adds sample projects and categories. Your existing data will not be deleted.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick  = {
                        scope.launch {
                            isResetting = true
                            DataSeeder.getInstance(application.repository)
                                .seedInitialDataIfUserAccepts(context, true)
                            isResetting = false
                            showResetDialog = false
                            Toast.makeText(context, "Default data added!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = !isResetting
                ) {
                    if (isResetting) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Text("Adding…")
                        }
                    } else Text("Add defaults")
                }
            },
            dismissButton = { TextButton(onClick = { showResetDialog = false }) { Text("Cancel") } },
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showNotificationDialog) NotificationSettingsBottomSheet(viewModel = viewModel, onDismiss = { showNotificationDialog = false })
    if (showBatteryDialog)      BatteryOptimizationBottomSheet(onDismiss = { showBatteryDialog = false })
    if (showAboutDialog)        AboutBottomSheet(onDismiss = { showAboutDialog = false })
    if (showThemeDialog) {
        ThemeSelectionBottomSheet(
            currentTheme     = currentTheme,
            onDismiss        = { showThemeDialog = false },
            onThemeSelected  = { settingsViewModel.saveTheme(it); showThemeDialog = false }
        )
    }
    if (showCurrencyDialog) {
        CurrencySelectionBottomSheet(
            currentCurrency  = selectedCurrency,
            onDismiss        = { showCurrencyDialog = false },
            onCurrencySelected = { settingsViewModel.saveCurrency(it.code); showCurrencyDialog = false }
        )
    }
}

// ─── Masonry card components ──────────────────────────────────────────────────

@Composable
private fun MasonryLabel(text: String) {
    Text(
        text          = text.uppercase(),
        style         = MaterialTheme.typography.labelSmall,
        fontWeight    = FontWeight.Bold,
        color         = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
        letterSpacing = 0.8.sp,
        modifier      = Modifier.padding(start = 2.dp, top = 6.dp, bottom = 2.dp)
    )
}

/**
 * Standard masonry card — variable height depending on content.
 * Used in the 2-column grid; height adapts to subtitle length and extra content.
 */
@Composable
private fun MasonryCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badge: String? = null,
    extra: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    Card(
        modifier  = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        border    = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier            = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(38.dp).clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                }
                badge?.let {
                    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                        Text(it, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                    }
                }
            }
            Text(title,    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,  color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp)
            extra?.invoke()
        }
    }
}

/**
 * Wide masonry card — spans full width (use with StaggeredGridItemSpan.FullLine).
 * Shows a horizontal layout: icon left + text right + optional badge.
 */
@Composable
private fun MasonryCardWide(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badgeText: String? = null,
    badgeGreen: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier  = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        border    = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier.size(42.dp).clip(RoundedCornerShape(13.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title,    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall,  color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp)
            }
            badgeText?.let {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (badgeGreen) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        it,
                        style      = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color      = if (badgeGreen) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary,
                        modifier   = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}