package org.app.nxplayer.ui.music

import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import kotlinx.coroutines.delay
import org.app.nxplayer.R
import org.app.nxplayer.core.formatDuration
import org.app.nxplayer.data.AppGraph
import org.app.nxplayer.playback.PlayerHolder
import org.app.nxplayer.player.EqualizerPanel
import org.app.nxplayer.player.PlayerAccent
import org.app.nxplayer.ui.LocalOps

private val Bg = Color(0xFF0D1117)
private val Dim = Color(0x99FFFFFF)
private val speeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

@Composable
fun NowPlayingScreen(onClose: () -> Unit) {
    val player = PlayerHolder.current ?: return onClose()
    val context = LocalContext.current
    val ops = LocalOps.current
    val queue by PlayerHolder.queue.collectAsState()
    val user by AppGraph.userData.data.collectAsState()
    var index by remember { mutableIntStateOf(player.currentMediaItemIndex) }
    var playing by remember { mutableStateOf(player.playWhenReady && player.playbackState != Player.STATE_ENDED) }
    var shuffle by remember { mutableStateOf(player.shuffleModeEnabled) }
    var repeat by remember { mutableIntStateOf(player.repeatMode) }
    var pos by remember { mutableLongStateOf(player.currentPosition) }
    var dur by remember { mutableLongStateOf(player.duration.coerceAtLeast(0)) }
    var dragging by remember { mutableStateOf<Float?>(null) }
    var abA by remember { mutableLongStateOf(-1L) }
    var abB by remember { mutableLongStateOf(-1L) }
    var sheet by remember { mutableStateOf<String?>(null) }
    var speedMenu by remember { mutableStateOf(false) }
    var moreMenu by remember { mutableStateOf(false) }
    val file = queue.getOrNull(index)

    DisposableEffect(player) {
        val l = object : Player.Listener {
            override fun onEvents(p: Player, events: Player.Events) { playing = p.playWhenReady && p.playbackState != Player.STATE_ENDED }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) { index = player.currentMediaItemIndex; abA = -1; abB = -1 }
            override fun onShuffleModeEnabledChanged(enabled: Boolean) { shuffle = enabled }
            override fun onRepeatModeChanged(mode: Int) { repeat = mode }
        }
        player.addListener(l)
        onDispose { player.removeListener(l) }
    }
    LaunchedEffect(player) {
        while (true) {
            pos = player.currentPosition
            dur = player.duration.coerceAtLeast(0)
            if (abA >= 0 && abB > abA && pos >= abB) player.seekTo(abA)
            delay(250)
        }
    }

    val spin = remember { Animatable(0f) }
    LaunchedEffect(playing) {
        while (playing) {
            spin.animateTo(spin.value + 360f, tween(12000, easing = LinearEasing))
            spin.snapTo(spin.value % 360f)
        }
    }
    val arm by animateFloatAsState(if (playing) 16f else 0f, tween(400), label = "arm")

    Dialog(onClose, DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Column(Modifier.fillMaxSize().background(Bg).statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClose) { Icon(painterResource(R.drawable.ic_chevron_down), "Close", tint = Color.White) }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(file?.title ?: player.mediaMetadata.title?.toString().orEmpty(), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium, maxLines = 1, modifier = Modifier.basicMarquee())
                    Text(file?.artist?.ifEmpty { null } ?: "Unknown", color = Dim, fontSize = 13.sp, maxLines = 1)
                }
                IconButton({ file?.let { ops.share(listOf(it)) } }) { Icon(painterResource(R.drawable.ic_ov_share), "Share", tint = Color.White) }
            }

            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Box(Modifier.padding(horizontal = 44.dp).fillMaxWidth().aspectRatio(1f).rotate(spin.value), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        val r = size.minDimension / 2
                        drawCircle(Color(0xFF3A3F46), r)
                        drawCircle(Color(0xFF0A0A0A), r * 0.96f)
                        for (k in 1..6) drawCircle(Color(0xFF1A1A1A), r * (0.68f + k * 0.045f), style = Stroke(1.dp.toPx()))
                        drawCircle(Color(0xFF222222), r * 0.68f)
                        drawCircle(Color(0xFF8A8A8A), r * 0.64f)
                    }
                    Icon(Icons.Default.MusicNote, null, Modifier.size(150.dp), tint = Color(0xFF5E5E5E))
                }
                Canvas(Modifier.align(Alignment.TopEnd).padding(top = 6.dp, end = 40.dp).size(170.dp, 90.dp)) {
                    val pivot = Offset(14.dp.toPx(), 14.dp.toPx())
                    rotate(arm, pivot) {
                        drawLine(Color(0xFFCFCFCF), pivot, Offset(size.width * 0.62f, size.height * 0.35f), 3.dp.toPx(), StrokeCap.Round)
                        drawLine(Color(0xFFCFCFCF), Offset(size.width * 0.62f, size.height * 0.35f), Offset(size.width * 0.9f, size.height * 0.7f), 3.dp.toPx(), StrokeCap.Round)
                        drawRect(Color(0xFFBDBDBD), Offset(size.width * 0.86f, size.height * 0.66f), androidx.compose.ui.geometry.Size(14.dp.toPx(), 10.dp.toPx()))
                    }
                    drawCircle(Color(0xFF9E9E9E), 13.dp.toPx(), pivot)
                    drawCircle(Color(0xFFE0E0E0), 8.dp.toPx(), pivot)
                }
            }

            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton({ sheet = "eq" }) { Icon(painterResource(R.drawable.ic_pl_eq), "Equalizer", tint = Color.White) }
                IconButton({
                    when {
                        abA < 0 -> { abA = player.currentPosition; Toast.makeText(context, "A point set", Toast.LENGTH_SHORT).show() }
                        abB < 0 -> { abB = player.currentPosition.coerceAtLeast(abA + 500); Toast.makeText(context, "A-B repeat on", Toast.LENGTH_SHORT).show() }
                        else -> { abA = -1; abB = -1; Toast.makeText(context, "A-B repeat off", Toast.LENGTH_SHORT).show() }
                    }
                }) {
                    val c = if (abA >= 0) PlayerAccent else Color.White
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("A→", color = c, fontSize = 11.sp, fontWeight = FontWeight.Bold, lineHeight = 11.sp)
                        Text("←B", color = c, fontSize = 11.sp, fontWeight = FontWeight.Bold, lineHeight = 11.sp)
                    }
                }
                Box {
                    IconButton({ speedMenu = true }) { Icon(Icons.Default.Speed, "Speed", tint = if (player.playbackParameters.speed != 1f) PlayerAccent else Color.White) }
                    DropdownMenu(speedMenu, { speedMenu = false }) {
                        speeds.forEach { s -> DropdownMenuItem({ Text("${s}x") }, { speedMenu = false; player.playbackParameters = PlaybackParameters(s) }) }
                    }
                }
                val fav = file != null && file.uri in user.favourites
                IconButton({ file?.let { f -> AppGraph.userData.update { it.toggleFavourite(f.uri) } } }) {
                    Icon(if (fav) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Favourite", tint = Color.White)
                }
                Box {
                    IconButton({ moreMenu = true }) { Icon(Icons.Default.MoreVert, "More", tint = Color.White) }
                    DropdownMenu(moreMenu, { moreMenu = false }) {
                        DropdownMenuItem({ Text("Add to Playlist") }, { moreMenu = false; sheet = "add" })
                        DropdownMenuItem({ Text("Share") }, { moreMenu = false; file?.let { ops.share(listOf(it)) } })
                    }
                }
            }

            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                Text(formatDuration(dragging?.let { (it * dur).toLong() } ?: pos), color = Color.White, fontSize = 13.sp)
                Spacer(Modifier.weight(1f))
                Text(formatDuration(dur), color = Color.White, fontSize = 13.sp)
            }
            val frac = dragging ?: if (dur > 0) (pos.toFloat() / dur).coerceIn(0f, 1f) else 0f
            val durNow by rememberUpdatedState(dur)
            fun commit(f: Float) { val t = (f * durNow).toLong(); pos = t; player.seekTo(t) }
            Canvas(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(40.dp)
                    .pointerInput(Unit) {
                        detectTapGestures { o -> commit((o.x / size.width).coerceIn(0f, 1f)) }
                    }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = { o -> dragging = (o.x / size.width).coerceIn(0f, 1f) },
                            onDragEnd = { dragging?.let { commit(it) }; dragging = null },
                            onDragCancel = { dragging = null },
                        ) { change, _ -> change.consume(); dragging = (change.position.x / size.width).coerceIn(0f, 1f) }
                    },
            ) {
                val y = size.height / 2
                val x = size.width * frac
                drawLine(Color(0xFF4A4F57), Offset(0f, y), Offset(size.width, y), 2.dp.toPx())
                drawLine(PlayerAccent, Offset(0f, y), Offset(x, y), 2.dp.toPx())
                drawCircle(PlayerAccent, 10.dp.toPx(), Offset(x, y))
            }

            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton({ player.shuffleModeEnabled = !shuffle }) { Icon(painterResource(R.drawable.ic_pl_shuffle), "Shuffle", tint = if (shuffle) PlayerAccent else Color.White) }
                IconButton({ player.seekToPreviousMediaItem() }) { Icon(painterResource(R.drawable.ic_pl_prev), "Previous", tint = Color.White) }
                Box(
                    Modifier.size(64.dp).clip(CircleShape).border(2.dp, Color.White, CircleShape).clickable {
                        if (player.playWhenReady && player.playbackState != Player.STATE_ENDED) player.pause() else {
                            if (player.playbackState == Player.STATE_IDLE) player.prepare()
                            if (player.playbackState == Player.STATE_ENDED) player.seekTo(player.currentMediaItemIndex, 0)
                            player.play(); PlayerHolder.startBackground(context)
                        }
                    },
                    contentAlignment = Alignment.Center,
                ) { Icon(painterResource(if (playing) R.drawable.ic_pl_pause else R.drawable.ic_pl_play), "Play/Pause", Modifier.size(32.dp), tint = Color.White) }
                IconButton({ player.seekToNextMediaItem() }) { Icon(painterResource(R.drawable.ic_pl_next), "Next", tint = Color.White) }
                IconButton({
                    player.repeatMode = when (repeat) { Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL; Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE; else -> Player.REPEAT_MODE_OFF }
                }) {
                    Icon(painterResource(if (repeat == Player.REPEAT_MODE_ONE) R.drawable.ic_pl_loop_one else R.drawable.ic_pl_loop), "Repeat", tint = if (repeat == Player.REPEAT_MODE_OFF) Color.White else PlayerAccent)
                }
            }

            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.clickable { Toast.makeText(context, "No lyrics found", Toast.LENGTH_SHORT).show() }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Description, null, Modifier.size(22.dp), tint = Color.White)
                    Spacer(Modifier.width(10.dp))
                    Text("Lyrics", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.weight(1f))
                Row(Modifier.clickable { sheet = "queue" }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.QueueMusic, null, Modifier.size(22.dp), tint = Color.White)
                    Spacer(Modifier.width(10.dp))
                    Text("Playing Queue", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        when (sheet) {
            "eq" -> Dialog({ sheet = null }) {
                Column(Modifier.fillMaxWidth().background(Color(0xF0202429), RoundedCornerShape(8.dp)).padding(20.dp)) { EqualizerPanel() }
            }
            "queue" -> Dialog({ sheet = null }) {
                Column(Modifier.fillMaxWidth().heightIn(max = 520.dp).background(Color(0xFF202429), RoundedCornerShape(8.dp)).padding(vertical = 12.dp)) {
                    Text("Playing Queue (${queue.size})", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                    LazyColumn {
                        itemsIndexed(queue) { i, f ->
                            Row(Modifier.fillMaxWidth().clickable { player.seekTo(i, 0); player.play(); sheet = null }.padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(f.title, color = if (i == index) PlayerAccent else Color.White, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(f.artist.ifEmpty { "Unknown" }, color = Dim, fontSize = 12.sp, maxLines = 1)
                                }
                                if (i == index) Icon(Icons.Default.MusicNote, null, Modifier.size(18.dp), tint = PlayerAccent)
                            }
                        }
                    }
                }
            }
            "add" -> file?.let { org.app.nxplayer.ui.library.AddToPlaylistDialog(listOf(it), music = true) { sheet = null } }
        }
    }
}
