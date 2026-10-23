package org.app.maxplayerpro.localnetwork.share

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.app.kmediaplayer.model.MediaFile
import org.app.kmediaplayer.settings.MemoryStore
import org.app.kmediaplayer.util.nowMs
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.test.assertFalse
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KtorShareHostTest {
    private val bytes = Random(7).nextBytes(3_000_000)
    private lateinit var file: File
    private lateinit var store: ShareStore
    private lateinit var host: KtorShareHost
    private lateinit var base: String
    private val http = HttpClient.newHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    private val platform = object : HostPlatform {
        override val deviceName = "Test host"
        override val platform = "Test"
        override fun open(item: SharedItem): FileChannel? = runCatching { FileChannel.open(File(item.path).toPath(), StandardOpenOption.READ) }.getOrNull()
        override fun onHostingChanged(running: Boolean) {}
        override fun acquireMulticast() = AutoCloseable {}
    }

    @BeforeTest
    fun setUp() = runBlocking {
        file = File.createTempFile("share", ".mp4").apply { writeBytes(bytes); deleteOnExit() }
        store = ShareStore(MemoryStore())
        host = KtorShareHost(store, platform)
        host.start()
        val running = host.state.value as HostState.Running
        base = "http://127.0.0.1:${running.port}"
    }

    @AfterTest
    fun tearDown() = runBlocking { host.stop() }

    private fun media() = MediaFile(1, "file://${file.path}", file.path, "clip one.mp4", bytes.size.toLong(), 1000, 0, mimeType = "video/mp4")

    private fun get(path: String, range: String? = null, deviceToken: String? = null): HttpResponse<ByteArray> {
        val req = HttpRequest.newBuilder(URI(base + path)).apply { range?.let { header("Range", it) }; deviceToken?.let { header(DEVICE_TOKEN_HEADER, it) } }.GET().build()
        return http.send(req, HttpResponse.BodyHandlers.ofByteArray())
    }

    private fun post(path: String, body: String): HttpResponse<String> =
        http.send(HttpRequest.newBuilder(URI(base + path)).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build(),
            HttpResponse.BodyHandlers.ofString())

    private fun shares(): List<RemoteShare> = json.decodeFromString(String(get("/api/v1/shares").body()))

    @Test
    fun publicShareStreamsWholeFileAndEveryRangeForm() = runBlocking {
        store.save("Clip", listOf(media()), null, ShareAccess.Public, null, nowMs(), null, null)
        val item = shares().single().items.single()
        assertEquals("/media/${shares().single().id}/0/clip%20one.mp4", item.path)

        val full = get(item.path)
        assertEquals(200, full.statusCode())
        assertEquals("bytes", full.headers().firstValue("Accept-Ranges").orElse(""))
        assertContentEquals(bytes, full.body())

        val mid = get(item.path, "bytes=1000-1999")
        assertEquals(206, mid.statusCode())
        assertEquals("bytes 1000-1999/${bytes.size}", mid.headers().firstValue("Content-Range").get())
        assertContentEquals(bytes.copyOfRange(1000, 2000), mid.body())

        // Open-ended and suffix ranges are what players send when seeking and probing the end of the file.
        val open = get(item.path, "bytes=2999000-")
        assertEquals(206, open.statusCode())
        assertContentEquals(bytes.copyOfRange(2_999_000, bytes.size), open.body())
        val suffix = get(item.path, "bytes=-500")
        assertContentEquals(bytes.copyOfRange(bytes.size - 500, bytes.size), suffix.body())

        assertEquals(416, get(item.path, "bytes=9000000-").statusCode())
    }

    @Test
    fun parallelRangeRequestsDoNotInterfere() = runBlocking {
        store.save("Clip", listOf(media()), null, ShareAccess.Public, null, nowMs(), null, null)
        val path = shares().single().items.single().path
        val starts = (0 until 24).map { it * 100_000 }
        val results = starts.map { s -> async(kotlinx.coroutines.Dispatchers.IO) { s to get(path, "bytes=$s-${s + 65_535}").body() } }.awaitAll()
        results.forEach { (s, body) -> assertContentEquals(bytes.copyOfRange(s, s + 65_536), body) }
    }

    @Test
    fun codedShareNeedsCorrectCodeAndToken() = runBlocking {
        val share = store.save("Secret", listOf(media()), null, ShareAccess.Protected, "424242", nowMs(), null, null)
        val listed = shares().single()
        assertTrue(listed.locked)
        assertTrue(listed.items.isEmpty())

        val path = "/media/${share.id}/0/x.mp4"
        assertEquals(401, get(path).statusCode())
        assertEquals(401, get("$path?t=forged").statusCode())
        assertEquals(401, post("/api/v1/shares/${share.id}/unlock", """{"code":"000000"}""").statusCode())

        val ok = post("/api/v1/shares/${share.id}/unlock", """{"code":"424242"}""")
        assertEquals(200, ok.statusCode())
        val unlocked = json.decodeFromString<UnlockResponse>(ok.body())
        assertEquals(1, unlocked.share.items.size)
        val stream = get(unlocked.share.items.single().path + "?t=" + unlocked.token, "bytes=0-99")
        assertEquals(206, stream.statusCode())
        assertContentEquals(bytes.copyOfRange(0, 100), stream.body())

        // Revoking takes effect on the very next request, even with a valid token.
        store.revoke(share.id)
        assertEquals(404, get(unlocked.share.items.single().path + "?t=" + unlocked.token).statusCode())
        assertTrue(shares().isEmpty())
    }

    @Test
    fun wrongCodesAreRateLimited() = runBlocking {
        val share = store.save("Secret", listOf(media()), null, ShareAccess.Protected, "424242", nowMs(), null, null)
        repeat(5) { assertEquals(401, post("/api/v1/shares/${share.id}/unlock", """{"code":"11111$it"}""").statusCode()) }
        // Even the right code is refused until the window passes, so guessing 10^6 codes is impractical.
        assertEquals(429, post("/api/v1/shares/${share.id}/unlock", """{"code":"424242"}""").statusCode())
    }

    @Test
    fun expiredAndNotYetOpenSharesAreClosed() = runBlocking {
        val now = nowMs()
        val expired = store.save("Old", listOf(media()), null, ShareAccess.Public, null, now - 10_000, null, now - 1)
        val later = store.save("Later", listOf(media().copy(uri = "file:///later")), null, ShareAccess.Public, null, now, now + 3_600_000, null)
        assertTrue(shares().isEmpty())
        assertEquals(410, get("/media/${expired.id}/0/x.mp4").statusCode())
        assertEquals(403, get("/media/${later.id}/0/x.mp4").statusCode())
        assertEquals(410, post("/api/v1/shares/${expired.id}/unlock", """{"code":""}""").statusCode())
    }

    @Test
    fun startAndStopAreIdempotentUnderConcurrency() = runBlocking {
        (1..8).map { async { if (it % 2 == 0) host.start() else host.stop() } }.awaitAll()
        host.stop(); host.stop()
        assertEquals(HostState.Stopped, host.state.value)
        host.start(); host.start()
        assertTrue(host.state.value is HostState.Running)
    }

    @Test
    fun devicePinGuardsEverythingAndTokensSurviveRestart() = runBlocking {
        val share = store.save("Clip", listOf(media()), null, ShareAccess.Public, null, nowMs(), null, null)
        store.setDevicePin("2468")
        val info = json.decodeFromString<DeviceInfo>(String(get("/api/v1/info").body()))
        assertTrue(info.pinRequired)
        val locked = get("/api/v1/shares")
        assertEquals(401, locked.statusCode())
        assertEquals(AUTH_DEVICE, locked.headers().firstValue(AUTH_HEADER).get())
        assertEquals(401, get("/media/${share.id}/0/x.mp4").statusCode())
        assertEquals(401, post("/api/v1/login", """{"pin":"1111"}""").statusCode())

        val token = json.decodeFromString<LoginResponse>(post("/api/v1/login", """{"pin":"2468"}""").body()).token
        assertEquals(1, json.decodeFromString<List<RemoteShare>>(String(get("/api/v1/shares", deviceToken = token).body())).size)
        assertEquals(206, get("/media/${share.id}/0/x.mp4?d=$token", "bytes=0-9").statusCode())

        // Tokens are signed, not kept in memory: a restarted server still accepts them.
        host.stop()
        host = KtorShareHost(store, platform)
        host.start()
        base = "http://127.0.0.1:${(host.state.value as HostState.Running).port}"
        assertEquals(200, get("/api/v1/shares", deviceToken = token).statusCode())

        // "Log out all devices" voids every token handed out so far.
        store.logOutAllDevices()
        assertEquals(401, get("/api/v1/shares", deviceToken = token).statusCode())
    }

    @Test
    fun shareTokensStopWorkingAfterLogOutAll() = runBlocking {
        val share = store.save("Secret", listOf(media()), null, ShareAccess.Protected, "9090", nowMs(), null, null)
        val unlocked = json.decodeFromString<UnlockResponse>(post("/api/v1/shares/${share.id}/unlock", """{"code":"9090"}""").body())
        val req = { t: String -> http.send(HttpRequest.newBuilder(URI("$base/api/v1/shares/${share.id}")).header(SHARE_TOKEN_HEADER, t).GET().build(), HttpResponse.BodyHandlers.ofString()) }
        assertEquals(200, req(unlocked.token).statusCode())
        store.logOutAllDevices()
        val after = req(unlocked.token)
        assertEquals(401, after.statusCode())
        assertEquals(AUTH_SHARE, after.headers().firstValue(AUTH_HEADER).get())
    }

    @Test
    fun deviceTimeLimitClosesEverything() = runBlocking {
        val share = store.save("Clip", listOf(media()), null, ShareAccess.Public, null, nowMs(), null, null)
        store.setDeviceUntil(nowMs() - 1)
        assertEquals(410, get("/api/v1/shares").statusCode())
        assertEquals(410, get("/media/${share.id}/0/x.mp4").statusCode())
        assertEquals(410, post("/api/v1/login", """{"pin":""}""").statusCode())
    }

    @Test
    fun resharingAFolderKeepsItsIdAndOnlyItsSnapshot() = runBlocking {
        val first = store.save("Movies", listOf(media()), "/videos", ShareAccess.Protected, "1234", nowMs(), null, null)
        val second = store.save("Movies", listOf(media(), media().copy(uri = "file:///other", name = "b.mp4")), "/videos", ShareAccess.Public, null, nowMs(), null, null)
        assertEquals(first.id, second.id)
        assertEquals(1, store.shares.value.size)
        assertEquals(2, store.findFor(emptyList(), "/videos")!!.items.size)
        assertTrue(shares().single().folder)
        assertFalse(shares().single().locked)
        store.delete(first.id)
        assertTrue(shares().isEmpty())
    }

    @Test
    fun clientNormalizesAddresses() {
        assertEquals("http://192.168.1.5:$DEFAULT_SHARE_PORT", NearbyClient.normalizeAddress(" 192.168.1.5/ "))
        assertEquals("http://192.168.1.5:8080", NearbyClient.normalizeAddress("192.168.1.5:8080"))
        assertEquals("http://host:1", NearbyClient.normalizeAddress("http://host:1"))
        assertEquals(null, NearbyClient.normalizeAddress("  "))
    }
}
