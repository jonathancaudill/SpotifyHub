package com.spotifyhub.spotify.dto.library

import com.spotifyhub.spotify.dto.player.ArtistDto
import com.spotifyhub.spotify.dto.player.ImageDto
import com.spotifyhub.spotify.dto.player.TrackDto
import com.spotifyhub.spotify.dto.search.SimplifiedAlbumDto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/* ── Saved Album ─────────────────────────────────────────────────── */

@JsonClass(generateAdapter = true)
data class SavedAlbumDto(
    @param:Json(name = "added_at") val addedAt: String? = null,
    val album: FullAlbumDto? = null,
)

@JsonClass(generateAdapter = true)
data class FullAlbumDto(
    val id: String? = null,
    val name: String? = null,
    @param:Json(name = "album_type") val albumType: String? = null,
    val artists: List<ArtistDto> ? = null,
    val images: List<ImageDto> ? = null,
    @param:Json(name = "total_tracks") val totalTracks: Int? = null,
    val uri: String? = null,
)

/* ── Saved Track ─────────────────────────────────────────────────── */

@JsonClass(generateAdapter = true)
data class SavedTrackDto(
    @param:Json(name = "added_at") val addedAt: String? = null,
    val track: TrackDto? = null,
)

/* ── Playlist Tracks ─────────────────────────────────────────────── */

@JsonClass(generateAdapter = true)
data class PlaylistTrackDto(
    @param:Json(name = "added_at") val addedAt: String? = null,
    val track: TrackDto? = null,
)

/* ── Paging wrappers ─────────────────────────────────────────────── */

@JsonClass(generateAdapter = true)
data class SavedAlbumPagingDto(
    val items: List<SavedAlbumDto> ? = null,
    val total: Int? = null,
    val limit: Int? = null,
    val offset: Int? = null,
    val next: String? = null,
)

@JsonClass(generateAdapter = true)
data class SavedTrackPagingDto(
    val items: List<SavedTrackDto> ? = null,
    val total: Int? = null,
    val limit: Int? = null,
    val offset: Int? = null,
    val next: String? = null,
)

@JsonClass(generateAdapter = true)
data class PlaylistTrackPagingDto(
    val items: List<PlaylistTrackDto> ? = null,
    val total: Int? = null,
    val limit: Int? = null,
    val offset: Int? = null,
    val next: String? = null,
)

@JsonClass(generateAdapter = true)
data class TrackPagingDto(
    val items: List<TrackDto> ? = null,
    val total: Int? = null,
    val limit: Int? = null,
    val offset: Int? = null,
    val next: String? = null,
)

/* ── Full Playlist (detail view) ─────────────────────────────────── */

@JsonClass(generateAdapter = true)
data class FullPlaylistDto(
    val id: String? = null,
    val name: String? = null,
    val description: String? = null,
    val images: List<ImageDto> ? = null,
    val owner: PlaylistOwnerDto? = null,
    val uri: String? = null,
    val tracks: PlaylistTrackPagingDto? = null,
)

@JsonClass(generateAdapter = true)
data class PlaylistOwnerDto(
    val id: String? = null,
    @param:Json(name = "display_name") val displayName: String? = null,
)

/* ── User Playlist Paging ────────────────────────────────────────── */

@JsonClass(generateAdapter = true)
data class UserPlaylistPagingDto(
    val items: List<com.spotifyhub.spotify.dto.browse.SimplifiedPlaylistDto> ? = null,
    val total: Int? = null,
    val limit: Int? = null,
    val offset: Int? = null,
    val next: String? = null,
)
