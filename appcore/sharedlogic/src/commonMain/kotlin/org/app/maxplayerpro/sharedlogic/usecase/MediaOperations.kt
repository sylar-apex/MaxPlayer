package org.app.maxplayerpro.sharedlogic.usecase

import org.app.kmediaplayer.KMediaPlayer
import org.app.kmediaplayer.model.MediaFile
import org.app.kmediaplayer.platform.PlatformMedia
import org.app.kmediaplayer.settings.ObservableSettings
import org.app.maxplayerpro.sharedlogic.data.MediaRepository
import org.app.maxplayerpro.sharedlogic.data.PrivateFolderRepository
import org.app.maxplayerpro.sharedlogic.settings.AppKeys

/** File actions shared by every media list: play, share (to apps or on the network), delete, rename and lock in the private folder. */
class MediaOperations(
    private val settings: ObservableSettings,
    private val media: MediaRepository,
    private val privateFolder: PrivateFolderRepository,
) {
    fun play(files: List<MediaFile>, index: Int) = KMediaPlayer.playVideo(files, index)

    fun playMusic(files: List<MediaFile>, index: Int, shuffle: Boolean = false) = KMediaPlayer.playMusic(files, index, shuffle)

    fun share(files: List<MediaFile>) = PlatformMedia.share(files)

    fun shareOnNetwork(files: List<MediaFile>) = NetworkShareRequests.request(files)

    fun toast(message: String) = PlatformMedia.showToast(message)

    suspend fun delete(files: List<MediaFile>) {
        if (!settings[AppKeys.allowEdits]) { toast("Editing is disabled in Settings → General"); return }
        media.delete(files, settings[AppKeys.deleteSubtitlesTogether])
    }

    suspend fun rename(file: MediaFile, newName: String) {
        if (!settings[AppKeys.allowEdits]) { toast("Editing is disabled in Settings → General"); return }
        val name = if ('.' in newName) newName else "$newName.${file.extension}"
        if (!media.rename(file, name)) toast("Rename failed")
    }

    suspend fun lockInPrivate(files: List<MediaFile>) {
        if (!privateFolder.hasPin) { toast("Set up the Private Folder first (Me → Private Folder)"); return }
        val copied = files.filter { privateFolder.lockIn(it) }
        if (copied.isEmpty()) { toast("Could not move file"); return }
        media.delete(copied, withSubtitles = false)
        toast("${copied.size} file(s) locked in Private Folder")
    }
}
