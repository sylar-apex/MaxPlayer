package org.app.maxplayerpro.theme.ui

import com.psbapp.mvi.UiEffect
import com.psbapp.mvi.UiEvent
import com.psbapp.mvi.UiState

class ThemeContract {
    sealed class Event : UiEvent {
        data class SelectMode(val key: String) : Event()
        data class SelectAccent(val color: Long) : Event()
    }

    data class State(
        val mode: String = "dark",
        val accent: Long = 0,
        /** Theme modes as key to label. */
        val modes: List<Pair<String, String>> = emptyList(),
    ) : UiState

    sealed class Effect : UiEffect
}
