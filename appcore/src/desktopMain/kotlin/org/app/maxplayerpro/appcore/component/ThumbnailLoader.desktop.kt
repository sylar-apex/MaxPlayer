package org.app.maxplayerpro.appcore.component

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.app.kmediaplayer.desktop.VlcThumbnailer
import org.app.kmediaplayer.model.MediaFile
import org.app.maxplayerpro.sharedlogic.data.ThumbnailCache
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import java.io.File

/**
 * Desktop thumbnails: a frame grabbed by libVLC, kept in memory and as a JPEG on disk under
 * `~/.maxplayer/thumbs`, keyed by path, size and modification time so edited files refresh.
 */
actual object ThumbnailLoader : ThumbnailCache {
    private val dir = File(System.getProperty("user.home"), ".maxplayer/thumbs").apply { mkdirs() }
    private val memory = object : LinkedHashMap<String, ImageBitmap>(256, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ImageBitmap>) = size > 400
    }
    /** Files that failed once; not retried until the cache is cleared. */
    private val failed = mutableSetOf<String>()

    actual suspend fun load(file: MediaFile, px: Int): ImageBitmap? {
        if (file.isAudio || !VlcThumbnailer.isAvailable) return null
        val key = "${file.path}|${file.sizeBytes}|${file.dateModifiedMs}|$px".hashCode().toUInt().toString(16)
        synchronized(memory) { memory[key] }?.let { return it }
        if (key in failed) return null
        val cached = File(dir, "$key.jpg")
        val image = withContext(Dispatchers.IO) {
            if (cached.exists()) runCatching { Image.makeFromEncoded(cached.readBytes()) }.getOrNull()
            else grab(file, px)?.also { img -> img.encodeToData(EncodedImageFormat.JPEG, 82)?.bytes?.let(cached::writeBytes) }
        }
        val bitmap = image?.toComposeImageBitmap()
        if (bitmap == null) failed += key else synchronized(memory) { memory[key] = bitmap }
        return bitmap
    }

    private suspend fun grab(file: MediaFile, px: Int): Image? {
        val frame = VlcThumbnailer.frame(file.path, px) ?: return null
        return Image.makeRaster(ImageInfo(frame.width, frame.height, ColorType.BGRA_8888, ColorAlphaType.OPAQUE), frame.pixels, frame.width * 4)
    }

    actual override fun clear() {
        synchronized(memory) { memory.clear() }
        failed.clear()
        dir.listFiles()?.forEach { it.delete() }
    }
}
