package com.example.paisatracker.util

import android.util.Log

/** Lightweight model for data parsed from a single notification/SMS line */
data class NotificationTransaction(
    val amount: Double,
    val payeeName: String,
    val payeeVpa: String,
    val utrNumber: String,
    val sourceApp: String,
    val status: String,
    val transactionDate: String
)

/**
 * Parses a UPI payment notification or bank SMS into a [NotificationTransaction].
 *
 * Returns null if the text doesn't look like a UPI payment notification.
 *
 * Tested notification formats:
 *
 * GPay:
 *   "You paid ₹250 to Rahul Kumar"
 *   "Payment of ₹1,240 to DMart successful"
 *   "₹500 sent to rahul@oksbi"
 *
 * PhonePe:
 *   "₹250.00 sent to Rahul Kumar via UPI"
 *   "Payment successful! ₹1240 paid to DMart"
 *
 * Paytm:
 *   "₹250 paid to Rahul Kumar. UPI Ref: 423891234567"
 *   "Money transferred ₹500 to rahul@oksbi"
 *
 * Bank SMS (Kotak/HDFC/SBI/ICICI etc.):
 *   "Your a/c XX1234 is debited with INR 500.00 on 12-Apr. UPI Ref 423891234567"
 *   "Sent Rs.250 to rahul@oksbi on 12/04. Ref no 423891234567"
 *   "INR 1,240.00 debited from Kotak a/c XX5678. UPI/P2M/423891234567/DMart"
 *   "Debited INR 580 for UPI txn. Ref 423891234567. Avbl bal INR 42,000"
 */
