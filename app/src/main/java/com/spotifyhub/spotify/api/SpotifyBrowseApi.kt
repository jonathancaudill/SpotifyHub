package com.spotifyhub.spotify.api

import com.spotifyhub.spotify.dto.browse.CategoriesResponseDto
import com.spotifyhub.spotify.dto.browse.CategoryPlaylistsResponseDto
import com.spotifyhub.spotify.dto.browse.FeaturedPlaylistsResponseDto
import com.spotifyhub.spotify.dto.browse.PlaylistPagingDto
import com.spotifyhub.spotify.dto.browse.RecentlyPlayedResponseDto
import com.spotifyhub.spotify.dto.browse.RecommendationsResponseDto
import com.spotifyhub.spotify.dto.browse.TopArtistsResponseDto
import com.spotifyhub.spotify.dto.browse.TopTracksResponseDto
import com.spotifyhub.spotify.dto.browse.UserProfileDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface SpotifyBrowseApi {
    @GET("v1/me/player/recently-played")
    suspend fun getRecentlyPlayed(
        @Query("limit") limit: Int = 50,
        @Query("before") before: Long? = null,
    ): RecentlyPlayedResponseDto

    @GET("v1/me/top/tracks")
    suspend fun getTopTracks(
        @Query("time_range") timeRange: String = "short_term",
        @Query("limit") limit: Int = 20,
    ): TopTracksResponseDto

    @GET("v1/me/top/artists")
    suspend fun getTopArtists(
        @Query("time_range") timeRange: String = "medium_term",
        @Query("limit") limit: Int = 20,
    ): TopArtistsResponseDto

    @GET("v1/me/playlists")
    suspend fun getUserPlaylists(
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
    ): PlaylistPagingDto

    @GET("v1/recommendations")
    suspend fun getRecommendations(
        @Query("seed_artists") seedArtists: String? = null,
        @Query("seed_tracks") seedTracks: String? = null,
        @Query("seed_genres") seedGenres: String? = null,
        @Query("limit") limit: Int = 20,
    ): RecommendationsResponseDto

    @GET("v1/browse/featured-playlists")
    suspend fun getFeaturedPlaylists(
        @Query("limit") limit: Int = 20,
    ): FeaturedPlaylistsResponseDto

    @GET("v1/me")
    suspend fun getCurrentUserProfile(): UserProfileDto

    @GET("v1/browse/categories")
    suspend fun getCategories(
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
        @Query("locale") locale: String? = null,
    ): CategoriesResponseDto

    @GET("v1/browse/categories/{categoryId}/playlists")
    suspend fun getCategoryPlaylists(
        @Path("categoryId") categoryId: String,
        @Query("limit") limit: Int = 20,
    ): CategoryPlaylistsResponseDto
}
