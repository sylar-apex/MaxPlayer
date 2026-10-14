package org.app.maxplayerpro.settings.domain.model

import org.app.kmediaplayer.settings.PlayerKeys
import org.app.kmediaplayer.settings.SettingGroup
import org.app.maxplayerpro.sharedlogic.settings.AppKeys

enum class SettingsPage(val title: String) { List("List"), Player("Player"), Decoder("Decoder"), Audio("Audio"), Subtitle("Subtitle"), General("General") }

/** The rows of each settings page, grouped under their section headers. */
val settingsPages: Map<SettingsPage, List<SettingGroup>> = mapOf(
    SettingsPage.List to listOf(
        SettingGroup("Appearance", listOf(AppKeys.markLastPlayed, AppKeys.scrollToLast, AppKeys.selectThumbnail, AppKeys.floatingButton, AppKeys.newTaggedPeriod)),
        SettingGroup("Scan", listOf(AppKeys.scanFolders, AppKeys.fileExtensions, AppKeys.autoRefresh)),
    ),
    SettingsPage.Player to listOf(
        SettingGroup("Interface", listOf(PlayerKeys.doubleTapBack, PlayerKeys.quickZoom)),
        SettingGroup("Playback", listOf(
            PlayerKeys.resume, PlayerKeys.resumeOnlyFirst, PlayerKeys.defaultSpeed, PlayerKeys.rememberSelections, PlayerKeys.backToList, PlayerKeys.seekPreviews,
            PlayerKeys.fastSeek, PlayerKeys.playAlone, PlayerKeys.mediaButtons, PlayerKeys.multiPressNextPrev, PlayerKeys.prevNextToRewFf, PlayerKeys.smartPrev, PlayerKeys.suppressError,
        )),
        SettingGroup("Background Play", listOf(PlayerKeys.backgroundMode, PlayerKeys.backgroundAudio, PlayerKeys.albumArt, PlayerKeys.smoothSwitch)),
        SettingGroup("Miscellaneous", listOf(PlayerKeys.loadingCircle)),
    ),
    SettingsPage.Decoder to listOf(SettingGroup(null, listOf(PlayerKeys.defaultDecoder))),
    SettingsPage.Audio to listOf(SettingGroup(null, listOf(
        PlayerKeys.audioPlayer, PlayerKeys.volumeBoost, PlayerKeys.syncSystemVolume, PlayerKeys.pauseOnHeadsetDisconnect, PlayerKeys.fadeOnStart, PlayerKeys.fadeOnSeek,
        PlayerKeys.audioLanguage, PlayerKeys.audioDelay, PlayerKeys.equalizerEnabled,
    ))),
    SettingsPage.Subtitle to listOf(
        SettingGroup(null, listOf(PlayerKeys.subtitleFolder, PlayerKeys.subtitleCharset, PlayerKeys.subtitleLanguage, PlayerKeys.subtitleDefaultSync)),
        SettingGroup("Text processing", listOf(PlayerKeys.italicTag, PlayerKeys.forceLtr)),
    ),
    SettingsPage.General to listOf(
        SettingGroup(null, listOf(PlayerKeys.playMediaLinks, PlayerKeys.userAgent, AppKeys.quitButton)),
        SettingGroup("Edit", listOf(AppKeys.allowEdits, AppKeys.deleteSubtitlesTogether)),
        SettingGroup("User data", listOf(AppKeys.cacheThumbnail)),
    ),
)
