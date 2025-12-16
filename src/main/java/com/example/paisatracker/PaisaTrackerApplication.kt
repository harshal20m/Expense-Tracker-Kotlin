package com.example.paisatracker

import android.app.Application
import android.content.Context
import androidx.work.*
import com.example.paisatracker.data.PaisaTrackerDatabase
import com.example.paisatracker.data.PaisaTrackerRepository
import com.example.paisatracker.util.ExpenseReminderWorker
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

    override fun onCreate() {
        super.onCreate()
        scheduleDailyReminder()
    }

    private fun scheduleDailyReminder() {
        val sharedPrefs = getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
        val isEnabled = sharedPrefs.getBoolean("notification_enabled", true)
        val notificationHour = sharedPrefs.getInt("notification_hour", 20)

        val workManager = WorkManager.getInstance(applicationContext)

        if (!isEnabled) {
            // Cancel notifications if disabled
            workManager.cancelUniqueWork("daily_expense_reminder")
            return
        }

        // Calculate next trigger time
        val currentTime = Calendar.getInstance()
        val targetTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, notificationHour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If target time has passed today, schedule for tomorrow
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
                    .setRequiresBatteryNotLow(false) // Changed to false for better reliability
                    .build()
            )
            .addTag("expense_reminder")
            .build()

        // Use REPLACE to update existing work with new schedule
        workManager.enqueueUniquePeriodicWork(
            "daily_expense_reminder",
            ExistingPeriodicWorkPolicy.REPLACE,
            reminderRequest
        )
    }
}