fun parseNotificationText(text: String, packageName: String): NotificationTransaction? {
    if (text.isBlank()) return null

    // ── Quick filter: must mention a payment action ───────────────────────────
    val lc = text.lowercase()
    val isPaymentNotification = lc.contains("paid") || lc.contains("sent") ||
        lc.contains("debit") || lc.contains("transfer") || lc.contains("payment") ||
        lc.contains("₹") || lc.contains("inr") || lc.contains("rs.")
    if (!isPaymentNotification) return null

    // ── Source app ────────────────────────────────────────────────────────────
    val sourceApp = when (packageName) {
        "com.google.android.apps.nbu.paisa.user" -> "GPay"
        "com.phonepe.app"                         -> "PhonePe"
        "net.one97.paytm"                         -> "Paytm"
        "in.org.npci.upiapp"                      -> "BHIM"
        "in.amazon.mShop.android.shopping"        -> "Amazon Pay"
        else -> when {
            lc.contains("kotak")   -> "Kotak"
            lc.contains("hdfc")    -> "HDFC"
            lc.contains("icici")   -> "ICICI"
            lc.contains("sbi")     -> "SBI"
            lc.contains("axis")    -> "Axis"
            lc.contains("phonepe") -> "PhonePe"
            lc.contains("gpay") || lc.contains("google pay") -> "GPay"
            lc.contains("paytm")   -> "Paytm"
            else -> "Bank SMS"
        }
    }

    // ── Normalise text for parsing ────────────────────────────────────────────
    val normalised = text
        .replace(Regex("""(?m)^[7T]\s*(\d)"""), "₹$1")
        .replace(Regex("""[\s=]([7T])(\d{1,3}[,.]?\d)"""), " ₹$2")
        .replace("₨", "₹").replace("Rs.", "₹").replace("Rs ", "₹")
        .replace("INR ", "₹").replace("INR", "₹")
        .replace(Regex("""(\d),(\d{3})"""), "$1$2")

    // ── Amount ────────────────────────────────────────────────────────────────
    //
    // Notification amount patterns — ordered by confidence:
    //  1. "₹250"  "₹ 1250.00"  (₹ directly before digits)
    //  2. "paid/sent/debited ₹250"
    //  3. Standalone decimal "1250.00"
    //
    // Key insight: notification text is SHORT (< 150 chars) so there are
    // far fewer false positives than in a full receipt screenshot.

    val amount: Double = run {
        val patterns = listOf(
            Regex("""₹\s*(\d[\d,]*(?:\.\d{1,2})?)"""),
            Regex("""(?:paid|sent|debited?|transferred?|deducted)\s+₹\s*(\d[\d,]*(?:\.\d{1,2})?)""", RegexOption.IGNORE_CASE),
            Regex("""(?<![.\d])(\d{2,7}\.\d{2})(?![.\d])""")
        )
        // Collect ALL matches, pick the largest (hero amount, not a version number)
        val candidates = mutableListOf<Double>()
        patterns.forEach { p ->
            p.findAll(normalised).forEach { m ->
                m.groupValues.getOrNull(1)
                    ?.replace(",", "")
                    ?.toDoubleOrNull()
                    ?.takeIf { it > 0.0 && it < 10_000_000.0 }
                    ?.let { candidates.add(it) }
            }
        }
        // Pick the largest candidate (payment amount > version numbers/percentages)
        candidates.maxOrNull() ?: return null
    }

    // ── Payee VPA ─────────────────────────────────────────────────────────────
    val pspHandles = "okicici|okhdfcbank|okaxis|oksbi|ybl|ibl|axisb|upi|paytm|paytmbank|freecharge|apl|rbl|kotak|indus|idfcbank|aubank|federal|idbi|icici|hdfc|sbi|axis|pnb|bob|unionbank|airtel|jio|amazon|nsdl|boi|bandhan|equitas|slice|jupiter|fi"
    val payeeVpa = Regex("""([A-Za-z0-9.\-_+]{2,64}@(?:$pspHandles))""", RegexOption.IGNORE_CASE)
        .find(text)?.value?.trim().orEmpty()

    // ── Payee Name ────────────────────────────────────────────────────────────
    val payeeName: String = run {
        val patterns = listOf(
            Regex("""(?:paid|sent|transferred)\s+(?:to|₹[^t]+to)\s+([A-Za-z][A-Za-z0-9 .&'\-]{1,35})""", RegexOption.IGNORE_CASE),
            Regex("""to\s+([A-Za-z][A-Za-z0-9 .&'\-]{2,35})\s*(?:via|on|\.|,|$)""", RegexOption.IGNORE_CASE),
            Regex("""UPI/P2[MP]/\d+/([A-Za-z][A-Za-z0-9 .&'\-]{1,30})"""),
        )
        patterns.firstNotNullOfOrNull { p ->
            p.find(text)?.groupValues?.getOrNull(1)
                ?.trim()
                ?.takeIf { it.length >= 2 && !it.all { c -> c.isDigit() } }
        } ?: payeeVpa.substringBefore("@").replace(Regex("[._\\-]+"), " ").trim()
             .replaceFirstChar { it.uppercase() }.ifBlank { "Payment" }
    }

    // ── UTR / Transaction reference ───────────────────────────────────────────
    val utrNumber: String = run {
        val patterns = listOf(
            Regex("""[Uu][Tt][Rr][:\s#No.]*([A-Za-z0-9]{10,18})"""),
            Regex("""[Rr]ef(?:erence)?\s*(?:[Nn][Oo]?\.?|#)[:\s]*([A-Za-z0-9]{8,18})"""),
            Regex("""[Tt]xn\s*(?:[Ii][Dd]|[Nn][Oo]?)[:\s#]*([A-Za-z0-9]{8,18})"""),
            Regex("""(?<!\d)(\d{12})(?!\d)"""),
            Regex("""(?<![A-Za-z0-9])(T[0-9]{11,15})(?![A-Za-z0-9])""")
        )
        patterns.firstNotNullOfOrNull { p ->
            p.find(text)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.length >= 8 }
        }.orEmpty()
    }

    // ── Date ──────────────────────────────────────────────────────────────────
    val transactionDate: String = run {
        listOf(
            Regex("""(\d{1,2}[-/]\d{1,2}(?:[-/]\d{2,4})?)"""),
            Regex("""(\d{1,2}\s+(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*)""", RegexOption.IGNORE_CASE),
            Regex("""(Today|Yesterday)""", RegexOption.IGNORE_CASE)
        ).firstNotNullOfOrNull { it.find(text)?.groupValues?.getOrNull(1)?.trim() }.orEmpty()
    }

    // ── Status ────────────────────────────────────────────────────────────────
    val status = when {
        lc.contains("fail") || lc.contains("decline") || lc.contains("unsuccessful") -> "Failed"
        lc.contains("pending") || lc.contains("processing") -> "Pending"
        else -> "Success"
    }

    Log.d("NOTIF_PARSE", "App=$sourceApp | ₹$amount | $payeeName | UTR=$utrNumber")

    return NotificationTransaction(
        amount          = amount,
        payeeName       = payeeName,
        payeeVpa        = payeeVpa,
        utrNumber       = utrNumber,
        sourceApp       = sourceApp,
        status          = status,
        transactionDate = transactionDate
    )
}
