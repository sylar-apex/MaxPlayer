package org.app.nxplayer.ui.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import org.app.nxplayer.R
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SpaceDashboard
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.SendToMobile
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.ViewQuilt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import org.app.nxplayer.core.Keys
import org.app.nxplayer.core.ListField
import org.app.nxplayer.core.ListLayout
import org.app.nxplayer.core.ListViewOptions
import org.app.nxplayer.core.MediaFile
import org.app.nxplayer.core.MediaFolder
import org.app.nxplayer.core.SortKey
import org.app.nxplayer.core.ViewMode
import org.app.nxplayer.core.buildFolders
import org.app.nxplayer.core.filterVisible
import org.app.nxplayer.core.formatDuration
import org.app.nxplayer.core.formatSize
import org.app.nxplayer.core.isNew
import org.app.nxplayer.core.sortFiles
import org.app.nxplayer.core.sortFolders
import org.app.nxplayer.data.AppGraph
import org.app.nxplayer.ui.EmptyState
import org.app.nxplayer.ui.LocalNav
import org.app.nxplayer.ui.LocalOps
import org.app.nxplayer.ui.Screen
import org.app.nxplayer.ui.Tag
import org.app.nxplayer.ui.TextInputDialog
import org.app.nxplayer.ui.Thumbnail
import org.app.nxplayer.ui.TopBar
import org.app.nxplayer.ui.settingsVersion
import java.text.DateFormat
import java.util.Date

@Composable
fun viewOptions(): ListViewOptions {
    settingsVersion()
    return ListViewOptions.decode(AppGraph.settings[Keys.listViewOptions])
}

@Composable
fun HomeScreen() {
    val nav = LocalNav.current
    val ops = LocalOps.current
    val videos by AppGraph.media.videos.collectAsState()
    val noMedia by AppGraph.media.noMediaDirs.collectAsState()
    val loading by AppGraph.media.loading.collectAsState()
    val user by AppGraph.userData.data.collectAsState()
    val opts = viewOptions()
    var showMenu by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize()) {
            TopBar(if (opts.viewMode == ViewMode.Files) "Videos" else "Folders") {
                IconButton({ nav.push(Screen.Search) }) { Icon(painterResource(R.drawable.ic_search), "Search") }
                IconButton({ showMenu = true }) { Icon(painterResource(R.drawable.ic_view_menu), "View Menu") }
                IconButton({ nav.push(Screen.Me) }) { Icon(painterResource(R.drawable.ic_account), "Account") }
            }
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth().height(2.dp))
            ShortcutChips()
            when (opts.viewMode) {
                ViewMode.Files -> {
                    val files = sortFiles(filterVisible(videos, opts, noMedia), opts.sortKey, opts.ascending, user.history)
                    FileList(files, opts, emptyTitle = "No videos found.")
                }
                ViewMode.Folders, ViewMode.AllFolders -> {
                    val folders = sortFolders(buildFolders(videos, opts, noMedia), opts.sortKey, opts.ascending, user.history)
                    if (folders.isEmpty()) EmptyState(if (loading) "Scanning…" else "No videos found.\nGrant media access to scan your device.")
                    else FolderList(folders, opts, showPath = opts.viewMode == ViewMode.AllFolders) { nav.push(Screen.Folder(it.path)) }
                }
            }
        }
        val last = user.lastPlayedUri?.let { AppGraph.media.findByUri(it) }
        if (AppGraph.settings[Keys.floatingButton] && last != null) {
            FloatingActionButton(
                { val folder = videos.filter { it.folderPath == last.folderPath }.let { sortFiles(it, opts.sortKey, opts.ascending, user.history) }; ops.play(folder, folder.indexOf(last).coerceAtLeast(0)) },
                Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(20.dp),
                containerColor = MaterialTheme.colorScheme.primary, shape = CircleShape,
            ) { Icon(Icons.Default.PlayArrow, "Play last", Modifier.size(30.dp), tint = MaterialTheme.colorScheme.onPrimary) }
        }
    }
    if (showMenu) ViewMenuDialog(opts) { showMenu = false }
}

@Composable
private fun ShortcutChips() {
    val nav = LocalNav.current
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Chip(R.drawable.ic_chip_music, "Music") { nav.push(Screen.Music) }
        Chip(R.drawable.ic_chip_privacy, "Privacy") { nav.push(Screen.Private) }
        Chip(R.drawable.ic_chip_share, "MXShare") { nav.push(Screen.Unavailable("File Transfer", "Peer-to-peer transfer uses MX's proprietary MXShare protocol and is not implemented.")) }
        Chip(R.drawable.ic_chip_playlist, "Video Playlist") { nav.push(Screen.Playlists) }
    }
}

