package org.app.maxplayerpro.privatefolder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.app.kmediaplayer.ui.common.PlayerDialog
import org.app.kmediaplayer.ui.settings.TextInputDialog
import org.app.kmediaplayer.util.formatSize
import org.app.maxplayerpro.appcore.component.EmptyState
import org.app.maxplayerpro.appcore.component.TopBar
import org.app.maxplayerpro.privatefolder.ui.component.PinGate
import org.app.maxplayerpro.sharedlogic.model.PrivateFile
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PrivateFolderScreen(onBack: () -> Unit, viewModel: PrivateFolderViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    PrivateFolderContent(state, viewModel.effect, onBack, onEvent = viewModel::setEvent)
}

@Composable
fun PrivateFolderContent(
    state: PrivateFolderContract.State,
    effects: Flow<PrivateFolderContract.Effect> = emptyFlow(),
    onBack: () -> Unit,
    onEvent: (PrivateFolderContract.Event) -> Unit,
) {
    var menuFor by remember { mutableStateOf<PrivateFile?>(null) }
    if (!state.unlocked) {
        PinGate(isSetup = !state.hasPin, effects = effects, onCancel = onBack,
            onVerify = { onEvent(PrivateFolderContract.Event.VerifyPin(it)) }, onCreate = { onEvent(PrivateFolderContract.Event.CreatePin(it)) })
        return
    }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column {
            TopBar("Private Folder", onBack = onBack)
            if (state.files.isEmpty()) EmptyState("You haven't added any files yet.\nUse ⋮ → Lock in Private Folder on any video.")
            LazyColumn {
                items(state.files, key = { it.path }) { f ->
                    Row(Modifier.fillMaxWidth().clickable { onEvent(PrivateFolderContract.Event.Play(f)) }.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, null)
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(f.name, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(formatSize(f.sizeBytes), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton({ menuFor = f }) { Icon(Icons.Default.Edit, "Options") }
                    }
                }
            }
        }
        FloatingActionButton(onBack, Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(20.dp)) { Icon(Icons.Default.Add, "Add files") }
    }
    menuFor?.let { f ->
        var renaming by remember { mutableStateOf(false) }
        PlayerDialog(
            onDismissRequest = { menuFor = null },
            title = { Text(f.name, maxLines = 2) },
            text = {
                Column {
                    Text("Move out of Private Folder", Modifier.fillMaxWidth().clickable { onEvent(PrivateFolderContract.Event.Restore(f)); menuFor = null }.padding(vertical = 12.dp))
                    Text("Rename", Modifier.fillMaxWidth().clickable { renaming = true }.padding(vertical = 12.dp))
                    Text("Delete", Modifier.fillMaxWidth().clickable { onEvent(PrivateFolderContract.Event.Delete(f)); menuFor = null }.padding(vertical = 12.dp))
                }
            },
            confirmButton = { TextButton({ menuFor = null }) { Text("Cancel") } },
        )
        if (renaming) TextInputDialog("Rename", f.name, onDismiss = { renaming = false }) { onEvent(PrivateFolderContract.Event.Rename(f, it)); renaming = false; menuFor = null }
    }
}
