package org.app.maxplayerpro.sharedlogic.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.app.kmediaplayer.model.MediaFile

/** Photos from the system media index (Camera, Screenshots, downloads, …). */
class AndroidPhotoRepository(private val context: Context) : PhotoRepository {
    private val _photos = MutableStateFlow<List<MediaFile>>(emptyList())
    override val photos: StateFlow<List<MediaFile>> = _photos.asStateFlow()
    private val _loading = MutableStateFlow(false)
    override val loading: StateFlow<Boolean> = _loading.asStateFlow()

    override suspend fun refresh() = withContext(Dispatchers.IO) {
        _loading.value = true
        try {
            val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val cols = arrayOf(
                MediaStore.Images.Media._ID, MediaStore.Images.Media.DATA, MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.DATE_MODIFIED, MediaStore.Images.Media.WIDTH, MediaStore.Images.Media.HEIGHT, MediaStore.Images.Media.MIME_TYPE,
            )
            val list = mutableListOf<MediaFile>()
            runCatching {
                context.contentResolver.query(uri, cols, null, null, "${MediaStore.Images.Media.DATE_MODIFIED} DESC")?.use { c ->
                    while (c.moveToNext()) {
                        val id = c.getLong(0)
                        val path = c.getString(1) ?: continue
                        list += MediaFile(
                            id = id, uri = ContentUris.withAppendedId(uri, id).toString(), path = path, name = c.getString(2) ?: path.substringAfterLast('/'),
                            sizeBytes = c.getLong(3), durationMs = 0, dateModifiedMs = c.getLong(4) * 1000, width = c.getInt(5), height = c.getInt(6),
                            mimeType = c.getString(7) ?: imageMime(path),
                        )
                    }
                }
            }
            _photos.value = list
        } finally {
            _loading.value = false
        }
    }
}
