package com.spotifyhub.playback

import com.spotifyhub.auth.SessionState
import com.spotifyhub.spotify.api.PlayContextBody
import com.spotifyhub.spotify.api.SpotifyLibraryApi
import com.spotifyhub.spotify.api.SpotifyPlayerApi
import com.spotifyhub.spotify.dto.browse.PlaylistPagingDto
import com.spotifyhub.spotify.dto.library.FullPlaylistDto
import com.spotifyhub.spotify.dto.library.PlaylistTrackPagingDto
import com.spotifyhub.spotify.dto.library.SavedAlbumPagingDto
import com.spotifyhub.spotify.dto.library.SavedTrackPagingDto
import com.spotifyhub.spotify.dto.library.TrackPagingDto
import com.spotifyhub.spotify.dto.player.AlbumDto
import com.spotifyhub.spotify.dto.player.ArtistDto
import com.spotifyhub.spotify.dto.player.DeviceDto
import com.spotifyhub.spotify.dto.player.ImageDto
import com.spotifyhub.spotify.dto.player.PlaybackItemDto
import com.spotifyhub.spotify.dto.player.PlaybackResponseDto
import com.spotifyhub.spotify.dto.player.QueueResponseDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackRepositoryTest {
    @Test
    fun `previous skips to prior track before ten seconds`() {
        assertFalse(PlaybackRepository.shouldRestartCurrentTrackOnPrevious(positionMs = 9_999L))
    }

    @Test
    fun `previous restarts current track at ten seconds and later`() {
        assertTrue(PlaybackRepository.shouldRestartCurrentTrackOnPrevious(positionMs = 10_000L))
        assertTrue(PlaybackRepository.shouldRestartCurrentTrackOnPrevious(positionMs = 42_000L))
    }

    @Test
    fun `previous before ten seconds swaps ui to cached previous track immediately`() = runTest {
        val playerApi = FakePlayerApi(
            playbackResponses = ArrayDeque(
                listOf(
                    playbackResponse(trackId = "track-a", progressMs = 24_000L),
                    playbackResponse(trackId = "track-b", progressMs = 5_000L),
                ),
            ),
        )
        val appScope = CoroutineScope(StandardTestDispatcher(testScheduler) + Job())
        val repository = createRepository(
            appScope = appScope,
            playerApi = playerApi,
        )
        try {
            advanceUntilIdle()

            repository.refreshNow()
            advanceUntilIdle()
            repository.refreshNow()
            advanceUntilIdle()

            repository.skipPrevious()

            assertEquals("track-a", repository.playbackState.value?.item?.id)
            assertEquals(0L, repository.playbackState.value?.progressMs)

            advanceUntilIdle()

            assertEquals(1, playerApi.skipPreviousCalls)
            assertEquals(emptyList<Long>(), playerApi.seekRequests)
        } finally {
            appScope.cancel()
        }
    }

    @Test
    fun `previous at ten seconds restarts current track instead of swapping to cached previous track`() = runTest {
        val playerApi = FakePlayerApi(
            playbackResponses = ArrayDeque(
                listOf(
                    playbackResponse(trackId = "track-a", progressMs = 24_000L),
                    playbackResponse(trackId = "track-b", progressMs = 10_000L),
                ),
            ),
        )
        val appScope = CoroutineScope(StandardTestDispatcher(testScheduler) + Job())
        val repository = createRepository(
            appScope = appScope,
            playerApi = playerApi,
        )
        try {
            advanceUntilIdle()

            repository.refreshNow()
            advanceUntilIdle()
            repository.refreshNow()
            advanceUntilIdle()

            repository.skipPrevious()

            assertEquals("track-b", repository.playbackState.value?.item?.id)
            assertEquals(0L, repository.playbackState.value?.progressMs)

            advanceUntilIdle()

            assertEquals(0, playerApi.skipPreviousCalls)
            assertEquals(listOf(0L), playerApi.seekRequests)
        } finally {
            appScope.cancel()
        }
    }

    private fun createRepository(
        appScope: CoroutineScope,
        playerApi: FakePlayerApi,
    ): PlaybackRepository {
        return PlaybackRepository(
            appScope = appScope,
            sessionState = MutableStateFlow(SessionState.SignedOut),
            playerApi = playerApi,
            libraryApi = FakeLibraryApi(),
        )
    }

    private fun playbackResponse(
        trackId: String,
        progressMs: Long,
    ): PlaybackResponseDto {
        return PlaybackResponseDto(
            isPlaying = false,
            shuffleState = false,
            repeatState = "off",
            progressMs = progressMs,
            currentlyPlayingType = "track",
            item = PlaybackItemDto(
                id = trackId,
                name = "Song $trackId",
                durationMs = 180_000L,
                uri = "spotify:track:$trackId",
                type = "track",
                album = AlbumDto(
                    id = "album-$trackId",
                    name = "Album $trackId",
                    images = listOf(ImageDto(url = "https://example.com/$trackId.jpg")),
                    uri = "spotify:album:$trackId",
                    artists = listOf(ArtistDto(name = "Artist $trackId")),
                    releaseDate = "2024-01-01",
                ),
                artists = listOf(ArtistDto(name = "Artist $trackId")),
            ),
            device = DeviceDto(
                id = "device-1",
                name = "Living Room",
                type = "Speaker",
                volumePercent = 50,
            ),
        )
    }

    private class FakePlayerApi(
        private val playbackResponses: ArrayDeque<PlaybackResponseDto>,
    ) : SpotifyPlayerApi {
        var skipPreviousCalls: Int = 0
            private set

        val seekRequests = mutableListOf<Long>()

        override suspend fun getCurrentPlayback(additionalTypes: String): PlaybackResponseDto? {
            return playbackResponses.removeFirstOrNull()
        }

        override suspend fun play() = Unit

        override suspend fun pause() = Unit

        override suspend fun skipNext() = Unit

        override suspend fun skipPrevious() {
            skipPreviousCalls += 1
        }

        override suspend fun setShuffle(enabled: Boolean) = Unit

        override suspend fun setRepeatMode(repeatMode: String) = Unit

        override suspend fun setVolume(volumePercent: Int) = Unit

        override suspend fun seekTo(positionMs: Long) {
            seekRequests += positionMs
        }

        override suspend fun playContext(body: PlayContextBody) = Unit

        override suspend fun getQueue(): QueueResponseDto? {
            return QueueResponseDto(
                currentlyPlaying = null,
                queue = emptyList(),
            )
        }
    }

    private class FakeLibraryApi : SpotifyLibraryApi {
        override suspend fun containsSavedItems(uris: String): List<Boolean> = emptyList()

        override suspend fun saveItems(uris: String) = Unit

        override suspend fun removeItems(uris: String) = Unit

        override suspend fun getUserPlaylists(limit: Int, offset: Int) = PlaylistPagingDto()

        override suspend fun getSavedAlbums(limit: Int, offset: Int) = SavedAlbumPagingDto()

        override suspend fun getSavedTracks(limit: Int, offset: Int) = SavedTrackPagingDto()

        override suspend fun getPlaylist(playlistId: String) = FullPlaylistDto()

        override suspend fun getPlaylistTracks(playlistId: String, limit: Int, offset: Int) = PlaylistTrackPagingDto()

        override suspend fun getAlbumTracks(albumId: String, limit: Int, offset: Int) = TrackPagingDto()
    }
}
