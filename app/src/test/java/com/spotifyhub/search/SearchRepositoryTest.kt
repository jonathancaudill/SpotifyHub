package com.spotifyhub.search

import com.spotifyhub.search.model.SearchSection
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
        assertEquals("track,album,artist,playlist", call.type)
        assertEquals(10, call.limit)
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
    fun `search adds structured track query for artist title separators`() = runTest {
        val api = FakeSpotifySearchApi(
            responses = mapOf(
                "track:Style artist:Taylor Swift" to responseWithTrack(name = "Style"),
            ),
        )
        val repository = SearchRepository(api)

        val results = repository.search("Taylor Swift - Style")

        assertEquals(
            listOf("Taylor Swift - Style", "track:Style artist:Taylor Swift"),
            api.calls.map { it.query },
        )
        assertEquals("track", api.calls.last().type)
        assertEquals("Style", results.tracks.first().title)
    }

    @Test
    fun `search narrows explicit album filters into album focused request`() = runTest {
        val api = FakeSpotifySearchApi(
            responses = mapOf(
                "album:Punisher artist:Phoebe Bridgers" to SearchResponseDto(
                    albums = AlbumPagingDto(
                        items = listOf(album(id = "punisher", name = "Punisher", artistName = "Phoebe Bridgers")),
                    ),
                ),
            ),
        )
        val repository = SearchRepository(api)

        val results = repository.search("artist:Phoebe Bridgers album:Punisher")

        assertEquals(
            listOf(
                "artist:Phoebe Bridgers album:Punisher",
                "album:Punisher artist:Phoebe Bridgers",
            ),
            api.calls.map { it.query },
        )
        assertEquals("album,track", api.calls.last().type)
        assertEquals("Punisher", results.albums.first().title)
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

    @Test
    fun `search ranks tracks using album context when titles are identical`() = runTest {
        val api = FakeSpotifySearchApi(
            responses = mapOf(
                "style 1989" to SearchResponseDto(
                    tracks = TrackPagingDto(
                        items = listOf(
                            track(id = "2", name = "Style", artistName = "Taylor Swift", albumName = "Taylor Swift Essentials"),
                            track(id = "1", name = "Style", artistName = "Taylor Swift", albumName = "1989"),
                        ),
                    ),
                ),
            ),
        )
        val repository = SearchRepository(api)

        val results = repository.search("style 1989")

        assertEquals("1", results.tracks.first().id)
        assertEquals("Style", results.tracks.first().title)
        assertEquals("Taylor Swift • 1989", results.tracks.first().subtitle)
    }

    @Test
    fun `search prefers artists section for loose artist name queries`() = runTest {
        val api = FakeSpotifySearchApi(
            responses = mapOf(
                "sufjan" to SearchResponseDto(
                    tracks = TrackPagingDto(
                        items = listOf(
                            track(id = "track-1", name = "Chicago", artistName = "Sufjan Stevens"),
                            track(id = "track-2", name = "Fourth of July", artistName = "Sufjan Stevens"),
                        ),
                    ),
                    artists = ArtistPagingDto(
                        items = listOf(
                            artist(id = "artist-1", name = "Sufjan Stevens"),
                            artist(id = "artist-2", name = "Stevens Sufjan"),
                        ),
                    ),
                ),
            ),
        )
        val repository = SearchRepository(api)

        val results = repository.search("sufjan")

        assertEquals(SearchSection.Artists, results.sectionOrder.first())
        assertEquals("Sufjan Stevens", results.artists.first().title)
    }

    @Test
    fun `search keeps songs first when the query directly matches track titles`() = runTest {
        val api = FakeSpotifySearchApi(
            responses = mapOf(
                "hello" to SearchResponseDto(
                    tracks = TrackPagingDto(
                        items = listOf(
                            track(id = "track-1", name = "Hello", artistName = "Adele"),
                            track(id = "track-2", name = "Hello Again", artistName = "The Cars"),
                        ),
                    ),
                    artists = ArtistPagingDto(
                        items = listOf(
                            artist(id = "artist-1", name = "Hello Seahorse!"),
                        ),
                    ),
                ),
            ),
        )
        val repository = SearchRepository(api)

        val results = repository.search("hello")

        assertEquals(SearchSection.Tracks, results.sectionOrder.first())
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

    private fun track(
        id: String,
        name: String,
        artistName: String = "Artist $name",
        albumName: String = "Album $name",
    ): TrackDto {
        return TrackDto(
            id = id,
            name = name,
            durationMs = 180_000,
            uri = "spotify:track:$id",
            album = AlbumDto(
                name = albumName,
                images = listOf(ImageDto(url = "https://example.com/$id.jpg")),
            ),
            artists = listOf(ArtistDto(name = artistName)),
        )
    }

    private fun album(
        id: String,
        name: String,
        artistName: String = "Artist $name",
    ): SimplifiedAlbumDto {
        return SimplifiedAlbumDto(
            id = id,
            name = name,
            albumType = "album",
            artists = listOf(ArtistDto(name = artistName)),
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
