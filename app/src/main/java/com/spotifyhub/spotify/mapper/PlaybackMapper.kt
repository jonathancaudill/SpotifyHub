package com.spotifyhub.spotify.mapper

import com.spotifyhub.playback.model.PlaybackDevice
import com.spotifyhub.playback.model.PlaybackContentType
import com.spotifyhub.playback.model.PlaybackItem
import com.spotifyhub.playback.model.PlaybackSnapshot
import com.spotifyhub.playback.model.RepeatMode
import com.spotifyhub.spotify.dto.player.PlaybackItemDto
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
                val contentType = it.toContentType(dto.currentlyPlayingType)
                PlaybackItem(
                    id = it.id.orEmpty(),
                    title = it.name.orEmpty(),
                    artist = when (contentType) {
                        PlaybackContentType.Audiobook -> it.audiobook?.authors.orEmpty().firstOrNull()?.name.orEmpty()
                        PlaybackContentType.Podcast -> it.show?.publisher.orEmpty()
                        else -> it.artists.orEmpty().firstOrNull()?.name.orEmpty()
                    },
                    album = when (contentType) {
                        PlaybackContentType.Audiobook -> it.audiobook?.name.orEmpty()
                        PlaybackContentType.Podcast -> it.show?.name.orEmpty()
                        else -> it.album?.name.orEmpty()
                    },
                    artworkUrl = it.album?.images?.firstOrNull()?.url
                        ?: it.images?.firstOrNull()?.url
                        ?: it.audiobook?.images?.firstOrNull()?.url
                        ?: it.show?.images?.firstOrNull()?.url,
                    releaseDate = it.album?.releaseDate ?: it.releaseDate,
                    uri = it.uri.orEmpty(),
                    contentType = contentType,
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

    private fun PlaybackItemDto.toContentType(
        currentlyPlayingType: String?,
    ): PlaybackContentType {
        return when {
            audiobook != null -> PlaybackContentType.Audiobook
            show != null -> PlaybackContentType.Podcast
            currentlyPlayingType == "track" || type == "track" || uri.orEmpty().startsWith("spotify:track:") -> PlaybackContentType.Track
            currentlyPlayingType == "episode" || type == "episode" || uri.orEmpty().startsWith("spotify:episode:") -> PlaybackContentType.Podcast
            else -> PlaybackContentType.Unknown
        }
    }
}
