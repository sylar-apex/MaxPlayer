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
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.DatePeriod
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
import org.koin.compose.koinInject
import kotlin.time.Instant

/** How long a share stays open. */
private enum class Expiry(val label: String, val hours: Int?) {
    H1("1 hour", 1), H2("2 hours", 2), H3("3 hours", 3), Day("24 hours", 24), Custom("Custom hours", null), Range("Date range", null), Never("No expiry", null),
}

/** Asks how to share [files] on the network, creates the share, then shows its code and addresses. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkShareDialog(files: List<MediaFile>, onDismiss: () -> Unit) {
    val sharing = koinInject<SharingController>()
    val scope = rememberCoroutineScope()
    if (!sharing.host.supported) {
        PlayerDialog(onDismiss, confirmButton = { TextButton(onDismiss) { Text("OK") } }, title = { Text("Share on network") },
            text = { Text("Sharing from here isn't available. Share from Max Player on Android or desktop, then open it here by address.") })
        return
    }
    var created by remember { mutableStateOf<Share?>(null) }
    created?.let { ShareCreatedDialog(it, sharing, onDismiss); return }

    var title by remember { mutableStateOf(if (files.size == 1) files[0].title else files[0].folderName.ifEmpty { "${files.size} files" }) }
    var access by remember { mutableStateOf(ShareAccess.Code) }
    var code by remember { mutableStateOf(ShareStore.newCode()) }
    var expiry by remember { mutableStateOf(Expiry.H2) }
    var customHours by remember { mutableStateOf("6") }
    var range by remember { mutableStateOf<Pair<Long, Long>?>(null) }
    var pickingRange by remember { mutableStateOf(false) }

    PlayerDialog(
        onDismissRequest = onDismiss,
        title = { Text("Share on network") },
        text = {
            Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
                Text("${files.size} ${if (files.size == 1) "file" else "files"} · stream on any device on this Wi-Fi, no copying.",
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth().padding(top = 10.dp), singleLine = true, label = { Text("Name") })

                Label("Who can watch")
                Choice("Only people with the code", access == ShareAccess.Code) { access = ShareAccess.Code }
                if (access == ShareAccess.Code) {
                    OutlinedTextField(code, { code = it.filter { c -> c.isLetterOrDigit() }.take(12) }, Modifier.fillMaxWidth().padding(start = 40.dp),
                        singleLine = true, label = { Text("Code") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
                Choice("Anyone on this network", access == ShareAccess.Public) { access = ShareAccess.Public }

                Label("Available for")
                Expiry.entries.forEach { e ->
                    Choice(e.label, expiry == e) { expiry = e; if (e == Expiry.Range) pickingRange = true }
                    if (e == Expiry.Custom && expiry == Expiry.Custom) {
                        OutlinedTextField(customHours, { customHours = it.filter(Char::isDigit).take(4) }, Modifier.fillMaxWidth().padding(start = 40.dp),
                            singleLine = true, label = { Text("Hours") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    }
                    if (e == Expiry.Range && expiry == Expiry.Range) {
                        Text(range?.let { (s, end) -> "${formatDate(s)} → ${formatDate(end - 1)}" } ?: "Pick dates", Modifier.padding(start = 40.dp)
                            .clickable { pickingRange = true }.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        },
        confirmButton = {
            val window = expiryWindow(expiry, customHours.toIntOrNull(), range)
            TextButton({
                val (starts, expires) = window ?: return@TextButton
                scope.launch {
                    created = sharing.share(title.trim(), files, access, code, starts, expires)
                }
            }, enabled = window != null && (access == ShareAccess.Public || code.length >= 4)) { Text("SHARE") }
        },
        dismissButton = { TextButton(onDismiss) { Text("CANCEL") } },
    )

    if (pickingRange) {
        val state = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { pickingRange = false },
            confirmButton = {
                TextButton({
                    val s = state.selectedStartDateMillis
                    val e = state.selectedEndDateMillis ?: s
                    if (s != null && e != null) range = localDayStart(s) to localDayStart(e, plusDays = 1)
                    pickingRange = false
                }, enabled = state.selectedStartDateMillis != null) { Text("OK") }
            },
            dismissButton = { TextButton({ pickingRange = false }) { Text("CANCEL") } },
        ) { DateRangePicker(state, Modifier.height(480.dp), title = { Text("Share open from – to", Modifier.padding(start = 24.dp, top = 16.dp)) }) }
    }
}

@Composable
private fun ShareCreatedDialog(share: Share, sharing: SharingController, onDismiss: () -> Unit) {
    val host by sharing.host.state.collectAsState()
    PlayerDialog(
        onDismissRequest = onDismiss,
        title = { Text("Shared") },
        text = {
            SelectionContainer {
                Column {
                    if (share.access == ShareAccess.Code) {
                        Text("Code", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(share.code.orEmpty(), fontSize = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
                        Spacer(Modifier.height(10.dp))
                    }
                    Text(shareStatus(share, nowMs()).label, fontSize = 13.sp)
                    Spacer(Modifier.height(10.dp))
                    when (val h = host) {
                        is HostState.Running -> {
                            Text("On another device open Max Player → Network share, or open in a browser:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

@Composable
private fun Label(text: String) {
    Text(text, Modifier.padding(top = 14.dp, bottom = 2.dp), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun Choice(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected, onClick)
        Text(text, fontSize = 15.sp)
    }
}

/** (startsAt, expiresAt) for the chosen option, or null while the choice is incomplete. */
private fun expiryWindow(expiry: Expiry, customHours: Int?, range: Pair<Long, Long>?): Pair<Long?, Long?>? {
    val now = nowMs()
    return when (expiry) {
        Expiry.Never -> null to null
        Expiry.Custom -> customHours?.takeIf { it > 0 }?.let { null to now + it * 3_600_000L }
        Expiry.Range -> range?.let { (s, e) -> (s.takeIf { it > now }) to e }
        else -> null to now + expiry.hours!! * 3_600_000L
    }
}

/** The date picker reports UTC midnight of the chosen day; shares open and close at local midnight. */
private fun localDayStart(utcMidnightMs: Long, plusDays: Int = 0): Long {
    val day: LocalDate = Instant.fromEpochMilliseconds(utcMidnightMs).toLocalDateTime(TimeZone.UTC).date.plus(DatePeriod(days = plusDays))
    return day.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
}

private fun formatDate(ms: Long): String = PlatformMedia.formatDateLocalized(ms, "medium", null)
