package com.spotifyhub.ui.main

import android.text.format.DateFormat
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spotifyhub.ui.detail.DetailScreen
import com.spotifyhub.ui.detail.DetailViewModel
import com.spotifyhub.ui.home.HomeScreen
import com.spotifyhub.ui.home.HomeViewModel
import com.spotifyhub.ui.icons.AppIcons
import com.spotifyhub.ui.library.LibraryScreen
import com.spotifyhub.ui.library.LibraryViewModel
import com.spotifyhub.ui.nowplaying.NowPlayingContent
import com.spotifyhub.ui.nowplaying.PlayerViewModel
import com.spotifyhub.ui.nowplaying.backdrop.AlbumBackdropHost
import com.spotifyhub.ui.rating.RatingScreen
import com.spotifyhub.ui.rating.RatingViewModel
import com.spotifyhub.ui.search.SearchScreen
import com.spotifyhub.ui.search.SearchViewModel
import kotlinx.coroutines.delay

/* ── Tokens ──────────────────────────────────────────────────────── */

private val SidebarShape = RoundedCornerShape(24.dp)
private val SidebarSurface = Color(0x2415181D)
private val SidebarBorder = Color.White.copy(alpha = 0.08f)
private val SurfaceShadow = Color.Black.copy(alpha = 0.20f)
private val SpotifyGreen = Color(0xFF1ED760)
private val BrowseBackdropStart = Color(0xFF132433)
private val BrowseBackdropMid = Color(0xFF161A22)
private val BrowseBackdropEnd = Color(0xFF101318)
private val BrowseContentSurface = Color(0xFF171A1F)
private val DetailSheetShape = RoundedCornerShape(0.dp)

/* ── Public entry-point ──────────────────────────────────────────── */

@Composable
fun MainScreen(
    mainViewModel: MainViewModel,
    playerViewModel: PlayerViewModel,
    homeViewModel: HomeViewModel,
    searchViewModel: SearchViewModel,
    libraryViewModel: LibraryViewModel,
    ratingViewModel: RatingViewModel,
    detailViewModel: DetailViewModel,
    isOffline: Boolean,
) {
    val selectedTab by mainViewModel.selectedTab.collectAsStateWithLifecycle()
    val showDetail by mainViewModel.showDetail.collectAsStateWithLifecycle()
    val playerUiState by playerViewModel.uiState.collectAsStateWithLifecycle()
    val currentTrackId = playerUiState.playback?.item?.id
    val isPlaybackActive = playerUiState.playback?.isPlaying == true
    val isNowPlayingTab = selectedTab == MainTab.NowPlaying
    val showSwipeDetail = showDetail && !isNowPlayingTab
    val baseOffsetX by animateDpAsState(
        targetValue = if (showSwipeDetail) (-22).dp else 0.dp,
        animationSpec = tween(durationMillis = 280),
        label = "browse-base-offset",
    )
    val baseAlpha by animateFloatAsState(
        targetValue = if (showSwipeDetail) 0.82f else 1f,
        animationSpec = tween(durationMillis = 280),
        label = "browse-base-alpha",
    )

    /* Helper lambda for handling album/playlist/track clicks from Home & Search */
    val handleBrowseItemClick: (com.spotifyhub.browse.model.BrowseItem) -> Unit = { item ->
        when (item.type) {
            com.spotifyhub.browse.model.BrowseItemType.Playlist -> {
                detailViewModel.loadPlaylist(item.id, fallbackItem = item)
                mainViewModel.openDetail()
            }
            com.spotifyhub.browse.model.BrowseItemType.Album -> {
                detailViewModel.loadAlbum(
                    albumId = item.id,
                    albumName = item.title,
                    artistName = item.subtitle,
                    artworkUrl = item.artworkUrl,
                    albumUri = item.uri,
                )
                mainViewModel.openDetail()
            }
            else -> {
                /* Track or Artist — play directly */
                detailViewModel.playTrack(item.uri)
            }
        }
    }

    /* Disable Compose's built-in overscroll so custom bounce handles all list surfaces. */
    CompositionLocalProvider(LocalOverscrollFactory provides null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF08090B)),
        ) {
            NowPlayingBackdropLayer(
                artworkUrl = playerUiState.playback?.item?.artworkUrl,
                artworkKey = playerUiState.playback?.item?.id,
            )

            if (!isNowPlayingTab) {
                BrowseBackdropLayer()
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SidebarRail(
                    selectedTab = selectedTab,
                    isOffline = isOffline,
                    onTabSelected = { mainViewModel.selectTab(it) },
                    modifier = Modifier.fillMaxHeight(),
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(28.dp)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .offset(x = baseOffsetX)
                                .graphicsLayer { alpha = baseAlpha },
                        ) {
                            Crossfade(
                                targetState = selectedTab,
                                animationSpec = tween(durationMillis = 250),
                                label = "tab-crossfade",
                            ) { tab ->
                                when (tab) {
                                    MainTab.Home -> HomeScreen(
                                        viewModel = homeViewModel,
                                        onItemClick = handleBrowseItemClick,
                                    )

                                    MainTab.Search -> SearchScreen(
                                        viewModel = searchViewModel,
                                        currentTrackId = currentTrackId,
                                        isPlaybackActive = isPlaybackActive,
                                        onItemClick = handleBrowseItemClick,
                                    )

                                    MainTab.Library -> LibraryScreen(
                                        viewModel = libraryViewModel,
                                        detailViewModel = detailViewModel,
                                        currentTrackId = currentTrackId,
                                        isPlaybackActive = isPlaybackActive,
                                        onNavigateToDetail = { mainViewModel.openDetail() },
                                    )

                                    MainTab.Rate -> RatingScreen(
                                        viewModel = ratingViewModel,
                                    )

                                    MainTab.NowPlaying -> NowPlayingContent(
                                        viewModel = playerViewModel,
                                        isOffline = isOffline,
                                        renderBackdrop = false,
                                    )
                                }
                            }
                        }
                    }

                    androidx.compose.animation.AnimatedVisibility(
                        visible = showSwipeDetail,
                        enter = slideInHorizontally(
                            initialOffsetX = { fullWidth -> fullWidth / 2 },
                            animationSpec = tween(durationMillis = 320),
                        ),
                        exit = slideOutHorizontally(
                            targetOffsetX = { fullWidth -> fullWidth },
                            animationSpec = tween(durationMillis = 260),
                        ),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .shadow(
                                    elevation = 28.dp,
                                    shape = DetailSheetShape,
                                    clip = false,
                                    ambientColor = SurfaceShadow,
                                    spotColor = SurfaceShadow,
                                )
                                .clip(DetailSheetShape)
                                .background(BrowseContentSurface),
                        ) {
                            DetailScreen(
                                viewModel = detailViewModel,
                                currentTrackId = currentTrackId,
                                isPlaybackActive = isPlaybackActive,
                                onBack = { mainViewModel.closeDetail() },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppBackdropLayer(
    artworkUrl: String?,
    artworkKey: String?,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        AlbumBackdropHost(
            artworkUrl = artworkUrl,
            artworkKey = artworkKey,
            blurPassCount = 8,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0x2808090C),
                            Color(0x10090A0C),
                            Color(0x32060709),
                        ),
                    ),
                ),
        )
    }
}

