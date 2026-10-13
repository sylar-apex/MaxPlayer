package org.app.nxplayer.player

import android.provider.OpenableColumns
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import org.app.nxplayer.R
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import org.app.nxplayer.core.AspectRatio
import org.app.nxplayer.core.Keys
import org.app.nxplayer.core.Setting
import org.app.nxplayer.core.BoolSetting
import org.app.nxplayer.core.ColorSetting
import org.app.nxplayer.core.ChoiceSetting
import org.app.nxplayer.core.Shortcut
import org.app.nxplayer.core.SleepTimerInput
import org.app.nxplayer.core.Speed
import org.app.nxplayer.core.formatDuration
import org.app.nxplayer.core.formatSize
import org.app.nxplayer.data.AppGraph
import org.app.nxplayer.playback.AudioEffects
import org.app.nxplayer.playback.Decoder
import org.app.nxplayer.playback.PlayerHolder
import org.app.nxplayer.ui.SettingRow
import org.app.nxplayer.ui.tunerTabs
import java.text.DateFormat
import java.util.Date

private val PanelBg = Color(0xE61A1F26)
private val Dim = Color(0xFF9AA6B2)

@Composable
fun PanelHost(activity: PlayerActivity) {
    val st = activity.state
    if (st.panel == Panel.Decoder) { DecoderDialog(st); return }
    if (st.panel == Panel.AbRepeat) { AbRepeatCard(st); return }
    val widthFraction = when (st.panel) { Panel.More -> 0.28f; Panel.Display -> 0.57f; Panel.Equalizer -> 0.5f; Panel.Speed -> 1f; else -> 0.46f }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val portrait = maxHeight > maxWidth
        if (st.panel == Panel.Speed) { SpeedPanel(st, Modifier.align(Alignment.BottomCenter)); return@BoxWithConstraints }
        Box(Modifier.fillMaxSize().clickable(interactionSource = null, indication = null) { st.panel = Panel.None })
        val overflow = st.panel != Panel.Display
        val mod = if (portrait) Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(0.6f)
        else Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(when { st.panel == Panel.Equalizer -> 367.dp; st.panel == Panel.More -> 220.dp; overflow -> 360.dp; st.panel == Panel.Display -> 448.dp; else -> maxWidth * widthFraction })
        val bg = if (overflow) Modifier.background(androidx.compose.ui.graphics.Brush.horizontalGradient(0f to Color(0x66000000), 0.2f to Color(0xB3000000), 1f to Color(0xCC000000)))
        else if (st.panel == Panel.Display) Modifier.background(Color(0x99000000)) else Modifier.background(PanelBg)
        Column(mod.then(bg).let { if (overflow && !portrait) it else it.safeDrawingPadding() }.clickable(interactionSource = null, indication = null) {}
            .let { if (overflow) it.padding(start = 24.dp, end = 24.dp, top = if (st.panel == Panel.Overflow) 24.dp else 16.dp) else if (st.panel == Panel.Display) it.padding(start = 24.dp, end = 24.dp) else it.padding(horizontal = 20.dp, vertical = 12.dp) }) {
            when (st.panel) {
                Panel.Overflow -> OverflowPanel(activity)
                Panel.Audio -> AudioPanel(st)
                Panel.Subtitle -> SubtitlePanel(st)
                Panel.Equalizer -> EqualizerPanel()
                Panel.SleepTimer -> SleepTimerPanel(st)
                Panel.Aspect -> AspectPanel(st)
                Panel.Info -> InfoPanel(st)
                Panel.Bookmark -> BookmarkPanel(st)
                Panel.More -> MorePanel(activity)
                Panel.Display -> DisplayPanel(activity)
                Panel.Playlist -> PlaylistPanel(st)
                Panel.Customise -> CustomisePanel()
                else -> {}
            }
        }
    }
}

@Composable
private fun PanelTitle(text: String, onBack: (() -> Unit)? = null, trailing: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().height(22.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) Box(Modifier.layout { m, c -> val p = m.measure(c); layout(0, p.height) { p.place(-36.dp.roundToPx(), 0) } }) {
            Icon(painterResource(R.drawable.ic_back), "Back", Modifier.size(24.dp).clickable(onClick = onBack), tint = Color.White)
        }
        Text(text, Modifier.weight(1f), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        trailing()
    }
}

private val MxBlue: Color get() = PlayerAccent
private val Disabled = Color(0x73FFFFFF)

@Composable
private fun PanelRow(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(42.dp).clickable(enabled = enabled, onClick = onClick), contentAlignment = Alignment.CenterStart) {
        Text(text, color = if (enabled) Color.White else Disabled, fontSize = 14.sp)
    }
}

@Composable
private fun PanelDivider() = Box(Modifier.padding(vertical = 8.dp).fillMaxWidth().height(1.dp).background(Color(0x26FFFFFF)))

@Composable
private fun RadioRow(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(46.dp).clickable(onClick = onClick), verticalAlignment = Alignment.CenterVertically) {
        PanelRadio(selected); Spacer(Modifier.width(4.dp))
        Text(text, color = Color.White, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun CheckRow(text: String, checked: Boolean, enabled: Boolean = true, height: androidx.compose.ui.unit.Dp = 46.dp, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(height).clickable(enabled = enabled, onClick = onClick), verticalAlignment = Alignment.CenterVertically) {
        PanelCheck(checked); Spacer(Modifier.width(4.dp))
        Text(text, color = if (enabled) Color.White else Disabled, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun PanelRadio(selected: Boolean) {
    Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(20.dp).border(2.dp, if (selected) MxBlue else Color(0xFFBFC5CC), androidx.compose.foundation.shape.CircleShape), contentAlignment = Alignment.Center) {
            if (selected) Box(Modifier.size(10.dp).background(MxBlue, androidx.compose.foundation.shape.CircleShape))
        }
    }
}

@Composable
private fun PanelCheck(checked: Boolean) {
    Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(18.dp).background(if (checked) MxBlue else Color.Transparent, RoundedCornerShape(2.dp)).border(2.dp, if (checked) MxBlue else Color(0xFF3F5470), RoundedCornerShape(2.dp)), contentAlignment = Alignment.Center) {
            if (checked) Icon(painterResource(R.drawable.ic_check), null, Modifier.size(14.dp), tint = Color(0xFF101820))
        }
    }
}

