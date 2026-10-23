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

actual fun platformShareModule(): Module = jvmShareModule { AndroidHostPlatform(get()) }
