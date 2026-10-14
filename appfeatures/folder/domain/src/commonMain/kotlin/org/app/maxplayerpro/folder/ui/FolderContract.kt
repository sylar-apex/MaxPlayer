package org.app.maxplayerpro.folder.ui

import com.psbapp.mvi.UiEffect
import com.psbapp.mvi.UiEvent
import com.psbapp.mvi.UiState
import org.app.kmediaplayer.model.MediaFile
import org.app.maxplayerpro.sharedlogic.model.ListViewOptions

class FolderContract {
    sealed class Event : UiEvent {
        data class SaveViewOptions(val options: ListViewOptions) : Event()
    }

    data class State(
        val title: String = "",
        val options: ListViewOptions = ListViewOptions(),
        val files: List<MediaFile> = emptyList(),
        val lastPlayed: String? = null,
    ) : UiState

    sealed class Effect : UiEffect
}
