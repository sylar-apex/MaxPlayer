package org.app.maxplayerpro.localnetwork.share

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.app.kmediaplayer.model.MediaFile

/** What went wrong talking to another device, worded for the user. */
class NearbyException(message: String) : Exception(message)

/** Talks to another device's share server: lists its shares, unlocks one with a code and builds playable files. */
class NearbyClient(engineClient: HttpClient) {
    private val http = engineClient.config {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        install(HttpTimeout) { requestTimeoutMillis = 8_000; connectTimeoutMillis = 4_000 }
        expectSuccess = false
    }

    suspend fun info(baseUrl: String): DeviceInfo = call { http.get("$baseUrl/api/v1/info") }.body()

    suspend fun shares(baseUrl: String): List<RemoteShare> = call { http.get("$baseUrl/api/v1/shares") }.body()

    suspend fun unlock(baseUrl: String, shareId: String, code: String): UnlockResponse {
        val response = call {
            http.post("$baseUrl/api/v1/shares/$shareId/unlock") { contentType(ContentType.Application.Json); setBody(UnlockRequest(code)) }
        }
        return response.body()
    }

    /** Playable files for [share]; [token] comes from [unlock] for coded shares. */
    fun mediaFiles(baseUrl: String, share: RemoteShare, token: String?): List<MediaFile> = share.items.mapIndexed { i, item ->
        val url = baseUrl + item.path + (token?.let { "?t=$it" } ?: "")
        MediaFile(
            id = i.toLong(), uri = url, path = url, name = item.name, sizeBytes = item.sizeBytes, durationMs = item.durationMs,
            dateModifiedMs = 0, width = item.width, height = item.height, mimeType = item.mimeType, isAudio = item.isAudio,
        )
    }

    private suspend fun call(block: suspend () -> HttpResponse): HttpResponse {
        val response = try {
            block()
        } catch (e: Exception) {
            throw NearbyException("Device not reachable (${e.message ?: e::class.simpleName})")
        }
        return when (response.status) {
            HttpStatusCode.OK -> response
            HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden -> throw NearbyException("Wrong code")
            HttpStatusCode.Gone -> throw NearbyException("This share has expired")
            HttpStatusCode.NotFound -> throw NearbyException("Share no longer available")
            HttpStatusCode.TooManyRequests -> throw NearbyException("Too many wrong codes. Try again in a minute")
            else -> throw NearbyException("Device answered ${response.status.value}")
        }
    }

    companion object {
        /** Accepts `192.168.1.5`, `192.168.1.5:53319` or a full `http://` URL. */
        fun normalizeAddress(input: String): String? {
            val t = input.trim().trimEnd('/')
            if (t.isEmpty()) return null
            val withScheme = if ("://" in t) t else "http://$t"
            val hostPart = withScheme.substringAfter("://")
            return if (':' in hostPart) withScheme else "$withScheme:$DEFAULT_SHARE_PORT"
        }
    }
}
