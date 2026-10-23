package org.app.maxplayerpro.localnetwork.share

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.app.kmediaplayer.util.nowMs
import org.app.maxplayerpro.sharedlogic.usecase.NetworkShareMarks
import org.app.maxplayerpro.sharedlogic.usecase.ShareMarks

/** Marks for the library lists: what other devices can open now (sharing on, share open), and making one item private. */
class ShareMarksSource(private val store: ShareStore, host: ShareHost) : NetworkShareMarks {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val marks: StateFlow<ShareMarks> = combine(host.state, store.shares) { state, shares ->
        if (state !is HostState.Running) return@combine ShareMarks()
        val now = nowMs()
        val open = shares.filter { it.isOpen(now) }
        ShareMarks(folders = open.mapNotNull { it.folderPath }.toSet(), files = open.flatMap { s -> s.items.map { it.uri } }.toSet())
    }.stateIn(scope, SharingStarted.Eagerly, ShareMarks())

    override fun makeFolderPrivate(path: String) {
        scope.launch { store.shares.value.filter { it.folderPath == path }.forEach { store.delete(it.id) } }
    }

    override fun makeFilePrivate(uri: String) {
        scope.launch { store.removeFile(uri) }
    }
}
