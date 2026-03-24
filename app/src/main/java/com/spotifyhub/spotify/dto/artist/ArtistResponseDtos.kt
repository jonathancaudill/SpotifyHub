package com.spotifyhub.spotify.dto.artist

import com.spotifyhub.spotify.dto.player.ImageDto
import com.spotifyhub.spotify.dto.search.SimplifiedAlbumDto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ArtistDetailDto(
    val id: String? = null,
    val name: String? = null,
    val images: List<ImageDto>? = null,
    val genres: List<String>? = null,
    val uri: String? = null,
    val popularity: Int? = null,
    val followers: FollowersDto? = null,
    @param:Json(name = "external_urls") val externalUrls: ExternalUrlsDto? = null,
)

@JsonClass(generateAdapter = true)
data class FollowersDto(
    val total: Int? = null,
)

@JsonClass(generateAdapter = true)
data class ExternalUrlsDto(
    val spotify: String? = null,
)

@JsonClass(generateAdapter = true)
data class ArtistAlbumPagingDto(
    val items: List<SimplifiedAlbumDto>? = null,
    val total: Int? = null,
    val limit: Int? = null,
    val offset: Int? = null,
    val next: String? = null,
)
