package com.spotifyhub.artist
import com.spotifyhub.artist.model.ArtistBio
import com.spotifyhub.artist.model.ArtistDetail
import com.spotifyhub.artist.model.ArtistPlaylistSummary
import com.spotifyhub.artist.model.ArtistReleaseSummary
import com.spotifyhub.browse.model.BrowseItem
import com.spotifyhub.spotify.api.SpotifyArtistApi
import com.spotifyhub.spotify.api.SpotifySearchApi
import com.spotifyhub.spotify.api.WikipediaApi
import com.spotifyhub.spotify.dto.browse.SimplifiedPlaylistDto
import com.spotifyhub.spotify.mapper.ArtistMapper
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.Normalizer
import java.util.Collections

class ArtistRepository(
    private val artistApi: SpotifyArtistApi,
    private val searchApi: SpotifySearchApi,
    private val wikipediaApi: WikipediaApi,
) {
    private val bioCache = Collections.synchronizedMap(mutableMapOf<String, ArtistBio?>())

    suspend fun getArtistDetail(
        artistId: String,
        fallbackItem: BrowseItem? = null,
    ): ArtistDetail = coroutineScope {
        val artistDeferred = async { artistApi.getArtist(artistId) }
        val albumsDeferred = async { fetchArtistReleaseGroup(artistId, "album") }
        val singlesDeferred = async { fetchArtistReleaseGroup(artistId, "single") }
        val featuredOnDeferred = async { fetchArtistReleaseGroup(artistId, "appears_on") }
        val curatedPlaylistsDeferred = async {
            val artistName = fallbackItem?.title ?: artistDeferred.await().name.orEmpty()
            fetchCuratedPlaylists(artistName)
        }

        val artist = artistDeferred.await()
        ArtistMapper.mapArtistDetail(
            dto = artist,
            albums = albumsDeferred.await(),
            singlesAndEps = singlesDeferred.await(),
            featuredOn = featuredOnDeferred.await(),
            curatedPlaylists = curatedPlaylistsDeferred.await(),
        )
    }

    suspend fun getArtistBio(artistName: String): ArtistBio? {
        val normalized = normalizeForMatching(artistName)
        if (normalized.isBlank()) {
            return null
        }
        if (bioCache.containsKey(normalized)) {
            return bioCache[normalized]
        }

        val bio = runCatching {
            resolveWikipediaBio(artistName)
        }.getOrNull()
        bioCache[normalized] = bio
        return bio
    }

    private suspend fun fetchArtistReleaseGroup(
        artistId: String,
        includeGroup: String,
    ): List<ArtistReleaseSummary> {
        val releases = mutableListOf<ArtistReleaseSummary>()
        var offset = 0
        var hasNextPage = false
        do {
            val response = artistApi.getArtistAlbums(
                artistId = artistId,
                includeGroups = includeGroup,
                offset = offset,
            )
            val pageItems = response.items.orEmpty()
                .mapNotNull(ArtistMapper::mapRelease)
            releases += pageItems
            offset += pageItems.size
            if (pageItems.isEmpty()) {
                break
            }
            hasNextPage = offset < (response.total ?: releases.size) && response.next != null
        } while (hasNextPage)

        return releases
            .distinctBy { it.id }
            .sortedBy { it.title.lowercase() }
    }

    private suspend fun fetchCuratedPlaylists(artistName: String): List<ArtistPlaylistSummary> {
        if (artistName.isBlank()) {
            return emptyList()
        }

        val candidateQueries = listOf(
            "playlist:\"This Is $artistName\"",
            "playlist:\"Mixed By $artistName\"",
            "\"This Is $artistName\"",
            "\"Mixed By $artistName\"",
            "$artistName This Is",
            "$artistName Mixed By",
        )

        val matches = mutableListOf<ArtistPlaylistSummary>()
        for (query in candidateQueries) {
            val response = runCatching {
                searchApi.search(
                    query = query,
                    type = "playlist",
                    limit = 10,
                    market = "from_token",
                    includeExternal = "audio",
                )
            }.getOrNull() ?: continue

            val pageMatches = response.playlists?.items.orEmpty()
                .filterNotNull()
                .filter { isCuratedArtistPlaylist(it, artistName) }
                .sortedWith(
                    compareByDescending<SimplifiedPlaylistDto> { it.owner?.displayName.equals("Spotify", ignoreCase = true) }
                        .thenBy { it.name.orEmpty().length },
                )
                .mapNotNull(ArtistMapper::mapPlaylist)

            matches += pageMatches
            if (matches.size >= 4) {
                break
            }
        }

        return matches.distinctBy { it.id }.take(4)
    }

    private suspend fun resolveWikipediaBio(artistName: String): ArtistBio? {
        val searchResponse = wikipediaApi.searchPages("\"$artistName\"", limit = 5)
        val candidates = searchResponse.query?.search.orEmpty()
            .mapNotNull { it.title }
            .filter { title -> isConfidentWikiMatch(title, artistName) }

        for (title in candidates) {
            val summary = runCatching {
                wikipediaApi.getSummary(encodePathSegment(title))
            }.getOrNull() ?: continue

            if (summary.type.equals("disambiguation", ignoreCase = true)) {
                continue
            }

            val bio = ArtistMapper.mapBio(summary) ?: continue
            if (!isConfidentWikiMatch(bio.title, artistName)) {
                continue
            }
            if (!looksLikeArtistBio(summary.extract, summary.description)) {
                continue
            }
            return bio
        }

        return null
    }

    private fun isCuratedArtistPlaylist(
        playlist: SimplifiedPlaylistDto,
        artistName: String,
    ): Boolean {
        val normalizedPlaylistName = normalizeForMatching(playlist.name.orEmpty())
        val normalizedArtistName = normalizeForMatching(artistName)
        if (normalizedPlaylistName.isBlank() || normalizedArtistName.isBlank()) {
            return false
        }

        return normalizedPlaylistName == "this is $normalizedArtistName" ||
            normalizedPlaylistName == "mixed by $normalizedArtistName"
    }

    private fun isConfidentWikiMatch(
        candidateTitle: String,
        artistName: String,
    ): Boolean {
        val normalizedCandidate = normalizeForMatching(candidateTitle)
        val normalizedArtist = normalizeForMatching(artistName)
        if (normalizedCandidate == normalizedArtist) {
            return true
        }

        val strippedQualifier = normalizedCandidate.substringBefore("(").trim()
        return strippedQualifier == normalizedArtist
    }

    private fun normalizeForMatching(value: String): String {
        return Normalizer.normalize(value, Normalizer.Form.NFKD)
            .replace("\\p{M}+".toRegex(), "")
            .lowercase()
            .replace("[^a-z0-9() ]".toRegex(), " ")
            .replace("\\s+".toRegex(), " ")
            .trim()
    }

    private fun encodePathSegment(value: String): String {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20")
    }

    private fun looksLikeArtistBio(
        extract: String?,
        description: String?,
    ): Boolean {
        val haystack = listOf(extract.orEmpty(), description.orEmpty())
            .joinToString(" ")
            .lowercase()

        return listOf(
            "singer",
            "songwriter",
            "musician",
            "rapper",
            "band",
            "artist",
            "recording",
            "composer",
            "producer",
        ).any { keyword -> haystack.contains(keyword) }
    }
}
