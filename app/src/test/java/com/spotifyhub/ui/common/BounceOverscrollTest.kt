package com.spotifyhub.ui.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BounceOverscrollTest {
    @Test
    fun rubberBandDistance_roundTripsThroughInverse() {
        val dimension = 1200f
        val samples = listOf(1f, 12f, 48f, 120f, 260f)

        samples.forEach { stretch ->
            val offset = rubberBandDistance(distance = stretch, dimension = dimension)
            val reconstructedStretch =
                inverseRubberBandDistance(offset = offset, dimension = dimension)

            assertEquals(stretch, reconstructedStretch, 0.001f)
        }
    }

    @Test
    fun rubberBandDistance_staysWithinDimensionAndSoftensInput() {
        val dimension = 900f
        val stretch = 220f
        val offset = rubberBandDistance(distance = stretch, dimension = dimension)

        assertTrue(offset > 0f)
        assertTrue(offset < stretch)
        assertTrue(offset < dimension)
    }

    @Test
    fun applyStretchDelta_preventsCrossingZeroDuringRelease() {
        val nextStretch =
            applyStretchDelta(
                currentStretch = 36f,
                delta = -80f,
                maxStretch = 400f,
                allowCrossingZero = false,
            )

        assertEquals(0f, nextStretch, 0.001f)
    }

    @Test
    fun applyStretchDelta_allowsCrossingZeroWhenRequested() {
        val nextStretch =
            applyStretchDelta(
                currentStretch = 36f,
                delta = -80f,
                maxStretch = 400f,
                allowCrossingZero = true,
            )

        assertEquals(-44f, nextStretch, 0.001f)
    }
}
