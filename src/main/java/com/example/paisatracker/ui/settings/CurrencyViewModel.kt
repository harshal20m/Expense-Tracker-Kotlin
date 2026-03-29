package com.example.paisatracker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paisatracker.data.Currency
import com.example.paisatracker.data.CurrencyList
import com.example.paisatracker.data.CurrencyPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CurrencyViewModel(
    private val repository: CurrencyPreferencesRepository
) : ViewModel() {

    private val _currentCurrency = MutableStateFlow<Currency>(CurrencyList.getCurrencyByCode("INR"))
    val currentCurrency: StateFlow<Currency> = _currentCurrency.asStateFlow()

    init {
        viewModelScope.launch {
            repository.selectedCurrency.collect { currency ->
                _currentCurrency.value = currency
            }
        }
    }

    fun saveCurrency(currencyCode: String) {
        viewModelScope.launch {
            repository.saveCurrency(currencyCode)
        }
    }
}