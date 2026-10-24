package org.app.maxplayerpro.localnetwork.share

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.app.kmediaplayer.model.MediaFile
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
        val roots = open.mapNotNull { it.folderPath }.toSet()
        // A subfolder counts as shared while anything under it is.
        val subfolders = open.filter { it.folderPath != null }.flatMap { s ->
            s.items.flatMap { item -> generateSequence(item.dir.ifEmpty { null }) { it.substringBeforeLast('/', "").ifEmpty { null } }.map { s.folderPath + "/" + it } }
        }
        ShareMarks(folders = roots + subfolders, files = open.flatMap { s -> s.items.map { it.uri } }.toSet(), roots = roots)
    }.stateIn(scope, SharingStarted.Eagerly, ShareMarks())

    override fun makeFolderPrivate(path: String) {
        scope.launch { store.removeUnder(path) }
    }

    override fun addToSharedFolder(files: List<MediaFile>) {
        if (files.isNotEmpty()) scope.launch { store.addToFolder(files) }
    }

    override fun makeFilePrivate(uri: String) {
        scope.launch { store.removeFile(uri) }
    }
}
