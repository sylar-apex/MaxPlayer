package org.app.maxplayerpro.localnetwork.share

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.app.kmediaplayer.model.MediaFile
import org.app.kmediaplayer.settings.MemoryStore
import org.app.kmediaplayer.util.nowMs
import java.io.File
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * Real-network check, skipped unless `LAN_E2E_SECONDS` is set: hosts a public share from this machine and waits for
 * another Max Player on the LAN to show up through multicast discovery. Run it while the phone has the Network
 * share screen open (or is hosting) to verify discovery in both directions.
 */
class LanDiscoveryE2ETest {
    @Test
    fun discoversAndIsDiscoveredOnTheLan() = runBlocking {
        val seconds = System.getenv("LAN_E2E_SECONDS")?.toLongOrNull() ?: return@runBlocking
        val file = File(System.getenv("LAN_E2E_FILE") ?: error("set LAN_E2E_FILE"))
        val platform = object : HostPlatform {
            override val deviceName = "Mac E2E host"
            override val platform = "Mac OS X"
            override fun open(item: SharedItem): FileChannel? = FileChannel.open(File(item.path).toPath(), StandardOpenOption.READ)
            override fun onHostingChanged(running: Boolean) {}
            override fun acquireMulticast() = AutoCloseable {}
        }
        val store = ShareStore(MemoryStore())
        val host = KtorShareHost(store, platform)
        val client = NearbyClient(HttpClient(CIO)) { SelfDevice(store.deviceId, platform.deviceName, platform.platform) }
        val discovery = MulticastDiscovery(store, host, platform, client)
        val sharing = SharingController(store, host, discovery, client)
        sharing.share("Mac test share", listOf(MediaFile(1, "file://${file.path}", file.path, file.name, file.length(), 0, 0, width = System.getenv("LAN_E2E_W")?.toInt() ?: 0, height = System.getenv("LAN_E2E_H")?.toInt() ?: 0)),
            null, ShareAccess.Public, null, null, nowMs() + 3_600_000)
        // Optional: a device PIN, and a protected folder share of the same file with its own PIN.
        System.getenv("LAN_E2E_PIN")?.let { store.setDevicePin(it) }
        System.getenv("LAN_E2E_LOOSE_PIN")?.let { pin ->
            sharing.share("Protected clip", listOf(MediaFile(3, "file://${file.path}#3", file.path, "Protected clip.mp4", file.length(), 0, 0)),
                null, ShareAccess.Protected, pin, null, null)
        }
        System.getenv("LAN_E2E_SHARE_PIN")?.let { pin ->
            sharing.share("Protected folder", listOf(MediaFile(2, "file://${file.path}#2", file.path, file.name, file.length(), 0, 0)),
                file.parent, ShareAccess.Protected, pin, null, null)
        }
        sharing.beginBrowsing()
        println("Hosting: ${host.state.value}")
        val found = withTimeoutOrNull(seconds * 1000) {
            while (discovery.devices.value.isEmpty()) delay(500)
            discovery.devices.value
        }
        println("Discovered: $found")
        assertNotNull(found)
        // Stay up so the other device can open and play this share.
        delay(System.getenv("LAN_E2E_HOLD_SECONDS")?.toLongOrNull()?.times(1000) ?: 0)
        sharing.stopHosting()
    }
}
