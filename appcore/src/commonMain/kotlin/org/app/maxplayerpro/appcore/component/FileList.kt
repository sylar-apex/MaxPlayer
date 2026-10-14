package org.app.maxplayerpro.appcore.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.app.kmediaplayer.model.MediaFile
import org.app.kmediaplayer.model.PlaybackRecord
import org.app.kmediaplayer.platform.PlatformMedia
import org.app.kmediaplayer.ui.common.PlayerDialog
import org.app.kmediaplayer.ui.settings.TextInputDialog
import org.app.kmediaplayer.util.formatDuration
import org.app.kmediaplayer.util.formatSize
import org.app.kmediaplayer.util.groupThousands
import org.app.kmediaplayer.util.nowMs
import org.app.maxplayerpro.appcore.theme.MaxColors
import org.app.maxplayerpro.appres.resources.Res
import org.app.maxplayerpro.appres.resources.ic_fm_cut
import org.app.maxplayerpro.appres.resources.ic_fm_delete
import org.app.maxplayerpro.appres.resources.ic_fm_info
import org.app.maxplayerpro.appres.resources.ic_fm_lock
import org.app.maxplayerpro.appres.resources.ic_fm_mp3
import org.app.maxplayerpro.appres.resources.ic_fm_playlist_add
import org.app.maxplayerpro.appres.resources.ic_fm_rename
import org.app.maxplayerpro.appres.resources.ic_fm_share
import org.app.maxplayerpro.appres.resources.ic_fm_subsearch
import org.app.maxplayerpro.appres.resources.ic_fm_transfer
import org.app.maxplayerpro.appres.resources.ic_more_vert
import org.app.maxplayerpro.sharedlogic.model.ListField
import org.app.maxplayerpro.sharedlogic.model.ListLayout
import org.app.maxplayerpro.sharedlogic.model.ListViewOptions
import org.app.maxplayerpro.sharedlogic.model.Playlist
import org.app.maxplayerpro.sharedlogic.model.isNew
import org.app.maxplayerpro.sharedlogic.ui.filelist.FileListContract.Event
import org.app.maxplayerpro.sharedlogic.ui.filelist.FileListViewModel
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

private val subtitleExtensions = setOf("srt", "ass", "ssa", "vtt", "sub")

