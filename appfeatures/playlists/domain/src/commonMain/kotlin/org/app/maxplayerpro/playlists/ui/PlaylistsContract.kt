package org.app.maxplayerpro.playlists.ui

import com.psbapp.mvi.UiEffect
import com.psbapp.mvi.UiEvent
import com.psbapp.mvi.UiState
import org.app.kmediaplayer.model.MediaFile
import org.app.maxplayerpro.sharedlogic.model.Playlist

class PlaylistsContract {
    sealed class Event : UiEvent {
        data class Create(val name: String) : Event()
    }

    data class State(val playlists: List<Playlist> = emptyList()) : UiState

    sealed class Effect : UiEffect
}

class PlaylistDetailContract {
    sealed class Event : UiEvent {
        data class Play(val index: Int) : Event()
        data class Rename(val name: String) : Event()
        data object Delete : Event()
        data class Remove(val uri: String) : Event()
    }

    data class State(
        /** Null once the playlist has been deleted. */
        val playlist: Playlist? = null,
        val files: List<MediaFile> = emptyList(),
    ) : UiState

    sealed class Effect : UiEffect {
        data object Deleted : Effect()
    }
}
