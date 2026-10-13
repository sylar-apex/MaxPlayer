package org.app.nxplayer.ui.me

import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.ui.res.painterResource
import org.app.nxplayer.R
import androidx.compose.material3.LocalContentColor
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PermMedia
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.app.nxplayer.core.Keys
import org.app.nxplayer.core.MediaFile
import org.app.nxplayer.core.formatSize
import org.app.nxplayer.data.AppGraph
import org.app.nxplayer.player.PlayerActivity
import org.app.nxplayer.player.PlayerLauncher
import org.app.nxplayer.ui.ColorSwatch
import org.app.nxplayer.ui.EmptyState
import org.app.nxplayer.ui.LocalNav
import org.app.nxplayer.ui.LocalOps
import org.app.nxplayer.ui.Screen
import org.app.nxplayer.ui.TextInputDialog
import org.app.nxplayer.ui.Thumbnail
import org.app.nxplayer.ui.TopBar
import org.app.nxplayer.ui.accentSwatches
import org.app.nxplayer.ui.library.FileList
import org.app.nxplayer.ui.library.viewOptions
import org.app.nxplayer.ui.settingsVersion

@Composable
fun MeScreen() {
    val nav = LocalNav.current
    val context = LocalContext.current
    var networkStream by remember { mutableStateOf(false) }
    settingsVersion()
    Column(Modifier.fillMaxSize().background(MePageColor).verticalScroll(rememberScrollState())) {
        Row(Modifier.statusBarsPadding().height(50.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton({ nav.pop() }, Modifier.padding(start = 1.dp)) { Icon(painterResource(R.drawable.ic_back), "Navigate up") }
        }
        MeCard(Modifier.padding(top = 4.dp)) {
            val tiles = listOf<Triple<Int, String, () -> Unit>>(
                Triple(R.drawable.ic_file_transfer, "File Transfer") { nav.push(Screen.Unavailable("File Transfer", "MXShare peer-to-peer transfer (Send/Receive, share with Jio/iOS/PC/Tablet) uses MX's proprietary protocol and is not implemented.")) },
                Triple(R.drawable.ic_private_folder, "Private Folder") { nav.push(Screen.Private) },
                Triple(R.drawable.ic_video_playlists, "Video Playlists") { nav.push(Screen.Playlists) },
                Triple(R.drawable.ic_network_stream, "Network Stream") { networkStream = true },
                Triple(R.drawable.ic_local_network, "Local Network") { nav.push(Screen.Networks) },
                Triple(R.drawable.ic_media_manager, "Media Manager") { nav.push(Screen.MediaManager) },
                Triple(R.drawable.ic_cloud_drive, "Cloud Drive") { nav.push(Screen.Unavailable("Cloud Drive", "Google Drive, Dropbox and OneDrive need OAuth client registrations for this app; not implemented.")) },
            )
            Column(Modifier.padding(start = 9.5.dp, end = 9.5.dp, top = 6.dp, bottom = 6.dp)) {
                tiles.chunked(3).forEach { row ->
                    Row {
                        row.forEach { (icon, label, action) -> Tile(icon, label, Modifier.weight(1f), action) }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
        MeCard(Modifier.padding(top = 8.dp)) {
            MeRow(R.drawable.ic_status_saver, "WhatsApp Status Saver", height = 52, tint = Color(0xFF25D366)) {
                nav.push(Screen.Unavailable("WhatsApp Status Saver", "Saving statuses needs access to WhatsApp's private media folder, which Android only grants through a per-folder SAF permission; not implemented."))
            }
        }
        MeCard(Modifier.padding(top = 9.dp), vertical = 3) {
            MeRow(R.drawable.ic_app_theme, "App Theme") { nav.push(Screen.Theme) }
            MeRow(R.drawable.ic_settings, "Settings") { nav.push(Screen.Settings) }
        }
        MeCard(Modifier.padding(top = 10.dp), vertical = 3) {
            MeRow(R.drawable.ic_legal, "Legal") { nav.push(Screen.Unavailable("Legal", "NxPlayer — an independent media player. Implements features equivalent to the reference player without using its code or assets.")) }
            MeRow(R.drawable.ic_help, "Help") { nav.push(Screen.Help) }
            if (AppGraph.settings[Keys.quitButton]) MeRow(R.drawable.ic_quit, "Quit") {
                org.app.nxplayer.playback.PlayerHolder.stopBackground(context); org.app.nxplayer.playback.PlayerHolder.release()
                (context as? android.app.Activity)?.finishAndRemoveTask()
            }
        }
    }
    if (networkStream) NetworkStreamDialog { networkStream = false }
}

private val MePageColor = Color(0xFF161A1E)

@Composable
private fun MeCard(modifier: Modifier, vertical: Int = 0, content: @Composable () -> Unit) {
    Column(modifier.padding(horizontal = 16.dp).fillMaxWidth().background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp)).padding(vertical = vertical.dp)) { content() }
}

@Composable
private fun Tile(icon: Int, label: String, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.height(66.dp).clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(painterResource(icon), null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(4.dp))
        Text(label, Modifier.width(90.dp), fontSize = 12.sp, lineHeight = 14.5.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun MeRow(icon: Int, label: String, height: Int = 44, tint: Color = LocalContentColor.current, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(height.dp).clickable(onClick = onClick).padding(start = 12.dp, end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(icon), null, Modifier.size(20.dp), tint = tint)
        Spacer(Modifier.width(12.dp))
        Text(label, Modifier.weight(1f), fontSize = 14.sp, lineHeight = 17.sp)
        Icon(painterResource(R.drawable.ic_chevron_right), null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun NetworkStreamDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var url by remember { mutableStateOf("") }
    org.app.nxplayer.player.PlayerDialog(
        onDismissRequest = onDismiss,
        title = { Text("Network Stream") },
        text = {
            Column {
                Text("Please enter a network URL:")
                Text("Example: http://www.example.com/video.mkv", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                androidx.compose.material3.TextField(url, { url = it }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    colors = androidx.compose.material3.TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent))
            }
        },
        confirmButton = {
            TextButton({
                val u = url.trim()
                if (u.contains("://")) { PlayerLauncher.playUrl(context, u); onDismiss() }
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onDismiss) { Text("CANCEL") } },
    )
}

@Composable
fun ThemeScreen() {
    val nav = LocalNav.current
    settingsVersion()
    val mode = AppGraph.settings[Keys.appTheme]
    val accent = AppGraph.settings[Keys.accentColor]
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState())) {
        TopBar("App Theme", onBack = { nav.pop() })
        Keys.appTheme.choices.chunked(2).forEach { row ->
            Row(Modifier.padding(horizontal = 12.dp)) {
                row.forEach { (key, label) ->
                    val preview = when (key) { "light" -> Color.White; "black" -> Color.Black; "dark" -> Color(0xFF17202B); else -> Color(0xFF6B7A89) }
                    Box(
                        Modifier.weight(1f).padding(6.dp).height(80.dp).background(preview, RoundedCornerShape(10.dp))
                            .border(if (key == mode) 3.dp else 1.dp, if (key == mode) MaterialTheme.colorScheme.primary else Color.Gray, RoundedCornerShape(10.dp))
                            .clickable { AppGraph.settings[Keys.appTheme] = key }.padding(12.dp),
                    ) {
                        Text(label, color = if (key == "light") Color.Black else Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.CenterStart))
                        if (key == mode) Text("Using", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, modifier = Modifier.align(Alignment.TopEnd))
                    }
                }
            }
        }
        Text("Classic Themes", Modifier.padding(start = 16.dp, top = 16.dp, bottom = 16.dp), fontSize = 16.sp, fontWeight = FontWeight.Medium)
        FlowRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            accentSwatches.forEach { c ->
                Box(Modifier.size(48.dp).background(Color(c), RoundedCornerShape(4.dp))
                    .border(if (c == accent) 2.dp else 0.dp, if (c == accent) Color.White else Color.Transparent, RoundedCornerShape(4.dp))
                    .clickable { AppGraph.settings[Keys.accentColor] = c })
            }
        }
    }
}

@Composable
fun PlaylistsScreen() {
    val nav = LocalNav.current
    val user by AppGraph.userData.data.collectAsState()
    var creating by remember { mutableStateOf(false) }
    val lists = user.playlists.filter { !it.isMusic }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopBar("VIDEO PLAYLISTS") { IconButton({ nav.pop() }) { Icon(painterResource(org.app.nxplayer.R.drawable.ic_close), "Close") } }
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth().height(36.dp).border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp)).clickable { creating = true },
            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text("Create New Playlist", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
        if (lists.isEmpty()) EmptyState("No Playlists found.")
        LazyColumn {
            items(lists, key = { it.id }) { p ->
                Row(Modifier.fillMaxWidth().clickable { nav.push(Screen.PlaylistDetail(p.id)) }.padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.PlaylistPlay, null, Modifier.size(36.dp))
                    Spacer(Modifier.width(16.dp))
                    Column { Text(p.name, fontSize = 17.sp); Text("${p.uris.size} videos", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
    }
    if (creating) TextInputDialog("Create New Playlist", "", onDismiss = { creating = false }, label = "Playlist name") { name ->
        if (name.isNotBlank()) AppGraph.userData.update { it.createPlaylist(name, false, System.currentTimeMillis()) }
        creating = false
    }
}

@Composable
fun PlaylistDetailScreen(id: Long) {
    val nav = LocalNav.current
    val ops = LocalOps.current
    val user by AppGraph.userData.data.collectAsState()
    val videos by AppGraph.media.videos.collectAsState()
    val audio by AppGraph.media.audio.collectAsState()
    val p = user.playlists.firstOrNull { it.id == id } ?: run { EmptyState("Playlist deleted."); return }
    val byUri = (videos + audio).associateBy { it.uri }
    val files = p.uris.mapNotNull { byUri[it] }
    var renaming by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopBar(p.name, onBack = { nav.pop() }) {
            IconButton({ if (files.isNotEmpty()) ops.play(files, 0) }) { Icon(Icons.AutoMirrored.Filled.PlaylistPlay, "Play all") }
            IconButton({ renaming = true }) { Icon(Icons.Default.Edit, "Rename") }
            IconButton({ AppGraph.userData.update { it.deletePlaylist(id) }; nav.pop() }) { Icon(Icons.Default.Delete, "Delete playlist") }
        }
        LazyColumn {
            items(files, key = { it.uri }) { f ->
                Row(Modifier.fillMaxWidth().clickable { ops.play(files, files.indexOf(f)) }.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Thumbnail(f, Modifier.size(width = 110.dp, height = 62.dp), true)
                    Spacer(Modifier.width(12.dp))
                    Text(f.title, Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    IconButton({ AppGraph.userData.update { it.removeFromPlaylist(id, f.uri) } }) { Icon(Icons.Default.Delete, "Remove") }
                }
            }
        }
        if (files.isEmpty()) EmptyState("Add videos from a file's ⋮ menu → Add To Playlist.")
    }
    if (renaming) TextInputDialog("Rename playlist", p.name, onDismiss = { renaming = false }) { AppGraph.userData.update { d -> d.renamePlaylist(id, it) }; renaming = false }
}

@Composable
fun PrivateFolderScreen() {
    val nav = LocalNav.current
    val ops = LocalOps.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pf = AppGraph.privateFolder
    var unlocked by remember { mutableStateOf(false) }
    val files by pf.files.collectAsState()
    var menuFor by remember { mutableStateOf<java.io.File?>(null) }
    if (!unlocked) { PinGate(isSetup = !pf.hasPin, onCancel = { nav.pop() }) { unlocked = true }; return }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column {
            TopBar("Private Folder", onBack = { nav.pop() })
            if (files.isEmpty()) EmptyState("You haven't added any files yet.\nUse ⋮ → Lock in Private Folder on any video.")
            LazyColumn {
                items(files, key = { it.path }) { f ->
                    Row(Modifier.fillMaxWidth().clickable {
                        context.startActivity(Intent(context, PlayerActivity::class.java).setData(Uri.fromFile(f)))
                    }.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, null)
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(f.name, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(formatSize(f.length()), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton({ menuFor = f }) { Icon(Icons.Default.Edit, "Options") }
                    }
                }
            }
        }
        FloatingActionButton({ nav.pop() }, Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(20.dp)) { Icon(Icons.Default.Add, "Add files") }
    }
    menuFor?.let { f ->
        var renaming by remember { mutableStateOf(false) }
        org.app.nxplayer.player.PlayerDialog(
            onDismissRequest = { menuFor = null },
            title = { Text(f.name, maxLines = 2) },
            text = {
                Column {
                    Text("Move out of Private Folder", Modifier.fillMaxWidth().clickable { scope.launch { pf.restore(f); AppGraph.media.refresh(); ops.toast("Moved to Movies/NxPlayer") }; menuFor = null }.padding(vertical = 12.dp))
                    Text("Rename", Modifier.fillMaxWidth().clickable { renaming = true }.padding(vertical = 12.dp))
                    Text("Delete", Modifier.fillMaxWidth().clickable { pf.delete(f); menuFor = null }.padding(vertical = 12.dp))
                }
            },
            confirmButton = { TextButton({ menuFor = null }) { Text("Cancel") } },
        )
        if (renaming) TextInputDialog("Rename", f.name, onDismiss = { renaming = false }) { pf.rename(f, it); renaming = false; menuFor = null }
    }
}

@Composable
private fun PinGate(isSetup: Boolean, onCancel: () -> Unit, onUnlocked: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf("") }
    val title = when { !isSetup -> "Enter PIN"; confirm == null -> "Create a 4-digit PIN"; else -> "Confirm PIN" }
    org.app.nxplayer.player.PlayerDialog(
        onDismissRequest = onCancel,
        title = { Text("Private Folder") },
        text = {
            Column {
                Text(title)
                androidx.compose.material3.TextField(pin, { if (it.length <= 4 && it.all(Char::isDigit)) pin = it }, singleLine = true, visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    colors = androidx.compose.material3.TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent))
                if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }
        },
        confirmButton = {
            TextButton({
                if (pin.length != 4) { error = "PIN must be 4 digits"; return@TextButton }
                when {
                    !isSetup -> if (AppGraph.privateFolder.checkPin(pin)) onUnlocked() else { error = "Wrong PIN"; pin = "" }
                    confirm == null -> { confirm = pin; pin = ""; error = "" }
                    confirm == pin -> { AppGraph.privateFolder.setPin(pin); onUnlocked() }
                    else -> { error = "PINs don't match"; confirm = null; pin = "" }
                }
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onCancel) { Text("Cancel") } },
    )
}

@Composable
fun MediaManagerScreen() {
    val nav = LocalNav.current
    val videos by AppGraph.media.videos.collectAsState()
    val summary by produceState<org.app.nxplayer.data.StorageSummary?>(null, videos) { value = withContext(Dispatchers.IO) { AppGraph.media.storageSummary() } }
    val user by AppGraph.userData.data.collectAsState()
    val ops = LocalOps.current
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState())) {
        TopBar("Media Manager", onBack = { nav.pop() })
        summary?.let { s ->
            Row(Modifier.padding(horizontal = 20.dp)) {
                Text("Device storage", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                Text("Used ${formatSize(s.usedBytes)} / ${formatSize(s.totalBytes)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LinearProgressIndicator({ s.usedBytes.toFloat() / s.totalBytes }, Modifier.fillMaxWidth().padding(20.dp))
            Row(Modifier.padding(horizontal = 12.dp)) {
                listOf("Video" to s.videoBytes, "Music" to s.audioBytes, "Image" to s.imageBytes).forEach { (k, v) ->
                    Column(Modifier.weight(1f).padding(6.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)).padding(12.dp)) {
                        Text(k, fontWeight = FontWeight.SemiBold); Text(formatSize(v), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        Row(Modifier.padding(12.dp)) {
            listOf("recent" to "Recently Played", "large" to "Large Files", "added" to "Recently Added").forEach { (k, label) ->
                Box(Modifier.weight(1f).padding(6.dp).height(90.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)).clickable { nav.push(Screen.MediaList(k)) }, contentAlignment = Alignment.Center) {
                    Text(label, textAlign = TextAlign.Center, fontWeight = FontWeight.Medium)
                }
            }
        }
        Text("Haven't Played  ›", Modifier.fillMaxWidth().clickable { nav.push(Screen.MediaList("unplayed")) }.padding(20.dp), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        val unplayed = videos.filter { user.history[it.uri] == null }.take(4)
        FlowRow(Modifier.padding(horizontal = 14.dp), maxItemsInEachRow = 2) {
            unplayed.forEach { f ->
                Column(Modifier.width(180.dp).padding(6.dp).clickable { ops.play(listOf(f), 0) }) {
                    Thumbnail(f, Modifier.fillMaxWidth().height(100.dp), true)
                    Text(f.title, maxLines = 2, fontSize = 13.sp, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
fun MediaListScreen(kind: String) {
    val nav = LocalNav.current
    val videos by AppGraph.media.videos.collectAsState()
    val user by AppGraph.userData.data.collectAsState()
    val (title, files) = when (kind) {
        "recent" -> "Recently Played" to videos.filter { (user.history[it.uri]?.lastPlayedMs ?: 0) > 0 }.sortedByDescending { user.history[it.uri]!!.lastPlayedMs }
        "large" -> "Large Files" to videos.filter { it.sizeBytes > 100L * 1024 * 1024 }.sortedByDescending { it.sizeBytes }
        "added" -> "Recently Added" to videos.sortedByDescending { it.dateModifiedMs }.take(50)
        else -> "Haven't Played" to videos.filter { user.history[it.uri] == null }
    }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopBar(title, onBack = { nav.pop() })
        FileList(files, viewOptions().copy(layout = org.app.nxplayer.core.ListLayout.List, fields = setOf(org.app.nxplayer.core.ListField.Thumbnail, org.app.nxplayer.core.ListField.Length, org.app.nxplayer.core.ListField.Size)), "Nothing here.")
    }
}

@Composable
fun HelpScreen() {
    val nav = LocalNav.current
    val items = listOf(
        "Gestures" to "Swipe left/right to seek (speed set in Display Settings → Navigation). Swipe up/down on the left half for brightness, right half for volume. Pinch to zoom, drag with two fingers to pan. Double tap left/right to rewind/forward, centre to pause. Long press for fast-forward speed.",
        "Lock" to "Tap the lock button to block all touches. Tap the small lock icon in the top-left corner to unlock.",
        "Background play" to "Tap the headphones shortcut or choose Settings → Player → Background/PiP mode. Audio continues with a notification and lock-screen controls.",
        "Subtitles" to "Subtitles with the same name next to the video load automatically. Use Subtitles → Open to pick another .srt or .vtt file, and Synchronization to shift timing.",
        "Private Folder" to "Choose ⋮ → Lock in Private Folder on a video. Locked files move into app-private storage and need your PIN.",
    )
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState())) {
        TopBar("Help", onBack = { nav.pop() })
        items.forEach { (q, a) ->
            Text(q, Modifier.padding(start = 20.dp, top = 16.dp, end = 20.dp), fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
            Text(a, Modifier.padding(horizontal = 20.dp, vertical = 6.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun UnavailableScreen(title: String, reason: String) {
    val nav = LocalNav.current
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopBar(title, onBack = { nav.pop() })
        Text(reason, Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
    }
}

@Composable
fun NetworksScreen() {
    val nav = LocalNav.current
    val context = LocalContext.current
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize()) {
            TopBar("Networks", onBack = { nav.pop() })
            Column(Modifier.fillMaxWidth().weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Box(Modifier.size(56.dp).background(MaterialTheme.colorScheme.surfaceVariant, androidx.compose.foundation.shape.CircleShape), contentAlignment = Alignment.Center) {
                    Text("+", fontSize = 30.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(12.dp))
                Text("Add a server by tapping the\nPLUS BUTTON", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
        androidx.compose.material3.FloatingActionButton(
            { android.widget.Toast.makeText(context, "SMB/LAN servers are not supported yet", android.widget.Toast.LENGTH_SHORT).show() },
            Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(20.dp),
            containerColor = MaterialTheme.colorScheme.primary, shape = androidx.compose.foundation.shape.CircleShape,
        ) { Text("+", fontSize = 28.sp, color = MaterialTheme.colorScheme.onPrimary) }
    }
}
