package com.example.paisatracker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.paisatracker.data.AppTheme
import com.example.paisatracker.data.Currency
import com.example.paisatracker.data.CurrencyPreferencesRepository
import com.example.paisatracker.data.ThemePreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val themePreferencesRepository: ThemePreferencesRepository,
    private val currencyPreferencesRepository: CurrencyPreferencesRepository
) : ViewModel() {

    val currentTheme: StateFlow<AppTheme> = themePreferencesRepository.appTheme
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppTheme.SYSTEM_DEFAULT
        )

    val selectedCurrency: StateFlow<Currency> = currencyPreferencesRepository.selectedCurrency
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = com.example.paisatracker.data.CurrencyList.getCurrencyByCode("INR")
        )

    fun saveTheme(theme: AppTheme) {
        viewModelScope.launch {
            themePreferencesRepository.saveTheme(theme)
        }
    }

    fun saveCurrency(currencyCode: String) {
        viewModelScope.launch {
            currencyPreferencesRepository.saveCurrency(currencyCode)
        }
    }
}

class SettingsViewModelFactory(
    private val themePreferencesRepository: ThemePreferencesRepository,
    private val currencyPreferencesRepository: CurrencyPreferencesRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(themePreferencesRepository, currencyPreferencesRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}