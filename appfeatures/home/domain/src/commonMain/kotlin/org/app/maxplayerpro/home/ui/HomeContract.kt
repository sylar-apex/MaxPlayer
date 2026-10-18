package org.app.maxplayerpro.home.ui

import com.psbapp.mvi.UiEffect
import com.psbapp.mvi.UiEvent
import com.psbapp.mvi.UiState
import org.app.kmediaplayer.model.MediaFile
import org.app.maxplayerpro.sharedlogic.model.ListViewOptions
import org.app.maxplayerpro.sharedlogic.model.MediaFolder

class HomeContract {
    sealed class Event : UiEvent {
        data class SaveViewOptions(val options: ListViewOptions) : Event()
        data object PlayLast : Event()
        /** Rescans the library (drives, media index or remembered browser folders). */
        data object Refresh : Event()
    }

    data class State(
        val options: ListViewOptions = ListViewOptions(),
        val folders: List<MediaFolder> = emptyList(),
        /** Every visible video, sorted; shown when the view mode is Files. */
        val files: List<MediaFile> = emptyList(),
        val loading: Boolean = false,
        /** The last played file, when the floating "play last" button is enabled. */
        val lastPlayed: MediaFile? = null,
        /** Paths of folders holding at least one unplayed video tagged "NEW". */
        val newFolders: Set<String> = emptySet(),
    ) : UiState

    sealed class Effect : UiEffect
}
