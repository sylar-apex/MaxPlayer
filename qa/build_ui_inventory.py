#!/usr/bin/env python3
"""QA control inventory workbook generated from source-read labels. Not a runtime scan."""
from openpyxl import Workbook
from openpyxl.styles import Alignment, Border, Font, PatternFill, Side
from openpyxl.utils import get_column_letter
from openpyxl.worksheet.datavalidation import DataValidation
from openpyxl.formatting.rule import FormulaRule
from openpyxl.chart.shapes import GraphicalProperties
from openpyxl.workbook.properties import CalcProperties

OUT = "qa/MaxPlayer_UI_Control_Inventory.xlsx"

HEADER = Font(name="Calibri", bold=True, color="FFFFFF", size=11)
HEADER_FILL = PatternFill("solid", fgColor="1F4E79")
ALT = PatternFill("solid", fgColor="F2F7FB")
WRAP = Alignment(wrap_text=True, vertical="top")
THIN = Border(
    left=Side(style="thin", color="D0D7DE"),
    right=Side(style="thin", color="D0D7DE"),
    top=Side(style="thin", color="D0D7DE"),
    bottom=Side(style="thin", color="D0D7DE"),
)
TITLE = Font(name="Calibri", bold=True, size=16, color="1F4E79")
SUB = Font(name="Calibri", size=11, color="334155")

COLS = [
    "ID",
    "Surface",
    "Kind",
    "Exact label",
    "When it shows",
    "Expected behavior",
    "Must not happen",
    "Platform",
    "PlayerKeys / flags",
    "Source",
]

# Platform shorthand used in the Platform column.
A = "Android"
I = "iOS"
D = "Desktop"
W = "Web"
ALL = "All"
AW = "Android, Web (PiP if browser supports it)"

rows = []

def add(surface, kind, label, shows, behavior, must_not, platform, keys, source):
    rows.append((surface, kind, label, shows, behavior, must_not, platform, keys, source))

# --- Video gestures ---
add("Video player", "Gesture", "Single tap",
    "Not locked, not PiP, not a committed drag/pinch/long-press. Delayed 300ms if any double-tap flag is on.",
    "touchAction toggle (default): close open panel, else show/hide controls. pause: toggle play only. both: toggle play and show controls.",
    "Must not seek, change volume, or unlock. Must not run while locked or in PiP.",
    ALL, "PlayerKeys.touchAction (touch_action): toggle | pause | both",
    "VideoPlayerScreen.kt:451-480")
add("Video player", "Gesture", "Double tap center",
    "Not locked. At least one of gDoubleTapPlayPause, gDoubleTapFfRw, gDoubleTapZoom. Hit is middle third of width.",
    "If gDoubleTapZoom: cycleZoom (toast is zoom.name: Fit/Stretch/Crop/Original). Else if gDoubleTapPlayPause: toggle play and center pill Playing or Paused for 800ms. If neither flag, no-op.",
    "Must not rewind/forward. Must not play/pause if only zoom flag is on.",
    ALL, "g.dt_zoom, g.dt_play_pause",
    "VideoPlayerScreen.kt:451-469; PlayerLogic.kt:93-98")
add("Video player", "Gesture", "Double tap left / right",
    "Not locked. Same double-tap enablement. Left third = rewind, right third = forward.",
    "If gDoubleTapFfRw (default true): seek ±moveInterval seconds (default 10). Side badge '{seconds}s' accumulates same direction. Else if gDoubleTapPlayPause: toggle play with no pill.",
    "Must not seek if gDoubleTapFfRw is off. Sides must not play/pause when FF/RW is on (FF/RW wins).",
    ALL, "g.dt_ffrw, move_interval, g.dt_play_pause",
    "VideoPlayerScreen.kt:461-468")
add("Video player", "Gesture", "Swipe left/right (seek)",
    "Not locked, not PiP. Horizontal drag past 24px slop and |dx|>|dy|. gSeek default true.",
    "Offset = drag cm × seekSpeed sec/cm (default 10, range 1–60). Center OSD shows target duration and [+mm:ss] or [-mm:ss]. Seek commits on finger up. Controls show if hidden. Live position only if displaySeekPosition.",
    "Must not seek if gSeek is false. Must not move the thumb live if displaySeekPosition is false. Must not run on a vertical-dominant drag.",
    ALL, "g.seek, gesture_seek_speed, display_seeking_position",
    "VideoPlayerScreen.kt:405-440; PlayerLogic.kt:74-87")
add("Video player", "Gesture", "Swipe up/down left (brightness)",
    "Not locked. Start X < half width. Vertical-dominant. gBrightness default true.",
    "Level 0–30 over a full-height drag. Left level pill with brightness icon. On release writes brightnessLevel. Android sets window screenBrightness = level/30 (0.01–1) if brightnessEnabled. Desktop/web picture factor = brightness/24 clamped 0.15–1.25. iOS setBrightness is a no-op; picture factor still applied in Compose window.",
    "Must not change volume. Must not run on the right half. Must not change Android system brightness if brightnessEnabled is false (gesture still updates state and calls setBrightness only from the gesture path — activity applyScreenSettings gates the initial apply).",
    ALL, "g.brightness, screen_brightness, screen_brightness_enabled",
    "VideoPlayerScreen.kt:419-443; KMediaPlayerActivity.kt:147-171; ComposePlayerWindow.kt:67")
add("Video player", "Gesture", "Swipe up/down right (volume)",
    "Not locked. Start X >= half width. Vertical-dominant. gVolume default true.",
    "Delta uses system max over full height. If volumeBoost (default true) max level is 2× system max; pill turns orange above the boost line. Unmutes if increasing while muted. Right level pill.",
    "Must not change brightness. Must not exceed 100% if volume boost is off. Must not run on the left half.",
    ALL, "g.volume, volume_boost",
    "VideoPlayerScreen.kt:413-417; PlayerLogic.kt:119-126")
add("Video player", "Gesture", "Pinch zoom",
    "Not locked. Two fingers. Outside subtitle zone, or subtitle zone with gSubtitleZoom off.",
    "pinchScale 0.25–4 if gZoom or gZoomAndPan. Center label '{percent}%'. Snaps to 100% if released in 95–105%. Label clears after 600ms. Web applies transform on the HTML video.",
    "Must not zoom if both gZoom and gZoomAndPan are false. Must not zoom subtitles when the pinch is outside the subtitle zone.",
    ALL, "g.zoom (default true), g.zoom_and_pan (default false)",
    "VideoPlayerScreen.kt:367-377, 431-434")
add("Video player", "Gesture", "Two-finger pan",
    "Not locked. Two fingers, not subtitle-zoom mode.",
    "panX/panY change if gPan or gZoomAndPan.",
    "Must not pan if both flags are false (defaults: gPan false, gZoomAndPan false — pan is off unless a flag is turned on).",
    ALL, "g.pan, g.zoom_and_pan",
    "VideoPlayerScreen.kt:374-375")
add("Video player", "Gesture", "Pinch subtitle zoom",
    "Not locked. Two fingers. Start Y > 72% of height. gSubtitleZoom default true.",
    "subtitleZoom 0.5–3. No percent label.",
    "Must not also pinch-zoom the video in that gesture. Must not run if flag is off.",
    ALL, "g.subtitle_zoom",
    "VideoPlayerScreen.kt:371-372")
add("Video player", "Gesture", "Drag subtitles up/down",
    "Not locked. Start in bottom 28%. Vertical-dominant past slop. Visible subtitle lines. gSubtitleUpDown default true.",
    "Moves subtitleOffsetY. Wins over seek/volume classification.",
    "Must not seek or change volume/brightness on that drag. Must not move subtitles if no lines are visible.",
    ALL, "g.subtitle_updown",
    "VideoPlayerScreen.kt:391-404")
add("Video player", "Gesture", "Long press (speed FF)",
    "Not locked. Playing. Finger stays inside 24px slop past the view long-press timeout. gLongPressSpeed default true.",
    "Temporary speed = longPressSpeed (default 2.0, range 1.25–4). Top pill '{n.n}X Speed Playing'. Release restores previous speed and clears the pill.",
    "Must not engage while paused, after the finger has dragged past slop, or while locked. Must not seek.",
    ALL, "g.long_press_speed, long_press_speed_value",
    "VideoPlayerScreen.kt:384-389, 430")
