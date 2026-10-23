package org.app.maxplayerpro.localnetwork.share

import java.net.Inet4Address
import java.net.NetworkInterface
import java.nio.channels.FileChannel

/** What the share server needs from Android or desktop. */
interface HostPlatform {
    val deviceName: String
    val platform: String

    /** Opens [item] for reading, or null when the file is gone or unreadable. */
    fun open(item: SharedItem): FileChannel?

    /** Called after the server starts and after it stops; Android keeps the process and Wi-Fi awake in between. */
    fun onHostingChanged(running: Boolean)

    /** Holds whatever the platform needs to receive multicast packets until closed. */
    fun acquireMulticast(): AutoCloseable
}

/** Up, non-loopback IPv4 interfaces on a private network (Wi-Fi, Ethernet, hotspot), Wi-Fi-like names first. */
internal fun lanInterfaces(): List<Pair<NetworkInterface, Inet4Address>> =
    runCatching { NetworkInterface.getNetworkInterfaces().toList() }.getOrDefault(emptyList())
        .filter { runCatching { it.isUp && !it.isLoopback && !it.isVirtual && !it.isPointToPoint }.getOrDefault(false) }
        .flatMap { nif -> nif.inetAddresses.toList().filterIsInstance<Inet4Address>().filter { it.isSiteLocalAddress }.map { nif to it } }
        .sortedBy { (nif, _) -> if (nif.name.startsWith("wlan") || nif.name.startsWith("en") || nif.name.startsWith("eth")) 0 else 1 }
