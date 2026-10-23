package org.app.maxplayerpro

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.coroutines.launch
import androidx.navigation.NavDestination.Companion.hasRoute
import com.psbapp.navigation.utils.push
import org.koin.compose.koinInject
import org.app.maxplayerpro.appcore.adaptive.LocalTitleBarArea
import org.app.maxplayerpro.appcore.adaptive.LocalTitleBarHost
import org.app.maxplayerpro.appcore.adaptive.TitleBar
import org.app.maxplayerpro.appcore.adaptive.TitleBarHeight
import org.app.maxplayerpro.appcore.adaptive.TitleBarHost
import org.app.maxplayerpro.sharedlogic.data.MediaRepository
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.psbapp.navigation.utils.LocalNavHostController
import com.psbapp.navigation.utils.PSBNavHost
import com.psbapp.navigation.utils.push
import org.app.kmediaplayer.KMediaPlayer
import org.app.kmediaplayer.ui.video.ComposePlayerWindow
import org.app.maxplayerpro.appcore.adaptive.LocalWindowClass
import org.app.maxplayerpro.appcore.adaptive.ProvideWindowClass
import org.app.maxplayerpro.appcore.theme.MaxTheme
import org.app.maxplayerpro.navigation.Routes
import org.app.maxplayerpro.navigation.NavHistory
import org.app.maxplayerpro.navigation.Sidebar
import org.app.maxplayerpro.navigation.appNavGraph
import org.app.maxplayerpro.player.ui.MaxPlayerContent
import org.app.maxplayerpro.sharedlogic.platform.AppScreen
import org.app.maxplayerpro.sharedlogic.platform.AppScreenRequests
import org.app.maxplayerpro.sharedlogic.usecase.NetworkShareRequests
import org.app.maxplayerpro.localnetwork.share.SharingController
import org.app.maxplayerpro.localnetwork.ui.NetworkShareDialog

private fun NavController.open(screen: AppScreen) = when (screen) {
    AppScreen.Settings -> push(Routes.Settings)
    AppScreen.Help -> push(Routes.Help)
}

/**
 * The main window. [startScreen] opens on top of Home (e.g. Settings requested from the player).
 * [embeddedPlayer] shows the video player inside this window, for platforms without a separate player screen.
 */
@Composable
fun App(startScreen: AppScreen? = null, embeddedPlayer: Boolean = false) {
    MaxTheme {
        val navController = rememberNavController()
        ProvideWindowClass {
            val wide = LocalWindowClass.current.isWide
            // Every wide window (macOS, Windows, Linux, browser) gets the same bar across the top; on macOS it sits in the
            // title bar beside the traffic lights. Phones and tablets keep in-page headers everywhere.
            val inTitle = wide
            val titleBar = remember { TitleBarHost() }
            if (inTitle) {
                val media = koinInject<MediaRepository>()
                val refreshing by media.loading.collectAsState()
                val scope = rememberCoroutineScope()
                val entry by navController.currentBackStackEntryAsState()
                val history = remember(navController) { NavHistory(navController) }
                // A page without a WideHeader must not keep the previous page's title.
                LaunchedEffect(entry?.id) {
                    titleBar.clear(); history.onPageChanged()
                    // Leaving the results drops the search, so the box is empty on other pages.
                    if (entry?.destination?.hasRoute<Routes.Search>() == false) titleBar.query = ""
                }
                val canBack = entry != null && navController.previousBackStackEntry != null
                LocalTitleBarArea.current {
                    TitleBar(
                        titleBar, refreshing, onRefresh = { scope.launch { media.refresh() } },
                        onBack = history::back.takeIf { canBack }, onForward = history::forward.takeIf { history.canGoForward },
                        onSearchTyped = { if (navController.currentDestination?.hasRoute<Routes.Search>() != true) navController.push(Routes.Search) },
                    )
                }
            }
            Row(Modifier.fillMaxSize().padding(top = if (inTitle) TitleBarHeight else 0.dp).background(MaterialTheme.colorScheme.background)) {
                // Laptops, desktops and wide browser windows get a permanent sidebar; phones keep the chips and profile page.
                if (wide) Sidebar(navController)
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    CompositionLocalProvider(LocalNavHostController provides navController, LocalTitleBarHost provides titleBar.takeIf { inTitle }) {
                        PSBNavHost(navController, startDestination = Routes.Home, isAnimationEnabled = false) {
                            appNavGraph(navController)
                        }
                    }
                }
            }
        }
        LaunchedEffect(Unit) {
            startScreen?.let { navController.open(it) }
            AppScreenRequests.requests.collect { navController.open(it) }
        }
        NetworkShareHost()
        if (embeddedPlayer) EmbeddedPlayer()
    }
}

/** Resumes network sharing left on, and shows the share dialog when a list asks to share files on the network. */
@Composable
private fun NetworkShareHost() {
    val sharing = koinInject<SharingController>()
    LaunchedEffect(sharing) { sharing.resume() }
    val pending by NetworkShareRequests.pending.collectAsState()
    pending?.let { files -> NetworkShareDialog(files, onDismiss = NetworkShareRequests::dismiss) }
}

/** Shows the library's player over the app while [KMediaPlayer.videoRequested] is set. */
@Composable
private fun EmbeddedPlayer() {
    val requested by KMediaPlayer.videoRequested.collectAsState()
    if (!requested) return
    // First open after launch: libVLC may still be loading. Show the player with a loader instead of freezing the window.
    val ready by KMediaPlayer.engineReady.collectAsState()
    if (ready) ReadyPlayer() else PlayerInitialising()
}

@Composable
private fun ReadyPlayer() {
    val scope = rememberCoroutineScope()
    val window = remember { ComposePlayerWindow(scope) { KMediaPlayer.videoRequested.value = false } }
    LaunchedEffect(window) { if (!window.startPending()) KMediaPlayer.videoRequested.value = false }
    MaxPlayerContent(window)
}

@Composable
private fun PlayerInitialising() {
    Box(
        // Swallows clicks so the library underneath stays untouched while the player gets ready.
        Modifier.fillMaxSize().background(Color.Black).pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(Modifier.size(28.dp), color = Color.White, strokeWidth = 2.5.dp)
            Spacer(Modifier.height(14.dp))
            Text("Initialising player…", color = Color.White, fontSize = 14.sp)
            Spacer(Modifier.height(4.dp))
            Text("Loading video codecs for the first time", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
        }
    }
}
