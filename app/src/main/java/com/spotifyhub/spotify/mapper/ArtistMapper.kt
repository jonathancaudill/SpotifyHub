package com.spotifyhub.spotify.mapper

import com.spotifyhub.artist.model.ArtistBio
import com.spotifyhub.artist.model.ArtistDetail
import com.spotifyhub.artist.model.ArtistPlaylistSummary
import com.spotifyhub.artist.model.ArtistReleaseSummary
import com.spotifyhub.spotify.dto.artist.ArtistDetailDto
import com.spotifyhub.spotify.dto.browse.SimplifiedPlaylistDto
import com.spotifyhub.spotify.dto.search.SimplifiedAlbumDto
import com.spotifyhub.spotify.dto.wikipedia.WikipediaSummaryDto

object ArtistMapper {
    fun mapArtistDetail(
        dto: ArtistDetailDto,
        albums: List<ArtistReleaseSummary>,
        singlesAndEps: List<ArtistReleaseSummary>,
        featuredOn: List<ArtistReleaseSummary>,
        curatedPlaylists: List<ArtistPlaylistSummary>,
    ): ArtistDetail {
        return ArtistDetail(
            id = dto.id.orEmpty(),
            name = dto.name.orEmpty(),
            uri = dto.uri.orEmpty(),
            artworkUrl = dto.images.orEmpty().firstOrNull()?.url,
            genres = dto.genres.orEmpty(),
            followersTotal = dto.followers?.total,
            popularity = dto.popularity,
            albums = albums,
            singlesAndEps = singlesAndEps,
            featuredOn = featuredOn,
            curatedPlaylists = curatedPlaylists,
        )
    }

    fun mapRelease(dto: SimplifiedAlbumDto): ArtistReleaseSummary? {
        val id = dto.id ?: return null
        return ArtistReleaseSummary(
            id = id,
            title = dto.name.orEmpty(),
            subtitle = dto.artists.orEmpty().joinToString(", ") { it.name.orEmpty() },
            artworkUrl = dto.images.orEmpty().firstOrNull()?.url,
            uri = dto.uri.orEmpty(),
            totalTracks = dto.totalTracks,
        )
    }

    fun mapPlaylist(dto: SimplifiedPlaylistDto): ArtistPlaylistSummary? {
        val id = dto.id ?: return null
        return ArtistPlaylistSummary(
            id = id,
            title = dto.name.orEmpty(),
            subtitle = dto.owner?.displayName?.let { "by $it" } ?: "",
            artworkUrl = dto.images.orEmpty().firstOrNull()?.url,
            uri = dto.uri.orEmpty(),
        )
    }

    fun mapBio(dto: WikipediaSummaryDto): ArtistBio? {
        val summary = dto.extract.orEmpty().trim()
        if (summary.isBlank()) {
            return null
        }
        return ArtistBio(
            title = dto.title.orEmpty(),
            summary = summary,
            sourceUrl = dto.contentUrls?.desktop?.page,
        )
    }
}
