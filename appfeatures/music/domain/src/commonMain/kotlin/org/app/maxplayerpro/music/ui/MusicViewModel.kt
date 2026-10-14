package org.app.maxplayerpro.music.ui

import androidx.lifecycle.viewModelScope
import com.psbapp.mvi.MVIBaseViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.app.kmediaplayer.util.nowMs
import org.app.maxplayerpro.sharedlogic.data.MediaRepository
import org.app.maxplayerpro.sharedlogic.data.UserDataRepository
import org.app.maxplayerpro.sharedlogic.usecase.MediaOperations

class MusicViewModel(
    private val userData: UserDataRepository,
    private val media: MediaRepository,
    private val operations: MediaOperations,
) : MVIBaseViewModel<MusicContract.Event, MusicContract.State, MusicContract.Effect>() {

    override fun createInitialState() = MusicContract.State()
    override fun handleEffect(effect: MusicContract.Effect) {}

    init {
        viewModelScope.launch {
            combine(media.audio, userData.data) { audio, user ->
                MusicContract.State(audio, user.favourites, user.history, user.playlists.filter { it.isMusic })
            }.collect { s -> setState { s } }
        }
    }

    override fun handleEvent(event: MusicContract.Event) {
        when (event) {
            is MusicContract.Event.Play -> operations.playMusic(event.files, event.index, event.shuffle)
            is MusicContract.Event.ToggleFavourite -> userData.update { it.toggleFavourite(event.uri) }
            is MusicContract.Event.CreatePlaylist -> userData.update { it.createPlaylist(event.name, true, nowMs()) }
            is MusicContract.Event.AddToPlaylist -> userData.update { it.addToPlaylist(event.playlistId, event.files.map { f -> f.uri }) }
            is MusicContract.Event.CreatePlaylistWith -> {
                val id = nowMs()
                userData.update { it.createPlaylist(event.name, true, id).addToPlaylist(id, event.files.map { f -> f.uri }) }
            }
            MusicContract.Event.Refresh -> viewModelScope.launch { media.refresh() }
        }
    }
}
