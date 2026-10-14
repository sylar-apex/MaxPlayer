package org.app.maxplayerpro.profile.ui

import androidx.lifecycle.viewModelScope
import com.psbapp.mvi.MVIBaseViewModel
import kotlinx.coroutines.launch
import org.app.kmediaplayer.KMediaPlayer
import org.app.kmediaplayer.settings.ObservableSettings
import org.app.maxplayerpro.sharedlogic.platform.PlatformApp
import org.app.maxplayerpro.sharedlogic.settings.AppKeys

class ProfileViewModel(private val settings: ObservableSettings) :
    MVIBaseViewModel<ProfileContract.Event, ProfileContract.State, ProfileContract.Effect>() {

    override fun createInitialState() = ProfileContract.State()
    override fun handleEffect(effect: ProfileContract.Effect) {}

    init {
        viewModelScope.launch { settings.version.collect { setState { copy(showQuit = settings[AppKeys.quitButton]) } } }
    }

    override fun handleEvent(event: ProfileContract.Event) {
        when (event) {
            ProfileContract.Event.Quit -> { KMediaPlayer.quit(); PlatformApp.quit() }
        }
    }
}
