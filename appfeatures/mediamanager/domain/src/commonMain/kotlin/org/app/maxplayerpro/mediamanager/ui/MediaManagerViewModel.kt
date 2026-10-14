package org.app.maxplayerpro.mediamanager.ui

import androidx.lifecycle.viewModelScope
import com.psbapp.mvi.MVIBaseViewModel
import com.psbapp.utils.dispatcher.DispatcherProvider
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.app.kmediaplayer.settings.ObservableSettings
import org.app.maxplayerpro.mediamanager.domain.model.MediaListKind
import org.app.maxplayerpro.sharedlogic.data.MediaRepository
import org.app.maxplayerpro.sharedlogic.data.UserDataRepository
import org.app.maxplayerpro.sharedlogic.model.ListField
import org.app.maxplayerpro.sharedlogic.model.ListLayout
import org.app.maxplayerpro.sharedlogic.model.ListViewOptions
import org.app.maxplayerpro.sharedlogic.settings.AppKeys
import org.app.maxplayerpro.sharedlogic.usecase.MediaOperations

/** Storage usage and quick lists of the media library. */
class MediaManagerViewModel(
    private val dispatcher: DispatcherProvider,
    private val media: MediaRepository,
    userData: UserDataRepository,
    private val operations: MediaOperations,
) : MVIBaseViewModel<MediaManagerContract.Event, MediaManagerContract.State, MediaManagerContract.Effect>() {

    override fun createInitialState() = MediaManagerContract.State()
    override fun handleEffect(effect: MediaManagerContract.Effect) {}

    init {
        viewModelScope.launch {
            media.videos.collect {
                val summary = withContext(dispatcher.io) { media.storageSummary() }
                setState { copy(summary = summary) }
            }
        }
        viewModelScope.launch {
            combine(media.videos, userData.data) { videos, user -> videos.filter { user.history[it.uri] == null }.take(4) }
                .collect { u -> setState { copy(unplayedPreview = u) } }
        }
    }

    override fun handleEvent(event: MediaManagerContract.Event) {
        when (event) {
            is MediaManagerContract.Event.Play -> operations.play(listOf(event.file), 0)
        }
    }
}

/** One of the media manager's quick lists. */
class MediaListViewModel(
    private val kind: MediaListKind,
    dispatcher: DispatcherProvider,
    settings: ObservableSettings,
    media: MediaRepository,
    userData: UserDataRepository,
) : MVIBaseViewModel<MediaListContract.Event, MediaListContract.State, MediaListContract.Effect>() {

    override fun createInitialState() = MediaListContract.State()
    override fun handleEvent(event: MediaListContract.Event) {}
    override fun handleEffect(effect: MediaListContract.Effect) {}

    init {
        setState { copy(kind = kind) }
        viewModelScope.launch {
            combine(media.videos, userData.data, settings.version) { videos, user, _ ->
                val files = when (kind) {
                    MediaListKind.Recent -> videos.filter { (user.history[it.uri]?.lastPlayedMs ?: 0) > 0 }.sortedByDescending { user.history[it.uri]!!.lastPlayedMs }
                    MediaListKind.Large -> videos.filter { it.sizeBytes > 100L * 1024 * 1024 }.sortedByDescending { it.sizeBytes }
                    MediaListKind.Added -> videos.sortedByDescending { it.dateModifiedMs }.take(50)
                    MediaListKind.Unplayed -> videos.filter { user.history[it.uri] == null }
                }
                val opts = ListViewOptions.decode(settings[AppKeys.listViewOptions])
                    .copy(layout = ListLayout.List, fields = setOf(ListField.Thumbnail, ListField.Length, ListField.Size))
                MediaListContract.State(kind, files, opts)
            }.flowOn(dispatcher.default).collect { s -> setState { s } }
        }
    }
}
