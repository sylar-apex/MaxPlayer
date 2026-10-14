package org.app.maxplayerpro.music.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.app.kmediaplayer.model.MediaFile
import org.app.maxplayerpro.appcore.component.EmptyState
import org.app.maxplayerpro.appcore.theme.MaxColors

@Composable
internal fun GroupRow(title: String, subtitle: String, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 14.dp)) {
        Text(title, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (subtitle.isNotEmpty()) Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun Tracks(files: List<MediaFile>, favourites: Set<String>, modifier: Modifier, onAdd: (MediaFile) -> Unit, onToggleFavourite: (MediaFile) -> Unit, onPlay: (Int) -> Unit) {
    if (files.isEmpty()) { EmptyState("No music found.", modifier); return }
    LazyColumn(modifier) {
        items(files, key = { it.uri }) { f ->
            var menu by remember { mutableStateOf(false) }
            Row(Modifier.fillMaxWidth().height(56.dp).clickable { onPlay(files.indexOf(f)) }.padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).background(MaxColors.trackIconBackground, RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.MusicNote, null, Modifier.size(22.dp), tint = MaxColors.trackIcon)
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(f.title, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(f.artist, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                Box {
                    IconButton({ menu = true }) { Icon(Icons.Default.MoreVert, "More", Modifier.size(20.dp)) }
                    DropdownMenu(menu, { menu = false }) {
                        DropdownMenuItem({ Text(if (f.uri in favourites) "Remove from Favourites" else "Add to Favourites") }, { menu = false; onToggleFavourite(f) })
                        DropdownMenuItem({ Text("Add to Playlist") }, { menu = false; onAdd(f) })
                    }
                }
            }
        }
    }
}
