package com.spotifyhub.spotify.api

import com.spotifyhub.spotify.dto.player.PlaybackResponseDto
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

interface SpotifyPlayerApi {
    @GET("v1/me/player")
    suspend fun getCurrentPlayback(
        @Query("additional_types") additionalTypes: String = "track,episode",
    ): PlaybackResponseDto?

    @PUT("v1/me/player/play")
    suspend fun play()

    @PUT("v1/me/player/pause")
    suspend fun pause()

    @POST("v1/me/player/next")
    suspend fun skipNext()

    @POST("v1/me/player/previous")
    suspend fun skipPrevious()
}
