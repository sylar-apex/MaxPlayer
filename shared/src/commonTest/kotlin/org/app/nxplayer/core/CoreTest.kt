package org.app.nxplayer.core

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun file(name: String, folder: String = "/sdcard/Video", size: Long = 1, duration: Long = 1, date: Long = 1, w: Int = 0, h: Int = 0) =
    MediaFile(name.hashCode().toLong(), "content://$folder/$name", "$folder/$name", name, size, duration, date, w, h)

class MediaTest {
    @Test fun naturalOrderPutsNumbersInNumericOrder() {
        val sorted = listOf("Ep 10.mkv", "Ep 2.mkv", "ep 1.mkv").sortedWith { a, b -> naturalCompare(a, b) }
        assertEquals(listOf("ep 1.mkv", "Ep 2.mkv", "Ep 10.mkv"), sorted)
    }

    @Test fun sortBySizeDescending() {
        val files = listOf(file("a", size = 5), file("b", size = 50), file("c", size = 10))
        assertEquals(listOf("b", "c", "a"), sortFiles(files, SortKey.Size, ascending = false).map { it.name })
    }

    @Test fun sortByStatusUsesPlaybackRecords() {
        val a = file("a"); val b = file("b"); val c = file("c")
        val records = mapOf(a.uri to PlaybackRecord(10, 5, finished = true), b.uri to PlaybackRecord(10, 5))
        assertEquals(listOf("c", "b", "a"), sortFiles(listOf(a, b, c), SortKey.Status, true, records).map { it.name })
    }

    @Test fun sortByResolution() {
        val files = listOf(file("hd", w = 1920, h = 1080), file("sd", w = 640, h = 480))
        assertEquals("sd", sortFiles(files, SortKey.Resolution, true).first().name)
    }

    @Test fun foldersHideHiddenAndNoMedia() {
        val files = listOf(file("a.mp4"), file("b.mp4", "/sdcard/.secret"), file("c.mp4", "/sdcard/Skip/sub"))
        val opts = ListViewOptions()
        val folders = buildFolders(files, opts, noMediaDirs = setOf("/sdcard/Skip"))
        assertEquals(listOf("/sdcard/Video"), folders.map { it.path })
        val all = buildFolders(files, opts.copy(showHidden = true, respectNoMedia = false), setOf("/sdcard/Skip"))
        assertEquals(3, all.size)
    }

    @Test fun viewOptionsRoundTrip() {
        val o = ListViewOptions(ViewMode.Files, ListLayout.Grid, SortKey.Length, false, setOf(ListField.Size, ListField.Path), false, true, false)
        assertEquals(o, ListViewOptions.decode(o.encode()))
        assertEquals(ListViewOptions(), ListViewOptions.decode("garbage"))
    }

    @Test fun newTag() {
        val now = 10 * 86_400_000L
        assertTrue(isNew(file("a", date = now - 86_400_000L), now, 7))
        assertFalse(isNew(file("a", date = now - 8 * 86_400_000L), now, 7))
        assertFalse(isNew(file("a", date = now), now, 0))
    }

    @Test fun durationFormats() {
        assertEquals("00:24", formatDuration(24_000))
        assertEquals("3:17:20", formatDuration(11_840_000))
        assertEquals("-3:16:56", formatRemaining(24_000, 11_840_000))
        assertEquals("4.00 GB", formatSize(4L * 1024 * 1024 * 1024))
    }
}

class SubtitleTest {
    private val srt = """
        1
        00:00:01,000 --> 00:00:03,500
        <i>Hello</i>

        2
        00:00:05,000 --> 00:00:06,000
        /World
    """.trimIndent()

    @Test fun parsesSrtAndStripsTags() {
        val doc = SubtitleParser.parse(srt, "srt")
        assertEquals(2, doc.cues.size)
        assertEquals(Cue(1000, 3500, "Hello", italic = true), doc.cues[0])
        assertEquals("Hello", doc.cuesAt(2000).single().text)
        assertTrue(doc.cuesAt(4000).isEmpty())
    }

