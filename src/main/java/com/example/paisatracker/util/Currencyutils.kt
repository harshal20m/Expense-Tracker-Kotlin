package com.example.paisatracker.util

import com.example.paisatracker.data.Currency
import com.example.paisatracker.data.CurrencyList
import java.text.DecimalFormat



import android.content.Context

/**
 * Stateless utility object — safe to call from widgets, background threads, anywhere.
 * No singleton state. Currency is always passed explicitly or read from prefs.
 */
object CurrencyUtils {

    /**
     * Primary function — always prefer passing currency explicitly.
     */
    fun formatCurrency(amount: Double, currency: Currency): String {
        val formatter = DecimalFormat("#,##0.00")
        return "${currency.symbol} ${formatter.format(amount)}"
    }

    /**
     * Reads currency from SharedPreferences synchronously.
     * Safe to use in widget coroutines (provideGlance runs on a background dispatcher).
     * Falls back to INR if nothing is saved.
     */
    fun formatCurrency(amount: Double, context: Context): String {
        val currency = readCurrencyFromPrefs(context)
        val formatter = DecimalFormat("#,##0.00")
        return "${currency.symbol} ${formatter.format(amount)}"
    }

    /**
     * Fallback overload — uses CurrentCurrency singleton.
     * Only valid inside the main app process where CurrentCurrency has been initialised.
     * Do NOT call this from widgets.
     */
    fun formatCurrency(amount: Double): String {
        return formatCurrency(amount, CurrentCurrency.get())
    }

    /**
     * Reads the saved currency code from SharedPreferences.
     * SharedPreferences is safe for synchronous reads on background threads.
     */
    fun readCurrencyFromPrefs(context: Context): Currency {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val code = prefs.getString(KEY_CURRENCY_CODE, "INR") ?: "INR"
        return CurrencyList.getCurrencyByCode(code)
    }

    // Keep these keys in sync with wherever you save settings in your app
    const val PREFS_NAME = "paisa_tracker_prefs"
    const val KEY_CURRENCY_CODE = "currency_code"
}

/**
 * Top-level convenience — delegates to CurrencyUtils object.
 * Keeps existing call sites in composables working without changes.
 */


/**
 * Format currency with the selected currency symbol
 */
fun formatCurrency(amount: Double, currency: Currency): String {
    val formatter = DecimalFormat("#,##0.00")
    val formattedAmount = formatter.format(amount)
    return "${currency.symbol} $formattedAmount"
}

/**
 * Legacy function for backward compatibility - uses INR by default
 * This function should be replaced with formatCurrencyWithPreference in your composables
 */
fun formatCurrency(amount: Double): String {
    val currency = CurrentCurrency.get()
    val formatter = DecimalFormat("#,##0.00")
    return "${currency.symbol} ${formatter.format(amount)}"
}

/**
 * Singleton to hold current currency
 */
object CurrentCurrency {
    private var _currency: Currency = CurrencyList.getCurrencyByCode("INR")
    var onCurrencyChanged: (() -> Unit)? = null

    fun set(currency: Currency) {
        _currency = currency
        onCurrencyChanged?.invoke()
    }

    fun get(): Currency = _currency
}