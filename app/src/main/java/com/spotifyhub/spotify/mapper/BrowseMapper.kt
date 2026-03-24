package com.spotifyhub.spotify.mapper

import com.spotifyhub.browse.model.BrowseItem
import com.spotifyhub.browse.model.BrowseItemType
import com.spotifyhub.spotify.dto.browse.ArtistFullDto
import com.spotifyhub.spotify.dto.browse.PlayHistoryDto
import com.spotifyhub.spotify.dto.browse.SimplifiedPlaylistDto
import com.spotifyhub.spotify.dto.player.AlbumDto
import com.spotifyhub.spotify.dto.player.TrackDto

object BrowseMapper {

    fun mapTrackToBrowseItem(dto: TrackDto, contextUri: String? = null): BrowseItem? {
        val id = dto.id ?: return null
        return BrowseItem(
            id = id,
            title = dto.name.orEmpty(),
            subtitle = dto.artists.orEmpty().joinToString(", ") { it.name.orEmpty() },
            artworkUrl = dto.album?.images?.firstOrNull()?.url,
            uri = dto.uri.orEmpty(),
            type = BrowseItemType.Track,
            contextUri = contextUri,
        )
    }

    fun mapPlayHistoryToBrowseItem(dto: PlayHistoryDto): BrowseItem? {
        val track = dto.track ?: return null
        return mapTrackToBrowseItem(track, contextUri = dto.context?.uri)
    }

    /**
     * Extract an album-level [BrowseItem] from a track's embedded album data.
     * Uses the track's artists as a fallback when the album DTO doesn't carry its own.
     */
    fun mapAlbumFromTrack(track: TrackDto): BrowseItem? {
        val album = track.album ?: return null
        val albumId = album.id ?: return null
        val albumUri = album.uri ?: return null
        val artists = album.artists?.takeIf { it.isNotEmpty() } ?: track.artists
        return BrowseItem(
            id = albumId,
            title = album.name.orEmpty(),
            subtitle = artists.orEmpty().joinToString(", ") { it.name.orEmpty() },
            artworkUrl = album.images.orEmpty().firstOrNull()?.url,
            uri = albumUri,
            type = BrowseItemType.Album,
            contextUri = albumUri,
        )
    }

    fun mapArtistToBrowseItem(dto: ArtistFullDto): BrowseItem? {
        val id = dto.id ?: return null
        return BrowseItem(
            id = id,
            title = dto.name.orEmpty(),
            subtitle = dto.genres.orEmpty().take(2).joinToString(", "),
            artworkUrl = dto.images.orEmpty().firstOrNull()?.url,
            uri = dto.uri.orEmpty(),
            type = BrowseItemType.Artist,
        )
    }

    fun mapPlaylistToBrowseItem(dto: SimplifiedPlaylistDto): BrowseItem? {
        val id = dto.id ?: return null
        return BrowseItem(
            id = id,
            title = dto.name.orEmpty(),
            subtitle = dto.owner?.displayName?.let { "by $it" } ?: "",
            artworkUrl = dto.images.orEmpty().firstOrNull()?.url,
            uri = dto.uri.orEmpty(),
            type = BrowseItemType.Playlist,
        )
    }
}
