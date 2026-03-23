package com.spotifyhub.spotify.dto.embed

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SpotifyOEmbedResponseDto(
    val title: String? = null,
    val thumbnail_url: String? = null,
)
