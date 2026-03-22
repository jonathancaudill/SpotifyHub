package com.spotifyhub.spotify.api

import com.spotifyhub.spotify.dto.search.SearchResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface SpotifySearchApi {
    @GET("v1/search")
    suspend fun search(
        @Query("q") query: String,
        @Query("type") type: String = "track,album,artist,playlist",
        @Query("limit") limit: Int = 10,
        @Query("offset") offset: Int = 0,
        @Query("market") market: String? = "from_token",
        @Query("include_external") includeExternal: String? = "audio",
    ): SearchResponseDto
}