add("Video player", "Gesture", "Mouse wheel / trackpad scroll zoom",
    "Not locked. Scroll event. zoomBy also requires gZoom or gZoomAndPan.",
    "Wheel up zooms in, down zooms out. Per-event factor capped 0.8–1.25. Step 0.1 on Android/iOS/desktop; 0.001 on web.",
    "Must not zoom while locked or if both zoom flags are off.",
    ALL, "g.zoom, g.zoom_and_pan; wheelZoomStep",
    "VideoPlayerScreen.kt:329-339; ZoomInput.kt:8-13")
add("Video player", "Gesture", "macOS trackpad pinch",
    "Desktop macOS only, and only if java.desktop export is present. Targets the on-screen player via ZoomInput.",
    "Calls zoomBy(1+magnification), same gates as wheel zoom.",
    "Must not run on Windows/Linux/Android/iOS/web. If the export is missing it logs and does nothing.",
    "Desktop macOS", "g.zoom or g.zoom_and_pan",
    "PlatformVideoTools.desktop.kt:40-61")
add("Video player", "Setting (unwired)", "Playback Speed gesture",
    "Checkbox exists in Display Settings → Controls → Gestures. Default true.",
    "No swipe changes playback speed. Speed is changed only by the Speed panel, keyboard [ ], music speed sheet, or defaultSpeed.",
    "Must not expect a vertical or horizontal playback-speed gesture.",
    ALL, "g.playback_speed",
    "PlayerKeys.kt:103; Tuner.kt:30")

# --- OSD ---
add("Video OSD", "OSD", "Tap-seek badge '{seconds}s'",
    "After double-tap FF/RW or keyboard seek. Side of the seek direction.",
    "Shows accumulated seconds with ff or rew icon. Clears after ~700ms (gesture) or 1500ms (effect).",
    "Must not shade half the picture. Must not show for digit-key seeks.",
    ALL, "move_interval",
    "VideoPlayerScreen.kt:224, 504-515")
add("Video OSD", "OSD", "Playing / Paused pill",
    "Center double-tap play/pause only.",
    "Center pill 'Playing' or 'Paused' for 800ms.",
    "Must not show for keyboard Space/K or for side double-tap play/pause fallback.",
    ALL, "g.dt_play_pause",
    "VideoPlayerScreen.kt:459, 488-500")
add("Video OSD", "OSD", "{n.n}X Speed Playing",
    "While long-press speed is held. Top center.",
    "Shows long-press speed. Clears on release.",
    "Must not remain after finger up.",
    ALL, "long_press_speed_value",
    "VideoPlayerScreen.kt:225, 388")
add("Video OSD", "OSD", "Level pill (volume or brightness)",
    "During volume/brightness gesture or volume/brightness keys.",
    "Number, vertical bar, volume or brightness icon. Volume on the left, brightness on the right. Boost range drawn orange.",
    "Must not swap sides.",
    ALL, "volume_boost",
    "VideoPlayerScreen.kt:226, 519-541")
add("Video OSD", "OSD", "Seek position overlay",
    "During horizontal seek drag.",
    "Large duration plus [+duration] or [-duration].",
    "Must not commit the seek until finger up.",
    ALL, "g.seek",
    "VideoPlayerScreen.kt:545-550")
add("Video OSD", "OSD", "Pinch percent label",
    "During video pinch or wheel zoom.",
    "Text '{percent}%'. Clears ~600ms after release.",
    "Must not show for subtitle pinch.",
    ALL, "g.zoom",
    "VideoPlayerScreen.kt:228-230, 376")
add("Video OSD", "OSD", "Persistent elapsed / clock",
    "Controls hidden, not locked, not PiP. At least one of alwaysShowElapsed or alwaysShowStatus.",
    "Elapsed time and/or HH:mm plus optional battery '  🔋{pct}%'. Top-start unless osdBottom. Corner offset if enabled.",
    "Must not show while controls are visible or while locked. Must not show if both flags are off (defaults are off).",
    ALL, "osd.always_elapsed, osd.always_status, osd.bottom, corner_offset_enable",
    "VideoPlayerScreen.kt:221, 571-586")
add("Video OSD", "OSD", "Toast / snack",
    "After an action sets toast or snack, or a platform notice arrives.",
    "Toast 1500ms bottom. Snack 3000ms bottom bar.",
    "Must not persist.",
    ALL, "",
    "VideoPlayerScreen.kt:167-168, 232-239")
add("Video OSD", "OSD", "Loading circle",
    "Buffering and loadingCircle true (default).",
    "White circular progress at center.",
    "Must not show if loading circle setting is off, or when not buffering.",
    ALL, "loading_circle_animation",
    "VideoPlayerScreen.kt:213")
add("Video OSD", "OSD", "Night mode overlay",
    "After Night Mode shortcut turns it on.",
    "Full-screen 60% black overlay. Web also dims the HTML video (factor × 0.45).",
    "Must not dim the picture when night mode is off.",
    ALL, "",
    "VideoPlayerScreen.kt:207; ComposePlayerWindow.kt:67")

# --- Top bar ---
add("Video controls", "Button", "Navigate up",
    "Controls visible, not locked, not PiP. Panel is None or A-B.",
    "Closes the player. Android finishes the activity and stops the engine. Desktop/web/iOS stop, clear items, and close the window.",
    "Must not only hide controls.",
    ALL, "",
    "VideoPlayerScreen.kt:611")
add("Video controls", "Text", "Title",
    "Same as controls.",
    "Shows st.title, one line, ellipsis.",
    "Must not wrap to two lines.",
    ALL, "",
    "VideoPlayerScreen.kt:613")
add("Video controls", "Text", "Battery/clock in title",
    "Controls visible and batteryClockInTitle (default false).",
    "Same clock/battery string as persistent OSD.",
    "Must not show when the flag is off.",
    ALL, "battery_clock_in_title_bar",
    "VideoPlayerScreen.kt:614")
add("Video controls", "Button", "Audio Track",
    "Controls visible and audioTrackSelection.",
    "Opens Audio Track panel.",
    "Hidden on platforms without the capability. iOS and Android and desktop-with-VLC and web show it. Desktop without VLC hides it.",
    "Android, iOS, Desktop if VLC, Web", "PlayerCapabilities.audioTrackSelection",
    "VideoPlayerScreen.kt:616")
add("Video controls", "Button", "Subtitles",
    "Controls visible. All platforms.",
    "Opens Subtitle panel.",
    "Must not be hidden when there are no tracks; the panel says No subtitles.",
    ALL, "",
    "VideoPlayerScreen.kt:617")
add("Video controls", "Button", "HW / HW+ / SW",
    "Controls visible and decoderChoice. Label is the current decoder label, not the word Decoder.",
    "Opens Select decoder dialog.",
    "Hidden on iOS and web. Hidden on desktop if VLC is unavailable.",
    "Android, Desktop if VLC", "PlayerCapabilities.decoderChoice; default_decoder",
    "VideoPlayerScreen.kt:618")
add("Video controls", "Button", "More",
    "Controls visible. All platforms.",
    "Opens overflow grid.",
    "Must not open app Settings directly.",
    ALL, "",
    "VideoPlayerScreen.kt:619")

