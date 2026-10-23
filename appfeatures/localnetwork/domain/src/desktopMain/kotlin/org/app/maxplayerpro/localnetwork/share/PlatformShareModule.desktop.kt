package org.app.maxplayerpro.localnetwork.share

import org.koin.core.module.Module
import java.io.File
import java.net.URI
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption

private class DesktopHostPlatform : HostPlatform {
    override val deviceName: String by lazy { computerName() }
    override val platform: String = System.getProperty("os.name") ?: "Desktop"

    override fun open(item: SharedItem): FileChannel? = runCatching {
        val file = if (item.uri.startsWith("file:")) File(URI(item.uri)) else File(item.path)
        FileChannel.open(file.toPath(), StandardOpenOption.READ)
    }.getOrNull()

    // The desktop app keeps running while its window is open; nothing extra keeps it awake.
    override fun onHostingChanged(running: Boolean) {}

    override fun acquireMulticast(): AutoCloseable = AutoCloseable {}

    /** The name the user gave this computer. InetAddress.getLocalHost() is avoided: it can stall for seconds on macOS. */
    private fun computerName(): String {
        val os = platform.lowercase()
        val fromCommand = if ("mac" in os) {
            runCatching { ProcessBuilder("scutil", "--get", "ComputerName").start().inputStream.bufferedReader().readText().trim() }.getOrNull()
        } else null
        return fromCommand?.takeIf { it.isNotEmpty() }
            ?: System.getenv("COMPUTERNAME")
            ?: System.getenv("HOSTNAME")
            ?: runCatching { File("/etc/hostname").readText().trim() }.getOrNull()?.takeIf { it.isNotEmpty() }
            ?: "${System.getProperty("user.name")}'s computer"
    }
}

actual fun platformShareModule(): Module = jvmShareModule { DesktopHostPlatform() }
