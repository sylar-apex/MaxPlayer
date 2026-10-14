package org.app.maxplayerpro.search.ui

import com.psbapp.mvi.UiEffect
import com.psbapp.mvi.UiEvent
import com.psbapp.mvi.UiState
import org.app.kmediaplayer.model.MediaFile
import org.app.maxplayerpro.sharedlogic.model.ListViewOptions

class SearchContract {
    sealed class Event : UiEvent {
        data class QueryChanged(val query: String) : Event()
        /** The keyboard search action: remembers [query] in the search history. */
        data class Submit(val query: String) : Event()
    }

    data class State(
        val query: String = "",
        val results: List<MediaFile> = emptyList(),
        val history: List<String> = emptyList(),
        val options: ListViewOptions = ListViewOptions(),
    ) : UiState

    sealed class Effect : UiEffect
}
