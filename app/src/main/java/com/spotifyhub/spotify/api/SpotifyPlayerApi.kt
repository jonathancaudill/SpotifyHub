package com.spotifyhub.spotify.api

import com.spotifyhub.spotify.dto.player.PlaybackResponseDto
import com.spotifyhub.spotify.dto.player.QueueResponseDto
import retrofit2.http.Body
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

    @PUT("v1/me/player/shuffle")
    suspend fun setShuffle(
        @Query("state") enabled: Boolean,
    )

    @PUT("v1/me/player/repeat")
    suspend fun setRepeatMode(
        @Query("state") repeatMode: String,
    )

    @PUT("v1/me/player/volume")
    suspend fun setVolume(
        @Query("volume_percent") volumePercent: Int,
    )

    @PUT("v1/me/player/seek")
    suspend fun seekTo(
        @Query("position_ms") positionMs: Long,
    )

    @PUT("v1/me/player/play")
    suspend fun playContext(
        @Body body: PlayContextBody,
    )

    @GET("v1/me/player/queue")
    suspend fun getQueue(): QueueResponseDto?
}
