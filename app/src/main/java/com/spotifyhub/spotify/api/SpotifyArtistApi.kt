package com.spotifyhub.spotify.api

import com.spotifyhub.spotify.dto.artist.ArtistAlbumPagingDto
import com.spotifyhub.spotify.dto.artist.ArtistDetailDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface SpotifyArtistApi {
    @GET("v1/artists/{id}")
    suspend fun getArtist(
        @Path("id") artistId: String,
    ): ArtistDetailDto

    @GET("v1/artists/{id}/albums")
    suspend fun getArtistAlbums(
        @Path("id") artistId: String,
        @Query("include_groups") includeGroups: String,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
        @Query("market") market: String? = "from_token",
    ): ArtistAlbumPagingDto
}
