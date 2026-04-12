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

data class ParsedReceipt(
    val amount: Double?,
    val payeeName: String,
    val payeeVpa: String,
    val utrNumber: String,
    val paymentApp: String,
    val transactionDate: String,
    val status: String,
    val rawText: String
)

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

fun parseReceiptText(rawText: String): ParsedReceipt {

    // ── Step 1: Detect app FIRST ──────────────────────────────────────────────
    // This drives which patterns we trust most.
    val lc = rawText.lowercase()

    val paymentApp: String = when {
        lc.contains("google pay") || lc.contains("gpay") || lc.contains("g.co/pay") -> "GPay"
        lc.contains("phonepe") || lc.contains("phone pe") -> "PhonePe"
        lc.contains("paytm") -> "Paytm"
        lc.contains("bhim") -> "BHIM"
        lc.contains("supermoney") || lc.contains("super money") -> "SuperMoney"
        lc.contains("jupiter") -> "Jupiter"
        lc.contains("kotak") -> "Kotak"
        lc.contains("amazon pay") -> "Amazon Pay"
        lc.contains("cred") -> "CRED"
        lc.contains("slice") -> "Slice"
        lc.contains("fi ") || lc.contains("fi money") -> "Fi"
        lc.contains("neft") || lc.contains("imps") -> "Bank Transfer"
        else -> "UPI"
    }

    // ── Step 2: Normalise OCR misreads BEFORE amount extraction ──────────────
    //
    // ML Kit misreads ₹ as: 7, T, =, F (varies by font/screenshot quality)
    //
    // Fix strategy:
    //   - Line that starts with isolated "7" or "T" + digits → replace with ₹
    //   - "space-7-digit" or "=7-digit" mid-sentence → replace with ₹
    //   - Normalise Rs./INR/₨ → ₹
    //   - Strip thousands separators from inside numbers (1,250 → 1250)

    val normalised = rawText
        .replace(Regex("""(?m)^([7T])\s*(\d)""")) { "₹${it.groupValues[2]}" }
        .replace(Regex("""([\s=])([7T])(\d{1,3}[,.]?\d)""")) { "${it.groupValues[1]}₹${it.groupValues[3]}" }
        .replace("₨", "₹")
        .replace(Regex("""[Rr]s\.?\s*"""), "₹")
        .replace(Regex("""INR\s*"""), "₹")
        .replace(Regex("""(\d),(\d{3})(?=\D|$)"""), "$1$2")

    Log.d("RECEIPT_PARSE", "App=$paymentApp\nNormalised:\n$normalised")

    // ── Step 3: App-aware amount extraction ───────────────────────────────────
    //
    // Key insight: collect ALL ₹-prefixed numbers across the entire receipt,
    // then apply app-specific selection heuristics.
    //
    // GPay:     hero amount is the LARGEST isolated number (shown in huge font)
    //           Also matches "₹250" alone on a line
    // PhonePe:  "Sent ₹250 to" or "₹250.00\nSent"
    // Paytm:    "₹ 250.00\nPaid" — may have spaces after ₹
    // Kotak:    "debited with ₹500" or "₹500.00 debited"
    // SuperMoney/Jupiter: "You paid ₹250"
    // BHIM:     "Amount\n₹250"

    val allAmountCandidates: List<Double> = buildList {
        // Pattern 1: ₹ directly before digits (most common)
        Regex("""₹\s*(\d[\d,]*(?:\.\d{1,2})?)""").findAll(normalised).forEach { m ->
            m.groupValues[1].replace(",", "").toDoubleOrNull()
                ?.takeIf { it > 0.0 && it < 10_000_000.0 }
                ?.let { add(it) }
        }
        // Pattern 2: verb + ₹ + amount
        Regex("""(?:sent|paid|debited?|transferred?|deducted|you paid|you sent|received)\s+₹\s*(\d[\d,]*(?:\.\d{1,2})?)""", RegexOption.IGNORE_CASE)
            .findAll(normalised).forEach { m ->
                m.groupValues[1].replace(",", "").toDoubleOrNull()
                    ?.takeIf { it > 0.0 && it < 10_000_000.0 }
                    ?.let { add(it) }
            }
        // Pattern 3: "Amount" label
        Regex("""[Aa]mount\s*[:\-₹]?\s*₹?\s*(\d[\d,]*(?:\.\d{1,2})?)""")
            .find(normalised)?.groupValues?.getOrNull(1)
            ?.replace(",", "")?.toDoubleOrNull()
            ?.takeIf { it > 0.0 && it < 10_000_000.0 }
            ?.let { add(it) }
    }.distinct()

    // Selection: pick the LARGEST candidate ≥ 1 rupee.
    // Rationale: UI element amounts (version "1.0", "₹5 cashback") are smaller
    // than the actual payment amount. The hero amount is always the largest.
    // Exception: if only one candidate exists, use it.
    val amount: Double? = when {
        allAmountCandidates.isEmpty() -> null
        allAmountCandidates.size == 1 -> allAmountCandidates.first()
        else -> allAmountCandidates
            .filter { it >= 1.0 }           // reject sub-rupee amounts
            .maxOrNull()                     // largest = hero payment amount
    }

    Log.d("RECEIPT_PARSE", "Candidates=$allAmountCandidates → Selected=$amount")

    // ── Step 4: Payee VPA ─────────────────────────────────────────────────────
    val pspHandles = "okicici|okhdfcbank|okaxis|oksbi|ybl|ibl|axisb|upi|paytm|paytmbank|freecharge|apl|barodampay|rbl|kotak|indus|juspay|pingpay|idfcbank|aubank|federal|idbi|cnrb|icici|hdfc|sbi|axis|pnb|bob|unionbank|cub|kvb|dbs|sc|hsbc|citibank|airtel|jio|flipkart|amazon|nsdl|mahb|uco|vijb|boi|obc|allbank|andb|canarabank|centralbank|indianbank|iob|psb|bandhan|equitas|esaf|ujjivan|utbi|slice|fampay|postbank|superpe|jupiter|fi|niyobank"
    val payeeVpa = Regex("""([A-Za-z0-9.\-_+]{2,64}@(?:$pspHandles))""", RegexOption.IGNORE_CASE)
        .find(rawText)?.value?.trim().orEmpty()

    // ── Step 5: Payee Name (app-specific first, then generic) ─────────────────
    val payeeName: String = run {
        // App-specific high-confidence patterns
        val appPatterns = when (paymentApp) {
            "GPay"     -> listOf(
                Regex("""[Pp]aid\s+to\s*\n?\s*([A-Za-z][A-Za-z0-9 .&'\-]{1,40})"""),
                Regex("""[Yy]ou\s+paid\s+[₹\d,. ]+\s+to\s+([A-Za-z][A-Za-z0-9 .&'\-]{1,40})""")
            )
            "PhonePe"  -> listOf(
                Regex("""[Ss]ent\s+to\s*\n?\s*([A-Za-z][A-Za-z0-9 .&'\-]{1,40})"""),
                Regex("""[Tt]o\s*\n\s*([A-Za-z][A-Za-z0-9 .&'\-]{2,40})""")
            )
            "Paytm"    -> listOf(
                Regex("""[Pp]aid\s+to\s*\n?\s*([A-Za-z][A-Za-z0-9 .&'\-]{1,40})"""),
                Regex("""[Ss]ent\s+to\s*\n?\s*([A-Za-z][A-Za-z0-9 .&'\-]{1,40})""")
            )
            else       -> listOf(
                Regex("""[Bb]eneficiary[:\s]+([A-Za-z][A-Za-z0-9 .&'\-]{1,40})"""),
                Regex("""[Tt]ransferred\s+to\s*\n?\s*([A-Za-z][A-Za-z0-9 .&'\-]{1,40})""")
            )
        }
        // Generic fallback patterns
        val genericPatterns = listOf(
            Regex("""[Pp]aid\s+to\s*\n?\s*([A-Za-z][A-Za-z0-9 .&'\-]{1,40})"""),
            Regex("""[Ss]ent\s+to\s*\n?\s*([A-Za-z][A-Za-z0-9 .&'\-]{1,40})"""),
            Regex("""[Tt]o\s*\n\s*([A-Za-z][A-Za-z0-9 .&'\-]{2,40})"""),
            Regex("""[Pp]ayee[:\s]+([A-Za-z][A-Za-z0-9 .&'\-]{1,40})"""),
            Regex("""[Rr]eceiver[:\s]+([A-Za-z][A-Za-z0-9 .&'\-]{1,40})"""),
            Regex("""[Mm]erchant[:\s]+([A-Za-z][A-Za-z0-9 .&'\-]{1,40})"""),
        )
        (appPatterns + genericPatterns).firstNotNullOfOrNull { p ->
            p.find(rawText)?.groupValues?.getOrNull(1)
                ?.trim()
                ?.takeIf { it.length >= 2 && !it.startsWith("@") && !it.all { c -> c.isDigit() } }
        } ?: payeeVpa.substringBefore("@").replace(Regex("[._\\-]+"), " ").trim().replaceFirstChar { it.uppercase() }.ifBlank { "Payment" }
    }

    // ── Step 6: UTR ───────────────────────────────────────────────────────────
    val utrNumber: String = run {
        listOf(
            Regex("""[Uu][Tt][Rr][:\s#No.]*([A-Za-z0-9]{10,22})"""),
            Regex("""[Tt]ransaction\s*[Ii][Dd][:\s#]*([A-Za-z0-9]{8,22})"""),
            Regex("""[Tt]xn\s*(?:[Ii][Dd]|[Nn][Oo]?)[:\s#]*([A-Za-z0-9]{8,22})"""),
            Regex("""[Rr]ef(?:erence)?\s*(?:[Nn][Oo]?|#)[:\s]*([A-Za-z0-9]{8,22})"""),
            Regex("""[Oo]rder\s*[Ii][Dd][:\s#]*([A-Za-z0-9]{8,22})"""),
            Regex("""(?<![A-Za-z0-9])(T[0-9]{11,18})(?![A-Za-z0-9])"""),
            Regex("""(?<![A-Za-z0-9])(CICAG[A-Za-z0-9]{10,20})(?![A-Za-z0-9])"""),
            Regex("""(?<!\d)(\d{12})(?!\d)"""),
            Regex("""(?<![A-Za-z0-9])([A-Z][A-Z0-9]{14,21})(?![A-Za-z0-9])""")
        ).firstNotNullOfOrNull { p -> p.find(rawText)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.length >= 8 } }.orEmpty()
    }

    // ── Step 7: Date ──────────────────────────────────────────────────────────
    val transactionDate: String = run {
        listOf(
            Regex("""(\d{1,2}\s+(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\.?\s+\d{4})""", RegexOption.IGNORE_CASE),
            Regex("""((?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\.?\s+\d{1,2},?\s+\d{4})""", RegexOption.IGNORE_CASE),
            Regex("""(\d{2}[/\-]\d{2}[/\-]\d{2,4})"""),
            Regex("""(Today|Yesterday)""", RegexOption.IGNORE_CASE)
        ).firstNotNullOfOrNull { it.find(rawText)?.groupValues?.getOrNull(1)?.trim() }.orEmpty()
    }

    // ── Step 8: Status ────────────────────────────────────────────────────────
    val status = when {
        lc.contains("successful") || lc.contains("success") || lc.contains("completed") || lc.contains("approved") -> "Success"
        lc.contains("failed") || lc.contains("failure") || lc.contains("declined") -> "Failed"
        lc.contains("pending") || lc.contains("processing") -> "Pending"
        else -> "Success"
    }

    Log.d("RECEIPT_PARSE", "App=$paymentApp | ₹$amount | Name=$payeeName | VPA=$payeeVpa | UTR=$utrNumber")

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