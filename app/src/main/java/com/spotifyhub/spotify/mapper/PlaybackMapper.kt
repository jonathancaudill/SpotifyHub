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
            item = dto.item?.let { mapItem(it, dto.currentlyPlayingType) },
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

    /** Map a single [PlaybackItemDto] to a domain [PlaybackItem]. Reused for queue items. */
    fun mapItem(
        dto: PlaybackItemDto,
        currentlyPlayingType: String? = null,
    ): PlaybackItem {
        val contentType = dto.toContentType(currentlyPlayingType)
        return PlaybackItem(
            id = dto.id.orEmpty(),
            title = dto.name.orEmpty(),
            artist = when (contentType) {
                PlaybackContentType.Audiobook -> dto.audiobook?.authors.orEmpty().firstOrNull()?.name.orEmpty()
                PlaybackContentType.Podcast -> dto.show?.publisher.orEmpty()
                else -> dto.artists.orEmpty().firstOrNull()?.name.orEmpty()
            },
            album = when (contentType) {
                PlaybackContentType.Audiobook -> dto.audiobook?.name.orEmpty()
                PlaybackContentType.Podcast -> dto.show?.name.orEmpty()
                else -> dto.album?.name.orEmpty()
            },
            artworkUrl = dto.album?.images?.firstOrNull()?.url
                ?: dto.images?.firstOrNull()?.url
                ?: dto.audiobook?.images?.firstOrNull()?.url
                ?: dto.show?.images?.firstOrNull()?.url,
            releaseDate = dto.album?.releaseDate ?: dto.releaseDate,
            uri = dto.uri.orEmpty(),
            durationMs = dto.durationMs ?: 0L,
            contentType = contentType,
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