@Composable
private fun Section(text: String) = Text(text, Modifier.padding(top = 10.dp, bottom = 2.dp), color = Dim, fontSize = 14.sp)

@Composable
private fun Scroll(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), content = content)
}

@Composable
private fun OverflowPanel(activity: PlayerActivity) {
    val st = activity.state
    var networkStream by remember { mutableStateOf(false) }
    var tutorial by remember { mutableStateOf(false) }
    var videoOn by remember { mutableStateOf(AppGraph.settings[Keys.videoDisplay]) }
    var shortcutsOn by remember { mutableStateOf(AppGraph.settings[Keys.shortcutsVisible]) }
    val ffSpeed = AppGraph.settings[Keys.longPressSpeed]
    data class Item(val label: String, val icon: Int?, val text: String? = null, val action: () -> Unit)
    val items = listOf(
        Item("Aspect Ratio", R.drawable.ic_ov_aspect) { st.panel = Panel.Aspect },
        Item("Speed FF\n(Long Press)", null, "%.1fX".format(ffSpeed)) {
            val next = listOf(1.5f, 2f, 3f, 4f).let { l -> l[(l.indexOf(ffSpeed) + 1) % l.size] }
            AppGraph.settings[Keys.longPressSpeed] = next
        },
        Item("Display Settings", R.drawable.ic_ov_display) { st.panel = Panel.Display },
        Item("Playlists", R.drawable.ic_ov_playlist) { st.panel = Panel.Playlist },
        Item("Network Stream", R.drawable.ic_ov_network) { networkStream = true },
        Item("Information", R.drawable.ic_ov_info) { st.panel = Panel.Info },
        Item("Share", R.drawable.ic_ov_share) { activity.shareCurrent() },
        Item("Cut", R.drawable.ic_ov_cut) { st.toast = "Video cutting is not available yet" },
        Item("Bookmark", R.drawable.ic_ov_bookmark) { st.panel = Panel.Bookmark },
        Item("Tutorial", R.drawable.ic_ov_tutorial) { tutorial = true },
        Item("More", R.drawable.ic_chevron_right) { st.panel = Panel.More },
    )
    Column(Modifier.verticalScroll(rememberScrollState())) {
        items.chunked(4).forEach { row ->
            Row {
                row.forEach { item ->
                    Column(Modifier.size(78.dp).clickable(onClick = item.action).padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(42.dp).border(1.dp, Color(0x59FFFFFF), androidx.compose.foundation.shape.CircleShape), contentAlignment = Alignment.Center) {
                            if (item.icon != null) Icon(painterResource(item.icon), null, Modifier.size(22.dp), tint = Color.White)
                            else Text(item.text.orEmpty(), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(item.label, Modifier.padding(top = 3.dp).width(72.dp), color = Color.White, fontSize = 10.sp, lineHeight = 12.sp, textAlign = TextAlign.Center, maxLines = 2)
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        SwitchRow("Video Display", videoOn) { videoOn = it; AppGraph.settings[Keys.videoDisplay] = it; if (!it) st.player.clearVideoSurface() else activity.textureView?.let { tv -> st.player.setVideoTextureView(tv) } }
        Spacer(Modifier.height(16.dp))
        SwitchRow("Shortcuts", shortcutsOn) { shortcutsOn = it; AppGraph.settings[Keys.shortcutsVisible] = it }
    }
    if (networkStream) org.app.nxplayer.ui.me.NetworkStreamDialog { networkStream = false }
    if (tutorial) PlayerDialog(
        onDismissRequest = { tutorial = false },
        containerColor = Color(0xFF2B3640),
        title = { Text("Gestures", color = Color.White) },
        text = {
            Text(
                "• Swipe left/right: seek\n• Swipe up/down on the left: brightness\n• Swipe up/down on the right: volume\n" +
                    "• Double tap sides: rewind / forward\n• Pinch: zoom, drag with two fingers: pan\n• Long press: fast forward at ${Speed.label(ffSpeed)}\n• Drag subtitles up/down to move them",
                color = Color(0xFFC3CAD3), fontSize = 14.sp, lineHeight = 22.sp,
            )
        },
        confirmButton = { TextButton({ tutorial = false }) { Text("OK") } },
    )
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().height(40.dp).clickable { onChange(!checked) }, verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), color = Color.White, fontSize = 15.sp)
        org.app.nxplayer.ui.MxSwitch(checked, onChange)
    }
}

@Composable
private fun AudioPanel(st: PlayerState) {
    val opener = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        st.toast = "External audio tracks are not supported by this player"
    }
    PanelTitle("Audio Track")
    Scroll {
        st.audioTracks.forEach { t -> RadioRow(t.label, t.selected) { st.selectTrack(t, C.TRACK_TYPE_AUDIO) } }
        RadioRow("Disable", st.audioTracks.none { it.selected }) { st.disableAudio() }
        CheckRow("Use SW audio decoder", st.decoder == Decoder.SW, height = 42.dp) { st.setDecoderMode(if (st.decoder == Decoder.SW) Decoder.HW else Decoder.SW) }
        PanelDivider()
        PanelRow("Open") { opener.launch(arrayOf("audio/*")) }
        PanelRow("Stereo mode", enabled = false) {}
        PanelRow("Synchronization", enabled = false) {}
        Row(Modifier.fillMaxWidth().height(42.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("AV sync", Modifier.weight(1f), color = Disabled, fontSize = 14.sp)
            Box(Modifier.size(20.dp).border(2.dp, Disabled, RoundedCornerShape(2.dp)))
            Spacer(Modifier.width(2.dp))
        }
    }
}

@Composable
private fun SubtitlePanel(st: PlayerState) {
    val context = LocalContext.current
    val opener = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val name = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c -> if (c.moveToFirst()) c.getString(0) else null } ?: "subtitle.srt"
        st.loadSubtitle(uri, name)
    }
    PanelTitle("Subtitle")
    Scroll {
        st.externalSubsName?.let { n -> CheckRow(n, true) { st.disableSubtitles() } }
        st.textTracks.forEach { t ->
            val on = t.selected && !st.subtitlesDisabled && st.externalSubs == null
            CheckRow(t.label, on) { if (on) st.disableSubtitles() else { st.externalSubs = null; st.externalSubsName = null; st.selectTrack(t, C.TRACK_TYPE_TEXT) } }
        }
        if (st.textTracks.isEmpty() && st.externalSubs == null) PanelRow("No subtitles", enabled = false) {}
        PanelDivider()
        PanelRow("Open") { opener.launch(arrayOf("*/*")) }
        PanelRow("Online subtitles", enabled = false) {}
        PanelRow("AI Subtitle Translation", enabled = false) {}
        Stepper("Synchronization", "${"%.1f".format(st.subtitleSyncMs / 1000f)}s", enabled = st.externalSubs != null,
            onMinus = { st.setSubtitleSync(st.subtitleSyncMs - 100) }, onPlus = { st.setSubtitleSync(st.subtitleSyncMs + 100) })
        Stepper("Speed", "${"%.1f".format(st.subtitleSpeed * 100)}%", enabled = st.externalSubs != null,
            onMinus = { st.subtitleSpeed = (st.subtitleSpeed - 0.001f).coerceAtLeast(0.5f) }, onPlus = { st.subtitleSpeed = (st.subtitleSpeed + 0.001f).coerceAtMost(2f) })
        PanelRow("Panel") { st.panel = Panel.Display }
    }
}

