package org.app.maxplayerpro.localnetwork.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SensorsOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.launch
import org.app.maxplayerpro.localnetwork.share.HostState
import org.app.maxplayerpro.localnetwork.share.SharingController
import org.koin.compose.koinInject

private val SharingOn = Color(0xFF34C759)

/** Top-bar switch for network sharing: green while this device is sharing, grey when not. Hidden where sharing isn't possible. */
@Composable
fun SharingToggle(modifier: Modifier = Modifier) {
    val sharing = koinInject<SharingController>()
    if (!sharing.host.supported) return
    val state by sharing.host.state.collectAsState()
    val scope = rememberCoroutineScope()
    val on = state is HostState.Running || state == HostState.Starting
    IconButton({ scope.launch { if (on) sharing.stopHosting() else sharing.startHosting() } }, modifier) {
        Icon(
            if (on) Icons.Default.Sensors else Icons.Default.SensorsOff,
            if (on) "Sharing on network: on. Tap to turn off" else "Sharing on network: off. Tap to turn on",
            tint = if (state is HostState.Running) SharingOn else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
