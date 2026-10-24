package org.app.maxplayerpro.mediamanager.ui

import androidx.compose.runtime.remember
import org.koin.compose.koinInject
import org.app.maxplayerpro.sharedlogic.data.FileRoot
import org.app.maxplayerpro.sharedlogic.data.FileBrowser
import org.app.maxplayerpro.appcore.component.PlacesGrid
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.app.kmediaplayer.util.formatSize
import org.app.maxplayerpro.appcore.component.FileList
import org.app.maxplayerpro.appcore.component.Thumbnail
import org.app.maxplayerpro.appcore.component.TopBar
import org.app.maxplayerpro.mediamanager.domain.model.MediaListKind
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun MediaManagerScreen(
    onBack: () -> Unit, onList: (MediaListKind) -> Unit, onOpenFolder: (String) -> Unit = {},
    viewModel: MediaManagerViewModel = koinViewModel(), files: FileBrowser = koinInject(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Finder-style places (Desktop, Downloads, drives, …): a tile opens it in Files to browse, open or share.
    val places = remember(files) { files.roots() }
    MediaManagerContent(state, onBack, onList, onEvent = viewModel::setEvent, places = places, onOpenFolder = onOpenFolder)
}

@Composable
fun MediaManagerContent(
    state: MediaManagerContract.State, onBack: () -> Unit, onList: (MediaListKind) -> Unit, onEvent: (MediaManagerContract.Event) -> Unit,
    places: List<FileRoot> = emptyList(), onOpenFolder: (String) -> Unit = {},
) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState())) {
        TopBar("Storage", onBack = onBack)
        if (places.isNotEmpty()) PlacesGrid(places, onOpen = { onOpenFolder(it.path) }, Modifier.padding(horizontal = 20.dp).padding(bottom = 12.dp))
        state.summary?.let { s ->
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
            listOf(MediaListKind.Recent, MediaListKind.Large, MediaListKind.Added).forEach { k ->
                Box(Modifier.weight(1f).padding(6.dp).height(90.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)).clickable { onList(k) }, contentAlignment = Alignment.Center) {
                    Text(k.title, textAlign = TextAlign.Center, fontWeight = FontWeight.Medium)
                }
            }
        }
        Text("Haven't Played  ›", Modifier.fillMaxWidth().clickable { onList(MediaListKind.Unplayed) }.padding(20.dp), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        FlowRow(Modifier.padding(horizontal = 14.dp), maxItemsInEachRow = 2) {
            state.unplayedPreview.forEach { f ->
                Column(Modifier.width(180.dp).padding(6.dp).clickable { onEvent(MediaManagerContract.Event.Play(f)) }) {
                    Thumbnail(f, Modifier.fillMaxWidth().height(100.dp), true)
                    Text(f.title, maxLines = 2, fontSize = 13.sp, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
fun MediaListScreen(kind: MediaListKind, onBack: () -> Unit, viewModel: MediaListViewModel = koinViewModel(key = "media_list_${kind.name}") { parametersOf(kind) }) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopBar(state.kind.title, onBack = onBack)
        FileList(state.files, state.options, "Nothing here.")
    }
}
