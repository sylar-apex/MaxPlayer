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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.unit.Dp
import org.app.maxplayerpro.appcore.component.SharedGreen
import org.app.maxplayerpro.appcore.component.rememberShareMarks
import org.app.maxplayerpro.sharedlogic.data.fileUri
import okio.Path.Companion.toPath
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
import org.app.kmediaplayer.ui.music.MiniPlayer
import org.app.kmediaplayer.util.formatSize
import org.app.maxplayerpro.appcore.component.FolderGlyph
import org.app.maxplayerpro.appcore.component.PhotoRef
import org.app.maxplayerpro.appcore.component.PlacesGrid
import org.app.maxplayerpro.appcore.component.PhotoViewer
import org.app.maxplayerpro.appcore.component.PullRefresh
import org.app.maxplayerpro.appcore.component.TopBar
import org.app.maxplayerpro.files.ui.FilesContract.Event
import org.app.maxplayerpro.sharedlogic.data.FileEntry
import org.app.maxplayerpro.sharedlogic.data.fileMime
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun FilesScreen(onBack: () -> Unit, startPath: String? = null, viewModel: FilesViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsState()
    // Opened on a place (from Storage): go straight into it.
    androidx.compose.runtime.LaunchedEffect(startPath) { startPath?.let { viewModel.setEvent(Event.OpenFolder(it)) } }
    FilesContent(state, onBack, viewModel::setEvent)
}

/** List or grid in Files; kept while the app runs. */
private var gridView by mutableStateOf(false)

/** How an entry stands on the network: shared, inside a shared folder but private, or neither. */
private enum class Mark { Shared, Addable, None }

/**
 * Storage roots, then folders and files. Tap opens or plays; ⋮ or long-press shares. Inside a shared folder each
 * item carries a mark: green is shared (tap to make private), grey is private (tap to share it too).
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun FilesContent(state: FilesContract.State, onBack: () -> Unit, onEvent: (Event) -> Unit) {
    var picked by remember(state.path) { mutableStateOf(emptySet<String>()) }
    var viewing by remember { mutableStateOf<Int?>(null) }
    BackHandler(enabled = picked.isNotEmpty() || state.path != null) { if (picked.isNotEmpty()) picked = emptySet() else onEvent(Event.Up) }
    val images = state.entries.filter { !it.isDirectory && fileMime(it.name).startsWith("image/") }
    val (marksState, _) = rememberShareMarks()
    val marks = marksState.value
    fun markOf(e: FileEntry): Mark = when {
        if (e.isDirectory) e.path in marks.folders else fileUri(e.path) in marks.files || "file://${e.path}" in marks.files -> Mark.Shared
        marks.rootOf(e.path) != null -> Mark.Addable
        else -> Mark.None
    }
    fun open(e: FileEntry) = when {
        picked.isNotEmpty() -> picked = picked.toggle(e.path)
        e.isDirectory -> onEvent(Event.OpenFolder(e.path))
        fileMime(e.name).startsWith("image/") -> viewing = images.indexOf(e)
        fileMime(e.name).startsWith("video/") || fileMime(e.name).startsWith("audio/") -> onEvent(Event.Play(e))
        else -> onEvent(Event.OpenExternally(e))
    }
    fun onMark(e: FileEntry, m: Mark) = when (m) {
        Mark.Shared -> { onEvent(Event.MakePrivate(e)); PlatformMedia.showToast("Made private · no longer shared") }
        Mark.Addable -> { onEvent(Event.ShareInside(e)); PlatformMedia.showToast("Shared with this folder") }
        Mark.None -> onEvent(Event.Share(listOf(e)))
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val title = state.path?.let { p -> state.roots.firstOrNull { it.path == p }?.label ?: p.substringAfterLast('/') } ?: "Files"
        TopBar(title, onBack = { if (state.path != null) onEvent(Event.Up) else onBack() }) {
            if (state.path != null) IconButton({ gridView = !gridView }) {
                Icon(if (gridView) Icons.AutoMirrored.Filled.ViewList else Icons.Outlined.GridView, if (gridView) "List view" else "Grid view")
            }
            IconButton({ onEvent(Event.Refresh) }) { Icon(Icons.Default.Refresh, "Refresh") }
        }
        state.path?.let { Text(it, Modifier.padding(horizontal = 16.dp).padding(bottom = 6.dp), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        when {
            !state.hasAccess -> AccessNeeded(onEvent)
            else -> PullRefresh(state.loading, { onEvent(Event.Refresh) }, Modifier.fillMaxWidth().weight(1f)) {
                val grid = gridView && state.path != null
                LazyVerticalGrid(
                    if (grid) GridCells.Adaptive(132.dp) else GridCells.Fixed(1), Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = if (grid) 12.dp else 0.dp),
                    horizontalArrangement = Arrangement.spacedBy(if (grid) 8.dp else 0.dp),
                    verticalArrangement = Arrangement.spacedBy(if (grid) 8.dp else 0.dp),
                ) {
                    if (state.path == null) {
                        item(span = { GridItemSpan(maxLineSpan) }) { PlacesGrid(state.roots, onOpen = { onEvent(Event.OpenFolder(it.path)) }, Modifier.padding(horizontal = 16.dp)) }
                    } else if (state.entries.isEmpty() && !state.loading) {
                        item(span = { GridItemSpan(maxLineSpan) }) { Text("This folder is empty.", Modifier.fillMaxWidth().padding(32.dp), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    } else {
                        items(state.entries, key = { it.path }) { e ->
                            val m = markOf(e)
                            val menu: @Composable () -> Unit = { EntryMenu(e, m, onOpen = { open(e) }, onMark = { onMark(e, m) }) }
                            val markView: @Composable () -> Unit = { if (picked.isEmpty()) EntryMark(m) { onMark(e, m) } }
                            if (grid) EntryTile(e, picked.isNotEmpty(), e.path in picked, onClick = { open(e) }, onLongClick = { picked = picked.toggle(e.path) }, mark = markView, menu = menu)
                            else EntryRow(e, picked.isNotEmpty(), e.path in picked, onClick = { open(e) }, onLongClick = { picked = picked.toggle(e.path) }, mark = markView, menu = menu)
                        }
                    }
                    item(span = { GridItemSpan(maxLineSpan) }) { Spacer(Modifier.navigationBarsPadding().height(16.dp)) }
                }
            }
        }
        // Songs started here keep playing in the music player; this bar shows and controls them.
        if (picked.isEmpty()) MiniPlayer()
        if (picked.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).navigationBarsPadding().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton({ picked = emptySet() }) { Icon(Icons.Default.Close, "Cancel") }
                Text("${picked.size} selected", Modifier.weight(1f), fontWeight = FontWeight.Medium)
                IconButton({ picked = state.entries.map { it.path }.toSet() }) { Icon(Icons.Default.DoneAll, "Select all") }
                IconButton({ onEvent(Event.Share(state.entries.filter { it.path in picked })); picked = emptySet() }) { Icon(Icons.Default.Sensors, "Share on network") }
            }
        }
    }
    viewing?.let { start -> PhotoViewer(images.map { PhotoRef(it.path.toPath(), it.name) }, start, onClose = { viewing = null }) }
}

/** Green: shared, tap makes it private. Grey: inside a shared folder but private, tap shares it too. */
@Composable
private fun EntryMark(mark: Mark, onClick: () -> Unit) {
    when (mark) {
        Mark.Shared -> IconButton(onClick, Modifier.size(32.dp)) { Icon(Icons.Default.Sensors, "Shared on network. Tap to make private", Modifier.size(18.dp), tint = SharedGreen) }
        Mark.Addable -> IconButton(onClick, Modifier.size(32.dp)) {
            Icon(Icons.Default.Sensors, "Private. Tap to share with this folder", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f))
        }
        Mark.None -> {}
    }
}

