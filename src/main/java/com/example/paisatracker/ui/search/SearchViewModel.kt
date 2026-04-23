package com.example.paisatracker.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.paisatracker.data.PaisaTrackerRepository
import com.example.paisatracker.data.RecentExpense
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SearchViewModel(private val repository: PaisaTrackerRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _minAmount = MutableStateFlow("")
    val minAmount: StateFlow<String> = _minAmount

    private val _maxAmount = MutableStateFlow("")
    val maxAmount: StateFlow<String> = _maxAmount

    private val _searchResults = MutableStateFlow<List<RecentExpense>>(emptyList())
    val searchResults: StateFlow<List<RecentExpense>> = _searchResults

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive

    private val _projectId = MutableStateFlow<Long?>(null)
    val projectId: StateFlow<Long?> = _projectId

    private var searchJob: Job? = null

    fun setProjectId(id: Long?) {
        _projectId.value = id
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onMinAmountChanged(min: String) {
        _minAmount.value = min
    }

    fun onMaxAmountChanged(max: String) {
        _maxAmount.value = max
    }

    fun executeSearch() {
        searchJob?.cancel()
        _isSearchActive.value = true
        searchJob = viewModelScope.launch {
            val query = _searchQuery.value.trim()
            val min = _minAmount.value.toDoubleOrNull()
            val max = _maxAmount.value.toDoubleOrNull()
            val currentProjectId = _projectId.value

            if (query.isBlank() && min == null && max == null) {
                _searchResults.value = emptyList()
                return@launch
            }

            val resultsFlow = if (query.isNotBlank()) {
                repository.searchExpensesByDescription(query, currentProjectId)
            } else {
                repository.searchExpensesByAmount(min, max, currentProjectId)
            }

            _searchResults.value = resultsFlow.first()
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _minAmount.value = ""
        _maxAmount.value = ""
        _searchResults.value = emptyList()
        _isSearchActive.value = false
        _projectId.value = null
        searchJob?.cancel()
    }
}

class SearchViewModelFactory(
    private val repository: PaisaTrackerRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SearchViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}