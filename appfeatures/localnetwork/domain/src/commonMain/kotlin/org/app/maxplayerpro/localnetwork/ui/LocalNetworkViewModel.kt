package org.app.maxplayerpro.localnetwork.ui

import androidx.lifecycle.viewModelScope
import com.psbapp.mvi.MVIBaseViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.app.kmediaplayer.util.nowMs
import org.app.maxplayerpro.localnetwork.share.NearbyClient
import org.app.maxplayerpro.localnetwork.share.NearbyException
import org.app.maxplayerpro.localnetwork.share.RemoteShare
import org.app.maxplayerpro.localnetwork.share.SharingController
import org.app.maxplayerpro.localnetwork.ui.LocalNetworkContract.DeviceEntry
import org.app.maxplayerpro.localnetwork.ui.LocalNetworkContract.Event
import org.app.maxplayerpro.localnetwork.ui.LocalNetworkContract.OpenDevice
import org.app.maxplayerpro.localnetwork.ui.LocalNetworkContract.PinPrompt
import org.app.maxplayerpro.sharedlogic.usecase.MediaOperations

/**
 * Both tabs of the network screen. "Shared with me" lists devices; opening one asks for its PIN once (remembered
 * until that device logs everyone out), then shows its shared folders and files, where a protected share asks for
 * its own PIN. "Shared by me" manages this device's sharing.
 */
