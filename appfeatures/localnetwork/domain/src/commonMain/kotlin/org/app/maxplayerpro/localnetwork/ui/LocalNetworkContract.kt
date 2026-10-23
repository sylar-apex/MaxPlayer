package org.app.maxplayerpro.localnetwork.ui

import com.psbapp.mvi.UiEffect
import com.psbapp.mvi.UiEvent
import com.psbapp.mvi.UiState
import org.app.maxplayerpro.localnetwork.share.HostState
import org.app.maxplayerpro.localnetwork.share.NearbyDevice
import org.app.maxplayerpro.localnetwork.share.RemoteShare
import org.app.maxplayerpro.localnetwork.share.Share

class LocalNetworkContract {
    sealed class Event : UiEvent {
        data object StartHosting : Event()
        data object StopHosting : Event()
        data class RevokeShare(val id: String) : Event()
        data class DeleteShare(val id: String) : Event()
        data class AddAddress(val address: String) : Event()
        data class RemoveAddress(val baseUrl: String) : Event()
        data class OpenDevice(val name: String, val baseUrl: String) : Event()
        data object CloseDevice : Event()
        data object RefreshDevice : Event()
        data class Unlock(val share: RemoteShare, val code: String) : Event()
        data class Play(val share: RemoteShare, val index: Int) : Event()
    }

    /** A device whose shares are open on screen; [unlocked] holds coded shares opened this session. */
    data class Browsing(
        val name: String,
        val baseUrl: String,
        val loading: Boolean = true,
        val error: String? = null,
        val shares: List<RemoteShare> = emptyList(),
        val unlocked: Map<String, Unlocked> = emptyMap(),
        val unlockError: Map<String, String> = emptyMap(),
    )

    data class Unlocked(val token: String, val share: RemoteShare)

    data class State(
        val canHost: Boolean = false,
        val canDiscover: Boolean = false,
        val host: HostState = HostState.Stopped,
        val shares: List<Share> = emptyList(),
        val nearby: List<NearbyDevice> = emptyList(),
        val savedAddresses: List<String> = emptyList(),
        val browsing: Browsing? = null,
        val nowMs: Long = 0,
    ) : UiState

    sealed class Effect : UiEffect
}
