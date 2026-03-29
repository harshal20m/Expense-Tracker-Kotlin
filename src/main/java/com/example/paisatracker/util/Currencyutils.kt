package com.example.paisatracker.util

import com.example.paisatracker.data.Currency
import com.example.paisatracker.data.CurrencyList
import java.text.DecimalFormat

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