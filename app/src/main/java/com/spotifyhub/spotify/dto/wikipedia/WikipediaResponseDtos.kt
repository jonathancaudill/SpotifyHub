package com.spotifyhub.spotify.dto.wikipedia

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WikipediaSearchResponseDto(
    val query: WikipediaQueryDto? = null,
)

@JsonClass(generateAdapter = true)
data class WikipediaQueryDto(
    val search: List<WikipediaSearchResultDto>? = null,
)

@JsonClass(generateAdapter = true)
data class WikipediaSearchResultDto(
    val title: String? = null,
)

@JsonClass(generateAdapter = true)
data class WikipediaSummaryDto(
    val title: String? = null,
    val extract: String? = null,
    val description: String? = null,
    val type: String? = null,
    @param:Json(name = "content_urls") val contentUrls: WikipediaContentUrlsDto? = null,
)

@JsonClass(generateAdapter = true)
data class WikipediaContentUrlsDto(
    val desktop: WikipediaPageUrlDto? = null,
)

@JsonClass(generateAdapter = true)
data class WikipediaPageUrlDto(
    val page: String? = null,
)