# shortcuts
for label, when, behavior, must, plat, keys, src in [
    ("Night Mode", "Shortcut row if shortcutsVisible and item enabled. Label text only when row expanded.",
     "Toggles night overlay. Toast 'Night mode on' or 'Night mode off'.",
     "Must not change system night mode.", ALL, "osd.shortcuts, osd.shortcut_items", "VideoPlayerScreen.kt:764"),
    ("Customise Items", "Expanded shortcut row only (always included when expanded).",
     "Opens Customise Items panel.",
     "Must not appear in the collapsed row.", ALL, "osd.shortcut_items", "VideoPlayerScreen.kt:765"),
    ("Shuffle", "Shortcut row if enabled in shortcut items.",
     "Toast 'Shuffle on' or 'Shuffle off'. Next picks a random other index. Does not reorder the queue.",
     "Must not affect music shuffle.", ALL, "", "VideoPlayerScreen.kt:766; VideoPlayerState.kt:290-294"),
    ("Loop", "Shortcut row if enabled. Icon becomes loop-one when Repeat one.",
     "Cycles Loop off → Repeat one → Repeat all. Toast is the mode label. On end: One restarts file; All restarts queue at item 0; Off does nothing unless backToList then closes.",
     "Must not use the music Repeat Off/All/One engine mode.", ALL, "back_to_list", "VideoPlayerScreen.kt:767; VideoPlayerState.kt:251-257, 349"),
    ("Mute", "Shortcut row if enabled. Icon switches to mute when muted.",
     "Engine volume 0 or 1. Toast 'Mute' or 'Unmute'.",
     "Must not change system volume level.", ALL, "", "VideoPlayerScreen.kt:768; VideoPlayerState.kt:347"),
    ("Sleep Timer", "Shortcut row if enabled. Highlighted while a timer is running.",
     "Opens Sleep Timer panel.",
     "Must not start a timer by itself.", ALL, "", "VideoPlayerScreen.kt:769"),
    ("A - B Repeat", "Shortcut row if enabled. Highlighted once A is set.",
     "Opens A-B card.",
     "Must not set A by itself.", ALL, "", "VideoPlayerScreen.kt:770"),
    ("Equalizer", "Shortcut row if enabled AND equalizer capability.",
     "Opens Equalizer panel.",
     "Hidden on iOS, web, and desktop without VLC. Music Now Playing still shows its own equalizer button.",
     "Android, Desktop if VLC", "PlayerCapabilities.equalizer", "VideoPlayerScreen.kt:771; PlayerCapabilities.kt:32-37"),
    ("Speed", "Shortcut row if enabled. Shows speed text such as 1X when used as the button face.",
     "Opens speed panel.",
     "Must not change speed by itself.", ALL, "default_playback_speed", "VideoPlayerScreen.kt:734, 772"),
    ("Screenshot", "Shortcut row if enabled. Available on all platforms; result differs.",
     "Android: PNG in Pictures/MaxPlayerPro, snack 'Screenshot saved to {path}'. Desktop: ~/Pictures/MaxPlayer/MaxPlayer_yyyyMMdd_HHmmss.png. Web: downloads MaxPlayer_{ms}.png. iOS toast 'Screenshot is not available on iOS yet'. Failures: 'Screenshot unavailable', 'Screenshot failed', 'No frame to capture yet', 'Screenshots aren't allowed for videos from other websites'.",
     "Must not claim a saved file on iOS.", ALL, "", "VideoPlayerScreen.kt:773; PlatformVideoTools.*.kt"),
    ("Background Play", "Shortcut row if enabled AND backgroundPlay capability (Android only).",
     "Toggles session flag. Toast 'Background play on' or 'off'. Android onStop keeps audio if this flag is on, or if videoW==0 and backgroundAudio.",
     "Hidden on iOS, desktop, and web. Must not change the Background/PiP mode setting.",
     A, "PlayerCapabilities.backgroundPlay; sticky_video; sticky_audio", "VideoPlayerScreen.kt:774; KMediaPlayerActivity.kt:220-230"),
    ("Screen Rotation", "Shortcut row if enabled AND screenRotation (Android only).",
     "Flips sensor landscape/portrait.",
     "Hidden on iOS, desktop, and web. rotationButton setting is never read by this row.",
     A, "PlayerCapabilities.screenRotation; screen_rotation_button is unwired", "VideoPlayerScreen.kt:775; KMediaPlayerActivity.kt:190-193"),
]:
    add("Video shortcuts", "Shortcut", label, when, behavior, must, plat, keys, src)

add("Video shortcuts", "Button", "Expand shortcuts",
    "Shortcut row visible.",
    "Toggles collapsed vs expanded row. Collapsed order: Equalizer, Speed, Screenshot, Background Play, Screen Rotation, filtered by availability and shortcutItems.",
    "Must not show unavailable shortcuts.",
    ALL, "osd.shortcuts",
    "VideoPlayerScreen.kt:717-742; PlayerKeys.kt:11-13")

# bottom transport
add("Video transport", "Button", "Lock",
    "Bottom transport, controls visible.",
    "Sets locked and closes any panel.",
    "Must not unlock. Kids vs normal unlock is decided later by lockMode.",
    ALL, "lock_mode",
    "VideoPlayerScreen.kt:630")
add("Video transport", "Button", "Rewind",
    "showMoveButtons default false.",
    "Seeks −moveInterval seconds (default 10, range 1–120).",
    "Hidden unless the setting is on. Must not go to the previous file.",
    ALL, "show_move_buttons, move_interval",
    "VideoPlayerScreen.kt:632")
add("Video transport", "Button", "Forward",
    "showMoveButtons default false.",
    "Seeks +moveInterval seconds.",
    "Hidden unless the setting is on.",
    ALL, "show_move_buttons, move_interval",
    "VideoPlayerScreen.kt:636")
add("Video transport", "Button", "Previous",
    "showPrevNext default true.",
    "If prevNextToRewFf: seek −moveInterval. Else if smartPrev (default true) and position > 3s, or no previous item: seek 0. Else previous item.",
    "Must not skip to the previous file when more than 3s in and smartPrev is on.",
    ALL, "show_prev_next, smart_prev, prevnext_to_rewff",
    "VideoPlayerScreen.kt:633; VideoPlayerState.kt:297-300")
add("Video transport", "Button", "Next",
    "showPrevNext default true.",
    "If prevNextToRewFf: seek +moveInterval. Else shuffle picks another index if queue > 1. Else next item. Else toast 'Last item'.",
    "Must not wrap to the first item unless Loop is Repeat all and playback ends.",
    ALL, "show_prev_next, prevnext_to_rewff",
    "VideoPlayerScreen.kt:635; VideoPlayerState.kt:290-294")
add("Video transport", "Button", "Play/Pause",
    "Always on the transport.",
    "Pauses if playing. If ended, seeks 0 then plays. Icon is pause while playing else play.",
    "Must not close the player.",
    ALL, "",
    "VideoPlayerScreen.kt:634; VideoPlayerState.kt:277-279")
add("Video transport", "Button", "Zoom",
    "Always on the transport.",
    "Cycles Fit → Stretch → Crop → Original. Resets pinch and pan. Toast is zoom.name (Fit, Stretch, Crop, Original), not the longer labels Fit to screen / 100%.",
    "Must not open the Aspect Ratio panel.",
    ALL, "remember_selections stores zoom",
    "VideoPlayerScreen.kt:639; VideoPlayerState.kt:329-332; PlayerLogic.kt:33-36")
add("Video transport", "Button", "Popup",
    "pictureInPicture capability. Android always; web only if document.pictureInPictureEnabled.",
    "Enters PiP. Failure toast 'Picture-in-picture not supported'. Android also auto-enters PiP on leaving the app if playing, background play flag off, and backgroundMode is pip.",
    "Hidden on iOS and desktop. In PiP, transport, panels, dialogs, and lock UI must be hidden. Subtitles remain at 0.4 scale.",
    "Android; Web if PiP enabled", "PlayerCapabilities.pictureInPicture; sticky_video",
    "VideoPlayerScreen.kt:215, 640; KMediaPlayerActivity.kt:196-212")
add("Video transport", "Button", "Full screen / Exit full screen",
    "fullscreenButton: desktop and web only.",
    "Toggles document or window fullscreen. Icon and a11y name follow isFullscreen.",
    "Must not appear on Android or iOS.",
    "Desktop, Web", "PlayerCapabilities.fullscreenButton",
    "VideoPlayerScreen.kt:641-646")
add("Video transport", "Seek bar", "Elapsed / remaining",
    "Seek row with controls. Right side is remaining if showRemaining (default true), else total duration.",
    "Drag updates the thumb; seek commits on release, not live. A-B caption 'A-B: {a} → {b or …}' if A is set. Bar is below buttons only if progressBelowButtons.",
    "Must not seek live while dragging the bar (unlike the swipe gesture).",
    ALL, "show_left_time, osd.progress_below, osd.progress_style, osd.progress_color",
    "VideoPlayerScreen.kt:673-690")
add("Video transport", "Notice", "Continue from where you stopped.",
    "After automatic resume (resume setting = Resume), for 5 seconds.",
    "Close dismisses. START OVER seeks to 0.",
    "Must not appear for Ask (that uses the dialog) or Start over.",
    ALL, "resume_last = resume",
    "VideoPlayerScreen.kt:656-668")

# lock
add("Video lock", "Button", "Unlock",
    "While locked. Icon shows for 2.5s after any touch.",
    "Normal lock (lockMode lock): tap or long-press unlocks and shows controls. Kids lock: tap toast 'Long press to unlock'; long-press unlocks.",
    "Kids lock must not unlock on tap. Gestures must not seek/zoom/volume while locked. Full transport must stay hidden while locked even if lock_show_interface is on (that flag only refreshes the lock icon).",
    ALL, "lock_mode (Lock | Kids lock); lock_show_interface",
    "VideoPlayerScreen.kt:217-218, 356-360, 554-567")
