package com.spotifyhub.spotify.api

import com.spotifyhub.spotify.dto.browse.PlaylistPagingDto
import com.spotifyhub.spotify.dto.library.FullPlaylistDto
import com.spotifyhub.spotify.dto.library.PlaylistTrackPagingDto
import com.spotifyhub.spotify.dto.library.SavedAlbumPagingDto
import com.spotifyhub.spotify.dto.library.SavedTrackPagingDto
import com.spotifyhub.spotify.dto.library.TrackPagingDto
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface SpotifyLibraryApi {
    /* ── Existing save-state endpoints ────────────────────────────── */

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

    /* ── User playlists ──────────────────────────────────────────── */

    @GET("v1/me/playlists")
    suspend fun getUserPlaylists(
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
    ): PlaylistPagingDto

    /* ── Saved albums ────────────────────────────────────────────── */

    @GET("v1/me/albums")
    suspend fun getSavedAlbums(
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
    ): SavedAlbumPagingDto

    /* ── Saved tracks ────────────────────────────────────────────── */

    @GET("v1/me/tracks")
    suspend fun getSavedTracks(
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
    ): SavedTrackPagingDto

    /* ── Playlist detail ─────────────────────────────────────────── */

    @GET("v1/playlists/{id}")
    suspend fun getPlaylist(
        @Path("id") playlistId: String,
    ): FullPlaylistDto

    @GET("v1/playlists/{id}/tracks")
    suspend fun getPlaylistTracks(
        @Path("id") playlistId: String,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
    ): PlaylistTrackPagingDto

    /* ── Album tracks ────────────────────────────────────────────── */

    @GET("v1/albums/{id}/tracks")
    suspend fun getAlbumTracks(
        @Path("id") albumId: String,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
    ): TrackPagingDto
}
