package org.app.maxplayerpro.localnetwork.ui

import androidx.lifecycle.viewModelScope
import com.psbapp.mvi.MVIBaseViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.app.kmediaplayer.util.nowMs
import org.app.maxplayerpro.localnetwork.share.NearbyClient
import org.app.maxplayerpro.localnetwork.share.NearbyException
import org.app.maxplayerpro.localnetwork.share.RemoteShare
import org.app.maxplayerpro.localnetwork.share.SharingController
import org.app.maxplayerpro.localnetwork.ui.LocalNetworkContract.RemoteDevice
import org.app.maxplayerpro.sharedlogic.usecase.MediaOperations

/**
 * Both tabs of the network screen. "Shared with me" lists every device found on the network or added by address
 * and loads its shares by itself, so files play straight from the list.
 */
class LocalNetworkViewModel(
    private val sharing: SharingController,
    private val client: NearbyClient,
    private val operations: MediaOperations,
) : MVIBaseViewModel<LocalNetworkContract.Event, LocalNetworkContract.State, LocalNetworkContract.Effect>() {

    private val saved = MutableStateFlow(emptyList<String>())
    /** One refresh per device at a time, so a slow device never has two loads racing to write its entry. */
    private val deviceLocks = mutableMapOf<String, Mutex>()

    // Runs from the base class constructor, before this class's properties are set: dependencies are read in init.
    override fun createInitialState() = LocalNetworkContract.State()

    override fun handleEffect(effect: LocalNetworkContract.Effect) {}

    init {
        saved.value = sharing.store.savedAddresses
        updateState { copy(canHost = sharing.host.supported, canDiscover = sharing.discovery.supported) }
        sharing.beginBrowsing()
        viewModelScope.launch {
            combine(sharing.host.state, sharing.store.shares) { h, s -> h to s }
                .collect { (h, s) -> updateState { copy(host = h, shares = s.sortedByDescending { it.createdAtMs }) } }
        }
        // The device list: discovered devices plus typed-in addresses, keeping what was already loaded for each.
        viewModelScope.launch {
            combine(sharing.discovery.devices, saved) { found, addresses ->
                found.map { RemoteDevice(it.name, it.platform, it.baseUrl, discovered = true) } +
                    addresses.filter { a -> found.none { it.baseUrl == a } }.map { RemoteDevice(it.substringAfter("://"), "", it, discovered = false) }
            }.collect { targets ->
                val known = currentState.devices.associateBy { it.baseUrl }
                updateState {
                    copy(devices = targets.map { t ->
                        known[t.baseUrl]?.copy(name = if (t.discovered) t.name else known.getValue(t.baseUrl).name, platform = t.platform.ifEmpty { known.getValue(t.baseUrl).platform }, discovered = t.discovered) ?: t
                    })
                }
                targets.filter { it.baseUrl !in known }.forEach { load(it.baseUrl) }
            }
        }
        // Shares come and go on other devices; reload while this screen is open.
        viewModelScope.launch {
            while (isActive) {
                delay(REFRESH_MS)
                currentState.devices.forEach { load(it.baseUrl) }
            }
        }
        viewModelScope.launch { while (isActive) { updateState { copy(nowMs = nowMs()) }; delay(30_000) } }
    }

    override fun onCleared() {
        sharing.endBrowsing()
        super.onCleared()
    }

    override fun handleEvent(event: LocalNetworkContract.Event) {
        when (event) {
            is LocalNetworkContract.Event.SelectTab -> updateState { copy(tab = event.tab) }
            LocalNetworkContract.Event.StartHosting -> viewModelScope.launch { sharing.startHosting() }
            LocalNetworkContract.Event.StopHosting -> viewModelScope.launch { sharing.stopHosting() }
            is LocalNetworkContract.Event.RevokeShare -> viewModelScope.launch { sharing.store.revoke(event.id) }
            is LocalNetworkContract.Event.DeleteShare -> viewModelScope.launch { sharing.store.delete(event.id) }
            is LocalNetworkContract.Event.AddAddress -> {
                val url = NearbyClient.normalizeAddress(event.address) ?: return operations.toast("Enter an address like 192.168.1.5")
                sharing.store.savedAddresses = sharing.store.savedAddresses + url
                saved.value = sharing.store.savedAddresses
                updateState { copy(tab = LocalNetworkContract.Tab.SharedWithMe) }
            }
            is LocalNetworkContract.Event.RemoveAddress -> {
                sharing.store.savedAddresses = sharing.store.savedAddresses - event.baseUrl
                saved.value = sharing.store.savedAddresses
            }
            LocalNetworkContract.Event.Refresh -> currentState.devices.forEach { load(it.baseUrl) }
            is LocalNetworkContract.Event.Unlock -> unlock(event.baseUrl, event.share, event.code)
            is LocalNetworkContract.Event.Play -> play(event.baseUrl, event.share, event.index)
        }
    }

    private fun load(baseUrl: String) {
        val lock = deviceLocks.getOrPut(baseUrl) { Mutex() }
        if (lock.isLocked) return
        viewModelScope.launch {
            lock.withLock {
                val result = runCatching { client.info(baseUrl) to client.shares(baseUrl) }
                editDevice(baseUrl) { d ->
                    result.fold(
                        { (info, shares) ->
                            d.copy(
                                name = info.name, platform = info.platform, loading = false, error = null, shares = shares,
                                // Drop unlocks for shares that are gone, so an expired share doesn't keep showing its files.
                                unlocked = d.unlocked.filterKeys { id -> shares.any { it.id == id } },
                            )
                        },
                        { d.copy(loading = false, error = (it as? NearbyException)?.message ?: "Not reachable") },
                    )
                }
            }
        }
    }

    private fun unlock(baseUrl: String, share: RemoteShare, code: String) {
        viewModelScope.launch {
            val result = runCatching { client.unlock(baseUrl, share.id, code) }
            editDevice(baseUrl) { d ->
                result.fold(
                    { d.copy(unlocked = d.unlocked + (share.id to LocalNetworkContract.Unlocked(it.token, it.share)), unlockError = d.unlockError - share.id) },
                    { d.copy(unlockError = d.unlockError + (share.id to ((it as? NearbyException)?.message ?: "Could not unlock"))) },
                )
            }
        }
    }

    private fun play(baseUrl: String, share: RemoteShare, index: Int) {
        val device = currentState.devices.firstOrNull { it.baseUrl == baseUrl } ?: return
        val unlocked = device.unlocked[share.id]
        val files = client.mediaFiles(baseUrl, unlocked?.share ?: share, unlocked?.token)
        if (files.isEmpty()) return
        val start = index.coerceIn(files.indices)
        if (files[start].isAudio) operations.playMusic(files, start) else operations.play(files, start)
    }

    private fun editDevice(baseUrl: String, change: (RemoteDevice) -> RemoteDevice) =
        updateState { copy(devices = devices.map { if (it.baseUrl == baseUrl) change(it) else it }) }

    private companion object {
        const val REFRESH_MS = 15_000L
    }
}
