package com.example.paisatracker.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.currencyDataStore: DataStore<Preferences> by preferencesDataStore(name = "currency_preferences")

class CurrencyPreferencesRepository(private val context: Context) {
    private object PreferencesKeys {
        val CURRENCY_CODE = stringPreferencesKey("currency_code")
    }

    val selectedCurrency: Flow<Currency> = context.currencyDataStore.data
        .map { preferences ->
            val code = preferences[PreferencesKeys.CURRENCY_CODE] ?: "INR"
            CurrencyList.getCurrencyByCode(code)
        }

    suspend fun saveCurrency(currencyCode: String) {
        context.currencyDataStore.edit { preferences ->
            preferences[PreferencesKeys.CURRENCY_CODE] = currencyCode
        }
    }
}