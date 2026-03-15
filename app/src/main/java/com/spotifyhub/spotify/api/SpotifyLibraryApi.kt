package com.spotifyhub.spotify.api

import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Query

interface SpotifyLibraryApi {
    @GET("v1/me/library/contains")
    suspend fun containsSavedItems(
        @Query("uris") uris: String,
    ): List<Boolean>

    @PUT("v1/me/library")
    suspend fun saveItems(
        @Query("uris") uris: String,
    )

    @DELETE("v1/me/library")
    suspend fun removeItems(
        @Query("uris") uris: String,
    )
}
