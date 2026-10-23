package org.app.maxplayerpro.appcore.component

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.app.kmediaplayer.platform.PlatformMedia
import org.app.maxplayerpro.sharedlogic.usecase.NetworkShareMarks
import org.app.maxplayerpro.sharedlogic.usecase.ShareMarks
import org.koin.compose.koinInject

/** Same green as the sharing switch in the top bar. */
val SharedGreen = Color(0xFF34C759)

/** The current share marks, for lists that tag shared folders and files. */
@Composable
fun rememberShareMarks(marks: NetworkShareMarks = koinInject()): Pair<State<ShareMarks>, NetworkShareMarks> = marks.marks.collectAsState() to marks

/** Green sharing mark on a shared folder or file; tapping it makes that item private. */
@Composable
fun SharedMark(onMakePrivate: () -> Unit, modifier: Modifier = Modifier, size: Dp = 18.dp) {
    IconButton({ onMakePrivate(); PlatformMedia.showToast("Made private · no longer shared") }, modifier.size(size + 14.dp)) {
        Icon(Icons.Default.Sensors, "Shared on network. Tap to make private", Modifier.size(size), tint = SharedGreen)
    }
}
