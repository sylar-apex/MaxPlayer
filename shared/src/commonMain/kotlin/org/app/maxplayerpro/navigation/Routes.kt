package org.app.maxplayerpro.navigation

import kotlinx.serialization.Serializable

/** Every destination of the main window. */
object Routes {
    @Serializable data object Home
    @Serializable data class Folder(val path: String)
    @Serializable data object Search
    @Serializable data object Music
    @Serializable data object Me
    @Serializable data object Theme
    @Serializable data object Settings
    @Serializable data class SettingsPage(val page: String)
    @Serializable data class Tuner(val tab: Int)
    @Serializable data object Playlists
    @Serializable data class PlaylistDetail(val id: Long)
    @Serializable data object PrivateFolder
    @Serializable data object MediaManager
    @Serializable data class MediaList(val kind: String)
    @Serializable data object Help
    @Serializable data object LocalNetwork
    @Serializable data object Gallery
    @Serializable data object Files
    @Serializable data class Unavailable(val title: String, val reason: String)
}
