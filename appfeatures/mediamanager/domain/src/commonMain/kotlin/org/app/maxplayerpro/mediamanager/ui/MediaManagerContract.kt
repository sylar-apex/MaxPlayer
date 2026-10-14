package org.app.maxplayerpro.mediamanager.ui

import com.psbapp.mvi.UiEffect
import com.psbapp.mvi.UiEvent
import com.psbapp.mvi.UiState
import org.app.kmediaplayer.model.MediaFile
import org.app.maxplayerpro.mediamanager.domain.model.MediaListKind
import org.app.maxplayerpro.sharedlogic.model.ListViewOptions
import org.app.maxplayerpro.sharedlogic.model.StorageSummary

class MediaManagerContract {
    sealed class Event : UiEvent {
        data class Play(val file: MediaFile) : Event()
    }

    data class State(
        val summary: StorageSummary? = null,
        /** First few videos never played. */
        val unplayedPreview: List<MediaFile> = emptyList(),
    ) : UiState

    sealed class Effect : UiEffect
}

class MediaListContract {
    sealed class Event : UiEvent

    data class State(
        val kind: MediaListKind = MediaListKind.Recent,
        val files: List<MediaFile> = emptyList(),
        val options: ListViewOptions = ListViewOptions(),
    ) : UiState

    sealed class Effect : UiEffect
}
