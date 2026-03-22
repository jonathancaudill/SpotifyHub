package com.spotifyhub.ui.common

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlin.math.sign

private const val FORWARD_RESISTANCE = 0.28f
private const val RELEASE_RESISTANCE = 0.55f
private const val FLING_VELOCITY_SCALE = 0.12f

@Stable
fun Modifier.bounceOverscroll(
    orientation: Orientation,
    enabled: Boolean = true,
    maxOffset: Dp = 88.dp,
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "bounceOverscroll"
        properties["orientation"] = orientation
        properties["enabled"] = enabled
        properties["maxOffset"] = maxOffset
    },
) {
    if (!enabled) {
        return@composed this
    }

    val density = LocalDensity.current
    val maxOffsetPx = with(density) { maxOffset.toPx() }
    val overscrollOffset = remember { Animatable(0f) }
    val decay = remember { exponentialDecay<Float>() }
    val scope = rememberCoroutineScope()
    var targetOffset by remember { mutableFloatStateOf(0f) }

    fun launchSnapTo(newOffset: Float) {
        targetOffset = newOffset
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            overscrollOffset.stop()
            overscrollOffset.snapTo(newOffset)
        }
    }

    suspend fun animateBackToRest() {
        overscrollOffset.stop()
        targetOffset = 0f
        overscrollOffset.animateTo(
            targetValue = 0f,
            animationSpec = spring(
                stiffness = Spring.StiffnessLow,
                dampingRatio = Spring.DampingRatioNoBouncy,
            ),
        )
    }

    suspend fun animateMomentumOverscroll(velocity: Float) {
        if (velocity == 0f) {
            return
        }

        overscrollOffset.stop()
        overscrollOffset.updateBounds(lowerBound = -maxOffsetPx, upperBound = maxOffsetPx)
        targetOffset = overscrollOffset.value
        overscrollOffset.animateDecay(
            initialVelocity = velocity * FLING_VELOCITY_SCALE,
            animationSpec = decay,
        )
        targetOffset = overscrollOffset.value
        animateBackToRest()
    }

    fun consumeOverscrollDelta(delta: Float): Float {
        if (delta == 0f) {
            return 0f
        }

        val current = targetOffset
        val resistance =
            if (current == 0f || sign(current) == sign(delta)) {
                FORWARD_RESISTANCE
            } else {
                RELEASE_RESISTANCE
            }
        val newOffset = (current + (delta * resistance)).coerceIn(-maxOffsetPx, maxOffsetPx)
        if (newOffset == current) {
            return 0f
        }

        launchSnapTo(newOffset)
        return (newOffset - current) / resistance
    }

    fun offsetOf(value: Float): Offset =
        if (orientation == Orientation.Vertical) {
            Offset(0f, value)
        } else {
            Offset(value, 0f)
        }

    fun velocityOf(value: Float): Velocity =
        if (orientation == Orientation.Vertical) {
            Velocity(0f, value)
        } else {
            Velocity(value, 0f)
        }

    fun axis(offset: Offset): Float =
        if (orientation == Orientation.Vertical) {
            offset.y
        } else {
            offset.x
        }

    fun axis(velocity: Velocity): Float =
        if (orientation == Orientation.Vertical) {
            velocity.y
        } else {
            velocity.x
        }

    val connection = remember(orientation, maxOffsetPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput) {
                    return Offset.Zero
                }

                val delta = axis(available)
                if (targetOffset == 0f || delta == 0f || sign(delta) == sign(targetOffset)) {
                    return Offset.Zero
                }

                return offsetOf(consumeOverscrollDelta(delta))
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (source != NestedScrollSource.UserInput) {
                    return Offset.Zero
                }

                val delta = axis(available)
                if (delta == 0f) {
                    return Offset.Zero
                }

                return offsetOf(consumeOverscrollDelta(delta))
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (targetOffset == 0f) {
                    return Velocity.Zero
                }

                animateBackToRest()
                return velocityOf(axis(available))
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                val velocity = axis(available)
                if (velocity != 0f) {
                    animateMomentumOverscroll(velocity)
                    return velocityOf(velocity)
                }

                if (targetOffset != 0f) {
                    animateBackToRest()
                }
                return Velocity.Zero
            }
        }
    }

    this
        .nestedScroll(connection)
        .graphicsLayer {
            if (orientation == Orientation.Vertical) {
                translationY = overscrollOffset.value
            } else {
                translationX = overscrollOffset.value
            }
        }
}
