package com.spotifyhub.ui.detail

import com.spotifyhub.artist.ArtistRepository
import com.spotifyhub.browse.model.BrowseItem
import com.spotifyhub.browse.model.BrowseItemType
import com.spotifyhub.library.LibraryRepository
import com.spotifyhub.spotify.api.PlayContextBody
import com.spotifyhub.spotify.api.SpotifyArtistApi
import com.spotifyhub.spotify.api.SpotifyLibraryApi
import com.spotifyhub.spotify.api.SpotifyPlayerApi
import com.spotifyhub.spotify.api.SpotifySearchApi
import com.spotifyhub.spotify.api.WikipediaApi
import com.spotifyhub.spotify.dto.artist.ArtistAlbumPagingDto
import com.spotifyhub.spotify.dto.artist.ArtistDetailDto
import com.spotifyhub.spotify.dto.artist.FollowersDto
import com.spotifyhub.spotify.dto.library.FullPlaylistDto
import com.spotifyhub.spotify.dto.library.PlaylistTrackPagingDto
import com.spotifyhub.spotify.dto.library.SavedAlbumPagingDto
import com.spotifyhub.spotify.dto.library.SavedTrackPagingDto
import com.spotifyhub.spotify.dto.library.TrackPagingDto
import com.spotifyhub.spotify.dto.player.ArtistDto
import com.spotifyhub.spotify.dto.player.ImageDto
import com.spotifyhub.spotify.dto.player.PlaybackResponseDto
import com.spotifyhub.spotify.dto.player.QueueResponseDto
import com.spotifyhub.spotify.dto.search.SearchResponseDto
import com.spotifyhub.spotify.dto.search.SimplifiedAlbumDto
import com.spotifyhub.spotify.dto.wikipedia.WikipediaContentUrlsDto
import com.spotifyhub.spotify.dto.wikipedia.WikipediaPageUrlDto
import com.spotifyhub.spotify.dto.wikipedia.WikipediaQueryDto
import com.spotifyhub.spotify.dto.wikipedia.WikipediaSearchResponseDto
import com.spotifyhub.spotify.dto.wikipedia.WikipediaSearchResultDto
import com.spotifyhub.spotify.dto.wikipedia.WikipediaSummaryDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DetailViewModelTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `load artist publishes spotify detail before wikipedia bio`() = runTest(StandardTestDispatcher()) {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val viewModel = DetailViewModel(
            libraryRepository = LibraryRepository(
                libraryApi = FakeLibraryApi(),
                playerApi = FakePlayerApi(),
            ),
            artistRepository = ArtistRepository(
                artistApi = FakeArtistApi(),
                searchApi = FakeSearchApi(),
                wikipediaApi = DelayedWikipediaApi(),
            ),
        )

        viewModel.loadArtist(
            artistId = "artist-1",
            fallbackItem = BrowseItem(
                id = "artist-1",
                title = "Sufjan Stevens",
                subtitle = "",
                artworkUrl = null,
                uri = "spotify:artist:artist-1",
                type = BrowseItemType.Artist,
            ),
        )

        runCurrent()
        runCurrent()

        val firstContent = viewModel.uiState.value.content as DetailContent.Artist
        assertEquals("Sufjan Stevens", firstContent.detail.name)
        assertNull(firstContent.detail.bio)

        advanceTimeBy(500)
        runCurrent()

        val secondContent = viewModel.uiState.value.content as DetailContent.Artist
        requireNotNull(secondContent.detail.bio)
        assertTrue(secondContent.detail.bio.summary.contains("singer-songwriter"))

        Dispatchers.resetMain()
    }

    private class FakeArtistApi : SpotifyArtistApi {
        override suspend fun getArtist(artistId: String): ArtistDetailDto {
            return ArtistDetailDto(
                id = artistId,
                name = "Sufjan Stevens",
                uri = "spotify:artist:$artistId",
                genres = listOf("indie folk"),
                followers = FollowersDto(total = 1000),
                popularity = 75,
                images = listOf(ImageDto(url = "https://images.test/artist.jpg")),
            )
        }

        override suspend fun getArtistAlbums(
            artistId: String,
            includeGroups: String,
            limit: Int,
            offset: Int,
            market: String?,
        ): ArtistAlbumPagingDto {
            val item = SimplifiedAlbumDto(
                id = "$includeGroups-1",
                name = includeGroups.replace('_', ' '),
                albumType = "album",
                artists = listOf(ArtistDto(name = "Sufjan Stevens")),
                images = listOf(ImageDto(url = "https://images.test/$includeGroups.jpg")),
                totalTracks = 8,
                uri = "spotify:album:$includeGroups-1",
            )
            return ArtistAlbumPagingDto(
                items = listOf(item),
                total = 1,
                next = null,
            )
        }
    }

    private class FakeSearchApi : SpotifySearchApi {
        override suspend fun search(
            query: String,
            type: String,
            limit: Int,
            offset: Int,
            market: String?,
            includeExternal: String?,
        ): SearchResponseDto {
            return SearchResponseDto()
        }
    }

    private class DelayedWikipediaApi : WikipediaApi {
        override suspend fun searchPages(query: String, limit: Int): WikipediaSearchResponseDto {
            delay(500)
            return WikipediaSearchResponseDto(
                query = WikipediaQueryDto(
                    search = listOf(WikipediaSearchResultDto(title = "Sufjan Stevens")),
                ),
            )
        }

        override suspend fun getSummary(title: String): WikipediaSummaryDto {
            return WikipediaSummaryDto(
                title = "Sufjan Stevens",
                extract = "Sufjan Stevens is an American singer-songwriter.",
                type = "standard",
                contentUrls = WikipediaContentUrlsDto(
                    desktop = WikipediaPageUrlDto(page = "https://en.wikipedia.org/wiki/Sufjan_Stevens"),
                ),
            )
        }
    }

    private class FakeLibraryApi : SpotifyLibraryApi {
        override suspend fun containsSavedItems(uris: String): List<Boolean> = emptyList()
        override suspend fun saveItems(uris: String) = Unit
        override suspend fun removeItems(uris: String) = Unit
        override suspend fun getUserPlaylists(limit: Int, offset: Int) = throw UnsupportedOperationException()
        override suspend fun getSavedAlbums(limit: Int, offset: Int) = SavedAlbumPagingDto()
        override suspend fun getSavedTracks(limit: Int, offset: Int) = SavedTrackPagingDto()
        override suspend fun getPlaylist(playlistId: String) = FullPlaylistDto()
        override suspend fun getPlaylistTracks(playlistId: String, limit: Int, offset: Int) = PlaylistTrackPagingDto()
        override suspend fun getAlbumTracks(albumId: String, limit: Int, offset: Int) = TrackPagingDto()
    }

    private class FakePlayerApi : SpotifyPlayerApi {
        override suspend fun getCurrentPlayback(additionalTypes: String): PlaybackResponseDto? = null
        override suspend fun play() = Unit
        override suspend fun pause() = Unit
        override suspend fun skipNext() = Unit
        override suspend fun skipPrevious() = Unit
        override suspend fun setShuffle(enabled: Boolean) = Unit
        override suspend fun setRepeatMode(repeatMode: String) = Unit
        override suspend fun setVolume(volumePercent: Int) = Unit
        override suspend fun seekTo(positionMs: Long) = Unit
        override suspend fun playContext(body: PlayContextBody) = Unit
        override suspend fun getQueue(): QueueResponseDto? = null
    }
}
