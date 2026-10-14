package org.app.maxplayerpro.profile.ui

import com.psbapp.mvi.UiEffect
import com.psbapp.mvi.UiEvent
import com.psbapp.mvi.UiState

class ProfileContract {
    sealed class Event : UiEvent {
        data object Quit : Event()
    }

    data class State(val showQuit: Boolean = false) : UiState

    sealed class Effect : UiEffect
}
