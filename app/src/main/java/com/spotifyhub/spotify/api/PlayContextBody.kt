package com.spotifyhub.spotify.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PlayContextBody(
    @param:Json(name = "context_uri") val contextUri: String? = null,
    val uris: List<String>? = null,
    val offset: PlayOffset? = null,
)

@JsonClass(generateAdapter = true)
data class PlayOffset(
    val position: Int? = null,
    val uri: String? = null,
)
