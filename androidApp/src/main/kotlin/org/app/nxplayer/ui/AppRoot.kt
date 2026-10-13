package org.app.nxplayer.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.app.nxplayer.core.Keys
import org.app.nxplayer.core.MediaFile
import org.app.nxplayer.data.AppGraph
import org.app.nxplayer.player.PlayerLauncher
import org.app.nxplayer.ui.library.FolderScreen
import org.app.nxplayer.ui.library.HomeScreen
import org.app.nxplayer.ui.library.SearchScreen
import org.app.nxplayer.ui.me.HelpScreen
import org.app.nxplayer.ui.me.MeScreen
import org.app.nxplayer.ui.me.MediaListScreen
import org.app.nxplayer.ui.me.MediaManagerScreen
import org.app.nxplayer.ui.me.PlaylistDetailScreen
import org.app.nxplayer.ui.me.PlaylistsScreen
import org.app.nxplayer.ui.me.PrivateFolderScreen
import org.app.nxplayer.ui.me.ThemeScreen
import org.app.nxplayer.ui.me.UnavailableScreen
import org.app.nxplayer.ui.music.MusicScreen
import org.app.nxplayer.ui.settings.SettingsPageScreen
import org.app.nxplayer.ui.settings.SettingsScreen

sealed interface Screen {
    data object Home : Screen
    data class Folder(val path: String) : Screen
    data object Search : Screen
    data object Me : Screen
    data object Settings : Screen
    data class SettingsDetail(val page: SettingsPage) : Screen
    data class TunerDetail(val tab: Int) : Screen
    data object Theme : Screen
    data object Playlists : Screen
    data class PlaylistDetail(val id: Long) : Screen
    data object Private : Screen
    data object MediaManager : Screen
    data class MediaList(val kind: String) : Screen
    data object Music : Screen
    data object Help : Screen
    data object Networks : Screen
    data class Unavailable(val title: String, val reason: String) : Screen
}

class Navigator(private val stack: MutableList<Screen>) {
    val current: Screen get() = stack.last()
    fun push(s: Screen) { stack += s }
    fun pop(): Boolean = if (stack.size > 1) { stack.removeAt(stack.lastIndex); true } else false
}

/** File operations that may need a system consent dialog on Android 11+. */
class MediaOps(
    private val context: Context,
    private val scope: CoroutineScope,
    private val requestConsent: (android.content.IntentSender, suspend () -> Unit) -> Unit,
) {
    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()

    fun play(files: List<MediaFile>, index: Int) = PlayerLauncher.play(context, files, index)

    fun delete(files: List<MediaFile>, after: () -> Unit = {}) {
        if (!AppGraph.settings[Keys.allowEdits]) { toast("Editing is disabled in Settings → General"); return }
        val subs = AppGraph.settings[Keys.deleteSubtitlesTogether]
        val sender = AppGraph.media.deleteRequest(files)
        if (sender == null) scope.launch { AppGraph.media.deleteNow(files, subs); after() }
        else requestConsent(sender) {
            if (subs) files.forEach { f -> AppGraph.media.subtitleFilesFor(f).forEach { it.delete() } }
            files.forEach { f -> AppGraph.userData.update { it.forgetFile(f.uri) } }
            AppGraph.media.refresh(); after()
        }
    }

    fun rename(file: MediaFile, newName: String) {
        if (!AppGraph.settings[Keys.allowEdits]) { toast("Editing is disabled in Settings → General"); return }
        val name = if ('.' in newName) newName else "$newName.${file.extension}"
        suspend fun doIt() { if (!AppGraph.media.renameNow(file, name)) toast("Rename failed") }
        val sender = AppGraph.media.writeRequest(listOf(file))
        if (sender == null) scope.launch { doIt() } else requestConsent(sender) { doIt() }
    }

    fun lockInPrivate(files: List<MediaFile>) {
        if (!AppGraph.privateFolder.hasPin) { toast("Set up the Private Folder first (Me → Private Folder)"); return }
        scope.launch {
            val copied = files.filter { AppGraph.privateFolder.lockIn(it) }
            if (copied.isEmpty()) { toast("Could not move file"); return@launch }
            val sender = AppGraph.media.deleteRequest(copied)
            if (sender == null) AppGraph.media.deleteNow(copied, false)
            else requestConsent(sender) { copied.forEach { f -> AppGraph.userData.update { it.forgetFile(f.uri) } }; AppGraph.media.refresh() }
            toast("${copied.size} file(s) locked in Private Folder")
        }
    }

    fun share(files: List<MediaFile>) {
        val uris = ArrayList(files.map { Uri.parse(it.uri) })
        val intent = if (uris.size == 1) Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, uris[0])
        else Intent(Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        intent.type = if (files.all { it.isAudio }) "audio/*" else "video/*"
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(intent, "Share"))
    }
}

val LocalNav = staticCompositionLocalOf<Navigator> { error("no navigator") }
val LocalOps = staticCompositionLocalOf<MediaOps> { error("no ops") }

@Composable
fun AppRoot(open: String? = null) {
    val stack = remember {
        mutableStateListOf<Screen>(Screen.Home).apply {
            when (open) { "settings" -> add(Screen.Settings); "help" -> add(Screen.Help) }
        }
    }
    val nav = remember { Navigator(stack) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pending = remember { arrayOfNulls<suspend () -> Unit>(1) }
    val consent = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val action = pending[0]
        pending[0] = null
        if (result.resultCode == Activity.RESULT_OK && action != null) scope.launch { action() }
    }
    val ops = remember {
        MediaOps(context, scope) { sender, after ->
            pending[0] = after
            consent.launch(IntentSenderRequest.Builder(sender).build())
        }
    }
    BackHandler(enabled = stack.size > 1) { nav.pop() }

    CompositionLocalProvider(LocalNav provides nav, LocalOps provides ops) {
        when (val s = stack.last()) {
            Screen.Home -> HomeScreen()
            is Screen.Folder -> FolderScreen(s.path)
            Screen.Search -> SearchScreen()
            Screen.Me -> MeScreen()
            Screen.Settings -> SettingsScreen()
            is Screen.SettingsDetail -> SettingsPageScreen(s.page.title, settingsPages.getValue(s.page), s.page)
            is Screen.TunerDetail -> SettingsPageScreen(tunerTabs[s.tab].title, tunerTabs[s.tab].groups, null)
            Screen.Theme -> ThemeScreen()
            Screen.Playlists -> PlaylistsScreen()
            is Screen.PlaylistDetail -> PlaylistDetailScreen(s.id)
            Screen.Private -> PrivateFolderScreen()
            Screen.MediaManager -> MediaManagerScreen()
            is Screen.MediaList -> MediaListScreen(s.kind)
            Screen.Music -> MusicScreen()
            Screen.Help -> HelpScreen()
            Screen.Networks -> org.app.nxplayer.ui.me.NetworksScreen()
            is Screen.Unavailable -> UnavailableScreen(s.title, s.reason)
        }
    }
}
