package org.app.maxplayerpro.localnetwork.share

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * Small JPEG thumbnails of shared photos and videos, made only when needed and kept on disk under [dir] up to
 * [maxBytes] (oldest dropped first). Nothing is made for folders nobody opened: a share warms only its top level,
 * and each subfolder warms when a viewer opens it ([warm]); anything scrolled to sooner is made on request.
 */
internal class ThumbCache(
    private val dir: File,
    private val platform: HostPlatform,
    private val maxBytes: Long = 100L * 1024 * 1024,
) {
    /** A few at once: a grid asks for many together, but decoding is CPU- and disk-heavy. */
    private val slots = Semaphore(3)
    /** Thumbnails being made, so a request and the warm-up don't make the same one twice. */
    private val inFlight = ConcurrentHashMap<String, CompletableDeferred<ByteArray?>>()
    /** Files that gave no thumbnail this run; asked again they get none at once instead of another slow try. */
    private val failed = ConcurrentHashMap.newKeySet<String>()
    private val queue = Channel<Pair<Share, String>>(Channel.UNLIMITED)
    private val written = java.util.concurrent.atomic.AtomicInteger()

    /** Thumbnails in the background, one folder level at a time, until [scope] ends. */
    fun startWarming(scope: CoroutineScope) {
        scope.launch {
            for ((share, folder) in queue) {
                // In parallel; [slots] keeps it to a few at once.
                coroutineScope {
                    share.items.filter { it.dir == folder && hasThumb(it) }.forEach { launch { runCatching { get(it) } } }
                }
            }
        }
    }

    /** Makes thumbnails for the files directly in [folder] ("" for the top) of [share], in the background. */
    fun warm(share: Share, folder: String) { queue.trySend(share to folder) }

    /** The thumbnail of [item], made now if it isn't on disk yet; null when it can't be made. */
    suspend fun get(item: SharedItem): ByteArray? {
        val key = key(item)
        val file = File(dir, "$key.jpg")
        if (file.exists()) return file.readBytes().also { file.setLastModified(System.currentTimeMillis()) }
        if (key in failed) return null
        val mine = CompletableDeferred<ByteArray?>()
        inFlight.putIfAbsent(key, mine)?.let { return it.await() }
        try {
            val bytes = slots.withPermit { runCatching { platform.thumbnail(item, THUMB_PX) }.getOrNull() }
            if (bytes == null) failed += key
            if (bytes != null) {
                dir.mkdirs()
                file.writeBytes(bytes)
                if (written.incrementAndGet() % 50 == 0) trim()
            }
            mine.complete(bytes)
            return bytes
        } catch (e: Throwable) {
            mine.complete(null)
            throw e
        } finally {
            inFlight.remove(key)
        }
    }

    /** Drops the least recently used thumbnails until the folder is under [maxBytes]. */
    private fun trim() {
        val files = dir.listFiles()?.sortedBy { it.lastModified() } ?: return
        var total = files.sumOf { it.length() }
        for (f in files) {
            if (total <= maxBytes) break
            total -= f.length()
            f.delete()
        }
    }

    /** Changes when the file does (size), so an edited photo gets a new thumbnail. */
    private fun key(item: SharedItem): String {
        val raw = "${item.uri}|${item.sizeBytes}|$THUMB_PX"
        return MessageDigest.getInstance("SHA-1").digest(raw.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    companion object {
        /** Long side of a thumbnail: sharp in a list or grid cell, and a quick blurred stand-in full screen. */
        const val THUMB_PX = 320

        fun hasThumb(item: SharedItem) = item.mimeType.startsWith("image/") || item.mimeType.startsWith("video/")
    }
}
