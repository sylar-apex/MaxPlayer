package org.app.maxplayerpro.playlists.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import org.app.kmediaplayer.ui.settings.TextInputDialog
import org.app.maxplayerpro.appcore.component.EmptyState
import org.app.maxplayerpro.appcore.component.Thumbnail
import org.app.maxplayerpro.appcore.component.TopBar
import org.app.maxplayerpro.appres.resources.Res
import org.app.maxplayerpro.appres.resources.ic_close
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun PlaylistsScreen(onClose: () -> Unit, onPlaylist: (Long) -> Unit, viewModel: PlaylistsViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    PlaylistsContent(state, onClose, onPlaylist, onEvent = viewModel::setEvent)
}

@Composable
fun PlaylistsContent(state: PlaylistsContract.State, onClose: () -> Unit, onPlaylist: (Long) -> Unit, onEvent: (PlaylistsContract.Event) -> Unit) {
    var creating by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopBar("VIDEO PLAYLISTS") { IconButton(onClose) { Icon(painterResource(Res.drawable.ic_close), "Close") } }
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth().height(36.dp).border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp)).clickable { creating = true },
            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text("Create New Playlist", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
        if (state.playlists.isEmpty()) EmptyState("No Playlists found.")
        LazyColumn {
            items(state.playlists, key = { it.id }) { p ->
                Row(Modifier.fillMaxWidth().clickable { onPlaylist(p.id) }.padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.PlaylistPlay, null, Modifier.size(36.dp))
                    Spacer(Modifier.width(16.dp))
                    Column { Text(p.name, fontSize = 17.sp); Text("${p.uris.size} videos", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
    }
    if (creating) TextInputDialog("Create New Playlist", "", onDismiss = { creating = false }, label = "Playlist name") { name ->
        if (name.isNotBlank()) onEvent(PlaylistsContract.Event.Create(name))
        creating = false
    }
}

@Composable
fun PlaylistDetailScreen(id: Long, onBack: () -> Unit, viewModel: PlaylistDetailViewModel = koinViewModel(key = "playlist_$id") { parametersOf(id) }) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                PlaylistDetailContract.Effect.Deleted -> onBack()
            }
        }
    }
    PlaylistDetailContent(state, onBack, onEvent = viewModel::setEvent)
}

@Composable
fun PlaylistDetailContent(state: PlaylistDetailContract.State, onBack: () -> Unit, onEvent: (PlaylistDetailContract.Event) -> Unit) {
    val p = state.playlist ?: run { EmptyState("Playlist deleted."); return }
    val files = state.files
    var renaming by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopBar(p.name, onBack = onBack) {
            IconButton({ onEvent(PlaylistDetailContract.Event.Play(0)) }) { Icon(Icons.AutoMirrored.Filled.PlaylistPlay, "Play all") }
            IconButton({ renaming = true }) { Icon(Icons.Default.Edit, "Rename") }
            IconButton({ onEvent(PlaylistDetailContract.Event.Delete) }) { Icon(Icons.Default.Delete, "Delete playlist") }
        }
        LazyColumn {
            items(files, key = { it.uri }) { f ->
                Row(Modifier.fillMaxWidth().clickable { onEvent(PlaylistDetailContract.Event.Play(files.indexOf(f))) }.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Thumbnail(f, Modifier.size(width = 110.dp, height = 62.dp), true)
                    Spacer(Modifier.width(12.dp))
                    Text(f.title, Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    IconButton({ onEvent(PlaylistDetailContract.Event.Remove(f.uri)) }) { Icon(Icons.Default.Delete, "Remove") }
                }
            }
        }
        if (files.isEmpty()) EmptyState("Add videos from a file's ⋮ menu → Add To Playlist.")
    }
    if (renaming) TextInputDialog("Rename playlist", p.name, onDismiss = { renaming = false }) { onEvent(PlaylistDetailContract.Event.Rename(it)); renaming = false }
}
