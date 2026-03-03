package com.example.paisatracker.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_preferences")

class ThemePreferencesRepository private constructor(private val dataStore: DataStore<Preferences>) {

    private object PreferencesKeys {
        val APP_THEME = stringPreferencesKey("app_theme")
    }

    val appTheme: Flow<AppTheme> = dataStore.data
        .map { preferences ->
            val themeName = preferences[PreferencesKeys.APP_THEME] ?: AppTheme.SYSTEM_DEFAULT.name
            AppTheme.valueOf(themeName)
        }

    suspend fun saveTheme(theme: AppTheme) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_THEME] = theme.name
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: ThemePreferencesRepository? = null

        fun getInstance(context: Context): ThemePreferencesRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = ThemePreferencesRepository(context.themeDataStore)
                INSTANCE = instance
                instance
            }
        }
    }
}