add("Video lock", "System", "Android back while locked",
    "Android activity, player locked.",
    "Calls showControls(). Does not unlock and does not close.",
    "Must not exit or unlock.",
    A, "double_tap_back_key does not apply while locked",
    "KMediaPlayerActivity.kt:86-96")
add("Video lock", "System", "Press back again to exit",
    "Android, not locked, no panel, doubleTapBack true, more than 2s since last back.",
    "Toast 'Press back again to exit'. Second back within 2s closes.",
    "Must not require a double back if the setting is off.",
    A, "double_tap_back_key",
    "KMediaPlayerActivity.kt:91-95")

# dialogs
add("Video dialog", "Dialog", "Do you wish to resume from where you stopped?",
    "resume setting Ask, saved position at least 5s from both ends, and (if resume only first) this is the first file.",
    "RESUME seeks to the saved position and plays. START OVER or dismiss plays from the current position (does not seek).",
    "Must not appear for HTTP/RTSP (progress is not saved). Must not appear within 5s of start or end. Default setting is Resume, which skips this dialog.",
    ALL, "resume_last, resume_only_first",
    "VideoPlayerScreen.kt:241-250; PlayerLogic.kt:131-144; VideoPlayerState.kt:231-248")
add("Video dialog", "Dialog", "Can't play this media.",
    "Engine error, and suppressError did not skip to next.",
    "Text includes the error code name. OK goes next if there is a next item, else closes. TRY SW DECODER only if the error is not IO and decoder is not already SW.",
    "Must not offer TRY SW DECODER for IO errors or when already on SW. If suppressError and hasNext, this dialog must not appear.",
    ALL, "suppress_error_message",
    "VideoPlayerScreen.kt:251-258; VideoPlayerState.kt:135-137")
add("Video dialog", "Dialog", "All files access",
    "Error needsFileAccess.",
    "Text: This file type needs \"All files access\". Allow it so Max Player can open every video format on your device. ALLOW starts the Android settings screen on API 30+; if the platform returns false the window closes. CANCEL closes. If access is granted on resume, playback retries; otherwise the activity closes.",
    "Must not appear on desktop/web/iOS as a working grant; those close because requestFileAccess returns false.",
    "Dialog all; grant Android 11+", "",
    "VideoPlayerScreen.kt:265-272; KMediaPlayerActivity.kt:233-247")
add("Video dialog", "Dialog", "Gestures",
    "Overflow → Tutorial.",
    "Lists swipe seek, left brightness, right volume, double-tap sides, pinch/pan, long-press speed, subtitle drag. OK closes.",
    "Copy does not mention flags. QA must follow code flags, not this text, when a gesture is disabled.",
    ALL, "",
    "Panels.kt:278-289")
add("Video dialog", "Dialog", "Network Stream",
    "Overflow → Network Stream.",
    "Prompt 'Please enter a network URL:' and example http://www.example.com/video.mkv. OK launches playUrl only if the trimmed URL contains ://. CANCEL closes.",
    "OK must not launch if the URL has no ://. No error toast in that case; the dialog stays open.",
    ALL, "play_video_links gates external link intents, not this dialog",
    "NetworkStreamDialog.kt:26-46; Panels.kt:277")
add("Video dialog", "Dialog", "Select decoder",
    "Decoder chip. Android and desktop with VLC.",
    "Radios 'HW decoder', 'HW+ decoder', 'SW decoder'. Selecting restarts playback at the same index and position and toasts '{label} decoder'.",
    "Must not appear on iOS or web.",
    "Android, Desktop if VLC", "default_decoder",
    "Panels.kt:354-370; MediaEngine.kt:5-8")
add("Video dialog", "Dialog", "Delete \"{name}\"?",
    "More → Delete. fileEditing and uri starts with content or file:.",
    "DELETE deletes asynchronously, then next item or close. CANCEL dismisses.",
    "Hidden on iOS and web. Hidden for http streams.",
    "Android, Desktop", "PlayerCapabilities.fileEditing",
    "Panels.kt:784-807")
add("Video dialog", "Dialog", "Rename",
    "More → Rename. Same file-editing gate.",
    "Field is the name without extension. OK keeps the extension. Toast 'Renamed' or 'Rename failed'.",
    "Blank name must not rename.",
    "Android, Desktop", "PlayerCapabilities.fileEditing",
    "Panels.kt:809-829")
add("Video dialog", "Dialog", "Speed edit",
    "Tap the speed value in the speed panel.",
    "Decimal field with suffix x. OK clamps 0.25–4. CANCEL discards.",
    "Must not accept values outside 0.25–4.",
    ALL, "",
    "Panels.kt:411-420; PlayerLogic.kt:101-104")
add("Video system", "Toast", "Playing media links is disabled in Settings → General",
    "Android intent with http(s) URL and playMediaLinks false.",
    "Toast and the activity does not play the link.",
    "Must not block the in-player Network Stream dialog.",
    A, "play_video_links",
    "KMediaPlayerActivity.kt:130-132")

# panels
add("Video overflow", "Button", "Aspect Ratio", "Overflow grid.", "Opens Aspect Ratio panel.", "", ALL, "", "Panels.kt:244")
add("Video overflow", "Button", "Speed FF (Long Press)", "Overflow grid. Face shows current speed such as 2.0X.",
    "Cycles 1.5 → 2 → 3 → 4 → 1.5 and writes longPressSpeed. A value not in that list (for example 1.25) jumps to 1.5.",
    "Must not open the Speed panel.", ALL, "long_press_speed_value", "Panels.kt:245-248")
add("Video overflow", "Button", "Display Settings", "Overflow grid.", "Opens tuner tabs Style, Screen, Controls, Navigation, Text, Layout.", "", ALL, "tunerTabs", "Panels.kt:249; Tuner.kt:9-43")
add("Video overflow", "Button", "Playlists", "Overflow grid.", "Opens the current video queue, titled Playlist ({count}). Not the music playlist library.",
    "Must not create a music playlist.", ALL, "", "Panels.kt:250, 902-909")
add("Video overflow", "Button", "Network Stream", "Overflow grid.", "Opens Network Stream dialog.", "", ALL, "", "Panels.kt:251")
add("Video overflow", "Button", "Information", "Overflow grid.", "Opens Information panel.", "", ALL, "", "Panels.kt:252")
add("Video overflow", "Button", "Share", "Overflow grid.", "Shares the current file if one exists. No toast if there is no file.",
    "Must not crash with an empty queue.", ALL, "", "Panels.kt:253")
add("Video overflow", "Button", "Bookmark", "Overflow grid.", "Opens Bookmark panel.", "", ALL, "", "Panels.kt:254")
add("Video overflow", "Button", "Tutorial", "Overflow grid.", "Opens Gestures dialog.", "", ALL, "", "Panels.kt:255")
add("Video overflow", "Button", "More", "Overflow grid.", "Opens More panel.", "", ALL, "", "Panels.kt:256")
add("Video overflow", "Switch", "Video Display", "Overflow, default on.",
    "Off hides the picture and keeps audio. Android disables the video output; other hosts hide the surface.",
    "Must not stop audio.", ALL, "osd.video_display", "Panels.kt:273; ComposePlayerWindow.kt:69-70")
add("Video overflow", "Switch", "Shortcuts", "Overflow, default on.", "Shows or hides the shortcut row.", "", ALL, "osd.shortcuts", "Panels.kt:275")

add("Video panel", "Panel", "Audio Track", "From Audio Track button.",
    "Radio per engine track label. Empty row 'No audio tracks found' is disabled. Disable radio if the platform can switch tracks. Web without native track switching opens help instead of switching and hides Disable.",
    "Must not offer external audio file, stereo mode, or audio sync. Those are not implemented.",
    "Android, iOS, Desktop if VLC, Web", "",
    "Panels.kt:302-317")
add("Video panel", "Checkbox", "Use SW audio decoder", "Audio panel and decoderChoice.",
    "Toggles SW ↔ HW only. Does not select HW+.",
    "Hidden on iOS and web.", "Android, Desktop if VLC", "", "Panels.kt:316")
add("Video panel", "Panel", "Switch audio in {browser}", "Web, selecting a non-current audio track when native switching is off.",
    "Firefox steps mention about:config and media.track.enabled. Chrome/Edge/Opera steps mention Experimental Web Platform features. Button 'Open settings' copies the link and shows 'Link copied. Paste it in a new tab.'",
    "Must not switch the track until the browser setting is on.", W, "", "Panels.kt:930-957; PlatformVideoTools.wasmJs.kt:28-59")
