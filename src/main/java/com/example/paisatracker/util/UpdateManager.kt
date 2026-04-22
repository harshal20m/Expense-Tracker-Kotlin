package com.example.paisatracker.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.Keep
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.paisatracker.R
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.util.concurrent.atomic.AtomicBoolean

@Keep
data class GithubRelease(
    @SerializedName("tag_name") val tag_name: String,
    @SerializedName("html_url") val html_url: String,
    @SerializedName("body") val body: String
)

interface GithubService {
    @GET("repos/harshal20m/PaisaTracker/releases/latest")
    suspend fun getLatestRelease(): GithubRelease
}

class UpdateManager(private val context: Context) {
    private val TAG = "UpdateManager"

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.github.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val service = retrofit.create(GithubService::class.java)

    private val _updateAvailable = MutableStateFlow<GithubRelease?>(null)
    val updateAvailable: StateFlow<GithubRelease?> = _updateAvailable

    private val isChecking = AtomicBoolean(false)

    suspend fun checkForUpdates(isManual: Boolean = false) {
        if (isChecking.getAndSet(true)) return
        Log.d(TAG, "Checking for updates... (Manual: $isManual)")
        try {
            val latestRelease = withContext(Dispatchers.IO) {
                try {
                    val release = service.getLatestRelease()
                    Log.d(TAG, "Fetched latest release: ${release.tag_name}")
                    release
                } catch (e: Exception) {
                    Log.e(TAG, "Network error: ${e.message}")
                    null
                }
            } ?: return

            val currentVersion = getAppVersionName()
            Log.d(TAG, "Current Version: $currentVersion, Latest: ${latestRelease.tag_name}")

            if (isNewerVersion(currentVersion, latestRelease.tag_name)) {
                Log.d(TAG, "Newer version found!")
                _updateAvailable.value = latestRelease
                if (!isManual) {
                    showUpdateNotification(latestRelease)
                }
            } else {
                Log.d(TAG, "No update needed.")
                if (isManual) {
                    _updateAvailable.value = null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Update check failed: ${e.message}")
            e.printStackTrace()
        } finally {
            isChecking.set(false)
        }
    }

    fun dismissUpdate() {
        _updateAvailable.value = null
    }

    private fun getAppVersionName(): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0"
        } catch (e: Exception) {
            "1.0"
        }
    }

    private fun isNewerVersion(current: String, latest: String): Boolean {
        // Clean up versions (e.g., "v2.0" -> "2.0")
        val currentClean = current.replace(Regex("[^0-9.]"), "")
        val latestClean = latest.replace(Regex("[^0-9.]"), "")

        val currentParts = currentClean.split(".").mapNotNull { it.toIntOrNull() }
        val latestParts = latestClean.split(".").mapNotNull { it.toIntOrNull() }

        val maxLength = maxOf(currentParts.size, latestParts.size)
        for (i in 0 until maxLength) {
            val curr = currentParts.getOrElse(i) { 0 }
            val late = latestParts.getOrElse(i) { 0 }
            if (late > curr) return true
            if (late < curr) return false
        }
        return false
    }

    private fun showUpdateNotification(release: GithubRelease) {
        val channelId = "app_updates"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "App Updates"
            val descriptionText = "Notifications for new app versions"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(release.html_url))
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_download_done) // Replace with your app icon
            .setContentTitle("Update Available: ${release.tag_name}")
            .setContentText("A new version of PaisaTracker is available. Tap to download.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            with(NotificationManagerCompat.from(context)) {
                notify(1001, builder.build())
            }
        } catch (e: SecurityException) {
            // Handle missing notification permission on Android 13+
        }
    }
}
