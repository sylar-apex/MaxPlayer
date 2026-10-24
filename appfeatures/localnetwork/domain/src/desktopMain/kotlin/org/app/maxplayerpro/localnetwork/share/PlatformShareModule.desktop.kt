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

    override val cacheDir: File by lazy {
        val home = System.getProperty("user.home")
        val os = platform.lowercase()
        when {
            "mac" in os -> File(home, "Library/Caches/MaxPlayer")
            "win" in os -> File(System.getenv("LOCALAPPDATA") ?: home, "MaxPlayer/cache")
            else -> File(System.getenv("XDG_CACHE_HOME") ?: "$home/.cache", "maxplayer")
        }
    }

    override fun thumbnail(item: SharedItem, maxPx: Int): ByteArray? = runCatching {
        val file = if (item.uri.startsWith("file:")) File(URI(item.uri)) else File(item.path)
        val picture = if (item.mimeType in javaReadable) readReduced(file, maxPx) else null
        val shot = picture ?: if ("mac" in platform.lowercase()) quickLook(file, maxPx) else null
        shot?.let { toJpeg(fit(it, maxPx)) }
    }.getOrNull()

    /** JPEG, PNG, GIF, BMP: decoded at a reduced size straight from the file, which is far faster than full size. */
    private fun readReduced(file: File, maxPx: Int): java.awt.image.BufferedImage? =
        javax.imageio.ImageIO.createImageInputStream(file).use { input ->
            val reader = javax.imageio.ImageIO.getImageReaders(input).asSequence().firstOrNull() ?: return null
            try {
                reader.input = input
                val long = maxOf(reader.getWidth(0), reader.getHeight(0))
                val step = (long / (maxPx * 2)).coerceAtLeast(1)
                reader.read(0, reader.defaultReadParam.apply { setSourceSubsampling(step, step, 0, 0) })
            } finally {
                reader.dispose()
            }
        }

    /** macOS Quick Look: the system's own thumbnails, for HEIC, RAW, videos and anything else it can preview. */
    private fun quickLook(file: File, maxPx: Int): java.awt.image.BufferedImage? {
        val out = java.nio.file.Files.createTempDirectory("ql").toFile()
        try {
            val p = ProcessBuilder("qlmanage", "-t", "-s", "$maxPx", "-o", out.path, file.path)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.DISCARD).start()
            // Some files make Quick Look hang; give up instead of holding a thumbnail slot.
            if (!p.waitFor(8, java.util.concurrent.TimeUnit.SECONDS)) { p.destroyForcibly(); return null }
            return out.listFiles()?.firstOrNull { it.extension == "png" }?.let { javax.imageio.ImageIO.read(it) }
        } finally {
            out.deleteRecursively()
        }
    }

    private fun fit(src: java.awt.image.BufferedImage, maxPx: Int): java.awt.image.BufferedImage {
        val scale = minOf(1.0, maxPx.toDouble() / maxOf(src.width, src.height))
        val w = (src.width * scale).toInt().coerceAtLeast(1)
        val h = (src.height * scale).toInt().coerceAtLeast(1)
        return java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_RGB).also { dst ->
            dst.createGraphics().apply {
                setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR)
                drawImage(src, 0, 0, w, h, null)
                dispose()
            }
        }
    }

    private fun toJpeg(img: java.awt.image.BufferedImage): ByteArray {
        val writer = javax.imageio.ImageIO.getImageWritersByFormatName("jpg").next()
        val bytes = java.io.ByteArrayOutputStream()
        javax.imageio.ImageIO.createImageOutputStream(bytes).use { out ->
            writer.output = out
            val param = writer.defaultWriteParam.apply { compressionMode = javax.imageio.ImageWriteParam.MODE_EXPLICIT; compressionQuality = 0.72f }
            writer.write(null, javax.imageio.IIOImage(img, null, null), param)
            writer.dispose()
        }
        return bytes.toByteArray()
    }

    private val javaReadable = setOf("image/jpeg", "image/png", "image/gif", "image/bmp")

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

actual fun platformShareModule(): Module = jvmShareModule({ DesktopHostPlatform() }, { DesktopDownloadSink() })
