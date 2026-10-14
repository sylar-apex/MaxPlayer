package org.app.maxplayerpro.folder.ui

import androidx.lifecycle.viewModelScope
import com.psbapp.mvi.MVIBaseViewModel
import com.psbapp.utils.dispatcher.DispatcherProvider
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import org.app.kmediaplayer.settings.ObservableSettings
import org.app.maxplayerpro.sharedlogic.data.MediaRepository
import org.app.maxplayerpro.sharedlogic.data.UserDataRepository
import org.app.maxplayerpro.sharedlogic.model.ListViewOptions
import org.app.maxplayerpro.sharedlogic.model.filterVisible
import org.app.maxplayerpro.sharedlogic.model.sortFiles
import org.app.maxplayerpro.sharedlogic.settings.AppKeys

/** The videos directly inside the folder at [path]. */
class FolderViewModel(
    private val path: String,
    dispatcher: DispatcherProvider,
    private val settings: ObservableSettings,
    userData: UserDataRepository,
    media: MediaRepository,
) : MVIBaseViewModel<FolderContract.Event, FolderContract.State, FolderContract.Effect>() {

    override fun createInitialState() = FolderContract.State()
    override fun handleEffect(effect: FolderContract.Effect) {}

    init {
        setState { copy(title = path.substringAfterLast('/').replaceFirstChar { it.uppercaseChar() }) }
        viewModelScope.launch {
            combine(media.videos, media.noMediaDirs, userData.data, settings.version) { videos, noMedia, user, _ ->
                val opts = ListViewOptions.decode(settings[AppKeys.listViewOptions])
                currentState.copy(
                    options = opts,
                    files = sortFiles(filterVisible(videos.filter { it.folderPath == path }, opts, noMedia), opts.sortKey, opts.ascending, user.history),
                    lastPlayed = user.lastPlayedPerFolder[path],
                )
            }.flowOn(dispatcher.default).collect { s -> setState { s } }
        }
    }

    override fun handleEvent(event: FolderContract.Event) {
        when (event) {
            is FolderContract.Event.SaveViewOptions -> settings[AppKeys.listViewOptions] = event.options.encode()
        }
    }
}
