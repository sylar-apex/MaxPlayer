package org.app.maxplayerpro.localnetwork.share

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.Settings
import org.koin.core.module.Module
import java.io.File
import java.io.FileInputStream
import java.nio.channels.FileChannel

private class AndroidHostPlatform(private val context: Context) : HostPlatform {
    override val deviceName: String by lazy {
        runCatching { Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME) }.getOrNull()?.takeIf { it.isNotBlank() }
            ?: "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"
    }
    override val platform: String = "Android"

    /** Library files come as content:// URIs; private-folder files as plain paths. */
    override fun open(item: SharedItem): FileChannel? = runCatching {
        if (item.uri.startsWith("content:")) {
            val pfd = context.contentResolver.openFileDescriptor(Uri.parse(item.uri), "r") ?: return null
            // Closing the stream closes the descriptor too.
            ParcelFileDescriptor.AutoCloseInputStream(pfd).channel
        } else {
            FileInputStream(File(item.path)).channel
        }
    }.getOrNull()

    override val cacheDir: File get() = context.cacheDir

    override fun thumbnail(item: SharedItem, maxPx: Int): ByteArray? = runCatching {
        val bmp = if (item.mimeType.startsWith("video/")) videoFrame(item, maxPx) else photo(item, maxPx)
        bmp ?: return null
        val scale = minOf(1f, maxPx.toFloat() / maxOf(bmp.width, bmp.height))
        val fitted = if (scale < 1f) android.graphics.Bitmap.createScaledBitmap(bmp, (bmp.width * scale).toInt().coerceAtLeast(1), (bmp.height * scale).toInt().coerceAtLeast(1), true) else bmp
        java.io.ByteArrayOutputStream().also { fitted.compress(android.graphics.Bitmap.CompressFormat.JPEG, 72, it) }.toByteArray()
            .also { if (fitted !== bmp) fitted.recycle(); bmp.recycle() }
    }.getOrNull()

    private fun stream(item: SharedItem) =
        if (item.uri.startsWith("content:")) context.contentResolver.openInputStream(Uri.parse(item.uri)) else FileInputStream(File(item.path))

    /** Decoded at the power-of-two scale just above the target, never at full size. */
    private fun photo(item: SharedItem, maxPx: Int): android.graphics.Bitmap? {
        val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        stream(item)?.use { android.graphics.BitmapFactory.decodeStream(it, null, bounds) }
        val long = maxOf(bounds.outWidth, bounds.outHeight).takeIf { it > 0 } ?: return null
        var sample = 1
        while (long / (sample * 2) >= maxPx) sample *= 2
        return stream(item)?.use { android.graphics.BitmapFactory.decodeStream(it, null, android.graphics.BitmapFactory.Options().apply { inSampleSize = sample }) }
    }

    /** A frame one second in (the very first is often black), scaled down by the decoder where it can. */
    private fun videoFrame(item: SharedItem, maxPx: Int): android.graphics.Bitmap? {
        val r = android.media.MediaMetadataRetriever()
        try {
            if (item.uri.startsWith("content:")) r.setDataSource(context, Uri.parse(item.uri)) else r.setDataSource(item.path)
            val at = 1_000_000L
            val mode = android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            return if (Build.VERSION.SDK_INT >= 27) r.getScaledFrameAtTime(at, mode, maxPx, maxPx) else r.getFrameAtTime(at, mode)
        } finally {
            r.release()
        }
    }

    override fun onHostingChanged(running: Boolean) {
        val intent = Intent(context, ShareHostService::class.java)
        if (!running) { context.stopService(intent); return }
        if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent) else context.startService(intent)
    }

    override fun acquireMulticast(): AutoCloseable {
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val lock = wifi.createMulticastLock("maxplayer-share").apply { setReferenceCounted(false); acquire() }
        return AutoCloseable { if (lock.isHeld) lock.release() }
    }
}

actual fun platformShareModule(): Module = jvmShareModule({ AndroidHostPlatform(get()) }, { AndroidDownloadSink(get()) })
