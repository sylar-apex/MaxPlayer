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
    val browsing = state.browsing
    if (browsing != null) {
        DeviceSharesContent(browsing, onEvent)
        return
    }
    var adding by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopBar("Network share", onBack = onBack, actions = {
            IconButton({ adding = true }) { Icon(Icons.Default.Add, "Add device by address") }
        })
        LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
            if (state.canHost) hostSection(state, onEvent)
            nearbySection(state, onEvent, onAdd = { adding = true })
            item { Spacer(Modifier.navigationBarsPadding().height(24.dp)) }
        }
    }
    if (adding) AddAddressDialog(onDismiss = { adding = false }, onAdd = { adding = false; onEvent(Event.AddAddress(it)) })
}

private fun LazyListScope.hostSection(state: LocalNetworkContract.State, onEvent: (Event) -> Unit) {
    item { SectionTitle("This device") }
    item {
        val host = state.host
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Share on this network", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Text(
                        when (host) {
                            is HostState.Running -> "On · others on this Wi-Fi can open your shares"
                            HostState.Starting -> "Starting…"
                            is HostState.Failed -> host.reason
                            HostState.Stopped -> "Off"
                        },
                        fontSize = 13.sp,
                        color = if (host is HostState.Failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(host is HostState.Running || host == HostState.Starting, { on -> onEvent(if (on) Event.StartHosting else Event.StopHosting) })
            }
            if (host is HostState.Running && host.addresses.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
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
    item { SectionTitle("My shares", top = 20.dp) }
    if (state.shares.isEmpty()) {
        item { Hint("Select videos or songs in your library and choose \"Share on network\".") }
    }
    items(state.shares, key = { "share-" + it.id }) { MyShareRow(it, state.nowMs, onEvent) }
}

@Composable
private fun MyShareRow(share: Share, nowMs: Long, onEvent: (Event) -> Unit) {
    val status = shareStatus(share, nowMs)
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(share.title, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val access = if (share.access == ShareAccess.Public) "Public" else "Code ${share.code}"
            Text("${share.items.size} ${if (share.items.size == 1) "file" else "files"} · $access", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(status.label, fontSize = 12.sp, color = if (status.open) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
        }
        if (status.open) TextButton({ onEvent(Event.RevokeShare(share.id)) }) { Text("Stop") }
        IconButton({ onEvent(Event.DeleteShare(share.id)) }) { Icon(Icons.Default.Close, "Remove") }
    }
}

private fun LazyListScope.nearbySection(state: LocalNetworkContract.State, onEvent: (Event) -> Unit, onAdd: () -> Unit) {
    item { SectionTitle("Nearby devices", top = 20.dp) }
    if (state.canDiscover && state.nearby.isEmpty()) {
        item {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(12.dp))
                Text("Looking for Max Player on this Wi-Fi…", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    if (!state.canDiscover) {
        item { Hint("This browser can't search the network. Add a device by the address shown on it.") }
    }
    items(state.nearby, key = { "near-" + it.id }) { d ->
        DeviceRow(platformIcon(d.platform), d.name, d.baseUrl.substringAfter("://"), onClick = { onEvent(Event.OpenDevice(d.name, d.baseUrl)) })
    }
    val saved = state.savedAddresses.filter { url -> state.nearby.none { it.baseUrl == url } }
    items(saved, key = { "saved-$it" }) { url ->
        DeviceRow(Icons.Default.Wifi, url.substringAfter("://"), "Added by address", onClick = { onEvent(Event.OpenDevice(url.substringAfter("://"), url)) }) {
            IconButton({ onEvent(Event.RemoveAddress(url)) }) { Icon(Icons.Default.Close, "Forget") }
        }
    }
    item { TextButton(onAdd, Modifier.padding(horizontal = 8.dp)) { Text("Add device by address") } }
}

@Composable
private fun DeviceRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit, trailing: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing()
    }
}

@Composable
private fun DeviceSharesContent(b: LocalNetworkContract.Browsing, onEvent: (Event) -> Unit) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton({ onEvent(Event.CloseDevice) }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            Column(Modifier.weight(1f)) {
                Text(b.name, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(b.baseUrl.substringAfter("://"), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton({ onEvent(Event.RefreshDevice) }) { Icon(Icons.Default.Refresh, "Refresh") }
        }
        HorizontalDivider()
        when {
            b.loading && b.shares.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            b.error != null -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(b.error.orEmpty(), color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(12.dp))
                    Button({ onEvent(Event.RefreshDevice) }) { Text("Try again") }
                }
            }
            b.shares.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nothing is shared on this device right now.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> LazyColumn(Modifier.fillMaxSize()) {
                items(b.shares, key = { it.id }) { share -> RemoteShareCard(share, b, onEvent) }
                item { Spacer(Modifier.navigationBarsPadding().height(24.dp)) }
            }
        }
    }
}

@Composable
private fun RemoteShareCard(share: RemoteShare, b: LocalNetworkContract.Browsing, onEvent: (Event) -> Unit) {
    val open = b.unlocked[share.id]?.share ?: share.takeIf { !it.locked }
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
                Button({ onEvent(Event.Unlock(share, code)) }, enabled = code.isNotEmpty()) { Text("Unlock") }
            }
            b.unlockError[share.id]?.let { Text(it, Modifier.padding(start = 14.dp, bottom = 12.dp), color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
        } else {
            open.items.forEachIndexed { i, item ->
                HorizontalDivider(color = MaterialTheme.colorScheme.background)
                Row(Modifier.fillMaxWidth().clickable { onEvent(Event.Play(open, i)) }.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
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
