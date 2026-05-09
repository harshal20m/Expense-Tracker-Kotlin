package com.example.paisatracker.ui.search

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.paisatracker.data.PaisaTrackerRepository
import com.example.paisatracker.data.RecentExpense
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private val Context.recentSearchesDataStore: DataStore<Preferences> by preferencesDataStore(name = "recent_searches")

class SearchViewModel(
    private val repository: PaisaTrackerRepository,
    private val context: Context
) : ViewModel() {

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

    private val _recentSearches = MutableStateFlow<List<String>>(emptyList())
    val recentSearches: StateFlow<List<String>> = _recentSearches

    private var searchJob: Job? = null

    companion object {
        private val RECENT_SEARCHES_KEY = stringPreferencesKey("recent_searches")
        private const val MAX_RECENT_SEARCHES = 10
    }

    init {
        loadRecentSearches()
    }

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

            // Save to recent searches if query is not blank
            if (query.isNotBlank()) {
                saveRecentSearch(query)
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

    private fun loadRecentSearches() {
        viewModelScope.launch {
            context.recentSearchesDataStore.data
                .map { preferences ->
                    val searchesString = preferences[RECENT_SEARCHES_KEY] ?: ""
                    if (searchesString.isBlank()) emptyList()
                    else searchesString.split("|||").filter { it.isNotBlank() }
                }
                .collect { searches ->
                    _recentSearches.value = searches
                }
        }
    }

    private fun saveRecentSearch(query: String) {
        viewModelScope.launch {
            context.recentSearchesDataStore.edit { preferences ->
                val currentSearches = preferences[RECENT_SEARCHES_KEY]?.split("|||")?.filter { it.isNotBlank() } ?: emptyList()
                val updatedSearches = (listOf(query) + currentSearches)
                    .distinct()
                    .take(MAX_RECENT_SEARCHES)
                preferences[RECENT_SEARCHES_KEY] = updatedSearches.joinToString("|||")
            }
        }
    }

    fun clearRecentSearches() {
        viewModelScope.launch {
            context.recentSearchesDataStore.edit { preferences ->
                preferences.remove(RECENT_SEARCHES_KEY)
            }
            _recentSearches.value = emptyList()
        }
    }
}

class SearchViewModelFactory(
    private val repository: PaisaTrackerRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SearchViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}