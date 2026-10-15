package org.app.maxplayerpro.sharedlogic.model

import org.app.kmediaplayer.model.MediaFile
import org.app.kmediaplayer.model.PlaybackRecord
import kotlin.test.Test
import kotlin.test.assertEquals

class ViewOptionsTest {
    private fun file(name: String, folder: String = "/m", size: Long = 0, date: Long = 0, length: Long = 0, w: Int = 0, h: Int = 0, fps: Float = 0f) =
        MediaFile(name.hashCode().toLong(), "file://$folder/$name", "$folder/$name", name, size, length, date, w, h, fps)

    private val a = file("Ep 2.mkv", size = 300, date = 3, length = 100, w = 1920, h = 1080, fps = 24f)
    private val b = file("Ep 10.mp4", size = 100, date = 1, length = 300, w = 3840, h = 2160, fps = 60f)
    private val c = file("clip.avi", size = 200, date = 2, length = 200, w = 1280, h = 720, fps = 30f)
    private val all = listOf(a, b, c)

    private fun names(key: SortKey, ascending: Boolean = true, records: Map<String, PlaybackRecord> = emptyMap()) =
        sortFiles(all, key, ascending, records).map { it.name }

    @Test fun titleUsesNaturalOrder() = assertEquals(listOf("clip.avi", "Ep 2.mkv", "Ep 10.mp4"), names(SortKey.Title))
    @Test fun descendingReverses() = assertEquals(listOf("Ep 10.mp4", "Ep 2.mkv", "clip.avi"), names(SortKey.Title, ascending = false))
    @Test fun date() = assertEquals(listOf("Ep 10.mp4", "clip.avi", "Ep 2.mkv"), names(SortKey.Date))
    @Test fun size() = assertEquals(listOf("Ep 10.mp4", "clip.avi", "Ep 2.mkv"), names(SortKey.Size))
    @Test fun length() = assertEquals(listOf("Ep 2.mkv", "clip.avi", "Ep 10.mp4"), names(SortKey.Length))
    @Test fun resolution() = assertEquals(listOf("clip.avi", "Ep 2.mkv", "Ep 10.mp4"), names(SortKey.Resolution))
    @Test fun frameRate() = assertEquals(listOf("Ep 2.mkv", "clip.avi", "Ep 10.mp4"), names(SortKey.FrameRate))
    @Test fun type() = assertEquals(listOf("clip.avi", "Ep 2.mkv", "Ep 10.mp4"), names(SortKey.Type))

    @Test fun playedTimeAndStatusUseHistory() {
        val history = mapOf(
            b.uri to PlaybackRecord(positionMs = 10, lastPlayedMs = 50),
            c.uri to PlaybackRecord(positionMs = 0, lastPlayedMs = 90, finished = true),
        )
        assertEquals(listOf("Ep 2.mkv", "Ep 10.mp4", "clip.avi"), names(SortKey.PlayedTime, records = history))
        assertEquals(listOf("Ep 2.mkv", "Ep 10.mp4", "clip.avi"), names(SortKey.Status, records = history))
    }

    @Test fun foldersGroupAndSort() {
        val files = listOf(file("x.mp4", "/b", size = 5), file("y.mp4", "/a", size = 1), file("z.mp4", "/a", size = 1))
        val folders = buildFolders(files, ListViewOptions())
        assertEquals(listOf("a", "b"), sortFolders(folders, SortKey.Title, true).map { it.name })
        assertEquals(listOf("b", "a"), sortFolders(folders, SortKey.Size, false).map { it.name })
        assertEquals(2, folders.first { it.name == "a" }.files.size)
    }

    @Test fun hiddenFilesFollowTheOption() {
        val files = listOf(file("v.mp4", "/m/.private"), file("w.mp4", "/m"))
        assertEquals(listOf("w.mp4"), filterVisible(files, ListViewOptions(showHidden = false)).map { it.name })
        assertEquals(2, filterVisible(files, ListViewOptions(showHidden = true)).size)
    }

    @Test fun noMediaFoldersFollowTheOption() {
        val files = listOf(file("v.mp4", "/m/skip/sub"), file("w.mp4", "/m"))
        assertEquals(listOf("w.mp4"), filterVisible(files, ListViewOptions(respectNoMedia = true), setOf("/m/skip")).map { it.name })
        assertEquals(2, filterVisible(files, ListViewOptions(respectNoMedia = false), setOf("/m/skip")).size)
    }

    @Test fun optionsSurviveEncodeDecode() {
        val o = ListViewOptions(ViewMode.Files, ListLayout.Grid, SortKey.Resolution, false, setOf(ListField.Size, ListField.Path), false, true, false,
            videoThumbnails = false, folderThumbnails = false)
        assertEquals(o, ListViewOptions.decode(o.encode()))
    }

    @Test fun olderSavedOptionsKeepThumbnailsOn() {
        val old = "Folders|Grid|Title|true|Thumbnail+Length|true|false|true"
        val o = ListViewOptions.decode(old)
        assertEquals(ListLayout.Grid, o.layout)
        assertEquals(true, o.videoThumbnails)
        assertEquals(true, o.folderThumbnails)
    }
}
