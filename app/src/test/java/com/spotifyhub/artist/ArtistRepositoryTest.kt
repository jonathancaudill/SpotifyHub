package com.spotifyhub.artist

import com.spotifyhub.artist.model.ArtistBio
import com.spotifyhub.spotify.api.SpotifyArtistApi
import com.spotifyhub.spotify.api.SpotifySearchApi
import com.spotifyhub.spotify.api.WikipediaApi
import com.spotifyhub.spotify.dto.artist.ArtistAlbumPagingDto
import com.spotifyhub.spotify.dto.artist.ArtistDetailDto
import com.spotifyhub.spotify.dto.artist.ExternalUrlsDto
import com.spotifyhub.spotify.dto.artist.FollowersDto
import com.spotifyhub.spotify.dto.browse.PlaylistOwnerDto
import com.spotifyhub.spotify.dto.browse.SimplifiedPlaylistDto
import com.spotifyhub.spotify.dto.player.ArtistDto
import com.spotifyhub.spotify.dto.player.ImageDto
import com.spotifyhub.spotify.dto.search.PlaylistPagingDto
import com.spotifyhub.spotify.dto.search.SearchResponseDto
import com.spotifyhub.spotify.dto.search.SimplifiedAlbumDto
import com.spotifyhub.spotify.dto.wikipedia.WikipediaContentUrlsDto
import com.spotifyhub.spotify.dto.wikipedia.WikipediaPageUrlDto
import com.spotifyhub.spotify.dto.wikipedia.WikipediaQueryDto
import com.spotifyhub.spotify.dto.wikipedia.WikipediaSearchResponseDto
import com.spotifyhub.spotify.dto.wikipedia.WikipediaSearchResultDto
import com.spotifyhub.spotify.dto.wikipedia.WikipediaSummaryDto
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtistRepositoryTest {
    @Test
    fun `artist detail loads grouped releases and curated playlists`() = runTest {
        val repository = ArtistRepository(
            artistApi = FakeArtistApi(),
            searchApi = FakeSearchApi(
                results = mapOf(
                    "playlist:\"This Is Sufjan Stevens\"" to playlistsResponse(
                        playlist("this-is", "This Is Sufjan Stevens", "Spotify"),
                    ),
                    "playlist:\"Mixed By Sufjan Stevens\"" to playlistsResponse(
                        playlist("mixed-by", "Mixed By Sufjan Stevens", "Spotify"),
                    ),
                ),
            ),
            wikipediaApi = EmptyWikipediaApi(),
        )

        val detail = repository.getArtistDetail("artist-1")

        assertEquals("Sufjan Stevens", detail.name)
        assertEquals(listOf("album-1"), detail.albums.map { it.id })
        assertEquals(listOf("single-1"), detail.singlesAndEps.map { it.id })
        assertEquals(listOf("appears-1"), detail.featuredOn.map { it.id })
        assertEquals(listOf("this-is", "mixed-by"), detail.curatedPlaylists.map { it.id })
    }

    @Test
    fun `artist bio accepts exact wikipedia match`() = runTest {
        val repository = ArtistRepository(
            artistApi = FakeArtistApi(),
            searchApi = FakeSearchApi(),
            wikipediaApi = object : WikipediaApi {
                override suspend fun searchPages(query: String, limit: Int): WikipediaSearchResponseDto {
                    return WikipediaSearchResponseDto(
                        query = WikipediaQueryDto(
                            search = listOf(
                                WikipediaSearchResultDto(title = "Sufjan Stevens"),
                            ),
                        ),
                    )
                }

                override suspend fun getSummary(title: String): WikipediaSummaryDto {
                    return WikipediaSummaryDto(
                        title = "Sufjan Stevens",
                        extract = "Sufjan Stevens is an American singer-songwriter.",
                        type = "standard",
                        contentUrls = WikipediaContentUrlsDto(
                            desktop = WikipediaPageUrlDto(page = "https://en.wikipedia.org/wiki/Sufjan_Stevens"),
                        ),
                    )
                }
            },
        )

        val bio = repository.getArtistBio("Sufjan Stevens")

        requireNotNull(bio)
        assertTrue(bio.summary.contains("American singer-songwriter"))
    }

    @Test
    fun `artist bio rejects ambiguous wikipedia match`() = runTest {
        val repository = ArtistRepository(
            artistApi = FakeArtistApi(),
            searchApi = FakeSearchApi(),
            wikipediaApi = object : WikipediaApi {
                override suspend fun searchPages(query: String, limit: Int): WikipediaSearchResponseDto {
                    return WikipediaSearchResponseDto(
                        query = WikipediaQueryDto(
                            search = listOf(
                                WikipediaSearchResultDto(title = "Chicago"),
                            ),
                        ),
                    )
                }

                override suspend fun getSummary(title: String): WikipediaSummaryDto {
                    return WikipediaSummaryDto(
                        title = "Chicago",
                        extract = "Chicago is the most populous city in Illinois.",
                        type = "standard",
                    )
                }
            },
        )

        val bio = repository.getArtistBio("Chicago")

        assertNull(bio)
    }

    private class FakeArtistApi : SpotifyArtistApi {
        override suspend fun getArtist(artistId: String): ArtistDetailDto {
            return ArtistDetailDto(
                id = artistId,
                name = "Sufjan Stevens",
                uri = "spotify:artist:$artistId",
                genres = listOf("indie folk", "chamber pop"),
                popularity = 78,
                followers = FollowersDto(total = 1250000),
                images = listOf(ImageDto(url = "https://images.test/artist.jpg")),
                externalUrls = ExternalUrlsDto(spotify = "https://open.spotify.com/artist/$artistId"),
            )
        }

        override suspend fun getArtistAlbums(
            artistId: String,
            includeGroups: String,
            limit: Int,
            offset: Int,
            market: String?,
        ): ArtistAlbumPagingDto {
            val item = when (includeGroups) {
                "album" -> testAlbum("album-1", "Carrie & Lowell")
                "single" -> testAlbum("single-1", "Mystery of Love", type = "single")
                "appears_on" -> testAlbum("appears-1", "Dark Was the Night", artistName = "Various Artists")
                else -> null
            }

            return ArtistAlbumPagingDto(
                items = listOfNotNull(item),
                total = if (item == null) 0 else 1,
                next = null,
            )
        }
    }

    private class FakeSearchApi(
        private val results: Map<String, SearchResponseDto> = emptyMap(),
    ) : SpotifySearchApi {
        override suspend fun search(
            query: String,
            type: String,
            limit: Int,
            offset: Int,
            market: String?,
            includeExternal: String?,
        ): SearchResponseDto {
            return results[query] ?: SearchResponseDto(
                playlists = PlaylistPagingDto(items = emptyList()),
            )
        }
    }

    private class EmptyWikipediaApi : WikipediaApi {
        override suspend fun searchPages(query: String, limit: Int): WikipediaSearchResponseDto {
            return WikipediaSearchResponseDto(query = WikipediaQueryDto(search = emptyList()))
        }

        override suspend fun getSummary(title: String): WikipediaSummaryDto {
            return WikipediaSummaryDto()
        }
    }

    private fun playlistsResponse(vararg items: SimplifiedPlaylistDto): SearchResponseDto {
        return SearchResponseDto(
            playlists = PlaylistPagingDto(items = items.toList()),
        )
    }

    private fun playlist(id: String, name: String, ownerName: String): SimplifiedPlaylistDto {
        return SimplifiedPlaylistDto(
            id = id,
            name = name,
            owner = PlaylistOwnerDto(displayName = ownerName),
            images = listOf(ImageDto(url = "https://images.test/$id.jpg")),
            uri = "spotify:playlist:$id",
        )
    }

    private fun album(
        id: String,
        name: String,
        type: String = "album",
        artistName: String = "Sufjan Stevens",
    ): SimplifiedAlbumDto {
        return SimplifiedAlbumDto(
            id = id,
            name = name,
            albumType = type,
            artists = listOf(ArtistDto(name = artistName)),
            images = listOf(ImageDto(url = "https://images.test/$id.jpg")),
            totalTracks = 10,
            uri = "spotify:album:$id",
        )
    }

    companion object {
        private fun testAlbum(
            id: String,
            name: String,
            type: String = "album",
            artistName: String = "Sufjan Stevens",
        ): SimplifiedAlbumDto {
            return SimplifiedAlbumDto(
                id = id,
                name = name,
                albumType = type,
                artists = listOf(ArtistDto(name = artistName)),
                images = listOf(ImageDto(url = "https://images.test/$id.jpg")),
                totalTracks = 10,
                uri = "spotify:album:$id",
            )
        }
    }
}
