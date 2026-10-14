package org.app.maxplayerpro.search.ui

import androidx.lifecycle.viewModelScope
import com.psbapp.mvi.MVIBaseViewModel
import com.psbapp.utils.dispatcher.DispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import org.app.kmediaplayer.settings.ObservableSettings
import org.app.maxplayerpro.sharedlogic.data.MediaRepository
import org.app.maxplayerpro.sharedlogic.data.UserDataRepository
import org.app.maxplayerpro.sharedlogic.model.ListViewOptions
import org.app.maxplayerpro.sharedlogic.model.sortFiles
import org.app.maxplayerpro.sharedlogic.settings.AppKeys

class SearchViewModel(
    dispatcher: DispatcherProvider,
    private val settings: ObservableSettings,
    private val userData: UserDataRepository,
    media: MediaRepository,
) : MVIBaseViewModel<SearchContract.Event, SearchContract.State, SearchContract.Effect>() {

    private val query = MutableStateFlow("")

    override fun createInitialState() = SearchContract.State()
    override fun handleEffect(effect: SearchContract.Effect) {}

    init {
        viewModelScope.launch {
            combine(query, media.videos, userData.data, settings.version) { q, videos, user, _ ->
                val opts = ListViewOptions.decode(settings[AppKeys.listViewOptions])
                SearchContract.State(
                    query = q,
                    results = if (q.isBlank()) emptyList() else sortFiles(videos.filter { it.name.contains(q, ignoreCase = true) }, opts.sortKey, opts.ascending, user.history),
                    history = user.searchHistory,
                    options = opts,
                )
            }.flowOn(dispatcher.default).collect { s -> setState { s } }
        }
    }

    override fun handleEvent(event: SearchContract.Event) {
        when (event) {
            is SearchContract.Event.QueryChanged -> query.value = event.query
            is SearchContract.Event.Submit -> userData.update { it.addSearch(event.query) }
        }
    }
}
