package org.app.maxplayerpro.privatefolder.ui

import com.psbapp.mvi.UiEffect
import com.psbapp.mvi.UiEvent
import com.psbapp.mvi.UiState
import org.app.maxplayerpro.sharedlogic.model.PrivateFile

class PrivateFolderContract {
    sealed class Event : UiEvent {
        data class VerifyPin(val pin: String) : Event()
        data class CreatePin(val pin: String) : Event()
        data class Play(val file: PrivateFile) : Event()
        data class Restore(val file: PrivateFile) : Event()
        data class Rename(val file: PrivateFile, val name: String) : Event()
        data class Delete(val file: PrivateFile) : Event()
    }

    data class State(
        val unlocked: Boolean = false,
        val hasPin: Boolean = false,
        val files: List<PrivateFile> = emptyList(),
    ) : UiState

    sealed class Effect : UiEffect {
        data object WrongPin : Effect()
    }
}