add("Video panel", "Panel", "Subtitle", "From Subtitles button.",
    "Checks embedded tracks and the external file name. Empty: 'No subtitles'. Open picks a file. Success toast 'Subtitle: {name}'. Unsupported extension toast 'Unsupported subtitle format: .{ext}'. Read failure 'Can't read subtitle'. Parser accepts srt, vtt, webvtt only.",
    "Synchronization and Speed steppers stay disabled unless an external subtitle is loaded.",
    ALL, "subtitle_charset, italic_tag, subtitle_default_sync",
    "Panels.kt:321-338; Subtitles.kt:31; VideoPlayerState.kt:426-434")
add("Video panel", "Stepper", "Synchronization", "Subtitle panel. Enabled only with external subs.",
    "±0.1s. Value '{n.n}s'. Remembered per file if rememberSelections.",
    "Disabled for embedded-only subtitles.", ALL, "remember_selections", "Panels.kt:333-334")
add("Video panel", "Stepper", "Speed", "Subtitle panel. Enabled only with external subs.",
    "Subtitle timeline speed 50–200% in 0.1% steps. Not playback speed.",
    "Must not change video playback speed.", ALL, "", "Panels.kt:335-336")
add("Video panel", "Button", "Panel", "Subtitle panel.", "Opens Display Settings, not a separate subtitle-only screen.", "", ALL, "", "Panels.kt:337")
add("Video panel", "Panel", "Speed", "Speed shortcut. Bottom sheet.",
    "Decrease / Increase by 0.05, clamp 0.25–4. Center value '{n.nn}x' opens the edit dialog. Reset sets 1. Preset labels 0.25x, 1x, 2x, 3x, 4x.",
    "Must not use the music speed chip list.", ALL, "default_playback_speed", "Panels.kt:374-409")
add("Video panel", "Panel", "Equalizer", "Equalizer shortcut, or music Now Playing.",
    "On/Off switch. If effects are null: 'Equalizer is not available for this audio output.' Preset chips start with Custom. Reverb names: None, Small room, Medium room, Large room, Medium hall, Large hall, Plate. Bass Boost 0–100% if supported.",
    "Sliders must not edit while Off. Video shortcut hidden where capability is false; music button is not capability-gated.",
    "Bands: Android and Desktop VLC. Unavailable text elsewhere.", "audio_effects_enabled, eq.preset, eq.bands, eq.bass_boost, eq.reverb",
    "Panels.kt:456-530; MediaEngine.kt:78")
add("Video panel", "Panel", "Sleep Timer", "Sleep Timer shortcut.",
    "Keypad builds h and mm, max 3 digits, leading 0 ignored. Clear resets input. 'Play last media to the end' enabled only if input is valid or a timer is running. STOP clears and closes. START enabled only if minutes > 0. Toast 'Sleep timer: {h}h {m}m'.",
    "START must not work at 0. While running, the big numbers show remaining time, not the typed digits.",
    ALL, "", "Panels.kt:567-608; PlayerLogic.kt:22-30")
add("Video panel", "Card", "A - B Repeat", "A-B shortcut. Controls stay visible under this card.",
    "A sets point A. B disabled until A. B must be after A or toast 'B must be after A'. Valid B seeks to A and closes the card. Close (X) clears A-B entirely. Playback jumps to A when position reaches B.",
    "Closing the card must clear the repeat, not only hide the card. Separate from music A-B.",
    ALL, "", "Panels.kt:612-637; VideoPlayerState.kt:191")
add("Video panel", "Panel", "Aspect Ratio", "Overflow.",
    "Radios Default, 1:1, 4:3, 16:9, Custom. Custom slider 0.50–3.00 labeled 'Custom {n.nn}:1'. 'Apply to all videos' writes default aspect unless Custom.",
    "Must not change Zoom mode.", ALL, "player.default_aspect, remember_selections", "Panels.kt:641-653; PlayerLogic.kt:39-41")
add("Video panel", "Panel", "Information", "Overflow.",
    "Sections File, Media, Video, Audio. Rows File, Location, Size, Date, Subtitle (external name, Embedded subtitle, or None), optional Title, Format, Length, Decoder, Codec, Resolution, Frame rate, Bitrate, Channels, Sample rate, Language.",
    "Omit a section the engine does not report. Do not invent codec strings.", ALL, "", "Panels.kt:657-710")
add("Video panel", "Panel", "Bookmark", "Overflow.",
    "Empty: 'No bookmarks yet.' and 'Import Bookmarks'. Import toast '{n} bookmarks imported' or 'No bookmarks found in file'. Row tap seeks. Delete removes that time. Add stores the current position. Play/Pause toggles playback.",
    "Add disabled if there is no current file.", ALL, "", "Panels.kt:714-771")
add("Video panel", "Panel", "Playlist ({n})", "Overflow Playlists.",
    "Lists queue titles. Current item uses highlight color. Tap jumps to that item at 0 and plays, then closes.",
    "Must not edit music playlists.", ALL, "", "Panels.kt:902-909")
add("Video panel", "Button", "Delete", "More → Tools. Editable local/content file.", "Opens delete dialog.", "Hidden otherwise.", "Android, Desktop", "fileEditing", "Panels.kt:784")
add("Video panel", "Button", "Rename", "More → Tools. Same gate.", "Opens rename dialog.", "Hidden otherwise.", "Android, Desktop", "fileEditing", "Panels.kt:786")
add("Video panel", "Button", "Lock", "More → Tools. All platforms.", "Closes the panel and locks. Kids behavior follows lockMode.", "", ALL, "lock_mode", "Panels.kt:787")
add("Video panel", "Button", "Settings", "More → Tools.", "Opens app Settings screen.", "", ALL, "", "Panels.kt:788; MaxPlayerHost.kt:48")
add("Video panel", "Button", "FAQ", "More → Help.", "Opens app Help screen.", "", ALL, "", "Panels.kt:791; MaxPlayerHost.kt:49")
add("Video panel", "Button", "Check for updates", "More → Help and storeUpdates (Android).",
    "Opens the store page. Failure toast 'You're on the latest version'.",
    "Hidden on iOS, desktop, and web.", A, "storeUpdates", "Panels.kt:792; KMediaPlayerActivity.kt:306-307")
add("Video panel", "Button", "Bug Report", "More → Help.",
    "Mailto subject 'Max Player Pro bug report'. Failure toast 'No email app found'.",
    "", ALL, "", "Panels.kt:793; MaxPlayerHost.kt:23")
add("Video panel", "Panel", "Customise Items", "Expanded shortcut Customise.",
    "Checkbox per available shortcut except Customise. Unchecking removes it from the row.",
    "Customise itself cannot be unchecked.", ALL, "osd.shortcut_items", "Panels.kt:913-927")
add("Video panel", "Panel", "Display Settings tabs", "Overflow Display Settings or Subtitle → Panel.",
    "Tabs: Style, Screen, Controls, Navigation, Text, Layout. Bool settings are checkboxes using the setting title. Empty choice label shows Default. Changes call applyScreenSettings and engine.applySettings.",
    "rotationButton, gPlaybackSpeed, and subFadeOut appear here as settings but do not add or remove a player control.",
    ALL, "tunerTabs", "Panels.kt:837-868; Tuner.kt:9-43")

# keyboard
kb = [
    ("Space, K", "Play/pause. No OSD pill."),
    ("Left / Right", "Seek ±move interval. Shift = ±60s. Cmd/Ctrl = previous/next item."),
    ("J / L", "Seek ±10s, ignoring move interval."),
    ("Up / Down", "Volume ±1. Level pill."),
    ("Cmd/Ctrl + Up / Down", "Brightness ±2, clamp 0–30, persist brightness level."),
    ("M", "Mute toggle. Toast Mute or Unmute."),
    ("F", "Toggle fullscreen. Failure toast 'Full screen is not available here'."),
    ("Esc", "Close panel, else exit fullscreen, else not handled."),
    ("Cmd/Ctrl+N, Shift+N", "Next item."),
    ("Cmd/Ctrl+P, Shift+P", "Previous item."),
    ("[ / ]", "Speed ±0.25, clamp 0.25–4. Toast 'Speed {n.nn}X'."),
    ("\\", "Speed 1.0. Same toast."),
    ("0–9", "Seek to 0% … 90%. No seek badge."),
    ("Home / End", "Seek start / duration−1s."),
    ("S", "Screenshot unless Cmd/Ctrl."),
    ("C", "Toggle subtitles. Toast 'Subtitles on' or 'Subtitles off'. Does not unload an external file."),
]
for key, behavior in kb:
    add("Video keyboard", "Shortcut", key,
        "Desktop and web only. Key down. Ignored entirely while locked.",
        behavior,
        "Must not run on Android or iOS. Must not run while locked.",
        "Desktop, Web", "PlayerCapabilities.keyboardShortcuts; move_interval",
        "PlayerKeyboard.kt:15-76")

