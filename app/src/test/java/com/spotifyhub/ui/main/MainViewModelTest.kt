package com.spotifyhub.ui.main

import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.Assert.assertEquals
import org.junit.Test

class MainViewModelTest {
    @Test
    fun `defaults to home tab`() {
        val viewModel = MainViewModel(tabSelectedEvent = MutableSharedFlow(extraBufferCapacity = 1))

        assertEquals(MainTab.Home, viewModel.selectedTab.value)
    }
}
