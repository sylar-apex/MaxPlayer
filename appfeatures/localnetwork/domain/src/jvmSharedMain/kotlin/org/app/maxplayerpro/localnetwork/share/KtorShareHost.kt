package org.app.maxplayerpro.localnetwork.share

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.encodeURLPathPart
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.autohead.AutoHeadResponse
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.origin
import io.ktor.server.plugins.partialcontent.PartialContent
import io.ktor.server.request.header
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.writeFully
import io.ktor.utils.io.writer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.app.kmediaplayer.util.nowMs
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * Serves [ShareStore]'s shares over HTTP with Ktor. Seeking uses Ktor's PartialContent plugin, which handles every
 * Range form (`a-b`, `a-`, `-n`, multiple ranges, If-Range). Start/stop run one at a time under [lifecycle], and
 * request handlers only read immutable snapshots, so a share made private mid-stream stops at the next request.
 *
 * Access has two layers: an optional device PIN (exchanged for a device token at `login`) guards everything, and a
 * protected share additionally needs its own PIN (exchanged for a share token at `unlock`). Tokens are signed, not
 * stored (see [AccessTokens]).
 */
class KtorShareHost(private val store: ShareStore, private val platform: HostPlatform) : ShareHost {
    override val supported = true
    private val _state = MutableStateFlow<HostState>(HostState.Stopped)
    override val state: StateFlow<HostState> = _state.asStateFlow()

    private val lifecycle = Mutex()
    private var server: EmbeddedServer<*, *>? = null
    private var ioScope: CoroutineScope? = null
    private val tokens = AccessTokens(store.tokenSecret)
    private val failures = ConcurrentHashMap<String, Failures>()

    private data class Failures(val count: Int, val windowStartMs: Long)

    override suspend fun start() { lifecycle.withLock {
        if (server != null) return@withLock
        _state.value = HostState.Starting
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val started = withContext(Dispatchers.IO) {
            // Same port every time so a saved address keeps working; any free port if something else holds it.
            runCatching { launch(DEFAULT_SHARE_PORT, scope) }.recoverCatching { launch(0, scope) }
        }
        started.onSuccess { (srv, port) ->
            server = srv
            ioScope = scope
            _state.value = HostState.Running(port, lanInterfaces().map { (_, ip) -> "http://${ip.hostAddress}:$port" })
            platform.onHostingChanged(true)
        }.onFailure {
            scope.cancel()
            _state.value = HostState.Failed(it.message ?: "Could not start sharing")
        }
    } }

    override suspend fun stop() { lifecycle.withLock {
        val srv = server ?: return@withLock
        server = null
        withContext(Dispatchers.IO) { srv.stop(gracePeriodMillis = 300, timeoutMillis = 2_000) }
        ioScope?.cancel()
        ioScope = null
        platform.onHostingChanged(false)
        _state.value = HostState.Stopped
    } }

    private suspend fun launch(port: Int, scope: CoroutineScope): Pair<EmbeddedServer<*, *>, Int> {
        val srv = embeddedServer(CIO, port = port, host = "0.0.0.0") { module(scope) }
        srv.start(wait = false)
        val actual = srv.engine.resolvedConnectors().first().port
        return srv to actual
    }

