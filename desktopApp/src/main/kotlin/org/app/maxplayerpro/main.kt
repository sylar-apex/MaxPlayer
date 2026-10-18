package org.app.maxplayerpro

import androidx.compose.foundation.background
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import org.app.maxplayerpro.appcore.adaptive.LocalInTitleBar
import org.app.maxplayerpro.appcore.adaptive.LocalTitleBarArea
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import org.app.maxplayerpro.appcore.theme.MaxTheme
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import org.app.kmediaplayer.ui.video.PlatformVideoTools
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.window.application
import org.app.kmediaplayer.KMediaPlayer
import org.app.kmediaplayer.desktop.DesktopPlayback
import org.app.kmediaplayer.model.MediaFile
import org.app.maxplayerpro.di.initApp
import org.app.maxplayerpro.sharedlogic.data.DesktopMediaRepository
import org.app.maxplayerpro.sharedlogic.data.MediaRepository
import org.koin.mp.KoinPlatform
import java.io.File

/** Files given on the command line (or by "Open with") start playing straight away. */
fun main(args: Array<String>) {
    // Load libVLC off the UI thread so the window never waits for it.
    Thread({ DesktopPlayback.engine }, "libvlc-init").apply { isDaemon = true }.start()
    runApp(args)
}

private fun runApp(args: Array<String>) = application {
    initApp()
    val files = args.map(::File).filter { it.isFile }.map { it.toMediaFile() }
    if (files.isNotEmpty()) KMediaPlayer.playVideo(files, 0)
    val windowState = rememberWindowState(size = DpSize(1360.dp, 860.dp), position = WindowPosition(Alignment.Center))
    // The player's rotate button toggles full screen on desktop.
    PlatformVideoTools.fullscreenToggle = {
        windowState.placement = if (windowState.placement == WindowPlacement.Fullscreen) WindowPlacement.Floating else WindowPlacement.Fullscreen
    }
    Window(
        onCloseRequest = ::exitApplication,
        title = "Max Player Pro",
        state = windowState,
    ) {
        // Keeps the player's full-screen button in sync, including the green window button and Esc.
        LaunchedEffect(windowState) {
            snapshotFlow { windowState.placement }.collect { PlatformVideoTools.onFullscreenChanged(it == WindowPlacement.Fullscreen) }
        }
        // Scan the home folder and every drive, then rescan whenever a drive is plugged in or removed.
        LaunchedEffect(Unit) {
            val library = KoinPlatform.getKoin().get<MediaRepository>() as DesktopMediaRepository
            library.refresh()
            library.watchDrives()
        }
        // macOS: draw the app under a transparent title bar so it matches the theme instead of a white strip.
        val mac = remember { System.getProperty("os.name").orEmpty().startsWith("Mac") }
        LaunchedEffect(mac) {
            if (mac) window.rootPane.apply {
                putClientProperty("apple.awt.fullWindowContent", true)
                putClientProperty("apple.awt.transparentTitleBar", true)
                putClientProperty("apple.awt.windowTitleVisible", false)
            }
        }
        MaxTheme {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                // macOS: the app draws its own title bar (page title, back, search, refresh); dragging it moves the window.
                CompositionLocalProvider(
                    LocalInTitleBar provides mac,
                    LocalTitleBarArea provides { content -> WindowDraggableArea { content() } },
                ) { App(embeddedPlayer = true) }
            }
        }
    }
}

private fun File.toMediaFile() = MediaFile(path.hashCode().toLong(), toURI().toString(), path, name, length(), 0, lastModified())
