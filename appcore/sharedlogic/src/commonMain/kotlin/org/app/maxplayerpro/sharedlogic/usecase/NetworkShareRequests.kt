package org.app.maxplayerpro.sharedlogic.usecase

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.app.kmediaplayer.model.MediaFile

/** Files (or a whole folder's files, with its [folderPath]) a screen asked to share on the network. */
data class NetworkShareRequest(val files: List<MediaFile>, val folderPath: String? = null)

/** The pending share request; the main window shows the share dialog while this is set. */
object NetworkShareRequests {
    private val _pending = MutableStateFlow<NetworkShareRequest?>(null)
    val pending: StateFlow<NetworkShareRequest?> = _pending.asStateFlow()

    fun request(files: List<MediaFile>, folderPath: String? = null) {
        if (files.isNotEmpty()) _pending.value = NetworkShareRequest(files, folderPath)
    }

    fun dismiss() { _pending.value = null }
}
