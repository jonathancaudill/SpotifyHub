package com.spotifyhub.search

import com.spotifyhub.browse.model.BrowseItem
import com.spotifyhub.search.model.SearchResults
import com.spotifyhub.spotify.api.SpotifySearchApi
import com.spotifyhub.spotify.mapper.SearchMapper
import java.text.Normalizer

class SearchRepository(
    private val searchApi: SpotifySearchApi,
) {
    suspend fun search(query: String): SearchResults {
        val normalizedQuery = normalizeQuery(query)
        if (normalizedQuery.isBlank()) {
            return SearchResults(
                tracks = emptyList(),
                albums = emptyList(),
                artists = emptyList(),
                playlists = emptyList(),
            )
        }

        val initialResults = performSearch(normalizedQuery, limit = 20)
        val withFallbacks = if (initialResults.isEmpty) {
            resolveFallbackResults(normalizedQuery)
        } else {
            initialResults
        }

        return rerankResults(withFallbacks, normalizedQuery)
    }

    private suspend fun performSearch(
        query: String,
        limit: Int,
    ): SearchResults {
        val response = searchApi.search(
            query = query,
            type = "track,album,artist,playlist",
            limit = limit,
            market = "from_token",
            includeExternal = "audio",
        )
        return SearchMapper.map(response)
    }

    private suspend fun resolveFallbackResults(query: String): SearchResults {
        var merged = SearchResults(
            tracks = emptyList(),
            albums = emptyList(),
            artists = emptyList(),
            playlists = emptyList(),
        )
        val fallbacks = buildFallbackQueries(query)
        for (fallbackQuery in fallbacks) {
            val fallbackResults = performSearch(fallbackQuery, limit = 12)
            merged = mergeResults(merged, fallbackResults)
            if (!merged.isEmpty) {
                break
            }
        }
        return merged
    }

    private fun buildFallbackQueries(query: String): List<String> {
        val strippedPunctuation = query.replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        val tokens = strippedPunctuation
            .split(Regex("\\s+"))
            .filter { it.length >= 2 }

        return buildList {
            if (strippedPunctuation.isNotBlank() && strippedPunctuation != query) {
                add(strippedPunctuation)
            }
            if (tokens.size > 1) {
                add(tokens.joinToString(" "))
                add(tokens.take(2).joinToString(" "))
            }
            if (tokens.isNotEmpty()) {
                add(tokens.first())
            }
        }.distinct()
            .filter { it.isNotBlank() && it != query }
    }

    private fun mergeResults(
        left: SearchResults,
        right: SearchResults,
    ): SearchResults {
        fun mergeItems(a: List<BrowseItem>, b: List<BrowseItem>): List<BrowseItem> {
            return (a + b).distinctBy { it.id }
        }
        return SearchResults(
            tracks = mergeItems(left.tracks, right.tracks),
            albums = mergeItems(left.albums, right.albums),
            artists = mergeItems(left.artists, right.artists),
            playlists = mergeItems(left.playlists, right.playlists),
        )
    }

    private fun rerankResults(
        results: SearchResults,
        query: String,
    ): SearchResults {
        val normalizedQuery = normalizeForMatching(query)
        val queryTokens = normalizedQuery.split(Regex("\\s+")).filter { it.isNotBlank() }

        fun rank(items: List<BrowseItem>): List<BrowseItem> {
            return items
                .sortedWith(
                    compareByDescending<BrowseItem> {
                        relevanceScore(
                            item = it,
                            normalizedQuery = normalizedQuery,
                            queryTokens = queryTokens,
                        )
                    }.thenBy { it.title.lowercase() },
                )
                .take(12)
        }

        return SearchResults(
            tracks = rank(results.tracks),
            albums = rank(results.albums),
            artists = rank(results.artists),
            playlists = rank(results.playlists),
        )
    }

    private fun relevanceScore(
        item: BrowseItem,
        normalizedQuery: String,
        queryTokens: List<String>,
    ): Int {
        val title = normalizeForMatching(item.title)
        val subtitle = normalizeForMatching(item.subtitle)

        var score = 0
        if (title == normalizedQuery) score += 220
        if (title.startsWith(normalizedQuery)) score += 140
        if (title.contains(normalizedQuery)) score += 80
        if (subtitle.contains(normalizedQuery)) score += 24

        for (token in queryTokens) {
            if (token.isBlank()) continue
            if (title == token) score += 40
            if (title.startsWith(token)) score += 20
            if (title.contains(token)) score += 12
            if (subtitle.contains(token)) score += 6
        }
        return score
    }

    private fun normalizeQuery(query: String): String {
        return query.replace(Regex("\\s+"), " ").trim()
    }

    private fun normalizeForMatching(text: String): String {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .lowercase()
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