@Composable
private fun Stepper(label: String, value: String, enabled: Boolean = true, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(42.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), color = Color.White, fontSize = 14.sp)
        RoundStep(R.drawable.ic_sp_minus, "Decrease", enabled, onMinus)
        Box(Modifier.padding(horizontal = 8.dp).size(104.dp, 30.dp).background(Color(0x14FFFFFF)), contentAlignment = Alignment.Center) {
            Text(value, color = Color.White, fontSize = 14.sp)
        }
        RoundStep(R.drawable.ic_sp_plus, "Increase", enabled, onPlus)
    }
}

@Composable
private fun DecoderDialog(st: PlayerState) {
    PlayerDialog(
        onDismissRequest = { st.panel = Panel.None },
        title = { Text("Select decoder") },
        text = {
            Column {
                Decoder.entries.forEach { d ->
                    Row(Modifier.fillMaxWidth().height(48.dp).clickable { st.setDecoderMode(d); st.panel = Panel.None }, verticalAlignment = Alignment.CenterVertically) {
                        PanelRadio(d == st.decoder); Spacer(Modifier.width(16.dp))
                        Text("${d.label} decoder", color = Color.White, fontSize = 17.sp)
                    }
                }
            }
        },
        confirmButton = {},
        buttons = false,
    )
}

@Composable
private fun SpeedPanel(st: PlayerState, modifier: Modifier) {
    val accent = PlayerAccent
    var editing by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }
    Box(Modifier.fillMaxSize().clickable(interactionSource = null, indication = null) { st.panel = Panel.None })
    Column(
        modifier.windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.safeDrawing.only(androidx.compose.foundation.layout.WindowInsetsSides.Bottom)).padding(bottom = 17.dp, start = 16.dp, end = 16.dp).widthIn(max = 586.dp).fillMaxWidth()
            .background(Color(0xB3141414), RoundedCornerShape(6.dp)).clickable(interactionSource = null, indication = null) {}
            .padding(top = 10.dp, bottom = 6.dp),
    ) {
        Row(Modifier.fillMaxWidth().height(40.dp).padding(start = 16.dp, end = 40.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundStep(R.drawable.ic_sp_minus, "Decrease") { st.setSpeed(Speed.decrease(st.speed)) }
            Spacer(Modifier.weight(1f))
            Row(Modifier.width(120.dp).fillMaxHeight().background(Color(0x14FFFFFF), RoundedCornerShape(4.dp)).clickable { draft = "%.2f".format(st.speed); editing = true },
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Text("%.2fx".format(st.speed), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Light)
                Spacer(Modifier.width(6.dp))
                Icon(painterResource(R.drawable.ic_sp_edit), "Edit", Modifier.size(18.dp), tint = Color.White)
            }
            Spacer(Modifier.weight(1f))
            RoundStep(R.drawable.ic_sp_plus, "Increase") { st.setSpeed(Speed.increase(st.speed)) }
        }
        Row(Modifier.fillMaxWidth().height(30.dp).padding(start = 15.dp, end = 15.dp), verticalAlignment = Alignment.CenterVertically) {
            SpeedTrack(st.speed, accent, Modifier.weight(1f).fillMaxHeight()) { st.setSpeed(it) }
            Spacer(Modifier.width(8.dp))
            Icon(painterResource(R.drawable.ic_sp_reset), "Reset", Modifier.size(24.dp).clickable { st.setSpeed(1f) }, tint = Color.White)
        }
        BoxWithConstraints(Modifier.fillMaxWidth().height(23.dp).padding(start = 15.dp, end = 47.dp)) {
            val track = maxWidth - 32.dp
            listOf(0.25f, 1f, 2f, 3f, 4f).forEach { v ->
                val x = 16.dp + track * ((v - Speed.MIN) / (Speed.MAX - Speed.MIN))
                Text(if (v == 0.25f) "0.25x" else "${v}x", Modifier.layout { m, c ->
                    val p = m.measure(c.copy(minWidth = 0)); layout(p.width, p.height) { p.place(x.roundToPx() - p.width / 2, 0) }
                }.clickable { st.setSpeed(v) }, color = if (st.speed == v) accent else Color.White, fontSize = 12.sp)
            }
        }
    }
    if (editing) PlayerDialog(
        onDismissRequest = { editing = false },
        containerColor = Color(0xFF2B3640),
        text = {
            androidx.compose.material3.OutlinedTextField(draft, { draft = it }, singleLine = true, suffix = { Text("x") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal))
        },
        confirmButton = { TextButton({ draft.toFloatOrNull()?.let { st.setSpeed(it.coerceIn(Speed.MIN, Speed.MAX)) }; editing = false }) { Text("OK") } },
        dismissButton = { TextButton({ editing = false }) { Text("CANCEL") } },
    )
}

