package com.spotifyhub.search

import com.spotifyhub.spotify.api.SpotifySearchApi
import com.spotifyhub.spotify.dto.browse.ArtistFullDto
import com.spotifyhub.spotify.dto.browse.PlaylistOwnerDto
import com.spotifyhub.spotify.dto.browse.PlaylistTracksRefDto
import com.spotifyhub.spotify.dto.browse.SimplifiedPlaylistDto
import com.spotifyhub.spotify.dto.player.AlbumDto
import com.spotifyhub.spotify.dto.player.ArtistDto
import com.spotifyhub.spotify.dto.player.ImageDto
import com.spotifyhub.spotify.dto.player.TrackDto
import com.spotifyhub.spotify.dto.search.AlbumPagingDto
import com.spotifyhub.spotify.dto.search.ArtistPagingDto
import com.spotifyhub.spotify.dto.search.PlaylistPagingDto
import com.spotifyhub.spotify.dto.search.SearchResponseDto
import com.spotifyhub.spotify.dto.search.SimplifiedAlbumDto
import com.spotifyhub.spotify.dto.search.TrackPagingDto
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SearchRepositoryTest {

    @Test
    fun `search sends market and include external params`() = runTest {
        val api = FakeSpotifySearchApi(
            responses = mapOf(
                "phoebe bridgers" to responseWithTrack(name = "Motion Sickness"),
            ),
        )
        val repository = SearchRepository(api)

        val results = repository.search("phoebe bridgers")

        assertFalse(results.isEmpty)
        assertEquals(1, api.calls.size)
        val call = api.calls.first()
        assertEquals("from_token", call.market)
        assertEquals("audio", call.includeExternal)
    }

    @Test
    fun `search falls back to punctuation-stripped query when first attempt is empty`() = runTest {
        val api = FakeSpotifySearchApi(
            responses = mapOf(
                "ac dc thunderstruck" to responseWithTrack(name = "Thunderstruck"),
            ),
        )
        val repository = SearchRepository(api)

        val results = repository.search("ac/dc thunderstruck")

        assertEquals(listOf("ac/dc thunderstruck", "ac dc thunderstruck"), api.calls.map { it.query })
        assertEquals("Thunderstruck", results.tracks.first().title)
    }

    @Test
    fun `search ranks exact title match above partial matches`() = runTest {
        val api = FakeSpotifySearchApi(
            responses = mapOf(
                "hello" to SearchResponseDto(
                    tracks = TrackPagingDto(
                        items = listOf(
                            track(id = "2", name = "Say Hello"),
                            track(id = "1", name = "Hello"),
                            track(id = "3", name = "Hello Again"),
                        ),
                    ),
                ),
            ),
        )
        val repository = SearchRepository(api)

        val results = repository.search("hello")

        assertEquals("Hello", results.tracks.first().title)
    }

    private fun responseWithTrack(name: String): SearchResponseDto {
        return SearchResponseDto(
            tracks = TrackPagingDto(items = listOf(track(id = "track-$name", name = name))),
            albums = AlbumPagingDto(items = listOf(album(id = "album-$name", name = name))),
            artists = ArtistPagingDto(items = listOf(artist(id = "artist-$name", name = name))),
            playlists = PlaylistPagingDto(items = listOf(playlist(id = "playlist-$name", name = name))),
        )
    }

    private fun track(id: String, name: String): TrackDto {
        return TrackDto(
            id = id,
            name = name,
            durationMs = 180_000,
            uri = "spotify:track:$id",
            album = AlbumDto(
                name = "Album $name",
                images = listOf(ImageDto(url = "https://example.com/$id.jpg")),
            ),
            artists = listOf(ArtistDto(name = "Artist $name")),
        )
    }

    private fun album(id: String, name: String): SimplifiedAlbumDto {
        return SimplifiedAlbumDto(
            id = id,
            name = name,
            albumType = "album",
            artists = listOf(ArtistDto(name = "Artist $name")),
            images = listOf(ImageDto(url = "https://example.com/$id.jpg")),
            totalTracks = 10,
            uri = "spotify:album:$id",
        )
    }

    private fun artist(id: String, name: String): ArtistFullDto {
        return ArtistFullDto(
            id = id,
            name = name,
            images = listOf(ImageDto(url = "https://example.com/$id.jpg")),
            genres = listOf("indie"),
            uri = "spotify:artist:$id",
        )
    }

    private fun playlist(id: String, name: String): SimplifiedPlaylistDto {
        return SimplifiedPlaylistDto(
            id = id,
            name = name,
            images = listOf(ImageDto(url = "https://example.com/$id.jpg")),
            owner = PlaylistOwnerDto(id = "owner-1", displayName = "Owner"),
            uri = "spotify:playlist:$id",
            tracks = PlaylistTracksRefDto(total = 25),
            description = name,
        )
    }
}

private data class SearchCall(
    val query: String,
    val type: String,
    val limit: Int,
    val offset: Int,
    val market: String?,
    val includeExternal: String?,
)

private class FakeSpotifySearchApi(
    private val responses: Map<String, SearchResponseDto>,
) : SpotifySearchApi {
    val calls = mutableListOf<SearchCall>()

    override suspend fun search(
        query: String,
        type: String,
        limit: Int,
        offset: Int,
        market: String?,
        includeExternal: String?,
    ): SearchResponseDto {
        calls += SearchCall(
            query = query,
            type = type,
            limit = limit,
            offset = offset,
            market = market,
            includeExternal = includeExternal,
        )
        return responses[query] ?: SearchResponseDto()
    }
}