    @Test fun italicSlashOption() {
        val doc = SubtitleParser.parse(srt, "srt", italicSlash = true)
        assertEquals(Cue(5000, 6000, "World", italic = true), doc.cues[1])
    }

    @Test fun syncAndSpeedShiftTimeline() {
        val doc = SubtitleParser.parse(srt, "srt")
        assertTrue(doc.cuesAt(1500, syncMs = 1000).isEmpty())
        assertEquals("Hello", doc.cuesAt(2500, syncMs = 1000).single().text)
        assertEquals("Hello", doc.cuesAt(1000, speed = 2f).single().text)
        assertEquals(5000L, doc.nextCueStart(2000))
    }

    @Test fun parsesVttWithShortTimestamps() {
        val vtt = "WEBVTT\n\n00:01.000 --> 00:02.000\nHi\n"
        assertEquals(Cue(1000, 2000, "Hi"), SubtitleParser.parse(vtt, "vtt").cues.single())
    }
}

class PlayerLogicTest {
    @Test fun abRepeat() {
        var ab = AbRepeat().setA(10_000)
        assertFalse(ab.active)
        assertEquals(ab, ab.setB(5_000))
        ab = ab.setB(20_000)
        assertTrue(ab.active)
        assertNull(ab.loopTarget(15_000))
        assertEquals(10_000L, ab.loopTarget(20_000))
        assertFalse(ab.clear().active)
    }

    @Test fun sleepTimerKeypad() {
        val t = SleepTimerInput().press(0).press(1).press(3).press(0).press(9)
        assertEquals(1, t.hours)
        assertEquals(30, t.minutes)
        assertEquals(90, t.totalMinutes)
        assertFalse(SleepTimerInput().isValid)
    }

    @Test fun fitVideoModes() {
        val c = Size(2000f, 1000f)
        val v = Size(1000f, 1000f)
        assertEquals(Size(1000f, 1000f), fitVideo(c, v, ZoomMode.Fit))
        assertEquals(c, fitVideo(c, v, ZoomMode.Stretch))
        assertEquals(Size(2000f, 2000f), fitVideo(c, v, ZoomMode.Crop))
        assertEquals(Size(1000f * 16 / 9, 1000f), fitVideo(c, v, ZoomMode.Fit, 16f / 9))
        assertEquals(ZoomMode.Fit, ZoomMode.Original.next())
    }

    @Test fun gestureClassification() {
        assertEquals(GestureKind.None, Gestures.classify(5f, 5f, 100f, 1000f, true, true, true))
        assertEquals(GestureKind.Seek, Gestures.classify(100f, 10f, 100f, 1000f, true, true, true))
        assertEquals(GestureKind.Brightness, Gestures.classify(0f, -100f, 100f, 1000f, true, true, true))
        assertEquals(GestureKind.Volume, Gestures.classify(0f, -100f, 900f, 1000f, true, true, true))
        assertEquals(GestureKind.None, Gestures.classify(0f, -100f, 900f, 1000f, true, false, true))
        assertEquals(20_000L, Gestures.seekOffsetMs(200f, 100f, 10))
        assertEquals(-1, Gestures.doubleTapZone(10f, 900f))
        assertEquals(0, Gestures.doubleTapZone(450f, 900f))
        assertEquals(1, Gestures.doubleTapZone(800f, 900f))
    }

    @Test fun speedClampAndLabel() {
        assertEquals(4f, Speed.clamp(9f))
        assertEquals(0.25f, Speed.clamp(0f))
        assertEquals("1X", Speed.label(1f))
        assertEquals("2X", Speed.label(2f))
        assertEquals("1.25X", Speed.label(1.25f))
        assertEquals(1.05f, Speed.increase(1f))
    }

    @Test fun volumeBoost() {
        val v = VolumeState(15, 15, boostEnabled = true)
        assertEquals(100, v.boostPercent)
        val boosted = v.adjust(15)
        assertEquals(200, boosted.boostPercent)
        assertEquals(15, boosted.systemLevel)
        assertEquals(30, boosted.adjust(10).level)
        assertEquals(15, VolumeState(15, 15, false).adjust(5).level)
    }

