package org.app.maxplayerpro.gallery.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.app.kmediaplayer.model.MediaFile
import org.app.kmediaplayer.ui.common.PlayerDialog
import org.app.maxplayerpro.appcore.component.PhotoRef
import org.app.maxplayerpro.appcore.component.PhotoViewer
import org.app.maxplayerpro.appcore.component.PullRefresh
import org.app.maxplayerpro.appcore.component.TopBar
import org.app.maxplayerpro.gallery.ui.GalleryContract.Album
import org.app.maxplayerpro.gallery.ui.GalleryContract.Event
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun GalleryScreen(onBack: () -> Unit, viewModel: GalleryViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsState()
    GalleryContent(state, onBack, viewModel::setEvent)
}

/** Albums, then an album's photos, then a photo full screen. Long-press picks several to share or delete. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun GalleryContent(state: GalleryContract.State, onBack: () -> Unit, onEvent: (Event) -> Unit) {
    var albumPath by rememberSaveable { mutableStateOf<String?>(null) }
    var viewing by remember { mutableStateOf<Int?>(null) }
    var picked by remember { mutableStateOf(emptySet<String>()) }
    var confirmDelete by remember { mutableStateOf<List<MediaFile>?>(null) }
    val album = state.albums.firstOrNull { it.path == albumPath }
    BackHandler(enabled = picked.isNotEmpty() || album != null) { if (picked.isNotEmpty()) picked = emptySet() else albumPath = null }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopBar(album?.name ?: "Gallery", onBack = { if (album != null) { albumPath = null; picked = emptySet() } else onBack() }) {
            IconButton({ onEvent(Event.Refresh) }) { Icon(Icons.Default.Refresh, "Refresh") }
        }
        PullRefresh(state.loading, { onEvent(Event.Refresh) }, Modifier.fillMaxWidth().weight(1f)) {
            when {
                state.albums.isEmpty() && state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                state.albums.isEmpty() -> EmptyGallery()
                album == null -> AlbumGrid(state.albums, picked, onOpen = { a -> if (picked.isEmpty()) albumPath = a.path else picked = picked.toggle(a.path) },
                    onLongPress = { a -> picked = picked.toggle(a.path) })
                else -> PhotoGrid(album.photos, picked, onOpen = { i -> if (picked.isEmpty()) viewing = i else picked = picked.toggle(album.photos[i].uri) },
                    onLongPress = { i -> picked = picked.toggle(album.photos[i].uri) })
            }
        }
        if (picked.isNotEmpty()) {
            // Albums are picked by path and shared as folders; photos by URI and shared together.
            val pickedAlbums = if (album == null) state.albums.filter { it.path in picked } else emptyList()
            val pickedPhotos = album?.photos?.filter { it.uri in picked } ?: pickedAlbums.flatMap { it.photos }
            SelectionBar(
                count = if (album == null) pickedAlbums.size else pickedPhotos.size, noun = if (album == null) "album" else "photo",
                onCancel = { picked = emptySet() },
                onSelectAll = { picked = if (album == null) state.albums.map { it.path }.toSet() else album.photos.map { it.uri }.toSet() },
                onShare = {
                    if (album == null && pickedAlbums.size == 1) onEvent(Event.Share(pickedAlbums[0].photos, pickedAlbums[0].path))
                    else if (album == null) pickedAlbums.forEach { onEvent(Event.Share(it.photos, it.path)) }
                    else onEvent(Event.Share(pickedPhotos))
                    picked = emptySet()
                },
                onDelete = { confirmDelete = pickedPhotos },
            )
        }
    }
    viewing?.let { start ->
        val photos = album?.photos.orEmpty()
        PhotoViewer(photos.map { PhotoRef(it.uri, it.name) }, start, onClose = { viewing = null }) { i ->
            IconButton({ photos.getOrNull(i)?.let { onEvent(Event.Share(listOf(it))) } }) { Icon(Icons.Default.Sensors, "Share on network", tint = Color.White) }
        }
    }
    confirmDelete?.let { list ->
        PlayerDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Delete ${list.size} ${if (list.size == 1) "photo" else "photos"}?") },
            text = { Text("They will be removed from this device.") },
            confirmButton = { TextButton({ onEvent(Event.Delete(list)); confirmDelete = null; picked = emptySet() }) { Text("DELETE", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton({ confirmDelete = null }) { Text("CANCEL") } },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlbumGrid(albums: List<Album>, picked: Set<String>, onOpen: (Album) -> Unit, onLongPress: (Album) -> Unit) {
    LazyVerticalGrid(GridCells.Adaptive(160.dp), Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        items(albums, key = { it.path }) { a ->
            Column(Modifier.combinedClickable(onClick = { onOpen(a) }, onLongClick = { onLongPress(a) })) {
                Box {
                    AsyncImage(a.photos.first().uri, null, Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant), contentScale = ContentScale.Crop)
                    if (picked.isNotEmpty()) PickMark(a.path in picked, Modifier.align(Alignment.TopStart).padding(8.dp))
                    if (a.path in picked) Box(Modifier.matchParentSize().clip(RoundedCornerShape(14.dp)).border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp)))
                }
                Text(a.name.replaceFirstChar { it.uppercaseChar() }, Modifier.padding(top = 8.dp), fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${a.photos.size} ${if (a.photos.size == 1) "photo" else "photos"}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item { Spacer(Modifier.navigationBarsPadding().height(16.dp)) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoGrid(photos: List<MediaFile>, picked: Set<String>, onOpen: (Int) -> Unit, onLongPress: (Int) -> Unit) {
    LazyVerticalGrid(GridCells.Adaptive(108.dp), Modifier.fillMaxSize(), contentPadding = PaddingValues(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        items(photos.size, key = { photos[it].uri }) { i ->
            val p = photos[i]
            Box(Modifier.aspectRatio(1f).combinedClickable(onClick = { onOpen(i) }, onLongClick = { onLongPress(i) })) {
                AsyncImage(p.uri, p.name, Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant), contentScale = ContentScale.Crop)
                if (p.uri in picked) Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)))
                if (picked.isNotEmpty()) PickMark(p.uri in picked, Modifier.align(Alignment.TopEnd).padding(6.dp))
            }
        }
    }
}

@Composable
private fun SelectionBar(count: Int, noun: String, onCancel: () -> Unit, onSelectAll: () -> Unit, onShare: () -> Unit, onDelete: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).navigationBarsPadding().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onCancel) { Icon(Icons.Default.Close, "Cancel") }
        Text("$count ${if (count == 1) noun else noun + "s"} selected", Modifier.weight(1f), fontWeight = FontWeight.Medium)
        IconButton(onSelectAll) { Icon(Icons.Default.DoneAll, "Select all") }
        IconButton(onShare) { Icon(Icons.Default.Sensors, "Share on network") }
        IconButton(onDelete) { Icon(Icons.Default.Delete, "Delete") }
    }
}

@Composable
private fun PickMark(picked: Boolean, modifier: Modifier) {
    Icon(if (picked) Icons.Default.CheckCircle else Icons.Outlined.Circle, if (picked) "Picked" else "Not picked",
        modifier.size(24.dp).background(Color(0x66000000), CircleShape), tint = if (picked) MaterialTheme.colorScheme.primary else Color.White)
}

@Composable
private fun EmptyGallery() {
    Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Outlined.PhotoLibrary, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Text("No photos found", fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Text("Photos on this device show up here, by folder.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun Set<String>.toggle(key: String) = if (key in this) this - key else this + key
