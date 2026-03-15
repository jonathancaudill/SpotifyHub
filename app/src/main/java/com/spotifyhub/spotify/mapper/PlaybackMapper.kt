package com.spotifyhub.spotify.mapper

import com.spotifyhub.playback.model.PlaybackDevice
import com.spotifyhub.playback.model.PlaybackItem
import com.spotifyhub.playback.model.PlaybackSnapshot
import com.spotifyhub.spotify.dto.player.PlaybackResponseDto

object PlaybackMapper {
    fun map(dto: PlaybackResponseDto?): PlaybackSnapshot? {
        if (dto == null) {
            return null
        }

        return PlaybackSnapshot(
            isPlaying = dto.isPlaying == true,
            progressMs = dto.progressMs ?: 0L,
            item = dto.item?.let {
                PlaybackItem(
                    id = it.id.orEmpty(),
                    title = it.name.orEmpty(),
                    artist = it.artists.firstOrNull()?.name.orEmpty(),
                    album = it.album?.name.orEmpty(),
                    artworkUrl = it.album?.images?.firstOrNull()?.url,
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

