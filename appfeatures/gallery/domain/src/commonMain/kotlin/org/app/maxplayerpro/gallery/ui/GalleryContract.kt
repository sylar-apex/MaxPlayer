package org.app.maxplayerpro.gallery.ui

import com.psbapp.mvi.UiEffect
import com.psbapp.mvi.UiEvent
import com.psbapp.mvi.UiState
import org.app.kmediaplayer.model.MediaFile

class GalleryContract {
    sealed class Event : UiEvent {
        data object Refresh : Event()
        data class Share(val photos: List<MediaFile>, val folderPath: String? = null) : Event()
        data class Delete(val photos: List<MediaFile>) : Event()
    }

    /** Photos of one folder: the Gallery's albums. */
    data class Album(val path: String, val photos: List<MediaFile>) {
        val name: String get() = path.substringAfterLast('/').ifEmpty { path }
    }

    data class State(val albums: List<Album> = emptyList(), val loading: Boolean = false) : UiState

    sealed class Effect : UiEffect
}
