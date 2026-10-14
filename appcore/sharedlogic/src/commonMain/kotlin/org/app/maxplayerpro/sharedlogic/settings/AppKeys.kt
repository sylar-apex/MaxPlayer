package org.app.maxplayerpro.sharedlogic.settings

import org.app.kmediaplayer.settings.BoolSetting
import org.app.kmediaplayer.settings.ChoiceSetting
import org.app.kmediaplayer.settings.IntSetting
import org.app.kmediaplayer.settings.PlayerKeys
import org.app.kmediaplayer.settings.SettingRegistry
import org.app.kmediaplayer.settings.TextSetting
import org.app.maxplayerpro.sharedlogic.model.ListViewOptions

/** App preferences outside the player: theme, media list, library scan and editing. Player preferences live in [PlayerKeys]. */
object AppKeys : SettingRegistry() {
    // ---- App theme ----
    val appTheme = r(ChoiceSetting("app_theme", "App Theme", "dark", listOf(
        "adaptive" to "Adaptive", "dark" to "Dark Theme", "light" to "Light Theme", "black" to "Amoled Black")))

    // ---- List ----
    val listViewOptions = r(TextSetting("list.view_options", "View options", ListViewOptions().encode()))
    val markLastPlayed = r(BoolSetting("mark_last_played_media_for_each_folders", "Last media in each folder", true, "Mark last played media in each folder."))
    val scrollToLast = r(BoolSetting("scroll_down_to_last_media", "Scroll down to last media", false, "Auto-scroll to position of the last played media."))
    val selectThumbnail = r(BoolSetting("selection_mode", "Select thumbnail", false, "Switch to selection mode by touching the thumbnail or icon."))
    val floatingButton = r(BoolSetting("list.floating_action_button", "Floating button", true, "Place a floating button on the bottom right corner of the screen to start last played media."))
    val newTaggedPeriod = r(IntSetting("new_tagged_period", "Period tagged as \"NEW\"", 7, 0..365, "\"NEW\" tag will be displayed if the file was modified within this period.", "days"))
    val scanFolders = r(TextSetting("list.scan_folders", "Folders", "", "Select folders to be included in the media library. Empty = whole device."))
    val fileExtensions = r(TextSetting("list.extensions", "File extensions", "", "Extra file extensions to include (comma separated)."))
    val autoRefresh = r(BoolSetting("list.refresh_auto", "Refresh automatically", true, "Refresh the media library whenever the list is shown."))

    // ---- General ----
    val quitButton = r(BoolSetting("quit_button", "Quit Button", false, "Display Quit button on the Me page."))
    val allowEdits = r(BoolSetting("allow_edits", "Allow editing", true, "Enable edit menu which allows you to delete or rename files and folders."))
    val deleteSubtitlesTogether = r(BoolSetting("delete_subtitle_files_together", "Delete subtitle files together", true))
    val cacheThumbnail = r(BoolSetting("cache_thumbnail", "Cache thumbnail", true))
}

/** Every registry stored in the app's settings file. */
val appSettingRegistries = listOf(PlayerKeys, AppKeys)
