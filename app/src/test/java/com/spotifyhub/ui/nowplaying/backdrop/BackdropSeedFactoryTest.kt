package com.spotifyhub.ui.nowplaying.backdrop

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class BackdropSeedFactoryTest {
    @Test
    fun `from returns stable seed for the same key`() {
        val first = BackdropSeedFactory.from("album-backdrop-scene")
        val second = BackdropSeedFactory.from("album-backdrop-scene")

        assertEquals(first, second)
    }

    @Test
    fun `from produces multiple distinct channels for one key`() {
        val seed = BackdropSeedFactory.from("album-backdrop-scene")

        assertNotEquals(seed.x, seed.y)
        assertNotEquals(seed.y, seed.z)
        assertNotEquals(seed.z, seed.w)
    }
}