@Composable
private fun RoundStep(icon: Int, desc: String, enabled: Boolean = true, onClick: () -> Unit) {
    Box(Modifier.size(30.dp).background(Color(0x33FFFFFF), androidx.compose.foundation.shape.CircleShape).clickable(enabled = enabled, onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(painterResource(icon), desc, Modifier.size(16.dp), tint = Color(0xFFDADDE4))
    }
}

@Composable
private fun SpeedTrack(speed: Float, color: Color, modifier: Modifier, onChange: (Float) -> Unit) {
    val inset = with(androidx.compose.ui.platform.LocalDensity.current) { 16.dp.toPx() }
    fun value(x: Float, w: Int): Float {
        val raw = Speed.MIN + ((x - inset) / (w - 2 * inset)).coerceIn(0f, 1f) * (Speed.MAX - Speed.MIN)
        return (Math.round(raw * 20) / 20f).coerceIn(Speed.MIN, Speed.MAX)
    }
    androidx.compose.foundation.Canvas(
        modifier.pointerInput(Unit) { detectTapGestures { onChange(value(it.x, size.width)) } }
            .pointerInput(Unit) { detectHorizontalDragGestures { change, _ -> change.consume(); onChange(value(change.position.x, size.width)) } },
    ) {
        val y = size.height / 2
        val w = size.width - 2 * inset
        val frac = (speed - Speed.MIN) / (Speed.MAX - Speed.MIN)
        drawLine(Color(0x4DFFFFFF), androidx.compose.ui.geometry.Offset(inset, y), androidx.compose.ui.geometry.Offset(inset + w, y), 2.dp.toPx())
        drawLine(color, androidx.compose.ui.geometry.Offset(inset, y), androidx.compose.ui.geometry.Offset(inset + w * frac, y), 2.dp.toPx())
        var tick = 0.5f
        while (tick < Speed.MAX) {
            drawCircle(Color.White, 2.dp.toPx(), androidx.compose.ui.geometry.Offset(inset + w * (tick - Speed.MIN) / (Speed.MAX - Speed.MIN), y)); tick += 0.5f
        }
        drawCircle(Color.White, 7.dp.toPx(), androidx.compose.ui.geometry.Offset(inset + w * frac, y))
    }
}

@Composable
internal fun EqualizerPanel() {
    val fx = PlayerHolder.effects()
    var on by remember { mutableStateOf(AppGraph.settings[Keys.equalizerEnabled]) }
    var preset by remember { mutableIntStateOf(AppGraph.settings[Keys.equalizerPreset]) }
    var version by remember { mutableIntStateOf(0) }
    val fg = if (on) Color.White else Color(0x99FFFFFF)
    val active = if (on) MxBlue else Color(0xFF4A7BA8)
    PanelTitle("Equalizer") {
        Text(if (on) "On" else "Off", color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(end = 8.dp))
        org.app.nxplayer.ui.MxSwitch(on, { on = it; AppGraph.settings[Keys.equalizerEnabled] = it; fx?.applySaved() }, color = MxBlue)
    }
    if (fx == null) { Text("Equalizer is not available for this audio output.", color = Dim, fontSize = 14.sp, modifier = Modifier.padding(top = 12.dp)); return }
    val eq = fx.equalizer
    Row(Modifier.padding(top = 13.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val names = listOf("Custom") + (0 until eq.numberOfPresets).map { eq.getPresetName(it.toShort()) }
        names.forEachIndexed { i, n ->
            val id = i - 1
            Box(
                Modifier.height(28.dp).border(1.dp, if (preset == id) active else Color(0x66FFFFFF), RoundedCornerShape(50))
                    .clickable { preset = id; AppGraph.settings[Keys.equalizerPreset] = id; if (id >= 0) runCatching { eq.usePreset(id.toShort()) }; version++ }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) { Text(n, color = if (preset == id) active else fg, fontSize = 11.5.sp) }
        }
    }
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        val range = eq.bandLevelRange
        version
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            (0 until eq.numberOfBands).forEach { b ->
                var level by remember(version, b) { mutableFloatStateOf(eq.getBandLevel(b.toShort()).toFloat()) }
                Column(Modifier.width(60.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${Math.round(level / 100)} dB", color = fg, fontSize = 10.sp)
                    Spacer(Modifier.height(3.dp))
                    VerticalSlider(level, range[0].toFloat()..range[1].toFloat(), enabled = on, color = active) {
                        level = it
                        runCatching { eq.setBandLevel(b.toShort(), it.toInt().toShort()) }
                        preset = -1
                        AppGraph.settings[Keys.equalizerPreset] = -1
                        AppGraph.settings[Keys.equalizerBands] = (0 until eq.numberOfBands).joinToString(",") { i -> eq.getBandLevel(i.toShort()).toString() }
                    }
                    Spacer(Modifier.height(2.dp))
                    val hz = eq.getCenterFreq(b.toShort()) / 1000
                    Text("$hz Hz", color = fg, fontSize = 10.sp, maxLines = 1, softWrap = false)
                }
            }
        }
        fx.reverb?.let {
            var reverb by remember { mutableIntStateOf(AppGraph.settings[Keys.reverb]) }
            var open by remember { mutableStateOf(false) }
            Row(Modifier.fillMaxWidth().height(40.dp).clickable(enabled = on) { open = true }, verticalAlignment = Alignment.CenterVertically) {
                Text("Reverb", Modifier.width(216.dp), color = fg, fontSize = 14.5.sp)
                Box(Modifier.weight(1f)) {
                    Text(AudioEffects.reverbNames[reverb], color = fg, fontSize = 14.sp)
                    androidx.compose.material3.DropdownMenu(open, { open = false }, containerColor = Color(0xFF2B3640)) {
                        AudioEffects.reverbNames.forEachIndexed { idx, name ->
                            androidx.compose.material3.DropdownMenuItem({ Text(name, color = Color.White) }, {
                                reverb = idx; open = false; AppGraph.settings[Keys.reverb] = idx; fx.applySaved()
                            })
                        }
                    }
                }
                Icon(painterResource(R.drawable.ic_chevron_down), null, Modifier.size(16.dp), tint = fg)
            }
        }
        fx.bassBoost?.let {
            var bass by remember { mutableFloatStateOf(AppGraph.settings[Keys.bassBoost].toFloat()) }
            Row(Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Bass Boost", color = fg, fontSize = 14.5.sp)
                MxHSlider(bass / 1000f, active, on, Modifier.weight(1f).fillMaxHeight(), onChange = { bass = it * 1000f },
                    onFinished = { AppGraph.settings[Keys.bassBoost] = bass.toInt(); fx.applySaved() })
                Text("${(bass / 10).toInt()}%", color = fg, fontSize = 12.sp, modifier = Modifier.width(32.dp), textAlign = TextAlign.End)
            }
        }
    }
}

@Composable
private fun VerticalSlider(value: Float, range: ClosedFloatingPointRange<Float>, enabled: Boolean, color: Color, onChange: (Float) -> Unit) {
    val span = range.endInclusive - range.start
    fun at(y: Float, h: Int, pad: Float) = range.endInclusive - ((y - pad) / (h - 2 * pad)).coerceIn(0f, 1f) * span
    val pad = with(androidx.compose.ui.platform.LocalDensity.current) { 7.dp.toPx() }
    androidx.compose.foundation.Canvas(
        Modifier.size(28.dp, 102.dp)
            .pointerInput(enabled) { if (enabled) detectTapGestures { onChange(at(it.y, size.height, pad)) } }
            .pointerInput(enabled) { if (enabled) detectVerticalDragGestures { c, _ -> c.consume(); onChange(at(c.position.y, size.height, pad)) } },
    ) {
        val x = size.width / 2
        val y = pad + (size.height - 2 * pad) * (1 - (value - range.start) / span)
        drawLine(color.copy(alpha = 0.6f), androidx.compose.ui.geometry.Offset(x, pad), androidx.compose.ui.geometry.Offset(x, size.height - pad), 2.dp.toPx())
        drawCircle(color, 7.dp.toPx(), androidx.compose.ui.geometry.Offset(x, y))
    }
}

@Composable
private fun MxHSlider(value: Float, color: Color, enabled: Boolean, modifier: Modifier, onChange: (Float) -> Unit, onFinished: () -> Unit) {
    val pad = with(androidx.compose.ui.platform.LocalDensity.current) { 12.dp.toPx() }
    fun at(x: Float, w: Int) = ((x - pad) / (w - 2 * pad)).coerceIn(0f, 1f)
    androidx.compose.foundation.Canvas(
        modifier.pointerInput(enabled) { if (enabled) detectTapGestures { onChange(at(it.x, size.width)); onFinished() } }
            .pointerInput(enabled) { if (enabled) detectHorizontalDragGestures(onDragEnd = onFinished) { c, _ -> c.consume(); onChange(at(c.position.x, size.width)) } },
    ) {
        val y = size.height / 2
        val x = pad + (size.width - 2 * pad) * value
        drawLine(color.copy(alpha = 0.45f), androidx.compose.ui.geometry.Offset(pad, y), androidx.compose.ui.geometry.Offset(size.width - pad, y), 2.dp.toPx())
        drawLine(color, androidx.compose.ui.geometry.Offset(pad, y), androidx.compose.ui.geometry.Offset(x, y), 2.dp.toPx())
        drawCircle(color, 7.dp.toPx(), androidx.compose.ui.geometry.Offset(x, y))
    }
}

@Composable
private fun SleepTimerPanel(st: PlayerState) {
    var input by remember { mutableStateOf(SleepTimerInput()) }
    var finishLast by remember { mutableStateOf(st.sleepFinishLast) }
    PanelTitle("Sleep Timer")
    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(62.dp)) {
            val running = st.sleepEndAt?.let { ((it - System.currentTimeMillis()) / 60000).coerceAtLeast(0) }
            val h = running?.let { (it / 60).toInt() } ?: input.hours
            val m = running?.let { (it % 60).toInt() } ?: input.minutes
            Row(Modifier.align(Alignment.TopCenter).offset(y = 6.dp), verticalAlignment = Alignment.Bottom) {
                Text("$h", color = Color.White, fontSize = 44.sp, lineHeight = 50.sp)
                Text("h ", color = Color.White, fontSize = 20.sp, modifier = Modifier.padding(bottom = 6.dp))
                Text(m.toString().padStart(2, '0'), color = Color.White, fontSize = 44.sp, lineHeight = 50.sp)
                Text("m", color = Color.White, fontSize = 20.sp, modifier = Modifier.padding(bottom = 6.dp))
            }
            Box(Modifier.align(Alignment.TopEnd).offset(y = 18.7.dp).size(24.dp).background(Color(0x59FFFFFF), CircleShape).clickable { input = input.clear() }, contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_close), "Clear", Modifier.size(15.dp), tint = Color.White)
            }
            Box(Modifier.align(Alignment.TopStart).offset(y = 57.dp).fillMaxWidth().height(1.dp).background(Color(0x33FFFFFF)))
        }
        listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9), listOf(null, 0, null)).forEach { row ->
            Row(Modifier.fillMaxWidth().height(46.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                row.forEach { d ->
                    Box(Modifier.width(70.dp).fillMaxHeight(), contentAlignment = Alignment.Center) {
                        if (d != null) Box(Modifier.size(30.dp).background(Color(0x26FFFFFF), CircleShape).clickable { input = input.press(d) }, contentAlignment = Alignment.Center) {
                            Text("$d", color = Color.White, fontSize = 18.sp)
                        }
                    }
                }
            }
        }
        CheckRow("Play last media to the end", finishLast, enabled = input.isValid || st.sleepEndAt != null, height = 32.dp) { finishLast = !finishLast }
        Row(Modifier.fillMaxWidth().height(30.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(62.dp).fillMaxHeight().clickable { st.stopSleepTimer(); st.panel = Panel.None }, contentAlignment = Alignment.Center) {
                Text("STOP", color = MxBlue, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.width(12.dp))
            Box(Modifier.width(62.dp).fillMaxHeight().clickable(enabled = input.isValid) { st.startSleepTimer(input.totalMinutes, finishLast); st.panel = Panel.None }, contentAlignment = Alignment.Center) {
                Text("START", color = if (input.isValid) MxBlue else Disabled, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun AbRepeatCard(st: PlayerState) {
    Box(Modifier.fillMaxSize().safeDrawingPadding()) {
        Column(Modifier.align(Alignment.TopEnd).padding(top = 106.dp, end = 4.dp).width(233.dp).background(Color(0xCC161616), RoundedCornerShape(4.dp)).border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(4.dp)).padding(bottom = 12.dp)) {
            Row(Modifier.fillMaxWidth().height(42.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("A - B Repeat", Modifier.weight(1f), color = Color.White, fontSize = 14.sp)
                Box(Modifier.size(42.dp).clickable { st.ab = st.ab.clear(); st.panel = Panel.None }, contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_close), "Close", Modifier.size(18.dp), tint = Color.White)
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                AbButton("A", st.ab.a, Modifier.width(88.dp)) { st.ab = st.ab.setA(st.player.currentPosition) }
                Icon(painterResource(R.drawable.ic_pl_zoom), null, Modifier.weight(1f).size(20.dp), tint = Color(0x59FFFFFF))
                AbButton("B", st.ab.b, Modifier.width(88.dp), enabled = st.ab.a != null) {
                    val next = st.ab.setB(st.player.currentPosition)
                    if (next == st.ab) st.toast = "B must be after A" else { st.ab = next; st.player.seekTo(next.a!!); st.panel = Panel.None }
                }
            }
        }
    }
}

@Composable
private fun AbButton(label: String, value: Long?, modifier: Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Box(modifier.height(48.dp).background(Color(0x26FFFFFF), RoundedCornerShape(2.dp)).clickable(enabled = enabled, onClick = onClick), contentAlignment = Alignment.Center) {
        Text(value?.let { "$label  ${formatDuration(it)}" } ?: label, color = if (enabled) Color.White else Dim, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AspectPanel(st: PlayerState) {
    var applyAll by remember { mutableStateOf(false) }
    var custom by remember { mutableFloatStateOf(st.customAspect) }
    PanelTitle("Aspect Ratio", onBack = { st.panel = Panel.Overflow })
    Scroll {
        Spacer(Modifier.height(7.dp))
        AspectRatio.entries.forEach { a -> RadioRow(a.label, st.aspect == a) { st.setAspectRatio(a, applyAll) } }
        if (st.aspect == AspectRatio.Custom) {
            Text("Custom ${"%.2f".format(custom)}:1", color = Color.White, fontSize = 14.sp)
            MxHSlider((custom - 0.5f) / 2.5f, MxBlue, true, Modifier.fillMaxWidth().height(36.dp), onChange = { custom = 0.5f + it * 2.5f; st.customAspect = custom }, onFinished = {})
        }
        CheckRow("Apply to all videos", applyAll) { applyAll = !applyAll; st.setAspectRatio(st.aspect, applyAll) }
    }
}

@Composable
private fun InfoPanel(st: PlayerState) {
    val f = st.current
    PanelTitle("Information", onBack = { st.panel = Panel.Overflow })
    Scroll {
        Spacer(Modifier.height(22.dp))
        InfoSection("File")
        if (f != null) {
            InfoRow("File", f.name); InfoRow("Location", f.folderPath)
            if (f.sizeBytes > 0) InfoRow("Size", "${android.text.format.Formatter.formatShortFileSize(AppGraph.app, f.sizeBytes)} (${java.text.NumberFormat.getInstance().format(f.sizeBytes)} bytes)")
            if (f.dateModifiedMs > 0) InfoRow("Date", java.text.SimpleDateFormat("d MMMM yyyy 'at' HH:mm", java.util.Locale.getDefault()).format(Date(f.dateModifiedMs)))
            InfoRow("Subtitle", st.externalSubsName ?: if (st.textTracks.isNotEmpty()) "Embedded subtitle" else "None")
        }
        InfoSection("Media")
        st.player.mediaMetadata.title?.toString()?.takeIf { f == null || (it != f.title && it != f.name && it != f.uri.substringAfterLast('/')) }?.let { InfoRow("Title", it) }
        (st.player.videoFormat?.containerMimeType ?: st.player.audioFormat?.containerMimeType)?.let { InfoRow("Format", containerLabel(it)) }
        InfoRow("Length", formatDuration(st.duration))
        InfoRow("Decoder", "${st.decoder.label} decoder")
        st.player.videoFormat?.let { v ->
            InfoSection("Video")
            InfoRow("Codec", v.codecs ?: v.sampleMimeType ?: "—")
            InfoRow("Resolution", "${v.width} x ${v.height}")
            if (v.frameRate > 0) InfoRow("Frame rate", "%.3f fps".format(v.frameRate))
            if (v.bitrate > 0) InfoRow("Bitrate", "${v.bitrate / 1000} kbps")
        }
        st.player.audioFormat?.let { a ->
            InfoSection("Audio")
            InfoRow("Codec", a.codecs ?: a.sampleMimeType ?: "—")
            InfoRow("Channels", "${a.channelCount}")
            InfoRow("Sample rate", "${a.sampleRate} Hz")
            a.language?.let { InfoRow("Language", java.util.Locale(it).displayLanguage) }
        }
    }
}

private fun containerLabel(mime: String) = when {
    "matroska" in mime || "webm" in mime -> "Matroska / WebM"
    "mp4" in mime -> "MPEG-4"
    "avi" in mime -> "AVI"
    "mp2t" in mime -> "MPEG-TS"
    else -> mime.substringAfter('/')
}

@Composable
private fun InfoSection(text: String) = Text(text, Modifier.padding(top = 4.dp, bottom = 4.dp), color = Color.White, fontSize = 13.sp, lineHeight = 16.sp)

@Composable
private fun InfoRow(k: String, v: String) {
    Row(Modifier.fillMaxWidth().heightIn(min = 33.3.dp).padding(bottom = 4.4.dp)) {
        Text(k, Modifier.width(96.dp).padding(top = 5.7.dp), color = Color(0xB3FFFFFF), fontSize = 13.sp, lineHeight = 17.sp)
        Text(v, color = Color.White, fontSize = 14.sp, lineHeight = 16.7.sp)
    }
}

@Composable
private fun BookmarkPanel(st: PlayerState) {
    val user by AppGraph.userData.data.collectAsState()
    val f = st.current
    val marks = f?.let { user.bookmarks[it.uri] }.orEmpty()
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null || f == null) return@rememberLauncherForActivityResult
        val text = runCatching { AppGraph.app.contentResolver.openInputStream(uri)!!.bufferedReader().readText() }.getOrDefault("")
        val times = Regex("""(\d{1,2}):(\d{2}):(\d{2})|(\d{1,3}):(\d{2})""").findAll(text).map { m ->
            val g = m.groupValues
            if (g[1].isNotEmpty()) (g[1].toLong() * 3600 + g[2].toLong() * 60 + g[3].toLong()) * 1000 else (g[4].toLong() * 60 + g[5].toLong()) * 1000
        }.filter { it in 0..st.duration }.toList()
        AppGraph.userData.update { d -> times.fold(d) { acc, t -> acc.addBookmark(f.uri, t, System.currentTimeMillis()) } }
        st.toast = if (times.isEmpty()) "No bookmarks found in file" else "${times.size} bookmarks imported"
    }
    PanelTitle("Bookmark", onBack = { st.panel = Panel.Overflow })
    Column(Modifier.fillMaxWidth().fillMaxHeight()) {
        if (marks.isEmpty()) {
            Column(Modifier.fillMaxWidth().weight(1f).offset(y = 35.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("No bookmarks yet.", color = Color.White, fontSize = 14.sp)
                Spacer(Modifier.height(24.dp))
                Row(Modifier.size(181.dp, 36.dp).border(1.dp, Color(0x99FFFFFF), RoundedCornerShape(2.dp)).clickable { importer.launch(arrayOf("text/*")) },
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.ic_bm_import), null, Modifier.size(16.dp), tint = Color.White)
                    Spacer(Modifier.width(6.dp))
                    Text("Import Bookmarks", color = Color.White, fontSize = 14.sp)
                }
            }
        } else Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(8.dp))
            marks.forEach { b ->
                Row(Modifier.fillMaxWidth().height(42.dp).clickable { st.seekTo(b.positionMs) }, verticalAlignment = Alignment.CenterVertically) {
                    Text(formatDuration(b.positionMs), Modifier.weight(1f), color = Color.White, fontSize = 14.sp)
                    Icon(painterResource(R.drawable.ic_close), "Delete", Modifier.size(20.dp).clickable { AppGraph.userData.update { it.removeBookmark(f!!.uri, b.positionMs) } }, tint = Disabled)
                }
            }
        }
        val dur = st.duration.coerceAtLeast(1)
        Row(Modifier.fillMaxWidth().height(59.dp).offset(y = 10.dp).layout { m, c -> val p = m.measure(c.copy(maxWidth = c.maxWidth + 28.dp.roundToPx())); layout(c.maxWidth, p.height) { p.place(-16.dp.roundToPx(), 0) } }, verticalAlignment = Alignment.CenterVertically) {
            Text(formatDuration(st.position), Modifier.width(52.dp), color = Color.White, fontSize = 11.sp, textAlign = TextAlign.Center)
            org.app.nxplayer.player.MxSeekBar((st.position.toFloat() / dur).coerceIn(0f, 1f), MxBlue, false, Modifier.weight(1f).fillMaxHeight(), onChange = { st.seekTo((it * dur).toLong()) }, onFinished = {})
            Text(formatDuration(st.duration), color = Color.White, fontSize = 11.sp)
        }
        Box(Modifier.fillMaxWidth().height(44.dp)) {
            Icon(painterResource(if (st.isPlaying) R.drawable.ic_pl_pause else R.drawable.ic_pl_play), "Play/Pause", Modifier.align(Alignment.Center).size(30.dp).clickable { st.togglePlay() }, tint = Color.White)
            Row(Modifier.align(Alignment.CenterEnd).offset(x = 8.dp).height(27.dp).background(PlayerAccent, RoundedCornerShape(50))
                .clickable(enabled = f != null) { f?.let { AppGraph.userData.update { d -> d.addBookmark(it.uri, st.player.currentPosition, System.currentTimeMillis()) } } }
                .padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.ic_bm_add), null, Modifier.size(14.dp), tint = OnPlayerAccent)
                Spacer(Modifier.width(4.dp))
                Text("Add", color = OnPlayerAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(5.dp))
    }
}

@Composable
private fun MorePanel(activity: PlayerActivity) {
    val st = activity.state
    var confirmDelete by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    val editable = st.current?.uri?.startsWith("content") == true
    PanelTitle("More", onBack = { st.panel = Panel.Overflow })
    Scroll {
        Spacer(Modifier.height(8.dp))
        MoreSection("Tools")
        PanelRow("Delete", enabled = editable) { confirmDelete = true }
        PanelRow("Rename", enabled = editable) { renaming = true }
        PanelRow("Lock") { st.panel = Panel.None; st.locked = true }
        PanelRow("Settings") { activity.startActivity(android.content.Intent(activity, org.app.nxplayer.MainActivity::class.java).putExtra("open", "settings")) }
        Spacer(Modifier.height(8.dp))
        MoreSection("Help")
        PanelRow("FAQ") { activity.startActivity(android.content.Intent(activity, org.app.nxplayer.MainActivity::class.java).putExtra("open", "help")) }
        PanelRow("Check for updates") {
            runCatching { activity.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("market://details?id=${activity.packageName}"))) }
                .onFailure { st.toast = "You're on the latest version" }
        }
        PanelRow("Bug Report") {
            runCatching { activity.startActivity(android.content.Intent(android.content.Intent.ACTION_SENDTO, android.net.Uri.parse("mailto:")).putExtra(android.content.Intent.EXTRA_SUBJECT, "Nx Player bug report")) }
                .onFailure { st.toast = "No email app found" }
        }
    }
    if (confirmDelete) PlayerDialog(
        onDismissRequest = { confirmDelete = false },
        text = { Text("Delete \"${st.current?.name}\"?") },
        confirmButton = {
            TextButton({
                confirmDelete = false
                st.current?.let { f ->
                    val sender = AppGraph.media.deleteRequest(listOf(f))
                    if (sender != null) activity.startIntentSenderForResult(sender, 7, null, 0, 0, 0)
                    else { activity.contentResolver.delete(android.net.Uri.parse(f.uri), null, null) }
                    AppGraph.userData.update { it.forgetFile(f.uri) }
                    if (st.player.hasNextMediaItem()) st.next() else activity.close()
                }
            }) { Text("DELETE") }
        },
        dismissButton = { TextButton({ confirmDelete = false }) { Text("CANCEL") } },
    )
    if (renaming) {
        val f = st.current
        var name by remember { mutableStateOf(f?.name?.substringBeforeLast('.') ?: "") }
        PlayerDialog(
            onDismissRequest = { renaming = false },
            title = { Text("Rename") },
            text = { androidx.compose.material3.OutlinedTextField(name, { name = it }, singleLine = true) },
            confirmButton = {
                TextButton({
                    renaming = false
                    if (f != null && name.isNotBlank()) {
                        val ext = f.name.substringAfterLast('.', "")
                        val newName = if (ext.isEmpty()) name.trim() else "${name.trim()}.$ext"
                        val sender = AppGraph.media.writeRequest(listOf(f))
                        if (sender == null) kotlinx.coroutines.MainScope().launch {
                            if (AppGraph.media.renameNow(f, newName)) { st.title = newName.substringBeforeLast('.'); st.toast = "Renamed" } else st.toast = "Rename failed"
                        } else { st.pendingRename = newName; activity.startIntentSenderForResult(sender, 8, null, 0, 0, 0) }
                    }
                }) { Text("OK") }
            },
            dismissButton = { TextButton({ renaming = false }) { Text("CANCEL") } },
        )
    }
}

