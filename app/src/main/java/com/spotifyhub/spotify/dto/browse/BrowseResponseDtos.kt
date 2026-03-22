package com.spotifyhub.spotify.dto.browse

import com.spotifyhub.spotify.dto.player.ArtistDto
import com.spotifyhub.spotify.dto.player.ImageDto
import com.spotifyhub.spotify.dto.player.TrackDto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/* ── Recently Played ─────────────────────────────────────────────── */

@JsonClass(generateAdapter = true)
data class RecentlyPlayedResponseDto(
    val items: List<PlayHistoryDto> ? = null,
    val cursors: CursorsDto? = null,
    val next: String? = null,
)

@JsonClass(generateAdapter = true)
data class PlayHistoryDto(
    val track: TrackDto? = null,
    @param:Json(name = "played_at") val playedAt: String? = null,
    val context: ContextDto? = null,
)

@JsonClass(generateAdapter = true)
data class ContextDto(
    val uri: String? = null,
    val type: String? = null,
)

@JsonClass(generateAdapter = true)
data class CursorsDto(
    val after: String? = null,
    val before: String? = null,
)

/* ── Top Tracks ──────────────────────────────────────────────────── */

@JsonClass(generateAdapter = true)
data class TopTracksResponseDto(
    val items: List<TrackDto> ? = null,
    val total: Int? = null,
    val limit: Int? = null,
    val offset: Int? = null,
    val next: String? = null,
)

/* ── Top Artists ─────────────────────────────────────────────────── */

@JsonClass(generateAdapter = true)
data class TopArtistsResponseDto(
    val items: List<ArtistFullDto> ? = null,
    val total: Int? = null,
    val limit: Int? = null,
    val offset: Int? = null,
    val next: String? = null,
)

@JsonClass(generateAdapter = true)
data class ArtistFullDto(
    val id: String? = null,
    val name: String? = null,
    val images: List<ImageDto> ? = null,
    val genres: List<String> ? = null,
    val uri: String? = null,
)

/* ── Recommendations ─────────────────────────────────────────────── */

@JsonClass(generateAdapter = true)
data class RecommendationsResponseDto(
    val tracks: List<TrackDto> ? = null,
    val seeds: List<SeedDto> ? = null,
)

@JsonClass(generateAdapter = true)
data class SeedDto(
    val id: String? = null,
    val type: String? = null,
    @param:Json(name = "initialPoolSize") val initialPoolSize: Int? = null,
    @param:Json(name = "afterFilteringSize") val afterFilteringSize: Int? = null,
)

/* ── Featured Playlists ──────────────────────────────────────────── */

@JsonClass(generateAdapter = true)
data class FeaturedPlaylistsResponseDto(
    val message: String? = null,
    val playlists: PlaylistPagingDto? = null,
)

@JsonClass(generateAdapter = true)
data class PlaylistPagingDto(
    val items: List<SimplifiedPlaylistDto> ? = null,
    val total: Int? = null,
    val limit: Int? = null,
    val offset: Int? = null,
    val next: String? = null,
)

/* ── User Profile ────────────────────────────────────────────────── */

@JsonClass(generateAdapter = true)
data class UserProfileDto(
    val id: String? = null,
    @param:Json(name = "display_name") val displayName: String? = null,
    val images: List<ImageDto> ? = null,
)

/* ── Simplified Playlist (shared) ────────────────────────────────── */

@JsonClass(generateAdapter = true)
data class SimplifiedPlaylistDto(
    val id: String? = null,
    val name: String? = null,
    val images: List<ImageDto> ? = null,
    val owner: PlaylistOwnerDto? = null,
    val uri: String? = null,
    val tracks: PlaylistTracksRefDto? = null,
    val description: String? = null,
)

@JsonClass(generateAdapter = true)
data class PlaylistOwnerDto(
    val id: String? = null,
    @param:Json(name = "display_name") val displayName: String? = null,
)

@JsonClass(generateAdapter = true)
data class PlaylistTracksRefDto(
    val total: Int? = null,
)
