package org.app.maxplayerpro.music.ui.component

import org.app.maxplayerpro.sharedlogic.usecase.NetworkShareRequests
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.app.kmediaplayer.model.MediaFile
import org.app.kmediaplayer.ui.music.CoverArt
import org.app.kmediaplayer.ui.music.PlayingBars
import org.app.kmediaplayer.ui.music.rememberNowPlaying
import org.app.kmediaplayer.util.formatDuration
import org.app.maxplayerpro.appcore.component.EmptyState

/** Album, else title: what a track's generated cover is coloured by. */
internal val MediaFile.coverSeed: String get() = album.takeIf { it.isNotBlank() && it != "Unknown" } ?: title

/** Album tag, else the folder the track sits in. */
internal val MediaFile.albumOrFolder: String get() = album.takeIf { it.isNotBlank() && it != "Unknown" } ?: folderName.ifBlank { "Unknown album" }

internal fun String.orUnknown(what: String) = takeIf { it.isNotBlank() && it != "Unknown" } ?: "Unknown $what"

/** "42 songs · 2 h 13 min" */
internal fun summary(files: List<MediaFile>): String {
    val total = files.sumOf { it.durationMs } / 60_000
    val songs = "${files.size} song${if (files.size == 1) "" else "s"}"
    return when {
        total <= 0 -> songs
        total < 60 -> "$songs · $total min"
        else -> "$songs · ${total / 60} h ${total % 60} min"
    }
}

/** A faint panel colour that works on light and dark backgrounds. */
@Composable
internal fun panelColor() = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.035f)

@Composable
internal fun dividerColor() = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)

/**
 * Slim header: title and song count, then [middle] (search), then Shuffle and Play, then [trailing].
 * Phones drop the Shuffle label and keep icon buttons.
 */
