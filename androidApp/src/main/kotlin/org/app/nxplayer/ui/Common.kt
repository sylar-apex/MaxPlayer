package org.app.nxplayer.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import org.app.nxplayer.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.app.nxplayer.core.MediaFile
import org.app.nxplayer.core.formatDuration
import org.app.nxplayer.data.AppGraph

@Composable
fun TopBar(title: String, onBack: (() -> Unit)? = null, divider: Boolean = false, actions: @Composable RowScope.() -> Unit = {}) {
    Column(Modifier.background(MaterialTheme.colorScheme.background)) {
    Row(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).statusBarsPadding().height(52.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) IconButton(onBack, Modifier.width(56.dp)) { Icon(painterResource(R.drawable.ic_back), "Navigate up") }
        else Spacer(Modifier.width(16.dp))
        if (onBack != null) Spacer(Modifier.width(4.dp))
        Text(title, Modifier.weight(1f), fontSize = if (divider) 18.sp else 17.5.sp, fontWeight = if (divider) FontWeight.Medium else FontWeight.SemiBold,
            color = if (divider) Color(0xFFD5DBE2) else Color.Unspecified, maxLines = 1, overflow = TextOverflow.Ellipsis)
        actions()
    }
    if (divider) androidx.compose.material3.HorizontalDivider(color = Color(0xFF26303C), thickness = 1.dp)
    }
}

@Composable
fun Thumbnail(file: MediaFile, modifier: Modifier, showLength: Boolean) {
    var bmp by remember(file.id) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(file.id) { if (!file.isAudio) bmp = AppGraph.media.thumbnail(file) }
    Box(modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFF2B3744)), contentAlignment = Alignment.Center) {
        val b = bmp
        if (b != null) Image(b.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Icon(if (file.isAudio) Icons.Default.MusicNote else Icons.Default.Movie, null, tint = Color(0xFF6B7A89))
        if (showLength && file.durationMs > 0) {
            Text(
                formatDuration(file.durationMs), color = Color.White, fontSize = 10.sp, lineHeight = 12.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 4.dp, bottom = 5.dp).background(Color(0xCC000000), RoundedCornerShape(2.dp)).padding(horizontal = 2.dp),
            )
        }
    }
}

@Composable
fun Tag(text: String, color: Color) {
    Text(text, color = Color.White, fontSize = 9.sp, lineHeight = 12.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.background(color, RoundedCornerShape(2.dp)).padding(horizontal = 4.dp))
}

@Composable
fun EmptyState(text: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
    }
}
