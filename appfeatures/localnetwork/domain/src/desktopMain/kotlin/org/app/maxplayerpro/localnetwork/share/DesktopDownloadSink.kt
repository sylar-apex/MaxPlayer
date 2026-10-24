package org.app.maxplayerpro.localnetwork.share

import java.io.File
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption

/** Saves into ~/Downloads, or a folder picked with the desktop folder dialog. */
internal class DesktopDownloadSink : DownloadSink {
    private val downloads = File(System.getProperty("user.home"), "Downloads")
    override val defaultLabel = "Downloads"

    override fun create(name: String, mimeType: String, location: SaveLocation, subdir: String): DownloadFile {
        val base = when (location) {
            SaveLocation.Default -> downloads
            is SaveLocation.Folder -> File(location.id)
        }
        val dir = (if (subdir.isEmpty()) base else File(base, subdir)).apply { mkdirs() }
        val target = uniqueFile(dir, name)
        val part = File(dir, ".${target.name}.part")
        val channel = FileChannel.open(part.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)
        return object : DownloadFile {
            override val channel = channel
            override fun finish(): String { part.renameTo(target); return target.absolutePath }
            override fun abort() { part.delete() }
        }
    }

    /** "name.mp4", then "name (1).mp4", … so nothing is overwritten. */
    private fun uniqueFile(dir: File, name: String): File {
        val base = name.substringBeforeLast('.', name)
        val ext = name.substringAfterLast('.', "").let { if (it.isEmpty()) "" else ".$it" }
        var f = File(dir, name)
        var i = 1
        while (f.exists()) f = File(dir, "$base ($i)$ext").also { i++ }
        return f
    }
}
