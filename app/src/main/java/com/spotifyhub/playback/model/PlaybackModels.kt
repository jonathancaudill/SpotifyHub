package com.spotifyhub.playback.model

data class PlaybackSnapshot(
    val isPlaying: Boolean,
    val progressMs: Long,
    val durationMs: Long,
    val fetchedAtEpochMs: Long,
    val item: PlaybackItem?,
    val device: PlaybackDevice?,
)

data class PlaybackItem(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val artworkUrl: String?,
    val uri: String,
)

data class PlaybackDevice(
    val id: String,
    val name: String,
    val type: String,
    val volumePercent: Int?,
)