    @Test fun resumePolicy() {
        assertEquals(ResumeDecision.StartOver, ResumePolicy.decide("ask", 1_000, 100_000, true, false))
        assertEquals(ResumeDecision.Ask, ResumePolicy.decide("ask", 50_000, 100_000, true, false))
        assertEquals(ResumeDecision.Resume, ResumePolicy.decide("resume", 50_000, 100_000, true, false))
        assertEquals(ResumeDecision.StartOver, ResumePolicy.decide("resume", 50_000, 100_000, false, true))
        assertEquals(ResumeDecision.StartOver, ResumePolicy.decide("resume", 98_000, 100_000, true, false))
    }

    @Test fun shuffleKeepsCurrentFirst() {
        val order = shuffledOrder(10, 4, Random(1))
        assertEquals(4, order.first())
        assertEquals((0 until 10).toSet(), order.toSet())
    }
}

class SettingsTest {
    @Test fun defaultsMatchReference() {
        val s = Settings(MemoryStore())
        assertEquals("pip", s[Keys.backgroundMode])
        assertEquals("resume", s[Keys.resume])
        assertEquals(10, s[Keys.seekSpeed])
        assertEquals(2.0f, s[Keys.interfaceAutoHideDelay])
        assertEquals(19, s[Keys.subSize])
        assertEquals(7, s[Keys.newTaggedPeriod])
    }

    @Test fun invalidValuesFallBackToDefault() {
        val store = MemoryStore(mutableMapOf("sticky_video" to "bogus", "gesture_seek_speed" to "999"))
        val s = Settings(store)
        assertEquals("pip", s[Keys.backgroundMode])
        assertEquals(60, s[Keys.seekSpeed])
    }

    @Test fun exportImportRoundTrip() {
        val a = Settings(MemoryStore())
        a[Keys.seekSpeed] = 20
        a[Keys.userAgent] = "Agent\nX"
        val text = a.export(mapOf("userdata" to "H\u001Fx"))
        val b = Settings(MemoryStore())
        val user = b.import(text)
        assertEquals(20, b[Keys.seekSpeed])
        assertEquals("Agent\nX", b[Keys.userAgent])
        assertEquals("H\u001Fx", user["userdata"])
        b.reset()
        assertEquals(10, b[Keys.seekSpeed])
    }

    @Test fun keysAreUnique() {
        val keys = Keys.all.map { it.key }
        assertEquals(keys.size, keys.toSet().size)
    }
}

class UserDataTest {
    @Test fun recordAndRoundTrip() {
        var d = UserData()
            .recordPlayback("u1", "/v", 50_000, 100_000, 1)
            .createPlaylist("Mine", false, 7)
            .addToPlaylist(7, listOf("u1", "u2", "u1"))
            .addBookmark("u1", 30_000, 2)
            .addSearch("avatar")
        d = d.copy(selections = mapOf("u1" to FileSelections(audioTrack = "hi", speed = 1.5f, subtitleSyncMs = -200)))
        assertEquals(listOf("u1", "u2"), d.playlists.single().uris)
        assertEquals(d, UserData.decode(d.encode()))
    }

    @Test fun finishedPlaybackResetsPosition() {
        val d = UserData().recordPlayback("u", "/v", 99_000, 100_000, 1)
        assertTrue(d.history["u"]!!.finished)
        assertEquals(0L, d.history["u"]!!.positionMs)
    }

    @Test fun moveAndForgetFile() {
        val d = UserData().recordPlayback("a", "/v", 50_000, 100_000, 1).createPlaylist("p", false, 1).addToPlaylist(1, listOf("a"))
        val moved = d.moveFile("a", "b")
        assertEquals(setOf("b"), moved.history.keys)
        assertEquals(listOf("b"), moved.playlists.single().uris)
        assertEquals("b", moved.lastPlayedUri)
        val gone = moved.forgetFile("b")
        assertTrue(gone.history.isEmpty())
        assertNull(gone.lastPlayedUri)
    }
}
