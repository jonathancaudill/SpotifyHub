package com.spotifyhub.ui.rating

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.spotifyhub.playback.model.PlaybackItem
import kotlin.math.abs
import kotlin.math.roundToInt

/* ── Design tokens ───────────────────────────────────────────────── */

private val ArtworkShape = RoundedCornerShape(18.dp)
private val CardSurface = Color(0x2415181D)
private val CardBorder = Color.White.copy(alpha = 0.08f)
private val SurfaceShadow = Color.Black.copy(alpha = 0.20f)
private val SpotifyGreen = Color(0xFF1ED760)
private val LockedGold = Color(0xFFD4A843)

/* ── Public entry-point ──────────────────────────────────────────── */

@Composable
fun RatingScreen(
    viewModel: RatingViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF132433),
                        Color(0xFF161A22),
                        Color(0xFF101318),
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (uiState.currentItem == null) {
            EmptyState()
        } else {
            RatingContent(
                item = uiState.currentItem!!,
                selectedRating = uiState.selectedRating,
                submissionState = uiState.submissionState,
                errorMessage = uiState.errorMessage,
                existingRating = uiState.existingRating,
                isLookingUp = uiState.isLookingUp,
                isLocked = uiState.isLocked,
                onRatingChanged = viewModel::setRating,
                onSubmit = viewModel::submitRating,
            )
        }
    }
}

/* ── Empty state ─────────────────────────────────────────────────── */

@Composable
private fun EmptyState() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "?",
                color = Color.White.copy(alpha = 0.4f),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Nothing Playing",
            color = Color.White.copy(alpha = 0.6f),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Play an album to rate it",
            color = Color.White.copy(alpha = 0.35f),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/* ── Main rating content ─────────────────────────────────────────── */

@Composable
private fun RatingContent(
    item: PlaybackItem,
    selectedRating: Float,
    submissionState: SubmissionState,
    errorMessage: String?,
    existingRating: Float?,
    isLookingUp: Boolean,
    isLocked: Boolean,
    onRatingChanged: (Float) -> Unit,
    onSubmit: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        /* Left: album artwork */
        AlbumArtwork(
            artworkUrl = item.artworkUrl,
            title = item.album,
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(1f),
        )

        /* Center: metadata + submit */
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = item.artist,
                color = Color.White.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = item.album,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )

            item.releaseDate?.takeIf { it.isNotBlank() }?.let { date ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = date,
                    color = Color.White.copy(alpha = 0.45f),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        letterSpacing = 0.5.sp,
                    ),
                )
            }

            if (isLocked) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = "Already rated",
                        modifier = Modifier.size(14.dp),
                        tint = LockedGold,
                    )
                    Text(
                        text = "Already rated",
                        color = LockedGold,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            SubmitButton(
                submissionState = submissionState,
                errorMessage = errorMessage,
                isLocked = isLocked,
                isLookingUp = isLookingUp,
                onSubmit = onSubmit,
            )
        }

        /* Right: rating wheel */
        RatingWheel(
            value = selectedRating,
            isLocked = isLocked,
            isLookingUp = isLookingUp,
            onValueChanged = onRatingChanged,
            modifier = Modifier
                .fillMaxHeight()
                .width(100.dp),
        )
    }
}

/* ── Album artwork ───────────────────────────────────────────────── */

@Composable
private fun AlbumArtwork(
    artworkUrl: String?,
    title: String?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .shadow(24.dp, ArtworkShape, clip = false, ambientColor = SurfaceShadow, spotColor = SurfaceShadow)
            .clip(ArtworkShape)
            .background(Color(0x2014191F))
            .border(1.dp, CardBorder, ArtworkShape),
        contentAlignment = Alignment.Center,
    ) {
        if (artworkUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(artworkUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            ArtworkPlaceholder()
        }
    }
}

@Composable
private fun ArtworkPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF30343C),
                        Color(0xFF171A1F),
                        Color(0xFF090A0D),
                    ),
                    radius = 620f,
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "S",
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
            )
        }
    }
}

/* ── Rating wheel ────────────────────────────────────────────────── */

