package org.app.maxplayerpro.help.ui

import com.psbapp.mvi.UiEffect
import com.psbapp.mvi.UiEvent
import com.psbapp.mvi.UiState
import org.app.maxplayerpro.help.domain.model.HelpTopic

class HelpContract {
    sealed class Event : UiEvent

    data class State(val topics: List<HelpTopic> = emptyList()) : UiState

    sealed class Effect : UiEffect
}
