package com.spotifyhub.spotify.api

import com.spotifyhub.spotify.dto.wikipedia.WikipediaSearchResponseDto
import com.spotifyhub.spotify.dto.wikipedia.WikipediaSummaryDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface WikipediaApi {
    @GET("w/api.php?action=query&list=search&utf8=1&format=json")
    suspend fun searchPages(
        @Query("srsearch") query: String,
        @Query("srlimit") limit: Int = 5,
    ): WikipediaSearchResponseDto

    @GET("api/rest_v1/page/summary/{title}")
    suspend fun getSummary(
        @Path(value = "title", encoded = true) title: String,
    ): WikipediaSummaryDto
}
