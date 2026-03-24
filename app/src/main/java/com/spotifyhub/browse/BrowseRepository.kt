package com.spotifyhub.browse

import android.util.Log
import com.spotifyhub.BuildConfig
import com.spotifyhub.browse.model.BrowseItem
import com.spotifyhub.browse.model.BrowseItemType
import com.spotifyhub.browse.model.HomeData
import com.spotifyhub.browse.model.HomeSection
import com.spotifyhub.browse.model.SectionStyle
import com.spotifyhub.spotify.api.SpotifyBrowseApi
import com.spotifyhub.spotify.api.SpotifyEmbedApi
import com.spotifyhub.spotify.api.SpotifyLibraryApi
import com.spotifyhub.spotify.api.SpotifySearchApi
import com.spotifyhub.spotify.dto.browse.ArtistFullDto
import com.spotifyhub.spotify.dto.browse.PlayHistoryDto
import com.spotifyhub.spotify.dto.browse.SimplifiedPlaylistDto
import com.spotifyhub.spotify.mapper.BrowseMapper
import com.spotifyhub.spotify.mapper.SearchMapper
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.util.Calendar

class BrowseRepository(
    private val browseApi: SpotifyBrowseApi,
    private val libraryApi: SpotifyLibraryApi,
    private val searchApi: SpotifySearchApi,
    private val embedApi: SpotifyEmbedApi,
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
            val recentDeferred = async { runCatching { fetchRecentlyPlayed() }.getOrNull().orEmpty() }
            val topTracksShortDeferred = async { runCatching { browseApi.getTopTracks(timeRange = "short_term", limit = 20) }.getOrNull() }
            val topTracksMediumDeferred = async { runCatching { browseApi.getTopTracks(timeRange = "medium_term", limit = 20) }.getOrNull() }
            val topArtistsDeferred = async { runCatching { browseApi.getTopArtists(timeRange = "medium_term", limit = 20) }.getOrNull() }
            val allPlaylistsDeferred = async { runCatching { fetchAllUserPlaylists() }.getOrNull().orEmpty() }
            val savedAlbumsDeferred = async { runCatching { libraryApi.getSavedAlbums(limit = 20) }.getOrNull() }
            val savedTracksDeferred = async { runCatching { libraryApi.getSavedTracks(limit = 20) }.getOrNull() }
            val pinnedPlaylistSectionsDeferred = async { fetchPinnedPlaylistSections() }

            val profile = profileDeferred.await()
            val recentHistory = recentDeferred.await()
            val topTracksShort = topTracksShortDeferred.await()
            val topTracksMedium = topTracksMediumDeferred.await()
            val topArtists = topArtistsDeferred.await()
            val allPlaylists = allPlaylistsDeferred.await()
            val savedAlbums = savedAlbumsDeferred.await()
            val savedTracks = savedTracksDeferred.await()
            val pinnedPlaylistSections = pinnedPlaylistSectionsDeferred.await()

            val displayName = profile?.displayName
            cachedDisplayName = displayName

            /* Build recently-played items (tracks) for the "Recently Played" section */
            val recentItems = recentHistory.mapNotNull { BrowseMapper.mapPlayHistoryToBrowseItem(it) }

            /* ── Your Top Albums Right Now ─────────────────────────────
             * Extract albums from recently played tracks, rank by play
             * frequency, and use them for both the quick-access grid and
             * a dedicated section.
             */
            val recentAlbums = recentHistory
                .mapNotNull { it.track?.let(BrowseMapper::mapAlbumFromTrack) }
            // Count how many tracks were played per album to rank "faves"
            val albumPlayCounts = recentAlbums
                .groupingBy { it.id }
                .eachCount()
            // Deduplicate, keeping first occurrence (most recent), sort by play count desc
            val recentFaveAlbums = recentAlbums
                .distinctBy { it.id }
                .sortedByDescending { albumPlayCounts[it.id] ?: 0 }

            /* Quick-access grid: show top albums right now instead of individual tracks */
            val quickAccess = recentFaveAlbums.take(6)

            /* ── Categorize personalized playlists ──────────────────── */
            val categorized = allPlaylists
                .mapNotNull { playlist ->
                    categorizePlaylist(playlist)?.let { cat -> cat to playlist }
                }
                .groupBy({ it.first }, { it.second })
            val pinnedPlaylistIds = pinnedPersonalizedPlaylists.values.flatten().toSet()
            val personalizedPlaylistIds = categorized.values.flatten().mapNotNull { it.id }.toSet()
            val libraryPlaylistItems = allPlaylists
                .filter { it.id !in personalizedPlaylistIds && it.id !in pinnedPlaylistIds }
                .mapNotNull { BrowseMapper.mapPlaylistToBrowseItem(it) }
                .take(20)
            val topTrackItems = topTracksShort?.items.orEmpty()
                .mapNotNull { BrowseMapper.mapTrackToBrowseItem(it) }
                .distinctBy { it.id }
            val topTrackDeepCuts = topTracksMedium?.items.orEmpty()
                .mapNotNull { BrowseMapper.mapTrackToBrowseItem(it) }
                .distinctBy { it.id }
                .filterNot { mediumItem -> topTrackItems.any { it.id == mediumItem.id } }
            val savedAlbumItems = savedAlbums?.items.orEmpty()
                .mapNotNull { savedAlbum ->
                    val album = savedAlbum.album ?: return@mapNotNull null
                    val id = album.id ?: return@mapNotNull null
                    com.spotifyhub.browse.model.BrowseItem(
                        id = id,
                        title = album.name.orEmpty(),
                        subtitle = album.artists.orEmpty().joinToString(", ") { it.name.orEmpty() },
                        artworkUrl = album.images.orEmpty().firstOrNull()?.url,
                        uri = album.uri.orEmpty(),
                        type = com.spotifyhub.browse.model.BrowseItemType.Album,
                    )
                }
                .take(20)
            val savedTrackItems = savedTracks?.items.orEmpty()
                .mapNotNull { it.track?.let(BrowseMapper::mapTrackToBrowseItem) }
                .distinctBy { it.id }
                .take(20)
            val genreSections = fetchGenreSearchSections(topArtists?.items.orEmpty())

            /* ── Build sections in Spotify-like order ───────────────── */
            val sections = mutableListOf<HomeSection>()
            /* 1. Discover Weekly & Release Radar */
            pinnedPlaylistSections
                .firstOrNull { it.title == "Discover Weekly & Release Radar" }
                ?.let { sections += it }
                ?: addPlaylistSection(
                    sections, categorized, PersonalizedCategory.DISCOVER_RELEASE,
                    "Discover Weekly & Release Radar",
                )

            /* 2. Your Daily Mixes */
            pinnedPlaylistSections
                .firstOrNull { it.title == "Your Daily Mixes" }
                ?.let { sections += it }
                ?: addPlaylistSection(
                    sections, categorized, PersonalizedCategory.DAILY_MIX,
                    "Your Daily Mixes",
                )

            /* 3. Your Top Albums Right Now */
            recentFaveAlbums
                .takeIf { it.isNotEmpty() }
                ?.let { sections += HomeSection("Your Top Albums Right Now", it, SectionStyle.HorizontalCards) }

            /* 4. Recently Played */
            val recentUnique = recentItems.distinctBy { it.id }.take(20)
            if (recentUnique.isNotEmpty()) {
                sections += HomeSection("Recently Played", recentUnique, SectionStyle.HorizontalCards)
            }

            /* 5. Your Top Tracks Right Now */
            topTrackItems
                .takeIf { it.isNotEmpty() }
                ?.let { sections += HomeSection("Your Top Tracks Right Now", it, SectionStyle.HorizontalCards) }

            /* 5. Your Top Artists */
            val artists = topArtists?.items.orEmpty().mapNotNull { BrowseMapper.mapArtistToBrowseItem(it) }
            if (artists.isNotEmpty()) {
                sections += HomeSection("Your Top Artists", artists, SectionStyle.HorizontalCircle)
            }

            /* 6. Mixes for You (genre/mood mixes) */
            addPlaylistSection(
                sections, categorized, PersonalizedCategory.FOR_YOU_MIX,
                "Mixes for You",
            )

            /* 7. Made by Spotify AI (daylist, blend, etc.) */
            addPlaylistSection(
                sections, categorized, PersonalizedCategory.AI_PLAYLIST,
                "Made by Spotify AI",
            )

            /* 8. Saved Albums */
            savedAlbumItems
                .takeIf { it.isNotEmpty() }
                ?.let { sections += HomeSection("Saved Albums", it, SectionStyle.HorizontalCards) }

            /* 9. Deep-Cut Favorites */
            topTrackDeepCuts
                .takeIf { it.isNotEmpty() }
                ?.let { sections += HomeSection("Deep-Cut Favorites", it, SectionStyle.HorizontalCards) }

            /* 10. Made for You (catch-all remaining personalized) */
            addPlaylistSection(
                sections, categorized, PersonalizedCategory.OTHER_PERSONALIZED,
                "Made for You",
            )

            /* 11. Saved Songs */
            savedTrackItems
                .takeIf { it.isNotEmpty() }
                ?.let { sections += HomeSection("Saved Songs", it, SectionStyle.HorizontalCards) }

            /* 12. Your Playlists */
            libraryPlaylistItems
                .takeIf { it.isNotEmpty() }
                ?.let { sections += HomeSection("Your Playlists", it, SectionStyle.HorizontalCards) }

            /* 13. Search-driven genre discovery */
            sections += genreSections

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

    /* ── Playlist pagination ───────────────────────────────────────── */

    private suspend fun fetchAllUserPlaylists(): List<SimplifiedPlaylistDto> {
        val allPlaylists = mutableListOf<SimplifiedPlaylistDto>()
        var offset = 0
        val limit = 50
        do {
            val page = browseApi.getUserPlaylists(limit = limit, offset = offset)
            allPlaylists.addAll(page.items.orEmpty())
            if (allPlaylists.size >= MAX_PLAYLISTS_TO_FETCH) {
                return allPlaylists.take(MAX_PLAYLISTS_TO_FETCH)
            }
            offset += limit
            // Cap at 200 to avoid excessive API calls for users with huge libraries
        } while (page.next != null && allPlaylists.size < MAX_PLAYLISTS_TO_FETCH)
        return allPlaylists
    }

    /* ── Recently-played cursor pagination ────────────────────────── */

    /**
     * Fetches up to [MAX_RECENT_HISTORY] recently-played items using cursor
     * pagination. The Spotify API caps each request at 50, but provides a
     * `before` cursor so we can walk backwards through the history.
     */
    private suspend fun fetchRecentlyPlayed(): List<PlayHistoryDto> {
        val all = mutableListOf<PlayHistoryDto>()
        var beforeCursor: Long? = null
        do {
            val page = browseApi.getRecentlyPlayed(limit = 50, before = beforeCursor)
            val items = page.items.orEmpty()
            if (items.isEmpty()) break
            all.addAll(items)
            if (all.size >= MAX_RECENT_HISTORY) return all.take(MAX_RECENT_HISTORY)
            // The `before` cursor is a Unix-ms timestamp; parse it for the next page
            beforeCursor = page.cursors?.before?.toLongOrNull() ?: break
        } while (page.next != null)
        return all
    }

    /* ── Search-driven discovery sections ─────────────────────────── */

    private suspend fun fetchGenreSearchSections(
        topArtists: List<ArtistFullDto>,
    ): List<HomeSection> = coroutineScope {
        val topGenres = topArtists
            .flatMap { it.genres.orEmpty() }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .map { it.key }
            .take(2)

        topGenres.map { genre ->
            async {
                val results = runCatching {
                    searchApi.search(
                        query = "genre:\"$genre\"",
                        type = "playlist",
                        limit = 10,
                        market = "from_token",
                        includeExternal = "audio",
                    )
                }.getOrNull()
                val items = results?.let(SearchMapper::map)?.playlists.orEmpty()
                if (items.isNotEmpty()) {
                    HomeSection("Explore $genre", items, SectionStyle.HorizontalCards)
                } else null
            }
        }.awaitAll().filterNotNull()
    }

    private suspend fun fetchPinnedPlaylistSections(): List<HomeSection> = coroutineScope {
        pinnedPersonalizedPlaylists.map { (title, playlistIds) ->
            async {
                val items = playlistIds.mapNotNull { playlistId ->
                    val playlistItem = runCatching {
                        libraryApi.getPlaylist(playlistId)
                    }.onFailure { error ->
                        Log.w(TAG, "Pinned playlist Web API fetch failed for $playlistId", error)
                    }.getOrNull()?.let { playlist ->
                        SimplifiedPlaylistDto(
                            id = playlist.id,
                            name = playlist.name,
                            images = playlist.images,
                            owner = playlist.owner?.let { owner ->
                                com.spotifyhub.spotify.dto.browse.PlaylistOwnerDto(
                                    id = owner.id,
                                    displayName = owner.displayName,
                                )
                            },
                            uri = playlist.uri,
                            description = playlist.description,
                        )
                    }?.let(BrowseMapper::mapPlaylistToBrowseItem)

                    playlistItem ?: fetchPinnedPlaylistFallback(playlistId)
                }

                if (items.isNotEmpty()) {
                    HomeSection(title, items, SectionStyle.HorizontalCards)
                } else {
                    null
                }
            }
        }.awaitAll().filterNotNull()
    }

    /* ── Playlist categorization ───────────────────────────────────── */

    private enum class PersonalizedCategory {
        DISCOVER_RELEASE,
        DAILY_MIX,
        AI_PLAYLIST,
        FOR_YOU_MIX,
        OTHER_PERSONALIZED,
    }

    private fun categorizePlaylist(playlist: SimplifiedPlaylistDto): PersonalizedCategory? {
        val name = playlist.name?.lowercase() ?: return null

        return when {
            name.contains("discover weekly") || name.contains("release radar") ->
                PersonalizedCategory.DISCOVER_RELEASE

            name.contains("daily mix") ->
                PersonalizedCategory.DAILY_MIX

            name.contains("daylist") || name.contains("blend") || name.contains("dj") ->
                PersonalizedCategory.AI_PLAYLIST

            name.contains("chill mix") || name.contains("focus mix") ||
                name.contains("energy mix") || name.contains("mood mix") ||
                name.contains("rock mix") || name.contains("pop mix") ||
                name.contains("indie mix") || name.contains("jazz mix") ||
                name.contains("r&b mix") || name.contains("hip hop mix") ||
                name.contains("country mix") || name.contains("electronic mix") ||
                name.contains("classical mix") || name.contains("metal mix") ||
                name.contains("latin mix") || name.contains("folk mix") ||
                name.contains("soul mix") || name.contains("punk mix") ||
                name.contains("blues mix") || name.contains("reggae mix") ||
                (name.endsWith(" mix") && !name.contains("daily mix")) ->
                PersonalizedCategory.FOR_YOU_MIX

            name.contains("on repeat") || name.contains("repeat rewind") ||
                name.contains("time capsule") || name.contains("your top songs") ||
                name.contains("summer rewind") || name.contains("wrapped") ->
                PersonalizedCategory.OTHER_PERSONALIZED

            else -> null
        }
    }

    private fun addPlaylistSection(
        sections: MutableList<HomeSection>,
        categorized: Map<PersonalizedCategory, List<SimplifiedPlaylistDto>>,
        category: PersonalizedCategory,
        title: String,
    ) {
        val items = categorized[category]
            ?.mapNotNull { BrowseMapper.mapPlaylistToBrowseItem(it) }
            .orEmpty()
        if (items.isNotEmpty()) {
            sections += HomeSection(title, items, SectionStyle.HorizontalCards)
        }
    }

    /* ── Helpers ────────────────────────────────────────────────────── */

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

    companion object {
        private const val CACHE_TTL_MS = 5 * 60 * 1000L // 5 minutes
        private const val MAX_PLAYLISTS_TO_FETCH = 200
        /** Max recently-played items to fetch via cursor pagination (4 pages × 50). */
        private const val MAX_RECENT_HISTORY = 200
        private const val TAG = "BrowseRepository"
        private val openSpotifyPlaylistRegex =
            Regex("(?i)(?:https?://)?open\\.spotify\\.com/playlist/([A-Za-z0-9]{22})")
        private val spotifyPlaylistUriRegex = Regex("(?i)^spotify:playlist:([A-Za-z0-9]{22})$")
        private val spotifyIdRegex = Regex("^[A-Za-z0-9]{22}$")
    }

    private val pinnedPersonalizedPlaylists: LinkedHashMap<String, List<String>>
        get() = linkedMapOf<String, List<String>>().apply {
            val discoverRelease = parsePlaylistIds(BuildConfig.SPOTIFY_HOME_DISCOVER_RELEASE_IDS)
            if (discoverRelease.isNotEmpty()) {
                put("Discover Weekly & Release Radar", discoverRelease)
            }

            val dailyMixes = parsePlaylistIds(BuildConfig.SPOTIFY_HOME_DAILY_MIX_IDS)
            if (dailyMixes.isNotEmpty()) {
                put("Your Daily Mixes", dailyMixes)
            }
        }

    private fun parsePlaylistIds(raw: String): List<String> {
        return raw.split(",")
            .mapNotNull(::normalizePlaylistId)
            .distinct()
    }

    private fun normalizePlaylistId(rawValue: String): String? {
        val value = rawValue
            .trim()
            .removeSurrounding("\"")
            .removeSurrounding("'")

        if (value.isBlank()) {
            return null
        }

        spotifyPlaylistUriRegex.matchEntire(value)
            ?.groupValues
            ?.getOrNull(1)
            ?.let { return it }

        openSpotifyPlaylistRegex.find(value)
            ?.groupValues
            ?.getOrNull(1)
            ?.let { return it }

        val cleaned = value.substringBefore("?").substringBefore("/")
        return if (spotifyIdRegex.matches(cleaned)) cleaned else null
    }

    private suspend fun fetchPinnedPlaylistFallback(playlistId: String): BrowseItem? {
        val playlistUrl = "https://open.spotify.com/playlist/$playlistId"
        return runCatching {
            embedApi.getOEmbed(url = playlistUrl)
        }.onFailure { error ->
            Log.w(TAG, "Pinned playlist oEmbed fallback failed for $playlistId", error)
        }.getOrNull()?.let { embed ->
            BrowseItem(
                id = playlistId,
                title = embed.title.orEmpty().ifBlank { "Spotify Playlist" },
                subtitle = "Spotify",
                artworkUrl = embed.thumbnail_url,
                uri = "spotify:playlist:$playlistId",
                type = BrowseItemType.Playlist,
            )
        }
    }
}
