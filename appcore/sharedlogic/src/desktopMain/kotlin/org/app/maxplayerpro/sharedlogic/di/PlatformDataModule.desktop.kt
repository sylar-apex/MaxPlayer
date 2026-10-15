package org.app.maxplayerpro.sharedlogic.di

import org.app.kmediaplayer.model.MediaFile
import org.app.kmediaplayer.model.MediaFormats
import org.app.kmediaplayer.settings.KeyValueStore
import org.app.maxplayerpro.sharedlogic.data.MediaRepository
import org.app.maxplayerpro.sharedlogic.data.MemoryPrivateFolder
import org.app.maxplayerpro.sharedlogic.data.PrivateFolderRepository
import org.app.maxplayerpro.sharedlogic.data.SimpleMediaRepository
import org.app.maxplayerpro.sharedlogic.data.StoreFactory
import org.koin.core.module.Module
import org.koin.dsl.module
import java.io.File
import java.util.prefs.Preferences

private class PreferencesStore(private val node: Preferences) : KeyValueStore {
    override fun getString(key: String): String? = node.get(key, null)
    override fun putString(key: String, value: String?) { if (value == null) node.remove(key) else node.put(key, value) }
    override fun keys(): Set<String> = node.keys().toSet()
}

// libVLC plays all of these on desktop.
private val videoExtensions = setOf("mp4", "mkv", "webm", "avi", "mov", "m4v", "ts", "flv", "f4v", "3gp", "3g2", "mpg", "mpeg", "ogv") +
    MediaFormats.legacyVideoExtensions
private val audioExtensions = setOf("mp3", "m4a", "aac", "flac", "wav", "ogg", "opus", "alac", "aiff", "ac3", "eac3", "mka") +
    MediaFormats.legacyAudioExtensions

/** Scans the user's Movies, Videos and Music folders. */
private fun scanHome(): List<MediaFile> {
    val home = File(System.getProperty("user.home"))
    return listOf("Movies", "Videos", "Music", "Downloads").map { File(home, it) }.filter { it.isDirectory }
        .flatMap { root -> root.walkTopDown().maxDepth(4).filter { it.isFile }.toList() }
        .mapNotNull { f ->
            val ext = f.extension.lowercase()
            val audio = ext in audioExtensions
            if (!audio && ext !in videoExtensions) return@mapNotNull null
            MediaFile(f.path.hashCode().toLong(), f.toURI().toString(), f.path, f.name, f.length(), 0, f.lastModified(), isAudio = audio, artist = if (audio) "Unknown" else "", album = if (audio) "Unknown" else "")
        }
}

actual fun platformDataModule(): Module = module {
    single<StoreFactory> { StoreFactory { name -> PreferencesStore(Preferences.userRoot().node("org/app/maxplayerpro/$name")) } }
    single<MediaRepository> { SimpleMediaRepository(::scanHome) }
    single<PrivateFolderRepository> { MemoryPrivateFolder() }
}
