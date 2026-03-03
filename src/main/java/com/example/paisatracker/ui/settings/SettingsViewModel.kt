package com.example.paisatracker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.paisatracker.data.AppTheme
import com.example.paisatracker.data.ThemePreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val themePreferencesRepository: ThemePreferencesRepository) : ViewModel() {

    val currentTheme: StateFlow<AppTheme> = themePreferencesRepository.appTheme
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppTheme.SYSTEM_DEFAULT
        )

    fun saveTheme(theme: AppTheme) {
        viewModelScope.launch {
            themePreferencesRepository.saveTheme(theme)
        }
    }
}

class SettingsViewModelFactory(
    private val themePreferencesRepository: ThemePreferencesRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(themePreferencesRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
