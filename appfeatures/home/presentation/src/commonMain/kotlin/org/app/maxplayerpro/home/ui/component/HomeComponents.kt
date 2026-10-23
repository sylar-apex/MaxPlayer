package org.app.maxplayerpro.home.ui.component

import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.PhotoLibrary
import org.app.maxplayerpro.appcore.adaptive.LocalWindowClass
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import org.app.maxplayerpro.sharedlogic.usecase.ShareTarget
import org.app.kmediaplayer.ui.common.PlayerDialog
import androidx.compose.material3.TextButton
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.app.maxplayerpro.sharedlogic.usecase.NetworkShareRequests
import org.app.kmediaplayer.util.formatSize
import org.app.maxplayerpro.appcore.component.FolderGlyph
import org.app.maxplayerpro.appcore.component.SharedMark
import org.app.maxplayerpro.appcore.component.rememberShareMarks
import org.app.maxplayerpro.appcore.theme.MaxColors
import org.app.maxplayerpro.appres.resources.Res
import org.app.maxplayerpro.appres.resources.ic_chip_music
import org.app.maxplayerpro.appres.resources.ic_chip_playlist
import org.app.maxplayerpro.appres.resources.ic_chip_privacy
import org.app.maxplayerpro.appres.resources.ic_local_network
import org.app.maxplayerpro.home.ui.HomeNavigation
import org.app.maxplayerpro.sharedlogic.model.ListField
import org.app.maxplayerpro.sharedlogic.model.ListLayout
import org.app.maxplayerpro.sharedlogic.model.ListViewOptions
import org.app.maxplayerpro.sharedlogic.model.MediaFolder
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun ShortcutChips(navigation: HomeNavigation, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Chip(Res.drawable.ic_chip_music, "Music", navigation.onMusic)
        Chip(Res.drawable.ic_chip_privacy, "Privacy", navigation.onPrivacy)
        Chip(Res.drawable.ic_chip_playlist, "Video Playlist", navigation.onPlaylists)
        Chip(Res.drawable.ic_local_network, "Network", navigation.onNetwork)
        IconChip(Icons.Outlined.PhotoLibrary, "Gallery", navigation.onGallery)
        IconChip(Icons.Outlined.FolderOpen, "Files", navigation.onFiles)
    }
}

