package org.app.maxplayerpro.sharedlogic.model

import org.app.kmediaplayer.model.MediaFile
import org.app.kmediaplayer.model.PlaybackRecord

data class MediaFolder(val path: String, val files: List<MediaFile>) {
    val name: String get() = path.substringAfterLast('/').ifEmpty { path }
    val sizeBytes: Long get() = files.sumOf { it.sizeBytes }
    val latestModifiedMs: Long get() = files.maxOfOrNull { it.dateModifiedMs } ?: 0
}

enum class ViewMode(val label: String) { AllFolders("All folders"), Files("Files"), Folders("Folders") }

enum class ListLayout(val label: String) { List("List"), Grid("Grid") }

enum class SortKey(val label: String) {
    Title("Title"), Date("Date"), PlayedTime("Played time"), Status("Status"), Length("Length"),
    Size("Size"), Resolution("Resolution"), Path("Path"), FrameRate("Frame rate"), Type("Type"),
}

enum class ListField(val label: String) {
    Thumbnail("Thumbnail"), Length("Length"), FileExtension("File extension"), PlayedTime("Played time"),
    Resolution("Resolution"), FrameRate("Frame rate"), Path("Path"), Size("Size"), Date("Date"),
}

data class ListViewOptions(
    val viewMode: ViewMode = ViewMode.Folders,
    val layout: ListLayout = ListLayout.List,
    val sortKey: SortKey = SortKey.Title,
    val ascending: Boolean = true,
    val fields: Set<ListField> = setOf(ListField.Thumbnail, ListField.Length),
    val lengthOverThumbnail: Boolean = true,
    val showHidden: Boolean = false,
    val respectNoMedia: Boolean = true,
) {
    fun encode(): String = listOf(
        viewMode.name, layout.name, sortKey.name, ascending.toString(),
        fields.joinToString("+") { it.name }, lengthOverThumbnail.toString(),
        showHidden.toString(), respectNoMedia.toString(),
    ).joinToString("|")

    companion object {
        fun decode(raw: String?): ListViewOptions {
            val p = raw?.split('|') ?: return ListViewOptions()
            if (p.size < 8) return ListViewOptions()
            return runCatching {
                ListViewOptions(
                    viewMode = ViewMode.valueOf(p[0]),
                    layout = ListLayout.valueOf(p[1]),
                    sortKey = SortKey.valueOf(p[2]),
                    ascending = p[3].toBoolean(),
                    fields = p[4].split('+').filter { it.isNotEmpty() }.map { ListField.valueOf(it) }.toSet(),
                    lengthOverThumbnail = p[5].toBoolean(),
                    showHidden = p[6].toBoolean(),
                    respectNoMedia = p[7].toBoolean(),
                )
            }.getOrDefault(ListViewOptions())
        }
    }
}

/** Natural ordering so that "Ep 2" sorts before "Ep 10". */
fun naturalCompare(a: String, b: String): Int {
    var i = 0
    var j = 0
    while (i < a.length && j < b.length) {
        val ca = a[i]
        val cb = b[j]
        if (ca.isDigit() && cb.isDigit()) {
            val si = i; while (i < a.length && a[i].isDigit()) i++
            val sj = j; while (j < b.length && b[j].isDigit()) j++
            val na = a.substring(si, i).trimStart('0')
            val nb = b.substring(sj, j).trimStart('0')
            if (na.length != nb.length) return na.length - nb.length
            val c = na.compareTo(nb)
            if (c != 0) return c
        } else {
            val c = ca.lowercaseChar().compareTo(cb.lowercaseChar())
            if (c != 0) return c
            i++; j++
        }
    }
    return (a.length - i) - (b.length - j)
}

fun fileComparator(key: SortKey, records: Map<String, PlaybackRecord>): Comparator<MediaFile> {
    val byTitle = Comparator<MediaFile> { x, y -> naturalCompare(x.name, y.name) }
    val primary: Comparator<MediaFile> = when (key) {
        SortKey.Title -> byTitle
        SortKey.Date -> compareBy { it.dateModifiedMs }
        SortKey.PlayedTime -> compareBy { records[it.uri]?.lastPlayedMs ?: 0 }
        SortKey.Status -> compareBy { statusRank(records[it.uri]) }
        SortKey.Length -> compareBy { it.durationMs }
        SortKey.Size -> compareBy { it.sizeBytes }
        SortKey.Resolution -> compareBy { it.resolutionPixels }
        SortKey.Path -> Comparator { x, y -> naturalCompare(x.path, y.path) }
        SortKey.FrameRate -> compareBy { it.frameRate }
        SortKey.Type -> compareBy { it.extension }
    }
    return primary.then(byTitle)
}

/** 0 = never played, 1 = in progress, 2 = finished. */
fun statusRank(record: PlaybackRecord?): Int = when {
    record == null || record.lastPlayedMs == 0L -> 0
    record.finished -> 2
    else -> 1
}

fun sortFiles(files: List<MediaFile>, key: SortKey, ascending: Boolean, records: Map<String, PlaybackRecord> = emptyMap()): List<MediaFile> {
    val c = fileComparator(key, records)
    return files.sortedWith(if (ascending) c else c.reversed())
}

fun sortFolders(folders: List<MediaFolder>, key: SortKey, ascending: Boolean, records: Map<String, PlaybackRecord> = emptyMap()): List<MediaFolder> {
    val byName = Comparator<MediaFolder> { x, y -> naturalCompare(x.name, y.name) }
    val primary: Comparator<MediaFolder> = when (key) {
        SortKey.Date -> compareBy { it.latestModifiedMs }
        SortKey.Size -> compareBy { it.sizeBytes }
        SortKey.Path -> Comparator { x, y -> naturalCompare(x.path, y.path) }
        SortKey.Length -> compareBy { f -> f.files.sumOf { it.durationMs } }
        SortKey.PlayedTime -> compareBy { f -> f.files.maxOfOrNull { records[it.uri]?.lastPlayedMs ?: 0 } ?: 0 }
        else -> byName
    }
    val c = primary.then(byName)
    return folders.sortedWith(if (ascending) c else c.reversed())
}

/**
 * Builds the folder list. [noMediaDirs] are directories containing a `.nomedia` file; with
 * [ListViewOptions.respectNoMedia] their files and their descendants are excluded.
 */
fun buildFolders(files: List<MediaFile>, options: ListViewOptions, noMediaDirs: Set<String> = emptySet()): List<MediaFolder> =
    filterVisible(files, options, noMediaDirs).groupBy { it.folderPath }.map { (p, f) -> MediaFolder(p, f) }

fun filterVisible(files: List<MediaFile>, options: ListViewOptions, noMediaDirs: Set<String> = emptySet()): List<MediaFile> =
    files.filter { f ->
        (options.showHidden || !f.isHidden) &&
            (!options.respectNoMedia || noMediaDirs.none { d -> f.folderPath == d || f.folderPath.startsWith("$d/") })
    }

fun isNew(file: MediaFile, nowMs: Long, periodDays: Int): Boolean =
    periodDays > 0 && nowMs - file.dateModifiedMs in 0..(periodDays * 86_400_000L)

data class StorageSummary(val usedBytes: Long, val totalBytes: Long, val videoBytes: Long, val audioBytes: Long, val imageBytes: Long)

/** A file locked in the private folder. */
data class PrivateFile(val path: String, val name: String, val sizeBytes: Long) {
    val uri: String get() = "file://$path"
}
