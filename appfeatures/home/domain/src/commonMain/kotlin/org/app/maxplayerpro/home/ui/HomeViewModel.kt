package org.app.maxplayerpro.home.ui

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
import org.app.maxplayerpro.sharedlogic.model.ViewMode
import org.app.maxplayerpro.sharedlogic.model.buildFolders
import org.app.maxplayerpro.sharedlogic.model.filterVisible
import org.app.maxplayerpro.sharedlogic.model.isNew
import org.app.kmediaplayer.util.nowMs
import org.app.maxplayerpro.sharedlogic.model.sortFiles
import org.app.maxplayerpro.sharedlogic.model.sortFolders
import org.app.maxplayerpro.sharedlogic.settings.AppKeys
import org.app.maxplayerpro.sharedlogic.usecase.MediaOperations

class HomeViewModel(
    dispatcher: DispatcherProvider,
    private val settings: ObservableSettings,
    private val userData: UserDataRepository,
    private val media: MediaRepository,
    private val operations: MediaOperations,
) : MVIBaseViewModel<HomeContract.Event, HomeContract.State, HomeContract.Effect>() {

    override fun createInitialState() = HomeContract.State()
    override fun handleEffect(effect: HomeContract.Effect) {}

    init {
        viewModelScope.launch {
            combine(media.videos, media.noMediaDirs, media.loading, userData.data, settings.version) { videos, noMedia, loading, user, _ ->
                val opts = ListViewOptions.decode(settings[AppKeys.listViewOptions])
                val now = nowMs()
                val period = settings[AppKeys.newTaggedPeriod]
                HomeContract.State(
                    options = opts,
                    folders = if (opts.viewMode == ViewMode.Files) emptyList() else sortFolders(buildFolders(videos, opts, noMedia), opts.sortKey, opts.ascending, user.history),
                    files = if (opts.viewMode == ViewMode.Files) sortFiles(filterVisible(videos, opts, noMedia), opts.sortKey, opts.ascending, user.history) else emptyList(),
                    loading = loading,
                    newFolders = videos.filter { isNew(it, now, period) && user.history[it.uri] == null }.mapTo(HashSet()) { it.folderPath },
                    lastPlayed = user.lastPlayedUri?.takeIf { settings[AppKeys.floatingButton] }?.let { media.findByUri(it) },
                )
            }.flowOn(dispatcher.default).collect { s -> setState { s } }
        }
    }

    override fun handleEvent(event: HomeContract.Event) {
        when (event) {
            is HomeContract.Event.SaveViewOptions -> settings[AppKeys.listViewOptions] = event.options.encode()
            HomeContract.Event.Refresh -> viewModelScope.launch { media.refresh() }
            is HomeContract.Event.DeleteFolders -> viewModelScope.launch { operations.delete(event.folders.flatMap { it.files }) }
            HomeContract.Event.PlayLast -> {
                val last = currentState.lastPlayed ?: return
                val opts = currentState.options
                val folder = sortFiles(media.videos.value.filter { it.folderPath == last.folderPath }, opts.sortKey, opts.ascending, userData.value.history)
                operations.play(folder, folder.indexOf(last).coerceAtLeast(0))
            }
        }
    }
}
