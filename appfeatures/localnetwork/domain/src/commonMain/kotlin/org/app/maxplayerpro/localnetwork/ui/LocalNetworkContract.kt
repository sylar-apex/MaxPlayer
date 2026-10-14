package org.app.maxplayerpro.localnetwork.ui

import com.psbapp.mvi.UiEffect
import com.psbapp.mvi.UiEvent
import com.psbapp.mvi.UiState

class LocalNetworkContract {
    sealed class Event : UiEvent {
        data object AddServer : Event()
    }

    data object State : UiState

    sealed class Effect : UiEffect
}
