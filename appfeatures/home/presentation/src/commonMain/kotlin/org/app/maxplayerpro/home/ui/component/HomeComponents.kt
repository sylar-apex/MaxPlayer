package org.app.maxplayerpro.home.ui.component

import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.app.kmediaplayer.util.formatSize
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

@Composable
private fun FolderGlyph(modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        val r = h * 0.09f
        val tabTop = w * 0.33f
        val tabEnd = w * 0.49f
        val bodyY = h * 0.145f
        val path = Path().apply {
            moveTo(0f, r)
            quadraticTo(0f, 0f, r, 0f)
            lineTo(tabTop, 0f)
            cubicTo(tabTop + (tabEnd - tabTop) * 0.45f, 0f, tabTop + (tabEnd - tabTop) * 0.55f, bodyY, tabEnd, bodyY)
            lineTo(w - r, bodyY)
            quadraticTo(w, bodyY, w, bodyY + r)
            lineTo(w, h - r)
            quadraticTo(w, h, w - r, h)
            lineTo(r, h)
            quadraticTo(0f, h, 0f, h - r)
            close()
        }
        drawPath(path, MaxColors.folderGlyph)
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

@Composable
internal fun FolderList(
    folders: List<MediaFolder>, opts: ListViewOptions, showPath: Boolean,
    /** Folders holding unplayed "NEW" videos; their icon gets a NEW tag. */
    newFolders: Set<String> = emptySet(),
    onOpen: (MediaFolder) -> Unit,
) {
    if (opts.layout == ListLayout.Grid) {
        LazyVerticalGrid(GridCells.Adaptive(150.dp), Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp)) {
            items(folders, key = { it.path }) { f ->
                Column(Modifier.clickable { onOpen(f) }.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    FolderIcon(Modifier.size(width = 96.dp, height = 72.dp), f.path in newFolders)
                    Text(f.name, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.primary)
                    Text("${f.files.size} ${if (f.files.size == 1) "video" else "videos"}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    } else {
        LazyColumn(Modifier.fillMaxSize()) {
            items(folders, key = { it.path }) { f ->
                Row(Modifier.fillMaxWidth().height(62.dp).clickable { onOpen(f) }.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    FolderIcon(Modifier.size(width = 62.dp, height = 46.dp), f.path in newFolders)
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
                }
            }
        }
    }
}
