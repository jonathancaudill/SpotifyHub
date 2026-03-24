package com.spotifyhub.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import sv.lib.squircleshape.SquircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.spotifyhub.browse.model.BrowseItem
import com.spotifyhub.browse.model.BrowseItemType
import com.spotifyhub.browse.model.HomeSection
import com.spotifyhub.browse.model.SectionStyle
import com.spotifyhub.ui.common.bounceOverscroll

private val BrowseBackground = Color(0xFF171A1F)
private val SpotifyGreen = Color(0xFF1ED760)
private val CardShape = SquircleShape(8.dp)

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onItemClick: (BrowseItem) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BrowseBackground),
    ) {
        when {
            uiState.isLoading && uiState.sections.isEmpty() -> {
                CircularProgressIndicator(
                    color = SpotifyGreen,
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            uiState.error != null && uiState.sections.isEmpty() -> {
                Text(
                    text = uiState.error ?: "Something went wrong",
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .bounceOverscroll(orientation = Orientation.Vertical),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    /* Greeting */
                    if (uiState.greeting.isNotBlank()) {
                        item {
                            Text(
                                text = uiState.greeting,
                                color = Color.White,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 26.sp,
                                ),
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }
                    }

                    /* Quick-access grid */
                    if (uiState.quickAccess.isNotEmpty()) {
                        item {
                            QuickAccessGrid(
                                items = uiState.quickAccess,
                                onItemClick = onItemClick,
                            )
                        }
                    }

                    /* Sections */
                    items(uiState.sections, key = { it.title }) { section ->
                        HomeSectionRow(
                            section = section,
                            onItemClick = onItemClick,
                        )
                    }
                }
            }
        }
    }
}

/* ── Quick-access compact grid (2 rows × 3 columns) ─────────────── */

@Composable
private fun QuickAccessGrid(
    items: List<BrowseItem>,
    onItemClick: (BrowseItem) -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.chunked(3).take(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowItems.forEach { item ->
                    QuickAccessCard(
                        item = item,
                        onClick = { onItemClick(item) },
                        modifier = Modifier.weight(1f),
                    )
                }
                /* Fill remaining spots if row has < 3 items */
                repeat(3 - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun QuickAccessCard(
    item: BrowseItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Row(
        modifier = modifier
            .height(52.dp)
            .clip(SquircleShape(6.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(item.artworkUrl)
                .crossfade(true)
                .build(),
            contentDescription = item.title,
            modifier = Modifier
                .size(52.dp)
                .clip(SquircleShape(topStart = 6.dp, bottomStart = 6.dp)),
            contentScale = ContentScale.Crop,
        )
        Text(
            text = item.title,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
            ),
            modifier = Modifier.padding(horizontal = 8.dp),
        )
    }
}

/* ── Section row ─────────────────────────────────────────────────── */

@Composable
private fun HomeSectionRow(
    section: HomeSection,
    onItemClick: (BrowseItem) -> Unit,
) {
    Column {
        Text(
            text = section.title,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
            ),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
        )
        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            modifier = Modifier.bounceOverscroll(orientation = Orientation.Horizontal),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(section.items, key = { it.id }) { item ->
                when (section.style) {
                    SectionStyle.HorizontalCircle -> ArtistCircleCard(
                        item = item,
                        onClick = { onItemClick(item) },
                    )

                    SectionStyle.HorizontalCards -> BrowseCard(
                        item = item,
                        onClick = { onItemClick(item) },
                    )
                }
            }
        }
    }
}

/* ── Standard browse card ────────────────────────────────────────── */

@Composable
private fun BrowseCard(
    item: BrowseItem,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .width(120.dp)
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(item.artworkUrl)
                .crossfade(true)
                .build(),
            contentDescription = item.title,
            modifier = Modifier
                .size(120.dp)
                .clip(CardShape),
            contentScale = ContentScale.Crop,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = item.title,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
            ),
        )
        if (item.subtitle.isNotBlank()) {
            Text(
                text = item.subtitle,
                color = Color.White.copy(alpha = 0.55f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 10.sp,
                ),
            )
        }
    }
}

/* ── Circular artist card ────────────────────────────────────────── */

@Composable
private fun ArtistCircleCard(
    item: BrowseItem,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .width(100.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(item.artworkUrl)
                .crossfade(true)
                .build(),
            contentDescription = item.title,
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = item.title,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
            ),
        )
    }
}
