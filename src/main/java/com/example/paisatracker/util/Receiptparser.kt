package com.example.paisatracker.util

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// ─── Parsed receipt model ─────────────────────────────────────────────────────

data class ParsedReceipt(
    val amount: Double?,
    val payeeName: String,      // person/merchant name — used as expense note
    val payeeVpa: String,       // e.g. rohit@oksbi
    val utrNumber: String,      // 12-digit bank UTR / txn ref
    val paymentApp: String,     // "GPay" | "PhonePe" | "Paytm" | "Kotak" | "Jupiter" | etc.
    val transactionDate: String,// e.g. "12 Apr 2025" — extracted if present
    val status: String,         // "Success" | "Failed" | "Pending"
    val rawText: String         // full OCR output for debugging
)

// ─── ML Kit OCR ───────────────────────────────────────────────────────────────

suspend fun extractTextFromImage(context: Context, imageUri: Uri): String =
    suspendCancellableCoroutine { cont ->
        try {
            val image = InputImage.fromFilePath(context, imageUri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val raw = visionText.textBlocks
                        .joinToString("\n") { block -> block.lines.joinToString("\n") { it.text } }
                    Log.d("RECEIPT_OCR", "Raw OCR:\n$raw")
                    cont.resume(raw)
                }
                .addOnFailureListener { cont.resumeWithException(it) }
        } catch (e: Exception) {
            cont.resumeWithException(e)
        }
    }

// ─── Main parser ──────────────────────────────────────────────────────────────

