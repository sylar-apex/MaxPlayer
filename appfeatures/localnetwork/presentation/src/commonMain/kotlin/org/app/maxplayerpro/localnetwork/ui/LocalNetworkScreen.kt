package org.app.maxplayerpro.localnetwork.ui

import org.app.maxplayerpro.sharedlogic.platform.PlatformApp
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import org.app.maxplayerpro.localnetwork.share.SaveLocation
import org.app.maxplayerpro.localnetwork.share.DownloadState
import org.app.maxplayerpro.localnetwork.share.DownloadTask
import androidx.compose.material.icons.filled.Download
import androidx.compose.foundation.layout.heightIn
import org.app.maxplayerpro.localnetwork.share.ViewerSession
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.animation.AnimatedVisibility
import org.app.maxplayerpro.appcore.component.SharedGreen
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.SensorsOff
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.app.kmediaplayer.model.MediaFile
import org.app.kmediaplayer.ui.common.PlayerDialog
import org.app.kmediaplayer.util.formatDuration
import org.app.kmediaplayer.util.formatSize
import org.app.maxplayerpro.appcore.component.FolderGlyph
import org.app.maxplayerpro.appcore.component.PullRefresh
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
    val open = state.open
    if (open != null) {
        DeviceContent(state, open, state.refreshing, onEvent)
    } else {
        DeviceListContent(state, onBack, onEvent)
    }
    state.prompt?.let { PinDialog(it, deviceName = open?.name.orEmpty(), onEvent) }
}

// ---- Tabs: devices on the network, and this device's sharing ----

@Composable
private fun DeviceListContent(state: LocalNetworkContract.State, onBack: () -> Unit, onEvent: (Event) -> Unit) {
    var adding by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopBar("Network", onBack = onBack, actions = {
            IconButton({ onEvent(Event.Refresh) }) { Icon(Icons.Default.Refresh, "Refresh") }
            IconButton({ adding = true }) { Icon(Icons.Default.Add, "Add device by address") }
        })
        val tabs = if (state.canHost) LocalNetworkContract.Tab.entries else listOf(LocalNetworkContract.Tab.SharedWithMe)
        val tab = if (state.canHost) state.tab else LocalNetworkContract.Tab.SharedWithMe
        if (tabs.size > 1) {
            PrimaryTabRow(tabs.indexOf(tab), containerColor = MaterialTheme.colorScheme.background) {
                tabs.forEach { t ->
                    val count = when (t) {
                        LocalNetworkContract.Tab.SharedWithMe -> state.devices.count { it.reachable }
                        LocalNetworkContract.Tab.SharedByMe -> state.shares.count { shareStatus(it, state.nowMs).open }
                    }
                    Tab(tab == t, { onEvent(Event.SelectTab(t)) }, text = { Text(if (count > 0) "${t.label} ($count)" else t.label, fontWeight = FontWeight.Medium) })
                }
            }
        }
        PullRefresh(state.refreshing, { onEvent(Event.Refresh) }, Modifier.fillMaxWidth().weight(1f)) {
        LazyColumn(Modifier.fillMaxSize()) {
            when (tab) {
                LocalNetworkContract.Tab.SharedWithMe -> devicesList(state, onEvent, onAdd = { adding = true })
                LocalNetworkContract.Tab.SharedByMe -> sharedByMe(state, onEvent)
            }
            item { Spacer(Modifier.navigationBarsPadding().height(24.dp)) }
        }
        }
    }
    if (adding) AddAddressDialog(onDismiss = { adding = false }, onAdd = { adding = false; onEvent(Event.AddAddress(it)) })
}

