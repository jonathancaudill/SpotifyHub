package com.spotifyhub.spotify.dto.player

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PlaybackResponseDto(
    @param:Json(name = "is_playing") val isPlaying: Boolean?,
    @param:Json(name = "shuffle_state") val shuffleState: Boolean?,
    @param:Json(name = "repeat_state") val repeatState: String?,
    @param:Json(name = "progress_ms") val progressMs: Long?,
    @param:Json(name = "currently_playing_type") val currentlyPlayingType: String?,
    val item: PlaybackItemDto?,
    val device: DeviceDto?,
)

@JsonClass(generateAdapter = true)
data class PlaybackItemDto(
    val id: String?,
    val name: String?,
    @param:Json(name = "duration_ms") val durationMs: Long?,
    val uri: String?,
    val type: String? = null,
    val album: AlbumDto? = null,
    val artists: List<ArtistDto>? = null,
    val images: List<ImageDto>? = null,
    @param:Json(name = "release_date") val releaseDate: String? = null,
    val show: ShowDto? = null,
    val audiobook: AudiobookDto? = null,
)

@JsonClass(generateAdapter = true)
data class TrackDto(
    val id: String? = null,
    val name: String? = null,
    @param:Json(name = "duration_ms") val durationMs: Long? = null,
    val uri: String? = null,
    val album: AlbumDto? = null,
    val artists: List<ArtistDto>? = null,
)

@JsonClass(generateAdapter = true)
data class AlbumDto(
    val name: String?,
    val images: List<ImageDto>? = null,
    @param:Json(name = "release_date") val releaseDate: String? = null,
)

@JsonClass(generateAdapter = true)
data class ArtistDto(
    val name: String?,
)

@JsonClass(generateAdapter = true)
data class AuthorDto(
    val name: String?,
)

@JsonClass(generateAdapter = true)
data class ImageDto(
    val url: String?,
)

@JsonClass(generateAdapter = true)
data class ShowDto(
    val name: String?,
    val publisher: String? = null,
    val images: List<ImageDto>? = null,
)

@JsonClass(generateAdapter = true)
data class AudiobookDto(
    val name: String?,
    val authors: List<AuthorDto>? = null,
    val images: List<ImageDto>? = null,
)

@JsonClass(generateAdapter = true)
data class DeviceDto(
    val id: String?,
    val name: String?,
    val type: String?,
    @param:Json(name = "volume_percent") val volumePercent: Int?,
)
