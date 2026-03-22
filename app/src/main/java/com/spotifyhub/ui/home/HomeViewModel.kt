package com.spotifyhub.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spotifyhub.browse.BrowseRepository
import com.spotifyhub.browse.model.BrowseItem
import com.spotifyhub.browse.model.HomeSection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val greeting: String = "",
    val quickAccess: List<BrowseItem> = emptyList(),
    val sections: List<HomeSection> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

class HomeViewModel(
    private val browseRepository: BrowseRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
    }

    fun refresh() {
        loadHomeData(forceRefresh = true)
    }

    private fun loadHomeData(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching {
                browseRepository.getHomeData(forceRefresh = forceRefresh)
            }.onSuccess { homeData ->
                _uiState.value = HomeUiState(
                    greeting = homeData.greeting,
                    quickAccess = homeData.quickAccess,
                    sections = homeData.sections,
                    isLoading = false,
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load home data",
                )
            }
        }
    }
}
