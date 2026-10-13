package org.app.nxplayer.ui

import org.app.nxplayer.core.Keys
import org.app.nxplayer.core.Setting

/** Grouped rows; a null title renders the rows without a header. */
data class SettingGroup(val title: String?, val items: List<Setting<*>>)

data class TunerTab(val title: String, val groups: List<SettingGroup>)

/** The Display Settings tuner tabs observed in the player overflow and in Settings → Player / Subtitle. */
val tunerTabs = listOf(
    TunerTab("Style", listOf(SettingGroup(null, listOf(
        Keys.stylePreset, Keys.frameColor, Keys.controlNormalColor, Keys.controlHighlightColor,
        Keys.progressBarColor, Keys.progressBarStyle, Keys.progressBelowButtons,
    )))),
    TunerTab("Screen", listOf(
        SettingGroup(null, listOf(Keys.orientation, Keys.fullScreen, Keys.softButtons, Keys.brightnessEnabled, Keys.brightnessLevel)),
        SettingGroup("On-screen display", listOf(
            Keys.alwaysShowElapsed, Keys.alwaysShowStatus, Keys.cornerOffsetEnabled, Keys.cornerOffset,
            Keys.osdTextColor, Keys.osdBackground, Keys.osdBackColor, Keys.osdBottom,
        )),
        SettingGroup(null, listOf(
            Keys.rotationButton, Keys.batteryClockInTitle, Keys.showRemaining, Keys.keepScreenOn,
            Keys.pauseIfObscured, Keys.showInterfaceAtStartup,
        )),
    )),
    TunerTab("Controls", listOf(
        SettingGroup(null, listOf(Keys.touchAction, Keys.lockMode)),
        SettingGroup("Gestures", listOf(
            Keys.gSeek, Keys.gZoomAndPan, Keys.gZoom, Keys.gPan, Keys.gVolume, Keys.gBrightness,
            Keys.gDoubleTapPlayPause, Keys.gDoubleTapZoom, Keys.gDoubleTapFfRw, Keys.gLongPressSpeed, Keys.longPressSpeed,
            Keys.gPlaybackSpeed, Keys.gSubtitleUpDown, Keys.gSubtitleZoom,
        )),
        SettingGroup(null, listOf(Keys.interfaceAutoHide, Keys.interfaceAutoHideDelay, Keys.lockShowInterface)),
    )),
    TunerTab("Navigation", listOf(SettingGroup(null, listOf(
        Keys.seekSpeed, Keys.showMoveButtons, Keys.moveInterval, Keys.showPrevNext, Keys.displaySeekPosition,
    )))),
    TunerTab("Text", listOf(SettingGroup(null, listOf(
        Keys.subFont, Keys.subSize, Keys.subScale, Keys.subColor, Keys.subBold, Keys.subTextBackground, Keys.subTextBackgroundColor,
        Keys.subBorder, Keys.subBorderColor, Keys.subBorderThickness, Keys.subShadow, Keys.subFadeOut,
    )))),
    TunerTab("Layout", listOf(SettingGroup(null, listOf(
        Keys.subAlignment, Keys.subBottomMargin, Keys.subBackground, Keys.subBackgroundColor, Keys.subFitToVideo,
    )))),
)

enum class SettingsPage(val title: String) { List("List"), Player("Player"), Decoder("Decoder"), Audio("Audio"), Subtitle("Subtitle"), General("General") }

val settingsPages: Map<SettingsPage, List<SettingGroup>> = mapOf(
    SettingsPage.List to listOf(
        SettingGroup("Appearance", listOf(Keys.markLastPlayed, Keys.scrollToLast, Keys.selectThumbnail, Keys.floatingButton, Keys.newTaggedPeriod)),
        SettingGroup("Scan", listOf(Keys.scanFolders, Keys.fileExtensions, Keys.autoRefresh)),
    ),
    SettingsPage.Player to listOf(
        SettingGroup("Interface", listOf(Keys.doubleTapBack, Keys.quickZoom)),
        SettingGroup("Playback", listOf(
            Keys.resume, Keys.resumeOnlyFirst, Keys.defaultSpeed, Keys.rememberSelections, Keys.backToList, Keys.seekPreviews,
            Keys.fastSeek, Keys.playAlone, Keys.mediaButtons, Keys.multiPressNextPrev, Keys.prevNextToRewFf, Keys.smartPrev, Keys.suppressError,
        )),
        SettingGroup("Background Play", listOf(Keys.backgroundMode, Keys.backgroundAudio, Keys.albumArt, Keys.smoothSwitch)),
        SettingGroup("Miscellaneous", listOf(Keys.loadingCircle)),
    ),
    SettingsPage.Decoder to listOf(SettingGroup(null, listOf(Keys.defaultDecoder))),
    SettingsPage.Audio to listOf(SettingGroup(null, listOf(
        Keys.audioPlayer, Keys.volumeBoost, Keys.syncSystemVolume, Keys.pauseOnHeadsetDisconnect, Keys.fadeOnStart, Keys.fadeOnSeek,
        Keys.audioLanguage, Keys.audioDelay, Keys.equalizerEnabled,
    ))),
    SettingsPage.Subtitle to listOf(
        SettingGroup(null, listOf(Keys.subtitleFolder, Keys.subtitleCharset, Keys.subtitleLanguage, Keys.subtitleDefaultSync)),
        SettingGroup("Text processing", listOf(Keys.italicTag, Keys.forceLtr)),
    ),
    SettingsPage.General to listOf(
        SettingGroup(null, listOf(Keys.playMediaLinks, Keys.userAgent, Keys.quitButton)),
        SettingGroup("Edit", listOf(Keys.allowEdits, Keys.deleteSubtitlesTogether)),
        SettingGroup("User data", listOf(Keys.cacheThumbnail)),
    ),
)
