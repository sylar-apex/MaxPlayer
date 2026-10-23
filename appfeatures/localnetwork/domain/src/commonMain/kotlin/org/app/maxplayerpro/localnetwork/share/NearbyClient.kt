package org.app.maxplayerpro.localnetwork.share

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.encodeURLParameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.app.kmediaplayer.model.MediaFile

/** What went wrong talking to another device, worded for the user. [kind] tells the UI what to ask for next. */
class NearbyException(val kind: Kind, message: String) : Exception(message) {
    enum class Kind { DevicePinNeeded, SharePinNeeded, WrongPin, Expired, Gone, RateLimited, Unreachable, Other }
}

/** Talks to another device's share server: signs in with its PIN, lists and unlocks shares, builds playable files. */
class NearbyClient(engineClient: HttpClient) {
    private val http = engineClient.config {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        install(HttpTimeout) { requestTimeoutMillis = 8_000; connectTimeoutMillis = 4_000 }
        expectSuccess = false
    }

    suspend fun info(baseUrl: String): DeviceInfo = call { http.get("$baseUrl/api/v1/info") }.body()

    /** Exchanges the device PIN for a device token. */
    suspend fun login(baseUrl: String, pin: String): String = call(pinCall = true) {
        http.post("$baseUrl/api/v1/login") { contentType(ContentType.Application.Json); setBody(LoginRequest(pin)) }
    }.body<LoginResponse>().token

    suspend fun shares(baseUrl: String, deviceToken: String?): List<RemoteShare> =
        call { http.get("$baseUrl/api/v1/shares") { auth(deviceToken, null) } }.body()

    /** One share with its items; [shareToken] is needed for a protected share. */
    suspend fun share(baseUrl: String, id: String, deviceToken: String?, shareToken: String?): RemoteShare =
        call { http.get("$baseUrl/api/v1/shares/$id") { auth(deviceToken, shareToken) } }.body()

    suspend fun unlock(baseUrl: String, shareId: String, pin: String, deviceToken: String?): UnlockResponse = call(pinCall = true) {
        http.post("$baseUrl/api/v1/shares/$shareId/unlock") { auth(deviceToken, null); contentType(ContentType.Application.Json); setBody(UnlockRequest(pin)) }
    }.body()

    /** Playable files for [share]; media URLs carry the tokens as query parameters, since players can't add headers. */
    fun mediaFiles(baseUrl: String, share: RemoteShare, deviceToken: String?, shareToken: String?): List<MediaFile> {
        val query = listOfNotNull(deviceToken?.let { "d=" + it.encodeURLParameter() }, shareToken?.takeIf { it.isNotEmpty() }?.let { "t=" + it.encodeURLParameter() })
            .joinToString("&").let { if (it.isEmpty()) "" else "?$it" }
        return share.items.mapIndexed { i, item ->
            val url = baseUrl + item.path + query
            MediaFile(
                id = i.toLong(), uri = url, path = url, name = item.name, sizeBytes = item.sizeBytes, durationMs = item.durationMs,
                dateModifiedMs = 0, width = item.width, height = item.height, mimeType = item.mimeType, isAudio = item.isAudio,
            )
        }
    }

    private fun HttpRequestBuilder.auth(deviceToken: String?, shareToken: String?) {
        deviceToken?.let { header(DEVICE_TOKEN_HEADER, it) }
        shareToken?.takeIf { it.isNotEmpty() }?.let { header(SHARE_TOKEN_HEADER, it) }
    }

    /** [pinCall]: a 401 means the PIN just sent was wrong, not that one is missing. */
    private suspend fun call(pinCall: Boolean = false, block: suspend () -> HttpResponse): HttpResponse {
        val response = try {
            block()
        } catch (e: Exception) {
            throw NearbyException(NearbyException.Kind.Unreachable, "Device not reachable")
        }
        return when (response.status) {
            HttpStatusCode.OK -> response
            HttpStatusCode.Unauthorized -> throw when {
                response.headers[AUTH_HEADER] == AUTH_DEVICE -> NearbyException(NearbyException.Kind.DevicePinNeeded, "Enter this device's PIN")
                pinCall -> NearbyException(NearbyException.Kind.WrongPin, "Wrong PIN")
                else -> NearbyException(NearbyException.Kind.SharePinNeeded, "Enter the PIN for this share")
            }
            HttpStatusCode.Gone -> throw NearbyException(NearbyException.Kind.Expired, "Sharing time is over")
            HttpStatusCode.NotFound -> throw NearbyException(NearbyException.Kind.Gone, "No longer shared")
            HttpStatusCode.Forbidden -> throw NearbyException(NearbyException.Kind.Other, "Not open yet")
            HttpStatusCode.TooManyRequests -> throw NearbyException(NearbyException.Kind.RateLimited, "Too many wrong PINs. Try again in a minute")
            else -> throw NearbyException(NearbyException.Kind.Other, "Device answered ${response.status.value}")
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
