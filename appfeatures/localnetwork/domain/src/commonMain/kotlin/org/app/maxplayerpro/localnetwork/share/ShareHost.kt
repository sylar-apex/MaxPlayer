package org.app.maxplayerpro.localnetwork.share

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.module.Module

/** Where this device's share server is. [addresses] are the LAN URLs other devices and browsers open. */
sealed interface HostState {
    data object Stopped : HostState
    data object Starting : HostState
    data class Running(val port: Int, val addresses: List<String>) : HostState
    data class Failed(val reason: String) : HostState
}

/** Serves this device's shares over HTTP. Start and stop are idempotent and safe to call from any thread. */
interface ShareHost {
    val supported: Boolean
    val state: StateFlow<HostState>
    suspend fun start()
    suspend fun stop()
}

/** A device seen on the network that is serving shares. [baseUrl] is like `http://192.168.1.5:53319`. */
data class NearbyDevice(val id: String, val name: String, val platform: String, val baseUrl: String, val lastSeenMs: Long)

/** Finds other Max Player hosts on the local network. */
interface NearbyDiscovery {
    val supported: Boolean
    val devices: StateFlow<List<NearbyDevice>>
    fun start()
    fun stop()
}

/** For platforms that can only browse other devices (browser, iOS). */
object NoShareHost : ShareHost {
    override val supported = false
    override val state: StateFlow<HostState> = MutableStateFlow(HostState.Stopped).asStateFlow()
    override suspend fun start() {}
    override suspend fun stop() {}
}

object NoDiscovery : NearbyDiscovery {
    override val supported = false
    override val devices: StateFlow<List<NearbyDevice>> = MutableStateFlow(emptyList<NearbyDevice>()).asStateFlow()
    override fun start() {}
    override fun stop() {}
}

/** Binds [ShareHost], [NearbyDiscovery] and the HTTP client engine for the current platform. */
expect fun platformShareModule(): Module
