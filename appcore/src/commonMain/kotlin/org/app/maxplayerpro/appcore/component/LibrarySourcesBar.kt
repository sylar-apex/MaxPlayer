package org.app.maxplayerpro.appcore.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.app.maxplayerpro.sharedlogic.data.FolderImport
import org.app.maxplayerpro.sharedlogic.data.LibrarySource
import org.app.maxplayerpro.sharedlogic.data.SourceStatus

/**
 * The folders added to the library (web): a reconnect card for remembered folders that need the
 * user again, a notice for folders that disappeared, and chips to remove folders or add another.
 * Shows nothing on platforms that scan storage themselves.
 */
@Composable
fun LibrarySourcesBar(folderImport: FolderImport, modifier: Modifier = Modifier, inset: Dp = 16.dp) {
    if (!folderImport.available) return
    val sources by folderImport.sources.collectAsState()
    if (sources.isEmpty()) return
    val waiting = sources.filter { it.status == SourceStatus.NeedsPermission }
    val repick = sources.filter { it.status == SourceStatus.NeedsRepick }
    val missing = sources.filter { it.status == SourceStatus.Missing }
    Column(modifier.fillMaxWidth().padding(horizontal = inset), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (waiting.isNotEmpty()) Notice(
            Icons.Outlined.LockOpen,
            "Welcome back. Allow access again to show your videos from ${names(waiting)}.",
            "Restore access", folderImport::restoreAccess,
        )
        if (repick.isNotEmpty()) Notice(
            Icons.Outlined.CreateNewFolder,
            "This browser can't keep folder access between visits. Pick ${names(repick)} again to play its videos; your progress is kept.",
            "Pick folder", folderImport::pickFolder,
        )
        missing.forEach { m ->
            Notice(Icons.Outlined.FolderOff, "\"${m.name}\" was moved, renamed or its drive is disconnected.", "Remove", { folderImport.forget(m.id) }, primary = false)
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            sources.filter { it.status == SourceStatus.Ready || it.status == SourceStatus.Scanning }.forEach { SourceChip(it) { folderImport.forget(it.id) } }
            Row(
                Modifier.height(32.dp).border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50)).clickable(onClick = folderImport::pickFolder).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.CreateNewFolder, null, Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Add folder", fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

private fun names(sources: List<LibrarySource>): String = when (sources.size) {
    1 -> "\"${sources[0].name}\""
    2 -> "\"${sources[0].name}\" and \"${sources[1].name}\""
    else -> "\"${sources[0].name}\" and ${sources.size - 1} more folders"
}

@Composable
private fun Notice(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, action: String, onAction: () -> Unit, primary: Boolean = true) {
    Row(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp)).padding(start = 16.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Text(text, Modifier.weight(1f), fontSize = 13.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.width(12.dp))
        if (primary) Button(onAction, contentPadding = PaddingValues(horizontal = 16.dp)) { Text(action) }
        else OutlinedButton(onAction, contentPadding = PaddingValues(horizontal = 16.dp)) { Text(action) }
    }
}

@Composable
private fun SourceChip(source: LibrarySource, onRemove: () -> Unit) {
    Row(
        Modifier.height(32.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50)).padding(start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (source.status == SourceStatus.Scanning) CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
        else Icon(Icons.Outlined.Folder, null, Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            if (source.status == SourceStatus.Scanning) "${source.name} · scanning…" else "${source.name} · ${source.videoCount}",
            fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        Icon(Icons.Outlined.Close, "Remove ${source.name}", Modifier.size(28.dp).clickable(onClick = onRemove).padding(6.dp))
    }
}
