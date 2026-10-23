package org.app.maxplayerpro.localnetwork.ui

import com.psbapp.mvi.UiEffect
import com.psbapp.mvi.UiEvent
import com.psbapp.mvi.UiState
import org.app.maxplayerpro.localnetwork.share.DeviceSharing
import org.app.maxplayerpro.localnetwork.share.DownloadTask
import org.app.maxplayerpro.localnetwork.share.SaveLocation
import org.app.maxplayerpro.localnetwork.share.HostState
import org.app.maxplayerpro.localnetwork.share.RemoteShare
import org.app.maxplayerpro.localnetwork.share.Share

class LocalNetworkContract {
    enum class Tab(val label: String) { SharedWithMe("Shared with me"), SharedByMe("Shared by me") }

    sealed class Event : UiEvent {
        data class SelectTab(val tab: Tab) : Event()
        // This device as a host.
        data object StartHosting : Event()
        data object StopHosting : Event()
        data class SetDevicePin(val pin: String?) : Event()
        data class SetDeviceUntil(val untilMs: Long?) : Event()
        data object LogOutAllDevices : Event()
        data class LogOutDevice(val clientId: String) : Event()
        data class RevokeShare(val id: String) : Event()
        data class MakePrivate(val id: String) : Event()
        // Other devices.
        data class AddAddress(val address: String) : Event()
        data class RemoveAddress(val baseUrl: String) : Event()
        data object Refresh : Event()
        data class OpenDevice(val baseUrl: String) : Event()
        data object CloseDevice : Event()
        data class OpenShare(val share: RemoteShare) : Event()
        data object CloseShare : Event()
        data class SubmitPin(val pin: String) : Event()
        data object CancelPin : Event()
        data class Play(val share: RemoteShare, val index: Int) : Event()
        data class OpenGroup(val group: Group) : Event()
        /** Plays item [index] of [share] within [group]; the group's other open files play next. */
        data class PlayInGroup(val group: Group, val share: RemoteShare, val index: Int) : Event()
        /** Saves item [index] of [share] as [name] in [location]. */
        data class Download(val share: RemoteShare, val index: Int, val name: String, val location: SaveLocation) : Event()
        data class CancelDownload(val id: String) : Event()
        data object ClearDownloads : Event()
        /** Forgets the PINs entered for the open device and closes it. */
        data object ForgetOpenDevice : Event()
    }

    /** A device on the network (or added by address) in the "Shared with me" list. */
    data class DeviceEntry(
        val name: String,
        val platform: String,
        val baseUrl: String,
        val discovered: Boolean,
        val id: String? = null,
        val pinRequired: Boolean = false,
        val signedIn: Boolean = false,
        val reachable: Boolean = true,
    )

    /** The device being browsed. [unlocked] holds protected shares opened with their PIN, with their items. */
    data class OpenDevice(
        val baseUrl: String,
        val name: String,
        val id: String? = null,
        val loading: Boolean = true,
        val error: String? = null,
        val shares: List<RemoteShare> = emptyList(),
        val unlocked: Map<String, RemoteShare> = emptyMap(),
        val openShareId: String? = null,
        val openGroup: Group? = null,
    ) {
        /** [share] with its items when it can be opened now, else null. */
        fun openable(share: RemoteShare): RemoteShare? = unlocked[share.id] ?: share.takeIf { !it.locked && it.items.isNotEmpty() }

        /** Files shared on their own (not as a folder) that belong in [group]. */
        fun loose(group: Group): List<RemoteShare> = shares.filter { !it.folder && it.locked == group.protected && it.audio == group.audio }
    }

    /**
     * Where files shared on their own appear on the viewing side: one folder per kind, split by protection. A protected
     * file still has its own PIN; the group only gathers them.
     */
    enum class Group(val title: String, val protected: Boolean, val audio: Boolean) {
        Videos("Videos", false, false), ProtectedVideos("Protected videos", true, false),
        Music("Music", false, true), ProtectedMusic("Protected music", true, true),
    }

    /** A PIN being asked for: the open device's, or [share]'s when set. */
    data class PinPrompt(val share: RemoteShare? = null, val error: String? = null, val busy: Boolean = false)

    data class State(
        val tab: Tab = Tab.SharedWithMe,
        val canHost: Boolean = false,
        val canDiscover: Boolean = false,
        val host: HostState = HostState.Stopped,
        val device: DeviceSharing = DeviceSharing(),
        val shares: List<Share> = emptyList(),
        val devices: List<DeviceEntry> = emptyList(),
        val open: OpenDevice? = null,
        val prompt: PinPrompt? = null,
        val canDownload: Boolean = false,
        val downloadsLabel: String = "",
        val downloads: List<DownloadTask> = emptyList(),
        /** A pull-to-refresh reload is running. */
        val refreshing: Boolean = false,
        val nowMs: Long = 0,
    ) : UiState

    sealed class Effect : UiEffect
}
