package org.app.maxplayerpro.localnetwork.share

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.app.kmediaplayer.model.MediaFile
import org.app.kmediaplayer.util.nowMs

/**
 * One owner for sharing: remembers whether the user turned hosting on, and runs discovery while this device hosts
 * (to answer others) or while any screen is browsing for devices.
 */
class SharingController(val store: ShareStore, val host: ShareHost, val discovery: NearbyDiscovery) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val browsers = MutableStateFlow(0)

    init {
        scope.launch {
            combine(host.state, browsers) { state, count -> state is HostState.Running || count > 0 }
                .distinctUntilChanged()
                .collect { on -> if (on) discovery.start() else discovery.stop() }
        }
    }

    /** Starts hosting again after an app restart if the user left it on. */
    fun resume() {
        if (host.supported && store.hostingEnabled) scope.launch { host.start() }
    }

    suspend fun startHosting() {
        store.hostingEnabled = true
        host.start()
    }

    suspend fun stopHosting() {
        store.hostingEnabled = false
        host.stop()
    }

    fun beginBrowsing() = browsers.update { it + 1 }

    fun endBrowsing() = browsers.update { (it - 1).coerceAtLeast(0) }

    /** Creates a share and makes sure the server is up to serve it. */
    suspend fun share(title: String, files: List<MediaFile>, access: ShareAccess, code: String?, startsAtMs: Long?, expiresAtMs: Long?): Share {
        val share = store.create(title, files, access, code, nowMs(), startsAtMs, expiresAtMs)
        if (host.state.value !is HostState.Running) startHosting()
        return share
    }
}
