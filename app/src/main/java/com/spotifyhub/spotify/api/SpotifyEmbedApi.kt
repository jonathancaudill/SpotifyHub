package com.spotifyhub.spotify.api

import com.spotifyhub.spotify.dto.embed.SpotifyOEmbedResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface SpotifyEmbedApi {
    @GET("oembed")
    suspend fun getOEmbed(
        @Query("url") url: String,
    ): SpotifyOEmbedResponseDto
}
