package org.app.maxplayerpro.theme.ui

import androidx.lifecycle.viewModelScope
import com.psbapp.mvi.MVIBaseViewModel
import kotlinx.coroutines.launch
import org.app.kmediaplayer.settings.ObservableSettings
import org.app.kmediaplayer.settings.PlayerKeys
import org.app.maxplayerpro.sharedlogic.settings.AppKeys

class ThemeViewModel(private val settings: ObservableSettings) :
    MVIBaseViewModel<ThemeContract.Event, ThemeContract.State, ThemeContract.Effect>() {

    override fun createInitialState() = ThemeContract.State()
    override fun handleEffect(effect: ThemeContract.Effect) {}

    private fun read() = ThemeContract.State(settings[AppKeys.appTheme], settings[PlayerKeys.accentColor], AppKeys.appTheme.choices)

    init {
        viewModelScope.launch { settings.version.collect { setState { read() } } }
    }

    override fun handleEvent(event: ThemeContract.Event) {
        when (event) {
            is ThemeContract.Event.SelectMode -> settings[AppKeys.appTheme] = event.key
            is ThemeContract.Event.SelectAccent -> settings[PlayerKeys.accentColor] = event.color
        }
    }
}
