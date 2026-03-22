package com.spotifyhub.ui.common

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val SpotifyGreen = Color(0xFF1ED760)

@Composable
fun NowPlayingIndicator(
    modifier: Modifier = Modifier,
    color: Color = SpotifyGreen,
) {
    val transition = rememberInfiniteTransition(label = "now-playing-indicator")
    val barOne by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 900
                0.35f at 0
                1f at 240
                0.45f at 540
                0.8f at 900
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "bar-one",
    )
    val barTwo by transition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 780
                0.7f at 0
                0.3f at 180
                1f at 420
                0.4f at 780
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "bar-two",
    )
    val barThree by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 840
                0.45f at 0
                0.95f at 210
                0.25f at 500
                0.8f at 840
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "bar-three",
    )

    Row(
        modifier = modifier.height(16.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.Bottom,
    ) {
        WaveBar(level = barOne, color = color)
        WaveBar(level = barTwo, color = color)
        WaveBar(level = barThree, color = color)
    }
}

@Composable
private fun WaveBar(
    level: Float,
    color: Color,
) {
    Box(
        modifier = Modifier
            .width(3.dp)
            .fillMaxHeight(level.coerceIn(0.2f, 1f))
            .clip(RoundedCornerShape(3.dp))
            .background(color),
    )
}