/** Media file list with multi-select, per-file actions, properties and playlists. Shared by every video list. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun FileList(
    files: List<MediaFile>,
    opts: ListViewOptions,
    emptyTitle: String,
    lastPlayed: String? = null,
    header: (@Composable () -> Unit)? = null,
    viewModel: FileListViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var actionsFor by remember { mutableStateOf<MediaFile?>(null) }
    var infoFor by remember { mutableStateOf<List<MediaFile>?>(null) }
    var addToPlaylist by remember { mutableStateOf<List<MediaFile>?>(null) }
    var renaming by remember { mutableStateOf<MediaFile?>(null) }
    var confirmDelete by remember { mutableStateOf<List<MediaFile>?>(null) }
    val selecting = selected.isNotEmpty()
    val now = remember { nowMs() }
    val listState = rememberLazyListState()
    val send = viewModel::setEvent

    LaunchedEffect(files) { send(Event.FilesShown(files)) }
    LaunchedEffect(lastPlayed, files.size) {
        if (state.scrollToLast && lastPlayed != null) {
            val i = files.indexOfFirst { it.uri == lastPlayed }
            if (i >= 0) listState.scrollToItem(i)
        }
    }
    BackHandler(selecting) { selected = emptySet() }

    fun toggle(f: MediaFile) { selected = if (f.uri in selected) selected - f.uri else selected + f.uri }
    val selFiles = files.filter { it.uri in selected }

    Column(Modifier.fillMaxSize()) {
        if (selecting) {
            Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton({ selected = emptySet() }) { Icon(Icons.Default.Close, "Cancel") }
                Text("${selected.size} / ${files.size} Selected", Modifier.weight(1f), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                IconButton({ send(Event.Play(selFiles, 0)) }) { Icon(Icons.Default.PlayArrow, "Play") }
                IconButton({ infoFor = selFiles }) { Icon(Icons.Default.Info, "Info") }
            }
        }
        header?.invoke()
        if (files.isEmpty()) EmptyState(emptyTitle, Modifier.weight(1f))
        else Box(Modifier.weight(1f)) {
            val row: @Composable (MediaFile) -> Unit = { f ->
                val tags = buildList {
                    if (state.siblingNames.any { it != f.name && it.startsWith(f.title) && it.substringAfterLast('.').lowercase() in subtitleExtensions }) add("SUB")
                    if (isNew(f, now, state.newTaggedPeriod) && state.history[f.uri] == null) add("NEW")
                }
                FileRow(
                    f, opts, highlight = state.markLastPlayed && f.uri == lastPlayed, tags = tags, record = state.history[f.uri],
                    selecting = selecting, isSelected = f.uri in selected,
                    onClick = { if (selecting) toggle(f) else send(Event.Play(files, files.indexOf(f))) },
                    onThumbClick = { if (selecting || state.selectThumbnail) toggle(f) else send(Event.Play(files, files.indexOf(f))) },
                    onLongClick = { toggle(f) },
                    onMore = { actionsFor = f },
                )
            }
            if (opts.layout == ListLayout.Grid) {
                LazyVerticalGrid(GridCells.Adaptive(170.dp), Modifier.fillMaxSize()) { items(files, key = { it.uri }) { row(it) } }
            } else {
                LazyColumn(Modifier.fillMaxSize(), state = listState) { items(files, key = { it.uri }) { row(it) } }
            }
        }
        if (selecting) {
            Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).navigationBarsPadding(), horizontalArrangement = Arrangement.SpaceEvenly) {
                IconButton({ selected = if (selected.size == files.size) emptySet() else files.map { it.uri }.toSet() }) { Icon(Icons.Default.DoneAll, "Select all") }
                IconButton({ send(Event.Share(selFiles)) }) { Icon(Icons.Default.Share, "Share") }
                IconButton({ addToPlaylist = selFiles }) { Icon(Icons.AutoMirrored.Filled.PlaylistPlay, "Add to playlist") }
                IconButton({ send(Event.LockInPrivate(selFiles)); selected = emptySet() }) { Icon(Icons.Default.Lock, "Lock") }
                IconButton({ selFiles.singleOrNull()?.let { renaming = it } }, enabled = selected.size == 1) { Icon(Icons.Default.Edit, "Rename") }
                IconButton({ confirmDelete = selFiles }) { Icon(Icons.Default.Delete, "Delete") }
            }
        }
    }

    actionsFor?.let { f ->
        FileActionsSheet(
            f, edits = state.allowEdits, onDismiss = { actionsFor = null },
            onUnavailable = { what -> send(Event.Toast("$what is not available in Max Player Pro")) },
            onLock = { send(Event.LockInPrivate(listOf(f))) }, onShare = { send(Event.Share(listOf(f))) },
            onProperties = { infoFor = listOf(f) }, onAddToPlaylist = { addToPlaylist = listOf(f) },
            onRename = { renaming = f }, onDelete = { confirmDelete = listOf(f) },
        )
    }
    infoFor?.let { PropertiesDialog(it) { infoFor = null } }
    addToPlaylist?.let { list ->
        AddToPlaylistDialog(
            playlists = state.playlists.filter { !it.isMusic },
            onAdd = { id -> send(Event.AddToPlaylist(id, list)) },
            onCreate = { name -> send(Event.CreatePlaylist(name, list, music = false)) },
        ) { addToPlaylist = null; selected = emptySet() }
    }
    renaming?.let { f -> TextInputDialog("Rename", f.title, onDismiss = { renaming = null }) { send(Event.Rename(f, it)); renaming = null; selected = emptySet() } }
    confirmDelete?.let { list ->
        PlayerDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Delete") },
            text = { Text(if (list.size == 1) "Delete \"${list[0].name}\"?" else "Delete ${list.size} files?") },
            confirmButton = { TextButton({ send(Event.Delete(list)); confirmDelete = null; selected = emptySet() }) { Text("DELETE") } },
            dismissButton = { TextButton({ confirmDelete = null }) { Text("CANCEL") } },
        )
    }
}

@Composable
private fun FileRow(
    f: MediaFile, opts: ListViewOptions, highlight: Boolean, tags: List<String>, record: PlaybackRecord?,
    selecting: Boolean, isSelected: Boolean, onClick: () -> Unit, onThumbClick: () -> Unit, onLongClick: () -> Unit, onMore: () -> Unit,
) {
    val bg = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else Color.Transparent
    val titleColor = when {
        highlight -> MaterialTheme.colorScheme.primary
        record != null -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
    }
    val details = buildList {
        if (ListField.Length in opts.fields && !opts.lengthOverThumbnail && f.durationMs > 0) add(formatDuration(f.durationMs))
        if (ListField.FileExtension in opts.fields) add(f.extension.uppercase())
        if (ListField.Resolution in opts.fields && f.width > 0) add("${f.width}x${f.height}")
        if (ListField.FrameRate in opts.fields && f.frameRate > 0) add("${f.frameRate} fps")
        if (ListField.Size in opts.fields) add(formatSize(f.sizeBytes))
        if (ListField.Date in opts.fields) add(PlatformMedia.formatDateLocalized(f.dateModifiedMs, "medium", null))
        if (ListField.PlayedTime in opts.fields && record != null && record.lastPlayedMs > 0) add("Played " + PlatformMedia.formatDateLocalized(record.lastPlayedMs, "short", "short"))
    }.joinToString(" · ")
    val showThumb = ListField.Thumbnail in opts.fields
    val lengthOnThumb = ListField.Length in opts.fields && opts.lengthOverThumbnail
    if (opts.layout == ListLayout.Grid) {
        Column(Modifier.background(bg).combinedClickable(onClick = onClick, onLongClick = onLongClick).padding(6.dp)) {
            Thumbnail(f, Modifier.fillMaxWidth().aspectRatio(16f / 9f).combinedClickable(onClick = onThumbClick, onLongClick = onLongClick), lengthOnThumb)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(f.title, Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 14.sp, color = titleColor)
                if (!selecting) IconButton(onMore, Modifier.size(32.dp)) { Icon(Icons.Default.MoreVert, "More") }
            }
            if (details.isNotEmpty()) Text(details, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
        return
    }
    Row(
        Modifier.fillMaxWidth().heightIn(min = 97.dp).background(bg).combinedClickable(onClick = onClick, onLongClick = onLongClick).padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showThumb) {
            Box {
                Thumbnail(f, Modifier.size(width = 96.dp, height = 56.dp).combinedClickable(onClick = onThumbClick, onLongClick = onLongClick), lengthOnThumb)
                if (isSelected) Checkbox(true, null, Modifier.align(Alignment.TopEnd).size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
        } else if (selecting) Checkbox(isSelected, { onClick() })
        Column(Modifier.weight(1f).align(Alignment.Top)) {
            Text(f.title, maxLines = 3, overflow = TextOverflow.Ellipsis, fontSize = 13.5.sp, lineHeight = 20.5.sp, color = titleColor, modifier = Modifier.offset(y = (-1).dp))
            if (details.isNotEmpty()) Text(details, fontSize = 11.sp, lineHeight = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            if (ListField.Path in opts.fields) Text(f.folderPath, fontSize = 11.sp, lineHeight = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (tags.isNotEmpty()) {
                Row(Modifier.padding(top = 7.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    tags.forEach { Tag(it, if (it == "NEW") MaxColors.tagNew else MaxColors.tagSubtitle) }
                }
            }
        }
        if (!selecting) IconButton(onMore, Modifier.size(width = 40.dp, height = 38.dp)) { Icon(painterResource(Res.drawable.ic_more_vert), "More", Modifier.size(20.dp)) } else Spacer(Modifier.width(12.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FileActionsSheet(
    f: MediaFile, edits: Boolean, onDismiss: () -> Unit, onUnavailable: (String) -> Unit, onLock: () -> Unit, onShare: () -> Unit,
    onProperties: () -> Unit, onAddToPlaylist: () -> Unit, onRename: () -> Unit, onDelete: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(f.title, Modifier.padding(horizontal = 20.dp, vertical = 8.dp), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
        val item: @Composable (DrawableResource, String, Boolean, () -> Unit) -> Unit = { icon, label, enabled, action ->
            val tint = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            Row(Modifier.fillMaxWidth().height(48.dp).clickable(enabled = enabled) { onDismiss(); action() }.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(icon), null, Modifier.size(22.dp), tint = tint)
                Spacer(Modifier.width(20.dp))
                Text(label, fontSize = 15.sp, color = tint)
            }
        }
        item(Res.drawable.ic_fm_transfer, "File Transfer", true) { onUnavailable("File Transfer") }
        item(Res.drawable.ic_fm_lock, "Lock in Private Folder", true, onLock)
        item(Res.drawable.ic_fm_mp3, "Convert to MP3", true) { onUnavailable("Convert to MP3") }
        item(Res.drawable.ic_fm_share, "Share", true, onShare)
        item(Res.drawable.ic_fm_rename, "Rename", edits, onRename)
        item(Res.drawable.ic_fm_cut, "Cut", true) { onUnavailable("Cut") }
        item(Res.drawable.ic_fm_subsearch, "Search Subtitle", true) { onUnavailable("Online subtitle search") }
        item(Res.drawable.ic_fm_info, "Properties", true, onProperties)
        item(Res.drawable.ic_fm_playlist_add, "Add To Playlist", true, onAddToPlaylist)
        item(Res.drawable.ic_fm_delete, "Delete", edits, onDelete)
        Spacer(Modifier.navigationBarsPadding().height(12.dp))
    }
}

@Composable
fun PropertiesDialog(files: List<MediaFile>, onDismiss: () -> Unit) {
    PlayerDialog(
        onDismissRequest = onDismiss,
        title = { Text("Properties") },
        text = {
            Column {
                if (files.size == 1) {
                    val f = files[0]
                    listOf(
                        "File" to f.name, "Location" to f.folderPath, "Size" to "${formatSize(f.sizeBytes)} (${groupThousands(f.sizeBytes)} bytes)",
                        "Date" to PlatformMedia.formatDateLocalized(f.dateModifiedMs, "medium", "medium"), "Length" to formatDuration(f.durationMs),
                        "Resolution" to if (f.width > 0) "${f.width} x ${f.height}" else "—", "Format" to f.mimeType,
                    ).forEach { (k, v) ->
                        Row(Modifier.padding(vertical = 3.dp)) {
                            Text(k, Modifier.width(90.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(v)
                        }
                    }
                } else {
                    Text("${files.size} files")
                    Text("Total size: ${formatSize(files.sumOf { it.sizeBytes })}")
                    Text("Total length: ${formatDuration(files.sumOf { it.durationMs })}")
                }
            }
        },
        confirmButton = { TextButton(onDismiss) { Text("OK") } },
    )
}

/** Picks one of [playlists] or creates a new one. [onDismiss] runs after either. */
@Composable
fun AddToPlaylistDialog(playlists: List<Playlist>, onAdd: (Long) -> Unit, onCreate: (String) -> Unit, onDismiss: () -> Unit) {
    var creating by remember { mutableStateOf(false) }
    PlayerDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add To Playlist") },
        text = {
            Column {
                Text("+ Create New Playlist", Modifier.fillMaxWidth().clickable { creating = true }.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.primary)
                playlists.forEach { p ->
                    Text("${p.name} (${p.uris.size})", Modifier.fillMaxWidth().clickable { onAdd(p.id); onDismiss() }.padding(vertical = 12.dp))
                }
            }
        },
        confirmButton = { TextButton(onDismiss) { Text("Cancel") } },
    )
    if (creating) TextInputDialog("Create New Playlist", "", onDismiss = { creating = false }, label = "Playlist name") { name ->
        if (name.isNotBlank()) onCreate(name)
        creating = false; onDismiss()
    }
}
