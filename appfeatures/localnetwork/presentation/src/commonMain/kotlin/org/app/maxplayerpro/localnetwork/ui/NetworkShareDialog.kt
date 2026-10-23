package org.app.maxplayerpro.localnetwork.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.app.kmediaplayer.model.MediaFile
import org.app.kmediaplayer.platform.PlatformMedia
import org.app.kmediaplayer.ui.common.PlayerDialog
import org.app.kmediaplayer.util.nowMs
import org.app.maxplayerpro.localnetwork.share.HostState
import org.app.maxplayerpro.localnetwork.share.Share
import org.app.maxplayerpro.localnetwork.share.ShareAccess
import org.app.maxplayerpro.localnetwork.share.ShareStore
import org.app.maxplayerpro.localnetwork.share.SharingController
import org.app.maxplayerpro.sharedlogic.usecase.ShareTarget
import org.koin.compose.koinInject
import kotlin.time.Instant

/** How long something stays shared. */
internal enum class Expiry(val label: String, val hours: Int?) {
    H1("1 hour", 1), H2("2 hours", 2), H3("3 hours", 3), Day("24 hours", 24), Custom("Custom hours", null), Range("Date range", null), Never("No limit", null),
}

/** Who may watch: the share's mode. Private means not shared at all. */
internal enum class Mode(val label: String, val hint: String) {
    Public("Public", "Anyone who can open this device"),
    Protected("Protected", "Only with the PIN you set here"),
    Private("Private", "Not shared"),
}

/** Shares [files], or the folder [folderPath], on the network. */
@Composable
fun NetworkShareDialog(files: List<MediaFile>, folderPath: String?, onDismiss: () -> Unit) =
    NetworkShareDialog(listOf(ShareTarget(files, folderPath)), onDismiss)

/**
 * Asks how to share [targets] on the network; several folders picked together get the same mode, PIN and time. With one
 * target it opens on its current mode when already shared, so the same dialog also changes or ends a share. After
 * sharing it shows the PIN and addresses.
 */
