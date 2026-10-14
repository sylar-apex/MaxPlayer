package org.app.maxplayerpro.folder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.app.maxplayerpro.appcore.component.FileList
import org.app.maxplayerpro.appcore.component.TopBar
import org.app.maxplayerpro.appcore.component.ViewMenuDialog
import org.app.maxplayerpro.appres.resources.Res
import org.app.maxplayerpro.appres.resources.ic_search
import org.app.maxplayerpro.appres.resources.ic_view_menu
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun FolderScreen(
    path: String,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    viewModel: FolderViewModel = koinViewModel(key = path) { parametersOf(path) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    FolderContent(state, onBack, onSearch, onEvent = viewModel::setEvent)
}

@Composable
fun FolderContent(state: FolderContract.State, onBack: () -> Unit, onSearch: () -> Unit, onEvent: (FolderContract.Event) -> Unit) {
    var showMenu by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopBar(state.title, onBack = onBack) {
            IconButton(onSearch) { Icon(painterResource(Res.drawable.ic_search), "Search") }
            IconButton({ showMenu = true }) { Icon(painterResource(Res.drawable.ic_view_menu), "View Menu") }
        }
        FileList(state.files, state.options, emptyTitle = "This folder is empty.", lastPlayed = state.lastPlayed)
    }
    if (showMenu) ViewMenuDialog(state.options, onDone = { onEvent(FolderContract.Event.SaveViewOptions(it)) }) { showMenu = false }
}
