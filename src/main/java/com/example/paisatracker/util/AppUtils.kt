package com.example.paisatracker.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import kotlin.system.exitProcess

object AppUtils {
    fun restartApp(context: Context) {
        val packageManager: PackageManager = context.packageManager
        val intent: Intent? = packageManager.getLaunchIntentForPackage(context.packageName)
        val componentName = intent?.component
        val mainIntent = Intent.makeRestartActivityTask(componentName)
        context.startActivity(mainIntent)
        exitProcess(0)
    }
}
