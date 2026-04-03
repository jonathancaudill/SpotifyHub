package com.spotifyhub.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class MainTab {
    Home,
    Search,
    Library,
    Rate,
    NowPlaying,
}

class MainViewModel(
    private val tabSelectedEvent: MutableSharedFlow<MainTab>,
) : ViewModel() {
    private val _selectedTab = MutableStateFlow(MainTab.Home)
    val selectedTab: StateFlow<MainTab> = _selectedTab.asStateFlow()

    private val _detailTab = MutableStateFlow<MainTab?>(null)
    val showDetail: StateFlow<Boolean> = combine(_selectedTab, _detailTab) { selectedTab, detailTab ->
        selectedTab == detailTab
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = false,
    )

    fun selectTab(tab: MainTab) {
        _selectedTab.value = tab
        tabSelectedEvent.tryEmit(tab)
    }

    fun openDetail() {
        _detailTab.value = _selectedTab.value
    }

    fun closeDetail() {
        val selectedTab = _selectedTab.value
        if (_detailTab.value == selectedTab) {
            _detailTab.value = null
        }
    }
}
