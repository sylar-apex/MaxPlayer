package org.app.maxplayerpro.localnetwork.ui

import androidx.lifecycle.viewModelScope
import com.psbapp.mvi.MVIBaseViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.app.kmediaplayer.util.nowMs
import org.app.maxplayerpro.localnetwork.share.NearbyClient
import org.app.maxplayerpro.localnetwork.share.NearbyException
import org.app.maxplayerpro.localnetwork.share.RemoteShare
import org.app.maxplayerpro.localnetwork.share.SharingController
import org.app.maxplayerpro.sharedlogic.usecase.MediaOperations

class LocalNetworkViewModel(
    private val sharing: SharingController,
    private val client: NearbyClient,
    private val operations: MediaOperations,
) : MVIBaseViewModel<LocalNetworkContract.Event, LocalNetworkContract.State, LocalNetworkContract.Effect>() {

    private var browseJob: Job? = null

    // Runs from the base class constructor, before this class's properties are set: dependencies are read in init.
    override fun createInitialState() = LocalNetworkContract.State()

    override fun handleEffect(effect: LocalNetworkContract.Effect) {}

    init {
        updateState { copy(canHost = sharing.host.supported, canDiscover = sharing.discovery.supported, savedAddresses = sharing.store.savedAddresses) }
        sharing.beginBrowsing()
        viewModelScope.launch {
            combine(sharing.host.state, sharing.store.shares, sharing.discovery.devices) { h, s, d -> Triple(h, s, d) }
                .collect { (h, s, d) -> updateState { copy(host = h, shares = s.sortedByDescending { it.createdAtMs }, nearby = d) } }
        }
        // Ticks the clock so "expires in" labels and expired badges stay current.
        viewModelScope.launch { while (isActive) { updateState { copy(nowMs = nowMs()) }; delay(30_000) } }
    }

    override fun onCleared() {
        sharing.endBrowsing()
        super.onCleared()
    }

    override fun handleEvent(event: LocalNetworkContract.Event) {
        when (event) {
            LocalNetworkContract.Event.StartHosting -> viewModelScope.launch { sharing.startHosting() }
            LocalNetworkContract.Event.StopHosting -> viewModelScope.launch { sharing.stopHosting() }
            is LocalNetworkContract.Event.RevokeShare -> viewModelScope.launch { sharing.store.revoke(event.id) }
            is LocalNetworkContract.Event.DeleteShare -> viewModelScope.launch { sharing.store.delete(event.id) }
            is LocalNetworkContract.Event.AddAddress -> {
                val url = NearbyClient.normalizeAddress(event.address) ?: return operations.toast("Enter an address like 192.168.1.5")
                sharing.store.savedAddresses = sharing.store.savedAddresses + url
                updateState { copy(savedAddresses = sharing.store.savedAddresses) }
                open(url, url.substringAfter("://"))
            }
            is LocalNetworkContract.Event.RemoveAddress -> {
                sharing.store.savedAddresses = sharing.store.savedAddresses - event.baseUrl
                updateState { copy(savedAddresses = sharing.store.savedAddresses) }
            }
            is LocalNetworkContract.Event.OpenDevice -> open(event.baseUrl, event.name)
            LocalNetworkContract.Event.CloseDevice -> { browseJob?.cancel(); updateState { copy(browsing = null) } }
            LocalNetworkContract.Event.RefreshDevice -> currentState.browsing?.let { open(it.baseUrl, it.name, keepUnlocked = true) }
            is LocalNetworkContract.Event.Unlock -> unlock(event.share, event.code)
            is LocalNetworkContract.Event.Play -> play(event.share, event.index)
        }
    }

    private fun open(baseUrl: String, name: String, keepUnlocked: Boolean = false) {
        browseJob?.cancel()
        val previous = currentState.browsing?.takeIf { keepUnlocked && it.baseUrl == baseUrl }
        updateState { copy(browsing = LocalNetworkContract.Browsing(name, baseUrl, unlocked = previous?.unlocked.orEmpty())) }
        browseJob = viewModelScope.launch {
            val result = runCatching {
                val info = client.info(baseUrl)
                info.name to client.shares(baseUrl)
            }
            updateState {
                val b = browsing?.takeIf { it.baseUrl == baseUrl } ?: return@updateState this
                copy(browsing = result.fold(
                    { (n, shares) -> b.copy(name = n, loading = false, error = null, shares = shares, unlocked = b.unlocked.filterKeys { id -> shares.any { it.id == id } }) },
                    { b.copy(loading = false, error = (it as? NearbyException)?.message ?: "Could not reach the device") },
                ))
            }
        }
    }

    private fun unlock(share: RemoteShare, code: String) {
        val baseUrl = currentState.browsing?.baseUrl ?: return
        viewModelScope.launch {
            val result = runCatching { client.unlock(baseUrl, share.id, code) }
            updateState {
                val b = browsing?.takeIf { it.baseUrl == baseUrl } ?: return@updateState this
                copy(browsing = result.fold(
                    { b.copy(unlocked = b.unlocked + (share.id to LocalNetworkContract.Unlocked(it.token, it.share)), unlockError = b.unlockError - share.id) },
                    { b.copy(unlockError = b.unlockError + (share.id to ((it as? NearbyException)?.message ?: "Could not unlock"))) },
                ))
            }
        }
    }

    private fun play(share: RemoteShare, index: Int) {
        val b = currentState.browsing ?: return
        val unlocked = b.unlocked[share.id]
        val files = client.mediaFiles(b.baseUrl, unlocked?.share ?: share, unlocked?.token)
        if (files.isEmpty()) return
        val start = index.coerceIn(files.indices)
        if (files[start].isAudio) operations.playMusic(files, start) else operations.play(files, start)
    }
}
