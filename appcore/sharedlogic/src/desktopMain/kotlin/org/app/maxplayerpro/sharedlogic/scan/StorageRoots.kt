package org.app.maxplayerpro.sharedlogic.scan

import java.io.File

/** A place to scan: the user's home folder or a mounted drive (internal, external SSD, pen drive). */
data class StorageRoot(val path: String, val label: String, val removable: Boolean)

/** Finds the user's home folder and every mounted drive on macOS, Windows and Linux. */
object StorageRoots {
    private val os = System.getProperty("os.name").orEmpty().lowercase()
    private val home = File(System.getProperty("user.home"))

    /** Folder names never worth entering: operating system, app data and developer caches. */
    val skipFolderNames: Set<String> = setOf(
        // macOS
        "Library", "Applications", "System", "private", "cores", "Volumes", "dev", "usr", "bin", "sbin", "opt", "etc", "var", "tmp",
        // Windows
        "Windows", "Program Files", "Program Files (x86)", "ProgramData", "AppData", "System Volume Information", "Recovery", "PerfLogs",
        // Linux
        "proc", "sys", "snap", "lost+found", "boot",
        // Developer folders
        "node_modules", "build", "Pods", "DerivedData", "venv", "site-packages", "gradle",
    )

    /** `-Dlibrary.roots=/path/a:/path/b` limits the library to those folders instead of every drive. */
    private val overrideRoots: List<StorageRoot>? = System.getProperty("library.roots")?.split(File.pathSeparatorChar)
        ?.filter { it.isNotBlank() }?.map { StorageRoot(it, File(it).name, removable = false) }

    fun current(): List<StorageRoot> = overrideRoots ?: when {
        "mac" in os -> mac()
        "win" in os -> windows()
        else -> linux()
    }

    /** Home plus every volume under /Volumes except the startup disk, which is a link back to `/`. */
    private fun mac(): List<StorageRoot> = listOf(homeRoot()) +
        File("/Volumes").listFiles().orEmpty()
            .filter { it.isDirectory && runCatching { it.canonicalPath != "/" }.getOrDefault(false) }
            .map { StorageRoot(it.path, it.name, removable = true) }

    /** Every drive letter (C:, D:, USB sticks, external SSDs). */
    private fun windows(): List<StorageRoot> = File.listRoots().orEmpty()
        .filter { runCatching { it.totalSpace > 0 }.getOrDefault(false) }
        .map { StorageRoot(it.path, it.path.trimEnd('\\'), removable = !it.path.startsWith("C", ignoreCase = true)) }

    /** Home plus drives mounted under /media, /run/media and /mnt. */
    private fun linux(): List<StorageRoot> {
        val mounts = listOf("/media", "/run/media").flatMap { base ->
            File(base).listFiles().orEmpty().flatMap { user -> user.listFiles()?.toList() ?: listOf(user) }
        } + File("/mnt").listFiles().orEmpty()
        return listOf(homeRoot()) + mounts.filter { it.isDirectory }.distinctBy { it.path }.map { StorageRoot(it.path, it.name, removable = true) }
    }

    private fun homeRoot() = StorageRoot(home.path, "Home", removable = false)
}
