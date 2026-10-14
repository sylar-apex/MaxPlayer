package org.app.maxplayerpro.player.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.app.kmediaplayer.host.PlayerHost
import org.app.kmediaplayer.model.Bookmark
import org.app.kmediaplayer.model.FileSelections
import org.app.kmediaplayer.model.MediaFile
import org.app.kmediaplayer.model.PlaybackRecord
import org.app.kmediaplayer.model.SubtitleRef
import org.app.kmediaplayer.util.nowMs
import org.app.maxplayerpro.sharedlogic.data.MediaRepository
import org.app.maxplayerpro.sharedlogic.data.UserDataRepository
import org.app.maxplayerpro.sharedlogic.platform.AppScreen
import org.app.maxplayerpro.sharedlogic.platform.openAppScreen

/** Connects the media player library to Max Player's history, bookmarks, favourites and media library. */
class MaxPlayerHost(
    private val userData: UserDataRepository,
    private val media: MediaRepository,
) : PlayerHost {
    override val appName: String = "Max Player Pro"

    override fun playbackRecord(uri: String): PlaybackRecord? = userData.value.history[uri]

    override fun recordPlayback(file: MediaFile, positionMs: Long, durationMs: Long) =
        userData.update { it.recordPlayback(file.uri, file.folderPath, positionMs, durationMs, nowMs()) }

    override fun selections(uri: String): FileSelections? = userData.value.selections[uri]

    override fun updateSelections(uri: String, transform: (FileSelections) -> FileSelections) =
        userData.update { it.updateSelections(uri, transform) }

    override fun bookmarks(uri: String): Flow<List<Bookmark>> = userData.data.map { it.bookmarks[uri].orEmpty() }.distinctUntilChanged()
    override fun addBookmark(uri: String, positionMs: Long) = userData.update { it.addBookmark(uri, positionMs, nowMs()) }
    override fun removeBookmark(uri: String, positionMs: Long) = userData.update { it.removeBookmark(uri, positionMs) }

    override fun favourites(): Flow<Set<String>> = userData.data.map { it.favourites }.distinctUntilChanged()
    override fun toggleFavourite(uri: String) = userData.update { it.toggleFavourite(uri) }

    override fun findFile(uri: String): MediaFile? = media.findByUri(uri)
    override fun subtitleFiles(file: MediaFile): List<SubtitleRef> = media.subtitleFilesFor(file)

    override suspend fun deleteFile(file: MediaFile): Boolean = media.delete(listOf(file), withSubtitles = false)
    override suspend fun renameFile(file: MediaFile, newName: String): Boolean = media.rename(file, newName)

    override fun openSettings() = openAppScreen(AppScreen.Settings)
    override fun openHelp() = openAppScreen(AppScreen.Help)
}
