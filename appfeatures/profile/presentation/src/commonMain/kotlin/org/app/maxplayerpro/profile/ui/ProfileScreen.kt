package org.app.maxplayerpro.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.app.kmediaplayer.ui.common.NetworkStreamDialog
import org.app.maxplayerpro.appcore.theme.MaxColors
import org.app.maxplayerpro.appres.resources.Res
import org.app.maxplayerpro.appres.resources.ic_app_theme
import org.app.maxplayerpro.appres.resources.ic_back
import org.app.maxplayerpro.appres.resources.ic_cloud_drive
import org.app.maxplayerpro.appres.resources.ic_file_transfer
import org.app.maxplayerpro.appres.resources.ic_help
import org.app.maxplayerpro.appres.resources.ic_legal
import org.app.maxplayerpro.appres.resources.ic_local_network
import org.app.maxplayerpro.appres.resources.ic_media_manager
import org.app.maxplayerpro.appres.resources.ic_network_stream
import org.app.maxplayerpro.appres.resources.ic_private_folder
import org.app.maxplayerpro.appres.resources.ic_quit
import org.app.maxplayerpro.appres.resources.ic_settings
import org.app.maxplayerpro.appres.resources.ic_status_saver
import org.app.maxplayerpro.appres.resources.ic_video_playlists
import org.app.maxplayerpro.profile.ui.component.MeCard
import org.app.maxplayerpro.profile.ui.component.MeRow
import org.app.maxplayerpro.profile.ui.component.Tile
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

/** Where each Me-page entry leads. [onUnavailable] shows an explanation page for features not built yet. */
class ProfileNavigation(
    val onBack: () -> Unit,
    val onPrivateFolder: () -> Unit,
    val onPlaylists: () -> Unit,
    val onLocalNetwork: () -> Unit,
    val onMediaManager: () -> Unit,
    val onTheme: () -> Unit,
    val onSettings: () -> Unit,
    val onHelp: () -> Unit,
    val onUnavailable: (title: String, reason: String) -> Unit,
)

@Composable
fun ProfileScreen(navigation: ProfileNavigation, viewModel: ProfileViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ProfileContent(state, navigation, onEvent = viewModel::setEvent)
}

@Composable
fun ProfileContent(state: ProfileContract.State, navigation: ProfileNavigation, onEvent: (ProfileContract.Event) -> Unit) {
    var networkStream by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(MaxColors.mePage).verticalScroll(rememberScrollState())) {
        Row(Modifier.statusBarsPadding().height(50.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(navigation.onBack, Modifier.padding(start = 1.dp)) { Icon(painterResource(Res.drawable.ic_back), "Navigate up") }
        }
        MeCard(Modifier.padding(top = 4.dp)) {
            val tiles = listOf<Triple<DrawableResource, String, () -> Unit>>(
                Triple(Res.drawable.ic_file_transfer, "File Transfer") { navigation.onUnavailable("File Transfer", "MXShare peer-to-peer transfer (Send/Receive, share with Jio/iOS/PC/Tablet) uses MX's proprietary protocol and is not implemented.") },
                Triple(Res.drawable.ic_private_folder, "Private Folder", navigation.onPrivateFolder),
                Triple(Res.drawable.ic_video_playlists, "Video Playlists", navigation.onPlaylists),
                Triple(Res.drawable.ic_network_stream, "Network Stream") { networkStream = true },
                Triple(Res.drawable.ic_local_network, "Local Network", navigation.onLocalNetwork),
                Triple(Res.drawable.ic_media_manager, "Media Manager", navigation.onMediaManager),
                Triple(Res.drawable.ic_cloud_drive, "Cloud Drive") { navigation.onUnavailable("Cloud Drive", "Google Drive, Dropbox and OneDrive need OAuth client registrations for this app; not implemented.") },
            )
            Column(Modifier.padding(start = 9.5.dp, end = 9.5.dp, top = 6.dp, bottom = 6.dp)) {
                tiles.chunked(3).forEach { row ->
                    Row {
                        row.forEach { (icon, label, action) -> Tile(icon, label, Modifier.weight(1f), action) }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
        MeCard(Modifier.padding(top = 8.dp)) {
            MeRow(Res.drawable.ic_status_saver, "WhatsApp Status Saver", height = 52, tint = MaxColors.whatsApp) {
                navigation.onUnavailable("WhatsApp Status Saver", "Saving statuses needs access to WhatsApp's private media folder, which Android only grants through a per-folder SAF permission; not implemented.")
            }
        }
        MeCard(Modifier.padding(top = 9.dp), vertical = 3) {
            MeRow(Res.drawable.ic_app_theme, "App Theme", onClick = navigation.onTheme)
            MeRow(Res.drawable.ic_settings, "Settings", onClick = navigation.onSettings)
        }
        MeCard(Modifier.padding(top = 10.dp), vertical = 3) {
            MeRow(Res.drawable.ic_legal, "Legal") { navigation.onUnavailable("Legal", "MaxPlayerPro — an independent media player. Implements features equivalent to the reference player without using its code or assets.") }
            MeRow(Res.drawable.ic_help, "Help", onClick = navigation.onHelp)
            if (state.showQuit) MeRow(Res.drawable.ic_quit, "Quit") { onEvent(ProfileContract.Event.Quit) }
        }
    }
    if (networkStream) NetworkStreamDialog { networkStream = false }
}
