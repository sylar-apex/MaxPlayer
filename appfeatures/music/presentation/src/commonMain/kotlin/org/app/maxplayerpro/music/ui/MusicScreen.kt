package org.app.maxplayerpro.music.ui

import org.app.maxplayerpro.appcore.component.PullRefresh
import androidx.compose.foundation.background
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import org.app.maxplayerpro.music.ui.component.FavouriteIcon
import org.app.maxplayerpro.music.ui.component.albumOrFolder
import org.app.maxplayerpro.music.ui.component.coverSeed
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import org.app.maxplayerpro.appcore.adaptive.LocalWindowClass
import org.app.maxplayerpro.appcore.adaptive.WindowClass
import org.app.maxplayerpro.music.ui.component.CreateIcon
import org.app.maxplayerpro.music.ui.component.GroupGrid
import org.app.maxplayerpro.music.ui.component.GroupItem
import org.app.maxplayerpro.music.ui.component.MusicHeader
import org.app.maxplayerpro.music.ui.component.PillTabs
import org.app.maxplayerpro.music.ui.component.orUnknown
import org.app.maxplayerpro.music.ui.component.summary
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

    val wide = LocalWindowClass.current != WindowClass.Compact
    var grid by rememberSaveable { mutableStateOf(false) }
    val favourite: (MediaFile) -> Unit = { onEvent(MusicContract.Event.ToggleFavourite(it.uri)) }
    val pad = if (wide) 24.dp else 16.dp

    val g = group
    if (g != null) {
        BackHandler { group = null }
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
            MusicHeader(g.first, g.second, !wide, { play(g.second, 0, false) }, { play(g.second, 0, true) },
                Modifier.padding(start = 4.dp, end = pad, top = 8.dp, bottom = 12.dp),
                leading = { IconButton({ group = null }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                middle = { Spacer(Modifier.weight(1f)) })
            Tracks(g.second, state.favourites, Modifier.weight(1f), wide, onAdd = { addTo = it }, onToggleFavourite = favourite) { play(g.second, it, false) }
            MiniPlayer(addToPlaylist)
        }
        addTo?.let { f -> addToPlaylist(f) { addTo = null } }
        return
    }

    val audio = state.audio
    val filtered = sortFiles(audio.filter { query.isBlank() || it.title.contains(query, true) || it.artist.contains(query, true) || it.album.contains(query, true) }, sortKey, true)
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
        MusicHeader("Music", if (state.scanning) emptyList() else audio, !wide, { play(filtered, 0, false) }, { play(filtered, 0, true) },
            Modifier.padding(start = if (wide) pad else 4.dp, end = 8.dp, top = 8.dp),
            leading = { if (!wide) IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            middle = {
                if (wide) { SearchField(query, { query = it }, Modifier.width(400.dp)); Spacer(Modifier.weight(1f)) }
                else Spacer(Modifier.weight(1f))
            },
            trailing = { IconButton({ onEvent(MusicContract.Event.Refresh) }) { Icon(Icons.Outlined.Refresh, "Refresh") } })
        if (!wide) SearchField(query, { query = it }, Modifier.fillMaxWidth().padding(horizontal = pad).padding(top = 4.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = pad, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            PillTabs(tabs, tab, { tab = it }, Modifier.weight(1f))
            if (tab == 0) {
                SortMenu(sortKey, compact = !wide) { sortKey = it }
                if (wide) {
                    Spacer(Modifier.width(8.dp))
                    ViewToggle(grid) { grid = it }
                }
            }
        }
        PullRefresh(state.scanning, { onEvent(MusicContract.Event.Refresh) }, Modifier.weight(1f)) {
        val content = Modifier.fillMaxSize()
        if (state.scanning) ScanningState(content, wide, cards = tab != 0 || (grid && wide)) else when (tab) {
            0 -> if (grid && wide) GroupGrid(filtered.mapIndexed { i, f -> GroupItem(f.uri, f.title, f.artist.orUnknown("artist"), f.coverSeed) { play(filtered, i, false) } }, content)
            else Tracks(filtered, state.favourites, content, wide, onAdd = { addTo = it }, onToggleFavourite = favourite) { play(filtered, it, false) }
            1 -> {
                val recent = audio.filter { (state.history[it.uri]?.lastPlayedMs ?: 0) > 0 }.sortedByDescending { state.history[it.uri]!!.lastPlayedMs }
                val favs = audio.filter { it.uri in state.favourites }
                val items = listOf(
                    GroupItem("+new", "New playlist", "Create", "", CreateIcon) { creating = true },
                    GroupItem("+fav", "Favourites", summary(favs), "", FavouriteIcon) { group = "Favourites" to favs },
                    GroupItem("+recent", "Recently Played", summary(recent), "Recently Played") { group = "Recently Played" to recent },
                ) + state.playlists.map { p ->
                    val files = p.uris.mapNotNull { u -> audio.firstOrNull { it.uri == u } }
                    GroupItem(p.id.toString(), p.name, summary(files), p.name) { group = p.name to files }
                }
                GroupGrid(items, content)
            }
            else -> {
                val (what, key) = when (tab) {
                    2 -> "album" to { f: MediaFile -> f.albumOrFolder }
                    3 -> "artist" to { f: MediaFile -> f.artist }
                    else -> "folder" to { f: MediaFile -> f.folderName }
                }
                val items = audio.groupBy { key(it).orUnknown(what) }.entries.sortedBy { it.key.lowercase() }
                    .map { (name, files) -> GroupItem(name, name, summary(files), name) { group = name to files } }
                GroupGrid(items, content)
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

@Composable
private fun SearchField(query: String, onChange: (String) -> Unit, modifier: Modifier) {
    Row(
        modifier.height(44.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), CircleShape).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Search, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) Text("Search songs, artists, albums…", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            BasicTextField(query, onChange, singleLine = true,
                textStyle = TextStyle(fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary), modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun SortMenu(key: SortKey, compact: Boolean, onChange: (SortKey) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        // Phones: an icon only, so the tabs keep the row.
        if (compact) IconButton({ open = true }) { Icon(Icons.AutoMirrored.Filled.Sort, "Sort: ${key.label}") }
        else Row(
            Modifier.height(38.dp).clip(CircleShape).clickable { open = true }.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Sort: ${key.label}", fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Icon(Icons.Default.KeyboardArrowDown, null, Modifier.size(18.dp))
        }
        DropdownMenu(open, { open = false }) {
            listOf(SortKey.Title, SortKey.Date, SortKey.Length, SortKey.Size).forEach { k ->
                DropdownMenuItem({ Text(k.label, fontWeight = if (k == key) FontWeight.SemiBold else null) }, { open = false; onChange(k) })
            }
        }
    }
}

/** List / grid switch. */
@Composable
private fun ViewToggle(grid: Boolean, onChange: (Boolean) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.clip(RoundedCornerShape(10.dp)).background(colors.onSurface.copy(alpha = 0.035f)).padding(3.dp)) {
        listOf(false to Icons.AutoMirrored.Filled.ViewList, true to Icons.Default.GridView).forEach { (g, icon) ->
            val on = g == grid
            Box(
                Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(if (on) colors.primary.copy(alpha = 0.15f) else Color.Transparent).clickable { onChange(g) },
                contentAlignment = Alignment.Center,
            ) { Icon(icon, if (g) "Grid" else "List", Modifier.size(20.dp), tint = if (on) colors.primary else colors.onSurfaceVariant) }
        }
    }
}

/** Skeleton of the list (or card grid) with a moving shimmer while the first scan runs. */
@Composable
private fun ScanningState(modifier: Modifier, wide: Boolean, cards: Boolean) {
    val base = MaterialTheme.colorScheme.onSurface
    val t = rememberInfiniteTransition(label = "shimmer")
    val x by t.animateFloat(-600f, 2400f, infiniteRepeatable(tween(1300, easing = LinearEasing)), label = "x")
    val brush = Brush.linearGradient(
        listOf(base.copy(alpha = 0.05f), base.copy(alpha = 0.12f), base.copy(alpha = 0.05f)),
        start = Offset(x, 0f), end = Offset(x + 600f, 0f),
    )
    @Composable
    fun Bone(m: Modifier, corner: Int = 6) = Box(m.clip(RoundedCornerShape(corner.dp)).background(brush))

    if (cards) {
        Column(modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            repeat(2) {
                Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    repeat(if (wide) 6 else 2) {
                        Column(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(base.copy(alpha = 0.03f)).padding(12.dp)) {
                            Bone(Modifier.fillMaxWidth().aspectRatio(1f), 12)
                            Spacer(Modifier.height(12.dp))
                            Bone(Modifier.fillMaxWidth(0.8f).height(14.dp))
                            Spacer(Modifier.height(6.dp))
                            Bone(Modifier.fillMaxWidth(0.5f).height(10.dp))
                        }
                    }
                }
            }
        }
        return
    }
    Column(
        modifier.fillMaxWidth().padding(horizontal = if (wide) 24.dp else 0.dp)
            .clip(RoundedCornerShape(16.dp)).background(if (wide) base.copy(alpha = 0.035f) else Color.Transparent).padding(vertical = 8.dp),
    ) {
        repeat(12) { i ->
            Row(Modifier.fillMaxWidth().height(62.dp).padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                if (wide) { Bone(Modifier.size(14.dp, 12.dp)); Spacer(Modifier.width(34.dp)) }
                Bone(Modifier.size(44.dp), 8)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(3f)) {
                    Bone(Modifier.fillMaxWidth(listOf(0.7f, 0.5f, 0.6f, 0.45f)[i % 4]).height(13.dp))
                    Spacer(Modifier.height(7.dp))
                    Bone(Modifier.fillMaxWidth(0.3f).height(10.dp))
                }
                if (wide) {
                    Box(Modifier.weight(1.4f)) { Bone(Modifier.fillMaxWidth(0.5f).height(11.dp)) }
                    Box(Modifier.weight(1.4f)) { Bone(Modifier.fillMaxWidth(0.55f).height(11.dp)) }
                    Bone(Modifier.size(34.dp, 11.dp))
                    Spacer(Modifier.width(40.dp))
                }
            }
        }
    }
}