@Composable
private fun MoreSection(text: String) = Box(Modifier.fillMaxWidth().height(24.dp), contentAlignment = Alignment.CenterStart) { Text(text, color = Disabled, fontSize = 13.sp) }

@Composable
private fun DisplayPanel(activity: PlayerActivity) {
    var tab by remember { mutableIntStateOf(0) }
    org.app.nxplayer.ui.settingsVersion()
    val changed = { activity.applyScreenSettings(); PlayerHolder.applySettings() }
    Row(Modifier.fillMaxWidth().height(48.dp).horizontalScroll(rememberScrollState())) {
        tunerTabs.forEachIndexed { i, t ->
            Box(Modifier.width(120.dp).fillMaxHeight().clickable { tab = i }, contentAlignment = Alignment.Center) {
                Text(t.title, color = if (i == tab) Color.White else Disabled, fontSize = 14.sp)
                if (i == tab) Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(2.dp).background(PlayerAccent))
            }
        }
    }
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = 12.dp)) {
        val paired = mapOf<Setting<*>, Setting<*>>(Keys.controlNormalColor to Keys.controlHighlightColor, Keys.progressBarColor to Keys.progressBarStyle)
        val skip = paired.values.toSet()
        tunerTabs[tab].groups.forEach { g ->
            g.title?.let { Text(it, Modifier.padding(top = 8.dp, bottom = 4.dp), color = Disabled, fontSize = 13.sp) }
            g.items.filter { it !in skip }.forEach { item ->
                if (item is BoolSetting) {
                    val v = AppGraph.settings[item]
                    CheckRow(item.title, v, height = 40.dp) { AppGraph.settings[item] = !v; changed() }
                    return@forEach
                }
                if (item !is ColorSetting && item !is ChoiceSetting) { org.app.nxplayer.ui.SettingRow(item, compact = true) { changed() }; return@forEach }
                Row(Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (item == Keys.controlNormalColor) "Controls" else item.title, Modifier.width(96.dp), color = Color.White, fontSize = 13.sp, maxLines = 2, lineHeight = 15.sp)
                    TunerControl(item, changed)
                    paired[item]?.let { Spacer(Modifier.width(12.dp)); TunerControl(it, changed) }
                }
            }
        }
    }
}

