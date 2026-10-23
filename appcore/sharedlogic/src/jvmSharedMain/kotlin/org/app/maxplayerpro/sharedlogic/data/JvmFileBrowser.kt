package org.app.maxplayerpro.sharedlogic.data

import java.io.File

/** java.io browsing shared by Android and desktop; the platforms supply roots and access. */
abstract class JvmFileBrowser : FileBrowser {
    override fun list(path: String): List<FileEntry> =
        (File(path).listFiles() ?: emptyArray()).filter { !it.isHidden && !it.name.startsWith(".") }
            .map { FileEntry(it.name, it.path, it.isDirectory, if (it.isDirectory) 0 else it.length(), it.lastModified()) }
            .sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))

    override fun walk(path: String, limit: Int): List<FileEntry> {
        val out = mutableListOf<FileEntry>()
        File(path).walkTopDown().onEnter { !it.isHidden && !it.name.startsWith(".") }.forEach { f ->
            if (out.size >= limit) return out
            if (f.isFile && !f.name.startsWith(".")) out += FileEntry(f.name, f.path, false, f.length(), f.lastModified())
        }
        return out
    }
}
