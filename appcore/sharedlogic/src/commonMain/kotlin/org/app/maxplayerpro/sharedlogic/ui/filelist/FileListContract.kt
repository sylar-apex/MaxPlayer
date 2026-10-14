package org.app.maxplayerpro.sharedlogic.ui.filelist

import com.psbapp.mvi.UiEffect
import com.psbapp.mvi.UiEvent
import com.psbapp.mvi.UiState
import org.app.kmediaplayer.model.MediaFile
import org.app.kmediaplayer.model.PlaybackRecord
import org.app.maxplayerpro.sharedlogic.model.Playlist

class FileListContract {
    sealed class Event : UiEvent {
        data class FilesShown(val files: List<MediaFile>) : Event()
        data class Play(val files: List<MediaFile>, val index: Int) : Event()
        data class Share(val files: List<MediaFile>) : Event()
        data class LockInPrivate(val files: List<MediaFile>) : Event()
        data class Rename(val file: MediaFile, val newName: String) : Event()
        data class Delete(val files: List<MediaFile>) : Event()
        data class AddToPlaylist(val playlistId: Long, val files: List<MediaFile>) : Event()
        data class CreatePlaylist(val name: String, val files: List<MediaFile>, val music: Boolean) : Event()
        data class Toast(val message: String) : Event()
    }

    data class State(
        val history: Map<String, PlaybackRecord> = emptyMap(),
        val playlists: List<Playlist> = emptyList(),
        /** File names next to the shown videos, to tag the ones that have subtitles. */
        val siblingNames: Set<String> = emptySet(),
        val newTaggedPeriod: Int = 7,
        val markLastPlayed: Boolean = true,
        val scrollToLast: Boolean = false,
        val selectThumbnail: Boolean = false,
        val allowEdits: Boolean = true,
    ) : UiState

    sealed class Effect : UiEffect
}
