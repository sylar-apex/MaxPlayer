package org.app.maxplayerpro.privatefolder.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.Flow
import org.app.kmediaplayer.ui.common.PlayerDialog
import org.app.maxplayerpro.appcore.theme.MaxColors
import org.app.maxplayerpro.privatefolder.domain.constant.PrivateFolderLimits
import org.app.maxplayerpro.privatefolder.ui.PrivateFolderContract

/** Asks for the PIN, or on first use asks for a new PIN twice. */
@Composable
internal fun PinGate(
    isSetup: Boolean,
    effects: Flow<PrivateFolderContract.Effect>,
    onCancel: () -> Unit,
    onVerify: (String) -> Unit,
    onCreate: (String) -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf("") }
    LaunchedEffect(effects) {
        effects.collect { if (it == PrivateFolderContract.Effect.WrongPin) { error = "Wrong PIN"; pin = "" } }
    }
    val title = when { !isSetup -> "Enter PIN"; confirm == null -> "Create a 4-digit PIN"; else -> "Confirm PIN" }
    PlayerDialog(
        onDismissRequest = onCancel,
        title = { Text("Private Folder") },
        text = {
            Column {
                Text(title)
                TextField(pin, { if (it.length <= PrivateFolderLimits.PIN_LENGTH && it.all(Char::isDigit)) pin = it }, singleLine = true, visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    colors = TextFieldDefaults.colors(focusedContainerColor = MaxColors.transparent, unfocusedContainerColor = MaxColors.transparent))
                if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }
        },
        confirmButton = {
            TextButton({
                if (pin.length != PrivateFolderLimits.PIN_LENGTH) { error = "PIN must be 4 digits"; return@TextButton }
                when {
                    !isSetup -> onVerify(pin)
                    confirm == null -> { confirm = pin; pin = ""; error = "" }
                    confirm == pin -> onCreate(pin)
                    else -> { error = "PINs don't match"; confirm = null; pin = "" }
                }
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onCancel) { Text("Cancel") } },
    )
}
