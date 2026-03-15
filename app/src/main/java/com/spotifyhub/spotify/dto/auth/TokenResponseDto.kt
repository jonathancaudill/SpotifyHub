package com.spotifyhub.spotify.dto.auth

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TokenResponseDto(
    @param:Json(name = "access_token") val accessToken: String,
    @param:Json(name = "token_type") val tokenType: String,
    @param:Json(name = "expires_in") val expiresIn: Long,
    @param:Json(name = "refresh_token") val refreshToken: String?,
    val scope: String?,
)
