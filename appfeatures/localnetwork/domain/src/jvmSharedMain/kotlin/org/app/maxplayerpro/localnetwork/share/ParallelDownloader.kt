package org.app.maxplayerpro.localnetwork.share

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
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
    /** [subdir] (`a/b`) is created under [location] when missing. */
    fun create(name: String, mimeType: String, location: SaveLocation, subdir: String = ""): DownloadFile
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

    override fun start(url: String, name: String, sizeBytes: Long, mimeType: String, location: SaveLocation): String =
        launchTask(DownloadTask(ShareStore.newId(), name, sizeBytes)) { id, done ->
            val where = save(url, name, "", sizeBytes, mimeType, location, done)
            edit(id) { it.copy(doneBytes = sizeBytes, state = DownloadState.Done, detail = where) }
        }

    override fun startFolder(name: String, files: List<FolderFile>, location: SaveLocation): String =
        launchTask(DownloadTask(ShareStore.newId(), name, files.sumOf { it.sizeBytes }, files = files.size)) { id, done ->
            // One file at a time (each over several connections), so a big folder doesn't open hundreds of sockets.
            var where = ""
            files.forEachIndexed { i, f ->
                where = save(f.url, f.name, listOf(name, f.dir).filter { it.isNotEmpty() }.joinToString("/"), f.sizeBytes, f.mimeType, location, done)
                edit(id) { it.copy(doneFiles = i + 1) }
            }
            // The saved folder itself: the last file's place, cut back to the folder's name.
            val folder = where.substringBefore("/$name/", where.substringBeforeLast('/')) + "/$name/"
            edit(id) { it.copy(doneBytes = it.sizeBytes, state = DownloadState.Done, detail = folder) }
        }

    /** Adds [task] to the list and runs [work] for it, with progress ticks and failure/cancel handling. */
    private fun launchTask(task: DownloadTask, work: suspend CoroutineScope.(String, AtomicLong) -> Unit): String {
        val id = task.id
        _tasks.update { it + task }
        jobs[id] = scope.launch {
            val done = AtomicLong(0)
            val ticker = launch { while (isActive) { delay(250); edit(id) { it.copy(doneBytes = done.get()) } } }
            try {
                work(id, done)
            } catch (e: Throwable) {
                val cancelled = !coroutineContext.isActive
                edit(id) { it.copy(state = if (cancelled) DownloadState.Cancelled else DownloadState.Failed, detail = if (cancelled) null else (e.message ?: "Download failed")) }
                if (cancelled) throw e
            } finally {
                ticker.cancel()
                jobs.remove(id)
            }
        }
        return id
    }

    /** Downloads one file into [subdir] of [location]; returns where it was saved. */
    private suspend fun save(url: String, name: String, subdir: String, sizeBytes: Long, mimeType: String, location: SaveLocation, done: AtomicLong): String {
        val file = runCatching { sink.create(name, mimeType, location, subdir) }.getOrElse { e -> error("Can't save here: ${e.message}") }
        try {
            coroutineScope {
                val parts = if (sizeBytes >= MIN_SPLIT) PARTS else 1
                val step = (sizeBytes + parts - 1) / parts
                (0 until parts).map { i ->
                    val from = i * step
                    val to = maxOf(from, minOf(sizeBytes, from + step) - 1)
                    async { if (sizeBytes > 0) fetchRange(url, from, to, file.channel, done) }
                }.awaitAll()
            }
            file.channel.close()
            return file.finish()
        } catch (e: Throwable) {
            runCatching { file.channel.close() }
            file.abort()
            throw e
        }
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
