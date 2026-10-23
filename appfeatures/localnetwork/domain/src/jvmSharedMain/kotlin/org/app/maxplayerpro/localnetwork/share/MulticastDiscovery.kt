package org.app.maxplayerpro.localnetwork.share

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.app.kmediaplayer.util.nowMs
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.MulticastSocket
import java.net.SocketTimeoutException
import java.net.StandardSocketOptions

/**
 * LAN discovery modelled on LocalSend's protocol: every device joins a multicast group in 224.0.0.0/24 (the only
 * range some Android phones accept), announces itself on start and every [ANNOUNCE_EVERY_MS], and answers queries
 * at once. Hosts carry their HTTP port; a device is listed only after its `/api/v1/info` answers, so stale or
 * firewalled announcements never show up as tappable entries.
 */
class MulticastDiscovery(
    private val store: ShareStore,
    private val host: ShareHost,
    private val platform: HostPlatform,
    private val client: NearbyClient,
) : NearbyDiscovery {
    override val supported = true
    private val _devices = MutableStateFlow<List<NearbyDevice>>(emptyList())
    override val devices: StateFlow<List<NearbyDevice>> = _devices.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true }
    private var job: Job? = null
    private val verifying = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    @Serializable
    private data class Announce(val proto: String = PROTO, val id: String, val name: String, val platform: String, val port: Int, val query: Boolean)

    @Synchronized
    override fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            val lock = platform.acquireMulticast()
            val socket = MulticastSocket(null).apply {
                setOption(StandardSocketOptions.SO_REUSEADDR, true)
                bind(InetSocketAddress(PORT))
                timeToLive = 1
                // Wakes receive() regularly so cancelling [job] is noticed.
                soTimeout = 1_000
            }
            val group = InetSocketAddress(InetAddress.getByName(GROUP), PORT)
            val interfaces = lanInterfaces().map { it.first }.distinct()
            interfaces.forEach { runCatching { socket.joinGroup(group, it) } }
            try {
                launch { while (isActive) { send(socket, query = true); delay(ANNOUNCE_EVERY_MS) } }
                // Re-announce right away when hosting starts or stops, so others see the change without waiting.
                launch { host.state.map { it is HostState.Running }.distinctUntilChanged().collect { send(socket, query = false) } }
                launch { while (isActive) { delay(ANNOUNCE_EVERY_MS); prune() } }
                val buf = ByteArray(2048)
                while (isActive) {
                    val packet = DatagramPacket(buf, buf.size)
                    try { socket.receive(packet) } catch (_: SocketTimeoutException) { continue }
                    val msg = runCatching { json.decodeFromString<Announce>(String(packet.data, 0, packet.length)) }.getOrNull() ?: continue
                    if (msg.proto != PROTO || msg.id == store.deviceId) continue
                    if (msg.query) send(socket, query = false)
                    if (msg.port > 0) seen(msg, "http://${packet.address.hostAddress}:${msg.port}") else forget(msg.id)
                }
            } finally {
                socket.close()
                lock.close()
            }
        }.also { j -> j.invokeOnCompletion { _devices.value = emptyList() } }
    }

    @Synchronized
    override fun stop() {
        job?.cancel()
        job = null
    }

    private fun send(socket: MulticastSocket, query: Boolean) {
        val port = (host.state.value as? HostState.Running)?.port ?: 0
        val bytes = json.encodeToString(Announce(id = store.deviceId, name = platform.deviceName, platform = platform.platform, port = port, query = query)).toByteArray()
        val target = InetSocketAddress(InetAddress.getByName(GROUP), PORT)
        // Send on every LAN interface: a laptop on Wi-Fi and Ethernet, or a phone with a hotspot, sees both sides.
        val interfaces = lanInterfaces().map { it.first }.distinct()
        if (interfaces.isEmpty()) runCatching { socket.send(DatagramPacket(bytes, bytes.size, target)) }
        interfaces.forEach { nif ->
            runCatching {
                socket.setOption(StandardSocketOptions.IP_MULTICAST_IF, nif)
                socket.send(DatagramPacket(bytes, bytes.size, target))
            }
        }
    }

    private fun seen(msg: Announce, baseUrl: String) {
        val now = nowMs()
        val known = _devices.value.firstOrNull { it.id == msg.id && it.baseUrl == baseUrl }
        if (known != null) {
            _devices.update { list -> list.map { if (it.id == msg.id) it.copy(lastSeenMs = now, name = msg.name) else it } }
            return
        }
        if (!verifying.add(msg.id)) return
        scope.launch {
            try {
                val info = runCatching { client.info(baseUrl) }.getOrNull() ?: return@launch
                if (info.id != msg.id) return@launch
                val device = NearbyDevice(info.id, info.name, info.platform, baseUrl, nowMs())
                _devices.update { list -> (list.filterNot { it.id == device.id } + device).sortedBy { it.name.lowercase() } }
            } finally {
                verifying.remove(msg.id)
            }
        }
    }

    private fun forget(id: String) = _devices.update { list -> list.filterNot { it.id == id } }

    private fun prune() {
        val cutoff = nowMs() - ANNOUNCE_EVERY_MS * 3
        _devices.update { list -> list.filter { it.lastSeenMs >= cutoff } }
    }

    private companion object {
        const val PROTO = "maxplayer-share/1"
        // LocalSend uses 224.0.0.167:53317; we take the next port so both apps can run side by side.
        const val GROUP = "224.0.0.167"
        const val PORT = 53318
        const val ANNOUNCE_EVERY_MS = 10_000L
    }
}
