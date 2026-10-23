package org.app.maxplayerpro.appcore.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage

/** A photo for [PhotoViewer]: [model] is whatever the image loader opens (file URI, content URI or http URL). */
data class PhotoRef(val model: Any, val name: String)

/**
 * Full-screen photos: swipe between them, pinch or double-tap to zoom, drag to pan while zoomed (swiping pages
 * pauses then, so a pan never flips the photo). A tap shows or hides the top bar; [actions] go at its end.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PhotoViewer(photos: List<PhotoRef>, start: Int, onClose: () -> Unit, actions: @Composable RowScope.(index: Int) -> Unit = {}) {
    BackHandler(onBack = onClose)
    val pager = rememberPagerState(initialPage = start.coerceIn(0, (photos.size - 1).coerceAtLeast(0))) { photos.size }
    var chrome by remember { mutableStateOf(true) }
    var zoomed by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(pager, Modifier.fillMaxSize(), userScrollEnabled = !zoomed, key = { photos[it].model.toString() }) { page ->
            ZoomableImage(photos[page], onZoomChanged = { if (page == pager.currentPage) zoomed = it }, onTap = { chrome = !chrome })
        }
        AnimatedVisibility(chrome, enter = fadeIn(), exit = fadeOut()) {
            Row(
                Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color(0xCC000000), Color.Transparent)))
                    .statusBarsPadding().padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }
                Column(Modifier.weight(1f)) {
                    Text(photos.getOrNull(pager.currentPage)?.name.orEmpty(), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${pager.currentPage + 1} / ${photos.size}", color = Color(0xB3FFFFFF), fontSize = 12.sp)
                }
                actions(pager.currentPage)
            }
        }
    }
}

@Composable
private fun ZoomableImage(photo: PhotoRef, onZoomChanged: (Boolean) -> Unit, onTap: () -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    fun set(s: Float, o: Offset) {
        scale = s.coerceIn(1f, 5f)
        offset = if (scale == 1f) Offset.Zero else o
        onZoomChanged(scale > 1.01f)
    }
    AsyncImage(
        photo.model, photo.name, contentScale = ContentScale.Fit,
        modifier = Modifier.fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = { p -> if (scale > 1f) set(1f, Offset.Zero) else set(2.5f, (Offset(size.width / 2f, size.height / 2f) - p) * 1.5f) },
                )
            }
            .pointerInput(Unit) {
                // Pinch always zooms; one finger only pans once zoomed in. Otherwise the events are left alone,
                // so the pager still swipes between photos at 1x.
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        if (event.changes.count { it.pressed } >= 2 || scale > 1.01f) {
                            set(scale * event.calculateZoom(), offset + event.calculatePan())
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
            .graphicsLayer { scaleX = scale; scaleY = scale; translationX = offset.x; translationY = offset.y },
    )
}
