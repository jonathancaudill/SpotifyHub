package com.spotifyhub.artist.model

data class ArtistDetail(
    val id: String,
    val name: String,
    val uri: String,
    val artworkUrl: String?,
    val genres: List<String>,
    val followersTotal: Int?,
    val popularity: Int?,
    val albums: List<ArtistReleaseSummary>,
    val singlesAndEps: List<ArtistReleaseSummary>,
    val featuredOn: List<ArtistReleaseSummary>,
    val curatedPlaylists: List<ArtistPlaylistSummary>,
    val bio: ArtistBio? = null,
)

data class ArtistReleaseSummary(
    val id: String,
    val title: String,
    val subtitle: String,
    val artworkUrl: String?,
    val uri: String,
    val totalTracks: Int?,
)

data class ArtistPlaylistSummary(
    val id: String,
    val title: String,
    val subtitle: String,
    val artworkUrl: String?,
    val uri: String,
)

data class ArtistBio(
    val title: String,
    val summary: String,
    val sourceUrl: String?,
)
