package org.app.nxplayer.ui.music

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.height
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.MoreVert
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import kotlinx.coroutines.delay
import org.app.nxplayer.core.MediaFile
import org.app.nxplayer.core.SortKey
import org.app.nxplayer.core.formatDuration
import org.app.nxplayer.core.shuffledOrder
import org.app.nxplayer.core.sortFiles
import org.app.nxplayer.data.AppGraph
import org.app.nxplayer.playback.PlayerHolder
import org.app.nxplayer.ui.EmptyState
import org.app.nxplayer.ui.LocalNav
import org.app.nxplayer.ui.Screen
import org.app.nxplayer.ui.TextInputDialog
import org.app.nxplayer.ui.TopBar
import org.app.nxplayer.ui.library.AddToPlaylistDialog

object MusicPlayer {
    fun play(context: Context, files: List<MediaFile>, index: Int, shuffle: Boolean = false) {
        if (files.isEmpty()) return
        val order = if (shuffle) shuffledOrder(files.size, index).map { files[it] } else files
        val start = if (shuffle) 0 else index
        val player = PlayerHolder.get(context)
        PlayerHolder.queue.value = order
        PlayerHolder.isMusicSession.value = true
        player.setMediaItems(order.map { PlayerHolder.mediaItem(it) }, start, 0)
        player.shuffleModeEnabled = false
        player.prepare()
        player.play()
        PlayerHolder.startBackground(context)
    }
}

private val tabs = listOf("Tracks", "Playlists", "Albums", "Artists", "Folders")

@Composable
fun MusicScreen() {
    val nav = LocalNav.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val audio by AppGraph.media.audio.collectAsState()
    val user by AppGraph.userData.data.collectAsState()
    var tab by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    var sortKey by remember { mutableStateOf(SortKey.Title) }
    var group by remember { mutableStateOf<Pair<String, List<MediaFile>>?>(null) }
    var addTo by remember { mutableStateOf<MediaFile?>(null) }
    var creating by remember { mutableStateOf(false) }

    val g = group
    if (g != null) {
        androidx.activity.compose.BackHandler { group = null }
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            TopBar(g.first, onBack = { group = null }) { IconButton({ MusicPlayer.play(context, g.second, 0, shuffle = true) }) { Icon(Icons.Default.Shuffle, "Shuffle") } }
            Tracks(g.second, user.favourites, Modifier.weight(1f), onAdd = { addTo = it }) { MusicPlayer.play(context, g.second, it) }
            MiniPlayer()
        }
        addTo?.let { AddToPlaylistDialog(listOf(it), music = true) { addTo = null } }
        return
    }

    val filtered = sortFiles(audio.filter { query.isBlank() || it.title.contains(query, true) || it.artist.contains(query, true) }, sortKey, true)
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        var menu by remember { mutableStateOf(false) }
        TopBar("MUSIC", onBack = { nav.pop() }) {
            Box {
                IconButton({ menu = true }) { Icon(Icons.Default.MoreVert, "More") }
                androidx.compose.material3.DropdownMenu(menu, { menu = false }) {
                    androidx.compose.material3.DropdownMenuItem({ Text("Refresh") }, { menu = false; scope.launch { AppGraph.media.refresh() } })
                }
            }
        }
        androidx.compose.material3.TabRow(tab, containerColor = MaterialTheme.colorScheme.background) { tabs.forEachIndexed { i, t -> Tab(i == tab, { tab = i }, text = { Text(t, fontSize = 13.sp, maxLines = 1) }, unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant) } }
        when (tab) {
            0 -> {
                Row(Modifier.padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.weight(1f).height(36.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp)).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Search, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(6.dp))
                        Box(Modifier.weight(1f)) {
                            if (query.isEmpty()) Text("Search Songs...", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            androidx.compose.foundation.text.BasicTextField(query, { query = it }, singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface),
                                cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary), modifier = Modifier.fillMaxWidth())
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Row(Modifier.height(36.dp).border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp)).clickable { MusicPlayer.play(context, filtered, 0, shuffle = true) }.padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shuffle, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Shuffle All", fontSize = 13.sp)
                    }
                    IconButton({ sortKey = listOf(SortKey.Title, SortKey.Date, SortKey.Length, SortKey.Size).let { it[(it.indexOf(sortKey) + 1) % it.size] } }) {
                        Icon(Icons.AutoMirrored.Filled.Sort, "Sort: ${sortKey.label}")
                    }
                }
                Tracks(filtered, user.favourites, Modifier.weight(1f), onAdd = { addTo = it }) { MusicPlayer.play(context, filtered, it) }
            }
            1 -> LazyColumn(Modifier.weight(1f)) {
                item {
                    GroupRow("+ Create New Playlist", "") { creating = true }
                    GroupRow("Favourites", "${user.favourites.size} songs") { group = "Favourites" to audio.filter { it.uri in user.favourites } }
                    val recent = audio.filter { (user.history[it.uri]?.lastPlayedMs ?: 0) > 0 }.sortedByDescending { user.history[it.uri]!!.lastPlayedMs }
                    GroupRow("Recently Played", "${recent.size} songs") { group = "Recently Played" to recent }
                }
                items(user.playlists.filter { it.isMusic }, key = { it.id }) { p ->
                    val files = p.uris.mapNotNull { u -> audio.firstOrNull { it.uri == u } }
                    GroupRow(p.name, "${files.size} songs") { group = p.name to files }
                }
            }
            else -> {
                val groups = when (tab) {
                    2 -> audio.groupBy { it.album }
                    3 -> audio.groupBy { it.artist }
                    else -> audio.groupBy { it.folderName }
                }.toSortedMap(String.CASE_INSENSITIVE_ORDER)
                if (groups.isEmpty()) EmptyState("No music found.", Modifier.weight(1f))
                LazyColumn(Modifier.weight(1f)) {
                    items(groups.entries.toList(), key = { it.key }) { (name, files) -> GroupRow(name, "${files.size} songs") { group = name to files } }
                }
            }
        }
        MiniPlayer()
    }
    addTo?.let { AddToPlaylistDialog(listOf(it), music = true) { addTo = null } }
    if (creating) TextInputDialog("Create New Playlist", "", onDismiss = { creating = false }, label = "Playlist name") { name ->
        if (name.isNotBlank()) AppGraph.userData.update { it.createPlaylist(name, true, System.currentTimeMillis()) }
        creating = false
    }
}

