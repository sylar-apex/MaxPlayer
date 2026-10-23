package org.app.maxplayerpro.files.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.app.kmediaplayer.platform.PlatformMedia
import org.app.kmediaplayer.util.formatSize
import org.app.maxplayerpro.appcore.component.FolderGlyph
import org.app.maxplayerpro.appcore.component.PhotoRef
import org.app.maxplayerpro.appcore.component.PhotoViewer
import org.app.maxplayerpro.appcore.component.PullRefresh
import org.app.maxplayerpro.appcore.component.TopBar
import org.app.maxplayerpro.files.ui.FilesContract.Event
import org.app.maxplayerpro.sharedlogic.data.FileEntry
import org.app.maxplayerpro.sharedlogic.data.fileMime
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun FilesScreen(onBack: () -> Unit, viewModel: FilesViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsState()
    FilesContent(state, onBack, viewModel::setEvent)
}

/** Storage roots, then folders and files. Tap opens or plays; long-press picks several to share on the network. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun FilesContent(state: FilesContract.State, onBack: () -> Unit, onEvent: (Event) -> Unit) {
    var picked by remember(state.path) { mutableStateOf(emptySet<String>()) }
    var viewing by remember { mutableStateOf<Int?>(null) }
    BackHandler(enabled = picked.isNotEmpty() || state.path != null) { if (picked.isNotEmpty()) picked = emptySet() else onEvent(Event.Up) }
    val images = state.entries.filter { !it.isDirectory && fileMime(it.name).startsWith("image/") }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val title = state.path?.let { p -> state.roots.firstOrNull { it.path == p }?.label ?: p.substringAfterLast('/') } ?: "Files"
        TopBar(title, onBack = { if (state.path != null) onEvent(Event.Up) else onBack() }) {
            IconButton({ onEvent(Event.Refresh) }) { Icon(Icons.Default.Refresh, "Refresh") }
        }
        state.path?.let { Text(it, Modifier.padding(horizontal = 16.dp).padding(bottom = 6.dp), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        when {
            !state.hasAccess -> AccessNeeded(onEvent)
            else -> PullRefresh(state.loading, { onEvent(Event.Refresh) }, Modifier.fillMaxWidth().weight(1f)) {
                LazyColumn(Modifier.fillMaxSize()) {
                    if (state.path == null) {
                        items(state.roots, key = { it.path }) { r ->
                            Row(Modifier.fillMaxWidth().clickable { onEvent(Event.OpenFolder(r.path)) }.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(if (r.label.startsWith("SD")) Icons.Default.SdStorage else Icons.Default.Storage, null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(16.dp))
                                Column { Text(r.label, fontSize = 16.sp, fontWeight = FontWeight.Medium); Text(r.path, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            }
                        }
                    } else if (state.entries.isEmpty() && !state.loading) {
                        item { Text("This folder is empty.", Modifier.fillMaxWidth().padding(32.dp), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    } else {
                        items(state.entries, key = { it.path }) { e ->
                            EntryRow(e, picked.isNotEmpty(), e.path in picked,
                                onClick = {
                                    when {
                                        picked.isNotEmpty() -> picked = picked.toggle(e.path)
                                        e.isDirectory -> onEvent(Event.OpenFolder(e.path))
                                        fileMime(e.name).startsWith("image/") -> viewing = images.indexOf(e)
                                        fileMime(e.name).startsWith("video/") || fileMime(e.name).startsWith("audio/") -> onEvent(Event.Play(e))
                                        else -> PlatformMedia.showToast("Long-press to share this file on the network")
                                    }
                                },
                                onLongClick = { picked = picked.toggle(e.path) })
                        }
                    }
                    item { Spacer(Modifier.navigationBarsPadding().height(16.dp)) }
                }
            }
        }
        if (picked.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).navigationBarsPadding().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton({ picked = emptySet() }) { Icon(Icons.Default.Close, "Cancel") }
                Text("${picked.size} selected", Modifier.weight(1f), fontWeight = FontWeight.Medium)
                IconButton({ picked = state.entries.map { it.path }.toSet() }) { Icon(Icons.Default.DoneAll, "Select all") }
                IconButton({ onEvent(Event.Share(state.entries.filter { it.path in picked })); picked = emptySet() }) { Icon(Icons.Default.Sensors, "Share on network") }
            }
        }
    }
    viewing?.let { start -> PhotoViewer(images.map { PhotoRef("file://${it.path}", it.name) }, start, onClose = { viewing = null }) }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EntryRow(e: FileEntry, picking: Boolean, picked: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    val mime = fileMime(e.name)
    Row(
        Modifier.fillMaxWidth().background(if (picked) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.background)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(width = 52.dp, height = 40.dp), contentAlignment = Alignment.Center) {
            when {
                e.isDirectory -> FolderGlyph(Modifier.fillMaxSize())
                mime.startsWith("image/") -> AsyncImage("file://${e.path}", null, Modifier.fillMaxSize().clip(RoundedCornerShape(6.dp)), contentScale = ContentScale.Crop)
                else -> Icon(iconFor(mime), null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(e.name, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = if (e.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
            Text(if (e.isDirectory) "Folder" else formatSize(e.sizeBytes) + " · " + PlatformMedia.formatDateLocalized(e.modifiedMs, "medium", null),
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (picking) Icon(if (picked) Icons.Default.CheckCircle else Icons.Outlined.Circle, null, tint = if (picked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun iconFor(mime: String): ImageVector = when {
    mime.startsWith("video/") -> Icons.Default.Movie
    mime.startsWith("audio/") -> Icons.Default.MusicNote
    mime == "application/pdf" -> Icons.Default.PictureAsPdf
    mime.contains("zip") || mime.contains("rar") || mime.contains("7z") -> Icons.Default.Archive
    mime.startsWith("text/") || mime.contains("word") || mime.contains("sheet") || mime.contains("presentation") -> Icons.Default.Description
    else -> Icons.AutoMirrored.Filled.InsertDriveFile
}

@Composable
private fun AccessNeeded(onEvent: (Event) -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Outlined.FolderOff, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Text("Allow access to all files", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        Text("To browse and share any folder or file, Max Player needs \"All files access\" in Android settings.", Modifier.padding(top = 6.dp),
            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Button({ onEvent(Event.RequestAccess) }, Modifier.padding(top = 18.dp)) { Text("Open settings") }
        TextButton({ onEvent(Event.Refresh) }) { Text("I've allowed it") }
    }
}

private fun Set<String>.toggle(key: String) = if (key in this) this - key else this + key
