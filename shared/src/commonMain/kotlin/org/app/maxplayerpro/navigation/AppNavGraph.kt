package org.app.maxplayerpro.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.psbapp.navigation.utils.pop
import com.psbapp.navigation.utils.push
import org.app.maxplayerpro.appcore.component.InfoScreen
import org.app.maxplayerpro.folder.ui.FolderScreen
import org.app.maxplayerpro.help.ui.HelpScreen
import org.app.maxplayerpro.home.ui.HomeNavigation
import org.app.maxplayerpro.home.ui.HomeScreen
import org.app.maxplayerpro.localnetwork.ui.LocalNetworkScreen
import org.app.maxplayerpro.mediamanager.domain.model.MediaListKind
import org.app.maxplayerpro.mediamanager.ui.MediaListScreen
import org.app.maxplayerpro.mediamanager.ui.MediaManagerScreen
import org.app.maxplayerpro.music.ui.MusicScreen
import org.app.maxplayerpro.playlists.ui.PlaylistDetailScreen
import org.app.maxplayerpro.playlists.ui.PlaylistsScreen
import org.app.maxplayerpro.privatefolder.ui.PrivateFolderScreen
import org.app.maxplayerpro.profile.ui.ProfileNavigation
import org.app.maxplayerpro.profile.ui.ProfileScreen
import org.app.maxplayerpro.search.ui.SearchScreen
import org.app.maxplayerpro.settings.domain.model.SettingsPage
import org.app.maxplayerpro.settings.ui.SettingsPageScreen
import org.app.maxplayerpro.settings.ui.SettingsScreen
import org.app.maxplayerpro.theme.ui.ThemeScreen

private const val FILE_TRANSFER_REASON = "Peer-to-peer transfer uses MX's proprietary MXShare protocol and is not implemented."

fun NavGraphBuilder.appNavGraph(navController: NavController) {
    val back: () -> Unit = { navController.pop() }

    composable<Routes.Home> {
        HomeScreen(
            HomeNavigation(
                onSearch = { navController.push(Routes.Search) },
                onMe = { navController.push(Routes.Me) },
                onFolder = { navController.push(Routes.Folder(it)) },
                onMusic = { navController.push(Routes.Music) },
                onPrivacy = { navController.push(Routes.PrivateFolder) },
                onFileTransfer = { navController.push(Routes.Unavailable("File Transfer", FILE_TRANSFER_REASON)) },
                onPlaylists = { navController.push(Routes.Playlists) },
                onNetwork = { navController.push(Routes.LocalNetwork) },
            ),
        )
    }
    composable<Routes.Folder> { entry ->
        FolderScreen(entry.toRoute<Routes.Folder>().path, onBack = back, onSearch = { navController.push(Routes.Search) })
    }
    composable<Routes.Search> { SearchScreen(onBack = back) }
    composable<Routes.Music> { MusicScreen(onBack = back) }
    composable<Routes.Me> {
        ProfileScreen(
            ProfileNavigation(
                onBack = back,
                onPrivateFolder = { navController.push(Routes.PrivateFolder) },
                onPlaylists = { navController.push(Routes.Playlists) },
                onLocalNetwork = { navController.push(Routes.LocalNetwork) },
                onMediaManager = { navController.push(Routes.MediaManager) },
                onTheme = { navController.push(Routes.Theme) },
                onSettings = { navController.push(Routes.Settings) },
                onHelp = { navController.push(Routes.Help) },
                onUnavailable = { title, reason -> navController.push(Routes.Unavailable(title, reason)) },
            ),
        )
    }
    composable<Routes.Theme> { ThemeScreen(onBack = back) }
    composable<Routes.Settings> {
        SettingsScreen(
            onBack = back,
            onPage = { navController.push(Routes.SettingsPage(it.name)) },
            onDevelopment = { navController.push(Routes.Unavailable("Development", "Developer diagnostics (codec logs, crash reporting toggles) are not exposed in this build.")) },
        )
    }
    composable<Routes.SettingsPage> { entry ->
        SettingsPageScreen(SettingsPage.valueOf(entry.toRoute<Routes.SettingsPage>().page), null, onBack = back, onTuner = { navController.push(Routes.Tuner(it)) })
    }
    composable<Routes.Tuner> { entry ->
        SettingsPageScreen(null, entry.toRoute<Routes.Tuner>().tab, onBack = back, onTuner = { navController.push(Routes.Tuner(it)) })
    }
    composable<Routes.Playlists> { PlaylistsScreen(onClose = back, onPlaylist = { navController.push(Routes.PlaylistDetail(it)) }) }
    composable<Routes.PlaylistDetail> { entry -> PlaylistDetailScreen(entry.toRoute<Routes.PlaylistDetail>().id, onBack = back) }
    composable<Routes.PrivateFolder> { PrivateFolderScreen(onBack = back) }
    composable<Routes.MediaManager> { MediaManagerScreen(onBack = back, onList = { navController.push(Routes.MediaList(it.name)) }) }
    composable<Routes.MediaList> { entry -> MediaListScreen(MediaListKind.valueOf(entry.toRoute<Routes.MediaList>().kind), onBack = back) }
    composable<Routes.Help> { HelpScreen(onBack = back) }
    composable<Routes.LocalNetwork> { LocalNetworkScreen(onBack = back) }
    composable<Routes.Unavailable> { entry ->
        val route = entry.toRoute<Routes.Unavailable>()
        InfoScreen(route.title, route.reason, onBack = back)
    }
}