# music library
add("Music library", "Tab", "Tracks", "Music screen.", "Song list. Wide can switch List/Grid.", "", ALL, "", "MusicScreen.kt:84")
add("Music library", "Tab", "Playlists", "Music screen.", "Cards: New playlist, Favourites, Recently Played, then user music playlists.", "", ALL, "", "MusicScreen.kt:157-168")
add("Music library", "Tab", "Albums", "Music screen.", "Groups by album, else folder. Unknown album if blank.", "", ALL, "", "MusicScreen.kt:170-178")
add("Music library", "Tab", "Artists", "Music screen.", "Groups by artist. Unknown artist if blank.", "", ALL, "", "MusicScreen.kt:173")
add("Music library", "Tab", "Folders", "Music screen.", "Groups by folder name. Unknown folder if blank.", "", ALL, "", "MusicScreen.kt:174")
add("Music library", "Button", "Back", "Compact music header and group drill-in.", "Leaves music or closes the group.", "Wide library header has no back button.", ALL, "", "MusicScreen.kt:136, 141")
add("Music library", "Button", "Shuffle", "Header, after the first scan, if the list is not empty. Wide shows the word Shuffle; phone is icon-only.",
    "Plays the filtered list in a one-shot shuffled order from index 0. Engine shuffleEnabled is set false.",
    "Must not turn on ongoing engine shuffle. Hidden while scanning.", ALL, "", "MusicScreen.kt:112-121; KMediaPlayer.kt:98-109")
add("Music library", "Button", "Play all", "Header play circle, list not empty.", "Plays the filtered list from index 0, not shuffled.", "Hidden while scanning or if the list is empty.", ALL, "", "MusicScreen.kt:123-125")
add("Music library", "Button", "Refresh", "Header.", "Rescans the library. First scan shows a skeleton, not partial rows.", "", ALL, "", "MusicScreen.kt:141, 154")
add("Music library", "Field", "Search songs, artists, albums…", "Under the header on phones; in the header on wide.", "Filters title, artist, and album, case-insensitive.", "", ALL, "", "MusicScreen.kt:199")
add("Music library", "Menu", "Sort: Title / Date / Length / Size", "Tracks tab.", "Sorts the filtered list. Only those four keys are offered.",
    "Must not offer Played time, Status, Resolution, Path, Frame rate, or Type.", ALL, "", "MusicScreen.kt:208-225; Library.kt:16-19")
add("Music library", "Toggle", "List / Grid", "Wide Tracks tab.", "Switches table vs card grid. A11y List and Grid.", "Hidden on phones.", "Wide windows", "", "MusicScreen.kt:229-240")
add("Music library", "Card", "New playlist / Create", "Playlists tab.", "Opens Create New Playlist. Field label Playlist name. Blank name does not create.", "", ALL, "", "MusicScreen.kt:161, 184-186")
add("Music library", "Card", "Favourites", "Playlists tab.", "Opens tracks whose URI is in favourites.", "", ALL, "", "MusicScreen.kt:162")
add("Music library", "Card", "Recently Played", "Playlists tab.", "Opens tracks with history, newest first.", "", ALL, "", "MusicScreen.kt:163")
add("Music library", "Row menu", "Add to Favourites / Remove from Favourites", "Track more menu. Heart is shown instead of More when already favourite.",
    "Toggles favourite.", "", ALL, "", "MusicComponents.kt:259-269")
add("Music library", "Row menu", "Add to Playlist", "Track more menu.", "Opens Add To Playlist.", "", ALL, "", "MusicComponents.kt:268")
add("Music library", "Dialog", "Add To Playlist", "From a track menu or Now Playing more menu.",
    "Title Add To Playlist. Row '+ Create New Playlist'. Playlist rows '{name} ({count})'. Button Cancel.",
    "", ALL, "", "FileList.kt:356-370")
add("Music library", "Dialog", "Create New Playlist", "From New playlist card or Add To Playlist.", "Label Playlist name. Blank name does not create.", "", ALL, "", "MusicScreen.kt:184; FileList.kt:371")
add("Music library", "Empty", "No music found.", "After scan, empty list or empty group.", "Empty state. Not shown during the first scan.", "Must not show while scanning.", ALL, "", "MusicComponents.kt:159, 279")
add("Music library", "Table", "TITLE / ARTIST / ALBUM / Length", "Wide track table.", "Columns #, TITLE, ARTIST, ALBUM, length. Duration '–' if unknown. Phone omits artist/album columns and hides a zero duration.",
    "", "Wide", "", "MusicComponents.kt:170-178")

# mini player
add("Music mini player", "Bar", "Mini player", "isMusicSession and the engine has items. Hidden during video (isMusicSession false).",
    "Tap opens Now Playing. Narrow (<900dp): Play/Pause, Next, Close. Wide: Favourite, Shuffle, Previous, Play/Pause, Next, Repeat, seek, Volume, volume slider, Close.",
    "Must not show during video playback. Narrow bar is not seekable.",
    ALL, "", "MiniPlayer.kt:82-180")
add("Music mini player", "Button", "Play / Pause", "Mini player.", "togglePlayback. If ended, seeks to item start then plays and starts background playback.", "", ALL, "", "MiniPlayer.kt:69-74, 133")
add("Music mini player", "Button", "Next", "Mini player, both widths.", "Engine seekToNext. Not the video smart-next rules.", "Must not apply prevNextToRewFf.", ALL, "", "MiniPlayer.kt:134, 159")
add("Music mini player", "Button", "Previous", "Wide mini player only.", "Engine seekToPrevious. Not video smart previous.", "Must not appear on the narrow bar.", "Wide ≥900dp", "", "MiniPlayer.kt:157")
add("Music mini player", "Button", "Close", "Mini player.", "Stops, clears items, empties the queue, which hides the bar.", "", ALL, "", "MiniPlayer.kt:135, 176")
add("Music mini player", "Button", "Shuffle", "Wide mini player.", "Toggles engine shuffleEnabled. Accent when on.", "Not the library one-shot shuffle.", "Wide ≥900dp", "", "MiniPlayer.kt:156")
add("Music mini player", "Button", "Repeat", "Wide mini player.", "Cycles Off → All → One → Off. Accent when not Off. One uses the repeat-one icon.", "", "Wide ≥900dp", "", "MiniPlayer.kt:160-161")
add("Music mini player", "Button", "Favourite", "Wide mini player.", "Toggles host favourite. Accent when on.", "Hidden on the narrow bar.", "Wide ≥900dp", "", "MiniPlayer.kt:151-152")
add("Music mini player", "Slider", "Seek", "Wide mini player.", "Commits on release. Times are formatDuration.", "Narrow bar only shows a non-interactive progress line.", "Wide ≥900dp", "", "MiniPlayer.kt:165-168")
add("Music mini player", "Button", "Volume", "Wide mini player.", "Toggles player.volume between 0 and 1. Not system volume. Slider sets 0–1 live.",
    "Must not use the video volume-boost scale.", "Wide ≥900dp", "", "MiniPlayer.kt:170-174")

