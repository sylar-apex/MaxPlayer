package org.app.maxplayerpro.files.ui

import com.psbapp.mvi.UiEffect
import com.psbapp.mvi.UiEvent
import com.psbapp.mvi.UiState
import org.app.maxplayerpro.sharedlogic.data.FileEntry
import org.app.maxplayerpro.sharedlogic.data.FileRoot

class FilesContract {
    sealed class Event : UiEvent {
        data class OpenFolder(val path: String) : Event()
        data object Up : Event()
        data object Refresh : Event()
        data object RequestAccess : Event()
        /** Plays a video or song, with the folder's other media queued after it. */
        data class Play(val entry: FileEntry) : Event()
        data class Share(val entries: List<FileEntry>) : Event()
        /** Documents, archives, …: open in the system's app for that file type. */
        data class OpenExternally(val entry: FileEntry) : Event()
        /** Stops sharing a shared folder, subfolder or file. */
        data class MakePrivate(val entry: FileEntry) : Event()
        /** Shares a folder or file that sits inside a shared folder, as part of that share. */
        data class ShareInside(val entry: FileEntry) : Event()
    }

    data class State(
        val hasAccess: Boolean = true,
        val roots: List<FileRoot> = emptyList(),
        /** Null at the top, where the roots are listed. */
        val path: String? = null,
        val entries: List<FileEntry> = emptyList(),
        val loading: Boolean = false,
    ) : UiState

    sealed class Effect : UiEffect
}