@Composable
private fun Chip(icon: Int, label: String, onClick: () -> Unit) {
    Row(
        Modifier.height(32.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50)).clickable(onClick = onClick).padding(start = 12.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(icon), null, Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun FolderGlyph(modifier: Modifier) {
    val color = Color(0xFF444C55)
    androidx.compose.foundation.Canvas(modifier) {
        val w = size.width; val h = size.height
        val r = h * 0.09f
        val tabTop = w * 0.33f
        val tabEnd = w * 0.49f
        val bodyY = h * 0.145f
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(0f, r)
            quadraticTo(0f, 0f, r, 0f)
            lineTo(tabTop, 0f)
            cubicTo(tabTop + (tabEnd - tabTop) * 0.45f, 0f, tabTop + (tabEnd - tabTop) * 0.55f, bodyY, tabEnd, bodyY)
            lineTo(w - r, bodyY)
            quadraticTo(w, bodyY, w, bodyY + r)
            lineTo(w, h - r)
            quadraticTo(w, h, w - r, h)
            lineTo(r, h)
            quadraticTo(0f, h, 0f, h - r)
            close()
        }
        drawPath(path, color)
    }
}

@Composable
private fun FolderList(folders: List<MediaFolder>, opts: ListViewOptions, showPath: Boolean, onOpen: (MediaFolder) -> Unit) {
    if (opts.layout == ListLayout.Grid) {
        LazyVerticalGrid(GridCells.Adaptive(150.dp), Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp)) {
            items(folders, key = { it.path }) { f ->
                Column(Modifier.clickable { onOpen(f) }.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    FolderGlyph(Modifier.size(width = 96.dp, height = 72.dp))
                    Text(f.name, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.primary)
                    Text("${f.files.size} videos", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    } else {
        LazyColumn(Modifier.fillMaxSize()) {
            items(folders, key = { it.path }) { f ->
                Row(Modifier.fillMaxWidth().height(62.dp).clickable { onOpen(f) }.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    FolderGlyph(Modifier.size(width = 62.dp, height = 46.dp))
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(f.name.replaceFirstChar { it.uppercaseChar() }, fontSize = 15.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(2.dp))
                        val extra = buildList {
                            add("${f.files.size} videos")
                            if (ListField.Size in opts.fields) add(formatSize(f.sizeBytes))
                        }.joinToString(" · ")
                        Text(extra, fontSize = 12.sp, lineHeight = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (showPath || ListField.Path in opts.fields) Text(f.path, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
fun FolderScreen(path: String) {
    val nav = LocalNav.current
    val videos by AppGraph.media.videos.collectAsState()
    val noMedia by AppGraph.media.noMediaDirs.collectAsState()
    val user by AppGraph.userData.data.collectAsState()
    val opts = viewOptions()
    var showMenu by remember { mutableStateOf(false) }
    val files = sortFiles(filterVisible(videos.filter { it.folderPath == path }, opts, noMedia), opts.sortKey, opts.ascending, user.history)
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopBar(path.substringAfterLast('/').replaceFirstChar { it.uppercaseChar() }, onBack = { nav.pop() }) {
            IconButton({ nav.push(Screen.Search) }) { Icon(painterResource(R.drawable.ic_search), "Search") }
            IconButton({ showMenu = true }) { Icon(painterResource(R.drawable.ic_view_menu), "View Menu") }
        }
        FileList(files, opts, emptyTitle = "This folder is empty.", lastPlayed = user.lastPlayedPerFolder[path])
    }
    if (showMenu) ViewMenuDialog(opts) { showMenu = false }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileList(files: List<MediaFile>, opts: ListViewOptions, emptyTitle: String, lastPlayed: String? = null, header: (@Composable () -> Unit)? = null) {
    val ops = LocalOps.current
    val user by AppGraph.userData.data.collectAsState()
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var actionsFor by remember { mutableStateOf<MediaFile?>(null) }
    var infoFor by remember { mutableStateOf<List<MediaFile>?>(null) }
    var addToPlaylist by remember { mutableStateOf<List<MediaFile>?>(null) }
    var renaming by remember { mutableStateOf<MediaFile?>(null) }
    var confirmDelete by remember { mutableStateOf<List<MediaFile>?>(null) }
    val selecting = selected.isNotEmpty()
    val now = remember { System.currentTimeMillis() }
    val newDays = AppGraph.settings[Keys.newTaggedPeriod]
    val markLast = AppGraph.settings[Keys.markLastPlayed]
    val listState = rememberLazyListState()
    val subtitleNames = remember(files) { files.map { it.folderPath }.distinct().flatMap { d -> java.io.File(d).list()?.toList().orEmpty() }.toSet() }

    LaunchedEffect(lastPlayed, files.size) {
        if (AppGraph.settings[Keys.scrollToLast] && lastPlayed != null) {
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
                IconButton({ ops.play(selFiles, 0) }) { Icon(Icons.Default.PlayArrow, "Play") }
                IconButton({ infoFor = selFiles }) { Icon(Icons.Default.Info, "Info") }
            }
        }
        header?.invoke()
        if (files.isEmpty()) EmptyState(emptyTitle, Modifier.weight(1f))
        else Box(Modifier.weight(1f)) {
            val row: @Composable (MediaFile) -> Unit = { f ->
                val tags = buildList {
                    if (subtitleNames.any { it != f.name && it.startsWith(f.title) && it.substringAfterLast('.').lowercase() in setOf("srt", "ass", "ssa", "vtt", "sub") }) add("SUB")
                    if (isNew(f, now, newDays) && user.history[f.uri] == null) add("NEW")
                }
                FileRow(
                    f, opts, highlight = markLast && f.uri == lastPlayed, tags = tags, record = user.history[f.uri],
                    selecting = selecting, isSelected = f.uri in selected,
                    onClick = { if (selecting) toggle(f) else ops.play(files, files.indexOf(f)) },
                    onThumbClick = { if (selecting || AppGraph.settings[Keys.selectThumbnail]) toggle(f) else ops.play(files, files.indexOf(f)) },
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
                IconButton({ ops.share(selFiles) }) { Icon(Icons.Default.Share, "Share") }
                IconButton({ addToPlaylist = selFiles }) { Icon(Icons.AutoMirrored.Filled.PlaylistPlay, "Add to playlist") }
                IconButton({ ops.lockInPrivate(selFiles); selected = emptySet() }) { Icon(Icons.Default.Lock, "Lock") }
                IconButton({ selFiles.singleOrNull()?.let { renaming = it } }, enabled = selected.size == 1) { Icon(Icons.Default.Edit, "Rename") }
                IconButton({ confirmDelete = selFiles }) { Icon(Icons.Default.Delete, "Delete") }
            }
        }
    }

    actionsFor?.let { f ->
        FileActionsSheet(f, onDismiss = { actionsFor = null }, onProperties = { infoFor = listOf(f) }, onAddToPlaylist = { addToPlaylist = listOf(f) },
            onRename = { renaming = f }, onDelete = { confirmDelete = listOf(f) })
    }
    infoFor?.let { PropertiesDialog(it) { infoFor = null } }
    addToPlaylist?.let { AddToPlaylistDialog(it, music = false) { addToPlaylist = null; selected = emptySet() } }
    renaming?.let { f -> TextInputDialog("Rename", f.title, onDismiss = { renaming = null }) { ops.rename(f, it); renaming = null; selected = emptySet() } }
    confirmDelete?.let { list ->
        org.app.nxplayer.player.PlayerDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Delete") },
            text = { Text(if (list.size == 1) "Delete \"${list[0].name}\"?" else "Delete ${list.size} files?") },
            confirmButton = { TextButton({ ops.delete(list); confirmDelete = null; selected = emptySet() }) { Text("DELETE") } },
            dismissButton = { TextButton({ confirmDelete = null }) { Text("CANCEL") } },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileRow(
    f: MediaFile, opts: ListViewOptions, highlight: Boolean, tags: List<String>, record: org.app.nxplayer.core.PlaybackRecord?,
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
        if (ListField.Date in opts.fields) add(DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(f.dateModifiedMs)))
        if (ListField.PlayedTime in opts.fields && record != null && record.lastPlayedMs > 0) add("Played " + DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(record.lastPlayedMs)))
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
                    tags.forEach { Tag(it, if (it == "NEW") Color(0xFFE53935) else Color(0xFF2EBD59)) }
                }
            }
        }
        if (!selecting) IconButton(onMore, Modifier.size(width = 40.dp, height = 38.dp)) { Icon(painterResource(R.drawable.ic_more_vert), "More", Modifier.size(20.dp)) } else Spacer(Modifier.width(12.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FileActionsSheet(f: MediaFile, onDismiss: () -> Unit, onProperties: () -> Unit, onAddToPlaylist: () -> Unit, onRename: () -> Unit, onDelete: () -> Unit) {
    val ops = LocalOps.current
    val nav = LocalNav.current
    val edits = AppGraph.settings[Keys.allowEdits]
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(f.title, Modifier.padding(horizontal = 20.dp, vertical = 8.dp), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
        val item: @Composable (Int, String, Boolean, () -> Unit) -> Unit = { icon, label, enabled, action ->
            val tint = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            Row(Modifier.fillMaxWidth().height(48.dp).clickable(enabled = enabled) { onDismiss(); action() }.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(icon), null, Modifier.size(22.dp), tint = tint)
                Spacer(Modifier.width(20.dp))
                Text(label, fontSize = 15.sp, color = tint)
            }
        }
        val unavailable = { what: String -> ops.toast("$what is not available in Nx Player") }
        item(R.drawable.ic_fm_transfer, "File Transfer", true) { unavailable("File Transfer") }
        item(R.drawable.ic_fm_lock, "Lock in Private Folder", true) { ops.lockInPrivate(listOf(f)) }
        item(R.drawable.ic_fm_mp3, "Convert to MP3", true) { unavailable("Convert to MP3") }
        item(R.drawable.ic_fm_share, "Share", true) { ops.share(listOf(f)) }
        item(R.drawable.ic_fm_rename, "Rename", edits) { onRename() }
        item(R.drawable.ic_fm_cut, "Cut", true) { unavailable("Cut") }
        item(R.drawable.ic_fm_subsearch, "Search Subtitle", true) { unavailable("Online subtitle search") }
        item(R.drawable.ic_fm_info, "Properties", true) { onProperties() }
        item(R.drawable.ic_fm_playlist_add, "Add To Playlist", true) { onAddToPlaylist() }
        item(R.drawable.ic_fm_delete, "Delete", edits) { onDelete() }
        Spacer(Modifier.navigationBarsPadding().height(12.dp))
    }
}

@Composable
fun PropertiesDialog(files: List<MediaFile>, onDismiss: () -> Unit) {
    org.app.nxplayer.player.PlayerDialog(
        onDismissRequest = onDismiss,
        title = { Text("Properties") },
        text = {
            Column {
                if (files.size == 1) {
                    val f = files[0]
                    listOf(
                        "File" to f.name, "Location" to f.folderPath, "Size" to "${formatSize(f.sizeBytes)} (${"%,d".format(f.sizeBytes)} bytes)",
                        "Date" to DateFormat.getDateTimeInstance().format(Date(f.dateModifiedMs)), "Length" to formatDuration(f.durationMs),
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

@Composable
fun AddToPlaylistDialog(files: List<MediaFile>, music: Boolean, onDismiss: () -> Unit) {
    val user by AppGraph.userData.data.collectAsState()
    var creating by remember { mutableStateOf(false) }
    val lists = user.playlists.filter { it.isMusic == music }
    org.app.nxplayer.player.PlayerDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add To Playlist") },
        text = {
            Column {
                Text("+ Create New Playlist", Modifier.fillMaxWidth().clickable { creating = true }.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.primary)
                lists.forEach { p ->
                    Text("${p.name} (${p.uris.size})", Modifier.fillMaxWidth().clickable {
                        AppGraph.userData.update { it.addToPlaylist(p.id, files.map { f -> f.uri }) }; onDismiss()
                    }.padding(vertical = 12.dp))
                }
            }
        },
        confirmButton = { TextButton(onDismiss) { Text("Cancel") } },
    )
    if (creating) TextInputDialog("Create New Playlist", "", onDismiss = { creating = false }, label = "Playlist name") { name ->
        if (name.isNotBlank()) {
            val id = System.currentTimeMillis()
            AppGraph.userData.update { it.createPlaylist(name, music, id).addToPlaylist(id, files.map { f -> f.uri }) }
        }
        creating = false; onDismiss()
    }
}

@Composable
fun ViewMenuDialog(current: ListViewOptions, onDismiss: () -> Unit) {
    var o by remember { mutableStateOf(current) }
    var fieldsOpen by remember { mutableStateOf(false) }
    var advancedOpen by remember { mutableStateOf(false) }
    val accent = MaterialTheme.colorScheme.primary
    androidx.compose.ui.window.Dialog(onDismiss, androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.width(299.dp).height(517.dp).background(MenuBg, RoundedCornerShape(3.dp))) {
            Column(Modifier.weight(1f).padding(top = 12.dp).verticalScroll(rememberScrollState())) {
                Row(Modifier.padding(horizontal = 12.dp)) {
                    Column(Modifier.width(167.dp)) {
                        MenuTitle("View Mode")
                        Row {
                            ViewMode.entries.forEachIndexed { i, m ->
                                MenuChoice(listOf(R.drawable.ic_vm_all, R.drawable.ic_vm_files, R.drawable.ic_vm_folders)[i], m.label, m == o.viewMode, Modifier.width(56.dp)) { o = o.copy(viewMode = m) }
                            }
                        }
                    }
                    Box(Modifier.padding(top = 54.dp).size(width = 1.dp, height = 45.dp).background(MenuDivider))
                    Column(Modifier.width(107.dp)) {
                        MenuTitle("Layout")
                        Row {
                            ListLayout.entries.forEachIndexed { i, l ->
                                MenuChoice(listOf(R.drawable.ic_lay_list, R.drawable.ic_lay_grid)[i], l.label, l == o.layout, Modifier.width(54.dp)) { o = o.copy(layout = l) }
                            }
                        }
                    }
                }
                MenuDividerLine(Modifier.padding(top = 8.dp))
                Column(Modifier.padding(horizontal = 12.dp)) {
                    MenuTitle("Sort", bottom = 12.dp)
                    val sortIcons = listOf(
                        R.drawable.ic_sort_title, R.drawable.ic_sort_date, R.drawable.ic_sort_played, R.drawable.ic_sort_status, R.drawable.ic_sort_length,
                        R.drawable.ic_sort_size, R.drawable.ic_sort_resolution, R.drawable.ic_sort_path, R.drawable.ic_sort_framerate, R.drawable.ic_sort_type,
                    )
                    SortKey.entries.chunked(5).forEachIndexed { r, row ->
                        Row(Modifier.padding(top = if (r == 0) 0.dp else 1.dp)) {
                            row.forEachIndexed { c, k -> MenuChoice(sortIcons[r * 5 + c], k.label, k == o.sortKey, Modifier.width(55.dp)) { o = o.copy(sortKey = k) } }
                        }
                    }
                    Row(Modifier.padding(start = 16.dp, top = 16.dp, bottom = 12.dp).size(width = 243.dp, height = 28.dp).border(1.dp, MenuSegBorder, RoundedCornerShape(2.dp))) {
                        SortDirection(R.drawable.ic_arrow_up, "A to Z", o.ascending, accent, Modifier.weight(1f)) { o = o.copy(ascending = true) }
                        Box(Modifier.width(1.dp).fillMaxHeight().background(MenuSegBorder))
                        SortDirection(R.drawable.ic_arrow_down, "Z to A", !o.ascending, accent, Modifier.weight(1f)) { o = o.copy(ascending = false) }
                    }
                }
                MenuDividerLine()
                MenuExpander("Fields", fieldsOpen) { fieldsOpen = !fieldsOpen }
                if (fieldsOpen) Column(Modifier.padding(start = 20.dp, end = 20.dp)) {
                    ListField.entries.chunked(3).forEach { row ->
                        Row { row.forEach { fld -> MenuCheck(fld.label, fld in o.fields, Modifier.width(86.5.dp)) { o = o.copy(fields = if (fld in o.fields) o.fields - fld else o.fields + fld) } } }
                    }
                }
                MenuDividerLine()
                MenuExpander("Advanced", advancedOpen) { advancedOpen = !advancedOpen }
                if (advancedOpen) Column(Modifier.padding(start = 20.dp, end = 20.dp)) {
                    MenuCheck("Display length over thumbnail", o.lengthOverThumbnail) { o = o.copy(lengthOverThumbnail = !o.lengthOverThumbnail) }
                    MenuCheck("Show hidden files and folders", o.showHidden) { o = o.copy(showHidden = !o.showHidden) }
                    MenuCheck("Recognize .nomedia", o.respectNoMedia) { o = o.copy(respectNoMedia = !o.respectNoMedia) }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 4.dp, end = 12.dp, bottom = 9.dp), horizontalArrangement = Arrangement.End) {
                Text("Cancel", Modifier.clickable(onClick = onDismiss).height(39.dp).padding(horizontal = 12.dp).wrapContentHeight(), fontSize = 16.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF85929C))
                Spacer(Modifier.width(4.dp))
                Text("Done", Modifier.clickable { AppGraph.settings[Keys.listViewOptions] = o.encode(); onDismiss() }.height(39.dp).padding(horizontal = 12.dp).wrapContentHeight(),
                    fontSize = 16.5.sp, fontWeight = FontWeight.SemiBold, color = accent)
            }
        }
    }
}

private val MenuBg = Color(0xFF26374C)
private val MenuDivider = Color(0xFF39495C)
private val MenuSegBorder = Color(0xFF3D5068)
private val MenuText = Color(0xFFDADDE4)

@Composable
private fun MenuTitle(text: String, bottom: androidx.compose.ui.unit.Dp = 13.dp) =
    Text(text, Modifier.padding(start = 12.dp, top = 12.dp, bottom = bottom), fontSize = 14.5.sp, lineHeight = 17.sp, fontWeight = FontWeight.SemiBold, color = MenuText)

@Composable
private fun MenuDividerLine(modifier: Modifier = Modifier) = Box(modifier.padding(horizontal = 16.dp).fillMaxWidth().height(1.dp).background(MenuDivider))

@Composable
private fun MenuChoice(icon: Int, label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val color = if (selected) MaterialTheme.colorScheme.primary else MenuText
    Column(modifier.height(60.dp).clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(painterResource(icon), label, Modifier.size(24.dp), tint = color)
        Spacer(Modifier.height(6.dp))
        Text(label, Modifier.wrapContentWidth(unbounded = true), fontSize = 11.sp, lineHeight = 13.sp, color = color, maxLines = 1, softWrap = false)
    }
}

@Composable
private fun SortDirection(icon: Int, label: String, selected: Boolean, accent: Color, modifier: Modifier, onClick: () -> Unit) {
    val color = if (selected) accent else MenuText
    Row(
        modifier.fillMaxHeight().let { if (selected) it.border(1.dp, accent, RoundedCornerShape(2.dp)).background(accent.copy(alpha = 0.08f)) else it }.clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(icon), null, Modifier.size(12.dp), tint = color)
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 10.5.sp, color = color)
    }
}

@Composable
private fun MenuExpander(title: String, open: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(44.dp).clickable(onClick = onClick).padding(start = 24.dp, end = 24.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold, color = MenuText)
        Icon(painterResource(R.drawable.ic_chevron_down), null, Modifier.size(16.dp).graphicsLayer { rotationZ = if (open) 180f else 0f }, tint = MenuText)
    }
}

@Composable
private fun MenuCheck(label: String, checked: Boolean, modifier: Modifier = Modifier, onToggle: () -> Unit) {
    Row(modifier.heightIn(min = 40.dp).clickable(onClick = onToggle), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(18.dp).background(if (checked) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(2.dp))
                .border(2.dp, if (checked) MaterialTheme.colorScheme.primary else Color(0xFFB8C0CA), RoundedCornerShape(2.dp)),
            contentAlignment = Alignment.Center,
        ) { if (checked) Icon(painterResource(R.drawable.ic_check), null, Modifier.size(14.dp), tint = Color.White) }
        Spacer(Modifier.width(8.dp))
        Text(label, fontSize = 12.sp, lineHeight = 14.sp, color = MenuText)
    }
}

@Composable
fun SearchScreen() {
    val nav = LocalNav.current
    val videos by AppGraph.media.videos.collectAsState()
    val user by AppGraph.userData.data.collectAsState()
    var query by remember { mutableStateOf("") }
    val opts = viewOptions()
    val results = if (query.isBlank()) emptyList() else sortFiles(videos.filter { it.name.contains(query, ignoreCase = true) }, opts.sortKey, opts.ascending, user.history)
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopBar("Search", onBack = { nav.pop() })
        OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().padding(horizontal = 16.dp), placeholder = { Text("Search videos") }, singleLine = true,
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { AppGraph.userData.update { it.addSearch(query) } }),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search))
        if (query.isBlank()) {
            if (user.searchHistory.isNotEmpty()) {
                Text("Recent searches", Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                user.searchHistory.forEach { q -> Text(q, Modifier.fillMaxWidth().clickable { query = q }.padding(horizontal = 16.dp, vertical = 10.dp)) }
            }
        } else FileList(results, opts, "No results for \"$query\".")
    }
}