@Composable
private fun TunerControl(item: Setting<*>, changed: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    when (item) {
        is ColorSetting -> {
            val v = AppGraph.settings[item]
            Box(Modifier.size(40.dp, 20.dp).background(Color(0xFF808080)).background(Color(v)).border(1.dp, Color(0x66FFFFFF)).clickable { open = true })
            if (open) org.app.nxplayer.ui.ColorPickerDialog(item.title, v, { open = false }) { AppGraph.settings[item] = it; changed(); open = false }
        }
        is ChoiceSetting -> Box {
            val v = AppGraph.settings[item]
            Row(Modifier.height(33.dp).clickable { open = true }.padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(item.labelOf(v).ifEmpty { "Default" }, color = Color.White, fontSize = 13.sp, maxLines = 1)
                Spacer(Modifier.width(24.dp))
                androidx.compose.foundation.Canvas(Modifier.size(8.dp, 5.dp)) {
                    drawPath(androidx.compose.ui.graphics.Path().apply { moveTo(0f, 0f); lineTo(size.width, 0f); lineTo(size.width / 2, size.height); close() }, Color.White)
                }
            }
            androidx.compose.material3.DropdownMenu(open, { open = false }, containerColor = Color(0xF0161616)) {
                item.choices.forEach { (key, label) ->
                    androidx.compose.material3.DropdownMenuItem({ Text(label, color = if (key == v) PlayerAccent else Color.White, fontSize = 14.sp) }, { AppGraph.settings[item] = key; changed(); open = false })
                }
            }
        }
        else -> {}
    }
}

