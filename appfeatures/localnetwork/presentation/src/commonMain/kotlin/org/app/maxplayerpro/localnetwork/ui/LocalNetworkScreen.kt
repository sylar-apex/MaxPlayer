package org.app.maxplayerpro.localnetwork.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.app.kmediaplayer.ui.common.PlayerDialog
import org.app.kmediaplayer.util.formatDuration
import org.app.kmediaplayer.util.formatSize
import org.app.maxplayerpro.appcore.component.TopBar
import org.app.maxplayerpro.localnetwork.share.HostState
import org.app.maxplayerpro.localnetwork.share.RemoteShare
import org.app.maxplayerpro.localnetwork.share.Share
import org.app.maxplayerpro.localnetwork.share.ShareAccess
import org.app.maxplayerpro.localnetwork.ui.LocalNetworkContract.Event
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LocalNetworkScreen(onBack: () -> Unit, viewModel: LocalNetworkViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsState()
    LocalNetworkContent(state, onBack, onEvent = viewModel::setEvent)
}

@Composable
fun LocalNetworkContent(state: LocalNetworkContract.State, onBack: () -> Unit, onEvent: (Event) -> Unit) {
    var adding by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopBar("Network", onBack = onBack, actions = {
            IconButton({ onEvent(Event.Refresh) }) { Icon(Icons.Default.Refresh, "Refresh") }
            IconButton({ adding = true }) { Icon(Icons.Default.Add, "Add device by address") }
        })
        val tabs = if (state.canHost) LocalNetworkContract.Tab.entries else listOf(LocalNetworkContract.Tab.SharedWithMe)
        if (tabs.size > 1) {
            PrimaryTabRow(tabs.indexOf(state.tab).coerceAtLeast(0), containerColor = MaterialTheme.colorScheme.background) {
                tabs.forEach { tab ->
                    val count = when (tab) {
                        LocalNetworkContract.Tab.SharedWithMe -> state.devices.sumOf { it.shares.size }
                        LocalNetworkContract.Tab.SharedByMe -> state.shares.count { shareStatus(it, state.nowMs).open }
                    }
                    Tab(state.tab == tab, { onEvent(Event.SelectTab(tab)) }, text = {
                        Text(if (count > 0) "${tab.label} ($count)" else tab.label, fontWeight = FontWeight.Medium)
                    })
                }
            }
        }
        LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
            when (if (state.canHost) state.tab else LocalNetworkContract.Tab.SharedWithMe) {
                LocalNetworkContract.Tab.SharedWithMe -> sharedWithMe(state, onEvent, onAdd = { adding = true })
                LocalNetworkContract.Tab.SharedByMe -> sharedByMe(state, onEvent)
            }
            item { Spacer(Modifier.navigationBarsPadding().height(24.dp)) }
        }
    }
    if (adding) AddAddressDialog(onDismiss = { adding = false }, onAdd = { adding = false; onEvent(Event.AddAddress(it)) })
}

private fun LazyListScope.sharedByMe(state: LocalNetworkContract.State, onEvent: (Event) -> Unit) {
    item {
        val host = state.host
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Share on this network", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Text(
                        when (host) {
                            is HostState.Running -> "On · others on this Wi-Fi can open your shares"
                            HostState.Starting -> "Starting…"
                            is HostState.Failed -> host.reason
                            HostState.Stopped -> "Off · your shares are hidden"
                        },
                        fontSize = 13.sp,
                        color = if (host is HostState.Failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(host is HostState.Running || host == HostState.Starting, { on -> onEvent(if (on) Event.StartHosting else Event.StopHosting) })
            }
            if (host is HostState.Running && host.addresses.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp)).padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Language, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(8.dp))
                        Text("Open in any browser on this network", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    SelectionContainer {
                        Column { host.addresses.forEach { Text(it, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp)) } }
                    }
                }
            }
        }
    }
    if (state.shares.isEmpty()) {
        item { Hint("Nothing shared yet. In your library, tap ⋮ on a video or song and choose \"Share on network\".") }
    }
    items(state.shares, key = { "share-" + it.id }) { MyShareCard(it, state.nowMs, onEvent) }
}