@Composable
private fun EntryMenu(e: FileEntry, mark: Mark, onOpen: () -> Unit, onMark: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton({ open = true }, Modifier.size(36.dp)) { Icon(Icons.Default.MoreVert, "More", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        DropdownMenu(open, { open = false }) {
            DropdownMenuItem({ Text("Open") }, { open = false; onOpen() })
            DropdownMenuItem(
                { Text(when (mark) { Mark.Shared -> "Make private"; Mark.Addable -> "Share with this folder"; Mark.None -> "Share on network" }) },
                { open = false; onMark() },
                leadingIcon = { Icon(Icons.Default.Sensors, null, tint = if (mark == Mark.Shared) SharedGreen else MaterialTheme.colorScheme.onSurfaceVariant) },
            )
        }
    }
}

@Composable
private fun EntryThumb(e: FileEntry, modifier: Modifier, iconSize: Dp) {
    val mime = fileMime(e.name)
    Box(modifier, contentAlignment = Alignment.Center) {
        when {
            e.isDirectory -> FolderGlyph(Modifier.fillMaxSize())
            mime.startsWith("image/") -> AsyncImage(e.path.toPath(), null, Modifier.fillMaxSize().clip(RoundedCornerShape(6.dp)), contentScale = ContentScale.Crop)
            else -> Icon(iconFor(mime), null, Modifier.size(iconSize), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun subtitle(e: FileEntry) = if (e.isDirectory) "Folder" else formatSize(e.sizeBytes) + " · " + PlatformMedia.formatDateLocalized(e.modifiedMs, "medium", null)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EntryRow(e: FileEntry, picking: Boolean, picked: Boolean, onClick: () -> Unit, onLongClick: () -> Unit, mark: @Composable () -> Unit, menu: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(if (picked) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.background)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick).padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EntryThumb(e, Modifier.size(width = 52.dp, height = 40.dp), 28.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(e.name, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = if (e.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
            Text(subtitle(e), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (picking) Icon(if (picked) Icons.Default.CheckCircle else Icons.Outlined.Circle, null, tint = if (picked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        else { mark(); menu() }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EntryTile(e: FileEntry, picking: Boolean, picked: Boolean, onClick: () -> Unit, onLongClick: () -> Unit, mark: @Composable () -> Unit, menu: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .background(if (picked) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick).padding(8.dp),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1.3f)) {
            EntryThumb(e, Modifier.fillMaxSize().padding(if (e.isDirectory) 6.dp else 0.dp), 44.dp)
            Box(Modifier.align(Alignment.TopEnd)) {
                if (picking) Icon(if (picked) Icons.Default.CheckCircle else Icons.Outlined.Circle, null, tint = if (picked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                else mark()
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(top = 6.dp)) {
                Text(e.name, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                Text(subtitle(e), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!picking) menu()
        }
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
