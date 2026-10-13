package org.app.nxplayer.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import org.app.nxplayer.R
import org.app.nxplayer.core.Keys
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.app.nxplayer.core.UserData
import org.app.nxplayer.data.AppGraph
import org.app.nxplayer.playback.PlayerHolder
import org.app.nxplayer.ui.ActionRow
import org.app.nxplayer.ui.LocalNav
import org.app.nxplayer.ui.LocalOps
import org.app.nxplayer.ui.Screen
import org.app.nxplayer.ui.SectionHeader
import org.app.nxplayer.ui.SettingGroup
import org.app.nxplayer.ui.SettingRow
import org.app.nxplayer.ui.SettingsPage
import org.app.nxplayer.ui.TopBar
import org.app.nxplayer.ui.tunerTabs

@Composable
fun SettingsScreen() {
    val nav = LocalNav.current
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState())) {
        TopBar("Settings", onBack = { nav.pop() }, divider = true)
        Spacer(Modifier.height(40.dp))
        val icons = mapOf(
            SettingsPage.List to R.drawable.ic_set_list, SettingsPage.Player to R.drawable.ic_set_player, SettingsPage.Decoder to R.drawable.ic_set_decoder,
            SettingsPage.Audio to R.drawable.ic_set_audio, SettingsPage.Subtitle to R.drawable.ic_set_subtitle, SettingsPage.General to R.drawable.ic_set_general,
        )
        SettingsPage.entries.forEach { p -> IndexRow(icons.getValue(p), p.title) { nav.push(Screen.SettingsDetail(p)) } }
        IndexRow(R.drawable.ic_set_development, "Development") {
            nav.push(Screen.Unavailable("Development", "Developer diagnostics (codec logs, crash reporting toggles) are not exposed in this build."))
        }
        val context = LocalContext.current
        IndexRow(R.drawable.ic_set_language, "App Language") {
            val intent = if (android.os.Build.VERSION.SDK_INT >= 33)
                android.content.Intent(android.provider.Settings.ACTION_APP_LOCALE_SETTINGS, android.net.Uri.fromParts("package", context.packageName, null))
            else android.content.Intent(android.provider.Settings.ACTION_LOCALE_SETTINGS)
            runCatching { context.startActivity(intent) }
        }
    }
}

@Composable
fun SettingsPageScreen(title: String, groups: List<SettingGroup>, page: SettingsPage?) {
    val nav = LocalNav.current
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState())) {
        TopBar(title, onBack = { nav.pop() }, divider = true)
        Spacer(Modifier.height(40.dp))
        var firstHeader = true
        when (page) {
            SettingsPage.Player -> {
                SectionHeader("Interface", first = true); firstHeader = false
                ActionRow("Theme", trailing = {
                    Column(Modifier.size(width = 38.dp, height = 30.dp).border(1.dp, Color(0xFF3A4654))) {
                        Box(Modifier.weight(1f).fillMaxWidth().background(Color(AppGraph.settings[Keys.frameColor]).copy(alpha = 1f)))
                        Box(Modifier.weight(1f).fillMaxWidth().background(Color(AppGraph.settings[Keys.progressBarColor])))
                    }
                }) { nav.push(Screen.TunerDetail(0)) }
                ActionRow("Style") { nav.push(Screen.TunerDetail(0)) }
                (1..3).forEach { i -> ActionRow(tunerTabs[i].title, tunerSummary(i)) { nav.push(Screen.TunerDetail(i)) } }
            }
            SettingsPage.Subtitle -> {
                SectionHeader("Appearance")
                ActionRow("Text", "Subtitle text settings: font, size, color, border, etc.") { nav.push(Screen.TunerDetail(4)) }
                ActionRow("Layout", "Subtitle layout settings: alignment, padding, background color.") { nav.push(Screen.TunerDetail(5)) }
            }
            SettingsPage.Decoder -> Text(
                "Decoding uses the platform media codecs. HW prefers hardware decoders; SW prefers software decoders. HW+ (MX's custom hardware path) behaves like HW.",
                Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            else -> {}
        }
        groups.forEachIndexed { i, g ->
            if (page == SettingsPage.Player && i == 0) Unit
            else g.title?.let { SectionHeader(it, first = firstHeader); firstHeader = false }
            g.items.forEach { SettingRow(it) { PlayerHolder.applySettings() } }
        }
        if (page == SettingsPage.General) UserDataActions()
    }
}

@Composable
private fun IndexRow(icon: Int, title: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(48.dp).clickable(onClick = onClick).padding(start = 23.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(icon), null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(9.dp))
        Text(title, fontSize = 16.sp, lineHeight = 19.sp)
    }
}

private fun tunerSummary(i: Int) = when (i) {
    0 -> "Preset, frame, controls and progress bar colours"
    1 -> "Screen settings: Orientation, Full Screen, Brightness, etc."
    2 -> "Input controls: Touch actions, gestures, lock mode, etc."
    else -> "Navigation settings: seeking settings, forward/backward buttons, etc."
}

@Composable
private fun UserDataActions() {
    val ops = LocalOps.current
    val context = LocalContext.current
    var confirm by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val text = AppGraph.settings.raw.export(mapOf("userdata" to AppGraph.userData.value.encode()))
        runCatching { context.contentResolver.openOutputStream(uri)!!.use { it.write(text.toByteArray()) } }
            .onSuccess { ops.toast("Settings exported") }.onFailure { ops.toast("Export failed") }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            val text = context.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() }
            val user = AppGraph.settings.import(text)
            user["userdata"]?.let { raw -> AppGraph.userData.update { UserData.decode(raw) } }
        }.onSuccess { ops.toast("Settings imported") }.onFailure { ops.toast("Import failed") }
    }
    SectionHeader("User data")
    ActionRow("Clear history", "Clear all user activity records, including playback and search history.") {
        confirm = "Clear all history?" to { AppGraph.userData.update { it.clearHistory() } }
    }
    ActionRow("Clear thumbnail cache", "Thumbnails will be generated again when the media list is shown.") {
        AppGraph.media.clearThumbnailCache(); ops.toast("Thumbnail cache cleared")
    }
    ActionRow("Reset settings", "Reset all settings to their default condition.") {
        confirm = "Reset all settings?" to { AppGraph.settings.reset(); PlayerHolder.applySettings() }
    }
    ActionRow("Export", "Export settings and activity records to a file.") { exporter.launch("nxplayer-settings.txt") }
    ActionRow("Import", "Import settings and activity records from a file.") { importer.launch(arrayOf("text/*", "application/octet-stream")) }
    confirm?.let { (msg, action) ->
        org.app.nxplayer.player.PlayerDialog(
            onDismissRequest = { confirm = null },
            text = { Text(msg) },
            confirmButton = { TextButton({ action(); confirm = null }) { Text("OK") } },
            dismissButton = { TextButton({ confirm = null }) { Text("Cancel") } },
        )
    }
}
