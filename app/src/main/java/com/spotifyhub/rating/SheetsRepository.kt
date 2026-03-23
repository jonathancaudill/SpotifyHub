package com.spotifyhub.rating

import com.spotifyhub.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class SheetsRepository {

    companion object {
        /**
         * Set via Gradle property SHEETS_SCRIPT_URL in gradle.properties or local env.
         * This is the deployed Google Apps Script web-app URL.
         */
        val APPS_SCRIPT_URL: String = BuildConfig.SHEETS_SCRIPT_URL
    }

    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Looks up whether an album already has a rating in the sheet.
     * Returns the existing rating if found, or null if not.
     */
    suspend fun lookupRating(
        artistName: String,
        title: String,
    ): Result<Float?> = withContext(Dispatchers.IO) {
        runCatching {
            val url = APPS_SCRIPT_URL.toHttpUrl().newBuilder()
                .addQueryParameter("action", "lookup")
                .addQueryParameter("artistName", artistName)
                .addQueryParameter("title", title)
                .build()

            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    error("Sheets lookup returned ${response.code}")
                }
                val body = response.body.string()
                val json = JSONObject(body)
                if (json.optBoolean("found", false)) {
                    json.optDouble("rating", -1.0).let { rating ->
                        if (rating >= 0.0) rating.toFloat() else null
                    }
                } else {
                    null
                }
            }
        }
    }

    suspend fun submitRating(
        albumCover: String?,
        artistName: String,
        title: String,
        releaseDate: String?,
        rating: Float,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val json = buildJsonPayload(albumCover, artistName, title, releaseDate, rating)

            val request = Request.Builder()
                .url(APPS_SCRIPT_URL)
                .post(json.toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    error("Sheets API returned ${response.code}: ${response.body.string()}")
                }
            }
        }
    }

    private fun buildJsonPayload(
        albumCover: String?,
        artistName: String,
        title: String,
        releaseDate: String?,
        rating: Float,
    ): String {
        // Manual JSON building to avoid needing Moshi adapter generation
        fun String.jsonEscape(): String =
            replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")

        return buildString {
            append('{')
            append("\"albumCover\":\"${(albumCover.orEmpty()).jsonEscape()}\",")
            append("\"artistName\":\"${artistName.jsonEscape()}\",")
            append("\"title\":\"${title.jsonEscape()}\",")
            append("\"releaseDate\":\"${(releaseDate.orEmpty()).jsonEscape()}\",")
            append("\"rating\":${String.format("%.1f", rating)}")
            append('}')
        }
    }
}
