package org.app.maxplayerpro.localnetwork.share

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.coroutineContext

/** A file being written by a download; positional writes, so parts can land in any order. */
interface DownloadFile {
    val channel: FileChannel
    /** Makes the file visible where it was saved and returns where that is, for the user. */
    fun finish(): String
    /** Removes the partial file. */
    fun abort()
}

/** Where downloads are written on this platform. */
interface DownloadSink {
    val defaultLabel: String
    fun create(name: String, mimeType: String, location: SaveLocation): DownloadFile
}

/**
 * Downloads with several connections at once: a large file is split into [PARTS] byte ranges fetched in parallel
 * and written straight to their place in the file, which fills a fast Wi-Fi link that one connection can't.
 * A part that drops retries from where it stopped.
 */
class ParallelDownloader(private val sink: DownloadSink) : Downloader {
    override val supported = true
    override val defaultLabel: String get() = sink.defaultLabel
    private val _tasks = MutableStateFlow<List<DownloadTask>>(emptyList())
    override val tasks: StateFlow<List<DownloadTask>> = _tasks.asStateFlow()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val jobs = ConcurrentHashMap<String, Job>()

    override fun start(url: String, name: String, sizeBytes: Long, mimeType: String, location: SaveLocation): String {
        val id = ShareStore.newId()
        val task = DownloadTask(id, name, sizeBytes)
        _tasks.update { it + task }
        jobs[id] = scope.launch {
            val file = runCatching { sink.create(name, mimeType, location) }.getOrElse { e ->
                edit(id) { it.copy(state = DownloadState.Failed, detail = "Can't save here: ${e.message}") }
                return@launch
            }
            val done = AtomicLong(0)
            val ticker = launch { while (isActive) { delay(250); edit(id) { it.copy(doneBytes = done.get()) } } }
            try {
                val parts = if (sizeBytes >= MIN_SPLIT) PARTS else 1
                val step = (sizeBytes + parts - 1) / parts
                (0 until parts).map { i ->
                    val from = i * step
                    val to = minOf(sizeBytes, from + step) - 1
                    async { fetchRange(url, from, to, file.channel, done) }
                }.awaitAll()
                ticker.cancel()
                file.channel.close()
                val where = file.finish()
                edit(id) { it.copy(doneBytes = sizeBytes, state = DownloadState.Done, detail = where) }
            } catch (e: Throwable) {
                ticker.cancel()
                runCatching { file.channel.close() }
                file.abort()
                val cancelled = !coroutineContext.isActive
                edit(id) { it.copy(state = if (cancelled) DownloadState.Cancelled else DownloadState.Failed, detail = if (cancelled) null else (e.message ?: "Download failed")) }
                if (cancelled) throw e
            } finally {
                jobs.remove(id)
            }
        }
        return id
    }

    override fun cancel(id: String) { jobs[id]?.cancel() }

    override fun clearFinished() = _tasks.update { list -> list.filter { it.state == DownloadState.Running } }

    /** Writes bytes [from]..[to] of [url] at the same offsets, resuming after a dropped connection (up to [RETRIES] times). */
    private suspend fun fetchRange(url: String, from: Long, to: Long, channel: FileChannel, done: AtomicLong) {
        var pos = from
        var attempts = 0
        while (pos <= to) {
            coroutineContext.ensureActive()
            try {
                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8_000; readTimeout = 20_000
                    setRequestProperty("Range", "bytes=$pos-$to")
                }
                try {
                    val code = conn.responseCode
                    if (code != 206 && !(code == 200 && pos == 0L)) error(httpReason(code))
                    conn.inputStream.use { input ->
                        val buf = ByteArray(BUFFER)
                        while (pos <= to) {
                            coroutineContext.ensureActive()
                            val n = input.read(buf, 0, minOf(BUFFER.toLong(), to - pos + 1).toInt())
                            if (n < 0) break
                            var written = 0
                            val bb = ByteBuffer.wrap(buf, 0, n)
                            while (bb.hasRemaining()) written += channel.write(bb, pos + written)
                            pos += n
                            done.addAndGet(n.toLong())
                        }
                    }
                } finally {
                    conn.disconnect()
                }
            } catch (e: java.io.IOException) {
                if (++attempts > RETRIES) throw e
                delay(500L * attempts)
            }
        }
    }

    private fun httpReason(code: Int) = when (code) {
        401 -> "Access was revoked; open the share again"
        403 -> "The owner doesn't allow downloading this"
        404 -> "No longer shared"
        410 -> "Sharing time is over"
        else -> "Device answered $code"
    }

    private fun edit(id: String, change: (DownloadTask) -> DownloadTask) = _tasks.update { list -> list.map { if (it.id == id) change(it) else it } }

    private companion object {
        const val PARTS = 4
        const val MIN_SPLIT = 8L * 1024 * 1024
        const val BUFFER = 256 * 1024
        const val RETRIES = 4
    }
}
