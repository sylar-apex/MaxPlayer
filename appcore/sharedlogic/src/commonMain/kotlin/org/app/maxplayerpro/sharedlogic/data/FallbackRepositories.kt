package org.app.maxplayerpro.sharedlogic.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.app.kmediaplayer.model.MediaFile
import org.app.kmediaplayer.model.SubtitleRef
import org.app.maxplayerpro.sharedlogic.model.PrivateFile
import org.app.maxplayerpro.sharedlogic.model.StorageSummary

/** Media library for platforms without device scanning yet: shows whatever [scan] returns. */
class SimpleMediaRepository(private val scan: () -> List<MediaFile> = { emptyList() }) : MediaRepository {
    private val _videos = MutableStateFlow<List<MediaFile>>(emptyList())
    override val videos: StateFlow<List<MediaFile>> = _videos.asStateFlow()
    private val _audio = MutableStateFlow<List<MediaFile>>(emptyList())
    override val audio: StateFlow<List<MediaFile>> = _audio.asStateFlow()
    override val noMediaDirs: StateFlow<Set<String>> = MutableStateFlow(emptySet())
    private val _loading = MutableStateFlow(false)
    override val loading: StateFlow<Boolean> = _loading.asStateFlow()

    override suspend fun refresh() {
        _loading.value = true
        val all = runCatching(scan).getOrDefault(emptyList())
        _videos.value = all.filterNot { it.isAudio }
        _audio.value = all.filter { it.isAudio }
        _loading.value = false
    }

    override fun findByUri(uri: String) = (_videos.value + _audio.value).firstOrNull { it.uri == uri }
    override fun subtitleFilesFor(file: MediaFile): List<SubtitleRef> = emptyList()
    override fun folderFileNames(folders: Collection<String>): Set<String> = emptySet()
    override suspend fun delete(files: List<MediaFile>, withSubtitles: Boolean) = false
    override suspend fun rename(file: MediaFile, newName: String) = false
    override suspend fun storageSummary(): StorageSummary? = null
}

/** Private folder kept only in memory, for platforms without app-private storage support yet. */
class MemoryPrivateFolder : PrivateFolderRepository {
    private var pin: String? = null
    override val files: StateFlow<List<PrivateFile>> = MutableStateFlow(emptyList())
    override val hasPin: Boolean get() = pin != null
    override fun setPin(pin: String) { this.pin = pin }
    override fun checkPin(pin: String) = this.pin == pin
    override suspend fun lockIn(file: MediaFile) = false
    override suspend fun restore(file: PrivateFile) = false
    override fun delete(file: PrivateFile) {}
    override fun rename(file: PrivateFile, name: String) = false
}
