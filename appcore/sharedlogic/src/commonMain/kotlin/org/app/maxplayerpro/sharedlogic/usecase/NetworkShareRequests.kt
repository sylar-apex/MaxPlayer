package org.app.maxplayerpro.sharedlogic.usecase

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.app.kmediaplayer.model.MediaFile

/** Files a list asked to share on the network; the main window shows the share dialog while this is set. */
object NetworkShareRequests {
    private val _pending = MutableStateFlow<List<MediaFile>?>(null)
    val pending: StateFlow<List<MediaFile>?> = _pending.asStateFlow()

    fun request(files: List<MediaFile>) { if (files.isNotEmpty()) _pending.value = files }

    fun dismiss() { _pending.value = null }
}
