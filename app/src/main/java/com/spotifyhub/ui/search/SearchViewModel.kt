package com.spotifyhub.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spotifyhub.search.SearchRepository
import com.spotifyhub.search.model.SearchResults
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val results: SearchResults? = null,
    val isSearching: Boolean = false,
    val error: String? = null,
)

class SearchViewModel(
    private val searchRepository: SearchRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _queryFlow = MutableStateFlow("")

    init {
        @OptIn(FlowPreview::class)
        viewModelScope.launch {
            _queryFlow
                .debounce(300L)
                .distinctUntilChanged()
                .collect { query ->
                    if (query.isBlank()) {
                        _uiState.value = SearchUiState(query = query)
                    } else {
                        performSearch(query)
                    }
                }
        }
    }

    fun updateQuery(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        _queryFlow.value = query
    }

    private suspend fun performSearch(query: String) {
        _uiState.value = _uiState.value.copy(isSearching = true, error = null)
        runCatching {
            searchRepository.search(query)
        }.onSuccess { results ->
            _uiState.value = _uiState.value.copy(
                results = results,
                isSearching = false,
            )
        }.onFailure { e ->
            _uiState.value = _uiState.value.copy(
                results = SearchResults(
                    tracks = emptyList(),
                    albums = emptyList(),
                    artists = emptyList(),
                    playlists = emptyList(),
                ),
                isSearching = false,
                error = e.message ?: "Search failed",
            )
        }
    }
}