/** A chip with a vector icon, for sections without a drawable of their own. */
@Composable
private fun IconChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(
        Modifier.height(32.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50)).clickable(onClick = onClick).padding(start = 12.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun Chip(icon: DrawableResource, label: String, onClick: () -> Unit) {
    Row(
        Modifier.height(32.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50)).clickable(onClick = onClick).padding(start = 12.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(icon), null, Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FolderActionsSheet(folder: MediaFolder, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(folder.name, Modifier.padding(horizontal = 20.dp, vertical = 8.dp), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
        Row(
            Modifier.fillMaxWidth().height(48.dp).clickable { onDismiss(); NetworkShareRequests.request(folder.files, folder.path) }.padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Sensors, null, Modifier.size(22.dp))
            Spacer(Modifier.width(20.dp))
            Column {
                Text("Share on network", fontSize = 15.sp)
                Text("Public, protected with a PIN, or private", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.navigationBarsPadding().height(12.dp))
    }
}

/** The folder glyph, with a small NEW tag in its top corner when the folder has new videos. */
@Composable
private fun FolderIcon(modifier: Modifier, isNew: Boolean) {
    Box(modifier) {
        FolderGlyph(Modifier.fillMaxSize())
        if (isNew) Text(
            "NEW", color = Color.White,
            style = TextStyle(
                fontSize = 6.5.sp, lineHeight = 6.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp,
                lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
            ),
            // Tucked inside the folder body, below the tab, so it reads as a small label rather than a sticker.
            modifier = Modifier.align(Alignment.TopEnd).offset(x = (-4).dp, y = 8.dp)
                .background(MaxColors.tagNew, RoundedCornerShape(2.5.dp)).padding(horizontal = 3.dp, vertical = 2.dp),
        )
    }
}

/**
 * Folders picked for a bulk action. Long-press starts picking; while [selecting], taps add or remove folders instead of
 * opening them. [menuFor] is the folder whose ⋮ sheet is open.
 */
internal class FolderPicker {
    var selected by mutableStateOf(emptySet<String>())
    var menuFor by mutableStateOf<MediaFolder?>(null)
    val selecting: Boolean get() = selected.isNotEmpty()

    fun toggle(f: MediaFolder) { selected = if (f.path in selected) selected - f.path else selected + f.path }
    fun clear() { selected = emptySet() }

    /** A tap: picks while picking, otherwise opens. */
    fun tap(f: MediaFolder, open: (MediaFolder) -> Unit) = if (selecting) toggle(f) else open(f)
}

@Composable
internal fun rememberFolderPicker() = remember { FolderPicker() }

/** The ⋮ sheet and, while picking, the bottom bar: share all on the network, delete all, select all, cancel. */
@Composable
internal fun FolderPickerUi(picker: FolderPicker, folders: List<MediaFolder>, onDelete: (List<MediaFolder>) -> Unit, modifier: Modifier = Modifier) {
    picker.menuFor?.let { f -> FolderActionsSheet(f) { picker.menuFor = null } }
    var confirmDelete by remember { mutableStateOf<List<MediaFolder>?>(null) }
    if (picker.selecting) {
        val picked = folders.filter { it.path in picker.selected }
        Row(
            modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).navigationBarsPadding().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(picker::clear) { Icon(Icons.Default.Close, "Cancel") }
            Text("${picked.size} selected", Modifier.weight(1f), fontWeight = FontWeight.Medium)
            IconButton({ picker.selected = if (picker.selected.size == folders.size) emptySet() else folders.map { it.path }.toSet() }) {
                Icon(Icons.Default.DoneAll, "Select all")
            }
            IconButton({ NetworkShareRequests.request(picked.map { ShareTarget(it.files, it.path) }); picker.clear() }) {
                Icon(Icons.Default.Sensors, "Share on network")
            }
            IconButton({ confirmDelete = picked }) { Icon(Icons.Default.Delete, "Delete") }
        }
    }
    confirmDelete?.let { list ->
        val count = list.sumOf { it.files.size }
        PlayerDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Delete ${list.size} ${if (list.size == 1) "folder" else "folders"}?") },
            text = { Text("$count ${if (count == 1) "video" else "videos"} will be deleted from this device. This can't be undone.") },
            confirmButton = { TextButton({ onDelete(list); confirmDelete = null; picker.clear() }) { Text("DELETE", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton({ confirmDelete = null }) { Text("CANCEL") } },
        )
    }
}

@Composable
internal fun FolderList(
    folders: List<MediaFolder>, opts: ListViewOptions, showPath: Boolean,
    /** Folders holding unplayed "NEW" videos; their icon gets a NEW tag. */
    newFolders: Set<String> = emptySet(),
    onDelete: (List<MediaFolder>) -> Unit = {},
    onOpen: (MediaFolder) -> Unit,
) {
    val picker = rememberFolderPicker()
    val (shareMarks, shareActions) = rememberShareMarks()
    val wide = LocalWindowClass.current.isWide
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            if (opts.layout == ListLayout.Grid) {
                LazyVerticalGrid(GridCells.Adaptive(150.dp), Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp)) {
                    items(folders, key = { it.path }) { f ->
                        Column(Modifier.combinedClickable(onClick = { picker.tap(f, onOpen) }, onLongClick = { picker.toggle(f) }).padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box {
                                FolderIcon(Modifier.size(width = 96.dp, height = 72.dp), f.path in newFolders)
                                if (picker.selecting) PickMark(f.path in picker.selected, Modifier.align(Alignment.TopStart))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(f.name, Modifier.weight(1f, fill = false), maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.primary)
                                if (!picker.selecting && f.path in shareMarks.value.folders) SharedMark({ shareActions.makeFolderPrivate(f.path) }, size = 16.dp)
                                if (!picker.selecting) Box {
                                    var open by remember { mutableStateOf(false) }
                                    IconButton({ if (wide) open = true else picker.menuFor = f }, Modifier.size(24.dp)) {
                                        Icon(Icons.Default.MoreVert, "Folder actions", Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    DropdownMenu(open, { open = false }) { FolderMenuItems(f) { open = false } }
                                }
                            }
                            Text("${f.files.size} ${if (f.files.size == 1) "video" else "videos"}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(folders, key = { it.path }) { f ->
                        val picked = f.path in picker.selected
                        Row(
                            Modifier.fillMaxWidth().height(62.dp).background(if (picked) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent)
                                .combinedClickable(onClick = { picker.tap(f, onOpen) }, onLongClick = { picker.toggle(f) }).padding(start = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box {
                                FolderIcon(Modifier.size(width = 62.dp, height = 46.dp), f.path in newFolders)
                                if (picker.selecting) PickMark(picked, Modifier.align(Alignment.TopStart))
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(f.name.replaceFirstChar { it.uppercaseChar() }, fontSize = 15.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Spacer(Modifier.height(2.dp))
                                val extra = buildList {
                                    add("${f.files.size} ${if (f.files.size == 1) "video" else "videos"}")
                                    if (ListField.Size in opts.fields) add(formatSize(f.sizeBytes))
                                }.joinToString(" · ")
                                Text(extra, fontSize = 12.sp, lineHeight = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (showPath || ListField.Path in opts.fields) Text(f.path, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            if (!picker.selecting && f.path in shareMarks.value.folders) SharedMark({ shareActions.makeFolderPrivate(f.path) })
                            if (!picker.selecting) Box {
                                var open by remember { mutableStateOf(false) }
                                IconButton({ if (wide) open = true else picker.menuFor = f }) { Icon(Icons.Default.MoreVert, "Folder actions", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                                // Desktop: a menu at the button; phones keep the bottom sheet.
                                DropdownMenu(open, { open = false }) { FolderMenuItems(f) { open = false } }
                            }
                            else Spacer(Modifier.width(12.dp))
                        }
                    }
                }
            }
        }
        FolderPickerUi(picker, folders, onDelete)
    }
}

/** A folder's actions as menu items, for the desktop dropdown at its ⋮. */
@Composable
internal fun FolderMenuItems(folder: MediaFolder, dismiss: () -> Unit) {
    DropdownMenuItem(
        text = { Text("Share on network") },
        leadingIcon = { Icon(Icons.Default.Sensors, null) },
        onClick = { dismiss(); NetworkShareRequests.request(folder.files, folder.path) },
    )
}

/** The pick circle shown on each folder while picking. */
@Composable
internal fun PickMark(picked: Boolean, modifier: Modifier = Modifier) {
    Icon(
        if (picked) Icons.Default.CheckCircle else Icons.Outlined.Circle, if (picked) "Picked" else "Not picked",
        modifier.padding(2.dp).size(20.dp).background(MaterialTheme.colorScheme.background, CircleShape),
        tint = if (picked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
