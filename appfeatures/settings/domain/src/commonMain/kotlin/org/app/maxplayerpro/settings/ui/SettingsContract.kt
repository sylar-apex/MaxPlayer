package org.app.maxplayerpro.settings.ui

import com.psbapp.mvi.UiEffect
import com.psbapp.mvi.UiEvent
import com.psbapp.mvi.UiState

class SettingsContract {
    sealed class Event : UiEvent {
        /** A setting row changed a value the player only reads when it is built. */
        data object SettingChanged : Event()
        data object ClearHistory : Event()
        data object ClearThumbnailCache : Event()
        data object ResetSettings : Event()
        data object Export : Event()
        data object Import : Event()
        data object OpenLanguageSettings : Event()
    }

    data class State(val version: Int = 0) : UiState

    sealed class Effect : UiEffect
}