@Composable
fun NetworkShareDialog(targets: List<ShareTarget>, onDismiss: () -> Unit) {
    val sharing = koinInject<SharingController>()
    val scope = rememberCoroutineScope()
    if (!sharing.host.supported) {
        PlayerDialog(onDismiss, confirmButton = { TextButton(onDismiss) { Text("OK") } }, title = { Text("Share on network") },
            text = { Text("Sharing from here isn't available. Share from Max Player on Android or desktop, then open it here by address.") })
        return
    }
    var created by remember { mutableStateOf<Share?>(null) }
    created?.let { ShareCreatedDialog(it, sharing, onDismiss); return }

    val single = targets.singleOrNull()
    val files = targets.flatMap { it.files }
    val folderPath = single?.folderPath
    val existing = remember { single?.let { sharing.store.findFor(it.files, it.folderPath) } }
    val folderName = folderPath?.substringAfterLast('/')?.ifEmpty { folderPath }
    var title by remember { mutableStateOf(existing?.title ?: folderName ?: if (files.size == 1) files[0].title else "${files.size} files") }
    var mode by remember { mutableStateOf(when (existing?.access) { ShareAccess.Public -> Mode.Public; ShareAccess.Protected -> Mode.Protected; null -> Mode.Public }) }
    var pin by remember { mutableStateOf(existing?.code ?: ShareStore.newCode()) }
    var allowDownload by remember { mutableStateOf(existing?.allowDownload ?: false) }
    val expiry = rememberExpiryState(if (existing?.expiresAtMs == null) Expiry.Never else Expiry.Range,
        existing?.let { s -> s.expiresAtMs?.let { (s.startsAtMs ?: s.createdAtMs) to it } })

    PlayerDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing != null) "Shared on network" else "Share on network") },
        text = {
            Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
                val count = "${files.size} ${if (files.size == 1) "file" else "files"}"
                val folders = targets.count { it.folderPath != null }
                Text(
                    when {
                        targets.size > 1 -> "${targets.size} items · $count. Each folder keeps its own name. Files added later stay private."
                        folderPath != null -> "Folder · $count. Files added later stay private."
                        else -> count
                    },
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (targets.size == 1) OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth().padding(top = 10.dp), singleLine = true, label = { Text("Name") })
                else if (folders < targets.size) Text("Files picked on their own are shared together.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Label("Mode")
                Mode.entries.forEach { m ->
                    Choice(m.label, m.hint, mode == m) { mode = m }
                    if (m == Mode.Protected && mode == Mode.Protected) {
                        OutlinedTextField(pin, { pin = it.filter { c -> c.isLetterOrDigit() }.take(12) }, Modifier.fillMaxWidth().padding(start = 40.dp),
                            singleLine = true, label = { Text("PIN for this share") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    }
                }
                if (mode != Mode.Private) {
                    Row(Modifier.fillMaxWidth().clickable { allowDownload = !allowDownload }.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Allow download", fontSize = 15.sp)
                            Text(if (allowDownload) "Others can save these files to their device" else "Others can only watch or listen",
                                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(allowDownload, { allowDownload = it })
                    }
                    Label("Available for")
                    ExpiryChooser(expiry)
                }
            }
        },
        confirmButton = {
            val window = expiry.window()
            val ready = mode == Mode.Private || (window != null && (mode == Mode.Public || pin.length >= 4))
            TextButton({
                scope.launch {
                    if (mode == Mode.Private) {
                        // Makes every picked folder or file private again.
                        targets.forEach { t -> sharing.store.findFor(t.files, t.folderPath)?.let { sharing.store.delete(it.id) } }
                        onDismiss()
                        return@launch
                    }
                    val (starts, expires) = window ?: return@launch
                    val access = if (mode == Mode.Protected) ShareAccess.Protected else ShareAccess.Public
                    if (single != null) {
                        created = sharing.share(title.trim(), files, folderPath, access, pin, starts, expires, allowDownload)
                    } else {
                        // Folders keep their names; loose files go together in one share.
                        val loose = targets.filter { it.folderPath == null }.flatMap { it.files }
                        var last: Share? = null
                        targets.filter { it.folderPath != null }.forEach { t ->
                            val path = t.folderPath!!
                            last = sharing.share(path.substringAfterLast('/').ifEmpty { path }, t.files, t.folderPath, access, pin, starts, expires, allowDownload)
                        }
                        if (loose.isNotEmpty()) last = sharing.share(if (loose.size == 1) loose[0].title else "${loose.size} files", loose, null, access, pin, starts, expires, allowDownload)
                        created = last
                    }
                }
            }, enabled = ready) { Text(if (mode == Mode.Private) (if (existing != null) "MAKE PRIVATE" else "CLOSE") else "SHARE") }
        },
        dismissButton = { TextButton(onDismiss) { Text("CANCEL") } },
    )
    ExpiryRangePicker(expiry)
}

@Composable
private fun ShareCreatedDialog(share: Share, sharing: SharingController, onDismiss: () -> Unit) {
    val host by sharing.host.state.collectAsState()
    val device by sharing.store.device.collectAsState()
    PlayerDialog(
        onDismissRequest = onDismiss,
        title = { Text("Shared") },
        text = {
            SelectionContainer {
                Column {
                    if (share.access == ShareAccess.Protected) {
                        Text("PIN for this share", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(share.code.orEmpty(), fontSize = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
                        Spacer(Modifier.height(10.dp))
                    } else {
                        Text("Public", fontWeight = FontWeight.SemiBold)
                    }
                    Text(shareStatus(share, nowMs()).label, fontSize = 13.sp)
                    if (device.pin != null) Text("Others also need this device's PIN to see it.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(10.dp))
                    when (val h = host) {
                        is HostState.Running -> {
                            Text("On another device open Max Player → Network, or open in a browser:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            h.addresses.forEach { Text(it, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp)) }
                        }
                        is HostState.Failed -> Text(h.reason, color = MaterialTheme.colorScheme.error)
                        else -> Text("Starting…", fontSize = 13.sp)
                    }
                }
            }
        },
        confirmButton = { TextButton(onDismiss) { Text("DONE") } },
    )
}

/** Sets, changes or removes the PIN others need to open this device. */
@Composable
internal fun DevicePinDialog(hasPin: Boolean, onDismiss: () -> Unit, onSave: (String?) -> Unit) {
    var pin by remember { mutableStateOf("") }
    PlayerDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (hasPin) "Change device PIN" else "Set device PIN") },
        text = {
            Column {
                Text("Others enter this PIN once to open this device. Changing or removing it logs out every device.",
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(pin, { pin = it.filter { c -> c.isLetterOrDigit() }.take(12) }, Modifier.fillMaxWidth().padding(top = 8.dp), singleLine = true,
                    label = { Text("New PIN (4+ characters)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword))
            }
        },
        confirmButton = { TextButton({ onSave(pin) }, enabled = pin.length >= 4) { Text("SAVE") } },
        dismissButton = {
            Row {
                if (hasPin) TextButton({ onSave(null) }) { Text("REMOVE PIN") }
                TextButton(onDismiss) { Text("CANCEL") }
            }
        },
    )
}

/** How long this whole device stays shared; past it sharing turns itself off. */
@Composable
internal fun DeviceLimitDialog(currentUntilMs: Long?, onDismiss: () -> Unit, onSave: (Long?) -> Unit) {
    val expiry = rememberExpiryState(if (currentUntilMs == null) Expiry.Never else Expiry.Range, currentUntilMs?.let { nowMs() to it })
    PlayerDialog(
        onDismissRequest = onDismiss,
        title = { Text("Share this device for") },
        text = { Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) { ExpiryChooser(expiry) } },
        confirmButton = {
            val window = expiry.window()
            TextButton({ window?.let { onSave(it.second) } }, enabled = window != null) { Text("SAVE") }
        },
        dismissButton = { TextButton(onDismiss) { Text("CANCEL") } },
    )
    ExpiryRangePicker(expiry)
}

/** The "available for" choice and its inputs, shared by the share and device dialogs. */
internal class ExpiryState(initial: Expiry, initialRange: Pair<Long, Long>?) {
    var expiry by mutableStateOf(initial)
    var customHours by mutableStateOf("6")
    var range by mutableStateOf(initialRange)
    var pickingRange by mutableStateOf(false)

    /** (startsAt, expiresAt) for the choice, or null while it is incomplete. */
    fun window(): Pair<Long?, Long?>? {
        val now = nowMs()
        return when (expiry) {
            Expiry.Never -> null to null
            Expiry.Custom -> customHours.toIntOrNull()?.takeIf { it > 0 }?.let { null to now + it * 3_600_000L }
            Expiry.Range -> range?.takeIf { it.second > now }?.let { (s, e) -> (s.takeIf { it > now }) to e }
            else -> null to now + expiry.hours!! * 3_600_000L
        }
    }
}

@Composable
internal fun rememberExpiryState(initial: Expiry, initialRange: Pair<Long, Long>?) = remember { ExpiryState(initial, initialRange) }

@Composable
internal fun ExpiryChooser(state: ExpiryState) {
    Expiry.entries.forEach { e ->
        Choice(e.label, null, state.expiry == e) { state.expiry = e; if (e == Expiry.Range && state.range == null) state.pickingRange = true }
        if (e == Expiry.Custom && state.expiry == Expiry.Custom) {
            OutlinedTextField(state.customHours, { state.customHours = it.filter(Char::isDigit).take(4) }, Modifier.fillMaxWidth().padding(start = 40.dp),
                singleLine = true, label = { Text("Hours") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        }
        if (e == Expiry.Range && state.expiry == Expiry.Range) {
            Text(state.range?.let { (s, end) -> "${formatDate(s)} → ${formatDate(end - 1)}" } ?: "Pick dates", Modifier.padding(start = 40.dp)
                .clickable { state.pickingRange = true }.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.primary)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExpiryRangePicker(state: ExpiryState) {
    if (!state.pickingRange) return
    val picker = rememberDateRangePickerState()
    DatePickerDialog(
        onDismissRequest = { state.pickingRange = false },
        confirmButton = {
            TextButton({
                val s = picker.selectedStartDateMillis
                val e = picker.selectedEndDateMillis ?: s
                if (s != null && e != null) state.range = localDayStart(s) to localDayStart(e, plusDays = 1)
                state.pickingRange = false
            }, enabled = picker.selectedStartDateMillis != null) { Text("OK") }
        },
        dismissButton = { TextButton({ state.pickingRange = false }) { Text("CANCEL") } },
    ) { DateRangePicker(picker, Modifier.height(480.dp), title = { Text("Open from – to", Modifier.padding(start = 24.dp, top = 16.dp)) }) }
}

@Composable
private fun Label(text: String) {
    Text(text, Modifier.padding(top = 14.dp, bottom = 2.dp), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun Choice(text: String, hint: String?, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected, onClick)
        Column {
            Text(text, fontSize = 15.sp)
            hint?.let { Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

/** The date picker reports UTC midnight of the chosen day; shares open and close at local midnight. */
private fun localDayStart(utcMidnightMs: Long, plusDays: Int = 0): Long {
    val day: LocalDate = Instant.fromEpochMilliseconds(utcMidnightMs).toLocalDateTime(TimeZone.UTC).date.plus(DatePeriod(days = plusDays))
    return day.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
}

internal fun formatDate(ms: Long): String = PlatformMedia.formatDateLocalized(ms, "medium", null)
