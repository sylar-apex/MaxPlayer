package org.app.maxplayerpro.appcore.adaptive

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.app.kmediaplayer.model.MediaFile
import org.app.kmediaplayer.util.formatDuration
import org.app.kmediaplayer.util.formatSize
import org.app.maxplayerpro.appcore.component.ThumbnailLoader
import org.app.maxplayerpro.appcore.theme.MaxColors
import org.app.kmediaplayer.platform.PlatformMedia
import org.app.maxplayerpro.sharedlogic.model.ListField
import org.app.maxplayerpro.sharedlogic.model.ListViewOptions
import org.app.maxplayerpro.sharedlogic.model.MediaFolder

/** Card grid spacing for wide layouts. */
object CardGrid {
    val minCardWidth = 232.dp
    val contentPadding = PaddingValues(start = 32.dp, end = 32.dp, top = 0.dp, bottom = 40.dp)
    val gap = 22.dp
}

private val cardShape = RoundedCornerShape(14.dp)

/** Loads [file]'s thumbnail into state; null until ready or for audio. */
@Composable
private fun rememberThumbnail(file: MediaFile?, px: Int = 480): ImageBitmap? {
    var bmp by remember(file?.uri) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(file?.uri) { if (file != null && !file.isAudio) bmp = ThumbnailLoader.load(file, px) }
    return bmp
}

/** Hover state plus a gentle lift, for pointer devices. */
@Composable
private fun rememberHover(): Pair<MutableInteractionSource, Boolean> {
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    return source to hovered
}

/** 16:9 artwork with a play glyph on hover, a duration badge and an optional progress bar. */
@Composable
private fun Artwork(
    file: MediaFile?, hovered: Boolean, modifier: Modifier, progress: Float? = null, fallbackFolder: Boolean = false,
    showThumbnail: Boolean = true, showLength: Boolean = true, tags: List<String> = emptyList(),
) {
    val thumb = if (showThumbnail) rememberThumbnail(file) else null
    val zoom by animateFloatAsState(if (hovered) 1.06f else 1f)
    Box(modifier.aspectRatio(16f / 9f).clip(cardShape).background(MaxColors.thumbnailBackground), contentAlignment = Alignment.Center) {
        if (thumb != null) Image(thumb, null, Modifier.fillMaxSize().graphicsLayer { scaleX = zoom; scaleY = zoom }, contentScale = ContentScale.Crop)
        else Icon(
            when { fallbackFolder -> Icons.Default.Folder; file?.isAudio == true -> Icons.Default.MusicNote; else -> Icons.Default.Movie },
            null, Modifier.size(44.dp), tint = MaxColors.thumbnailIcon,
        )
        if (hovered && !fallbackFolder) Box(
            Modifier.size(54.dp).background(Color(0xCC000000), CircleShape).border(1.5.dp, Color(0x55FFFFFF), CircleShape),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Default.PlayArrow, "Play", Modifier.size(32.dp), tint = Color.White) }
        if (showLength && file != null && file.durationMs > 0) Text(
            formatDuration(file.durationMs), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp).background(Color(0xB3000000), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
        )
        if (tags.isNotEmpty()) Row(Modifier.align(Alignment.TopEnd).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) { tags.forEach { CornerTag(it) } }
        if (progress != null && progress > 0f) LinearProgressIndicator(
            progress = { progress }, modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(3.dp),
            color = MaterialTheme.colorScheme.primary, trackColor = Color(0x55FFFFFF),
        )
    }
}

