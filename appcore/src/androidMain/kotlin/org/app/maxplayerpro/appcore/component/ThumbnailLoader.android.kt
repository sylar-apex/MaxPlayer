package org.app.maxplayerpro.appcore.component

import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.LruCache
import android.util.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.app.kmediaplayer.KMediaPlayer
import org.app.kmediaplayer.android.KMediaPlayerAndroid
import org.app.kmediaplayer.model.MediaFile
import org.app.maxplayerpro.sharedlogic.data.ThumbnailCache
import org.app.maxplayerpro.sharedlogic.settings.AppKeys

actual object ThumbnailLoader : ThumbnailCache {
    private val cache = object : LruCache<Long, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: Long, value: Bitmap) = value.byteCount
    }

    actual suspend fun load(file: MediaFile, px: Int): ImageBitmap? = withContext(Dispatchers.IO) {
        cache.get(file.id)?.let { return@withContext it.asImageBitmap() }
        val resolver = KMediaPlayerAndroid.context.contentResolver
        val bmp = runCatching {
            if (Build.VERSION.SDK_INT >= 29) resolver.loadThumbnail(Uri.parse(file.uri), Size(px, px * 9 / 16), null)
            else @Suppress("DEPRECATION") MediaStore.Video.Thumbnails.getThumbnail(resolver, file.id, MediaStore.Video.Thumbnails.MINI_KIND, null)
        }.getOrNull()
        if (bmp != null && KMediaPlayer.settings[AppKeys.cacheThumbnail]) cache.put(file.id, bmp)
        bmp?.asImageBitmap()
    }

    actual override fun clear() = cache.evictAll()
}
