package org.app.maxplayerpro.localnetwork.ui

import com.psbapp.mvi.UiEffect
import com.psbapp.mvi.UiEvent
import com.psbapp.mvi.UiState
import org.app.maxplayerpro.localnetwork.share.HostState
import org.app.maxplayerpro.localnetwork.share.RemoteShare
import org.app.maxplayerpro.localnetwork.share.Share

class LocalNetworkContract {
    enum class Tab(val label: String) { SharedWithMe("Shared with me"), SharedByMe("Shared by me") }

    sealed class Event : UiEvent {
        data class SelectTab(val tab: Tab) : Event()
        data object StartHosting : Event()
        data object StopHosting : Event()
        data class RevokeShare(val id: String) : Event()
        data class DeleteShare(val id: String) : Event()
        data class AddAddress(val address: String) : Event()
        data class RemoveAddress(val baseUrl: String) : Event()
        data object Refresh : Event()
        data class Unlock(val baseUrl: String, val share: RemoteShare, val code: String) : Event()
        data class Play(val baseUrl: String, val share: RemoteShare, val index: Int) : Event()
    }

    /** Another device and what it shares; [unlocked] holds coded shares opened this session. */
    data class RemoteDevice(
        val name: String,
        val platform: String,
        val baseUrl: String,
        /** True when found on the network, false when added by address. */
        val discovered: Boolean,
        val loading: Boolean = true,
        val error: String? = null,
        val shares: List<RemoteShare> = emptyList(),
        val unlocked: Map<String, Unlocked> = emptyMap(),
        val unlockError: Map<String, String> = emptyMap(),
    )

    data class Unlocked(val token: String, val share: RemoteShare)

    data class State(
        val tab: Tab = Tab.SharedWithMe,
        val canHost: Boolean = false,
        val canDiscover: Boolean = false,
        val host: HostState = HostState.Stopped,
        val shares: List<Share> = emptyList(),
        val devices: List<RemoteDevice> = emptyList(),
        val nowMs: Long = 0,
    ) : UiState

    sealed class Effect : UiEffect
}
