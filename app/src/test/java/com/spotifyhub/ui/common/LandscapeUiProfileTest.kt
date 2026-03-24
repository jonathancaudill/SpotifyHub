package com.spotifyhub.ui.common

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LandscapeUiProfileTest {

    @Test
    fun `960x480 resolves to compact-height landscape`() {
        val profile = resolveLandscapeUiProfile(width = 960.dp, height = 480.dp)

        assertTrue(profile.isCompactHeight)
        assertEquals(54.dp, profile.sidebarWidth)
        assertEquals(38.sp, profile.nowPlayingTitleSize)
    }

    @Test
    fun `1280x800 does not resolve to compact-height landscape`() {
        val profile = resolveLandscapeUiProfile(width = 1280.dp, height = 800.dp)

        assertFalse(profile.isCompactHeight)
    }

    @Test
    fun `portrait screens do not resolve to compact-height landscape`() {
        val profile = resolveLandscapeUiProfile(width = 480.dp, height = 960.dp)

        assertFalse(profile.isCompactHeight)
    }

    @Test
    fun `1024x540 resolves to compact-height landscape`() {
        val profile = resolveLandscapeUiProfile(width = 1024.dp, height = 540.dp)

        assertTrue(profile.isCompactHeight)
    }

    @Test
    fun `1920x1080 does not resolve to compact-height landscape`() {
        val profile = resolveLandscapeUiProfile(width = 1920.dp, height = 1080.dp)

        assertFalse(profile.isCompactHeight)
    }
}