/** A video in a wide grid: artwork, title and details. */
@Composable
fun VideoCard(
    file: MediaFile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    progress: Float? = null,
    tags: List<String> = emptyList(),
    highlighted: Boolean = false,
    options: ListViewOptions = ListViewOptions(),
    lastPlayedMs: Long = 0,
    onMore: (() -> Unit)? = null,
) {
    val (source, hovered) = rememberHover()
    val fields = options.fields
    val lengthOnArtwork = ListField.Length in fields && options.lengthOverThumbnail
    Column(modifier.hoverable(source).clickable(interactionSource = source, indication = null, onClick = onClick)) {
        Artwork(
            file, hovered, Modifier.fillMaxWidth().let { if (highlighted) it.border(2.dp, MaterialTheme.colorScheme.primary, cardShape) else it }, progress,
            showThumbnail = ListField.Thumbnail in fields && options.videoThumbnails, showLength = lengthOnArtwork, tags = tags,
        )
        Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(
                    file.title, maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 14.sp, lineHeight = 19.sp, fontWeight = FontWeight.Medium,
                    color = if (hovered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(fileDetails(file, fields, lengthInline = ListField.Length in fields && !lengthOnArtwork, lastPlayedMs), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (ListField.Path in fields) Text(file.folderPath, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (onMore != null) IconButton(onMore, Modifier.size(32.dp)) { Icon(Icons.Outlined.MoreVert, "More", Modifier.size(18.dp)) }
        }
    }
}

/** A folder in a wide grid: the first video's artwork with the folder's name and contents. */
@Composable
fun FolderCard(folder: MediaFolder, onClick: () -> Unit, modifier: Modifier = Modifier, showPath: Boolean = false, showThumbnail: Boolean = true, hasNew: Boolean = false) {
    val (source, hovered) = rememberHover()
    val cover = remember(folder.path, showThumbnail) { if (showThumbnail) folder.files.firstOrNull { !it.isAudio } else null }
    Column(modifier.hoverable(source).clickable(interactionSource = source, indication = null, onClick = onClick)) {
        Box {
            Artwork(cover, hovered, Modifier.fillMaxWidth(), fallbackFolder = cover == null)
            Box(Modifier.matchParentSize().clip(cardShape).background(Brush.verticalGradient(0.45f to Color.Transparent, 1f to Color(0xD9000000))))
            // The folder holds videos not played yet that are still tagged "NEW".
            if (hasNew) Box(Modifier.align(Alignment.TopEnd).padding(8.dp)) { CornerTag("NEW") }
            Row(Modifier.align(Alignment.BottomStart).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Folder, null, Modifier.size(16.dp), tint = Color(0xE6FFFFFF))
                Spacer(Modifier.width(6.dp))
                Text("${folder.files.size} ${if (folder.files.size == 1) "video" else "videos"}", color = Color(0xE6FFFFFF), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
        Text(
            folder.name, Modifier.padding(top = 10.dp), maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
            color = if (hovered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
        )
        Text(
            if (showPath) folder.path else formatSize(folder.sizeBytes), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

/** A small tag ("NEW", …) in the top corner of the artwork. */
@Composable
private fun CornerTag(tag: String) {
    // Tight, trimmed line box so the pill hugs the letters instead of carrying the font's extra leading.
    Text(
        tag, color = Color.White,
        style = TextStyle(
            fontSize = 9.sp, lineHeight = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.6.sp,
            lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
        ),
        modifier = Modifier.background(if (tag == "NEW") MaxColors.tagNew else MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 3.dp),
    )
}

/** Large "continue watching" banner for the most recently played video. */
@Composable
fun ContinueWatchingCard(file: MediaFile, progress: Float?, onPlay: () -> Unit, modifier: Modifier = Modifier) {
    val (source, hovered) = rememberHover()
    val thumb = rememberThumbnail(file, 960)
    Box(
        modifier.height(220.dp).clip(RoundedCornerShape(20.dp)).background(MaxColors.thumbnailBackground)
            .hoverable(source).clickable(interactionSource = source, indication = null, onClick = onPlay),
    ) {
        if (thumb != null) Image(thumb, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0f to Color(0xF2000000), 0.6f to Color(0x99000000), 1f to Color(0x22000000))))
        Column(Modifier.align(Alignment.CenterStart).padding(start = 32.dp, end = 32.dp).fillMaxWidth(0.6f)) {
            Text("CONTINUE WATCHING", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Spacer(Modifier.height(8.dp))
            Text(file.title, color = Color.White, fontSize = 26.sp, lineHeight = 31.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(18.dp))
            Row(
                Modifier.background(if (hovered) Color.White else Color(0xE6FFFFFF), RoundedCornerShape(50)).padding(start = 14.dp, end = 20.dp, top = 9.dp, bottom = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.PlayArrow, null, tint = Color.Black)
                Spacer(Modifier.width(6.dp))
                Text("Resume", color = Color.Black, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }
        if (progress != null && progress > 0f) LinearProgressIndicator(
            progress = { progress }, modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(4.dp),
            color = MaterialTheme.colorScheme.primary, trackColor = Color(0x44FFFFFF),
        )
    }
}

/** Section title for wide pages. */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: String? = null, inset: androidx.compose.ui.unit.Dp = 32.dp) {
    Row(modifier.fillMaxWidth().padding(start = inset, end = inset, top = 28.dp, bottom = 14.dp), verticalAlignment = Alignment.Bottom) {
        Text(text, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        if (trailing != null) {
            Spacer(Modifier.width(10.dp))
            Text(trailing, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 2.dp))
        }
    }
}

/** The detail line under a card: exactly the fields chosen in View options. */
private fun fileDetails(file: MediaFile, fields: Set<ListField>, lengthInline: Boolean, lastPlayedMs: Long): String = buildList {
    if (lengthInline && file.durationMs > 0) add(formatDuration(file.durationMs))
    if (ListField.Resolution in fields && file.height > 0) add(resolutionLabel(file.height))
    if (ListField.FrameRate in fields && file.frameRate > 0) add("${file.frameRate.toInt()} fps")
    if (ListField.FileExtension in fields) add(file.extension.uppercase())
    if (ListField.Size in fields && file.sizeBytes > 0) add(formatSize(file.sizeBytes))
    if (ListField.Date in fields && file.dateModifiedMs > 0) add(PlatformMedia.formatDateLocalized(file.dateModifiedMs, "medium", null))
    if (ListField.PlayedTime in fields && lastPlayedMs > 0) add("Played " + PlatformMedia.formatDateLocalized(lastPlayedMs, "short", "short"))
}.joinToString(" · ")

private fun resolutionLabel(height: Int): String = when {
    height >= 2000 -> "4K"
    height >= 1400 -> "1440p"
    height >= 1000 -> "1080p"
    height >= 700 -> "720p"
    else -> "${height}p"
}