@Composable
private fun PlaylistPanel(st: PlayerState) {
    PanelTitle("Playlist (${st.queue.size})")
    Scroll {
        st.queue.forEachIndexed { i, f ->
            Text(f.title, Modifier.fillMaxWidth().clickable { st.jumpTo(i); st.panel = Panel.None }.padding(vertical = 10.dp),
                color = if (i == st.index) Color(AppGraph.settings[Keys.controlHighlightColor]) else Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun CustomisePanel() {
    var enabled by remember { mutableStateOf(Shortcut.decode(AppGraph.settings[Keys.shortcutItems]).toSet()) }
    PanelTitle("Customise Items")
    Scroll {
        Shortcut.entries.filter { it != Shortcut.Customise }.forEach { sc ->
            Row(Modifier.fillMaxWidth().clickable {
                enabled = if (sc in enabled) enabled - sc else enabled + sc
                AppGraph.settings[Keys.shortcutItems] = Shortcut.entries.filter { it in enabled || it == Shortcut.Customise }.joinToString(",") { it.name }
            }, verticalAlignment = Alignment.CenterVertically) {
                Checkbox(sc in enabled, null, colors = CheckboxDefaults.colors(uncheckedColor = Dim))
                Text(sc.label, color = Color.White)
            }
        }
    }
}
