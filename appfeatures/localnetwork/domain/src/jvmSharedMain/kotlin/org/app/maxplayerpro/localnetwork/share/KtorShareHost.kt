package org.app.maxplayerpro.localnetwork.share

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.encodeURLPathPart
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.autohead.AutoHeadResponse
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.origin
import io.ktor.server.plugins.partialcontent.PartialContent
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
 * request handlers only read immutable snapshots, so a share revoked mid-stream stops at the next request.
 */
class KtorShareHost(private val store: ShareStore, private val platform: HostPlatform) : ShareHost {
    override val supported = true
    private val _state = MutableStateFlow<HostState>(HostState.Stopped)
    override val state: StateFlow<HostState> = _state.asStateFlow()

    private val lifecycle = Mutex()
    private var server: EmbeddedServer<*, *>? = null
    private var ioScope: CoroutineScope? = null
    private val tokens = ConcurrentHashMap<String, Grant>()
    private val failures = ConcurrentHashMap<String, Failures>()

    private data class Grant(val shareId: String, val expiresAtMs: Long)
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
        tokens.clear()
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
            exposeHeader(HttpHeaders.ContentRange)
            exposeHeader(HttpHeaders.AcceptRanges)
        }
        routing {
            get("/") { call.respondText(WEB_PAGE, ContentType.Text.Html) }
            get("/api/v1/info") {
                call.respond(DeviceInfo(store.deviceId, platform.deviceName, platform.platform, call.request.local.localPort))
            }
            get("/api/v1/shares") {
                val now = nowMs()
                call.respond(store.shares.value.filter { it.isOpen(now) }.map { it.toRemote(withItems = it.access == ShareAccess.Public) })
            }
            post("/api/v1/shares/{id}/unlock") {
                val client = call.request.origin.remoteHost
                val now = nowMs()
                val f = failures[client]?.takeIf { now - it.windowStartMs < FAILURE_WINDOW_MS }
                if (f != null && f.count >= MAX_FAILURES) return@post call.respond(HttpStatusCode.TooManyRequests)
                val share = store.find(call.parameters["id"].orEmpty())
                when {
                    share == null || share.revoked -> call.respond(HttpStatusCode.NotFound)
                    share.isExpired(now) -> call.respond(HttpStatusCode.Gone)
                    !share.isOpen(now) -> call.respond(HttpStatusCode.Forbidden)
                    share.access == ShareAccess.Public -> call.respond(UnlockResponse("", share.expiresAtMs ?: Long.MAX_VALUE, share.toRemote(true)))
                    !sameCode(share.code, runCatching { call.receive<UnlockRequest>().code }.getOrDefault("")) -> {
                        failures.compute(client) { _, old ->
                            if (old == null || now - old.windowStartMs >= FAILURE_WINDOW_MS) Failures(1, now) else old.copy(count = old.count + 1)
                        }
                        call.respond(HttpStatusCode.Unauthorized)
                    }
                    else -> {
                        failures.remove(client)
                        tokens.entries.removeIf { it.value.expiresAtMs <= now }
                        val expires = minOf(share.expiresAtMs ?: Long.MAX_VALUE, now + TOKEN_LIFETIME_MS)
                        val token = ShareStore.newId()
                        tokens[token] = Grant(share.id, expires)
                        call.respond(UnlockResponse(token, expires, share.toRemote(true)))
                    }
                }
            }
            get("/media/{share}/{item}/{name}") {
                val now = nowMs()
                val share = store.find(call.parameters["share"].orEmpty())
                val item = share?.items?.firstOrNull { it.id == call.parameters["item"] }
                val grant = call.request.queryParameters["t"]?.let { tokens[it] }
                when {
                    share == null || item == null || share.revoked -> call.respond(HttpStatusCode.NotFound)
                    share.isExpired(now) -> call.respond(HttpStatusCode.Gone)
                    !share.isOpen(now) -> call.respond(HttpStatusCode.Forbidden)
                    share.access == ShareAccess.Code && (grant == null || grant.shareId != share.id || grant.expiresAtMs <= now) ->
                        call.respond(HttpStatusCode.Unauthorized)
                    else -> {
                        val size = platform.open(item)?.use { it.size() } ?: return@get call.respond(HttpStatusCode.NotFound)
                        call.response.header(HttpHeaders.CacheControl, "no-store")
                        call.respond(ChannelContent(scope, { platform.open(item) }, size, ContentType.parse(item.mimeType.ifEmpty { "application/octet-stream" })))
                    }
                }
            }
        }
    }

    private fun Share.toRemote(withItems: Boolean) = RemoteShare(
        id = id, title = title, locked = access == ShareAccess.Code, itemCount = items.size, expiresAtMs = expiresAtMs,
        items = if (withItems) items.map { RemoteItem(it.id, it.name, it.sizeBytes, it.durationMs, it.mimeType, it.isAudio, "/media/$id/${it.id}/${it.name.encodeURLPathPart()}", it.width, it.height) } else emptyList(),
    )

    /** Compares in constant time so response timing does not reveal how many leading digits were right. */
    private fun sameCode(expected: String?, given: String): Boolean =
        expected != null && MessageDigest.isEqual(expected.toByteArray(), given.trim().toByteArray())

    private companion object {
        const val MAX_FAILURES = 5
        const val FAILURE_WINDOW_MS = 60_000L
        const val TOKEN_LIFETIME_MS = 12 * 3_600_000L
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
