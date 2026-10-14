package org.app.maxplayerpro.music.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.app.kmediaplayer.model.MediaFile
import org.app.kmediaplayer.ui.music.AddToPlaylistSlot
import org.app.kmediaplayer.ui.music.MiniPlayer
import org.app.kmediaplayer.ui.settings.TextInputDialog
import org.app.maxplayerpro.appcore.component.AddToPlaylistDialog
import org.app.maxplayerpro.appcore.component.EmptyState
import org.app.maxplayerpro.appcore.component.TopBar
import org.app.maxplayerpro.music.ui.component.GroupRow
import org.app.maxplayerpro.music.ui.component.Tracks
import org.app.maxplayerpro.sharedlogic.model.SortKey
import org.app.maxplayerpro.sharedlogic.model.sortFiles
import org.koin.compose.viewmodel.koinViewModel

private val tabs = listOf("Tracks", "Playlists", "Albums", "Artists", "Folders")

@Composable
fun MusicScreen(onBack: () -> Unit, viewModel: MusicViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    MusicContent(state, onBack, onEvent = viewModel::setEvent)
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MusicContent(state: MusicContract.State, onBack: () -> Unit, onEvent: (MusicContract.Event) -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    var sortKey by rememberSaveable { mutableStateOf(SortKey.Title) }
    var group by remember { mutableStateOf<Pair<String, List<MediaFile>>?>(null) }
    var addTo by remember { mutableStateOf<MediaFile?>(null) }
    var creating by remember { mutableStateOf(false) }
    val play = { files: List<MediaFile>, index: Int, shuffle: Boolean -> onEvent(MusicContract.Event.Play(files, index, shuffle)) }
    val addToPlaylist: AddToPlaylistSlot = { file, dismiss ->
        AddToPlaylistDialog(
            state.playlists,
            onAdd = { id -> onEvent(MusicContract.Event.AddToPlaylist(id, listOf(file))) },
            onCreate = { name -> onEvent(MusicContract.Event.CreatePlaylistWith(name, listOf(file))) },
            onDismiss = dismiss,
        )
    }

    val g = group
    if (g != null) {
        BackHandler { group = null }
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            TopBar(g.first, onBack = { group = null }) { IconButton({ play(g.second, 0, true) }) { Icon(Icons.Default.Shuffle, "Shuffle") } }
            Tracks(g.second, state.favourites, Modifier.weight(1f), onAdd = { addTo = it }, onToggleFavourite = { onEvent(MusicContract.Event.ToggleFavourite(it.uri)) }) { play(g.second, it, false) }
            MiniPlayer(addToPlaylist)
        }
        addTo?.let { f -> addToPlaylist(f) { addTo = null } }
        return
    }

    val audio = state.audio
    val filtered = sortFiles(audio.filter { query.isBlank() || it.title.contains(query, true) || it.artist.contains(query, true) }, sortKey, true)
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        var menu by remember { mutableStateOf(false) }
        TopBar("MUSIC", onBack = onBack) {
            Box {
                IconButton({ menu = true }) { Icon(Icons.Default.MoreVert, "More") }
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem({ Text("Refresh") }, { menu = false; onEvent(MusicContract.Event.Refresh) })
                }
            }
        }
        TabRow(tab, containerColor = MaterialTheme.colorScheme.background) {
            tabs.forEachIndexed { i, t -> Tab(i == tab, { tab = i }, text = { Text(t, fontSize = 13.sp, maxLines = 1) }, unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        when (tab) {
            0 -> {
                Row(Modifier.padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.weight(1f).height(36.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp)).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Search, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(6.dp))
                        Box(Modifier.weight(1f)) {
                            if (query.isEmpty()) Text("Search Songs...", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            BasicTextField(query, { query = it }, singleLine = true,
                                textStyle = TextStyle(fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary), modifier = Modifier.fillMaxWidth())
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Row(Modifier.height(36.dp).border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp)).clickable { play(filtered, 0, true) }.padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shuffle, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Shuffle All", fontSize = 13.sp)
                    }
                    IconButton({ sortKey = listOf(SortKey.Title, SortKey.Date, SortKey.Length, SortKey.Size).let { it[(it.indexOf(sortKey) + 1) % it.size] } }) {
                        Icon(Icons.AutoMirrored.Filled.Sort, "Sort: ${sortKey.label}")
                    }
                }
                Tracks(filtered, state.favourites, Modifier.weight(1f), onAdd = { addTo = it }, onToggleFavourite = { onEvent(MusicContract.Event.ToggleFavourite(it.uri)) }) { play(filtered, it, false) }
            }
            1 -> LazyColumn(Modifier.weight(1f)) {
                item {
                    GroupRow("+ Create New Playlist", "") { creating = true }
                    GroupRow("Favourites", "${state.favourites.size} songs") { group = "Favourites" to audio.filter { it.uri in state.favourites } }
                    val recent = audio.filter { (state.history[it.uri]?.lastPlayedMs ?: 0) > 0 }.sortedByDescending { state.history[it.uri]!!.lastPlayedMs }
                    GroupRow("Recently Played", "${recent.size} songs") { group = "Recently Played" to recent }
                }
                items(state.playlists, key = { it.id }) { p ->
                    val files = p.uris.mapNotNull { u -> audio.firstOrNull { it.uri == u } }
                    GroupRow(p.name, "${files.size} songs") { group = p.name to files }
                }
            }
            else -> {
                val groups = when (tab) {
                    2 -> audio.groupBy { it.album }
                    3 -> audio.groupBy { it.artist }
                    else -> audio.groupBy { it.folderName }
                }.entries.sortedBy { it.key.lowercase() }
                if (groups.isEmpty()) EmptyState("No music found.", Modifier.weight(1f))
                LazyColumn(Modifier.weight(1f)) {
                    items(groups, key = { it.key }) { (name, files) -> GroupRow(name, "${files.size} songs") { group = name to files } }
                }
            }
        }
        MiniPlayer(addToPlaylist)
    }
    addTo?.let { f -> addToPlaylist(f) { addTo = null } }
    if (creating) TextInputDialog("Create New Playlist", "", onDismiss = { creating = false }, label = "Playlist name") { name ->
        if (name.isNotBlank()) onEvent(MusicContract.Event.CreatePlaylist(name))
        creating = false
    }
}
