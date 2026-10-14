package org.app.maxplayerpro.playlists.ui

import androidx.lifecycle.viewModelScope
import com.psbapp.mvi.MVIBaseViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.app.kmediaplayer.util.nowMs
import org.app.maxplayerpro.sharedlogic.data.MediaRepository
import org.app.maxplayerpro.sharedlogic.data.UserDataRepository
import org.app.maxplayerpro.sharedlogic.usecase.MediaOperations

/** The video playlists. */
class PlaylistsViewModel(private val userData: UserDataRepository) :
    MVIBaseViewModel<PlaylistsContract.Event, PlaylistsContract.State, PlaylistsContract.Effect>() {

    override fun createInitialState() = PlaylistsContract.State()
    override fun handleEffect(effect: PlaylistsContract.Effect) {}

    init {
        viewModelScope.launch { userData.data.collect { d -> setState { copy(playlists = d.playlists.filter { !it.isMusic }) } } }
    }

    override fun handleEvent(event: PlaylistsContract.Event) {
        when (event) {
            is PlaylistsContract.Event.Create -> userData.update { it.createPlaylist(event.name, false, nowMs()) }
        }
    }
}

/** One playlist and the library files it still points to. */
class PlaylistDetailViewModel(
    private val id: Long,
    private val userData: UserDataRepository,
    media: MediaRepository,
    private val operations: MediaOperations,
) : MVIBaseViewModel<PlaylistDetailContract.Event, PlaylistDetailContract.State, PlaylistDetailContract.Effect>() {

    override fun createInitialState() = PlaylistDetailContract.State()
    override fun handleEffect(effect: PlaylistDetailContract.Effect) {}

    init {
        viewModelScope.launch {
            combine(userData.data, media.videos, media.audio) { d, videos, audio ->
                val p = d.playlists.firstOrNull { it.id == id }
                val byUri = (videos + audio).associateBy { it.uri }
                PlaylistDetailContract.State(p, p?.uris?.mapNotNull { byUri[it] }.orEmpty())
            }.collect { s -> setState { s } }
        }
    }

    override fun handleEvent(event: PlaylistDetailContract.Event) {
        when (event) {
            is PlaylistDetailContract.Event.Play -> if (currentState.files.isNotEmpty()) operations.play(currentState.files, event.index)
            is PlaylistDetailContract.Event.Rename -> userData.update { it.renamePlaylist(id, event.name) }
            PlaylistDetailContract.Event.Delete -> { userData.update { it.deletePlaylist(id) }; setEffect(PlaylistDetailContract.Effect.Deleted) }
            is PlaylistDetailContract.Event.Remove -> userData.update { it.removeFromPlaylist(id, event.uri) }
        }
    }
}
