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
import java.util.concurrent.TimeUnit

class SheetsRepository {

    companion object {
        /**
         * Set via Gradle property SHEETS_SCRIPT_URL in gradle.properties or local env.
         * This is the deployed Google Apps Script web-app URL.
         */
        val APPS_SCRIPT_URL: String = BuildConfig.SHEETS_SCRIPT_URL
    }

    /**
     * Google Apps Script returns a 302 redirect from script.google.com to
     * script.googleusercontent.com.  OkHttp's default behaviour for 302 is
     * to replay the request as GET, which drops the POST body.
     *
     * We disable automatic redirects and manually follow the 302 while
     * preserving the original method and body.
     *
     * Apps Script cold-starts can also take 20-30s, so generous timeouts
     * are essential.
     */
    private val client = OkHttpClient.Builder()
        .followRedirects(false)
        .followSslRedirects(false)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /** Max redirects to follow manually (safety valve). */
    private val maxRedirects = 5

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

            val body = executeFollowingRedirects(request)
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

            executeFollowingRedirects(request)
            Unit
        }
    }

    /**
     * Execute a request, manually following any 3xx redirects while
     * preserving the original HTTP method and body.  This is critical
     * for Google Apps Script which 302-redirects POSTs.
     */
    private fun executeFollowingRedirects(originalRequest: Request): String {
        var request = originalRequest
        var redirectCount = 0

        while (true) {
            val response = client.newCall(request).execute()

            if (response.isRedirect) {
                val location = response.header("Location")
                    ?: error("Redirect with no Location header")
                response.close()

                if (++redirectCount > maxRedirects) {
                    error("Too many redirects ($maxRedirects)")
                }

                // Rebuild request to the new URL, preserving method + body
                request = request.newBuilder()
                    .url(location)
                    .build()
                continue
            }

            return response.use { resp ->
                if (!resp.isSuccessful) {
                    error("Sheets API returned ${resp.code}: ${resp.body.string()}")
                }
                resp.body.string()
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
