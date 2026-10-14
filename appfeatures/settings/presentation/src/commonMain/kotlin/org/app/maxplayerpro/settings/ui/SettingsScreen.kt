package org.app.maxplayerpro.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.app.kmediaplayer.KMediaPlayer
import org.app.kmediaplayer.settings.PlayerKeys
import org.app.kmediaplayer.settings.SettingGroup
import org.app.kmediaplayer.settings.tunerTabs
import org.app.kmediaplayer.ui.common.PlayerDialog
import org.app.kmediaplayer.ui.settings.ActionRow
import org.app.kmediaplayer.ui.settings.SectionHeader
import org.app.kmediaplayer.ui.settings.SettingRow
import org.app.maxplayerpro.appcore.component.TopBar
import org.app.maxplayerpro.appcore.theme.MaxColors
import org.app.maxplayerpro.appres.resources.Res
import org.app.maxplayerpro.appres.resources.ic_set_audio
import org.app.maxplayerpro.appres.resources.ic_set_decoder
import org.app.maxplayerpro.appres.resources.ic_set_development
import org.app.maxplayerpro.appres.resources.ic_set_general
import org.app.maxplayerpro.appres.resources.ic_set_language
import org.app.maxplayerpro.appres.resources.ic_set_list
import org.app.maxplayerpro.appres.resources.ic_set_player
import org.app.maxplayerpro.appres.resources.ic_set_subtitle
import org.app.maxplayerpro.settings.domain.model.SettingsPage
import org.app.maxplayerpro.settings.domain.model.settingsPages
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

/** Settings index: one row per settings page plus Development and App Language. */
@Composable
fun SettingsScreen(onBack: () -> Unit, onPage: (SettingsPage) -> Unit, onDevelopment: () -> Unit, viewModel: SettingsViewModel = koinViewModel()) {
    val icons = mapOf(
        SettingsPage.List to Res.drawable.ic_set_list, SettingsPage.Player to Res.drawable.ic_set_player, SettingsPage.Decoder to Res.drawable.ic_set_decoder,
        SettingsPage.Audio to Res.drawable.ic_set_audio, SettingsPage.Subtitle to Res.drawable.ic_set_subtitle, SettingsPage.General to Res.drawable.ic_set_general,
    )
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState())) {
        TopBar("Settings", onBack = onBack, divider = true)
        Spacer(Modifier.height(40.dp))
        SettingsPage.entries.forEach { p -> IndexRow(icons.getValue(p), p.title) { onPage(p) } }
        IndexRow(Res.drawable.ic_set_development, "Development", onDevelopment)
        IndexRow(Res.drawable.ic_set_language, "App Language") { viewModel.setEvent(SettingsContract.Event.OpenLanguageSettings) }
    }
}

/** One settings page, or one Display Settings tuner tab when [tunerTab] is set. */
@Composable
fun SettingsPageScreen(
    page: SettingsPage?,
    tunerTab: Int?,
    onBack: () -> Unit,
    onTuner: (Int) -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val title = page?.title ?: tunerTabs[tunerTab ?: 0].title
    val groups = page?.let { settingsPages.getValue(it) } ?: tunerTabs[tunerTab ?: 0].groups
    SettingsPageContent(state, title, groups, page, onBack, onTuner, onEvent = viewModel::setEvent)
}

@Composable
fun SettingsPageContent(
    state: SettingsContract.State,
    title: String,
    groups: List<SettingGroup>,
    page: SettingsPage?,
    onBack: () -> Unit,
    onTuner: (Int) -> Unit,
    onEvent: (SettingsContract.Event) -> Unit,
) {
    state.version
    val settings = KMediaPlayer.settings
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState())) {
        TopBar(title, onBack = onBack, divider = true)
        Spacer(Modifier.height(40.dp))
        var firstHeader = true
        when (page) {
            SettingsPage.Player -> {
                SectionHeader("Interface", first = true); firstHeader = false
                ActionRow("Theme", trailing = {
                    Column(Modifier.size(width = 38.dp, height = 30.dp).border(1.dp, MaxColors.swatchBorder)) {
                        Box(Modifier.weight(1f).fillMaxWidth().background(Color(settings[PlayerKeys.frameColor]).copy(alpha = 1f)))
                        Box(Modifier.weight(1f).fillMaxWidth().background(Color(settings[PlayerKeys.progressBarColor])))
                    }
                }) { onTuner(0) }
                ActionRow("Style") { onTuner(0) }
                (1..3).forEach { i -> ActionRow(tunerTabs[i].title, tunerSummary(i)) { onTuner(i) } }
            }
            SettingsPage.Subtitle -> {
                SectionHeader("Appearance")
                ActionRow("Text", "Subtitle text settings: font, size, color, border, etc.") { onTuner(4) }
                ActionRow("Layout", "Subtitle layout settings: alignment, padding, background color.") { onTuner(5) }
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
            g.items.forEach { SettingRow(it) { onEvent(SettingsContract.Event.SettingChanged) } }
        }
        if (page == SettingsPage.General) UserDataActions(onEvent)
    }
}

@Composable
private fun IndexRow(icon: DrawableResource, title: String, onClick: () -> Unit) {
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
private fun UserDataActions(onEvent: (SettingsContract.Event) -> Unit) {
    var confirm by remember { mutableStateOf<Pair<String, SettingsContract.Event>?>(null) }
    SectionHeader("User data")
    ActionRow("Clear history", "Clear all user activity records, including playback and search history.") {
        confirm = "Clear all history?" to SettingsContract.Event.ClearHistory
    }
    ActionRow("Clear thumbnail cache", "Thumbnails will be generated again when the media list is shown.") {
        onEvent(SettingsContract.Event.ClearThumbnailCache)
    }
    ActionRow("Reset settings", "Reset all settings to their default condition.") {
        confirm = "Reset all settings?" to SettingsContract.Event.ResetSettings
    }
    ActionRow("Export", "Export settings and activity records to a file.") { onEvent(SettingsContract.Event.Export) }
    ActionRow("Import", "Import settings and activity records from a file.") { onEvent(SettingsContract.Event.Import) }
    confirm?.let { (msg, event) ->
        PlayerDialog(
            onDismissRequest = { confirm = null },
            text = { Text(msg) },
            confirmButton = { TextButton({ onEvent(event); confirm = null }) { Text("OK") } },
            dismissButton = { TextButton({ confirm = null }) { Text("Cancel") } },
        )
    }
}