    private fun Application.module(scope: CoroutineScope) {
        install(AutoHeadResponse)
        install(PartialContent)
        install(ContentNegotiation) { json(Json { encodeDefaults = true }) }
        // The browser build of Max Player runs on another origin; it only ever reads.
        install(CORS) {
            anyHost()
            allowMethod(HttpMethod.Get)
            allowMethod(HttpMethod.Post)
            allowHeader(HttpHeaders.ContentType)
            allowHeader(HttpHeaders.Range)
            allowHeader(DEVICE_TOKEN_HEADER)
            allowHeader(SHARE_TOKEN_HEADER)
            exposeHeader(AUTH_REASON_HEADER)
            exposeHeader(HttpHeaders.ContentRange)
            exposeHeader(HttpHeaders.AcceptRanges)
            exposeHeader(AUTH_HEADER)
        }
        routing {
            get("/") { call.respondText(WEB_PAGE, ContentType.Text.Html) }
            get("/api/v1/info") {
                call.respond(DeviceInfo(store.deviceId, platform.deviceName, platform.platform, call.request.local.localPort, pinRequired = store.device.value.pin != null))
            }
            post("/api/v1/login") {
                val now = nowMs()
                if (!deviceOpen(now)) return@post call.respond(HttpStatusCode.Gone)
                if (limited(call, now)) return@post call.respond(HttpStatusCode.TooManyRequests)
                val device = store.device.value
                val req = runCatching { call.receive<LoginRequest>() }.getOrDefault(LoginRequest(""))
                if (device.pin != null && !samePin(device.pin, req.pin)) {
                    failed(call, now)
                    return@post call.respond(HttpStatusCode.Unauthorized)
                }
                failures.remove(call.request.origin.remoteHost)
                // Devices that don't say who they are get a one-off id; they show up as "Unknown device".
                val clientId = req.clientId.filter { it.isLetterOrDigit() }.take(64).ifEmpty { ShareStore.newId() }
                store.signIn(clientId, ViewerSession(req.clientName.take(80).ifBlank { "Unknown device" }, req.clientPlatform.take(40), now))
                val epoch = store.device.value.sessionEpoch
                call.respond(LoginResponse(tokens.issue(AccessTokens.device(clientId), epoch, device.untilMs ?: Long.MAX_VALUE)))
            }
            get("/api/v1/shares") {
                val now = nowMs()
                if (!admitDevice(call, call.request.header(DEVICE_TOKEN_HEADER), now)) return@get
                val shareToken = call.request.header(SHARE_TOKEN_HEADER)
                call.respond(store.shares.value.filter { it.isOpen(now) }.map { it.toRemote(withItems = canOpen(it, shareToken, now)) })
            }
            get("/api/v1/shares/{id}") {
                val now = nowMs()
                if (!admitDevice(call, call.request.header(DEVICE_TOKEN_HEADER), now)) return@get
                val share = store.find(call.parameters["id"].orEmpty())
                if (!admitShare(call, share, call.request.header(SHARE_TOKEN_HEADER), now)) return@get
                call.respond(share!!.toRemote(withItems = true))
            }
            post("/api/v1/shares/{id}/unlock") {
                val now = nowMs()
                if (!admitDevice(call, call.request.header(DEVICE_TOKEN_HEADER), now)) return@post
                if (limited(call, now)) return@post call.respond(HttpStatusCode.TooManyRequests)
                val share = store.find(call.parameters["id"].orEmpty())
                when {
                    share == null || share.revoked -> call.respond(HttpStatusCode.NotFound)
                    share.isExpired(now) -> call.respond(HttpStatusCode.Gone)
                    !share.isOpen(now) -> call.respond(HttpStatusCode.Forbidden)
                    share.access == ShareAccess.Public -> call.respond(UnlockResponse("", share.expiresAtMs ?: Long.MAX_VALUE, share.toRemote(true)))
                    !samePin(share.code, runCatching { call.receive<UnlockRequest>().code }.getOrDefault("")) -> {
                        failed(call, now)
                        call.respond(HttpStatusCode.Unauthorized)
                    }
                    else -> {
                        failures.remove(call.request.origin.remoteHost)
                        val expires = share.expiresAtMs ?: Long.MAX_VALUE
                        val clientId = signedInClient(call.request.header(DEVICE_TOKEN_HEADER), now).orEmpty()
                        call.respond(UnlockResponse(tokens.issue(AccessTokens.share(share.id, clientId), store.device.value.sessionEpoch, expires), expires, share.toRemote(true)))
                    }
                }
            }
            get("/media/{share}/{item}/{name}") {
                val now = nowMs()
                if (!admitDevice(call, call.request.queryParameters["d"], now)) return@get
                val share = store.find(call.parameters["share"].orEmpty())
                if (!admitShare(call, share, call.request.queryParameters["t"], now)) return@get
                val item = share!!.items.firstOrNull { it.id == call.parameters["item"] } ?: return@get call.respond(HttpStatusCode.NotFound)
                val size = platform.open(item)?.use { it.size() } ?: return@get call.respond(HttpStatusCode.NotFound)
                call.response.header(HttpHeaders.CacheControl, "no-store")
                call.respond(ChannelContent(scope, { platform.open(item) }, size, ContentType.parse(item.mimeType.ifEmpty { "application/octet-stream" })))
            }
        }
    }

    /** Device-level time limit: past it, nothing on this device opens. */
    private fun deviceOpen(nowMs: Long): Boolean = store.device.value.untilMs?.let { nowMs < it } ?: true

    /** The device id behind a current device token whose device is still signed in, else null. */
    private fun signedInClient(token: String?, nowMs: Long): String? {
        val device = store.device.value
        val scope = tokens.read(token, device.sessionEpoch, nowMs)?.takeIf { it.startsWith("device:") } ?: return null
        return AccessTokens.clientOf(scope).takeIf { it in device.sessions }
    }

