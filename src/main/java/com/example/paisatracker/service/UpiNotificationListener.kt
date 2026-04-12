package com.example.paisatracker.service

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.paisatracker.PaisaTrackerApplication
import com.example.paisatracker.data.PendingTransaction
import com.example.paisatracker.util.parseNotificationText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Listens for UPI payment notifications from any app that delivers them.
 *
 * STRATEGY — two capture paths:
 *
 *   Path A (Direct UPI apps): GPay, PhonePe, Paytm, BHIM, Amazon Pay
 *     → exact package match, user-controlled per-app toggles
 *
 *   Path B (Bank SMS / forwarded notifications): ANY package
 *     → text content inspection — if it looks like a bank debit SMS, capture it
 *     → this catches Truecaller (forwards SMS), Messages apps, Mi/Samsung SMS,
 *        bank notification apps, etc.
 *     → enabled only when KEY_FILTER_BANK_SMS = true (default: true)
 *
 * Why Truecaller was missed:
 *   The old code checked `packageName == "com.android.mms"` etc. Truecaller
 *   sends notifications with its own package "com.truecaller.android" when it
 *   identifies/forwards an incoming SMS. The fix: don't check the package for
 *   bank SMS — check the notification TEXT instead.
 */
class UpiNotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Packages we explicitly EXCLUDE even if their text matches
    // (to avoid false-positives from social/chat apps)
    private val excludedPackages = setOf(
        "com.whatsapp",
        "com.whatsapp.w4b",
        "org.telegram.messenger",
        "com.instagram.android",
        "com.facebook.katana",
        "com.twitter.android",
        "com.snapchat.android",
        "com.linkedin.android",
        "com.discord",
        "com.google.android.youtube",
        "com.netflix.mediaclient",
        "com.spotify.music"
    )

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_ENABLED, false)) return

        val packageName = sbn.packageName ?: return
        if (packageName in excludedPackages) return

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        // Extract all text from the notification
        val title   = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text    = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""
        val infoText= extras.getCharSequence(Notification.EXTRA_INFO_TEXT)?.toString() ?: ""

        // Concatenate all content — use the longest available body
        val body    = when {
            bigText.length > text.length -> bigText
            else -> text
        }
        val combined = buildString {
            if (title.isNotBlank()) append(title).append("\n")
            if (body.isNotBlank()) append(body).append("\n")
            if (subText.isNotBlank()) append(subText).append("\n")
            if (infoText.isNotBlank()) append(infoText)
        }.trim()

        if (combined.isBlank()) return

        // ── Path A: Direct UPI app packages ──────────────────────────────────
        val isDirectUpiApp = when (packageName) {
            GPAY_PKG    -> prefs.getBoolean(KEY_FILTER_GPAY,    true)
            PHONEPE_PKG -> prefs.getBoolean(KEY_FILTER_PHONEPE, true)
            PAYTM_PKG   -> prefs.getBoolean(KEY_FILTER_PAYTM,  true)
            BHIM_PKG    -> prefs.getBoolean(KEY_FILTER_BHIM,   false)
            AMAZON_PKG  -> prefs.getBoolean(KEY_FILTER_AMAZON, false)
            else        -> false
        }

        // ── Path B: Any package whose text looks like a bank debit ────────────
        // This captures: Truecaller, Mi Messages, Samsung Messages, any custom
        // SMS app, bank notification apps, Airtel/Jio/Vi service notifications.
        val isBankSmsContent = prefs.getBoolean(KEY_FILTER_BANK_SMS, true)
                && isLikelyBankDebitText(combined)
                && !isDirectUpiApp // avoid double-processing

        if (!isDirectUpiApp && !isBankSmsContent) return

        Log.d("NOTIF_LISTENER", "Captured [${if (isDirectUpiApp) "DirectApp" else "BankSMS"}] pkg=$packageName | $combined")

        val parsed = parseNotificationText(combined, packageName) ?: run {
            Log.d("NOTIF_LISTENER", "parseNotificationText returned null, skipping")
            return
        }
        if (parsed.amount <= 0) return

        scope.launch {
            try {
                val repo = (application as PaisaTrackerApplication).repository

                val skipDups = prefs.getBoolean(KEY_SKIP_DUPLICATES, true)
                if (skipDups && parsed.utrNumber.isNotBlank()) {
                    if (repo.countPendingByUtr(parsed.utrNumber) > 0 ||
                        repo.countExpenseByUtr(parsed.utrNumber) > 0
                    ) {
                        Log.d("NOTIF_LISTENER", "Duplicate UTR ${parsed.utrNumber}, skipping")
                        return@launch
                    }
                }

                val pending = PendingTransaction(
                    amount              = parsed.amount,
                    payeeName           = parsed.payeeName,
                    payeeVpa            = parsed.payeeVpa,
                    utrNumber           = parsed.utrNumber,
                    sourceApp           = parsed.sourceApp,
                    status              = parsed.status,
                    transactionDate     = parsed.transactionDate,
                    rawNotificationText = combined
                )
                repo.insertPendingTransaction(pending)
                Log.d("NOTIF_LISTENER", "Saved: ${parsed.payeeName} ₹${parsed.amount}")

            } catch (e: Exception) {
                Log.e("NOTIF_LISTENER", "Save failed: ${e.message}")
            }
        }
    }

    /**
     * Content-based heuristic: does this notification text look like a bank debit?
     *
     * Rules (must satisfy ≥ 2 to avoid false positives):
     *   1. Contains a currency indicator (₹, INR, Rs., debited, debit)
     *   2. Contains a numeric amount pattern
     *   3. Contains a UPI/bank reference hint (UPI, Ref, UTR, a/c, acct, bank name)
     */
    private fun isLikelyBankDebitText(text: String): Boolean {
        val lc = text.lowercase()

        // Must contain a payment action word to be relevant
        if (!lc.contains("debit") && !lc.contains("paid") && !lc.contains("sent") &&
            !lc.contains("transfer") && !lc.contains("₹") && !lc.contains("inr")
        ) return false

        // Must contain a numeric amount (prevent empty/irrelevant notifications)
        val hasAmount = Regex("""(?:₹|inr|rs\.?)\s*\d""").containsMatchIn(lc) ||
                Regex("""\d{2,7}\.(?:00|\d\d)""").containsMatchIn(text)

        // Must have a bank/UPI reference signal
        val hasBankSignal = lc.contains("upi") || lc.contains("ref") || lc.contains("utr") ||
                lc.contains("a/c") || lc.contains("acct") || lc.contains("account") ||
                lc.contains("neft") || lc.contains("imps") || lc.contains("rtgs") ||
                Regex("""\d{12}""").containsMatchIn(text)  // 12-digit UTR

        return hasAmount && hasBankSignal
    }

    companion object {
        const val PREFS_NAME          = "auto_capture_prefs"
        const val KEY_ENABLED         = "listener_enabled"
        const val KEY_SKIP_DUPLICATES = "skip_duplicates"
        const val KEY_REVIEW_FIRST    = "review_before_save"
        const val KEY_FILTER_GPAY     = "filter_gpay"
        const val KEY_FILTER_PHONEPE  = "filter_phonepe"
        const val KEY_FILTER_PAYTM    = "filter_paytm"
        const val KEY_FILTER_BHIM     = "filter_bhim"
        const val KEY_FILTER_AMAZON   = "filter_amazon"
        const val KEY_FILTER_BANK_SMS = "filter_bank_sms"

        const val GPAY_PKG    = "com.google.android.apps.nbu.paisa.user"
        const val PHONEPE_PKG = "com.phonepe.app"
        const val PAYTM_PKG   = "net.one97.paytm"
        const val BHIM_PKG    = "in.org.npci.upiapp"
        const val AMAZON_PKG  = "in.amazon.mShop.android.shopping"

        fun isPermissionGranted(context: Context): Boolean {
            val cn   = ComponentName(context, UpiNotificationListener::class.java)
            val flat = android.provider.Settings.Secure.getString(
                context.contentResolver, "enabled_notification_listeners"
            ) ?: return false
            return flat.contains(cn.flattenToString())
        }

        fun openPermissionSettings(context: Context) {
            context.startActivity(
                android.content.Intent(
                    "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"
                ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}