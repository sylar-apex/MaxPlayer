package org.app.maxplayerpro.sharedlogic.usecase

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.app.kmediaplayer.model.MediaFile

/** One thing to share: some files, or a whole folder's files with its [folderPath]. */
data class ShareTarget(val files: List<MediaFile>, val folderPath: String? = null)

/** What a screen asked to share on the network; several folders picked together share with the same settings. */
data class NetworkShareRequest(val targets: List<ShareTarget>)

/** The pending share request; the main window shows the share dialog while this is set. */
object NetworkShareRequests {
    private val _pending = MutableStateFlow<NetworkShareRequest?>(null)
    val pending: StateFlow<NetworkShareRequest?> = _pending.asStateFlow()

    fun request(files: List<MediaFile>, folderPath: String? = null) = request(listOf(ShareTarget(files, folderPath)))

    fun request(targets: List<ShareTarget>) {
        val nonEmpty = targets.filter { it.files.isNotEmpty() }
        if (nonEmpty.isNotEmpty()) _pending.value = NetworkShareRequest(nonEmpty)
    }

    fun dismiss() { _pending.value = null }
}