@Composable
private fun GroupRow(title: String, subtitle: String, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 14.dp)) {
        Text(title, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (subtitle.isNotEmpty()) Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Tracks(files: List<MediaFile>, favourites: Set<String>, modifier: Modifier, onAdd: (MediaFile) -> Unit, onPlay: (Int) -> Unit) {
    if (files.isEmpty()) { EmptyState("No music found.", modifier); return }
    LazyColumn(modifier) {
        items(files, key = { it.uri }) { f ->
            var menu by remember { mutableStateOf(false) }
            Row(Modifier.fillMaxWidth().height(56.dp).clickable { onPlay(files.indexOf(f)) }.padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).background(Color(0xFFE4DCFA), RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.MusicNote, null, Modifier.size(22.dp), tint = Color(0xFF9C7BEA))
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(f.title, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(f.artist, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                Box {
                    IconButton({ menu = true }) { Icon(Icons.Default.MoreVert, "More", Modifier.size(20.dp)) }
                    androidx.compose.material3.DropdownMenu(menu, { menu = false }) {
                        androidx.compose.material3.DropdownMenuItem({ Text(if (f.uri in favourites) "Remove from Favourites" else "Add to Favourites") }, { menu = false; AppGraph.userData.update { it.toggleFavourite(f.uri) } })
                        androidx.compose.material3.DropdownMenuItem({ Text("Add to Playlist") }, { menu = false; onAdd(f) })
                    }
                }
            }
        }
    }
}

@Composable
fun MiniPlayer() {
    val context = LocalContext.current
    val isMusic by PlayerHolder.isMusicSession.collectAsState()
    val player = PlayerHolder.current
    if (!isMusic || player == null) return
    var title by remember { mutableStateOf(player.mediaMetadata.title?.toString() ?: "") }
    var playing by remember { mutableStateOf(player.isPlaying) }
    var progress by remember { mutableFloatStateOf(0f) }
    DisposableEffect(player) {
        val l = object : Player.Listener {
            override fun onEvents(p: Player, events: Player.Events) { playing = p.playWhenReady && p.playbackState != Player.STATE_ENDED }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) { title = mediaItem?.mediaMetadata?.title?.toString() ?: "" }
        }
        player.addListener(l)
        onDispose { player.removeListener(l) }
    }
    LaunchedEffect(player) {
        var tick = 0
        while (true) {
            val d = player.duration
            progress = if (d > 0) player.currentPosition.toFloat() / d else 0f
            if (player.isPlaying && ++tick % 5 == 0) PlayerHolder.queue.value.getOrNull(player.currentMediaItemIndex)?.let { f ->
                AppGraph.userData.update { it.recordPlayback(f.uri, f.folderPath, player.currentPosition, d, System.currentTimeMillis()) }
            }
            delay(1000)
        }
    }
    var open by remember { mutableStateOf(false) }
    if (open) NowPlayingScreen { open = false }
    if (player.mediaItemCount == 0) return
    Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 8.dp, vertical = 6.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(28.dp)).clip(RoundedCornerShape(28.dp)).clickable { open = true }) {
        Row(Modifier.height(52.dp).padding(start = 8.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).background(Color(0xFFE4DCFA), RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.MusicNote, null, Modifier.size(20.dp), tint = Color(0xFF9C7BEA))
            }
            Spacer(Modifier.width(10.dp))
            Text(title, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
            IconButton({
                if (player.playWhenReady && player.playbackState != Player.STATE_ENDED) player.pause() else {
                    if (player.playbackState == Player.STATE_IDLE) player.prepare()
                    if (player.playbackState == Player.STATE_ENDED) player.seekTo(player.currentMediaItemIndex, 0)
                    player.play(); PlayerHolder.startBackground(context)
                }
            }) {
                Box(Modifier.size(32.dp).border(1.5.dp, MaterialTheme.colorScheme.onSurface, androidx.compose.foundation.shape.CircleShape), contentAlignment = Alignment.Center) {
                    Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, "Play/Pause", Modifier.size(20.dp))
                }
            }
            IconButton({ player.seekToNextMediaItem() }) { Icon(Icons.Default.SkipNext, "Next") }
        }
    }
}
