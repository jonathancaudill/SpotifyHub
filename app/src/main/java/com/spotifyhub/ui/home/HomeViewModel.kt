package com.spotifyhub.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spotifyhub.browse.BrowseRepository
import com.spotifyhub.browse.model.BrowseItem
import com.spotifyhub.browse.model.HomeSection
import com.spotifyhub.ui.main.MainTab
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val greeting: String = "",
    val quickAccess: List<BrowseItem> = emptyList(),
    val sections: List<HomeSection> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
)

class HomeViewModel(
    private val browseRepository: BrowseRepository,
    private val tabSelectedEvent: MutableSharedFlow<MainTab>,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
        observeTabSelection()
        startPeriodicRefresh()
    }

    fun refresh() {
        loadHomeData(forceRefresh = true)
    }

    private fun observeTabSelection() {
        viewModelScope.launch {
            tabSelectedEvent.collect { tab ->
                if (tab == MainTab.Home) {
                    // Uses cache if TTL not expired; fetches fresh if expired
                    loadHomeData()
                }
            }
        }
    }

    private fun startPeriodicRefresh() {
        viewModelScope.launch {
            while (true) {
                delay(PERIODIC_REFRESH_MS)
                loadHomeData(forceRefresh = true)
            }
        }
    }

    private fun loadHomeData(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            val hasExistingData = _uiState.value.sections.isNotEmpty()
            _uiState.value = _uiState.value.copy(
                isLoading = !hasExistingData,
                isRefreshing = hasExistingData,
                error = null,
            )
            runCatching {
                browseRepository.getHomeData(forceRefresh = forceRefresh)
            }.onSuccess { homeData ->
                _uiState.value = HomeUiState(
                    greeting = homeData.greeting,
                    quickAccess = homeData.quickAccess,
                    sections = homeData.sections,
                    isLoading = false,
                    isRefreshing = false,
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    error = e.message ?: "Failed to load home data",
                )
            }
        }
    }

    companion object {
        private const val PERIODIC_REFRESH_MS = 5 * 60 * 1000L // 5 minutes
    }
}