class LocalNetworkViewModel(
    private val sharing: SharingController,
    private val client: NearbyClient,
    private val operations: MediaOperations,
) : MVIBaseViewModel<Event, LocalNetworkContract.State, LocalNetworkContract.Effect>() {

    private val saved = MutableStateFlow(emptyList<String>())
    private var openJob: Job? = null
    private val store get() = sharing.store

    // Runs from the base class constructor, before this class's properties are set: dependencies are read in init.
    override fun createInitialState() = LocalNetworkContract.State()

    override fun handleEffect(effect: LocalNetworkContract.Effect) {}

    init {
        saved.value = store.savedAddresses
        updateState { copy(canHost = sharing.host.supported, canDiscover = sharing.discovery.supported) }
        sharing.beginBrowsing()
        viewModelScope.launch {
            combine(sharing.host.state, store.shares, store.device) { h, s, d -> Triple(h, s, d) }
                .collect { (h, s, d) -> updateState { copy(host = h, shares = s.sortedByDescending { it.createdAtMs }, device = d) } }
        }
        viewModelScope.launch {
            combine(sharing.discovery.devices, saved) { found, addresses ->
                found.map { DeviceEntry(it.name, it.platform, it.baseUrl, discovered = true, id = it.id) } +
                    addresses.filter { a -> found.none { it.baseUrl == a } }.map { DeviceEntry(it.substringAfter("://"), "", it, discovered = false) }
            }.collect { targets ->
                val known = currentState.devices.associateBy { it.baseUrl }
                updateState { copy(devices = targets.map { t -> known[t.baseUrl]?.let { k -> t.copy(id = t.id ?: k.id, name = if (t.discovered) t.name else k.name, platform = t.platform.ifEmpty { k.platform }, pinRequired = k.pinRequired, reachable = k.reachable, signedIn = k.signedIn) } ?: t }) }
                targets.filter { it.baseUrl !in known }.forEach { probe(it.baseUrl) }
            }
        }
        viewModelScope.launch {
            while (isActive) {
                delay(REFRESH_MS)
                currentState.devices.forEach { probe(it.baseUrl) }
                currentState.open?.takeIf { currentState.prompt == null }?.let { loadShares(it.baseUrl, quiet = true) }
            }
        }
        viewModelScope.launch { while (isActive) { updateState { copy(nowMs = nowMs()) }; delay(30_000) } }
    }

    override fun onCleared() {
        sharing.endBrowsing()
        super.onCleared()
    }

    override fun handleEvent(event: Event) {
        when (event) {
            is Event.SelectTab -> updateState { copy(tab = event.tab) }
            Event.StartHosting -> viewModelScope.launch { sharing.startHosting() }
            Event.StopHosting -> viewModelScope.launch { sharing.stopHosting() }
            is Event.SetDevicePin -> viewModelScope.launch { store.setDevicePin(event.pin) }
            is Event.SetDeviceUntil -> viewModelScope.launch { store.setDeviceUntil(event.untilMs) }
            Event.LogOutAllDevices -> viewModelScope.launch { store.logOutAllDevices(); operations.toast("All devices logged out") }
            is Event.RevokeShare -> viewModelScope.launch { store.revoke(event.id) }
            is Event.MakePrivate -> viewModelScope.launch { store.delete(event.id) }
            is Event.AddAddress -> {
                val url = NearbyClient.normalizeAddress(event.address) ?: return operations.toast("Enter an address like 192.168.1.5")
                store.savedAddresses = store.savedAddresses + url
                saved.value = store.savedAddresses
                updateState { copy(tab = LocalNetworkContract.Tab.SharedWithMe) }
            }
            is Event.RemoveAddress -> {
                store.savedAddresses = store.savedAddresses - event.baseUrl
                saved.value = store.savedAddresses
            }
            Event.Refresh -> {
                currentState.devices.forEach { probe(it.baseUrl) }
                currentState.open?.let { loadShares(it.baseUrl) }
            }
            is Event.OpenDevice -> openDevice(event.baseUrl)
            Event.CloseDevice -> { openJob?.cancel(); updateState { copy(open = null, prompt = null) } }
            is Event.OpenShare -> openShare(event.share)
            Event.CloseShare -> editOpen { it.copy(openShareId = null) }
            is Event.SubmitPin -> submitPin(event.pin)
            Event.CancelPin -> updateState { copy(prompt = null, open = if (open?.shares.isNullOrEmpty() && open?.error == null) null else open) }
            is Event.Play -> play(event.share, event.index)
            Event.ForgetOpenDevice -> {
                val open = currentState.open ?: return
                viewModelScope.launch { open.id?.let { id -> store.remember(id) { null } } }
                updateState { copy(open = null, prompt = null, devices = devices.map { if (it.baseUrl == open.baseUrl) it.copy(signedIn = false) else it }) }
            }
        }
    }

    /** Refreshes a device's name, lock state and reachability for the list. */
    private fun probe(baseUrl: String) {
        viewModelScope.launch {
            val info = runCatching { client.info(baseUrl) }.getOrNull()
            updateState {
                copy(devices = devices.map { d ->
                    if (d.baseUrl != baseUrl) d
                    else if (info == null) d.copy(reachable = false)
                    else d.copy(id = info.id, name = info.name, platform = info.platform, pinRequired = info.pinRequired, reachable = true,
                        signedIn = store.remembered.value[info.id]?.deviceToken != null)
                })
            }
        }
    }

    private fun openDevice(baseUrl: String) {
        val entry = currentState.devices.firstOrNull { it.baseUrl == baseUrl }
        updateState { copy(open = OpenDevice(baseUrl, entry?.name ?: baseUrl.substringAfter("://"), entry?.id), prompt = null) }
        loadShares(baseUrl)
    }

    /** Loads the open device's shares with the remembered tokens; asks for the device PIN when they're missing or void. */
    private fun loadShares(baseUrl: String, quiet: Boolean = false) {
        openJob?.cancel()
        openJob = viewModelScope.launch {
            if (!quiet) editOpen { it.copy(loading = true, error = null) }
            val result = runCatching {
                val info = client.info(baseUrl)
                editOpen { it.copy(id = info.id, name = info.name) }
                val access = store.remembered.value[info.id]
                if (info.pinRequired && access?.deviceToken == null) throw NearbyException(NearbyException.Kind.DevicePinNeeded, "")
                val shares = client.shares(baseUrl, access?.deviceToken)
                // Reopen protected shares unlocked before; a token the owner voided just drops out.
                val unlocked = shares.filter { it.locked }.mapNotNull { s ->
                    val token = access?.shareTokens?.get(s.id) ?: return@mapNotNull null
                    runCatching { client.share(baseUrl, s.id, access.deviceToken, token) }.getOrElse {
                        store.remember(info.id) { a -> a.copy(shareTokens = a.shareTokens - s.id) }
                        null
                    }
                }.associateBy { it.id }
                shares to unlocked
            }
            result.onSuccess { (shares, unlocked) ->
                editOpen { o -> o.copy(loading = false, error = null, shares = shares, unlocked = unlocked, openShareId = o.openShareId?.takeIf { id -> shares.any { it.id == id } }) }
            }.onFailure { e ->
                val ex = e as? NearbyException
                if (ex?.kind == NearbyException.Kind.DevicePinNeeded) {
                    // Logged out by the owner (or never signed in): forget what we had and ask again.
                    currentState.open?.id?.let { id -> store.remember(id) { null } }
                    updateState { copy(open = open?.copy(loading = false, shares = emptyList(), unlocked = emptyMap(), openShareId = null), prompt = PinPrompt()) }
                } else if (!quiet) {
                    editOpen { it.copy(loading = false, error = ex?.message ?: "Could not reach the device") }
                }
            }
        }
    }

    private fun openShare(share: RemoteShare) {
        val open = currentState.open ?: return
        val ready = open.openable(share)
        when {
            ready == null -> updateState { copy(prompt = PinPrompt(share = share)) }
            ready.folder || ready.items.size > 1 -> editOpen { it.copy(openShareId = share.id) }
            else -> play(ready, 0)
        }
    }

    private fun submitPin(pin: String) {
        val open = currentState.open ?: return
        val prompt = currentState.prompt ?: return
        updateState { copy(prompt = prompt.copy(busy = true, error = null)) }
        viewModelScope.launch {
            val id = open.id ?: runCatching { client.info(open.baseUrl).id }.getOrNull()
            val result = runCatching {
                val share = prompt.share
                if (share == null) {
                    val token = client.login(open.baseUrl, pin)
                    if (id != null) store.remember(id) { it.copy(deviceToken = token) }
                    null
                } else {
                    val res = client.unlock(open.baseUrl, share.id, pin, id?.let { store.remembered.value[it]?.deviceToken })
                    if (id != null && res.token.isNotEmpty()) store.remember(id) { it.copy(shareTokens = it.shareTokens + (share.id to res.token)) }
                    res.share
                }
            }
            result.onSuccess { unlockedShare ->
                updateState { copy(prompt = null, devices = devices.map { if (it.baseUrl == open.baseUrl && unlockedShare == null) it.copy(signedIn = true) else it }) }
                if (unlockedShare == null) loadShares(open.baseUrl)
                else {
                    editOpen { it.copy(unlocked = it.unlocked + (unlockedShare.id to unlockedShare)) }
                    openShare(unlockedShare.copy(locked = false))
                }
            }.onFailure { e ->
                val ex = e as? NearbyException
                if (ex?.kind == NearbyException.Kind.DevicePinNeeded) updateState { copy(prompt = PinPrompt(error = "Enter this device's PIN first")) }
                else updateState { copy(prompt = prompt.copy(busy = false, error = ex?.message ?: "Could not reach the device")) }
            }
        }
    }

    private fun play(share: RemoteShare, index: Int) {
        val open = currentState.open ?: return
        val access = open.id?.let { store.remembered.value[it] }
        val withItems = open.unlocked[share.id] ?: share
        val files = client.mediaFiles(open.baseUrl, withItems, access?.deviceToken, access?.shareTokens?.get(share.id))
        if (files.isEmpty()) return
        val start = index.coerceIn(files.indices)
        if (files[start].isAudio) operations.playMusic(files, start) else operations.play(files, start)
    }

    private fun editOpen(change: (OpenDevice) -> OpenDevice) = updateState { copy(open = open?.let(change)) }

    private companion object {
        const val REFRESH_MS = 15_000L
    }
}