@Composable
private fun MyShareCard(share: Share, nowMs: Long, onEvent: (Event) -> Unit) {
    val status = shareStatus(share, nowMs)
    var expanded by remember(share.id) { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))) {
        Row(Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(start = 14.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(share.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val access = if (share.access == ShareAccess.Public) "Anyone on network" else "Code ${share.code}"
                Text("${share.items.size} ${if (share.items.size == 1) "file" else "files"} · $access", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(status.label, fontSize = 12.sp, color = if (status.open) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
            }
            if (status.open) TextButton({ onEvent(Event.RevokeShare(share.id)) }) { Text("Stop") }
            IconButton({ onEvent(Event.DeleteShare(share.id)) }) { Icon(Icons.Default.Close, "Remove") }
        }
        if (expanded) share.items.forEach { item ->
            HorizontalDivider(color = MaterialTheme.colorScheme.background)
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(if (item.isAudio) Icons.Default.MusicNote else Icons.Default.PlayArrow, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(10.dp))
                Text(item.name, Modifier.weight(1f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(formatSize(item.sizeBytes), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun LazyListScope.sharedWithMe(state: LocalNetworkContract.State, onEvent: (Event) -> Unit, onAdd: () -> Unit) {
    if (state.devices.isEmpty()) {
        item {
            Column(Modifier.fillMaxWidth().padding(top = 48.dp, start = 24.dp, end = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (state.canDiscover) {
                    CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.height(16.dp))
                    Text("Looking for Max Player on this Wi-Fi…", fontSize = 15.sp)
                    Spacer(Modifier.height(6.dp))
                    Text("Files others share with you show up here. Ask them to turn on sharing.", fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                } else {
                    Text("This browser can't search the network. Add a device by the address shown on it.", fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(12.dp))
                TextButton(onAdd) { Text("Add device by address") }
            }
        }
        return
    }
    state.devices.forEach { device ->
        item(key = "dev-" + device.baseUrl) { DeviceHeader(device, onEvent) }
        when {
            device.loading && device.shares.isEmpty() -> item(key = "load-" + device.baseUrl) {
                Row(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) { CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) }
            }
            device.error != null && device.shares.isEmpty() -> item(key = "err-" + device.baseUrl) {
                Text(device.error.orEmpty(), Modifier.padding(horizontal = 20.dp, vertical = 4.dp), fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
            }
            device.shares.isEmpty() -> item(key = "none-" + device.baseUrl) { Hint("Nothing shared right now.") }
            else -> items(device.shares, key = { device.baseUrl + "/" + it.id }) { RemoteShareCard(it, device, onEvent) }
        }
    }
    item { TextButton(onAdd, Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) { Text("Add device by address") } }
}

@Composable
private fun DeviceHeader(device: LocalNetworkContract.RemoteDevice, onEvent: (Event) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 16.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(36.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
            Icon(platformIcon(device.platform), null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(device.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(device.baseUrl.substringAfter("://") + if (device.discovered) "" else " · added by address", fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (!device.discovered) IconButton({ onEvent(Event.RemoveAddress(device.baseUrl)) }) { Icon(Icons.Default.Close, "Forget device") }
    }
}

@Composable
private fun RemoteShareCard(share: RemoteShare, device: LocalNetworkContract.RemoteDevice, onEvent: (Event) -> Unit) {
    val open = device.unlocked[share.id]?.share ?: share.takeIf { !it.locked }
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            if (share.locked) { Icon(Icons.Default.Lock, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)) }
            Text(share.title, Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("${share.itemCount} ${if (share.itemCount == 1) "file" else "files"}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (open == null) {
            var code by remember(share.id) { mutableStateOf("") }
            Row(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    code, { code = it.filter { c -> c.isLetterOrDigit() }.take(12) }, Modifier.weight(1f), singleLine = true,
                    placeholder = { Text("Enter code") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                )
                Spacer(Modifier.width(10.dp))
                Button({ onEvent(Event.Unlock(device.baseUrl, share, code)) }, enabled = code.isNotEmpty()) { Text("Unlock") }
            }
            device.unlockError[share.id]?.let { Text(it, Modifier.padding(start = 14.dp, bottom = 12.dp), color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
        } else {
            open.items.forEachIndexed { i, item ->
                HorizontalDivider(color = MaterialTheme.colorScheme.background)
                Row(Modifier.fillMaxWidth().clickable { onEvent(Event.Play(device.baseUrl, open, i)) }.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (item.isAudio) Icons.Default.MusicNote else Icons.Default.PlayArrow, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Text(item.name, Modifier.weight(1f), fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.width(8.dp))
                    Text(listOfNotNull(item.durationMs.takeIf { it > 0 }?.let(::formatDuration), formatSize(item.sizeBytes)).joinToString(" · "),
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun AddAddressDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    PlayerDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add device") },
        text = {
            Column {
                Text("Enter the address shown on the sharing device.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(text, { text = it }, Modifier.fillMaxWidth().padding(top = 8.dp), singleLine = true,
                    placeholder = { Text("192.168.1.5:53319") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri))
            }
        },
        confirmButton = { TextButton({ onAdd(text) }, enabled = text.isNotBlank()) { Text("OPEN") } },
        dismissButton = { TextButton(onDismiss) { Text("CANCEL") } },
    )
}

@Composable
private fun SectionTitle(text: String, top: androidx.compose.ui.unit.Dp = 8.dp) {
    Text(text.uppercase(), Modifier.padding(start = 16.dp, end = 16.dp, top = top, bottom = 8.dp), fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun Hint(text: String) {
    Text(text, Modifier.padding(horizontal = 16.dp, vertical = 6.dp), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

private fun platformIcon(platform: String): ImageVector = if (platform.equals("Android", ignoreCase = true)) Icons.Default.PhoneAndroid else Icons.Default.Computer

/** How a share reads in "My shares": open with time left, or why it is closed. */
internal data class ShareStatus(val open: Boolean, val label: String)

internal fun shareStatus(share: Share, nowMs: Long): ShareStatus {
    val starts = share.startsAtMs
    val expires = share.expiresAtMs
    return when {
        share.revoked -> ShareStatus(false, "Stopped")
        share.isExpired(nowMs) -> ShareStatus(false, "Expired")
        starts != null && nowMs < starts -> ShareStatus(false, "Starts in ${timeLeft(starts - nowMs)}")
        expires == null -> ShareStatus(true, "No expiry")
        else -> ShareStatus(true, "Expires in ${timeLeft(expires - nowMs)}")
    }
}

internal fun timeLeft(ms: Long): String {
    val minutes = (ms / 60_000).coerceAtLeast(1)
    val days = minutes / 1440
    val hours = minutes % 1440 / 60
    val mins = minutes % 60
    return when {
        days > 0 -> "${days}d ${hours}h"
        hours > 0 -> "${hours}h ${mins}m"
        else -> "${mins}m"
    }
}
