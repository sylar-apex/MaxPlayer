package org.app.maxplayerpro.appcore.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.app.kmediaplayer.model.MediaFile
import org.app.kmediaplayer.util.formatDuration
import org.app.maxplayerpro.appcore.theme.MaxColors
import org.app.maxplayerpro.sharedlogic.data.ThumbnailCache

/** Loads video frame thumbnails, cached in memory. */
expect object ThumbnailLoader : ThumbnailCache {
    suspend fun load(file: MediaFile, px: Int = 320): ImageBitmap?
    override fun clear()
}

@Composable
fun Thumbnail(file: MediaFile, modifier: Modifier, showLength: Boolean) {
    var bmp by remember(file.id) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(file.id) { if (!file.isAudio) bmp = ThumbnailLoader.load(file) }
    Box(modifier.clip(RoundedCornerShape(4.dp)).background(MaxColors.thumbnailBackground), contentAlignment = Alignment.Center) {
        val b = bmp
        if (b != null) Image(b, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Icon(if (file.isAudio) Icons.Default.MusicNote else Icons.Default.Movie, null, tint = MaxColors.thumbnailIcon)
        if (showLength && file.durationMs > 0) {
            Text(
                formatDuration(file.durationMs), color = MaxColors.lengthBadgeText, fontSize = 10.sp, lineHeight = 12.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 4.dp, bottom = 5.dp).background(MaxColors.lengthBadge, RoundedCornerShape(2.dp)).padding(horizontal = 2.dp),
            )
        }
    }
}
