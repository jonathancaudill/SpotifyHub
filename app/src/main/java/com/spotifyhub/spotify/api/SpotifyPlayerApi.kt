package com.spotifyhub.spotify.api

import com.spotifyhub.spotify.dto.player.PlaybackResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface SpotifyPlayerApi {
    @GET("v1/me/player")
    suspend fun getCurrentPlayback(
        @Query("additional_types") additionalTypes: String = "track,episode",
    ): PlaybackResponseDto?
}

