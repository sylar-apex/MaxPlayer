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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.blur
import androidx.compose.foundation.layout.size
import coil3.PlatformContext
import coil3.SingletonImageLoader
import androidx.compose.material3.TextButton
import kotlinx.coroutines.delay
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.memory.MemoryCache
import androidx.compose.runtime.mutableStateMapOf
import io.ktor.client.HttpClient
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import io.ktor.utils.io.readRemaining
import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import androidx.compose.runtime.mutableStateListOf
import coil3.request.crossfade

/**
 * A photo for [PhotoViewer]: [model] is whatever the image loader opens (file URI, content URI or http URL);
 * [thumb] is a small version already shown in a list, put up blurred at once while [model] loads.
 */
data class PhotoRef(val model: Any, val name: String, val thumb: Any? = null)

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
    // Full photos load one at a time: the one you stop on first, then the next, then the previous. Swiping on
    // cancels whatever was loading, so fast swiping never starts a pile of downloads. Finished photos stay in the
    // image cache, so going back to one is instant.
    val context = LocalPlatformContext.current
    val done = remember { mutableStateListOf<Int>() }
    // Download progress (0..1) of network photos, shown in the water loader.
    val progress = remember { mutableStateMapOf<Int, Float>() }
    val failed = remember { mutableStateListOf<Int>() }
    var retries by remember { mutableStateOf(0) }
    LaunchedEffect(pager.settledPage, retries) {
        val here = pager.settledPage
        // Only once you stay on a photo; passing through doesn't count.
        delay(250)
        val loader = SingletonImageLoader.get(context)
        for (i in listOf(here, here + 1, here - 1)) {
            if (i !in photos.indices || i in done || i in failed) continue
            var tries = 0
            while (true) {
                if (load(context, photos[i]) { progress[i] = it }) { done += i; break }
                // A busy network: try again a little later, then leave it for a tap.
                if (++tries >= 3) { failed += i; break }
                delay(800L * tries)
            }
        }
    }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(pager, Modifier.fillMaxSize(), userScrollEnabled = !zoomed, key = { photos[it].model.toString() }) { page ->
            ZoomableImage(photos[page], ready = page in done, failed = page in failed, progress = progress[page], onRetry = { failed -= page; retries++ },
                onZoomChanged = { if (page == pager.currentPage) zoomed = it }, onTap = { chrome = !chrome })
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
private fun ZoomableImage(photo: PhotoRef, ready: Boolean, failed: Boolean, progress: Float?, onRetry: () -> Unit, onZoomChanged: (Boolean) -> Unit, onTap: () -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    fun set(s: Float, o: Offset) {
        scale = s.coerceIn(1f, 5f)
        offset = if (scale == 1f) Offset.Zero else o
        onZoomChanged(scale > 1.01f)
    }
    val context = LocalPlatformContext.current
    var shown by remember { mutableStateOf(false) }
    Box(
        Modifier.fillMaxSize()
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
        contentAlignment = Alignment.Center,
    ) {
        // The list's thumbnail, softened, the moment the photo opens; the full photo fades in over it.
        if (!shown && photo.thumb != null) AsyncImage(photo.thumb, null, Modifier.fillMaxSize().blur(8.dp), contentScale = ContentScale.Fit)
        // Loaded by the viewer's queue; here it comes straight from the cache.
        if (ready) AsyncImage(fullRequest(context, photo), photo.name, Modifier.fillMaxSize(), contentScale = ContentScale.Fit, onSuccess = { shown = true })
        when {
            shown -> {}
            failed -> TextButton(onRetry) { Text("Couldn't load · Tap to retry", color = Color.White) }
            progress != null -> WaterLoader(progress)
            else -> CircularProgressIndicator(Modifier.size(36.dp), color = Color.White, strokeWidth = 3.dp)
        }
    }
}

/**
 * The full photo, decoded to at most [FULL_PX] on its long side: sharp on any screen and when zoomed, without
 * decoding every pixel of a 50-megapixel file. The queue and the page use the same request, so they share the cache.
 */
private fun fullRequest(context: PlatformContext, photo: PhotoRef): ImageRequest =
    ImageRequest.Builder(context).data(photo.model).memoryCacheKey(cacheKey(photo)).size(FULL_PX).crossfade(true).build()

private fun cacheKey(photo: PhotoRef) = "viewer:${photo.model}"

/** Shared by the viewer's downloads; photos from other devices come over plain HTTP. */
private val viewerHttp by lazy { HttpClient() }

/**
 * Loads [photo] into the image cache. A network photo is downloaded here so [onProgress] can report how much has
 * arrived, then decoded under the same cache key the page uses; anything else goes straight to the image loader.
 */
private suspend fun load(context: PlatformContext, photo: PhotoRef, onProgress: (Float) -> Unit): Boolean {
    val loader = SingletonImageLoader.get(context)
    val url = (photo.model as? String)?.takeIf { it.startsWith("http") }
        ?: return loader.execute(fullRequest(context, photo)) is SuccessResult
    // Already decoded earlier (going back to a photo): no second download.
    if (loader.memoryCache?.get(MemoryCache.Key(cacheKey(photo))) != null) return true
    val bytes = runCatching {
        viewerHttp.prepareGet(url).execute { response ->
            if (!response.status.isSuccess()) return@execute null
            val total = response.contentLength() ?: 0L
            val channel = response.bodyAsChannel()
            val out = Buffer()
            onProgress(0f)
            while (!channel.isClosedForRead) {
                val chunk = channel.readRemaining(64 * 1024L)
                out.write(chunk.readByteArray())
                // The download is the first 95%; decoding the photo is the rest, so 100% means it is on screen.
                if (total > 0) onProgress(out.size.toFloat() / total * DOWNLOAD_SHARE)
            }
            out.readByteArray()
        }
    }.getOrNull() ?: return false
    val request = ImageRequest.Builder(context).data(bytes).memoryCacheKey(cacheKey(photo)).size(FULL_PX).build()
    return (loader.execute(request) is SuccessResult).also { if (it) onProgress(1f) }
}

private const val FULL_PX = 2560
private const val DOWNLOAD_SHARE = 0.95f
