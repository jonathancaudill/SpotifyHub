package com.spotifyhub.library

import com.spotifyhub.library.model.AlbumDetail
import com.spotifyhub.library.model.LibraryItem
import com.spotifyhub.library.model.PlaylistDetail
import com.spotifyhub.spotify.api.PlayContextBody
import com.spotifyhub.spotify.api.PlayOffset
import com.spotifyhub.spotify.api.SpotifyLibraryApi
import com.spotifyhub.spotify.api.SpotifyPlayerApi
import com.spotifyhub.spotify.mapper.LibraryMapper
import com.spotifyhub.spotify.dto.player.TrackDto

class LibraryRepository(
    private val libraryApi: SpotifyLibraryApi,
    private val playerApi: SpotifyPlayerApi,
) {
    private companion object {
        const val PLAYLIST_TRACK_PAGE_SIZE = 100
        const val ALBUM_TRACK_PAGE_SIZE = 50
    }

    suspend fun getPlaylists(offset: Int = 0): Pair<List<LibraryItem>, Int> {
        val response = libraryApi.getUserPlaylists(limit = 50, offset = offset)
        val items = response.items.orEmpty().mapNotNull { LibraryMapper.mapPlaylistToLibraryItem(it) }
        return items to (response.total ?: items.size)
    }

    suspend fun getSavedAlbums(offset: Int = 0): Pair<List<LibraryItem>, Int> {
        val response = libraryApi.getSavedAlbums(limit = 50, offset = offset)
        val items = response.items.orEmpty().mapNotNull { LibraryMapper.mapSavedAlbumToLibraryItem(it) }
        return items to (response.total ?: items.size)
    }

    suspend fun getSavedTracks(offset: Int = 0): Pair<List<LibraryItem>, Int> {
        val response = libraryApi.getSavedTracks(limit = 50, offset = offset)
        val items = response.items.orEmpty().mapNotNull { LibraryMapper.mapSavedTrackToLibraryItem(it) }
        return items to (response.total ?: items.size)
    }

    suspend fun getPlaylistDetail(playlistId: String): PlaylistDetail? {
        val response = libraryApi.getPlaylist(playlistId)
        val firstPageTracks = response.tracks?.items.orEmpty().mapNotNull { it.track }
        val totalTracks = response.tracks?.total ?: firstPageTracks.size
        val allTracks = fetchRemainingPlaylistTracks(
            playlistId = playlistId,
            initialTracks = firstPageTracks,
            totalTracks = totalTracks,
        )
        return LibraryMapper.mapFullPlaylistToDetail(
            dto = response,
            tracks = allTracks,
            totalTracks = totalTracks,
        )
    }

    suspend fun getAlbumDetail(
        albumId: String,
        albumName: String,
        artistName: String?,
        artworkUrl: String?,
        albumUri: String,
    ): AlbumDetail {
        val response = libraryApi.getAlbumTracks(albumId, limit = ALBUM_TRACK_PAGE_SIZE)
        val firstPageTracks = response.items.orEmpty()
        val totalTracks = response.total ?: firstPageTracks.size
        val allTracks = fetchRemainingAlbumTracks(
            albumId = albumId,
            initialTracks = firstPageTracks,
            totalTracks = totalTracks,
        )
        return LibraryMapper.mapAlbumTracksToDetail(
            albumId = albumId,
            albumName = albumName,
            artistName = artistName,
            artworkUrl = artworkUrl,
            albumUri = albumUri,
            tracks = allTracks,
            totalTracks = totalTracks,
        )
    }

    suspend fun playContext(contextUri: String, trackOffset: Int? = null) {
        playerApi.playContext(
            PlayContextBody(
                contextUri = contextUri,
                offset = trackOffset?.let { PlayOffset(position = it) },
            ),
        )
    }

    suspend fun playTrack(trackUri: String) {
        playerApi.playContext(
            PlayContextBody(
                uris = listOf(trackUri),
            ),
        )
    }

    private suspend fun fetchRemainingPlaylistTracks(
        playlistId: String,
        initialTracks: List<TrackDto>,
        totalTracks: Int,
    ): List<TrackDto> {
        if (initialTracks.size >= totalTracks) {
            return initialTracks
        }

        val tracks = initialTracks.toMutableList()
        var offset = tracks.size

        while (tracks.size < totalTracks) {
            val page = libraryApi.getPlaylistTracks(
                playlistId = playlistId,
                limit = PLAYLIST_TRACK_PAGE_SIZE,
                offset = offset,
            )
            val pageTracks = page.items.orEmpty().mapNotNull { it.track }
            if (pageTracks.isEmpty()) {
                break
            }
            tracks += pageTracks
            offset += pageTracks.size
        }

        return tracks
    }

    private suspend fun fetchRemainingAlbumTracks(
        albumId: String,
        initialTracks: List<TrackDto>,
        totalTracks: Int,
    ): List<TrackDto> {
        if (initialTracks.size >= totalTracks) {
            return initialTracks
        }

        val tracks = initialTracks.toMutableList()
        var offset = tracks.size

        while (tracks.size < totalTracks) {
            val page = libraryApi.getAlbumTracks(
                albumId = albumId,
                limit = ALBUM_TRACK_PAGE_SIZE,
                offset = offset,
            )
            val pageTracks = page.items.orEmpty()
            if (pageTracks.isEmpty()) {
                break
            }
            tracks += pageTracks
            offset += pageTracks.size
        }

        return tracks
    }
}