    /** Responds and returns false unless the device is open and, when it has a PIN, [token] belongs to a signed-in device. */
    private suspend fun admitDevice(call: ApplicationCall, token: String?, nowMs: Long): Boolean {
        val device = store.device.value
        return when {
            !deviceOpen(nowMs) -> { call.respond(HttpStatusCode.Gone); false }
            device.pin != null && signedInClient(token, nowMs) == null -> {
                call.response.header(AUTH_HEADER, AUTH_DEVICE)
                // A token we issued that no longer works means the owner logged this device out.
                if (tokens.genuine(token)) call.response.header(AUTH_REASON_HEADER, AUTH_REASON_LOGGED_OUT)
                call.respond(HttpStatusCode.Unauthorized)
                false
            }
            else -> true
        }
    }

    /** Responds and returns false unless [share] is open and, when protected, [token] unlocks it. */
    private suspend fun admitShare(call: ApplicationCall, share: Share?, token: String?, nowMs: Long): Boolean {
        when {
            share == null || share.revoked -> call.respond(HttpStatusCode.NotFound)
            share.isExpired(nowMs) -> call.respond(HttpStatusCode.Gone)
            !share.isOpen(nowMs) -> call.respond(HttpStatusCode.Forbidden)
            !canOpen(share, token, nowMs) -> {
                call.response.header(AUTH_HEADER, AUTH_SHARE)
                if (tokens.genuine(token)) call.response.header(AUTH_REASON_HEADER, AUTH_REASON_LOGGED_OUT)
                call.respond(HttpStatusCode.Unauthorized)
            }
            else -> return true
        }
        return false
    }

    /** Public, or [token] opens this share for a device that is still signed in (or never signed in, on a device without a PIN). */
    private fun canOpen(share: Share, token: String?, nowMs: Long): Boolean {
        if (share.access == ShareAccess.Public) return true
        val device = store.device.value
        val scope = tokens.read(token, device.sessionEpoch, nowMs)?.takeIf { it.startsWith("share:${share.id}:") } ?: return false
        val clientId = AccessTokens.clientOf(scope)
        return clientId in device.sessions || (clientId.isEmpty() && device.pin == null)
    }

    private fun limited(call: ApplicationCall, nowMs: Long): Boolean {
        val f = failures[call.request.origin.remoteHost]?.takeIf { nowMs - it.windowStartMs < FAILURE_WINDOW_MS }
        return f != null && f.count >= MAX_FAILURES
    }

    private fun failed(call: ApplicationCall, nowMs: Long) {
        failures.compute(call.request.origin.remoteHost) { _, old ->
            if (old == null || nowMs - old.windowStartMs >= FAILURE_WINDOW_MS) Failures(1, nowMs) else old.copy(count = old.count + 1)
        }
    }

    private fun Share.toRemote(withItems: Boolean) = RemoteShare(
        id = id, title = title, locked = access == ShareAccess.Protected, itemCount = items.size, expiresAtMs = expiresAtMs, folder = isFolder, audio = items.isNotEmpty() && items.all { it.isAudio },
        items = if (withItems) items.map { RemoteItem(it.id, it.name, it.sizeBytes, it.durationMs, it.mimeType, it.isAudio, "/media/$id/${it.id}/${it.name.encodeURLPathPart()}", it.width, it.height) } else emptyList(),
    )

    /** Compares in constant time so response timing does not reveal how many leading digits were right. */
    private fun samePin(expected: String?, given: String): Boolean =
        expected != null && MessageDigest.isEqual(expected.toByteArray(), given.trim().toByteArray())

    private companion object {
        const val MAX_FAILURES = 5
        const val FAILURE_WINDOW_MS = 60_000L
    }
}

/**
 * A file streamed from a [FileChannel]. Each response opens its own channel, so parallel requests (a player
 * probing the end of the file while reading the start) never share a file position.
 */
private class ChannelContent(
    private val scope: CoroutineScope,
    private val open: () -> FileChannel?,
    override val contentLength: Long,
    override val contentType: ContentType,
) : OutgoingContent.ReadChannelContent() {
    override fun readFrom(): ByteReadChannel = readFrom(0 until contentLength)

    override fun readFrom(range: LongRange): ByteReadChannel = scope.writer(Dispatchers.IO) {
        val file = open() ?: return@writer
        file.use { ch ->
            ch.position(range.first)
            var remaining = range.last - range.first + 1
            val buf = ByteBuffer.allocate(64 * 1024)
            while (remaining > 0) {
                buf.clear()
                if (remaining < buf.capacity()) buf.limit(remaining.toInt())
                val n = ch.read(buf)
                if (n < 0) break
                buf.flip()
                channel.writeFully(buf)
                remaining -= n
            }
        }
    }.channel
}
