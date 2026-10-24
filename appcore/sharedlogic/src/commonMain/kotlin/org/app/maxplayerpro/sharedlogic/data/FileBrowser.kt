package org.app.maxplayerpro.sharedlogic.data

/** One entry in the File manager. */
data class FileEntry(val name: String, val path: String, val isDirectory: Boolean, val sizeBytes: Long, val modifiedMs: Long) {
    val extension: String get() = name.substringAfterLast('.', "").lowercase()
}

/** What a place is, for its icon. */
enum class PlaceKind { Home, Disk, Removable, Desktop, Documents, Downloads, Movies, Music, Pictures, Camera, Applications }

/**
 * A place to start browsing, Finder-style: [PlaceSection.Favourites] are everyday folders (Desktop, Downloads, …),
 * [PlaceSection.Locations] are the home folder and drives (internal storage, SD card, external disks).
 */
data class FileRoot(val label: String, val path: String, val kind: PlaceKind = PlaceKind.Disk, val section: PlaceSection = PlaceSection.Locations)

enum class PlaceSection(val title: String) { Favourites("Favourites"), Locations("Locations") }

/** Browses the device's storage for the File manager. */
interface FileBrowser {
    /** False when browsing needs a permission the user hasn't granted (Android "All files access"). */
    val hasAccess: Boolean
    /** Opens the system screen that grants [hasAccess]. */
    fun requestAccess()
    fun roots(): List<FileRoot>
    /** Folders first, then files, by name; hidden entries left out. Empty if unreadable. */
    fun list(path: String): List<FileEntry>
    /** Every file under [path] (subfolders too), up to [limit], for sharing a whole folder. */
    fun walk(path: String, limit: Int = 5_000): List<FileEntry>
}

/** Platforms without a file system to browse here. */
object NoFileBrowser : FileBrowser {
    override val hasAccess = false
    override fun requestAccess() {}
    override fun roots() = emptyList<FileRoot>()
    override fun list(path: String) = emptyList<FileEntry>()
    override fun walk(path: String, limit: Int) = emptyList<FileEntry>()
}

/** MIME type from a file name, for any file (documents, archives, media). */
fun fileMime(name: String): String = when (val ext = name.substringAfterLast('.', "").lowercase()) {
    in imageExtensions -> imageMime(name)
    "mp4", "m4v" -> "video/mp4"; "mkv" -> "video/x-matroska"; "webm" -> "video/webm"; "mov" -> "video/quicktime"; "avi" -> "video/x-msvideo"
    "mp3" -> "audio/mpeg"; "m4a", "aac" -> "audio/mp4"; "flac" -> "audio/flac"; "ogg", "opus" -> "audio/ogg"; "wav" -> "audio/wav"
    "pdf" -> "application/pdf"; "zip" -> "application/zip"; "rar" -> "application/vnd.rar"; "7z" -> "application/x-7z-compressed"
    "txt", "log", "md" -> "text/plain"; "csv" -> "text/csv"; "json" -> "application/json"; "xml" -> "application/xml"; "html", "htm" -> "text/html"
    "doc" -> "application/msword"; "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    "xls" -> "application/vnd.ms-excel"; "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    "ppt" -> "application/vnd.ms-powerpoint"; "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
    "apk" -> "application/vnd.android.package-archive"
    else -> if (ext.isEmpty()) "application/octet-stream" else "application/octet-stream"
}

/** A `file://` URI for [path] with spaces and other special characters escaped, so players can open it. */
fun fileUri(path: String): String {
    val safe = "-._~/!$&'()*+,;=:@"
    return "file://" + buildString {
        path.encodeToByteArray().forEach { b ->
            val c = (b.toInt() and 0xff).toChar()
            if (c.isLetterOrDigit() && c.code < 128 || c in safe) append(c) else append('%').append(((b.toInt() and 0xff)).toString(16).uppercase().padStart(2, '0'))
        }
    }
}
