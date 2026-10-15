package org.app.maxplayerpro.sharedlogic.scan

import org.app.kmediaplayer.desktop.ProbedInfo
import org.app.kmediaplayer.model.MediaFile
import java.io.File
import java.util.Properties

/**
 * Remembers probed length / resolution / frame rate per file in `~/.maxplayer/metadata.properties`,
 * keyed by path, size and modification time, so each file's header is read only once.
 */
class MetadataCache(private val file: File = File(System.getProperty("user.home"), ".maxplayer/metadata.properties")) {
    private val props = Properties().apply { runCatching { file.inputStream().use(::load) } }
    private var dirty = 0

    private fun key(f: MediaFile) = "${f.path}|${f.sizeBytes}|${f.dateModifiedMs}"

    fun get(f: MediaFile): ProbedInfo? = synchronized(props) {
        val v = props.getProperty(key(f))?.split(',') ?: return null
        runCatching { ProbedInfo(v[0].toLong(), v[1].toInt(), v[2].toInt(), v[3].toFloat()) }.getOrNull()
    }

    fun put(f: MediaFile, info: ProbedInfo) = synchronized(props) {
        props.setProperty(key(f), "${info.durationMs},${info.width},${info.height},${info.frameRate}")
        if (++dirty >= 50) save()
    }

    fun save() = synchronized(props) {
        if (dirty == 0) return
        runCatching { file.parentFile.mkdirs(); file.outputStream().use { props.store(it, "Max Player media metadata") } }
        dirty = 0
    }
}

fun MediaFile.withInfo(info: ProbedInfo): MediaFile =
    copy(durationMs = info.durationMs, width = info.width, height = info.height, frameRate = info.frameRate)
