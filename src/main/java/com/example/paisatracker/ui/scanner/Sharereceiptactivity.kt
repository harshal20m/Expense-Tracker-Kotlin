package com.example.paisatracker.ui.scanner

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity

/**
 * Minimal trampoline activity that receives shared receipt images from
 * GPay / PhonePe / any UPI app's native Share button.
 *
 * WHY a separate activity instead of putting ACTION_SEND on UpiScannerActivity:
 *
 *   1. UpiScannerActivity needs exported="false" for security — it contains
 *      payment logic and should not be launchable by other apps directly.
 *
 *   2. Play Protect is less suspicious of a clearly-named, minimal activity
 *      that only handles image sharing than a payment scanner that also
 *      declares itself as a share target.
 *
 *   3. Clean separation: this activity does ONE thing — receive an image URI
 *      and hand it to UpiScannerActivity. No business logic here.
 *
 * The flow:
 *   User taps Share in GPay → Android ShareSheet shows PaisaTracker →
 *   This activity receives the URI → starts UpiScannerActivity with the URI →
 *   finishes itself (no UI shown)
 */
class ShareReceiptActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent?.action == Intent.ACTION_SEND &&
            intent.type?.startsWith("image/") == true
        ) {
            val imageUri: Uri? = intent.getParcelableExtra(Intent.EXTRA_STREAM)

            if (imageUri != null) {
                // Forward to UpiScannerActivity with the receipt URI
                val scannerIntent = Intent(this, UpiScannerActivity::class.java).apply {
                    putExtra(EXTRA_SHARED_RECEIPT_URI, imageUri.toString())
                    // Don't start a new task — reuse existing UpiScannerActivity stack
                    addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                startActivity(scannerIntent)
            }
        }

        // Always finish immediately — no UI shown
        finish()
    }

    companion object {
        const val EXTRA_SHARED_RECEIPT_URI = "extra_shared_receipt_uri"
    }
}