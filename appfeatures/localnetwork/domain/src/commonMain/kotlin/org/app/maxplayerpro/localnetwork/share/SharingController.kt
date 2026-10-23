package org.app.maxplayerpro.localnetwork.share

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.app.kmediaplayer.model.MediaFile
import org.app.kmediaplayer.util.nowMs

/**
 * One owner for sharing: remembers whether the user turned hosting on, turns it off when the device's time limit
 * passes, and runs discovery while this device hosts (to answer others) or while any screen is browsing for devices.
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
        // The server already refuses everything past the limit; this also switches sharing off so the
        // notification and the always-on Wi-Fi lock go away.
        scope.launch {
            combine(host.state, store.device) { state, device -> if (state is HostState.Running) device.untilMs else null }
                .distinctUntilChanged()
                .collectLatest { until ->
                    if (until == null) return@collectLatest
                    delay((until - nowMs()).coerceAtLeast(0))
                    stopHosting()
                }
        }
    }

    /** Starts hosting again after an app restart if the user left it on and the time limit hasn't passed. */
    fun resume() {
        if (host.supported && store.hostingEnabled) scope.launch { startHosting() }
    }

    suspend fun startHosting() {
        // Turning sharing on again after its time limit ran out means "share again", not "stay closed".
        store.device.value.untilMs?.let { if (it <= nowMs()) store.setDeviceUntil(null) }
        store.hostingEnabled = true
        host.start()
    }

    suspend fun stopHosting() {
        store.hostingEnabled = false
        host.stop()
    }

    fun beginBrowsing() = browsers.update { it + 1 }

    fun endBrowsing() = browsers.update { (it - 1).coerceAtLeast(0) }

    /** Shares files or a folder and makes sure the server is up to serve them. */
    suspend fun share(title: String, files: List<MediaFile>, folderPath: String?, access: ShareAccess, pin: String?, startsAtMs: Long?, expiresAtMs: Long?): Share {
        val share = store.save(title, files, folderPath, access, pin, nowMs(), startsAtMs, expiresAtMs)
        if (host.state.value !is HostState.Running) startHosting()
        return share
    }
}
