package org.app.maxplayerpro.sharedlogic.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.app.kmediaplayer.model.MediaFile
import org.app.maxplayerpro.sharedlogic.scan.MediaFileWalker
import org.app.maxplayerpro.sharedlogic.scan.StorageRoots
import java.io.File

/** Photos found on the same drives and folders the video library scans. */
class DesktopPhotoRepository : PhotoRepository {
    private val _photos = MutableStateFlow<List<MediaFile>>(emptyList())
    override val photos: StateFlow<List<MediaFile>> = _photos.asStateFlow()
    private val _loading = MutableStateFlow(false)
    override val loading: StateFlow<Boolean> = _loading.asStateFlow()
    private val lock = Mutex()
    // Images take the walker's "video" slot; small files (icons, thumbnails) are skipped.
    private val walker = MediaFileWalker(StorageRoots.skipFolderNames, videoExtensions = imageExtensions, audioExtensions = emptySet(), minFileBytes = 30 * 1024)

    override suspend fun refresh() = lock.withLock {
        _loading.value = true
        try {
            withContext(Dispatchers.IO) {
                coroutineScope {
                    val found = StorageRoots.current().flatMap { root -> walker.walk(File(root.path)) { !isActive } }
                    _photos.value = found.map { it.copy(mimeType = imageMime(it.name)) }.distinctBy { it.path }.sortedByDescending { it.dateModifiedMs }
                }
            }
        } finally {
            _loading.value = false
        }
    }
}
