package com.spotifyhub.spotify.mapper

import com.spotifyhub.library.model.AlbumDetail
import com.spotifyhub.library.model.LibraryItem
import com.spotifyhub.library.model.LibraryItemType
import com.spotifyhub.library.model.PlaylistDetail
import com.spotifyhub.library.model.TrackItem
import com.spotifyhub.spotify.dto.browse.SimplifiedPlaylistDto
import com.spotifyhub.spotify.dto.library.FullPlaylistDto
import com.spotifyhub.spotify.dto.library.SavedAlbumDto
import com.spotifyhub.spotify.dto.library.SavedTrackDto
import com.spotifyhub.spotify.dto.player.TrackDto

object LibraryMapper {

    fun mapPlaylistToLibraryItem(dto: SimplifiedPlaylistDto): LibraryItem? {
        val id = dto.id ?: return null
        return LibraryItem(
            id = id,
            name = dto.name.orEmpty(),
            subtitle = dto.owner?.displayName?.let { "by $it" } ?: "",
            artworkUrl = dto.images.orEmpty().firstOrNull()?.url,
            uri = dto.uri.orEmpty(),
            type = LibraryItemType.Playlist,
            trackCount = dto.tracks?.total,
        )
    }

    fun mapSavedAlbumToLibraryItem(dto: SavedAlbumDto): LibraryItem? {
        val album = dto.album ?: return null
        val id = album.id ?: return null
        return LibraryItem(
            id = id,
            name = album.name.orEmpty(),
            subtitle = album.artists.orEmpty().joinToString(", ") { it.name.orEmpty() },
            artworkUrl = album.images.orEmpty().firstOrNull()?.url,
            uri = album.uri.orEmpty(),
            type = LibraryItemType.Album,
            trackCount = album.totalTracks,
        )
    }

    fun mapSavedTrackToLibraryItem(dto: SavedTrackDto): LibraryItem? {
        val track = dto.track ?: return null
        val id = track.id ?: return null
        return LibraryItem(
            id = id,
            name = track.name.orEmpty(),
            subtitle = track.artists.orEmpty().joinToString(", ") { it.name.orEmpty() },
            artworkUrl = track.album?.images?.firstOrNull()?.url,
            uri = track.uri.orEmpty(),
            type = LibraryItemType.Track,
        )
    }

    fun mapTrackDtoToTrackItem(dto: TrackDto, index: Int = 0): TrackItem? {
        val id = dto.id ?: return null
        return TrackItem(
            id = id,
            title = dto.name.orEmpty(),
            artist = dto.artists.orEmpty().joinToString(", ") { it.name.orEmpty() },
            album = dto.album?.name.orEmpty(),
            artworkUrl = dto.album?.images?.firstOrNull()?.url,
            durationMs = dto.durationMs ?: 0L,
            uri = dto.uri.orEmpty(),
            trackNumber = index + 1,
        )
    }

    fun mapFullPlaylistToDetail(
        dto: FullPlaylistDto,
        tracks: List<TrackDto>,
        totalTracks: Int,
    ): PlaylistDetail? {
        val id = dto.id ?: return null
        return PlaylistDetail(
            id = id,
            name = dto.name.orEmpty(),
            description = dto.description,
            artworkUrl = dto.images.orEmpty().firstOrNull()?.url,
            ownerName = dto.owner?.displayName,
            uri = dto.uri.orEmpty(),
            tracks = tracks.mapIndexedNotNull { index, item -> mapTrackDtoToTrackItem(item, index) },
            totalTracks = totalTracks,
        )
    }

    fun mapAlbumTracksToDetail(
        albumId: String,
        albumName: String,
        artistName: String?,
        artworkUrl: String?,
        albumUri: String,
        tracks: List<TrackDto>,
        totalTracks: Int,
    ): AlbumDetail {
        return AlbumDetail(
            id = albumId,
            name = albumName,
            artistName = artistName,
            artworkUrl = artworkUrl,
            uri = albumUri,
            tracks = tracks.mapIndexedNotNull { index, dto -> mapTrackDtoToTrackItem(dto, index) },
            totalTracks = totalTracks,
        )
    }
}