# now playing
add("Now Playing", "Button", "Back", "Now Playing header.", "Closes Now Playing. Does not stop playback.", "", ALL, "", "NowPlayingScreen.kt:484-492")
add("Now Playing", "Text", "NOW PLAYING", "Narrow layout only.", "Header label. Subtitle is album or folder if known.", "Hidden on wide layout.", "Narrow", "", "NowPlayingScreen.kt:224")
add("Now Playing", "Button", "Share", "Header.", "Shares the current file if present.", "", ALL, "", "NowPlayingScreen.kt:166, 228")
add("Now Playing", "Button", "Favourite", "Under the title.", "Toggles favourite. Filled heart is pink when on.", "", ALL, "", "NowPlayingScreen.kt:280-283")
add("Now Playing", "Button", "Shuffle", "Transport.", "Toggles engine shuffleEnabled.", "", ALL, "", "NowPlayingScreen.kt:289")
add("Now Playing", "Button", "Previous", "Transport.", "Engine seekToPrevious.", "Must not use video smart previous.", ALL, "", "NowPlayingScreen.kt:290")
add("Now Playing", "Button", "Play / Pause", "Transport.", "togglePlayback.", "", ALL, "", "NowPlayingScreen.kt:293")
add("Now Playing", "Button", "Next", "Transport.", "Engine seekToNext.", "", ALL, "", "NowPlayingScreen.kt:296")
add("Now Playing", "Button", "Repeat", "Transport.", "Off → All → One → Off.", "", ALL, "", "NowPlayingScreen.kt:297-299")
add("Now Playing", "Seek bar", "Elapsed / remaining", "Under the title.", "Drag previews; commits on release. Right side is -mm:ss or --:--. A-B span is drawn if A is set.", "", ALL, "", "NowPlayingScreen.kt:316-358")
add("Now Playing", "Button", "Equalizer", "Action bar. Not capability-gated.",
    "Opens Equalizer panel. If the output has no effects, the panel says equalizer is not available.",
    "Must not hide the button on iOS or web.", ALL, "", "NowPlayingScreen.kt:187, 368")
add("Now Playing", "Button", "A-B", "Action bar.",
    "First tap toast 'A point set'. Second 'A-B repeat on' (B at least A+500ms). Third 'A-B repeat off'. Loop seeks to A when position reaches B.",
    "Independent of the video A-B card. Toasts use PlatformMedia.showToast, not the video toast view, unless a video screen is collecting notices.",
    ALL, "", "NowPlayingScreen.kt:369-374, 324")
add("Now Playing", "Button", "Speed", "Action bar. Shows {speed}x in accent when not 1.",
    "Sheet title 'Playback speed'. Chips only 0.5x, 0.75x, 1x, 1.25x, 1.5x, 2x. Sets the shared engine speed.",
    "Must not offer 0.25x, 3x, or 4x. Changing it affects a later video session on the same engine.",
    ALL, "", "NowPlayingScreen.kt:113, 191-201, 376-378")
add("Now Playing", "Button", "Lyrics", "Action bar.", "Toast 'No lyrics found'. No panel.", "Must not open a lyrics view.", ALL, "", "NowPlayingScreen.kt:380")
add("Now Playing", "Button", "Playing queue", "Narrow action bar only.", "Opens Up next sheet.", "Hidden on wide because the queue is already beside the cover.", "Narrow", "", "NowPlayingScreen.kt:381")
add("Now Playing", "Button", "More", "Action bar.", "Sheet title is the track title. Add to playlist if the host provided the slot. Share.", "", ALL, "", "NowPlayingScreen.kt:203-206, 382")
add("Now Playing", "Sheet", "Add to playlist", "More sheet, only when MusicScreen supplied the slot.", "Opens Add To Playlist.", "", ALL, "", "NowPlayingScreen.kt:204")
add("Now Playing", "Panel", "Up next", "Wide always; narrow from Playing queue.",
    "Header 'Up next' and '{n} tracks'. Search placeholder 'Search in queue'. Empty 'No matching tracks'. Clear clears the query. Tap plays that index from 0.",
    "", ALL, "", "NowPlayingScreen.kt:387-438")
add("Now Playing", "Layout", "Wide Now Playing", "Width ≥ 840dp and height ≥ 480dp.",
    "Cover and queue side by side. No NOW PLAYING header. Back and Share sit above the cover.",
    "Must not use the 900dp mini-player breakpoint.", ALL, "", "NowPlayingScreen.kt:154-173")

# host
add("Host", "Wrapper", "MaxPlayerContent", "Video route.", "Wraps VideoPlayerScreen in Max theme and Mulish. No extra controls.", "", ALL, "", "MaxPlayerContent.kt:11-12")
add("Host", "Host", "Max Player Pro", "PlayerHost.appName.", "Used in the bug-report subject. Settings and Help navigation only.", "", ALL, "", "MaxPlayerHost.kt:23-49")

wb = Workbook()
wb.calculation = CalcProperties(calcMode="auto", fullCalcOnLoad=True)

# Cover
cover = wb.active
cover.title = "Read me"
cover["A1"] = "Max Player UI control inventory"
cover["A1"].font = TITLE
cover["A2"] = "Source-backed QA inventory. Labels and gates were read from the player source. Nothing in this workbook was inferred from a screenshot."
cover["A2"].font = SUB
cover.merge_cells("A2:F2")
notes = [
    (4, "How to use"),
    (5, "Filter Controls by Surface, Kind, or Platform. The Must not happen column is the negative test."),
    (6, "Platform values: All, Android, iOS, Desktop, Web, or a combination. Desktop equalizer, audio tracks, and decoder require VLC."),
    (7, "Settings that exist but do not create a control are listed as Kind = Setting (unwired) or called out in Must not happen."),
    (8, "There is no video-cut control. ic_ov_cut is imported in Panels.kt and unused."),
    (9, "Music A-B, shuffle, previous, and speed are not the video implementations. Do not copy video expected results onto music."),
    (11, "Counts"),
    (12, "Controls"),
    (13, "Platform rows"),
    (14, "PlayerKeys"),
]
for r, text in notes:
    cover.cell(r, 1, text).font = Font(bold=True) if text in ("How to use", "Counts") else SUB
cover["B12"] = '=COUNTA(Controls!A:A)-1'
cover["B13"] = '=COUNTA(\'Platform matrix\'!A:A)-1'
cover["B14"] = '=COUNTA(PlayerKeys!A:A)-1'
cover.column_dimensions["A"].width = 28
cover.column_dimensions["B"].width = 22
cover.row_dimensions[2].height = 32
cover.page_setup.orientation = "landscape"
cover.page_setup.fitToPage = True
cover.page_setup.fitToWidth = 1
cover.page_setup.fitToHeight = 1
cover.sheet_properties.pageSetUpPr.fitToPage = True
cover.oddHeader.left.text = "Max Player UI inventory"
cover.oddFooter.right.text = "Page &P of &N"
cover.print_title_rows = "1:1"
cover.auto_filter.ref = "A4:A4"
cover.freeze_panes = "A4"
cover.sheet_view.showGridLines = False
cover.sheet_view.zoomScale = 120

def style_table(ws, widths):
    ws.freeze_panes = "A2"
    ws.auto_filter.ref = f"A1:{get_column_letter(ws.max_column)}{ws.max_row}"
    ws.auto_filter.add_sort_condition("A2:A2")
    for col, width in enumerate(widths, 1):
        ws.column_dimensions[get_column_letter(col)].width = width
    for cell in ws[1]:
        cell.font = HEADER
        cell.fill = HEADER_FILL
        cell.alignment = Alignment(wrap_text=True, vertical="center")
    for r in range(2, ws.max_row + 1):
        ws.row_dimensions[r].height = 48
        for c in range(1, ws.max_column + 1):
            cell = ws.cell(r, c)
            cell.alignment = WRAP
            cell.border = THIN
            cell.font = Font(name="Calibri", size=10)
            if r % 2 == 0:
                cell.fill = ALT
    ws.page_setup.orientation = "landscape"
    ws.page_setup.fitToPage = True
    ws.page_setup.fitToWidth = 1
    ws.page_setup.fitToHeight = 0
    ws.page_setup.paperSize = ws.PAPERSIZE_TABLOID
    ws.sheet_properties.pageSetUpPr.fitToPage = True
    ws.page_setup.horizontalCentered = True
    ws.print_title_rows = "1:1"
    ws.page_setup.horizontalDpi = 300
    ws.page_setup.verticalDpi = 300
    ws.sheet_view.zoomScale = 110
    ws.oddHeader.left.text = ws.title
    ws.oddFooter.right.text = "Page &P of &N"
    ws.sheet_properties.tabColor = "1F4E79"

controls = wb.create_sheet("Controls")
controls.append(COLS)
for i, row in enumerate(rows, 1):
    controls.append((f"C{i:03d}",) + row)
style_table(controls, [8, 22, 16, 36, 42, 55, 48, 28, 36, 42])
# highlight unwired
unwired = PatternFill("solid", fgColor="FDE68A")
for r in range(2, controls.max_row + 1):
    if controls.cell(r, 3).value == "Setting (unwired)":
        for c in range(1, controls.max_column + 1):
            controls.cell(r, c).fill = unwired