@Composable
private fun NowPlayingBackdropLayer(
    artworkUrl: String?,
    artworkKey: String?,
) {
    AppBackdropLayer(
        artworkUrl = artworkUrl,
        artworkKey = artworkKey,
    )
}

@Composable
private fun BrowseBackdropLayer() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        BrowseBackdropStart,
                        BrowseBackdropMid,
                        BrowseBackdropEnd,
                    ),
                ),
            ),
    )
}

/* ── Sidebar ─────────────────────────────────────────────────────── */

@Composable
private fun SidebarRail(
    selectedTab: MainTab,
    isOffline: Boolean,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .width(62.dp)
            .shadow(16.dp, SidebarShape, clip = false, ambientColor = SurfaceShadow, spotColor = SurfaceShadow)
            .clip(SidebarShape)
            .background(SidebarSurface)
            .border(1.dp, SidebarBorder, SidebarShape),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            /* Top: clock */
            Text(
                text = rememberClockLabel(),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.4.sp,
                ),
                color = Color.White,
            )

            /* Center: tab icons */
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TabIcon(
                    icon = if (selectedTab == MainTab.Home) AppIcons.homeSelected else AppIcons.home,
                    label = "Home",
                    isSelected = selectedTab == MainTab.Home,
                    onClick = { onTabSelected(MainTab.Home) },
                )
                TabIcon(
                    icon = if (selectedTab == MainTab.Search) AppIcons.searchSelected else AppIcons.search,
                    label = "Search",
                    isSelected = selectedTab == MainTab.Search,
                    onClick = { onTabSelected(MainTab.Search) },
                )
                TabIcon(
                    icon = if (selectedTab == MainTab.Library) AppIcons.librarySelected else AppIcons.library,
                    label = "Library",
                    isSelected = selectedTab == MainTab.Library,
                    onClick = { onTabSelected(MainTab.Library) },
                )
                TabIcon(
                    icon = if (selectedTab == MainTab.Rate) AppIcons.rateSelected else AppIcons.rate,
                    label = "Rate",
                    isSelected = selectedTab == MainTab.Rate,
                    onClick = { onTabSelected(MainTab.Rate) },
                )
                TabIcon(
                    icon = if (selectedTab == MainTab.NowPlaying) {
                        AppIcons.nowPlayingSelected
                    } else {
                        AppIcons.nowPlaying
                    },
                    label = "Playing",
                    isSelected = selectedTab == MainTab.NowPlaying,
                    onClick = { onTabSelected(MainTab.NowPlaying) },
                )
            }

            /* Bottom: status */
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(SpotifyGreen),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "S",
                        color = Color(0xFF08110B),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                    )
                }

                Text(
                    text = if (isOffline) "OFF" else "LIVE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        fontSize = 9.sp,
                    ),
                    color = if (isOffline) Color(0xFFFF8B8B) else SpotifyGreen,
                )
            }
        }
    }
}

@Composable
private fun TabIcon(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val tint = when {
        isSelected -> Color.White
        else -> Color.White.copy(alpha = 0.45f)
    }

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(22.dp),
            tint = tint,
        )
        Text(
            text = label,
            color = tint,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            ),
        )
    }
}

@Composable
private fun rememberClockLabel(): String {
    val label by produceState(initialValue = "") {
        while (true) {
            value = DateFormat.format("h:mm", System.currentTimeMillis()).toString()
            delay(30_000L)
        }
    }
    return label
}
