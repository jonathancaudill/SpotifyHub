package com.spotifyhub.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spotifyhub.library.LibraryRepository
import com.spotifyhub.library.model.LibraryItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class LibraryCategory {
    Playlists,
    Albums,
    Tracks,
}

data class LibraryUiState(
    val selectedCategory: LibraryCategory = LibraryCategory.Playlists,
    val items: List<LibraryItem> = emptyList(),
    val totalItems: Int = 0,
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
)

class LibraryViewModel(
    private val libraryRepository: LibraryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        loadCategory(LibraryCategory.Playlists)
    }

    fun selectCategory(category: LibraryCategory) {
        if (category == _uiState.value.selectedCategory && _uiState.value.items.isNotEmpty()) return
        _uiState.value = LibraryUiState(selectedCategory = category, isLoading = true)
        loadCategory(category)
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || state.items.size >= state.totalItems) return
        _uiState.value = state.copy(isLoadingMore = true)
        loadCategory(state.selectedCategory, offset = state.items.size)
    }

    private fun loadCategory(category: LibraryCategory, offset: Int = 0) {
        viewModelScope.launch {
            runCatching {
                when (category) {
                    LibraryCategory.Playlists -> libraryRepository.getPlaylists(offset)
                    LibraryCategory.Albums -> libraryRepository.getSavedAlbums(offset)
                    LibraryCategory.Tracks -> libraryRepository.getSavedTracks(offset)
                }
            }.onSuccess { (items, total) ->
                val currentState = _uiState.value
                val allItems = if (offset > 0) currentState.items + items else items
                _uiState.value = currentState.copy(
                    items = allItems,
                    totalItems = total,
                    isLoading = false,
                    isLoadingMore = false,
                    error = null,
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    error = e.message ?: "Failed to load library",
                )
            }
        }
    }
}
