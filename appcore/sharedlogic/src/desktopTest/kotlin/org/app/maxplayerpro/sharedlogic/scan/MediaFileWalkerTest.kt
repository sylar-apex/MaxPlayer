package org.app.maxplayerpro.sharedlogic.scan

import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MediaFileWalkerTest {
    private val root: File = Files.createTempDirectory("walker").toFile()

    @AfterTest fun cleanUp() { root.deleteRecursively() }

    private fun file(path: String, bytes: ByteArray = ByteArray(200 * 1024)) = File(root, path).apply { parentFile.mkdirs(); writeBytes(bytes) }

    private fun names() = MediaFileWalker(StorageRoots.skipFolderNames).walk(root).map { it.name }.toSet()

    @Test fun findsVideosInDeeplyNestedFolders() {
        file("a.mkv"); file("Shows/S01/E01/episode.mp4"); file("x/y/z/w/v/u/deep.avi")
        assertEquals(setOf("a.mkv", "episode.mp4", "deep.avi"), names())
    }

    @Test fun skipsHiddenSystemAndBundleFolders() {
        file(".hidden/a.mp4"); file("Library/b.mp4"); file("node_modules/c.mp4"); file("Photos.photoslibrary/d.mov"); file("ok/e.mp4")
        assertEquals(setOf("e.mp4"), names())
    }

    @Test fun separatesAudioFromVideo() {
        file("song.flac"); file("clip.webm")
        val all = MediaFileWalker().walk(root)
        assertEquals(setOf("song.flac"), all.filter { it.isAudio }.map { it.name }.toSet())
    }

    @Test fun tellsTransportStreamsFromTypeScript() {
        val ts = ByteArray(200 * 1024).also { b -> for (i in 0 until 3) b[i * 188] = 0x47 }
        file("record.ts", ts)
        file("src/useThing.ts", "export const a = 1;".repeat(20_000).toByteArray())
        assertEquals(setOf("record.ts"), names())
    }

    @Test fun skipsCodeRepositoriesAndTinyFixtures() {
        file("project/.git/HEAD"); file("project/fixtures/sample.mp4"); file("clips/tiny.mp4", ByteArray(1_000)); file("clips/real.mp4")
        assertEquals(setOf("real.mp4"), names())
    }

    @Test fun ignoresMacResourceForksAndNonMedia() {
        file("._movie.mp4"); file("notes.txt"); file("movie.mp4")
        assertEquals(setOf("movie.mp4"), names())
    }
}
