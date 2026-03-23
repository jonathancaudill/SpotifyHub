package com.spotifyhub.spotify.mapper

import com.spotifyhub.browse.model.BrowseItem
import com.spotifyhub.browse.model.BrowseItemType
import com.spotifyhub.search.model.SearchResults
import com.spotifyhub.spotify.dto.search.SearchResponseDto
import com.spotifyhub.spotify.dto.search.SimplifiedAlbumDto

object SearchMapper {

    fun map(dto: SearchResponseDto): SearchResults {
        return SearchResults(
            tracks = dto.tracks?.items.orEmpty().filterNotNull().mapNotNull { mapTrackToSearchBrowseItem(it) },
            albums = dto.albums?.items.orEmpty().filterNotNull().mapNotNull { mapAlbumToBrowseItem(it) },
            artists = dto.artists?.items.orEmpty().filterNotNull().mapNotNull { BrowseMapper.mapArtistToBrowseItem(it) },
            playlists = dto.playlists?.items.orEmpty().filterNotNull().mapNotNull { BrowseMapper.mapPlaylistToBrowseItem(it) },
        )
    }

    private fun mapTrackToSearchBrowseItem(dto: com.spotifyhub.spotify.dto.player.TrackDto): BrowseItem? {
        val baseItem = BrowseMapper.mapTrackToBrowseItem(dto) ?: return null
        val albumName = dto.album?.name.orEmpty().trim()
        val enhancedSubtitle = listOf(baseItem.subtitle, albumName)
            .filter { it.isNotBlank() }
            .joinToString(" • ")

        return baseItem.copy(subtitle = enhancedSubtitle)
    }

    private fun mapAlbumToBrowseItem(dto: SimplifiedAlbumDto): BrowseItem? {
        val id = dto.id ?: return null
        return BrowseItem(
            id = id,
            title = dto.name.orEmpty(),
            subtitle = dto.artists.orEmpty().joinToString(", ") { it.name.orEmpty() },
            artworkUrl = dto.images.orEmpty().firstOrNull()?.url,
            uri = dto.uri.orEmpty(),
            type = BrowseItemType.Album,
        )
    }
}
