package com.example.paisatracker.ui.scanner

import android.content.pm.ActivityInfo
import android.os.Bundle
import com.journeyapps.barcodescanner.CaptureActivity

/**
 * Forces ZXing's camera scanner into portrait orientation.
 *
 * ZXing's default [CaptureActivity] reads the orientation from the calling
 * IntentIntegrator and defaults to landscape when setOrientationLocked(true)
 * is called without specifying which orientation to lock to.
 *
 * By subclassing CaptureActivity and setting requestedOrientation here,
 * we guarantee portrait regardless of device rotation.
 *
 * Register this in AndroidManifest.xml:
 *
 *   <activity
 *       android:name=".ui.scanner.PortraitCaptureActivity"
 *       android:exported="false"
 *       android:screenOrientation="portrait"
 *       android:stateNotNeeded="true"
 *       android:theme="@style/zxing_CaptureTheme"
 *       android:windowSoftInputMode="stateAlwaysHidden" />
 */
class PortraitCaptureActivity : CaptureActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        super.onCreate(savedInstanceState)
    }
}