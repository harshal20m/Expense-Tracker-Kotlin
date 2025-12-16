package com.example.paisatracker.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// 👇 Singleton pattern - prevents multiple DataStore instances
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_lock_prefs")

class AppLockPreferences private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: AppLockPreferences? = null

        private val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        private val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        private val PIN_CODE = stringPreferencesKey("pin_code")

        fun getInstance(context: Context): AppLockPreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppLockPreferences(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    val isAppLockEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[APP_LOCK_ENABLED] ?: false
    }

    val isBiometricEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[BIOMETRIC_ENABLED] ?: false
    }

    val pinCode: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PIN_CODE]
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[APP_LOCK_ENABLED] = enabled
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[BIOMETRIC_ENABLED] = enabled
        }
    }

    suspend fun setPinCode(pin: String) {
        context.dataStore.edit { preferences ->
            preferences[PIN_CODE] = pin
        }
    }

    suspend fun clearPinCode() {
        context.dataStore.edit { preferences ->
            preferences.remove(PIN_CODE)
        }
    }
}
