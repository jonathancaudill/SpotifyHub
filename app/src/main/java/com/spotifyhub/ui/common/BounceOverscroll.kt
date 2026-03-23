package com.spotifyhub.ui.common

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

private const val RUBBER_BAND_COEFFICIENT = 0.55f
private const val DEFAULT_RUBBER_BAND_DIMENSION_MULTIPLIER = 6f
private const val EDGE_MOMENTUM_THRESHOLD_MULTIPLIER = 3f
private const val MIN_EDGE_MOMENTUM_VELOCITY_PX = 480f
private const val OVERSCROLL_EPSILON_PX = 0.5f

internal fun rubberBandDistance(
    distance: Float,
    dimension: Float,
    constant: Float = RUBBER_BAND_COEFFICIENT,
): Float {
    if (distance <= 0f || dimension <= 0f) {
        return 0f
    }
    return (distance * dimension * constant) / (dimension + (constant * distance))
}

internal fun inverseRubberBandDistance(
    offset: Float,
    dimension: Float,
    constant: Float = RUBBER_BAND_COEFFICIENT,
): Float {
    if (offset <= 0f || dimension <= 0f) {
        return 0f
    }

    val safeOffset = offset.coerceAtMost(dimension - 1f)
    return (safeOffset * dimension) / (constant * (dimension - safeOffset))
}