platform = [
    ("Flag", "Android", "iOS", "Desktop", "Web", "What is hidden when false"),
    ("equalizer", "Yes", "No", "Only if VLC is available", "No", "Video Equalizer shortcut. Music button stays and shows the unavailable sentence."),
    ("audioTrackSelection", "Yes", "Yes", "Only if VLC is available", "Yes", "Audio Track button."),
    ("decoderChoice", "Yes", "No", "Only if VLC is available", "No", "HW/HW+/SW chip and Use SW audio decoder."),
    ("backgroundPlay", "Yes", "No", "No", "No", "Background Play shortcut."),
    ("screenRotation", "Yes", "No", "No", "No", "Screen Rotation shortcut. Desktop rotate() is fullscreen, but the shortcut is hidden."),
    ("pictureInPicture", "Yes", "No", "No", "Only if document.pictureInPictureEnabled", "Popup button. PiP hides all chrome."),
    ("fullscreenButton", "No", "No", "Yes", "Yes", "Full screen / Exit full screen button. Keyboard F still exists on desktop and web."),
    ("keyboardShortcuts", "No", "No", "Yes", "Yes", "Entire PlayerKeyboard map."),
    ("fileEditing", "Yes", "No", "Yes", "No", "Delete and Rename. Also requires a content: or file: URI."),
    ("storeUpdates", "Yes", "No", "No", "No", "Check for updates."),
    ("wheelZoomStep", "0.1", "0.1", "0.1", "0.001", "Scroll zoom sensitivity."),
    ("screenshot", "Pictures/MaxPlayerPro", "Toast: not available on iOS yet", "~/Pictures/MaxPlayer PNG", "Browser download, or CORS refusal", "Screenshot shortcut is still shown."),
    ("brightness apply", "Window screen brightness if enabled", "setBrightness no-op; picture factor still applied", "Picture factor on the frame", "CSS brightness filter", "Gesture still runs on all platforms."),
]
ps = wb.create_sheet("Platform matrix")
for row in platform:
    ps.append(row)
style_table(ps, [28, 22, 36, 32, 42, 62])
for cell in ps[1]:
    cell.fill = PatternFill("solid", fgColor="0F766E")

keys = [
    ("Key", "Storage name", "Title", "Default", "Wired to a control?"),
    ("touchAction", "touch_action", "Touch action", "toggle", "Yes — single tap"),
    ("lockMode", "lock_mode", "Lock mode", "lock", "Yes — kids vs lock"),
    ("lockShowInterface", "lock_show_interface", "Show interface when locked screen is touched", "false", "Partial — refreshes lock icon only; transport stays hidden"),
    ("gSeek", "g.seek", "Seek position", "true", "Yes"),
    ("gVolume", "g.volume", "Volume", "true", "Yes — right half"),
    ("gBrightness", "g.brightness", "Brightness", "true", "Yes — left half"),
    ("gZoom", "g.zoom", "Video zoom", "true", "Yes — pinch and wheel"),
    ("gPan", "g.pan", "Video pan", "false", "Yes — two-finger pan"),
    ("gZoomAndPan", "g.zoom_and_pan", "Zoom and Pan", "false", "Yes — either pinch or pan"),
    ("gDoubleTapPlayPause", "g.dt_play_pause", "Play/Pause (Double tap)", "true", "Yes"),
    ("gDoubleTapFfRw", "g.dt_ffrw", "FF/RW (Double tap)", "true", "Yes — sides win over play/pause"),
    ("gDoubleTapZoom", "g.dt_zoom", "Video zoom (Double tap)", "false", "Yes — center only, wins over play/pause"),
    ("gLongPressSpeed", "g.long_press_speed", "Speed FF (Long press)", "true", "Yes"),
    ("longPressSpeed", "long_press_speed_value", "Speed FF value", "2.0", "Yes"),
    ("gPlaybackSpeed", "g.playback_speed", "Playback Speed", "true", "No — checkbox only"),
    ("gSubtitleUpDown", "g.subtitle_updown", "Subtitle up/down", "true", "Yes"),
    ("gSubtitleZoom", "g.subtitle_zoom", "Subtitle zoom", "true", "Yes"),
    ("rotationButton", "screen_rotation_button", "Screen rotation button", "true", "No — shortcut uses screenRotation capability only"),
    ("subFadeOut", "sub.fadeout", "Fade out", "true", "No — subtitle renderer does not fade"),
    ("resume", "resume_last", "Resume", "resume", "Yes — dialog, notice, or nothing"),
    ("showMoveButtons", "show_move_buttons", "Forward/backward moving button", "false", "Yes"),
    ("showPrevNext", "show_prev_next", "Previous/next button", "true", "Yes"),
    ("shortcutsVisible", "osd.shortcuts", "Shortcuts", "true", "Yes"),
    ("backgroundMode", "sticky_video", "Background/PiP mode", "pip", "Yes — Android leave-app PiP"),
    ("doubleTapBack", "double_tap_back_key", "Double-tap the back button", "false", "Android back only"),
    ("playMediaLinks", "play_video_links", "Play media links", "true", "Android http intents only"),
]
ks = wb.create_sheet("PlayerKeys")
for row in keys:
    ks.append(row)
style_table(ks, [24, 32, 48, 14, 55])
for r in range(2, ks.max_row + 1):
    if str(ks.cell(r, 5).value).startswith("No"):
        for c in range(1, 6):
            ks.cell(r, c).fill = unwired

must = [
    ("Rule", "Detail"),
    ("No invented cut control", "ic_ov_cut is imported and unused. Do not write a video-cut test."),
    ("Locked transport", "Full controls are not composed while locked, even if lock_show_interface is on."),
    ("Kids lock", "A tap must toast Long press to unlock and stay locked."),
    ("PiP chrome", "Transport, panels, dialogs, lock, and OSD pills are not shown in PiP. Subtitles stay, scaled to 0.4."),
    ("Capability hiding", "Do not show decoder, equalizer shortcut, background play, rotation, PiP, or fullscreen when the capability is false."),
    ("Keyboard platforms", "Space, arrows, J/L, and the rest of PlayerKeyboard must not run on Android or iOS, or while locked."),
    ("Music is not video", "Music previous is not smart previous. Music speed chips are not the 0.25–4 track. Music A-B is a separate object."),
    ("Lyrics", "Lyrics must only toast No lyrics found."),
    ("Network Stream", "OK must not launch a URL that does not contain ://."),
    ("Resume edges", "Ask must not appear within 5 seconds of the start or the end, or on later files when resume only first is on."),
    ("SW decoder offer", "TRY SW DECODER must not appear for IO errors or when the decoder is already SW."),
    ("Subtitle steppers", "Synchronization and Speed stay disabled for embedded-only subtitles."),
    ("File edit", "Delete and Rename require fileEditing and a content: or file: URI."),
]
ms = wb.create_sheet("Must not")
for row in must:
    ms.append(row)
style_table(ms, [28, 110])
ms.auto_filter.add_sort_condition("A2:A2")

# data validation for testers
dv = DataValidation(type="list", formula1='"Pass,Fail,Blocked,Not run"', allow_blank=True)
dv.error = "Choose Pass, Fail, Blocked, or Not run"
dv.errorTitle = "Result"
dv.add("L2:L{}".format(controls.max_row))
controls.add_data_validation(dv)
controls.cell(1, 12, "QA result").font = HEADER
controls.cell(1, 12).fill = HEADER_FILL
controls.cell(1, 13, "Notes").font = HEADER
controls.cell(1, 13).fill = HEADER_FILL
controls.column_dimensions["L"].width = 14
controls.column_dimensions["M"].width = 36
controls.auto_filter.ref = f"A1:M{controls.max_row}"
fail_fill = PatternFill("solid", fgColor="FECACA")
pass_fill = PatternFill("solid", fgColor="BBF7D0")
controls.conditional_formatting.add(f"L2:L{controls.max_row}", FormulaRule(formula=['$L2="Fail"'], fill=fail_fill))
controls.conditional_formatting.add(f"L2:L{controls.max_row}", FormulaRule(formula=['$L2="Pass"'], fill=pass_fill))

cover.oddHeader.center.text = ""
wb.properties.title = "Max Player UI control inventory"
wb.properties.creator = "Max Player QA inventory"
wb.properties.subject = "Source-backed player and music controls"

wb.save(OUT)
print(f"rows={len(rows)} sheets={wb.sheetnames} file={OUT}")
