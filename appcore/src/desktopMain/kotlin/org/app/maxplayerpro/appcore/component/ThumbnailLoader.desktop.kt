package org.app.maxplayerpro.appcore.component

import androidx.compose.ui.graphics.ImageBitmap
import org.app.kmediaplayer.model.MediaFile
import org.app.maxplayerpro.sharedlogic.data.ThumbnailCache

actual object ThumbnailLoader : ThumbnailCache {
    actual suspend fun load(file: MediaFile, px: Int): ImageBitmap? = null
    actual override fun clear() {}
}
