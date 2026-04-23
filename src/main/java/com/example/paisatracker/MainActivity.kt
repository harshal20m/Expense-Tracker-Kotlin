package com.example.paisatracker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.example.paisatracker.data.AppLockPreferences
import com.example.paisatracker.data.AppTheme
import com.example.paisatracker.data.DataSeeder
import com.example.paisatracker.data.EmojiPreferencesRepository
import com.example.paisatracker.data.ThemePreferencesRepository
import com.example.paisatracker.ui.applock.AppLockScreen
import com.example.paisatracker.ui.main.MainApp
import com.example.paisatracker.ui.setup.FirstTimeSetupDialog
import com.example.paisatracker.ui.theme.PaisaTrackerTheme
import com.example.paisatracker.util.CurrentCurrency
import com.example.paisatracker.util.UpdateManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    private val updateManager by lazy { UpdateManager(this) }

    private val viewModel: PaisaTrackerViewModel by viewModels {
        PaisaTrackerViewModelFactory(
            (application as PaisaTrackerApplication).repository,
            (application as PaisaTrackerApplication).currencyPreferencesRepository,
            EmojiPreferencesRepository.getInstance(this),
            updateManager
        )
    }

    private val appLockPrefs by lazy { AppLockPreferences.getInstance(this) }
    private val themePreferencesRepository by lazy { ThemePreferencesRepository.getInstance(this) }

    private val dataSeeder by lazy { DataSeeder.getInstance((application as PaisaTrackerApplication).repository) }
    private var isUnlocked by mutableStateOf(false)

    private var showFirstTimeSetup by mutableStateOf(false)
    private var isFinishing = false

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            requestBatteryOptimizationExemption()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            lifecycleScope.launch {
                val isAppLockEnabled = appLockPrefs.isAppLockEnabled.first()
                isUnlocked = !isAppLockEnabled

                // Check if first time setup should be shown
                showFirstTimeSetup = dataSeeder.shouldShowFirstTimeSetup(this@MainActivity)
            }
        }

        requestNotificationPermission()
        viewModel.checkForUpdates(isManual = false)

        setContent {
            val currentTheme by themePreferencesRepository.appTheme.collectAsState(initial = AppTheme.SYSTEM_DEFAULT)

            // Observe currency changes
            val currentCurrency by viewModel.currentCurrency.collectAsState()

            // Update CurrentCurrency singleton whenever currency changes
            LaunchedEffect(currentCurrency) {
                CurrentCurrency.set(currentCurrency)
            }

            PaisaTrackerTheme(appTheme = currentTheme) {
                // Show first-time setup dialog if needed
                if (showFirstTimeSetup && !isFinishing) {
                    FirstTimeSetupDialog(
                        onSetupComplete = { shouldSeed ->
                            lifecycleScope.launch {
                                dataSeeder.seedInitialDataIfUserAccepts(
                                    this@MainActivity,
                                    shouldSeed
                                )
                                showFirstTimeSetup = false
                            }
                        }
                    )} else {
                    AppContent()
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("was_unlocked", isUnlocked)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        isUnlocked = savedInstanceState.getBoolean("was_unlocked", false)
    }

    override fun onPause() {
        super.onPause()
        isFinishing = isFinishing()
    }

    @Composable
    private fun AppContent() {
        val isAppLockEnabled by appLockPrefs.isAppLockEnabled.collectAsState(initial = false)
        val isBiometricEnabled by appLockPrefs.isBiometricEnabled.collectAsState(initial = false)
        val pinCode by appLockPrefs.pinCode.collectAsState(initial = null)

        when {
            isAppLockEnabled && !isUnlocked && pinCode != null -> {
                AppLockScreen(
                    onUnlock = { isUnlocked = true },
                    correctPin = pinCode!!,
                    biometricEnabled = isBiometricEnabled
                )
            }
            else -> {
                MainApp(viewModel = viewModel)
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    requestBatteryOptimizationExemption()
                }
                else -> {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            requestBatteryOptimizationExemption()
        }
    }

    private fun requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent().apply {
                action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                data = Uri.parse("package:$packageName")
            }
            try {
                startActivity(intent)
            } catch (e: Exception) {
                // Silently fail
            }
        }
    }
}