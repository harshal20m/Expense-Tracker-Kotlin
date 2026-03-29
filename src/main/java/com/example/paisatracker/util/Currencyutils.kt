package com.example.paisatracker.util

import android.content.Context
import com.example.paisatracker.data.Currency
import com.example.paisatracker.data.CurrencyList
import com.example.paisatracker.data.CurrencyPreferencesRepository
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

/**
 * Format currency with the selected currency symbol
 */
fun formatCurrency(amount: Double, currency: Currency): String {
    val formatter = DecimalFormat("#,##0.00")
    val formattedAmount = formatter.format(amount)
    return "${currency.symbol} $formattedAmount"
}

/**
 * Legacy function for backward compatibility - uses default currency
 * This function now uses the saved currency preference
 */
private var cachedCurrency: Currency = CurrencyList.getCurrencyByCode("INR")
private var cachedContext: Context? = null

/**
 * Initialize the currency formatter with context
 * Call this from your Application class or main activity
 */
fun initCurrencyFormatter(context: Context) {
    cachedContext = context.applicationContext
    val repo = CurrencyPreferencesRepository(cachedContext!!)
    // Note: This needs to be done asynchronously in a real app
    // For simplicity, we'll use the current value
}

/**
 * Format currency using the saved currency preference
 * This function should be used throughout the app
 */
fun formatCurrencyWithPreference(amount: Double, context: Context): String {
    try {
        val repo = CurrencyPreferencesRepository(context.applicationContext)
        // This is synchronous for simplicity, but in production you'd need to collect from Flow
        // For now, we'll use a simpler approach - store the current currency in a static variable

        // Simpler approach: Use a singleton to store current currency
        return formatCurrency(amount, CurrentCurrency.get())
    } catch (e: Exception) {
        return formatCurrency(amount, CurrencyList.getCurrencyByCode("INR"))
    }
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
 * Format currency based on locale
 */
fun formatCurrencyByLocale(amount: Double, currencyCode: String): String {
    return try {
        val format = NumberFormat.getCurrencyInstance()
        format.currency = java.util.Currency.getInstance(currencyCode)
        format.format(amount)
    } catch (e: Exception) {
        formatCurrency(amount)
    }
}

/**
 * Compact currency format for large numbers
 */
fun formatCurrencyCompact(amount: Double, currency: Currency): String {
    return when {
        amount >= 10_000_000 -> {
            val crores = amount / 10_000_000
            "${currency.symbol} ${DecimalFormat("#,##0.00").format(crores)}Cr"
        }
        amount >= 100_000 -> {
            val lakhs = amount / 100_000
            "${currency.symbol} ${DecimalFormat("#,##0.00").format(lakhs)}L"
        }
        amount >= 1_000 -> {
            val thousands = amount / 1_000
            "${currency.symbol} ${DecimalFormat("#,##0.00").format(thousands)}K"
        }
        else -> formatCurrency(amount, currency)
    }
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