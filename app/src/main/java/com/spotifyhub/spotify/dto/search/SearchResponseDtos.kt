package com.spotifyhub.spotify.dto.search

import com.spotifyhub.spotify.dto.browse.ArtistFullDto
import com.spotifyhub.spotify.dto.browse.SimplifiedPlaylistDto
import com.spotifyhub.spotify.dto.player.ArtistDto
import com.spotifyhub.spotify.dto.player.ImageDto
import com.spotifyhub.spotify.dto.player.TrackDto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SearchResponseDto(
    val tracks: TrackPagingDto? = null,
    val albums: AlbumPagingDto? = null,
    val artists: ArtistPagingDto? = null,
    val playlists: PlaylistPagingDto? = null,
)

/* ── Paging wrappers (Moshi doesn't support generics well) ─────── */

@JsonClass(generateAdapter = true)
data class TrackPagingDto(
    val items: List<TrackDto> ? = null,
    val total: Int? = null,
    val limit: Int? = null,
    val offset: Int? = null,
    val next: String? = null,
)

@JsonClass(generateAdapter = true)
data class AlbumPagingDto(
    val items: List<SimplifiedAlbumDto> ? = null,
    val total: Int? = null,
    val limit: Int? = null,
    val offset: Int? = null,
    val next: String? = null,
)

@JsonClass(generateAdapter = true)
data class ArtistPagingDto(
    val items: List<ArtistFullDto> ? = null,
    val total: Int? = null,
    val limit: Int? = null,
    val offset: Int? = null,
    val next: String? = null,
)

@JsonClass(generateAdapter = true)
data class PlaylistPagingDto(
    val items: List<SimplifiedPlaylistDto> ? = null,
    val total: Int? = null,
    val limit: Int? = null,
    val offset: Int? = null,
    val next: String? = null,
)

/* ── Simplified Album ────────────────────────────────────────────── */

@JsonClass(generateAdapter = true)
data class SimplifiedAlbumDto(
    val id: String? = null,
    val name: String? = null,
    @param:Json(name = "album_type") val albumType: String? = null,
    val artists: List<ArtistDto> ? = null,
    val images: List<ImageDto> ? = null,
    @param:Json(name = "total_tracks") val totalTracks: Int? = null,
    val uri: String? = null,
)
