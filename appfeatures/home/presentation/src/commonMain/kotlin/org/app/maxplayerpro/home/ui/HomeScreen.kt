package org.app.maxplayerpro.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Refresh
import org.app.maxplayerpro.appcore.component.LibrarySourcesBar
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import org.app.maxplayerpro.sharedlogic.data.FolderImport
import org.koin.compose.koinInject
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.app.maxplayerpro.appcore.component.EmptyState
import org.app.maxplayerpro.appcore.component.FileList
import org.app.maxplayerpro.appcore.component.TopBar
import org.app.maxplayerpro.appcore.component.ViewMenuDialog
import org.app.maxplayerpro.appres.resources.Res
import org.app.maxplayerpro.appres.resources.ic_account
import org.app.maxplayerpro.appres.resources.ic_search
import org.app.maxplayerpro.appres.resources.ic_view_menu
import org.app.maxplayerpro.appcore.adaptive.LocalWindowClass
import org.app.maxplayerpro.home.ui.component.FolderList
import org.app.maxplayerpro.home.ui.component.HomeWide
import org.app.maxplayerpro.home.ui.component.ShortcutChips
import org.app.maxplayerpro.sharedlogic.model.MediaFolder
import org.app.maxplayerpro.sharedlogic.model.ViewMode
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

/** Shortcut chip destinations and top-bar actions of the home screen. */
class HomeNavigation(
    val onSearch: () -> Unit,
    val onMe: () -> Unit,
    val onFolder: (String) -> Unit,
    val onMusic: () -> Unit,
    val onPrivacy: () -> Unit,
    val onFileTransfer: () -> Unit,
    val onPlaylists: () -> Unit,
)

@Composable
fun HomeScreen(navigation: HomeNavigation, viewModel: HomeViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeContent(state, navigation, onEvent = viewModel::setEvent)
}

@Composable
fun HomeContent(state: HomeContract.State, navigation: HomeNavigation, onEvent: (HomeContract.Event) -> Unit, folderImport: FolderImport = koinInject()) {
    var showMenu by rememberSaveable { mutableStateOf(false) }
    val opts = state.options
    if (LocalWindowClass.current.isWide) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            HomeWide(state, navigation, folderImport, onEvent, onViewMenu = { showMenu = true }) { LibraryEmptyState(state.loading, folderImport) }
        }
        if (showMenu) ViewMenuDialog(opts, onDone = { onEvent(HomeContract.Event.SaveViewOptions(it)) }) { showMenu = false }
        return
    }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize()) {
            TopBar(if (opts.viewMode == ViewMode.Files) "Videos" else "Folders") {
                IconButton({ onEvent(HomeContract.Event.Refresh) }) { Icon(Icons.Outlined.Refresh, "Refresh") }
                if (folderImport.available) IconButton(folderImport::pickFolder) { Icon(Icons.Outlined.CreateNewFolder, "Open folder") }
                IconButton(navigation.onSearch) { Icon(painterResource(Res.drawable.ic_search), "Search") }
                IconButton({ showMenu = true }) { Icon(painterResource(Res.drawable.ic_view_menu), "View Menu") }
                IconButton(navigation.onMe) { Icon(painterResource(Res.drawable.ic_account), "Account") }
            }
            if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth().height(2.dp))
            ShortcutChips(navigation)
            LibrarySourcesBar(folderImport, Modifier.padding(bottom = 8.dp))
            when (opts.viewMode) {
                ViewMode.Files -> FileList(state.files, opts, emptyTitle = "No videos found.")
                ViewMode.Folders, ViewMode.AllFolders -> {
                    if (state.folders.isEmpty()) LibraryEmptyState(state.loading, folderImport)
                    else FolderList(state.folders, opts, showPath = opts.viewMode == ViewMode.AllFolders) { f: MediaFolder -> navigation.onFolder(f.path) }
                }
            }
        }
        if (state.lastPlayed != null) {
            FloatingActionButton(
                { onEvent(HomeContract.Event.PlayLast) },
                Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(20.dp),
                containerColor = MaterialTheme.colorScheme.primary, shape = CircleShape,
            ) { Icon(Icons.Default.PlayArrow, "Play last", Modifier.size(30.dp), tint = MaterialTheme.colorScheme.onPrimary) }
        }
    }
    if (showMenu) ViewMenuDialog(opts, onDone = { onEvent(HomeContract.Event.SaveViewOptions(it)) }) { showMenu = false }
}

/** Shown while the library is empty: scanning progress, or a way to add videos where the app cannot scan. */
@Composable
private fun LibraryEmptyState(loading: Boolean, folderImport: FolderImport) {
    val sources by folderImport.sources.collectAsState()
    when {
        loading -> EmptyState("Scanning your drives…")
        // Remembered folders waiting to be reconnected: the card above explains what to do.
        sources.isNotEmpty() -> EmptyState("Your videos will appear here once your folders are connected.")
        folderImport.available -> EmptyState("Choose a folder to add its videos.\nEvery subfolder is included.") {
            Button(folderImport::pickFolder) {
                Icon(Icons.Outlined.CreateNewFolder, null)
                Spacer(Modifier.width(8.dp))
                Text("Open folder")
            }
        }
        else -> EmptyState("No videos found.\nGrant media access to scan your device.")
    }
}