@Composable
private fun RatingWheel(
    value: Float,
    isLocked: Boolean,
    isLookingUp: Boolean,
    onValueChanged: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    var accumulatedDrag by remember { mutableFloatStateOf(0f) }

    // The number of visible "notches" above and below center
    val visibleNotches = 5

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(CardSurface)
            .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
            .then(
                if (!isLocked && !isLookingUp) {
                    Modifier.pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragStart = { accumulatedDrag = 0f },
                            onVerticalDrag = { _, dragAmount ->
                                accumulatedDrag += dragAmount
                                // Each 18px of drag = 0.1 increment
                                val steps = (accumulatedDrag / 18f).toInt()
                                if (steps != 0) {
                                    accumulatedDrag -= steps * 18f
                                    // Drag up (negative) = increase, drag down = decrease
                                    val newValue = (value - steps * 0.1f)
                                        .coerceIn(0f, 10f)
                                        .let { (it * 10).roundToInt() / 10f }
                                    if (newValue != value) {
                                        onValueChanged(newValue)
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                }
                            },
                        )
                    }
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (isLookingUp) {
            CircularProgressIndicator(
                color = Color.White.copy(alpha = 0.6f),
                strokeWidth = 2.dp,
                modifier = Modifier.size(28.dp),
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // Show notches above center
                for (i in visibleNotches downTo 1) {
                    val notchValue = value + i * 0.1f
                    if (notchValue in 0f..10f) {
                        WheelNotch(
                            displayValue = notchValue,
                            distanceFromCenter = i,
                            isLocked = isLocked,
                        )
                    } else {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // Center value — the selected one
                CenterValue(
                    value = value,
                    isLocked = isLocked,
                )

                // Show notches below center
                for (i in 1..visibleNotches) {
                    val notchValue = value - i * 0.1f
                    if (notchValue in 0f..10f) {
                        WheelNotch(
                            displayValue = notchValue,
                            distanceFromCenter = i,
                            isLocked = isLocked,
                        )
                    } else {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            // Fade edges
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color(0xFF161A22),
                            0.25f to Color.Transparent,
                            0.75f to Color.Transparent,
                            1f to Color(0xFF161A22),
                        ),
                    ),
            )

            // Center highlight line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .align(Alignment.Center)
                    .graphicsLayer { translationY = -20f }
                    .background(
                        if (isLocked) LockedGold.copy(alpha = 0.3f)
                        else Color.White.copy(alpha = 0.12f),
                    ),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .align(Alignment.Center)
                    .graphicsLayer { translationY = 20f }
                    .background(
                        if (isLocked) LockedGold.copy(alpha = 0.3f)
                        else Color.White.copy(alpha = 0.12f),
                    ),
            )
        }
    }
}

@Composable
private fun CenterValue(
    value: Float,
    isLocked: Boolean,
) {
    val color = if (isLocked) LockedGold else Color.White

    Text(
        text = String.format("%.1f", value),
        color = color,
        style = MaterialTheme.typography.headlineMedium.copy(
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
        ),
        modifier = Modifier.padding(vertical = 2.dp),
    )
}

@Composable
private fun WheelNotch(
    displayValue: Float,
    distanceFromCenter: Int,
    isLocked: Boolean,
) {
    val alpha by animateFloatAsState(
        targetValue = when {
            distanceFromCenter <= 1 -> 0.50f
            distanceFromCenter <= 2 -> 0.35f
            distanceFromCenter <= 3 -> 0.20f
            else -> 0.10f
        },
        animationSpec = tween(durationMillis = 100),
        label = "notch-alpha",
    )
    val fontSize = when {
        distanceFromCenter <= 1 -> 14.sp
        distanceFromCenter <= 2 -> 12.sp
        else -> 10.sp
    }

    val color = if (isLocked) LockedGold else Color.White

    Text(
        text = String.format("%.1f", displayValue),
        color = color.copy(alpha = alpha),
        style = MaterialTheme.typography.bodySmall.copy(
            fontSize = fontSize,
            fontWeight = FontWeight.Medium,
        ),
        modifier = Modifier.padding(vertical = 1.dp),
    )
}

/* ── Submit button ───────────────────────────────────────────────── */

@Composable
private fun SubmitButton(
    submissionState: SubmissionState,
    errorMessage: String?,
    isLocked: Boolean,
    isLookingUp: Boolean,
    onSubmit: () -> Unit,
) {
    val enabled = !isLocked && !isLookingUp && submissionState == SubmissionState.Idle

    val backgroundColor = when {
        isLocked -> CardSurface
        submissionState == SubmissionState.Success -> SpotifyGreen.copy(alpha = 0.25f)
        submissionState == SubmissionState.Error -> Color(0x40FF4444)
        else -> SpotifyGreen.copy(alpha = 0.18f)
    }
    val borderColor = when {
        isLocked -> LockedGold.copy(alpha = 0.3f)
        submissionState == SubmissionState.Success -> SpotifyGreen.copy(alpha = 0.5f)
        submissionState == SubmissionState.Error -> Color(0xFFFF4444).copy(alpha = 0.4f)
        else -> SpotifyGreen.copy(alpha = 0.35f)
    }
    val textColor = when {
        isLocked -> LockedGold.copy(alpha = 0.6f)
        !enabled -> Color.White.copy(alpha = 0.4f)
        else -> Color.White
    }

    Box(
        modifier = Modifier
            .fillMaxWidth(0.7f)
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .then(
                if (enabled) Modifier.clickable(onClick = onSubmit)
                else Modifier,
            ),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = submissionState,
            label = "submit-content",
        ) { state ->
            when (state) {
                SubmissionState.Idle -> {
                    Text(
                        text = if (isLocked) "Already Rated" else "Submit Rating",
                        color = textColor,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.8.sp,
                        ),
                    )
                }

                SubmissionState.Submitting -> {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp),
                    )
                }

                SubmissionState.Success -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Success",
                            modifier = Modifier.size(18.dp),
                            tint = SpotifyGreen,
                        )
                        Text(
                            text = "Saved",
                            color = SpotifyGreen,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                        )
                    }
                }

                SubmissionState.Error -> {
                    Text(
                        text = errorMessage ?: "Error",
                        color = Color(0xFFFF8B8B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}
