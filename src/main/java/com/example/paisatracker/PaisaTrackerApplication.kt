package com.example.paisatracker

import android.app.Application
import android.content.Context
import androidx.work.*
import com.example.paisatracker.data.*
import com.example.paisatracker.util.CurrentCurrency
import com.example.paisatracker.util.ExpenseReminderWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

class PaisaTrackerApplication : Application() {
    val database: PaisaTrackerDatabase by lazy { PaisaTrackerDatabase.getDatabase(this) }
    val repository: PaisaTrackerRepository by lazy {
        PaisaTrackerRepository(
            database.projectDao(),
            database.categoryDao(),
            database.expenseDao(),
            database.assetDao(),
            database.backupDao()
        )
    }

    val themePreferencesRepository: ThemePreferencesRepository by lazy {
        ThemePreferencesRepository.getInstance(applicationContext)
    }

    val currencyPreferencesRepository: CurrencyPreferencesRepository by lazy {
        CurrencyPreferencesRepository(applicationContext)
    }

    override fun onCreate() {
        super.onCreate()
        scheduleDailyReminder()

        // Initialize currency from preferences
        CoroutineScope(Dispatchers.IO).launch {
            currencyPreferencesRepository.selectedCurrency.collect { currency ->
                CurrentCurrency.set(currency)
            }
        }
    }

    private fun scheduleDailyReminder() {
        val sharedPrefs = getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
        val isEnabled = sharedPrefs.getBoolean("notification_enabled", true)
        val notificationHour = sharedPrefs.getInt("notification_hour", 20)

        val workManager = WorkManager.getInstance(applicationContext)

        if (!isEnabled) {
            workManager.cancelUniqueWork("daily_expense_reminder")
            return
        }

        val currentTime = Calendar.getInstance()
        val targetTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, notificationHour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (currentTime.after(targetTime)) {
            targetTime.add(Calendar.DAY_OF_MONTH, 1)
        }

        val initialDelay = targetTime.timeInMillis - currentTime.timeInMillis

        val reminderRequest = PeriodicWorkRequestBuilder<ExpenseReminderWorker>(
            repeatInterval = 24,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(false)
                    .build()
            )
            .addTag("expense_reminder")
            .build()

        workManager.enqueueUniquePeriodicWork(
            "daily_expense_reminder",
            ExistingPeriodicWorkPolicy.REPLACE,
            reminderRequest
        )
    }
}