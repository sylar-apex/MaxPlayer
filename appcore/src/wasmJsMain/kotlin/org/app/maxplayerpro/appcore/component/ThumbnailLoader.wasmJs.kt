package org.app.maxplayerpro.appcore.component

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.browser.document
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import org.app.kmediaplayer.model.MediaFile
import org.app.maxplayerpro.sharedlogic.data.ThumbnailCache
import org.jetbrains.skia.Image
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLVideoElement
import org.w3c.dom.CanvasRenderingContext2D
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/** Browser thumbnails: a hidden video element seeks to ~12% and one frame is drawn onto a canvas. */
actual object ThumbnailLoader : ThumbnailCache {
    private val lock = Mutex()
    private val memory = mutableMapOf<String, ImageBitmap>()
    private val failed = mutableSetOf<String>()

    private val video by lazy {
        (document.createElement("video") as HTMLVideoElement).apply { muted = true; preload = "metadata"; setAttribute("playsinline", "") }
    }
    private val canvas by lazy { document.createElement("canvas") as HTMLCanvasElement }

    actual suspend fun load(file: MediaFile, px: Int): ImageBitmap? {
        if (file.isAudio || file.uri in failed) return null
        memory[file.uri]?.let { return it }
        val bitmap = lock.withLock { memory[file.uri] ?: grab(file.uri, px) }
        if (bitmap == null) failed += file.uri else memory[file.uri] = bitmap
        return bitmap
    }

    @OptIn(ExperimentalEncodingApi::class)
    private suspend fun grab(uri: String, px: Int): ImageBitmap? {
        val ready = CompletableDeferred<Boolean>()
        video.onloadedmetadata = { video.currentTime = (video.duration.takeIf { it.isFinite() } ?: 10.0) * 0.12; null }
        video.onseeked = { ready.complete(true); null }
        video.onerror = { _, _, _, _, _ -> ready.complete(false); null }
        video.src = uri
        val ok = withTimeoutOrNull(5_000) { ready.await() } ?: false
        if (!ok || video.videoWidth == 0) return null
        val w = minOf(px, video.videoWidth)
        val h = video.videoHeight * w / video.videoWidth
        canvas.width = w; canvas.height = h
        (canvas.getContext("2d") as CanvasRenderingContext2D).drawImage(video, 0.0, 0.0, w.toDouble(), h.toDouble())
        // Cross-site videos without CORS cannot be read back; they keep the placeholder icon.
        val data = runCatching { canvas.toDataURL("image/jpeg", 0.8.toJsNumber()).substringAfter("base64,") }.getOrNull()
        video.removeAttribute("src")
        data ?: return null
        return runCatching { Image.makeFromEncoded(Base64.decode(data)).toComposeImageBitmap() }.getOrNull()
    }

    actual override fun clear() { memory.clear(); failed.clear() }
}
