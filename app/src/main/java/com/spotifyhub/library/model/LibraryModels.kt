package com.spotifyhub.library.model

data class LibraryItem(
    val id: String,
    val name: String,
    val subtitle: String,
    val artworkUrl: String?,
    val uri: String,
    val type: LibraryItemType,
    val trackCount: Int? = null,
)

enum class LibraryItemType {
    Playlist,
    Album,
    Track,
}

data class PlaylistDetail(
    val id: String,
    val name: String,
    val description: String?,
    val artworkUrl: String?,
    val ownerName: String?,
    val uri: String,
    val tracks: List<TrackItem>,
    val totalTracks: Int,
)

data class AlbumDetail(
    val id: String,
    val name: String,
    val artistName: String?,
    val artworkUrl: String?,
    val uri: String,
    val tracks: List<TrackItem>,
    val totalTracks: Int,
)

data class TrackItem(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val artworkUrl: String?,
    val durationMs: Long,
    val uri: String,
    val trackNumber: Int? = null,
)
