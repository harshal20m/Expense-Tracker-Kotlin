package com.example.paisatracker.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

// ─── Parsed data from a UPI QR code ──────────────────────────────────────────

data class UpiQrData(
    val vpa: String,
    val payeeName: String,
    val amount: Double?,
    val note: String,
    val merchantCode: String,
    val currency: String,
    val isAmountLocked: Boolean,
    val isMerchantQr: Boolean,
    val rawUri: String
)

fun parseUpiQr(raw: String): UpiQrData? {
    val cleaned = raw.trim()

    val queryString = when {
        cleaned.startsWith("upi://pay?", ignoreCase = true) ->
            cleaned.substringAfter("upi://pay?")
        cleaned.startsWith("upi://", ignoreCase = true) ->
            cleaned.substringAfter("upi://").substringAfter("?")
        cleaned.contains("pa=") ->
            cleaned.substringAfter("?").ifBlank { cleaned }
        else -> return null
    }

    val params = queryString
        .split("&")
        .mapNotNull { part ->
            val idx = part.indexOf('=')
            if (idx < 0) null
            else part.substring(0, idx).trim().lowercase() to
                    Uri.decode(part.substring(idx + 1).trim())
        }
        .toMap()

    val vpa = params["pa"]?.takeIf { it.isNotBlank() } ?: return null
    val amountStr = params["am"]
    val amount = amountStr?.toDoubleOrNull()
    val merchantCode = params["mc"] ?: ""

    // P2M detection: mc= present OR VPA ends in known merchant PSP suffixes
    val isMerchantQr = merchantCode.isNotBlank()
            || params["mode"] == "02"
            || vpa.endsWith("@idfcbank")
            || vpa.endsWith("@axisb")
            || vpa.endsWith("@upi")
            || vpa.contains("merchant")
            || vpa.contains("store")
            || vpa.contains("shop")

    return UpiQrData(
        vpa            = vpa,
        payeeName      = params["pn"] ?: "",
        amount         = amount,
        note           = params["tn"] ?: "",
        merchantCode   = merchantCode,
        currency       = params["cu"] ?: "INR",
        isAmountLocked = amountStr != null && amount != null && amount > 0,
        isMerchantQr   = isMerchantQr,
        rawUri         = cleaned
    )
}

// ─── Build the UPI payment Intent ────────────────────────────────────────────

fun buildUpiPayIntent(
    context: Context,
    scannedUri: String,
    userEnteredAmount: Double
): Intent {
    val qrData = parseUpiQr(scannedUri)
    val amountFormatted = String.format(java.util.Locale.US, "%.2f", userEnteredAmount)

    var finalUriString = scannedUri.trim()

    // 1. Trick the bank into thinking this is a standard Intent (clean up camera modes)
    finalUriString = finalUriString
        .replace(Regex("&?mode=[^&]*"), "")
        .replace(Regex("&?qrmedium=[^&]*"), "")

    val modeSeparator = if (finalUriString.contains("?")) "&" else "?"
    finalUriString += "${modeSeparator}mode=04"

    // 2. Smart Routing: Only inject amount for Merchants
    if (qrData?.isMerchantQr == true) {
        if (!finalUriString.contains("&am=") && !finalUriString.contains("?am=")) {
            finalUriString += "&am=$amountFormatted"
        }
    } else {
        // P2P QR detected. We DO NOT inject `am=` or `tr=` to prevent the bank fraud filter from rejecting it.
        Log.d("UPI_DEBUG", "P2P QR detected. Skipping 'am=' to prevent bank rejection.")
    }

    // 3. Safely append a note if one doesn't exist
    if (!finalUriString.contains("&tn=") && !finalUriString.contains("?tn=")) {
        val encodedNote = URLEncoder.encode("PaisaTracker Expense", StandardCharsets.UTF_8.name()).replace("+", "%20")
        finalUriString += "&tn=$encodedNote"
    }

    Log.d("UPI_DEBUG", "Generated Safe UPI URI: $finalUriString")

    val upiUri = Uri.parse(finalUriString)
    val payIntent = Intent(Intent.ACTION_VIEW, upiUri)

    return Intent.createChooser(payIntent, "Pay via UPI")
}

// ─── Parse UPI callback response ─────────────────────────────────────────────

data class UpiCallbackResult(
    val status: String,
    val transactionId: String?,
    val responseCode: String?,
    val approvalRefNo: String?,
    val rawData: String
)

fun parseUpiCallbackResponse(data: Intent?): UpiCallbackResult {
    val raw = data?.getStringExtra("response") ?: ""

    val params = raw.split("&")
        .mapNotNull { p ->
            val i = p.indexOf('=')
            if (i < 0) null
            else p.substring(0, i).uppercase().trim() to p.substring(i + 1).trim()
        }
        .toMap()

    val statusRaw = params["STATUS"]
        ?: params["PAYMENTSTATUS"]
        ?: params["TXNSTATUS"]
        ?: ""

    val status = when {
        statusRaw.contains("SUCCESS",   ignoreCase = true) -> "SUCCESS"
        statusRaw.contains("SUBMITTED", ignoreCase = true) -> "SUBMITTED"
        else -> "FAILURE"
    }

    val txnId = params["TXNID"]
        ?: params["TRANSACTIONID"]
        ?: params["TXN_ID"]

    val responseCode = params["RESPONSECODE"]
        ?: params["RESPONSE_CODE"]
        ?: params["CODE"]

    val approvalRef = params["APPROVALREFNO"]
        ?: params["APPROVAL_REF_NO"]

    return UpiCallbackResult(
        status        = status,
        transactionId = txnId,
        responseCode  = responseCode,
        approvalRefNo = approvalRef,
        rawData       = raw
    )
}