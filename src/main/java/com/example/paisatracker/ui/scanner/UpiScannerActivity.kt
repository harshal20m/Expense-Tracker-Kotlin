package com.example.paisatracker.ui.scanner

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import com.example.paisatracker.PaisaTrackerApplication
import com.example.paisatracker.data.ThemePreferencesRepository
import com.example.paisatracker.ui.theme.PaisaTrackerTheme
import com.example.paisatracker.util.UpiCallbackResult
import com.example.paisatracker.util.UpiQrData
import com.example.paisatracker.util.buildUpiPayIntent
import com.example.paisatracker.util.parseUpiCallbackResponse
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class UpiScannerActivity : FragmentActivity() {

    private val viewModel: UpiScannerViewModel by viewModels {
        UpiScannerViewModelFactory(
            (application as PaisaTrackerApplication).repository
        )
    }

    private val themePrefs by lazy { ThemePreferencesRepository.getInstance(this) }

    // ── Launchers ─────────────────────────────────────────────────────────────

    /** Result from GPay/PhonePe/etc after P2M payment */
    private val upiPaymentLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult -> handleUpiResult(result) }

    /** Image picker — user manually picks a receipt screenshot */
    private val receiptPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.onReceiptImageShared(this, it) }
    }

    // ── State held between scan and callback ──────────────────────────────────
    private var currentQrData: UpiQrData? = null

    // ─────────────────────────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        super.onCreate(savedInstanceState)

        val initialTheme = runBlocking { themePrefs.appTheme.first() }

        // ── Handle ACTION_SEND (user shared a screenshot from GPay/PhonePe) ──
        if (intent?.action == Intent.ACTION_SEND &&
            intent.type?.startsWith("image/") == true
        ) {
            val sharedUri: Uri? = intent.getParcelableExtra(Intent.EXTRA_STREAM)
            sharedUri?.let { viewModel.onReceiptImageShared(this, it) }
        }

        setContent {
            val currentTheme by themePrefs.appTheme.collectAsState(initial = initialTheme)

            PaisaTrackerTheme(appTheme = currentTheme) {
                val uiState by viewModel.uiState.collectAsState()

                // Fire UPI intent when ViewModel signals PaymentLaunching
                LaunchedEffect(uiState) {
                    if (uiState is ScannerUiState.PaymentLaunching) {
                        currentQrData?.let { qr ->
                            val amount = viewModel.amountText.value.toDoubleOrNull() ?: return@let
                            launchUpiPayment(qr, amount)
                        }
                    }
                }

                UpiScannerScreen(
                    viewModel       = viewModel,
                    onStartScan     = { startZXingScanner() },
                    onPickReceipt   = { receiptPickerLauncher.launch("image/*") },
                    onClose         = { finish() },
                    onP2mPayAndRecord = { qrData ->
                        currentQrData = qrData
                        viewModel.onP2mPayAndRecord(qrData) { _, _ -> /* IDs stored in VM */ }
                    },
                    onReceiptSave   = { parsed, imageUri ->
                        viewModel.onReceiptSave(this, parsed, imageUri) { expenseId ->
                            Toast.makeText(
                                this,
                                "Expense saved! ✓",
                                Toast.LENGTH_SHORT
                            ).show()
                            finish()
                        }
                    }
                )
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  ZXing scanner
    // ─────────────────────────────────────────────────────────────────────────

    private fun startZXingScanner() {
        IntentIntegrator(this).apply {
            setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
            setPrompt("Scan a UPI QR code")
            setCameraId(0)
            setBeepEnabled(true)
            setBarcodeImageEnabled(false)
            setOrientationLocked(true)
            setCaptureActivity(PortraitCaptureActivity::class.java)
            initiateScan()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == IntentIntegrator.REQUEST_CODE) {
            val scanResult: IntentResult? =
                IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
            if (scanResult?.contents != null) {
                viewModel.onQrScanned(scanResult.contents, this)
                return
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  P2M UPI intent
    // ─────────────────────────────────────────────────────────────────────────

    private fun launchUpiPayment(qrData: UpiQrData, amount: Double) {
        val upiIntent = buildUpiPayIntent(this, qrData.rawUri, amount)

        // Check a UPI app is available via base intent (chooser always resolves)
        val probe = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(
            "upi://pay?pa=${qrData.vpa}&am=1.00&cu=INR"
        ))
        if (probe.resolveActivity(packageManager) == null) {
            Toast.makeText(
                this,
                "No UPI app found. Install GPay, PhonePe or Paytm.",
                Toast.LENGTH_LONG
            ).show()
            viewModel.uiState.value = ScannerUiState.Error("No UPI app found on this device.")
            return
        }

        upiPaymentLauncher.launch(upiIntent)
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  P2M result callback
    // ─────────────────────────────────────────────────────────────────────────

    private fun handleUpiResult(result: ActivityResult) {
        val callbackResult: UpiCallbackResult = parseUpiCallbackResponse(result.data)
        val qrData = currentQrData ?: run {
            viewModel.uiState.value = ScannerUiState.Error(
                "Lost payment context. Check your UPI app for status."
            )
            return
        }

        if (result.resultCode == Activity.RESULT_CANCELED && callbackResult.rawData.isBlank()) {
            Toast.makeText(this, "Payment cancelled.", Toast.LENGTH_SHORT).show()
            viewModel.uiState.value = ScannerUiState.P2mConfirm(qrData)
            return
        }

        viewModel.onP2mPaymentResult(callbackResult, qrData)
    }
}