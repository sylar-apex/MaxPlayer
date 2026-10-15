package org.app.maxplayerpro.music.ui

import com.psbapp.mvi.UiEffect
import com.psbapp.mvi.UiEvent
import com.psbapp.mvi.UiState
import org.app.kmediaplayer.model.MediaFile
import org.app.kmediaplayer.model.PlaybackRecord
import org.app.maxplayerpro.sharedlogic.model.Playlist

class MusicContract {
    sealed class Event : UiEvent {
        data class Play(val files: List<MediaFile>, val index: Int, val shuffle: Boolean = false) : Event()
        data class ToggleFavourite(val uri: String) : Event()
        data class CreatePlaylist(val name: String) : Event()
        data class AddToPlaylist(val playlistId: Long, val files: List<MediaFile>) : Event()
        data class CreatePlaylistWith(val name: String, val files: List<MediaFile>) : Event()
        data object Refresh : Event()
    }

    data class State(
        val audio: List<MediaFile> = emptyList(),
        val favourites: Set<String> = emptySet(),
        val history: Map<String, PlaybackRecord> = emptyMap(),
        val playlists: List<Playlist> = emptyList(),
        /** True until the first library scan finishes; partial results stay hidden meanwhile. */
        val scanning: Boolean = true,
    ) : UiState

    sealed class Effect : UiEffect
}
