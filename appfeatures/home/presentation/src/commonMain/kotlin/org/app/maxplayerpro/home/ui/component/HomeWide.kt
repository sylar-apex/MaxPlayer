package org.app.maxplayerpro.home.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Refresh
import org.app.maxplayerpro.appcore.component.LibrarySourcesBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.app.maxplayerpro.appcore.adaptive.CardGrid
import org.app.maxplayerpro.appcore.adaptive.ContinueWatchingCard
import org.app.maxplayerpro.appcore.adaptive.FolderCard
import org.app.maxplayerpro.appcore.component.rememberShareMarks
import org.app.maxplayerpro.appcore.adaptive.SectionTitle
import org.app.maxplayerpro.appcore.adaptive.WideHeader
import org.app.maxplayerpro.appcore.adaptive.LocalTitleBarHost
import org.app.maxplayerpro.appcore.component.FileList
import org.app.maxplayerpro.home.ui.HomeContract
import org.app.maxplayerpro.home.ui.HomeNavigation
import org.app.maxplayerpro.sharedlogic.data.FolderImport
import org.app.maxplayerpro.sharedlogic.model.ListLayout
import org.app.maxplayerpro.sharedlogic.model.ViewMode

/**
 * Home for laptops and desktops: a large header with search, a "continue watching" banner and the
 * library as artwork cards. Navigation to Music, Playlists and the rest lives in the sidebar.
 */
@Composable
internal fun HomeWide(
    state: HomeContract.State,
    navigation: HomeNavigation,
    folderImport: FolderImport,
    onEvent: (HomeContract.Event) -> Unit,
    onViewMenu: () -> Unit,
    emptyState: @Composable () -> Unit,
) {
    val opts = state.options
    val videoCount = state.folders.sumOf { it.files.size }
    val inTitleBar = LocalTitleBarHost.current != null
    val header: @Composable (inset: androidx.compose.ui.unit.Dp) -> Unit = { inset ->
        WideHeader(
            inset = inset,
            title = if (opts.viewMode == ViewMode.Files) "All videos" else "Library",
            subtitle = when {
                state.loading -> "Scanning your drives…"
                state.folders.isEmpty() -> null
                else -> "$videoCount videos in ${state.folders.size} folders"
            },
            onSearch = navigation.onSearch,
        ) {
            // While scanning, the refresh button turns into a small spinner (the title bar has its own refresh).
            if (inTitleBar) Unit else if (state.loading) Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            } else IconButton({ onEvent(HomeContract.Event.Refresh) }) { Icon(Icons.Outlined.Refresh, "Refresh") }
            if (folderImport.available) IconButton(folderImport::pickFolder) { Icon(Icons.Outlined.CreateNewFolder, "Open folder") }
            IconButton(onViewMenu) { Icon(Icons.Outlined.GridView, "View options") }
        }
        if (!inTitleBar) HorizontalDivider(Modifier.padding(horizontal = inset).padding(top = 8.dp, bottom = 4.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f))
    }

    if (opts.viewMode == ViewMode.Files) {
        FileList(state.files, opts, emptyTitle = "No videos found.", header = { header(32.dp) })
        return
    }
    if (state.folders.isEmpty()) {
        Column(Modifier.fillMaxSize()) { header(32.dp); LibrarySourcesBar(folderImport, Modifier.padding(top = 12.dp), inset = 32.dp); emptyState() }
        return
    }
    if (opts.layout == ListLayout.List) {
        Column(Modifier.fillMaxSize()) {
            header(32.dp)
            LibrarySourcesBar(folderImport, Modifier.padding(top = 12.dp), inset = 32.dp)
            Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                FolderList(state.folders, opts, showPath = opts.viewMode == ViewMode.AllFolders, newFolders = state.newFolders,
                    onDelete = { onEvent(HomeContract.Event.DeleteFolders(it)) }) { navigation.onFolder(it.path) }
            }
        }
        return
    }
    val picker = rememberFolderPicker()
    val (shareMarks, shareActions) = rememberShareMarks()
    Column(Modifier.fillMaxSize()) {
    LazyVerticalGrid(
        GridCells.Adaptive(CardGrid.minCardWidth), Modifier.fillMaxWidth().weight(1f),
        contentPadding = CardGrid.contentPadding,
        horizontalArrangement = Arrangement.spacedBy(CardGrid.gap),
        verticalArrangement = Arrangement.spacedBy(CardGrid.gap),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) { Column { header(0.dp); LibrarySourcesBar(folderImport, Modifier.padding(top = 12.dp), inset = 0.dp) } }
        state.lastPlayed?.let { last ->
            item(span = { GridItemSpan(maxLineSpan) }) {
                ContinueWatchingCard(last, progress = null, onPlay = { onEvent(HomeContract.Event.PlayLast) }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) { SectionTitle("Folders", trailing = "${state.folders.size}", inset = 0.dp) }
        items(state.folders, key = { it.path }) { folder ->
            FolderCard(
                folder, onClick = { picker.tap(folder) { navigation.onFolder(it.path) } }, showPath = opts.viewMode == ViewMode.AllFolders,
                showThumbnail = opts.folderThumbnails, hasNew = folder.path in state.newFolders,
                onLongClick = { picker.toggle(folder) }, menu = { dismiss -> FolderMenuItems(folder, dismiss) },
                selected = if (picker.selecting) folder.path in picker.selected else null,
                onMakePrivate = { shareActions.makeFolderPrivate(folder.path) }.takeIf { folder.path in shareMarks.value.folders },
            )
        }
    }
    FolderPickerUi(picker, state.folders, onDelete = { onEvent(HomeContract.Event.DeleteFolders(it)) })
    }
}
