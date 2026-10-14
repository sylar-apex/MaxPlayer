package org.app.maxplayerpro.sharedlogic.ui.filelist

import androidx.lifecycle.viewModelScope
import com.psbapp.mvi.MVIBaseViewModel
import com.psbapp.utils.dispatcher.DispatcherProvider
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.app.kmediaplayer.settings.ObservableSettings
import org.app.kmediaplayer.util.nowMs
import org.app.maxplayerpro.sharedlogic.data.MediaRepository
import org.app.maxplayerpro.sharedlogic.data.UserDataRepository
import org.app.maxplayerpro.sharedlogic.settings.AppKeys
import org.app.maxplayerpro.sharedlogic.usecase.MediaOperations

/** State and actions of the shared media file list: selection actions, playlists and file operations. */
class FileListViewModel(
    private val dispatcher: DispatcherProvider,
    private val settings: ObservableSettings,
    private val userData: UserDataRepository,
    private val media: MediaRepository,
    private val operations: MediaOperations,
) : MVIBaseViewModel<FileListContract.Event, FileListContract.State, FileListContract.Effect>() {

    override fun createInitialState() = FileListContract.State()
    override fun handleEffect(effect: FileListContract.Effect) {}

    init {
        viewModelScope.launch {
            combine(userData.data, settings.version) { data, _ -> data }.collect { data ->
                setState {
                    copy(
                        history = data.history, playlists = data.playlists,
                        newTaggedPeriod = settings[AppKeys.newTaggedPeriod], markLastPlayed = settings[AppKeys.markLastPlayed],
                        scrollToLast = settings[AppKeys.scrollToLast], selectThumbnail = settings[AppKeys.selectThumbnail],
                        allowEdits = settings[AppKeys.allowEdits],
                    )
                }
            }
        }
    }

    override fun handleEvent(event: FileListContract.Event) {
        when (event) {
            is FileListContract.Event.FilesShown -> viewModelScope.launch {
                val names = withContext(dispatcher.io) { media.folderFileNames(event.files.map { it.folderPath }.distinct()) }
                setState { copy(siblingNames = names) }
            }
            is FileListContract.Event.Play -> operations.play(event.files, event.index)
            is FileListContract.Event.Share -> operations.share(event.files)
            is FileListContract.Event.LockInPrivate -> viewModelScope.launch { operations.lockInPrivate(event.files) }
            is FileListContract.Event.Rename -> viewModelScope.launch { operations.rename(event.file, event.newName) }
            is FileListContract.Event.Delete -> viewModelScope.launch { operations.delete(event.files) }
            is FileListContract.Event.AddToPlaylist -> userData.update { it.addToPlaylist(event.playlistId, event.files.map { f -> f.uri }) }
            is FileListContract.Event.CreatePlaylist -> {
                val id = nowMs()
                userData.update { it.createPlaylist(event.name, event.music, id).addToPlaylist(id, event.files.map { f -> f.uri }) }
            }
            is FileListContract.Event.Toast -> operations.toast(event.message)
        }
    }
}
