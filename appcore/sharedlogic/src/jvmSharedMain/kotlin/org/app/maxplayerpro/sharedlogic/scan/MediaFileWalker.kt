package org.app.maxplayerpro.sharedlogic.scan

import org.app.kmediaplayer.model.MediaFile
import org.app.kmediaplayer.model.MediaFormats
import java.io.File
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

/**
 * Walks folder trees, including every nested folder, and collects video and audio files. Hidden
 * folders, folders named in [skipFolderNames] (system and app-data folders) and bundle-like folders
 * (`.app`, `.photoslibrary`, ...) are not entered, nor are source-code repositories (folders with a
 * `.git` folder), whose sample clips are test data rather than a library. Files smaller than
 * [minFileBytes] are ignored for the same reason. Symbolic links are not followed, so a link back to
 * a parent cannot loop. Unreadable folders are skipped silently.
 */
class MediaFileWalker(
    private val skipFolderNames: Set<String> = emptySet(),
    private val videoExtensions: Set<String> = MediaFormats.videoExtensions,
    private val audioExtensions: Set<String> = MediaFormats.audioExtensions,
    private val maxDepth: Int = 40,
    private val minFileBytes: Long = 100 * 1024,
) {
    private val skipLower = skipFolderNames.map { it.lowercase() }.toSet()

    /** Every media file under [root]; [isCancelled] stops the walk early. */
    fun walk(root: File, isCancelled: () -> Boolean = { false }): List<MediaFile> {
        if (!root.isDirectory) return emptyList()
        val found = mutableListOf<MediaFile>()
        val visitor = object : SimpleFileVisitor<Path>() {
            override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                if (isCancelled()) return FileVisitResult.TERMINATE
                return if (dir != root.toPath() && shouldSkip(dir)) FileVisitResult.SKIP_SUBTREE else FileVisitResult.CONTINUE
            }

            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                if (attrs.isRegularFile) file.toFile().toMediaFile(attrs)?.let(found::add)
                return FileVisitResult.CONTINUE
            }

            override fun visitFileFailed(file: Path, exc: IOException): FileVisitResult = FileVisitResult.CONTINUE
        }
        runCatching { Files.walkFileTree(root.toPath(), emptySet(), maxDepth, visitor) }
        return found
    }

    private fun shouldSkip(dir: Path): Boolean {
        val name = dir.fileName?.toString() ?: return false
        val lower = name.lowercase()
        return name.startsWith('.') || name.startsWith('$') || lower in skipLower || BUNDLE_SUFFIXES.any(lower::endsWith) ||
            Files.isDirectory(dir.resolve(".git"))
    }

    private fun File.toMediaFile(attrs: BasicFileAttributes): MediaFile? {
        val ext = extension.lowercase()
        val audio = ext in audioExtensions
        if (!audio && ext !in videoExtensions) return null
        if (attrs.size() < minFileBytes) return null
        if (name.startsWith("._")) return null // macOS resource forks on external drives
        if (ext == "ts" && !isMpegTransportStream()) return null // TypeScript source, not video
        return MediaFile(
            id = path.hashCode().toLong(), uri = toURI().toString(), path = path, name = name,
            sizeBytes = attrs.size(), durationMs = 0, dateModifiedMs = attrs.lastModifiedTime().toMillis(),
            isAudio = audio, artist = if (audio) "Unknown" else "", album = if (audio) "Unknown" else "",
        )
    }

    /** MPEG-TS packets are 188 bytes, each starting with the sync byte 0x47. */
    private fun File.isMpegTransportStream(): Boolean = runCatching {
        inputStream().use { input ->
            val head = ByteArray(TS_PACKET * 3)
            val read = input.readNBytes(head, 0, head.size)
            read >= TS_PACKET * 2 && (0 until read / TS_PACKET).all { head[it * TS_PACKET] == TS_SYNC }
        }
    }.getOrDefault(false)

    private companion object {
        const val TS_PACKET = 188
        const val TS_SYNC: Byte = 0x47
        val BUNDLE_SUFFIXES = listOf(".app", ".photoslibrary", ".bundle", ".framework", ".plugin", ".kext", ".xcarchive", ".lrlibrary")
    }
}
