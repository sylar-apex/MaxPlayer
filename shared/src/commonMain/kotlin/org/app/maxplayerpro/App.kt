package org.app.maxplayerpro

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.psbapp.navigation.utils.LocalNavHostController
import com.psbapp.navigation.utils.PSBNavHost
import com.psbapp.navigation.utils.push
import org.app.kmediaplayer.KMediaPlayer
import org.app.kmediaplayer.ui.video.ComposePlayerWindow
import org.app.maxplayerpro.appcore.theme.MaxTheme
import org.app.maxplayerpro.navigation.Routes
import org.app.maxplayerpro.navigation.appNavGraph
import org.app.maxplayerpro.player.ui.MaxPlayerContent
import org.app.maxplayerpro.sharedlogic.platform.AppScreen
import org.app.maxplayerpro.sharedlogic.platform.AppScreenRequests

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
        CompositionLocalProvider(LocalNavHostController provides navController) {
            PSBNavHost(navController, startDestination = Routes.Home, isAnimationEnabled = false) {
                appNavGraph(navController)
            }
        }
        LaunchedEffect(Unit) {
            startScreen?.let { navController.open(it) }
            AppScreenRequests.requests.collect { navController.open(it) }
        }
        if (embeddedPlayer) EmbeddedPlayer()
    }
}

/** Shows the library's player over the app while [KMediaPlayer.videoRequested] is set. */
@Composable
private fun EmbeddedPlayer() {
    val requested by KMediaPlayer.videoRequested.collectAsState()
    if (!requested) return
    val scope = rememberCoroutineScope()
    val window = remember { ComposePlayerWindow(scope) { KMediaPlayer.videoRequested.value = false } }
    LaunchedEffect(window) { if (!window.startPending()) KMediaPlayer.videoRequested.value = false }
    MaxPlayerContent(window)
}
