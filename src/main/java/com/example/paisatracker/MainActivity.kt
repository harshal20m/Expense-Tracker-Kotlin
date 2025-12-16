package com.example.paisatracker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.example.paisatracker.data.AppLockPreferences
import com.example.paisatracker.ui.applock.AppLockScreen
import com.example.paisatracker.ui.main.MainApp
import com.example.paisatracker.ui.theme.PaisaTrackerTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    private val viewModel: PaisaTrackerViewModel by viewModels {
        PaisaTrackerViewModelFactory((application as PaisaTrackerApplication).repository)
    }

    private val appLockPrefs by lazy { AppLockPreferences.getInstance(this) }
    private var isUnlocked by mutableStateOf(false)
    private var isFinishing = false

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            requestBatteryOptimizationExemption()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            lifecycleScope.launch {
                val isAppLockEnabled = appLockPrefs.isAppLockEnabled.first()
                isUnlocked = !isAppLockEnabled
            }
        }

        requestNotificationPermission()

        setContent {
            PaisaTrackerTheme {
                AppContent()
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

    override fun onResume() {
        super.onResume()
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
