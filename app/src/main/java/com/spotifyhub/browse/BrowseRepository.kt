package com.spotifyhub.browse

import com.spotifyhub.browse.model.BrowseItem
import com.spotifyhub.browse.model.BrowseItemType
import com.spotifyhub.browse.model.HomeData
import com.spotifyhub.browse.model.HomeSection
import com.spotifyhub.browse.model.SectionStyle
import com.spotifyhub.spotify.api.SpotifyBrowseApi
import com.spotifyhub.spotify.mapper.BrowseMapper
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.util.Calendar

class BrowseRepository(
    private val browseApi: SpotifyBrowseApi,
) {
    private var cachedHomeData: HomeData? = null
    private var cacheTimestamp: Long = 0L
    private var cachedDisplayName: String? = null

    suspend fun getHomeData(forceRefresh: Boolean = false): HomeData {
        val now = System.currentTimeMillis()
        if (!forceRefresh && cachedHomeData != null && now - cacheTimestamp < CACHE_TTL_MS) {
            return cachedHomeData!!
        }

        return coroutineScope {
            val profileDeferred = async { runCatching { browseApi.getCurrentUserProfile() }.getOrNull() }
            val recentDeferred = async { runCatching { browseApi.getRecentlyPlayed(limit = 50) }.getOrNull() }
            val topTracksDeferred = async { runCatching { browseApi.getTopTracks(timeRange = "short_term", limit = 20) }.getOrNull() }
            val topArtistsDeferred = async { runCatching { browseApi.getTopArtists(timeRange = "medium_term", limit = 20) }.getOrNull() }
            val playlistsDeferred = async { runCatching { browseApi.getUserPlaylists(limit = 50) }.getOrNull() }
            val featuredDeferred = async { runCatching { browseApi.getFeaturedPlaylists(limit = 20) }.getOrNull() }

            val profile = profileDeferred.await()
            val recent = recentDeferred.await()
            val topTracks = topTracksDeferred.await()
            val topArtists = topArtistsDeferred.await()
            val playlists = playlistsDeferred.await()
            val featured = featuredDeferred.await()

            val displayName = profile?.displayName
            cachedDisplayName = displayName

            /* Build quick-access from recently played unique contexts */
            val recentItems = recent?.items.orEmpty().mapNotNull { BrowseMapper.mapPlayHistoryToBrowseItem(it) }
            val quickAccess = recentItems
                .distinctBy { it.contextUri ?: it.id }
                .take(6)

            /* Build sections */
            val sections = mutableListOf<HomeSection>()

            /* Made for You — filter personalized playlists */
            val madeForYou = playlists?.items.orEmpty()
                .filter { isMadeForYouPlaylist(it) }
                .mapNotNull { BrowseMapper.mapPlaylistToBrowseItem(it) }
            if (madeForYou.isNotEmpty()) {
                sections += HomeSection("Made for You", madeForYou, SectionStyle.HorizontalCards)
            }

            /* Recently Played */
            val recentUnique = recentItems.distinctBy { it.id }.take(20)
            if (recentUnique.isNotEmpty()) {
                sections += HomeSection("Recently Played", recentUnique, SectionStyle.HorizontalCards)
            }

            /* Your Top Artists */
            val artists = topArtists?.items.orEmpty().mapNotNull { BrowseMapper.mapArtistToBrowseItem(it) }
            if (artists.isNotEmpty()) {
                sections += HomeSection("Your Top Artists", artists, SectionStyle.HorizontalCircle)
            }

            /* Recommended — seed from top artists + tracks for recommendations */
            val seedArtistIds = topArtists?.items.orEmpty().take(2).mapNotNull { it.id }.joinToString(",")
            val seedTrackIds = topTracks?.items.orEmpty().take(3).mapNotNull { it.id }.joinToString(",")
            if (seedArtistIds.isNotBlank() || seedTrackIds.isNotBlank()) {
                val recs = runCatching {
                    browseApi.getRecommendations(
                        seedArtists = seedArtistIds.takeIf { it.isNotBlank() },
                        seedTracks = seedTrackIds.takeIf { it.isNotBlank() },
                        limit = 20,
                    )
                }.getOrNull()
                val recItems = recs?.tracks.orEmpty().mapNotNull { BrowseMapper.mapTrackToBrowseItem(it) }
                if (recItems.isNotEmpty()) {
                    sections += HomeSection("Recommended for You", recItems, SectionStyle.HorizontalCards)
                }
            }

            /* Featured Playlists */
            val featuredItems = featured?.playlists?.items.orEmpty().mapNotNull { BrowseMapper.mapPlaylistToBrowseItem(it) }
            if (featuredItems.isNotEmpty()) {
                val title = featured?.message?.takeIf { it.isNotBlank() } ?: "Featured Playlists"
                sections += HomeSection(title, featuredItems, SectionStyle.HorizontalCards)
            }

            val homeData = HomeData(
                greeting = buildGreeting(displayName),
                displayName = displayName,
                quickAccess = quickAccess,
                sections = sections,
            )
            cachedHomeData = homeData
            cacheTimestamp = now
            homeData
        }
    }

    fun getCachedDisplayName(): String? = cachedDisplayName

    private fun buildGreeting(displayName: String?): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val timeGreeting = when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else -> "Good evening"
        }
        return if (!displayName.isNullOrBlank()) {
            "$timeGreeting, $displayName"
        } else {
            timeGreeting
        }
    }

    private fun isMadeForYouPlaylist(
        playlist: com.spotifyhub.spotify.dto.browse.SimplifiedPlaylistDto,
    ): Boolean {
        val ownerIsSpotify = playlist.owner?.id == "spotify"
        val name = playlist.name?.lowercase() ?: return false
        val knownPatterns = listOf(
            "discover weekly",
            "release radar",
            "daily mix",
            "on repeat",
            "repeat rewind",
            "time capsule",
            "daylist",
            "blend",
            "your top songs",
            "chill mix",
            "focus mix",
            "energy mix",
            "mood mix",
            "summer rewind",
            "wrapped",
        )
        return ownerIsSpotify && knownPatterns.any { name.contains(it) }
    }

    companion object {
        private const val CACHE_TTL_MS = 5 * 60 * 1000L // 5 minutes
    }
}
