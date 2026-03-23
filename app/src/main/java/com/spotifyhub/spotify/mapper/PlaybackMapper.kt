package com.spotifyhub.spotify.mapper

import com.spotifyhub.playback.model.PlaybackDevice
import com.spotifyhub.playback.model.PlaybackItem
import com.spotifyhub.playback.model.PlaybackSnapshot
import com.spotifyhub.playback.model.RepeatMode
import com.spotifyhub.spotify.dto.player.PlaybackResponseDto
import java.time.Instant

object PlaybackMapper {
    fun map(dto: PlaybackResponseDto?): PlaybackSnapshot? {
        if (dto == null) {
            return null
        }

        return PlaybackSnapshot(
            isPlaying = dto.isPlaying == true,
            isShuffleEnabled = dto.shuffleState == true,
            repeatMode = when (dto.repeatState) {
                "context" -> RepeatMode.Context
                "track" -> RepeatMode.Track
                else -> RepeatMode.Off
            },
            progressMs = dto.progressMs ?: 0L,
            durationMs = dto.item?.durationMs ?: 0L,
            fetchedAtEpochMs = Instant.now().toEpochMilli(),
            item = dto.item?.let {
                PlaybackItem(
                    id = it.id.orEmpty(),
                    title = it.name.orEmpty(),
                    artist = it.artists.orEmpty().firstOrNull()?.name.orEmpty(),
                    album = it.album?.name.orEmpty(),
                    artworkUrl = it.album?.images?.firstOrNull()?.url,
                    releaseDate = it.album?.releaseDate,
                    uri = it.uri.orEmpty(),
                )
            },
            device = dto.device?.let {
                PlaybackDevice(
                    id = it.id.orEmpty(),
                    name = it.name.orEmpty(),
                    type = it.type.orEmpty(),
                    volumePercent = it.volumePercent,
                )
            },
        )
    }
}