private fun LazyListScope.devicesList(state: LocalNetworkContract.State, onEvent: (Event) -> Unit, onAdd: () -> Unit) {
    if (state.devices.isEmpty()) {
        item {
            Column(Modifier.fillMaxWidth().padding(top = 72.dp, start = 32.dp, end = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(88.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Sensors, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(20.dp))
                Text(if (state.canDiscover) "Looking for devices…" else "Add a device", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text(
                    if (state.canDiscover) "Max Player on this Wi-Fi with sharing turned on shows up here."
                    else "This browser can't search the network. Add a device by the address shown on it.",
                    fontSize = 13.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
                )
                if (state.canDiscover) {
                    Spacer(Modifier.height(18.dp))
                    LinearProgressIndicator(Modifier.width(120.dp).height(3.dp))
                }
                Spacer(Modifier.height(24.dp))
                OutlinedButton(onAdd) { Text("Add device by address") }
            }
        }
        return
    }
    items(state.devices, key = { it.baseUrl }) { d ->
        val status = when {
            !d.reachable -> "Not reachable"
            d.pinRequired && !d.signedIn -> "PIN required"
            d.pinRequired -> "Signed in"
            else -> "Open"
        }
        Row(Modifier.fillMaxWidth().clickable(enabled = d.reachable) { onEvent(Event.OpenDevice(d.baseUrl)) }.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Icon(platformIcon(d.platform), null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(d.name, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(d.baseUrl.substringAfter("://") + " · " + status + if (d.discovered) "" else " · added by address", fontSize = 12.sp,
                    color = if (d.reachable) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error)
            }
            if (d.pinRequired) Icon(if (d.signedIn) Icons.Default.LockOpen else Icons.Default.Lock, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!d.discovered) IconButton({ onEvent(Event.RemoveAddress(d.baseUrl)) }) { Icon(Icons.Default.Close, "Forget device") }
            else Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    item { TextButton(onAdd, Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) { Text("Add device by address") } }
}

private fun LazyListScope.sharedByMe(state: LocalNetworkContract.State, onEvent: (Event) -> Unit) {
    item { HostCard(state, onEvent) }
    item { SectionTitle("Shared by you", top = 20.dp) }
    if (state.shares.isEmpty()) {
        item { Hint("Nothing shared. Everything is private until you share it: tap ⋮ on a folder or video and choose \"Share on network\".") }
    }
    items(state.shares, key = { "share-" + it.id }) { MyShareRow(it, state.nowMs, onEvent) }
}

@Composable
private fun HostCard(state: LocalNetworkContract.State, onEvent: (Event) -> Unit) {
    val host = state.host
    val on = host is HostState.Running || host == HostState.Starting
    var editingPin by remember { mutableStateOf(false) }
    var editingLimit by remember { mutableStateOf(false) }
    var expanded by rememberSaveable { mutableStateOf(false) }
    var showingDevices by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Card {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(if (on) Icons.Default.Sensors else Icons.Default.SensorsOff, null, Modifier.size(26.dp),
                    tint = if (host is HostState.Running) SharedGreen else MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Sharing", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        when (host) {
                            is HostState.Running -> "On · visible on this Wi-Fi"
                            HostState.Starting -> "Starting…"
                            is HostState.Failed -> host.reason
                            HostState.Stopped -> "Off · this device is hidden"
                        },
                        fontSize = 13.sp,
                        color = if (host is HostState.Failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(on, { onEvent(if (it) Event.StartHosting else Event.StopHosting) })
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.background)
            // Settings fold away so the shared list stays in view; the summary says what they are set to.
            Row(Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Settings, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text("Settings", fontSize = 15.sp)
                    val limit = state.device.untilMs?.let { "${timeLeft((it - state.nowMs).coerceAtLeast(0))} left" } ?: "No limit"
                    Text("PIN ${if (state.device.pin != null) "on" else "off"} · $limit", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, if (expanded) "Hide settings" else "Show settings",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            AnimatedVisibility(expanded) {
                Column {
                    if (host is HostState.Running && host.addresses.isNotEmpty()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.background)
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Language, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Open in a browser", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                SelectionContainer { Column { host.addresses.forEach { Text(it, fontSize = 15.sp, fontWeight = FontWeight.Medium) } } }
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.background)
                    SettingRow(Icons.Default.Lock, "Device PIN", if (state.device.pin != null) "On" else "Off") { editingPin = true }
                    HorizontalDivider(color = MaterialTheme.colorScheme.background)
                    SettingRow(Icons.Default.Timer, "Share device for",
                        state.device.untilMs?.let { "${timeLeft((it - state.nowMs).coerceAtLeast(0))} left" } ?: "No limit") { editingLimit = true }
                    HorizontalDivider(color = MaterialTheme.colorScheme.background)
                    SettingRow(Icons.AutoMirrored.Filled.Logout, "Log out devices", state.device.sessions.size.takeIf { it > 0 }?.let { "$it signed in" } ?: "None") { showingDevices = true }
                }
            }
        }
    }
    if (showingDevices) SignedInDevicesDialog(state.device.sessions, onDismiss = { showingDevices = false }, onEvent)
    if (editingPin) DevicePinDialog(state.device.pin != null, onDismiss = { editingPin = false }) { editingPin = false; onEvent(Event.SetDevicePin(it)) }
    if (editingLimit) DeviceLimitDialog(state.device.untilMs, onDismiss = { editingLimit = false }) { editingLimit = false; onEvent(Event.SetDeviceUntil(it)) }
}

/** Devices signed in to this one; each can be logged out alone, or all at once. A logged-out device must enter the PIN again. */
@Composable
private fun SignedInDevicesDialog(sessions: Map<String, ViewerSession>, onDismiss: () -> Unit, onEvent: (Event) -> Unit) {
    PlayerDialog(
        onDismissRequest = onDismiss,
        title = { Text("Signed-in devices") },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                if (sessions.isEmpty()) Text("No device has opened this one yet.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                sessions.entries.sortedByDescending { it.value.signedInAtMs }.forEach { (id, s) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(platformIcon(s.platform), null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(s.name, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(listOf(s.platform, "since " + formatDate(s.signedInAtMs)).filter { it.isNotBlank() }.joinToString(" · "),
                                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton({ onEvent(Event.LogOutDevice(id)) }) { Text("Log out") }
                    }
                }
                Text("Logging out a device makes it enter the PIN again. Without a device PIN, it can open public shares again but needs the PINs of protected ones.",
                    Modifier.padding(top = 10.dp), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton(onDismiss) { Text("DONE") } },
        dismissButton = { TextButton({ onEvent(Event.LogOutAllDevices); onDismiss() }) { Text("LOG OUT ALL", color = MaterialTheme.colorScheme.error) } },
    )
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))) { content() }
}

@Composable
private fun SettingRow(icon: ImageVector, title: String, value: String?, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Text(title, Modifier.weight(1f), fontSize = 15.sp)
        value?.let { Text(it, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** One thing this device shares, drawn like a library row: icon, name, mode and time left, and a ⋮ to edit or make it private. */
@Composable
private fun MyShareRow(share: Share, nowMs: Long, onEvent: (Event) -> Unit) {
    val status = shareStatus(share, nowMs)
    var editing by remember(share.id) { mutableStateOf(false) }
    var menu by remember(share.id) { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().clickable { editing = true }.padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (share.isFolder || share.items.size > 1) FolderGlyph(Modifier.size(width = 62.dp, height = 46.dp))
        else Box(Modifier.size(width = 62.dp, height = 46.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
            Icon(if (share.items.firstOrNull()?.isAudio == true) Icons.Default.MusicNote else Icons.Default.Movie, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(share.title.replaceFirstChar { it.uppercaseChar() }, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (share.access == ShareAccess.Protected) {
                    Icon(Icons.Default.Lock, null, Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                }
                val mode = if (share.access == ShareAccess.Public) "Public" else "PIN ${share.code}"
                Text("$mode · ${countLabel(share.items.size, share.items.all { it.isAudio })}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(status.label, fontSize = 12.sp, color = if (status.open) SharedGreen else MaterialTheme.colorScheme.error)
        }
        Box {
            IconButton({ menu = true }) { Icon(Icons.Default.MoreVert, "Share actions", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            DropdownMenu(menu, { menu = false }) {
                DropdownMenuItem({ Text("Edit sharing") }, { menu = false; editing = true })
                DropdownMenuItem({ Text("Make private") }, { menu = false; onEvent(Event.MakePrivate(share.id)) })
            }
        }
    }
    if (editing) {
        // Reopens the share dialog on this share, so its mode, PIN and time can be changed in place.
        val files = remember(share) {
            share.items.mapIndexed { i, it -> MediaFile(i.toLong(), it.uri, it.path, it.name, it.sizeBytes, it.durationMs, 0, it.width, it.height, mimeType = it.mimeType, isAudio = it.isAudio) }
        }
        NetworkShareDialog(files, share.folderPath, onDismiss = { editing = false })
    }
}

// ---- One device's shared folders and files ----

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun DeviceContent(state: LocalNetworkContract.State, open: LocalNetworkContract.OpenDevice, refreshing: Boolean, onEvent: (Event) -> Unit) {
    val canDownload = state.canDownload
    // The file whose save sheet is open: (share with items, item index).
    var saving by remember { mutableStateOf<Pair<RemoteShare, Int>?>(null) }
    saving?.let { (share, i) ->
        share.items.getOrNull(i)?.let { item ->
            DownloadSheet(item.name, item.sizeBytes, state.downloadsLabel, onDismiss = { saving = null }) { name, location ->
                saving = null
                onEvent(Event.Download(share, i, name, location))
            }
        }
    }
    val folder = open.openShareId?.let { id -> open.shares.firstOrNull { it.id == id } }?.let { open.openable(it) }
    val group = open.openGroup
    val inside = folder != null || group != null
    BackHandler { onEvent(if (inside) Event.CloseShare else Event.CloseDevice) }
    var menu by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // TopBar keeps clear of the status bar, like every other screen.
        TopBar(folder?.title ?: group?.title ?: open.name, onBack = { onEvent(if (inside) Event.CloseShare else Event.CloseDevice) }) {
            IconButton({ onEvent(Event.Refresh) }) { Icon(Icons.Default.Refresh, "Refresh") }
            Box {
                IconButton({ menu = true }) { Icon(Icons.Default.MoreVert, "More") }
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem({ Text("Forget PINs for this device") }, { menu = false; onEvent(Event.ForgetOpenDevice) })
                }
            }
        }
        PullRefresh(refreshing, { onEvent(Event.Refresh) }, Modifier.fillMaxWidth().weight(1f)) {
        when {
            folder != null -> LazyColumn(Modifier.fillMaxSize()) {
                itemsIndexed(folder.items, key = { _, it -> it.id }) { i, item ->
                    FileRow(item.name, item.isAudio, item.durationMs, item.sizeBytes, locked = false,
                        onDownload = if (canDownload && item.downloadPath != null) ({ saving = folder to i }) else null) {
                        onEvent(Event.Play(folder, i))
                    }
                }
                item { Spacer(Modifier.navigationBarsPadding().height(24.dp)) }
            }
            group != null -> LazyColumn(Modifier.fillMaxSize()) {
                // Each file on its own row; a protected one shows a lock and asks its own PIN when tapped.
                open.loose(group).forEach { share ->
                    val ready = open.openable(share)
                    if (ready == null) item(key = share.id) {
                        FileRow(share.title, group.audio, 0, null, locked = true) { onEvent(Event.OpenShare(share)) }
                    } else itemsIndexed(ready.items, key = { _, it -> share.id + "/" + it.id }) { i, item ->
                        FileRow(item.name, item.isAudio, item.durationMs, item.sizeBytes, locked = false,
                            onDownload = if (canDownload && item.downloadPath != null) ({ saving = ready to i }) else null) { onEvent(Event.PlayInGroup(group, ready, i)) }
                    }
                }
                item { Spacer(Modifier.navigationBarsPadding().height(24.dp)) }
            }
            open.loading && open.shares.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            open.error != null -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(open.error.orEmpty(), color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton({ onEvent(Event.Refresh) }) { Text("Try again") }
                }
            }
            open.shares.isEmpty() -> Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), contentAlignment = Alignment.Center) {
                Text(if (open.loading) "" else "Nothing shared on this device right now.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> LazyColumn(Modifier.fillMaxSize()) {
                // Shared folders by their own names, then one folder per kind for files shared on their own.
                items(open.shares.filter { it.folder }.sortedBy { it.title.lowercase() }, key = { it.id }) { share -> ShareRow(share, open, onEvent) }
                items(LocalNetworkContract.Group.entries.filter { open.loose(it).isNotEmpty() }, key = { "group-" + it.name }) { g ->
                    GroupRow(g, open.loose(g).sumOf { it.itemCount }) { onEvent(Event.OpenGroup(g)) }
                }
                item { Spacer(Modifier.navigationBarsPadding().height(24.dp)) }
            }
        }
        }
        DownloadsPanel(state.downloads, onEvent)
    }
}

/** One of the folders gathering files shared on their own; a lock at the end when its files are protected. */
@Composable
private fun GroupRow(group: LocalNetworkContract.Group, count: Int, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(62.dp).clickable(onClick = onClick).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        FolderGlyph(Modifier.size(width = 62.dp, height = 46.dp))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(group.title, fontSize = 15.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.primary, maxLines = 1)
            Spacer(Modifier.height(2.dp))
            Text(countLabel(count, group.audio), fontSize = 12.sp, lineHeight = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (group.protected) LockMark()
    }
}

/** A shared folder or file, drawn like the library's own rows; a lock at the end marks a protected one not yet opened. */
@Composable
private fun ShareRow(share: RemoteShare, open: LocalNetworkContract.OpenDevice, onEvent: (Event) -> Unit) {
    val ready = open.openable(share)
    val locked = ready == null
    if (share.folder || share.itemCount > 1) {
        Row(Modifier.fillMaxWidth().height(62.dp).clickable { onEvent(Event.OpenShare(share)) }.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            FolderGlyph(Modifier.size(width = 62.dp, height = 46.dp))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(share.title.replaceFirstChar { it.uppercaseChar() }, fontSize = 15.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.primary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(countLabel(share.itemCount, ready?.items?.all { it.isAudio } == true), fontSize = 12.sp, lineHeight = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (locked) LockMark()
        }
    } else {
        val item = ready?.items?.firstOrNull()
        FileRow(item?.name ?: share.title, item?.isAudio == true, item?.durationMs ?: 0, item?.sizeBytes, locked) { onEvent(Event.OpenShare(share)) }
    }
}

@Composable
private fun FileRow(
    name: String, isAudio: Boolean, durationMs: Long, sizeBytes: Long?, locked: Boolean,
    /** Set when the owner allows saving this file. */
    onDownload: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(width = 120.dp, height = 68.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
            Icon(if (isAudio) Icons.Default.MusicNote else Icons.Default.Movie, null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            if (durationMs > 0) Text(formatDuration(durationMs), Modifier.align(Alignment.BottomEnd).padding(4.dp)
                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(3.dp)).padding(horizontal = 4.dp, vertical = 1.dp),
                color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(name.substringBeforeLast('.').ifEmpty { name }, fontSize = 15.sp, lineHeight = 19.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            sizeBytes?.let { Text(formatSize(it), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        if (locked) LockMark()
        onDownload?.let { IconButton(it) { Icon(Icons.Default.Download, "Download", tint = MaterialTheme.colorScheme.onSurfaceVariant) } }
    }
}

@Composable
private fun LockMark() {
    Icon(Icons.Default.Lock, "Protected", Modifier.padding(start = 8.dp).size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
}

private fun countLabel(n: Int, audio: Boolean): String = if (audio) "$n ${if (n == 1) "song" else "songs"}" else "$n ${if (n == 1) "video" else "videos"}"

/** Downloads in progress and just finished: progress, cancel, and where finished ones were saved. */
@Composable
private fun DownloadsPanel(tasks: List<DownloadTask>, onEvent: (Event) -> Unit) {
    if (tasks.isEmpty()) return
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Downloads", Modifier.weight(1f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            if (tasks.any { it.state != DownloadState.Running }) TextButton({ onEvent(Event.ClearDownloads) }) { Text("Clear") }
        }
        tasks.takeLast(4).forEach { t ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(t.name, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    when (t.state) {
                        DownloadState.Running -> {
                            val p = if (t.sizeBytes > 0) t.doneBytes.toFloat() / t.sizeBytes else 0f
                            LinearProgressIndicator({ p }, Modifier.fillMaxWidth().padding(vertical = 4.dp))
                            Text("${formatSize(t.doneBytes)} of ${formatSize(t.sizeBytes)} · ${(p * 100).toInt()}%", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        DownloadState.Done -> Text("Saved to ${t.detail}", fontSize = 11.sp, color = SharedGreen, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        DownloadState.Failed -> Text(t.detail ?: "Failed", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                        DownloadState.Cancelled -> Text("Cancelled", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (t.state == DownloadState.Running) IconButton({ onEvent(Event.CancelDownload(t.id)) }) { Icon(Icons.Default.Close, "Cancel download") }
            }
        }
    }
}

/** Asks for the file name and where to save it (Downloads by default), then starts the download. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DownloadSheet(name: String, sizeBytes: Long, defaultLabel: String, onDismiss: () -> Unit, onDownload: (String, SaveLocation) -> Unit) {
    var fileName by remember { mutableStateOf(name) }
    var location by remember { mutableStateOf<SaveLocation>(SaveLocation.Default) }
    val scope = rememberCoroutineScope()
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).navigationBarsPadding().padding(bottom = 16.dp)) {
            Text("Download", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text(formatSize(sizeBytes), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(fileName, { fileName = it }, Modifier.fillMaxWidth().padding(top = 12.dp), singleLine = true, label = { Text("File name") })
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Folder, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Save to", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(when (val l = location) { SaveLocation.Default -> defaultLabel; is SaveLocation.Folder -> l.label }, fontSize = 15.sp)
                }
                TextButton({
                    scope.launch { PlatformApp.pickFolder()?.let { (id, label) -> location = SaveLocation.Folder(id, label) } }
                }) { Text("Change") }
            }
            Button({ onDownload(fileName.trim(), location) }, Modifier.fillMaxWidth().padding(top = 16.dp), enabled = fileName.isNotBlank()) { Text("Download") }
        }
    }
}

// ---- Dialogs and small pieces ----

@Composable
private fun PinDialog(prompt: LocalNetworkContract.PinPrompt, deviceName: String, onEvent: (Event) -> Unit) {
    var pin by remember(prompt.share?.id) { mutableStateOf("") }
    PlayerDialog(
        onDismissRequest = { onEvent(Event.CancelPin) },
        title = { Text(if (prompt.share != null) "Protected" else deviceName.ifEmpty { "Device PIN" }) },
        text = {
            Column {
                Text(prompt.share?.let { "Enter the PIN for \"${it.title}\"." } ?: "This device has a PIN. Enter it once; it's remembered until the owner logs everyone out.",
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(pin, { pin = it.filter { c -> c.isLetterOrDigit() }.take(12) }, Modifier.fillMaxWidth().padding(top = 8.dp), singleLine = true,
                    label = { Text("PIN") }, isError = prompt.error != null,
                    // Enter on the keyboard submits, so the button hidden behind the keyboard isn't needed.
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { if (pin.isNotEmpty() && !prompt.busy) onEvent(Event.SubmitPin(pin)) }))
                prompt.error?.let { Text(it, Modifier.padding(top = 6.dp), color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
            }
        },
        confirmButton = { TextButton({ onEvent(Event.SubmitPin(pin)) }, enabled = pin.isNotEmpty() && !prompt.busy) { Text(if (prompt.busy) "CHECKING…" else "OPEN") } },
        dismissButton = { TextButton({ onEvent(Event.CancelPin) }) { Text("CANCEL") } },
    )
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
        confirmButton = { TextButton({ onAdd(text) }, enabled = text.isNotBlank()) { Text("ADD") } },
        dismissButton = { TextButton(onDismiss) { Text("CANCEL") } },
    )
}

@Composable
private fun SectionTitle(text: String, top: Dp = 8.dp) {
    Text(text.uppercase(), Modifier.padding(start = 16.dp, end = 16.dp, top = top, bottom = 8.dp), fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun Hint(text: String) {
    Text(text, Modifier.padding(horizontal = 16.dp, vertical = 6.dp), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

private fun platformIcon(platform: String): ImageVector = if (platform.equals("Android", ignoreCase = true)) Icons.Default.PhoneAndroid else Icons.Default.Computer

/** How a share reads in "Shared by me": open with time left, or why it is closed. */
internal data class ShareStatus(val open: Boolean, val label: String)

internal fun shareStatus(share: Share, nowMs: Long): ShareStatus {
    val starts = share.startsAtMs
    val expires = share.expiresAtMs
    return when {
        share.revoked -> ShareStatus(false, "Stopped")
        share.isExpired(nowMs) -> ShareStatus(false, "Expired")
        starts != null && nowMs < starts -> ShareStatus(false, "Starts in ${timeLeft(starts - nowMs)}")
        expires == null -> ShareStatus(true, "No time limit")
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
