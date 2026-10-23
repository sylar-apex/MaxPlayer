package org.app.maxplayerpro.sharedlogic.data

import kotlinx.coroutines.flow.StateFlow
import org.app.kmediaplayer.model.MediaFile
import org.app.kmediaplayer.model.SubtitleRef
import org.app.kmediaplayer.settings.KeyValueStore
import org.app.maxplayerpro.sharedlogic.model.PrivateFile
import org.app.maxplayerpro.sharedlogic.model.StorageSummary

/** The device media library. Deletes and renames ask the user for consent where the platform requires it. */
interface MediaRepository {
    val videos: StateFlow<List<MediaFile>>
    val audio: StateFlow<List<MediaFile>>
    /** Directories containing a `.nomedia` file. */
    val noMediaDirs: StateFlow<Set<String>>
    val loading: StateFlow<Boolean>

    suspend fun refresh()
    fun findByUri(uri: String): MediaFile?
    fun subtitleFilesFor(file: MediaFile): List<SubtitleRef>
    /** Names of every file inside [folders], used to tag videos that have subtitles. */
    fun folderFileNames(folders: Collection<String>): Set<String>
    suspend fun delete(files: List<MediaFile>, withSubtitles: Boolean): Boolean
    suspend fun rename(file: MediaFile, newName: String): Boolean
    suspend fun storageSummary(): StorageSummary?
}

/** Photos on the device, for the Gallery. Deletes go through [MediaRepository] (same consent prompts). */
interface PhotoRepository {
    val photos: StateFlow<List<MediaFile>>
    val loading: StateFlow<Boolean>
    suspend fun refresh()
}

/** Platforms without a photo library here. */
class NoPhotos : PhotoRepository {
    override val photos: StateFlow<List<MediaFile>> = kotlinx.coroutines.flow.MutableStateFlow(emptyList<MediaFile>())
    override val loading: StateFlow<Boolean> = kotlinx.coroutines.flow.MutableStateFlow(false)
    override suspend fun refresh() {}
}

/** Image file extensions the Gallery shows. */
val imageExtensions = setOf("jpg", "jpeg", "png", "gif", "webp", "heic", "heif", "bmp", "avif")

fun imageMime(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
    "jpg", "jpeg" -> "image/jpeg"; "png" -> "image/png"; "gif" -> "image/gif"; "webp" -> "image/webp"
    "heic", "heif" -> "image/heif"; "bmp" -> "image/bmp"; "avif" -> "image/avif"; else -> "image/*"
}

/** Files moved into app-private storage, protected by a PIN. */
interface PrivateFolderRepository {
    val files: StateFlow<List<PrivateFile>>
    val hasPin: Boolean
    fun setPin(pin: String)
    fun checkPin(pin: String): Boolean
    /** Copies [file] into the private folder. The caller removes the original afterwards. */
    suspend fun lockIn(file: MediaFile): Boolean
    /** Moves [file] back to public storage. */
    suspend fun restore(file: PrivateFile): Boolean
    fun delete(file: PrivateFile)
    fun rename(file: PrivateFile, name: String): Boolean
}

/** Cached video thumbnails, owned by the UI layer. */
interface ThumbnailCache {
    fun clear()
}

/** Opens named key-value files on the current platform. */
fun interface StoreFactory {
    fun open(name: String): KeyValueStore
}