fun parseReceiptText(rawText: String): ParsedReceipt {

    // ── Step 1: Normalise OCR misreads ────────────────────────────────────────
    //
    // ML Kit commonly misreads:
    //   ₹  →  7, T, ?, 7, =, F  (depending on font rendering)
    //   ,  →  . or nothing      (thousands separator)
    //   o  →  0  in numeric contexts
    //
    // Strategy: work on a normalised copy for amount extraction,
    // but keep original for name/VPA extraction (names shouldn't be changed).

    val normalised = rawText
        // Fix ₹ misread as standalone "7" or "T" before digits
        // Pattern: line starts with 7 or T followed immediately by digits
        .replace(Regex("""(?m)^[7T]\s*(\d)"""), "₹$1")
        // Fix ₹ misread mid-sentence: space-7-digit or =7digit
        .replace(Regex("""[\s=]([7T])(\d{1,3}[,.]?\d)"""), " ₹$2")
        // Normalise ₹ symbol variants (Unicode lookalikes)
        .replace("₨", "₹")
        .replace("Rs.", "₹")
        .replace("Rs ", "₹")
        .replace("INR ", "₹")
        .replace("INR", "₹")
        // Remove thousands separator commas inside number sequences
        .replace(Regex("""(\d),(\d{3})"""), "$1$2")

    Log.d("RECEIPT_PARSE", "Normalised:\n$normalised")

    // ── Step 2: Detect payment app first (affects which patterns to prioritise) ──

    val paymentApp: String = when {
        rawText.contains("Google Pay",   ignoreCase = true) ||
                rawText.contains("GPay",         ignoreCase = true) ||
                // GPay's header on success screen
                rawText.contains("g.co/pay",     ignoreCase = true) -> "GPay"

        rawText.contains("PhonePe",      ignoreCase = true) ||
                rawText.contains("Phone Pe",     ignoreCase = true) -> "PhonePe"

        rawText.contains("Paytm",        ignoreCase = true) -> "Paytm"

        rawText.contains("BHIM",         ignoreCase = true) -> "BHIM"

        rawText.contains("SuperMoney",   ignoreCase = true) ||
                rawText.contains("Super Money",  ignoreCase = true) -> "SuperMoney"

        rawText.contains("Jupiter",      ignoreCase = true) -> "Jupiter"

        rawText.contains("Kotak",        ignoreCase = true) -> "Kotak"

        rawText.contains("Amazon Pay",   ignoreCase = true) -> "Amazon Pay"

        rawText.contains("CRED",         ignoreCase = true) -> "CRED"

        rawText.contains("Slice",        ignoreCase = true) -> "Slice"

        rawText.contains("Fi ",          ignoreCase = true) ||
                rawText.contains("fi money",     ignoreCase = true) -> "Fi"

        rawText.contains("Razorpay",     ignoreCase = true) -> "Razorpay"

        rawText.contains("NEFT",         ignoreCase = true) ||
                rawText.contains("IMPS",         ignoreCase = true) -> "Bank Transfer"

        else -> "UPI"
    }

    // ── Step 3: Amount extraction (from normalised text) ──────────────────────
    //
    // All major UPI app receipt formats:
    //
    // GPay:         "₹250"  (huge hero text, on its own line)
    //               "₹ 250.00"
    // PhonePe:      "Sent ₹250 to"  OR  "₹250.00\nSent"
    // Paytm:        "₹ 250.00\nPaid"  OR "You Paid ₹250"
    // SuperMoney:   "You paid ₹250"
    // Kotak/Jupiter:"Debited ₹250"  OR "Transferred ₹250"
    // BHIM:         "Amount\n₹250"
    // Amazon Pay:   "₹250 paid"
    //
    // We run patterns in priority order. The highest-confidence pattern wins.

    val amount: Double? = run {
        val patterns = listOf(
            // Highest priority: ₹ symbol immediately before digits (hero amount)
            // Handles: ₹250, ₹ 250, ₹250.00, ₹ 1250.50
            Regex("""₹\s*(\d[\d,]*(?:\.\d{1,2})?)"""),
            // "Sent/Paid/Debited/Transferred" + ₹ + amount
            Regex("""(?:sent|paid|debited|transferred|deducted|you paid|you sent)\s+₹\s*(\d[\d,]*(?:\.\d{1,2})?)""", RegexOption.IGNORE_CASE),
            // "Amount" label — may be on next line
            Regex("""[Aa]mount[\s:₹]*(\d[\d,]*(?:\.\d{1,2})?)"""),
            // "Total" label
            Regex("""[Tt]otal[\s:₹]*(\d[\d,]*(?:\.\d{1,2})?)"""),
            // Standalone decimal: looks like a rupee amount (≥ 2 digits before decimal)
            Regex("""(?<![.\d])(\d{2,7}\.\d{2})(?![.\d])""")
        )
        patterns.firstNotNullOfOrNull { pattern ->
            pattern.find(normalised)?.groupValues?.getOrNull(1)
                ?.replace(",", "")
                ?.toDoubleOrNull()
                ?.takeIf { it > 0.0 && it < 10_000_000.0 }
        }
    }

    // ── Step 4: Payee VPA ─────────────────────────────────────────────────────
    //
    // PSP handle list: all known Indian UPI PSP bank handles.
    // Keep this comprehensive — it's the most reliable way to find VPAs.

    val payeeVpa: String = run {
        val pspHandles = listOf(
            "okicici", "okhdfcbank", "okaxis", "oksbi",
            "ybl",     "ibl",        "axisb",  "upi",
            "paytm",   "paytmbank",  "freecharge", "apl",
            "barodampay", "rbl",     "kotak",  "indus",
            "juspay",  "pingpay",    "idfcbank","aubank",
            "federal", "idbi",       "cnrb",   "icici",
            "hdfc",    "sbi",        "axis",   "pnb",
            "bob",     "unionbank",  "cub",    "kvb",
            "dbs",     "sc",         "hsbc",   "citibank",
            "airtel",  "jio",        "flipkart","amazon",
            "nsdl",    "mahb",       "uco",    "vijb",
            "boi",     "obc",        "allbank", "andb",
            "syndicatebank", "canarabank", "centralbank",
            "indianbank", "iob", "psb", "bandhan",
            "equitas", "esaf", "ujjivan", "utbi",
            "slice",   "fampay",     "postbank", "superpe",
            "jupiter", "fi",         "niyobank"
        ).joinToString("|")

        Regex(
            """([A-Za-z0-9.\-_+]{2,64}@(?:$pspHandles))""",
            RegexOption.IGNORE_CASE
        ).find(rawText)?.value?.trim().orEmpty()
    }

    // ── Step 5: Payee Name ────────────────────────────────────────────────────
    //
    // This becomes the expense "note" so we want the human-readable name.
    //
    // App-specific patterns (checked before generic ones):
    //   GPay:       "Paid to\nName" or "Name\nphone number"
    //   PhonePe:    "Sent to Name"  or "To\nName"
    //   Paytm:      "to\nName"      or "Name\nSuccessfully"
    //   Kotak:      "Beneficiary: Name"
    //   SuperMoney: "Paid to Name"
    //   Jupiter:    "Transferred to Name"
    //   BHIM:       "Remitted to\nName"

    val payeeName: String = run {
        val patterns = listOf(
            Regex("""[Pp]aid\s+to\s*\n?\s*([A-Za-z][A-Za-z0-9 .&'\-]{1,40})"""),
            Regex("""[Ss]ent\s+to\s*\n?\s*([A-Za-z][A-Za-z0-9 .&'\-]{1,40})"""),
            Regex("""[Tt]ransferred\s+to\s*\n?\s*([A-Za-z][A-Za-z0-9 .&'\-]{1,40})"""),
            Regex("""[Rr]emitted\s+to\s*\n?\s*([A-Za-z][A-Za-z0-9 .&'\-]{1,40})"""),
            Regex("""[Bb]eneficiary[:\s]+([A-Za-z][A-Za-z0-9 .&'\-]{1,40})"""),
            Regex("""[Tt]o\s*\n\s*([A-Za-z][A-Za-z0-9 .&'\-]{2,40})"""),
            Regex("""[Pp]ayee[:\s]+([A-Za-z][A-Za-z0-9 .&'\-]{1,40})"""),
            Regex("""[Rr]eceiver[:\s]+([A-Za-z][A-Za-z0-9 .&'\-]{1,40})"""),
            Regex("""[Mm]erchant[:\s]+([A-Za-z][A-Za-z0-9 .&'\-]{1,40})"""),
        )
        patterns.firstNotNullOfOrNull { p ->
            p.find(rawText)?.groupValues?.getOrNull(1)
                ?.trim()
                ?.takeIf { it.length >= 2 && !it.startsWith("@") && !it.all { c -> c.isDigit() } }
        }
        // Fallback: clean up VPA localpart as display name
            ?: payeeVpa.substringBefore("@")
                .replace(Regex("[._\\-]+"), " ")
                .trim()
                .replaceFirstChar { it.uppercase() }
                .ifBlank { "Payment" }
    }

    // ── Step 6: UTR / Transaction reference ───────────────────────────────────

    val utrNumber: String = run {
        val patterns = listOf(
            Regex("""[Uu][Tt][Rr][:\s#No.]*([A-Za-z0-9]{10,22})"""),
            Regex("""[Tt]ransaction\s*[Ii][Dd][:\s#]*([A-Za-z0-9]{8,22})"""),
            Regex("""[Tt]xn\s*(?:[Ii][Dd]|[Nn][Oo]?)[:\s#]*([A-Za-z0-9]{8,22})"""),
            Regex("""[Rr]ef(?:erence)?\s*(?:[Nn][Oo]?|#)[:\s]*([A-Za-z0-9]{8,22})"""),
            Regex("""[Oo]rder\s*[Ii][Dd][:\s#]*([A-Za-z0-9]{8,22})"""),
            // PhonePe transaction ID (starts with T followed by digits)
            Regex("""(?<![A-Za-z0-9])(T[0-9]{11,18})(?![A-Za-z0-9])"""),
            // GPay order ID (starts with CICAgto or similar Google prefix)
            Regex("""(?<![A-Za-z0-9])(CICAG[A-Za-z0-9]{10,20})(?![A-Za-z0-9])"""),
            // Standalone 12-digit bank UTR
            Regex("""(?<!\d)(\d{12})(?!\d)"""),
            // Standalone uppercase alphanumeric 15–22 chars (Paytm/PhonePe)
            Regex("""(?<![A-Za-z0-9])([A-Z][A-Z0-9]{14,21})(?![A-Za-z0-9])""")
        )
        patterns.firstNotNullOfOrNull { p ->
            p.find(rawText)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.length >= 8 }
        }.orEmpty()
    }

    // ── Step 7: Date ──────────────────────────────────────────────────────────

    val transactionDate: String = run {
        val patterns = listOf(
            // "12 Apr 2025" or "12 April 2025"
            Regex("""(\d{1,2}\s+(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\.?\s+\d{4})""", RegexOption.IGNORE_CASE),
            // "Apr 12, 2025" or "April 12 2025"
            Regex("""((?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\.?\s+\d{1,2},?\s+\d{4})""", RegexOption.IGNORE_CASE),
            // "12/04/2025" or "2025-04-12"
            Regex("""(\d{2}[/\-]\d{2}[/\-]\d{2,4})"""),
            // "Today" or "Yesterday" (PhonePe/GPay often show relative dates)
            Regex("""(Today|Yesterday)""", RegexOption.IGNORE_CASE)
        )
        patterns.firstNotNullOfOrNull { p -> p.find(rawText)?.groupValues?.getOrNull(1)?.trim() }.orEmpty()
    }

    // ── Step 8: Status ────────────────────────────────────────────────────────

    val status: String = when {
        rawText.contains("successful", ignoreCase = true) ||
                rawText.contains("success",    ignoreCase = true) ||
                rawText.contains("completed",  ignoreCase = true) ||
                rawText.contains("approved",   ignoreCase = true) ||
                rawText.contains("done",       ignoreCase = true)  -> "Success"

        rawText.contains("failed",   ignoreCase = true) ||
                rawText.contains("failure",  ignoreCase = true) ||
                rawText.contains("declined", ignoreCase = true)    -> "Failed"

        rawText.contains("pending",  ignoreCase = true) ||
                rawText.contains("processing", ignoreCase = true)  -> "Pending"

        else -> "Success"   // assume success if shared from UPI app
    }

    Log.d("RECEIPT_PARSE", "App=$paymentApp | Amount=$amount | Name=$payeeName | VPA=$payeeVpa | UTR=$utrNumber | Date=$transactionDate | Status=$status")

    return ParsedReceipt(
        amount          = amount,
        payeeName       = payeeName,
        payeeVpa        = payeeVpa,
        utrNumber       = utrNumber,
        paymentApp      = paymentApp,
        transactionDate = transactionDate,
        status          = status,
        rawText         = rawText
    )
}