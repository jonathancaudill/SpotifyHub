package com.spotifyhub.ui.rating

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.shape.CircleShape
import sv.lib.squircleshape.SquircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.spotifyhub.playback.model.PlaybackItem
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/* ── Design tokens ───────────────────────────────────────────────── */

private val ArtworkShape = SquircleShape(18.dp)
private val CardSurface = Color(0x2415181D)
private val CardBorder = Color.White.copy(alpha = 0.08f)
private val SurfaceShadow = Color.Black.copy(alpha = 0.20f)
private val SpotifyGreen = Color(0xFF1ED760)
private val LockedGold = Color(0xFFD4A843)

private val TrackColorInactive = Color.White.copy(alpha = 0.10f)

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
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        /* Left column: album art + metadata + submit button */
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                /* Small album art */
                AlbumArtwork(
                    artworkUrl = item.artworkUrl,
                    title = item.album,
                    modifier = Modifier.size(110.dp),
                )

                /* Metadata beside art */
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = item.artist,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = item.album,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )

                    item.releaseDate?.takeIf { it.isNotBlank() }?.let { date ->
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = date,
                            color = Color.White.copy(alpha = 0.45f),
                            style = MaterialTheme.typography.bodySmall.copy(
                                letterSpacing = 0.5.sp,
                            ),
                        )
                    }

                    if (isLocked) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = "Already rated",
                                modifier = Modifier.size(12.dp),
                                tint = LockedGold,
                            )
                            Text(
                                text = "Already rated",
                                color = LockedGold,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                ),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            SubmitButton(
                submissionState = submissionState,
                errorMessage = errorMessage,
                isLocked = isLocked,
                isLookingUp = isLookingUp,
                onSubmit = onSubmit,
            )
        }

        /* Right: circular drag dial — use weight so it doesn't steal all width */
        Box(
            modifier = Modifier
                .weight(0.7f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center,
        ) {
            RatingDial(
                value = selectedRating,
                isLocked = isLocked,
                isLookingUp = isLookingUp,
                onValueChanged = onRatingChanged,
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(1f)
                    .padding(4.dp),
            )
        }
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

/* ── Circular drag dial (thermostat-style) ───────────────────────── */

/**
 * Arc spans 270 degrees: from 135° (bottom-left) clockwise to 45° (bottom-right).
 * The gap is at the bottom.  0.0 = left end, 10.0 = right end.
 */
private const val ARC_START_ANGLE = 135f
private const val ARC_SWEEP = 270f

@Composable
private fun RatingDial(
    value: Float,
    isLocked: Boolean,
    isLookingUp: Boolean,
    onValueChanged: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    var componentSize by remember { mutableStateOf(IntSize.Zero) }

    val fraction = (value / 10f).coerceIn(0f, 1f)
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = 80),
        label = "dial-fraction",
    )

    val activeColor = if (isLocked) LockedGold else SpotifyGreen

    Box(
        modifier = modifier
            .onSizeChanged { componentSize = it }
            .then(
                if (!isLocked && !isLookingUp) {
                    Modifier.pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            val cx = componentSize.width / 2f
                            val cy = componentSize.height / 2f
                            val dx = change.position.x - cx
                            val dy = change.position.y - cy

                            // atan2 gives angle from positive-x axis, in degrees
                            var angleDeg = Math
                                .toDegrees(atan2(dy.toDouble(), dx.toDouble()))
                                .toFloat()
                            if (angleDeg < 0f) angleDeg += 360f

                            // Map angle to fraction along our 270° arc starting at 135°
                            var relativeAngle = angleDeg - ARC_START_ANGLE
                            if (relativeAngle < 0f) relativeAngle += 360f

                            // If in the 90° dead zone at the bottom, snap to nearest end
                            val newFraction = when {
                                relativeAngle <= ARC_SWEEP -> relativeAngle / ARC_SWEEP
                                relativeAngle > ARC_SWEEP + (360f - ARC_SWEEP) / 2f -> 0f
                                else -> 1f
                            }.coerceIn(0f, 1f)

                            // Snap to 0.1 increments
                            val snapped = ((newFraction * 100f).roundToInt() / 10f)
                                .coerceIn(0f, 10f)

                            if (snapped != value) {
                                onValueChanged(snapped)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        }
                    }
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = size.minDimension * 0.09f
            val pad = strokeWidth / 2f + 4f
            val arcSize = Size(size.width - pad * 2, size.height - pad * 2)
            val topLeft = Offset(pad, pad)

            // Background track
            drawArc(
                color = TrackColorInactive,
                startAngle = ARC_START_ANGLE,
                sweepAngle = ARC_SWEEP,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )

            // Filled arc
            if (animatedFraction > 0f) {
                drawArc(
                    color = activeColor,
                    startAngle = ARC_START_ANGLE,
                    sweepAngle = ARC_SWEEP * animatedFraction,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }

            // Thumb dot at current position
            val thumbAngleRad = Math.toRadians(
                (ARC_START_ANGLE + ARC_SWEEP * animatedFraction).toDouble(),
            )
            val radius = arcSize.width / 2f
            val center = Offset(
                topLeft.x + arcSize.width / 2f,
                topLeft.y + arcSize.height / 2f,
            )
            val thumbPos = Offset(
                center.x + radius * cos(thumbAngleRad).toFloat(),
                center.y + radius * sin(thumbAngleRad).toFloat(),
            )
            drawCircle(
                color = Color.White,
                radius = strokeWidth * 0.72f,
                center = thumbPos,
            )
            drawCircle(
                color = activeColor,
                radius = strokeWidth * 0.46f,
                center = thumbPos,
            )
        }

        // Center label
        if (isLookingUp) {
            CircularProgressIndicator(
                color = Color.White.copy(alpha = 0.6f),
                strokeWidth = 2.dp,
                modifier = Modifier.size(28.dp),
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = String.format("%.1f", value),
                    color = if (isLocked) LockedGold else Color.White,
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Text(
                    text = "/ 10",
                    color = Color.White.copy(alpha = 0.35f),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp,
                    ),
                )
            }
        }
    }
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
            .clip(SquircleShape(14.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, SquircleShape(14.dp))
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
