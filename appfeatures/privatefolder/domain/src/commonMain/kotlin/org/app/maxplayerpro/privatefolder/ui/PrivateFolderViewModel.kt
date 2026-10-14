package org.app.maxplayerpro.privatefolder.ui

import androidx.lifecycle.viewModelScope
import com.psbapp.mvi.MVIBaseViewModel
import kotlinx.coroutines.launch
import org.app.kmediaplayer.model.MediaFile
import org.app.maxplayerpro.sharedlogic.data.MediaRepository
import org.app.maxplayerpro.sharedlogic.data.PrivateFolderRepository
import org.app.maxplayerpro.sharedlogic.usecase.MediaOperations

class PrivateFolderViewModel(
    private val privateFolder: PrivateFolderRepository,
    private val media: MediaRepository,
    private val operations: MediaOperations,
) : MVIBaseViewModel<PrivateFolderContract.Event, PrivateFolderContract.State, PrivateFolderContract.Effect>() {

    override fun createInitialState() = PrivateFolderContract.State()
    override fun handleEffect(effect: PrivateFolderContract.Effect) {}

    init {
        setState { copy(hasPin = privateFolder.hasPin) }
        viewModelScope.launch { privateFolder.files.collect { f -> setState { copy(files = f) } } }
    }

    override fun handleEvent(event: PrivateFolderContract.Event) {
        when (event) {
            is PrivateFolderContract.Event.VerifyPin ->
                if (privateFolder.checkPin(event.pin)) setState { copy(unlocked = true) } else setEffect(PrivateFolderContract.Effect.WrongPin)
            is PrivateFolderContract.Event.CreatePin -> { privateFolder.setPin(event.pin); setState { copy(unlocked = true, hasPin = true) } }
            is PrivateFolderContract.Event.Play -> {
                val f = event.file
                operations.play(listOf(MediaFile(f.uri.hashCode().toLong(), f.uri, f.path, f.name, 0, 0, 0)), 0)
            }
            is PrivateFolderContract.Event.Restore -> viewModelScope.launch {
                privateFolder.restore(event.file); media.refresh(); operations.toast("Moved to Movies/MaxPlayerPro")
            }
            is PrivateFolderContract.Event.Rename -> privateFolder.rename(event.file, event.name)
            is PrivateFolderContract.Event.Delete -> privateFolder.delete(event.file)
        }
    }
}