@Composable
internal fun MusicHeader(
    title: String, files: List<MediaFile>, compact: Boolean, onPlay: () -> Unit, onShuffle: () -> Unit,
    modifier: Modifier = Modifier, leading: @Composable () -> Unit = {}, middle: @Composable RowScope.() -> Unit = {}, trailing: @Composable () -> Unit = {},
) {
    val colors = MaterialTheme.colorScheme
    Row(modifier.fillMaxWidth().height(64.dp), verticalAlignment = Alignment.CenterVertically) {
        leading()
        Column(Modifier.padding(end = 20.dp)) {
            Text(title, fontSize = if (compact) 20.sp else 24.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(if (files.isEmpty()) " " else summary(files), fontSize = 12.sp, color = colors.onSurfaceVariant, maxLines = 1)
        }
        middle()
        if (files.isNotEmpty()) {
            if (compact) IconButton(onShuffle) { Icon(Icons.Default.Shuffle, "Shuffle") }
            else Row(
                Modifier.padding(start = 12.dp).height(40.dp).clip(CircleShape).border(1.dp, colors.onSurface.copy(alpha = 0.15f), CircleShape)
                    .clickable(onClick = onShuffle).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Shuffle, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Shuffle", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.width(10.dp))
            Box(Modifier.size(44.dp).clip(CircleShape).background(colors.primary).clickable(onClick = onPlay), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.PlayArrow, "Play all", Modifier.size(26.dp), tint = colors.onPrimary)
            }
        }
        trailing()
    }
}

/** Rounded tabs; the selected one is filled with the accent. */
@Composable
internal fun PillTabs(tabs: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        tabs.forEachIndexed { i, t ->
            val on = i == selected
            Box(
                Modifier.height(38.dp).clip(CircleShape)
                    .background(if (on) MaterialTheme.colorScheme.primary else panelColor())
                    .clickable { onSelect(i) }.padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(t, fontSize = 14.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

/**
 * Songs. Wide windows show a table (#, title, artist, album, length) inside a rounded panel;
 * phones show compact rows.
 */
@Composable
internal fun Tracks(
    files: List<MediaFile>, favourites: Set<String>, modifier: Modifier, wide: Boolean,
    onAdd: (MediaFile) -> Unit, onToggleFavourite: (MediaFile) -> Unit, onPlay: (Int) -> Unit,
) {
    // Every song can be shared on the network from its menu, like a video.
    val onShare: (MediaFile) -> Unit = { NetworkShareRequests.request(listOf(it)) }
    if (files.isEmpty()) { EmptyState("No music found.", modifier); return }
    val (nowUri, playing) = rememberNowPlaying()
    if (!wide) {
        LazyColumn(modifier, contentPadding = PaddingValues(vertical = 4.dp)) {
            itemsIndexed(files, key = { _, f -> f.uri }) { i, f ->
                TrackRow(i, f, f.uri == nowUri, playing, f.uri in favourites, false, { onPlay(i) }, { onAdd(f) }, { onToggleFavourite(f) }, { onShare(f) })
            }
        }
        return
    }
    Column(modifier.padding(horizontal = 24.dp).clip(RoundedCornerShape(16.dp)).background(panelColor())) {
        Row(Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            HeadCell("#", Modifier.width(40.dp), center = true)
            Spacer(Modifier.width(66.dp))
            HeadCell("TITLE", Modifier.weight(3f))
            HeadCell("ARTIST", Modifier.weight(1.4f))
            HeadCell("ALBUM", Modifier.weight(1.4f))
            Box(Modifier.width(64.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Schedule, "Length", Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(48.dp))
        }
        HorizontalDivider(color = dividerColor())
        LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
            itemsIndexed(files, key = { _, f -> f.uri }) { i, f ->
                TrackRow(i, f, f.uri == nowUri, playing, f.uri in favourites, true, { onPlay(i) }, { onAdd(f) }, { onToggleFavourite(f) }, { onShare(f) })
                if (i < files.lastIndex) HorizontalDivider(Modifier.padding(horizontal = 18.dp), color = dividerColor())
            }
        }
    }
}

@Composable
private fun HeadCell(text: String, modifier: Modifier, center: Boolean = false) {
    Text(text, modifier, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = if (center) TextAlign.Center else null)
}

@Composable
private fun TrackRow(
    index: Int, f: MediaFile, current: Boolean, playing: Boolean, favourite: Boolean, wide: Boolean,
    onPlay: () -> Unit, onAdd: () -> Unit, onToggleFavourite: () -> Unit, onShare: () -> Unit,
) {
    val hover = remember { MutableInteractionSource() }
    val hovered by hover.collectIsHoveredAsState()
    val colors = MaterialTheme.colorScheme
    val bg = when {
        current -> colors.primary.copy(alpha = 0.10f)
        hovered -> colors.onSurface.copy(alpha = 0.04f)
        else -> Color.Transparent
    }
    val sub = colors.onSurfaceVariant
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 3.dp).clip(RoundedCornerShape(10.dp)).background(bg)
            .hoverable(hover).clickable(onClick = onPlay).padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (wide) {
            Box(Modifier.width(40.dp), contentAlignment = Alignment.Center) {
                when {
                    current -> PlayingBars(colors.primary, playing, Modifier.size(16.dp))
                    hovered -> Icon(Icons.Default.PlayArrow, "Play", Modifier.size(20.dp))
                    else -> Text("${index + 1}", fontSize = 13.sp, color = sub)
                }
            }
            Spacer(Modifier.width(8.dp))
        }
        Box(contentAlignment = Alignment.Center) {
            CoverArt(f.coverSeed, Modifier.size(44.dp), corner = 8.dp)
            if (current && !wide) Box(Modifier.size(44.dp).background(Color(0x77000000), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                PlayingBars(Color.White, playing, Modifier.size(16.dp))
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(3f)) {
            Text(f.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = if (current) colors.primary else colors.onSurface)
            Text(
                if (wide) f.artist.orUnknown("artist") else "${f.artist.orUnknown("artist")} · ${f.albumOrFolder}",
                fontSize = 12.sp, color = sub, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        if (wide) {
            Cell(f.artist.orUnknown("artist"), Modifier.weight(1.4f))
            Cell(f.albumOrFolder, Modifier.weight(1.4f))
            Box(Modifier.width(64.dp), contentAlignment = Alignment.Center) {
                Text(if (f.durationMs > 0) formatDuration(f.durationMs) else "–", fontSize = 13.sp, color = sub)
            }
        } else if (f.durationMs > 0) {
            Text(formatDuration(f.durationMs), Modifier.padding(horizontal = 6.dp), fontSize = 12.sp, color = sub)
        }
        TrackMenu(favourite, onAdd, onToggleFavourite, onShare)
    }
}

@Composable
private fun RowScope.Cell(text: String, modifier: Modifier) {
    Text(text, modifier.padding(end = 12.dp), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
private fun TrackMenu(favourite: Boolean, onAdd: () -> Unit, onToggleFavourite: () -> Unit, onShare: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Box(Modifier.width(48.dp), contentAlignment = Alignment.Center) {
        IconButton({ menu = true }) {
            if (favourite) Icon(Icons.Default.Favorite, "Favourite", Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
            else Icon(Icons.Default.MoreVert, "More", Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DropdownMenu(menu, { menu = false }) {
            DropdownMenuItem({ Text(if (favourite) "Remove from Favourites" else "Add to Favourites") }, { menu = false; onToggleFavourite() })
            DropdownMenuItem({ Text("Add to Playlist") }, { menu = false; onAdd() })
            DropdownMenuItem({ Text("Share on network") }, { menu = false; onShare() })
        }
    }
}

/** An album, artist, folder, playlist or song shown as a card. */
internal class GroupItem(val key: String, val title: String, val subtitle: String, val seed: String, val icon: ImageVector? = null, val onClick: () -> Unit)

/** Cards in a responsive grid. */
@Composable
internal fun GroupGrid(items: List<GroupItem>, modifier: Modifier) {
    if (items.isEmpty()) { EmptyState("No music found.", modifier); return }
    LazyVerticalGrid(
        GridCells.Adaptive(170.dp), modifier,
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items, key = { it.key }) { g -> GroupCard(g) }
    }
}

@Composable
private fun GroupCard(g: GroupItem) {
    val hover = remember { MutableInteractionSource() }
    val hovered by hover.collectIsHoveredAsState()
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier.clip(RoundedCornerShape(16.dp)).background(if (hovered) colors.onSurface.copy(alpha = 0.06f) else panelColor())
            .hoverable(hover).clickable(onClick = g.onClick).padding(12.dp),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
            if (g.icon != null) {
                Box(Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)).background(colors.primary.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                    Icon(g.icon, null, Modifier.size(44.dp), tint = colors.primary)
                }
            } else CoverArt(g.seed, Modifier.fillMaxSize(), corner = 12.dp)
            if (hovered) {
                Box(Modifier.align(Alignment.BottomEnd).padding(10.dp).size(44.dp).clip(CircleShape).background(colors.primary), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.PlayArrow, null, Modifier.size(26.dp), tint = colors.onPrimary)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(g.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(g.subtitle.ifEmpty { " " }, fontSize = 12.sp, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

internal val CreateIcon = Icons.Default.Add
internal val FavouriteIcon = Icons.Default.Favorite
