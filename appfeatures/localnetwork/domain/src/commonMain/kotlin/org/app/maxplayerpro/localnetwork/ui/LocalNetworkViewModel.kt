package org.app.maxplayerpro.localnetwork.ui

import com.psbapp.mvi.MVIBaseViewModel
import org.app.maxplayerpro.sharedlogic.usecase.MediaOperations

class LocalNetworkViewModel(private val operations: MediaOperations) :
    MVIBaseViewModel<LocalNetworkContract.Event, LocalNetworkContract.State, LocalNetworkContract.Effect>() {

    override fun createInitialState() = LocalNetworkContract.State
    override fun handleEffect(effect: LocalNetworkContract.Effect) {}

    override fun handleEvent(event: LocalNetworkContract.Event) {
        when (event) {
            LocalNetworkContract.Event.AddServer -> operations.toast("SMB/LAN servers are not supported yet")
        }
    }
}