internal fun applyStretchDelta(
    currentStretch: Float,
    delta: Float,
    maxStretch: Float,
    allowCrossingZero: Boolean,
): Float {
    if (maxStretch <= 0f || delta == 0f) {
        return currentStretch.coerceIn(-maxStretch, maxStretch)
    }

    val unclamped = currentStretch + delta
    val nextStretch =
        if (
            !allowCrossingZero &&
            currentStretch != 0f &&
            sign(currentStretch) != sign(delta) &&
            unclamped != 0f &&
            sign(unclamped) != sign(currentStretch)
        ) {
            0f
        } else {
            unclamped
        }

    return nextStretch.coerceIn(-maxStretch, maxStretch)
}

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
    val viewConfiguration = LocalViewConfiguration.current
    val maxOffsetPx = with(density) { maxOffset.toPx() }
    val overscrollOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var axisSizePx by remember { mutableIntStateOf(0) }
    var releaseSettleToken by remember { mutableIntStateOf(0) }
    var pendingReleaseJob by remember { mutableStateOf<Job?>(null) }

    fun rubberBandDimensionPx(): Float {
        val measuredDimension = axisSizePx.toFloat()
        return if (measuredDimension > maxOffsetPx + 1f) {
            measuredDimension
        } else {
            maxOffsetPx * DEFAULT_RUBBER_BAND_DIMENSION_MULTIPLIER
        }
    }

    fun maxStretchDistancePx(): Float {
        val dimension = rubberBandDimensionPx()
        val safeMaxOffset = maxOffsetPx.coerceAtMost(dimension - 1f)
        return inverseRubberBandDistance(offset = safeMaxOffset, dimension = dimension)
    }

    fun stretchDistanceForOffset(offset: Float): Float {
        if (abs(offset) <= OVERSCROLL_EPSILON_PX) {
            return 0f
        }

        val stretchDistance =
            inverseRubberBandDistance(
                offset = abs(offset).coerceAtMost(maxOffsetPx),
                dimension = rubberBandDimensionPx(),
            )
        return stretchDistance * sign(offset)
    }

    fun offsetForStretchDistance(stretchDistance: Float): Float {
        if (abs(stretchDistance) <= OVERSCROLL_EPSILON_PX) {
            return 0f
        }

        val offset =
            rubberBandDistance(
                distance = abs(stretchDistance),
                dimension = rubberBandDimensionPx(),
            ).coerceAtMost(maxOffsetPx)
        return offset * sign(stretchDistance)
    }

    fun launchSnapToStretch(stretchDistance: Float) {
        val newOffset = offsetForStretchDistance(stretchDistance)
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            overscrollOffset.stop()
            overscrollOffset.snapTo(newOffset)
        }
    }

    suspend fun animateBackToRest(initialVelocity: Float = 0f) {
        overscrollOffset.stop()
        val currentOffset = overscrollOffset.value
        val velocityTowardRest =
            if (currentOffset == 0f) {
                initialVelocity
            } else if (sign(currentOffset) == sign(initialVelocity)) {
                0f
            } else {
                initialVelocity
            }
        overscrollOffset.animateTo(
            targetValue = 0f,
            initialVelocity = velocityTowardRest.coerceIn(
                minimumValue = -viewConfiguration.maximumFlingVelocity,
                maximumValue = viewConfiguration.maximumFlingVelocity,
            ),
            animationSpec = spring(
                stiffness = Spring.StiffnessMediumLow,
                dampingRatio = Spring.DampingRatioNoBouncy,
            ),
        )
    }

    fun cancelPendingReleaseSettle() {
        pendingReleaseJob?.cancel()
        pendingReleaseJob = null
        releaseSettleToken++
    }

    fun scheduleReleaseSettle() {
        cancelPendingReleaseSettle()
        pendingReleaseJob =
            scope.launch {
                val settleToken = releaseSettleToken
                withFrameNanos { }
                if (settleToken != releaseSettleToken) {
                    return@launch
                }
                if (abs(overscrollOffset.value) > OVERSCROLL_EPSILON_PX) {
                    animateBackToRest()
                }
            }
    }

    fun consumeOverscrollDelta(delta: Float, allowCrossingZero: Boolean): Float {
        if (delta == 0f) {
            return 0f
        }

        val currentStretch = stretchDistanceForOffset(overscrollOffset.value)
        val newStretch =
            applyStretchDelta(
                currentStretch = currentStretch,
                delta = delta,
                maxStretch = maxStretchDistancePx(),
                allowCrossingZero = allowCrossingZero,
            )
        if (abs(newStretch - currentStretch) <= OVERSCROLL_EPSILON_PX) {
            return 0f
        }

        launchSnapToStretch(newStretch)
        return newStretch - currentStretch
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

                cancelPendingReleaseSettle()
                val delta = axis(available)
                val currentOffset = overscrollOffset.value
                if (
                    abs(currentOffset) <= OVERSCROLL_EPSILON_PX ||
                    delta == 0f ||
                    sign(delta) == sign(currentOffset)
                ) {
                    return Offset.Zero
                }

                return offsetOf(consumeOverscrollDelta(delta, allowCrossingZero = false))
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (source != NestedScrollSource.UserInput) {
                    return Offset.Zero
                }

                cancelPendingReleaseSettle()
                val delta = axis(available)
                if (delta == 0f) {
                    return Offset.Zero
                }

                return offsetOf(consumeOverscrollDelta(delta, allowCrossingZero = true))
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                cancelPendingReleaseSettle()
                return Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                cancelPendingReleaseSettle()
                val velocity = axis(available)
                val momentumThreshold =
                    maxOf(
                        viewConfiguration.minimumFlingVelocity * EDGE_MOMENTUM_THRESHOLD_MULTIPLIER,
                        MIN_EDGE_MOMENTUM_VELOCITY_PX,
                    )
                val hasOverscroll = abs(overscrollOffset.value) > OVERSCROLL_EPSILON_PX
                val hasMomentum = abs(velocity) >= momentumThreshold

                if (hasMomentum) {
                    animateBackToRest(initialVelocity = velocity)
                    return velocityOf(velocity)
                }

                if (hasOverscroll) {
                    animateBackToRest()
                }
                return Velocity.Zero
            }
        }
    }

    this
        .onSizeChanged { size ->
            axisSizePx =
                if (orientation == Orientation.Vertical) {
                    size.height
                } else {
                    size.width
                }
        }
        .pointerInput(orientation, enabled) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                cancelPendingReleaseSettle()

                do {
                    val event = awaitPointerEvent(pass = PointerEventPass.Final)
                    val anyPressed = event.changes.any { it.pressed }
                    if (!anyPressed) {
                        break
                    }
                } while (true)

                scheduleReleaseSettle()
            }
        }
        .nestedScroll(connection)
        .graphicsLayer {
            if (orientation == Orientation.Vertical) {
                translationY = overscrollOffset.value
            } else {
                translationX = overscrollOffset.value
            }
        }
}
