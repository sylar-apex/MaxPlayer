#!/usr/bin/env python3
"""Build Max Player QA workbook from the screen inventory in source."""

from openpyxl import Workbook
from openpyxl.styles import Alignment, Border, Font, PatternFill, Side
from openpyxl.utils import get_column_letter
from openpyxl.worksheet.datavalidation import DataValidation
from openpyxl.formatting.rule import FormulaRule
from openpyxl.chart.shapes import GraphicalProperties
from openpyxl.workbook.properties import CalcProperties
from openpyxl.worksheet.table import Table, TableStyleInfo
from openpyxl.formatting.rule import CellIsRule
from openpyxl.comments import Comment

OUT = "/Volumes/BOHR@HD/Pankaj/Project/WeAreTeam/Max Player/docs/MaxPlayer-QA.xlsx"

HEADERS = [
    "#", "Section", "Title", "Platform", "Condition (when it shows)", "Flow (Steps)",
    "Expected Result", "Must Not Happen", "Platform note", "Priority",
    "Android Result", "Desktop Result", "iOS Result", "Web Result", "QA Input / Clarification",
]

PRI_ORDER = {"P0": 0, "P1": 1, "P2": 2}
SHEETS = {}  # name -> list of dicts


def add(sheet, section, title, platform, condition, steps, expected, must_not, note, priority):
    SHEETS.setdefault(sheet, []).append({
        "section": section, "title": title, "platform": platform, "condition": condition,
        "steps": steps, "expected": expected, "must_not": must_not, "note": note, "priority": priority,
    })


def matrix(sheet, section, title, steps, expected, must_not, priority, platform="Android, Desktop, iOS, Web",
           condition="Screen is reachable", note="UI exists on all targets. Playback engine is shipped on Android only."):
    add(sheet, section, title, platform, condition, steps, expected, must_not, note, priority)


# ---------------------------------------------------------------------------
# App Shell
# ---------------------------------------------------------------------------
def shell():
    s = "App Shell"
    matrix(s, "Launch", "Cold start opens Home",
           "Install or clear data. Launch Max Player.",
           "Start destination is Home. No splash or onboarding screen. Nav animations are off.",
           "Must not open Me, Settings, or a login screen. Must not wait on a custom permission screen before showing Home.",
           "P0")
    matrix(s, "Launch", "Android permission dialog is the system dialog",
           "Fresh install on Android. Launch the app.",
           "System permission dialog appears (API 33+: videos, audio, images, notifications; older: storage). App content is set immediately and does not wait for the result. Grant refreshes the library.",
           "Must not show an in-app Grant button or a custom rationale screen. Denial must not crash; Home still opens.",
           "P0", platform="Android", note="MainActivity requests permissions then still shows App().")
    matrix(s, "Launch", "No login, OTP, or account gate",
           "Launch logged-out, airplane mode, and after force-stop.",
           "Library UI is available without an account.",
           "Must not ask for a phone number, OTP, Google account, or MX account.",
           "P0")
    matrix(s, "Width", "Compact and Medium use the phone layout",
           "Resize or use a window under 960 dp.",
           "In-page top bar and shortcut chips. No permanent sidebar.",
           "Must not show the 248 dp sidebar or the desktop title bar.",
           "P1", note="WindowClass: Compact < 600 dp, Medium < 960 dp, Expanded otherwise. isWide is Expanded only.")
    matrix(s, "Width", "Expanded window shows the sidebar",
           "Use a window at least 960 dp wide (desktop, or a tablet in landscape).",
           "Permanent sidebar: logo, Max Player, PRO chip, LIBRARY, MORE, then Settings and Help.",
           "Must not also show phone shortcut chips or the phone Account icon as a second nav.",
           "P0")
    for label, dest, highlight in [
        ("Videos", "Home", "Home, Folder, and Search keep Videos highlighted"),
        ("Music", "Music", "Music stays highlighted"),
        ("Playlists", "Video playlists", "Playlists and playlist detail keep it highlighted"),
        ("Private folder", "Private Folder PIN gate", "Private Folder keeps it highlighted"),
        ("Network stream", "Networks empty state", "Local Network keeps it highlighted"),
        ("Storage", "Media Manager", "Media Manager and its lists keep it highlighted"),
        ("Theme", "App Theme", "Theme keeps it highlighted"),
        ("Settings", "Settings index", "Settings, a settings page, and a tuner tab keep it highlighted"),
        ("Help", "Help", "Help keeps it highlighted"),
    ]:
        matrix(s, "Sidebar", f"Sidebar {label} opens the right screen",
               f"Wide window. Tap {label}.",
               dest + ". " + highlight + ". Switching sections does not stack history.",
               "Must not push a duplicate of the current screen. Must not open a different section.",
               "P0" if label in ("Videos", "Music", "Settings") else "P1",
               platform="Desktop and wide windows", note="Sidebar is hidden below 960 dp.")
    matrix(s, "Sidebar", "Videos pops to Home instead of stacking",
           "Wide window. Open a folder, then tap Videos.",
           "Returns to Home. Videos row is selected.",
           "Must not leave Folder on the back stack as the visible screen.",
           "P1", platform="Desktop and wide windows")
    matrix(s, "Title bar", "Back is disabled on the first screen",
           "Wide window, freshly on Home.",
           "Back is visible but disabled at about 30% alpha.",
           "Must not pop Home or close the app.",
           "P1", platform="Desktop and wide windows")
    matrix(s, "Title bar", "Back and Forward remember the route",
           "Wide window. Open Music, then Back, then Forward.",
           "Back returns to the previous route and enables Forward. Forward restores Music.",
           "Any navigation that is not Back or Forward must clear the forward list.",
           "P1", platform="Desktop and wide windows")
    matrix(s, "Title bar", "Title-bar search opens Search",
           "Wide window with a title bar. Type a non-blank query in Search videos.",
           "Search is pushed if not already there. Results filter live. Enter or IME Search saves the query to recent searches.",
           "A blank Enter must not save history. Escape on a non-empty query clears it. Leaving Search clears the title-bar query.",
           "P0", platform="Desktop and wide windows")
    matrix(s, "Title bar", "Refresh is disabled while scanning",
           "Wide window. Trigger a library refresh.",
           "Refresh is disabled while media.loading. The divider becomes a 2 dp progress line. Refresh calls media.refresh().",
           "Must not start a second scan from a disabled control. Must not navigate away.",
           "P1", platform="Desktop and wide windows")
    matrix(s, "Player host", "Android opens playback in its own activity",
           "Android. Play a video from Home.",
           "Video player activity opens. The library is not covered by an embedded overlay.",
           "Must not draw the desktop 'Initialising player…' overlay on top of Home.",
           "P0", platform="Android", note="embeddedPlayer is false on Android.")
    matrix(s, "Player host", "Desktop, iOS, and Web use an embedded overlay",
           "Desktop, iOS, or Web. Play a video.",
           "A full overlay covers the library while video is requested. If the engine is not ready, a black overlay says Initialising player… / Loading video codecs for the first time and swallows clicks.",
           "Must not let taps reach the library under the overlay. If start fails, the overlay must close rather than stick forever.",
           "P0", platform="Desktop, iOS, Web", note="Playback engines on these targets are still planned. The overlay must not crash.")
    matrix(s, "Unavailable", "File Transfer route is not on the home chips",
           "Look at Home chips and the sidebar.",
           "There is no File Transfer chip. The unused route exists only if something calls onFileTransfer.",
           "Must not show a File Transfer shortcut on Home.",
           "P2", note="HomeNavigation.onFileTransfer is unused by ShortcutChips.")
    matrix(s, "Rotation", "Phone rotation does not reset Home",
           "Phone. Scroll the folder list, rotate, rotate back.",
           "Home remains. Scroll and view options are kept.",
           "Must not jump to Me or replay the last video.",
           "P1", platform="Android, iOS")
    matrix(s, "Process", "Force-stop returns to Home, not a half-open player",
           "Android. Open Settings, force-stop, relaunch.",
           "App opens on Home. Settings values that were saved remain.",
           "Must not restore a stuck player overlay with no engine.",
           "P1", platform="Android")


def home():
    s = "Home"
    matrix(s, "Top bar", "Title is Folders unless view mode is Files",
           "Phone Home. Default view options.",
           "Top bar title is Folders.",
           "Must not say Videos until View Mode is Files. All folders also uses the Folders title.",
           "P0", platform="Phone and medium windows")
    matrix(s, "Top bar", "Files mode title is Videos",
           "Open View Menu, choose Files, Done.",
           "Title becomes Videos and the file list is shown.",
           "Must not keep showing folder cards.",
           "P0")
    for name, expected, must, pri in [
        ("Refresh", "Library scan starts. A 2 dp progress line shows while loading.", "Must not open Search or change the current folder.", "P0"),
        ("Search", "Search screen opens.", "Must not start playback.", "P0"),
        ("View Menu", "View options dialog opens (299×517).", "Must not apply changes until Done.", "P0"),
        ("Account", "Me page opens.", "Must not open Settings directly. Must not ask for login.", "P0"),
    ]:
        matrix(s, "Top bar", f"{name} does only its job",
               f"Phone Home. Tap {name}.", expected, must, pri, platform="Phone and medium windows")
    matrix(s, "Top bar", "Open folder exists only on Web",
           "Compare Android, Desktop, iOS, and Web Home top bars.",
           "Web shows Open folder when the folder picker is available. Other platforms bind FolderImport.None and hide it.",
           "Must not show a dead Open folder button on Android.",
           "P1")
    matrix(s, "Chips", "Music chip opens Music",
           "Phone Home. Tap Music.",
           "Music screen opens.",
           "Must not open the video player.", "P0", platform="Phone and medium windows")
    matrix(s, "Chips", "Privacy chip opens Private Folder",
           "Phone Home. Tap Privacy.",
           "Private Folder PIN gate opens.",
           "Must not show private files before the PIN.", "P0", platform="Phone and medium windows")
    matrix(s, "Chips", "Video Playlist chip opens playlists",
           "Phone Home. Tap Video Playlist.",
           "VIDEO PLAYLISTS opens.",
           "Must not open music playlists.", "P1", platform="Phone and medium windows")
    matrix(s, "Chips", "Shortcuts at bottom moves the chips",
           "View menu → Advanced → Show shortcuts at bottom → Done.",
           "Chips render below the list with navigation-bar padding. The option exists only on the phone layout.",
           "Wide Home must not show these chips or this advanced option.", "P1")
    matrix(s, "Empty", "Scanning copy while the first scan runs",
           "Phone, folder view, library still loading, no folders yet.",
           "Text is Scanning your drives…",
           "Must not say No videos found while still scanning in folder mode. Files mode uses the file-list empty title instead.",
           "P1")
    matrix(s, "Empty", "Android empty state has no Grant button",
           "Deny media permission. Open Home in folder view with no folders.",
           "Copy is No videos found. Grant media access to scan your device. There is no button.",
           "Must not invent an in-app permission button. Must not loop the system dialog.",
           "P0", platform="Android")
    matrix(s, "Empty", "Web with no folder chosen asks to open a folder",
           "Web, no sources.",
           "Choose a folder to add its videos. Every subfolder is included. Button Open folder.",
           "Must not claim the whole device was scanned.",
           "P0", platform="Web")
    matrix(s, "Empty", "Web sources not yet connected",
           "Web, sources saved but not connected.",
           "Your videos will appear here once your folders are connected.",
           "Must not show a false empty library as if the user has no videos.",
           "P1", platform="Web")
    matrix(s, "FAB", "Play last plays that file's folder",
           "A last-played file still exists and Floating button is on. Phone layout.",
           "FAB Play last plays the last file's folder, sorted with current options, starting at that file (or index 0 if missing).",
           "Must not play the whole library. Must not appear on the wide layout. Must hide if the setting is off or the URI is gone.",
           "P0", platform="Phone and medium windows")
    matrix(s, "List", "Tap a folder opens that folder",
           "Folder view with at least one folder. Tap it.",
           "Folder screen opens for that path.",
           "Must not start playback. Must not open a sibling folder.",
           "P0")
    matrix(s, "List", "Folder row shows count and optional size",
           "Folder view, list layout.",
           "Name is first-letter uppercased. Subtitle is N video or N videos. Size is appended when the Size field is on.",
           "Grid must not uppercase the name and must not show the path unless All folders.",
           "P1")
    matrix(s, "List", "NEW tag on folders with unplayed new videos",
           "A folder contains a video modified within the NEW period (default 7 days) that has never been played.",
           "Folder icon gets a NEW tag.",
           "A played video must not keep the folder NEW. Hidden and .nomedia videos must not count when those filters are on.",
           "P1")
    matrix(s, "Wide", "Wide header counts videos and folders",
           "Wide Home, scan finished, folders exist.",
           "Title is Library (or All videos in Files mode). Subtitle is N videos in M folders.",
           "Must not show the phone chips. While scanning, subtitle is Scanning your drives…",
           "P1", platform="Desktop and wide windows")
    matrix(s, "Wide", "Continue watching does not draw a progress bar",
           "Wide grid, last played exists, floating button on.",
           "Card says CONTINUE WATCHING, shows the file title, and a Resume button. Home passes progress = null, so no progress bar.",
           "Resume must not play a file from another folder. Card must not appear when the floating-button setting is off.",
           "P1", platform="Desktop and wide windows")
    matrix(s, "Web sources", "Add folder chip adds a library source",
           "Web Home. Tap Add folder and pick a directory.",
           "A chip appears with the folder name and video count, or name · scanning… while scanning.",
           "Must not drop previously added folders.",
           "P0", platform="Web")
    matrix(s, "Web sources", "Remove folder chip removes only that source",
           "Web, two folders added. Remove one.",
           "Only that source disappears. Its videos leave the library.",
           "Must not clear the other folder or playback history.",
           "P1", platform="Web")
    matrix(s, "Web sources", "Restore access and Pick folder banners",
           "Web, saved source that needs permission or a re-pick.",
           "NeedsPermission: Welcome back… button Restore access. NeedsRepick: This browser can't keep folder access… button Pick folder. Missing: quoted name was moved… button Remove.",
           "Must not delete history when asking to pick the folder again.",
           "P1", platform="Web")


def view_menu():
    s = "Home"
    modes = ["All folders", "Files", "Folders"]
    for m in modes:
        matrix(s, "View menu", f"View mode {m}",
               f"Open View Menu. Choose {m}. Tap Done. Reopen the menu.",
               f"{m} is selected and the list matches that mode. The choice is saved in list view options.",
               "Cancel must discard the change. Done must not navigate away from Home.",
               "P0" if m != "All folders" else "P1")
    for lay in ("List", "Grid"):
        matrix(s, "View menu", f"Layout {lay}",
               f"View Menu → Layout → {lay} → Done.",
               f"Library uses the {lay} layout.",
               "Must not change sort or fields.",
               "P1")
    for key in ["Title", "Date", "Played time", "Status", "Length", "Size", "Resolution", "Path", "Frame rate", "Type"]:
        matrix(s, "View menu", f"Sort by {key}",
               f"View Menu → Sort → {key} → Done.",
               f"Files or folders reorder by {key}. Title uses natural order so Ep 2 is before Ep 10. Folder sort uses the folder name for Title, Status, Resolution, Frame rate, and Type.",
               "Must not drop items. Direction labels stay A to Z / Z to A even for size and date.",
               "P1" if key in ("Title", "Date", "Size") else "P2")
    matrix(s, "View menu", "Z to A reverses the current sort",
           "Sort by Title, choose Z to A, Done.",
           "Order is reversed.",
           "Must not switch the sort key.",
           "P1")
    for field in ["Thumbnail", "Length", "File extension", "Played time", "Resolution", "Frame rate", "Path", "Size", "Date"]:
        matrix(s, "View menu", f"Field {field} can be shown and hidden",
               f"View Menu → Fields → toggle {field} → Done.",
               f"The row shows or hides {field}. Default fields are Thumbnail and Length.",
               "Must not toggle a different field.",
               "P2")
    for label, effect in [
        ("Display length over thumbnail", "Duration draws on the thumbnail instead of in the subtitle line."),
        ("Show hidden files and folders", "Dot-folders and hidden files appear. Off, they stay out."),
        ("Recognize .nomedia", "Folders containing .nomedia, and their descendants, are excluded. Off, they are scanned."),
        ("Show thumbnail on videos", "Off shows a plain video icon."),
        ("Show thumbnail on folders", "Off shows a folder icon instead of a frame."),
    ]:
        matrix(s, "View menu", label,
               f"View Menu → Advanced → toggle {label} → Done.",
               effect,
               "Cancel must not persist. The toggle must not change unrelated options.",
               "P1")
    matrix(s, "View menu", "Cancel discards every edit in the dialog",
           "Change view mode, layout, sort, and two fields. Tap Cancel. Reopen.",
           "Previous saved options are unchanged.",
           "Must not write list.view_options.",
           "P0")
    matrix(s, "View menu", "Done persists across relaunch",
           "Change sort to Size, Z to A. Done. Force-stop and relaunch.",
           "Size / Z to A is still selected.",
           "Must not reset to Title / A to Z.",
           "P0")


def folder():
    s = "Folder"
    matrix(s, "Open", "Folder title is the last path segment",
           "From Home, open a folder named Movies.",
           "Title is Movies, first letter uppercased. Wide subtitle is N videos · size, even if empty.",
           "Must not show the parent path as the title.",
           "P0")
    matrix(s, "Open", "Only direct children are listed",
           "Open a folder that has videos and a subfolder with its own videos.",
           "Only files whose folder path equals this folder are listed.",
           "Must not flatten subfolder videos into this list.",
           "P0")
    matrix(s, "Empty", "Empty folder has its own copy",
           "Open a folder with no videos.",
           "This folder is empty.",
           "Must not show the Home grant-access copy or Scanning your drives…",
           "P0")
    matrix(s, "Nav", "Back returns to Home",
           "Home → folder → back.",
           "Home is shown. Phone back content description is Navigate up. Wide back label is Back.",
           "Must not exit the app.",
           "P0")
    matrix(s, "Nav", "Search from a folder searches all videos",
           "Inside a folder, tap Search and type a title that exists only in another folder.",
           "That video is found.",
           "Must not limit search to the current folder.",
           "P1")
    matrix(s, "View", "View options are shared with Home",
           "Set Grid on Home. Open a folder.",
           "The folder list uses the same saved options.",
           "Must not keep a separate unsaved layout.",
           "P1")
    matrix(s, "List", "Last played file in this folder is marked",
           "Last media in each folder is on. Play a file here, return.",
           "That row title uses the primary colour. Scroll-to-last only runs in list layout when that setting is on and the URI is present.",
           "Grid must not jump via the list scroll state. A file in another folder must not be marked here.",
           "P1")
    matrix(s, "Actions", "No folder-level delete or rename",
           "Open a folder and inspect the top bar.",
           "Only back, search, and view options.",
           "Must not delete or rename the folder from this screen.",
           "P1")


def search():
    s = "Search"
    matrix(s, "Open", "Phone search field placeholder",
           "Phone. Open Search from Home.",
           "Top bar title Search. Field placeholder Search videos. Back is Navigate up.",
           "Must not show results before a character is typed.",
           "P0", platform="Phone and medium windows")
    matrix(s, "Query", "Live filter matches the file name only",
           "Type a partial title that appears in the file name but not as a folder name.",
           "Matching videos appear as you type. Matching is case-insensitive contains on the name.",
           "Must not match path-only text. Must not write recent history on each keystroke.",
           "P0")
    matrix(s, "Query", "Blank query hides the file list",
           "Type a query, then delete it.",
           "Results are cleared. File list is not shown. If history exists, Recent searches is shown.",
           "Must not show every video. Must not show No results for \"\".",
           "P0")
    matrix(s, "Query", "No results copy includes the query",
           "Type zzz-no-such-video.",
           "No results for \"zzz-no-such-video\".",
           "Must not show unrelated files.",
           "P1")
    matrix(s, "History", "IME Search saves a recent query",
           "Type a query and press the keyboard Search action.",
           "Query is prepended to recent searches, deduped, capped at 20. Blank is ignored.",
           "Must not save on each character.",
           "P1")
    matrix(s, "History", "Tap a recent search fills the query and does not play",
           "With history, open Search and tap a past query.",
           "The query is applied and results filter. Nothing plays.",
           "Must not open the player.",
           "P1")
    matrix(s, "History", "Clear history removes recent searches",
           "Settings → General → Clear history → OK. Open Search.",
           "Recent searches is gone. Playback history used for NEW and last-played is also cleared.",
           "Cancel on the confirm dialog must not clear history. Playlists and the private folder must stay.",
           "P0")
    matrix(s, "Wide", "Wide subtitle counts results including zero",
           "Wide Search. Type a query with no matches.",
           "Subtitle is 0 results. Blank query has a null subtitle.",
           "Must not show an in-page search field when the title bar already has one.",
           "P1", platform="Desktop and wide windows")
    matrix(s, "Play", "Tap a result plays that video",
           "Search finds two videos. Tap the second.",
           "Player opens on that file. Queue is the result list.",
           "Must not play the first result.",
           "P0", platform="Android", note="Other platforms open the embedded player if the engine can start.")


def file_actions():
    s = "File Actions"
    matrix(s, "Play", "Tap a row plays when nothing is selected",
           "Files view or a folder with videos. Tap a row.",
           "That file plays. On Android the player activity opens.",
           "Must not enter selection mode.",
           "P0")
    matrix(s, "Select", "Long-press starts selection on phone rows",
           "Phone list or phone grid. Long-press a video.",
           "Selection bar appears. Row is selected. System back clears selection and stays on this screen.",
           "Must not pop the screen. Wide grid cards have no long-press, so selection cannot be started that way.",
           "P0", platform="Phone and medium windows")
    matrix(s, "Select", "Thumbnail tap selects only when the setting is on",
           "Select thumbnail is off. Tap a thumbnail.",
           "The video plays.",
           "Turn the setting on: the same tap toggles selection and must not play.",
           "P1")
    matrix(s, "Select", "Selection Play uses only the selected files",
           "Select two files that are not the first in the list. Tap Play.",
           "Playback starts at the first selected file, queue is the selection.",
           "Must not play the full folder.",
           "P0")
    matrix(s, "Select", "Select all and clear",
           "Select one file. Tap Select all. Tap it again.",
           "First tap selects every visible file. Second tap clears the selection.",
           "Must not include files filtered out by search or hidden-file settings.",
           "P1")
    matrix(s, "Tags", "NEW and SUB tags",
           "A never-played file modified within the NEW period, with a sibling .srt that starts with the title.",
           "Row shows NEW and SUB. Played files lose NEW. Wide cards get NEW only, not SUB.",
           "Must not tag an old played file as NEW.",
           "P1")
    matrix(s, "Row", "Played row is dimmed and last-played is primary",
           "Last media in each folder is on. Play one file in the folder, return.",
           "That title is primary. Other files with history use the variant colour. Unplayed files use the normal colour.",
           "Must not mark a file from another folder.",
           "P1")
    for label, expected, must, pri in [
        ("Properties", "Dialog title Properties. One file shows File, Location, Size with byte count, Date, Length, Resolution or an em dash, and Format. Several files show counts and totals. OK closes it.",
         "Must not change the file. Must not clear selection.", "P1"),
        ("Share", "Android opens a share sheet titled Share. Video MIME unless every file is audio. Desktop reveals the first local file in the file manager. Web copies or shares http links and refuses local files. iOS share is a silent no-op.",
         "Must not upload the file. Desktop must not pretend to share a link. iOS must not show a sheet that the code does not implement.", "P1"),
        ("Add To Playlist", "Dialog lists video playlists, not music ones, as name (count). + Create New Playlist. The close button says Cancel.",
         "Must not add the file if Cancel is tapped. A blank new name does not create a playlist.", "P1"),
        ("Lock in Private Folder", "If no PIN, toast Set up the Private Folder first (Me → Private Folder) and the file stays. After a PIN exists, the file is copied into private storage and the original is deleted only if the copy succeeds. Toast says N file(s) locked in Private Folder.",
         "Must not delete a file that failed to lock. There is no confirm dialog. Selection clears even if lock fails.", "P0"),
    ]:
        matrix(s, "Sheet", label, f"Open the row ⋮ menu and tap {label}.", expected, must, pri)
    matrix(s, "Sheet", "Unavailable tools toast and do not navigate",
           "Open ⋮. Tap File Transfer, Convert to MP3, Cut, and Search Subtitle.",
           "Each toasts that the tool is not available in Max Player Pro. Android shows a system toast.",
           "Must not open a converter, editor, or subtitle site. Outside the player, Desktop/iOS/Web may not show a snackbar because notices are collected by the video screen.",
           "P1")
    matrix(s, "Delete", "Delete asks, and Cancel keeps the file",
           "⋮ → Delete, or selection Delete.",
           "Dialog title Delete. One file: Delete \"name\"? Several: Delete N files? Buttons DELETE and CANCEL.",
           "CANCEL must not delete and must not clear selection. DELETE removes the file and, if Delete subtitle files together is on, sibling subtitles.",
           "P0")
    matrix(s, "Delete", "Editing disabled blocks delete and rename",
           "Settings → General → Allow editing off.",
           "Sheet Rename and Delete are disabled. Selection-bar Delete toasts Editing is disabled in Settings → General and does not delete.",
           "Must not delete. Must not hide the rows, only disable them.",
           "P0")
    matrix(s, "Delete", "Android 11+ may show a system consent dialog",
           "Android 11 or newer. Confirm DELETE.",
           "The system may ask to allow the delete. Deny: the file remains. The selection may already have been cleared and there is no app toast.",
           "Must not report success if the file is still there.",
           "P1", platform="Android")
    matrix(s, "Delete", "Web and iOS cannot delete files",
           "Web or iOS. Confirm DELETE.",
           "The platform delete returns false. The file remains. Rename toasts Rename failed.",
           "Must not expect the file to disappear.",
           "P0", platform="Web, iOS")
    matrix(s, "Rename", "Rename keeps the extension",
           "Rename a file video.mkv to Holiday. OK.",
           "Stored name is Holiday.mkv. Toast Renamed on success, Rename failed if the target exists or consent is denied.",
           "CANCEL must keep the old name. A name with no dot must not drop the extension.",
           "P0")
    matrix(s, "Info", "Selection Info does not clear the selection",
           "Select two files. Tap Info. Tap OK.",
           "Properties shows 2 files, total size, and total length. Selection remains.",
           "Must not play or delete.",
           "P2")


def music():
    s = "Music"
    matrix(s, "Open", "Music header and tabs",
           "Open Music from the chip or sidebar.",
           "Title Music. Tabs: Tracks, Playlists, Albums, Artists, Folders. Summary counts songs and duration.",
           "Must not list videos. While the first scan runs, a skeleton is shown, not a false empty library.",
           "P0")
    matrix(s, "Empty", "No music found after scan",
           "Device with no audio, scan finished.",
           "No music found.",
           "Must not show video files as songs.",
           "P1")
    matrix(s, "Tracks", "Play all starts at the first filtered song",
           "Tracks tab, search empty. Tap Play all.",
           "Music session starts at index 0 of the filtered list. Mini player appears.",
           "Must not shuffle unless Shuffle was used.",
           "P0", platform="Android", note="Other platforms depend on the unshipped engine.")
    matrix(s, "Tracks", "Shuffle is a one-shot reorder",
           "Tap Shuffle.",
           "The filtered list is shuffled and playback starts at the new index 0. The engine shuffle flag stays off.",
           "Must not keep reshuffling on every next track.",
           "P1", platform="Android")
    matrix(s, "Search", "Search matches title, artist, and album",
           "Type part of an artist name.",
           "Placeholder is Search songs, artists, albums…. Matching songs remain.",
           "Must not match unrelated folders only.",
           "P1")
    for key in ("Title", "Date", "Length", "Size"):
        matrix(s, "Sort", f"Sort by {key}",
               f"Open Sort and choose {key}.",
               f"Label becomes Sort: {key}. Rows reorder.",
               "Must not offer the video-only sort keys (Resolution, Frame rate, Status).",
               "P2")
    matrix(s, "Layout", "Wide grid and list toggle",
           "Wide Music, Tracks tab. Tap Grid, then List.",
           "Layout switches. Compact has no this toggle.",
           "Must not change the tab.",
           "P2", platform="Desktop and wide windows")
    matrix(s, "Playlists", "Create, Favourites, and Recently Played cards",
           "Playlists tab.",
           "Cards: New playlist / Create, Favourites, Recently Played, then user playlists.",
           "A blank playlist name must not create a playlist. Favourites lists only hearted URIs.",
           "P1")
    matrix(s, "Row", "Favourite from the row menu",
           "Open a track ⋮ or heart. Add to Favourites, then Remove from Favourites.",
           "Heart state flips. Favourites card gains and loses the song.",
           "Must not delete the file.",
           "P1")
    matrix(s, "Row", "Add to playlist from a song",
           "⋮ → Add to Playlist → pick one, and also + Create New Playlist.",
           "Song URI is added. Dialog title Add To Playlist. Cancel closes without adding.",
           "Must not add it to every playlist.",
           "P1")
    matrix(s, "Groups", "Album, artist, and folder drill-in",
           "Open an album, then Play and Shuffle in the header. Back.",
           "Only that group's songs play. Back returns to the tab.",
           "Unknown artist, Unknown album, and Unknown folder are the empty labels.",
           "P1")
    matrix(s, "Mini", "Mini player is hidden during video and when idle",
           "Stop music. Then play a video.",
           "Mini player hides when the queue is empty and whenever this is not a music session.",
           "Must not cover the video transport.",
           "P0")
    matrix(s, "Mini", "Narrow mini player controls",
           "Phone, music playing.",
           "Cover, title, artist if known, Play/Pause, Next, Close. Progress is not seekable. Close stops and clears the queue.",
           "Must not show Previous, shuffle, or repeat on the narrow bar.",
           "P0", platform="Phone")
    matrix(s, "Mini", "Wide mini player seek, volume, and repeat",
           "Wide window, music playing.",
           "Favourite, Shuffle, Previous, Play/Pause, Next, Repeat. Seek commits on release. Volume toggles mute and the slider sets player volume 0 to 1.",
           "Previous is the engine previous, not the video Smart Previous. Close clears the queue.",
           "P1", platform="Desktop and wide windows")


def now_playing():
    s = "Now Playing"
    matrix(s, "Open", "Tap the mini player opens Now Playing",
           "Music is playing. Tap the mini player bar (not a button).",
           "Now Playing opens. Compact header says NOW PLAYING. Back pill says Back.",
           "Must not stop playback.",
           "P0")
    matrix(s, "Transport", "Play and pause",
           "Tap Pause, then Play.",
           "Audio pauses and resumes. The icon swaps.",
           "Must not skip to the next track.",
           "P0", platform="Android")
    matrix(s, "Transport", "Next and previous",
           "Queue of three songs. Tap Next, then Previous.",
           "Next advances. Previous uses the engine previous, not the video 3-second smart rule.",
           "Must not apply Next/Prev → FF/Rew from video settings.",
           "P0", platform="Android")
    matrix(s, "Seek", "Drag previews and commits on release",
           "Drag the seek bar, then release.",
           "Time preview follows the finger. Position changes on release. Right side shows remaining time or --:--.",
           "Must not seek on every pixel if the finger is still down, beyond the preview.",
           "P1", platform="Android")
    matrix(s, "Speed", "Speed sheet only offers the music chips",
           "Tap Speed.",
           "Title Playback speed. Chips 0.5x, 0.75x, 1x, 1.25x, 1.5x, 2x. Choosing one sets engine speed and closes the sheet.",
           "Must not show the video 0.25x–4x slider. Speed is shared with a later video on the same engine.",
           "P1")
    matrix(s, "A-B", "Three taps set, enable, and clear A-B",
           "Tap A-B three times.",
           "First toast A point set. Second toast A-B repeat on, B at least 500 ms after A. Third toast A-B repeat off.",
           "Must not use the video A-B panel state.",
           "P1", platform="Android")
    matrix(s, "Queue", "Up next search and jump",
           "Open the queue. Search for a title. Tap it. Search for nonsense.",
           "Wide shows Up next and N tracks. Placeholder Search in queue. No match says No matching tracks. Tap seeks to that index at 0 and plays.",
           "Must not delete the track.",
           "P1")
    matrix(s, "More", "Share and add to playlist",
           "Tap More, then Share, and Add to playlist.",
           "Share uses the platform share. Add to playlist is present because Music passes the slot.",
           "Lyrics always toasts No lyrics found and must not open a lyrics view.",
           "P1")
    matrix(s, "Equalizer", "Button is visible even when the effect engine is missing",
           "iOS or Web. Tap Equalizer.",
           "The panel opens and says Equalizer is not available for this audio output.",
           "Must not show live bands that do nothing. Android and desktop VLC show On/Off, presets, reverb, and bass boost.",
           "P1")
    matrix(s, "Favourite", "Heart toggles without leaving the screen",
           "Tap the heart.",
           "Filled pink when favourite. Favourites playlist updates.",
           "Must not pause.",
           "P2")


def video():
    s = "Video Player"
    matrix(s, "Open", "Video opens and plays",
           "Android. Tap a video.",
           "Player activity opens, surface shows the frame, playback starts unless a resume dialog is waiting.",
           "Must not stay on a black screen with audio from a different file. Title is the file title.",
           "P0", platform="Android")
    matrix(s, "Open", "Loading circle can be turned off",
           "Settings → Player → Loading circle animation off. Open a slow file.",
           "No buffering spinner.",
           "Must not hide the video itself.",
           "P2", platform="Android")
    matrix(s, "Chrome", "Navigate up closes the player",
           "Tap Navigate up.",
           "Android finishes the activity and stops the engine. Desktop/Web clears the overlay.",
           "Must not delete the file. Must not return to a random screen.",
           "P0")
    matrix(s, "Chrome", "Single back asks twice when the setting is on",
           "Settings → Player → Double-tap the back button on. Press system back once, wait, press again within 2 seconds, then again after 2 seconds.",
           "First press toasts Press back again to exit and stays. A second press within 2 seconds closes. After 2 seconds the warning repeats.",
           "If a panel is open, the first back closes the panel and must not exit. If locked, back only shows the lock icon and must not unlock or exit.",
           "P0", platform="Android")
    matrix(s, "Chrome", "Play and pause",
           "Show controls. Tap Play/Pause. Also try when the file has ended.",
           "Toggles playback. If ended, seeks to 0 then plays.",
           "Must not skip.",
           "P0", platform="Android")
    matrix(s, "Chrome", "Next, previous, and last item",
           "Queue of two videos. Tap Next until the end. Tap Previous after 4 seconds, and again in the first 3 seconds.",
           "Next on the last item toasts Last item. Smart Previous (default on) seeks to 0 if position is over 3 seconds; otherwise it opens the previous file.",
           "If Next/Prev → FF/Rew is on, these buttons seek by the move interval and must not change files.",
           "P0", platform="Android")
    matrix(s, "Chrome", "Previous and next can be hidden",
           "Display Settings → Navigation → Previous/next button off.",
           "Previous and Next disappear. Play/Pause remains.",
           "Must not remove Rewind/Forward unless that separate setting is also off.",
           "P1")
    matrix(s, "Chrome", "Forward and backward buttons follow the move interval",
           "Turn on Forward/backward moving button. Set Move Interval to 15. Tap Forward.",
           "Seeks +15 seconds. Rewind seeks −15.",
           "Buttons are hidden by default and must not appear until the setting is on.",
           "P1")
    matrix(s, "Seek", "Seek bar commits on release",
           "Drag the bar to the middle and release. Toggle Show Remaining Time.",
           "Position changes on release. Right label is remaining time by default, or total duration if that setting is off. Elapsed is on the left.",
           "Must not jump to the end. A-B caption appears only after A is set.",
           "P0", platform="Android")
    matrix(s, "Zoom", "Zoom cycles Fit, Stretch, Crop, Original",
           "Tap Zoom four times.",
           "Order is Fit → Stretch → Crop → Original → Fit. Pinch and pan reset. Toast is the short name Fit, Stretch, Crop, or Original.",
           "Must not open the aspect-ratio panel.",
           "P1")
    matrix(s, "Lock", "Lock blocks every gesture",
           "Tap Lock. Try seek, volume, brightness, double-tap, and long-press.",
           "Transport hides. Only the small lock icon can return, for 2.5 seconds after a touch.",
           "Must not seek or change volume. Normal lock: tap the icon unlocks. Kids lock: tap toasts Long press to unlock and stays locked.",
           "P0")
    matrix(s, "PiP", "Popup enters picture-in-picture on Android",
           "Android 8+ with PiP. Tap Popup. Then try on a device without PiP.",
           "Activity enters PiP. Controls, panels, and dialogs hide. Failure toasts Picture-in-picture not supported.",
           "Button must not appear on iOS or desktop. Web shows it only if the browser exposes picture-in-picture.",
           "P0", platform="Android")
    matrix(s, "Fullscreen", "Fullscreen button is desktop and web only",
           "Desktop or Web. Tap Full screen, then Exit full screen. Check Android.",
           "Toggles document or window fullscreen. Android has no this button.",
           "Failure toasts Full screen is not available here.",
           "P1", platform="Desktop, Web")
    matrix(s, "Resume", "Ask at startup shows RESUME and START OVER",
           "Set Resume to Ask at startup. Stop a video after 10 seconds. Reopen it.",
           "Dialog: Do you wish to resume from where you stopped? (time). RESUME continues. START OVER or dismiss starts at 0.",
           "Must not ask if the saved point is within 5 seconds of either end, or on later files when Resume only the first file is on. HTTP links are not saved, so they must not ask.",
           "P0", platform="Android")
    matrix(s, "Resume", "Default Resume skips the dialog and shows the notice",
           "Leave Resume on the default Resume. Reopen a partly watched file.",
           "Playback seeks to the saved point. A notice says Continue from where you stopped. START OVER seeks to 0. Close hides it. It auto-hides in 5 seconds.",
           "Must not show the ask dialog.",
           "P0", platform="Android")
    matrix(s, "Error", "Unplayable file offers OK and sometimes SW",
           "Open a corrupt file, then a file that fails for a non-IO reason while on HW.",
           "Can't play this media. and the code name. OK goes to the next item or closes. TRY SW DECODER appears only when the error is not IO and the decoder is not already SW.",
           "Suppress error message skips to the next video with no dialog, but the dialog still shows if there is no next item.",
           "P0", platform="Android")
    matrix(s, "Error", "All-files access dialog",
           "Android 11+ file that needs all-files access.",
           "Dialog explains All files access. ALLOW opens settings. CANCEL closes the player. If the platform cannot request access, ALLOW closes.",
           "Must not keep a black player open after CANCEL.",
           "P0", platform="Android")
    matrix(s, "Links", "Disabled media links are refused",
           "Play media links off. Open an http(s) video from another app.",
           "Toast Playing media links is disabled in Settings → General. The player finishes.",
           "Must not play the URL.",
           "P1", platform="Android")
    matrix(s, "Background", "Leaving the app follows Background/PiP mode",
           "Set the mode to Stop, then Background play, then Picture-in-Picture. Play a video and switch apps.",
           "Stop pauses. Background play continues audio with the notification. PiP enters PiP when the device supports it. Audio-only can continue when Background play (audio) is on.",
           "Headset disconnect pauses when Pause on headset disconnected is on. Play alone should duck or stop other players.",
           "P0", platform="Android")
    matrix(s, "End", "Back to list closes after the last file",
           "Back to list on. Play the last file to the end with loop off.",
           "Player closes and the list is shown.",
           "Loop Repeat one must restart the same file. Repeat all must restart the queue. Loop off without the setting must stay on the ended frame.",
           "P1", platform="Android")
    matrix(s, "Remember", "Track, zoom, speed, and decoder are remembered per file",
           "Remember selections on. Change audio, subtitle, speed, zoom, and decoder. Leave and reopen that file, then open a different file.",
           "The same file restores those choices. A different file does not inherit them.",
           "Turning the setting off must stop saving new choices.",
           "P1", platform="Android")


def shortcuts():
    s = "Video Player"
    items = [
        ("Night Mode", "Overlay darkens the frame. Toast Night mode on / off. Web also dims the HTML video.", "Must not change system night mode.", "P1"),
        ("Shuffle", "Toast Shuffle on / off. Next picks another random item if the queue has more than one.", "Must not reorder the visible playlist.", "P1"),
        ("Loop", "Cycles Loop off → Repeat one → Repeat all. Toast matches.", "Must not set A-B.", "P1"),
        ("Mute", "Toast Mute / Unmute. Engine volume is 0 or 1.", "Must not change the system volume slider when muted by this button.", "P0"),
        ("Sleep Timer", "Sleep Timer panel opens.", "Must not start a timer until START.", "P1"),
        ("A - B Repeat", "A-B card opens. The shortcut highlights once A is set.", "Must not set B by itself.", "P1"),
        ("Equalizer", "Equalizer panel opens on Android and desktop VLC. Hidden where the capability is false.", "Must not appear on iOS or Web.", "P1"),
        ("Speed", "Speed panel opens. The button shows the speed, such as 1X, when not 1x.", "Must not change speed until a preset or stepper is used.", "P0"),
        ("Screenshot", "Android and desktop save a PNG and snack the folder. Web downloads a PNG or explains cross-site videos. iOS toasts Screenshot is not available on iOS yet.", "Must not crash if there is no frame yet.", "P1"),
        ("Background Play", "Toast Background play on / off. This toggles the session flag, not the settings picker. Hidden off Android.", "Must not enter PiP by itself.", "P1"),
        ("Screen Rotation", "Android flips landscape and portrait. Hidden on other platforms.", "Must not appear on desktop even though rotate() there toggles fullscreen.", "P1"),
        ("Customise Items", "Checklist of shortcuts. Unchecking one removes it from the row. Customise itself cannot be removed.", "Must not hide Play/Pause.", "P2"),
    ]
    for title, expected, must, pri in items:
        matrix(s, "Shortcuts", title,
               f"Show controls. If needed, expand shortcuts. Tap {title}.",
               expected, must, pri, platform="Android",
               note="Collapsed row shows Equalizer, Speed, Screenshot, Background Play, and Screen Rotation when available. Expand shows the rest.")
    matrix(s, "Shortcuts", "Shortcuts switch hides the row",
           "More → Shortcuts off.",
           "The shortcut row disappears. Playback continues.",
           "Must not hide the seek bar.",
           "P2")
    matrix(s, "Shortcuts", "Video Display off keeps audio",
           "More → Video Display off.",
           "Frames hide. Audio continues. Turning it on restores the surface.",
           "Must not pause.",
           "P1", platform="Android")


def gestures():
    s = "Gestures"
    matrix(s, "Tap", "Default tap shows and hides controls",
           "Touch action is Show/hide interface. Tap the video, wait, tap again.",
           "Controls toggle. If a panel is open, the tap closes the panel instead.",
           "Must not pause. Must not fire while locked or during a drag.",
           "P0", platform="Android")
    matrix(s, "Tap", "Pause/resume touch action",
           "Set Touch action to Pause/resume. Tap the video.",
           "Playback toggles. Controls do not toggle.",
           "Show interface + pause must do both.",
           "P1")
    matrix(s, "Double tap", "Sides seek by the move interval",
           "FF/RW (Double tap) on. Move interval 10. Double-tap the right third twice, then the left third.",
           "Right seeks forward and shows an accumulating seconds badge. Left rewinds. Same direction accumulates.",
           "Must not play/pause on the sides while FF/RW is enabled. Center double-tap pauses and shows Playing or Paused.",
           "P0", platform="Android")
    matrix(s, "Double tap", "Center zoom only when that flag is on",
           "Turn on Video zoom (Double tap). Double-tap the center.",
           "Zoom cycles and a toast shows the zoom name.",
           "With the flag off, center double-tap must pause, not zoom.",
           "P1")
    matrix(s, "Seek", "Horizontal swipe seeks",
           "Seek position on. Swipe right, then left.",
           "OSD shows the target time and a + or − offset. Position commits on finger up. Seek speed default is 10 sec/cm.",
           "With the flag off, a horizontal swipe must not seek. With Display the current position off, the thumb must not move live.",
           "P0", platform="Android")
    matrix(s, "Brightness", "Left-half vertical swipe changes brightness",
           "Brightness gesture on. Swipe up and down on the left half.",
           "Left pill shows a brightness icon and a value 0 to 30. Release saves Brightness level. Android also sets window brightness.",
           "Must not change volume. Must not run on the right half. iOS does not change system brightness.",
           "P0", platform="Android")
    matrix(s, "Volume", "Right-half vertical swipe changes volume",
           "Volume gesture on. Volume boost on. Swipe up on the right half past the system max.",
           "Right pill shows volume. Above the system max the bar can enter the boost range, up to 200%, and turns orange.",
           "Must not change brightness. With Volume boost off, it must not exceed 100%. Increasing while muted unmutes.",
           "P0", platform="Android")
    matrix(s, "Zoom", "Pinch zooms and a second finger pans",
           "Pinch in and out. Drag with two fingers. Release near 100%.",
           "Label shows a percent. Release between 95% and 105% snaps to 100%. Subtitle-zone pinch zooms subtitles when that flag is on.",
           "Video zoom off must not scale the video. Video pan off must not pan unless Zoom and Pan is on.",
           "P1", platform="Android")
    matrix(s, "Long press", "Long-press fast-forwards only while held and playing",
           "Speed FF (Long press) on. While playing, long-press without dragging. Release. Repeat while paused.",
           "Top pill says N.NX Speed Playing at the Speed FF value (default 2.0). Release restores the old speed. Paused long-press does nothing.",
           "Must not seek. Must not stick at 2x after release. Locked screen must ignore it.",
           "P0", platform="Android")
    matrix(s, "Subtitles", "Drag subtitles up and down",
           "A subtitle line is visible. Subtitle up/down on. Drag vertically in the bottom 28%.",
           "Subtitle offset moves.",
           "Must not seek. Must not move subtitles when no line is visible or the flag is off.",
           "P1", platform="Android")
    matrix(s, "Unwired", "Playback Speed gesture flag does nothing",
           "Controls → Gestures → Playback Speed on. Try every swipe.",
           "No swipe changes playback speed. Speed still changes from the Speed panel.",
           "Must not treat this checkbox as a working gesture.",
           "P2")
    matrix(s, "Hide", "Controls auto-hide while playing",
           "Interface auto hide on, delay 2 seconds. Play and do not touch. Pause.",
           "Controls hide after the delay only while playing. An open panel is not hidden by the timer.",
           "With the setting off, controls must stay until tapped.",
           "P1")
    matrix(s, "OSD", "Persistent clock and elapsed time",
           "Hide controls. Turn on Elapsed time and Battery/clock.",
           "A small OSD shows elapsed time and, on the status flag, clock and battery percent. Place at the bottom moves it.",
           "Must not show while controls are visible or while locked. Empty flags draw nothing.",
           "P2")
    keys = [
        ("Space or K", "Play and pause. No center pill.", "P0"),
        ("Left and Right", "Seek by the move interval. Shift seeks 1 minute. Ctrl or Cmd with arrows changes items.", "P0"),
        ("J and L", "Seek 10 seconds, ignoring the move interval.", "P1"),
        ("Up and Down", "Volume up and down by one system step.", "P0"),
        ("Ctrl or Cmd with Up and Down", "Brightness ±2, saved, clamped 0 to 30.", "P1"),
        ("M", "Mute toggle and toast.", "P1"),
        ("F", "Fullscreen. Failure toasts Full screen is not available here.", "P1"),
        ("Esc", "Closes an open panel, otherwise leaves fullscreen.", "P1"),
        ("Bracket keys", "[ and ] change speed by 0.25. Backslash resets to 1.0. Toast Speed N.NNX.", "P1"),
        ("Number keys", "0 through 9 jump to 0% through 90%.", "P1"),
        ("Home and End", "Home seeks to the start. End seeks near the end.", "P2"),
        ("S", "Screenshot, unless Ctrl or Cmd is held.", "P2"),
        ("C", "Subtitles on or off, with a toast. Does not unload an external file.", "P1"),
    ]
    for title, expected, pri in keys:
        matrix(s, "Keyboard", title,
               f"Desktop or Web player, unlocked. Press {title}.",
               expected,
               "Must not run on Android or iOS. Must not run while locked.",
               pri, platform="Desktop, Web")


def panels():
    s = "Player Panels"
    matrix(s, "More", "Overflow grid opens the matching panel",
           "Tap More. Tap each of Aspect Ratio, Display Settings, Playlists, Information, Bookmark, and More.",
           "Each opens its panel. Back returns to the overflow where a back control exists. Tap outside closes the panel.",
           "Must not start a different action.",
           "P0", platform="Android")
    matrix(s, "More", "Speed FF tile cycles 1.5, 2, 3, 4",
           "Tap the Speed FF (Long Press) tile several times.",
           "The badge cycles 1.5X, 2X, 3X, 4X and the long-press speed setting updates.",
           "Must not change the current playback speed until a long-press.",
           "P1")
    matrix(s, "More", "Tutorial lists the gesture copy",
           "More → Tutorial.",
           "Title Gestures. Body lists swipe seek, left brightness, right volume, double-tap sides, pinch, two-finger pan, long-press, and subtitle drag. OK closes it.",
           "The tutorial does not mention kids lock. QA should follow the flags, not the tutorial, when a flag is off.",
           "P2")
    matrix(s, "Network", "Network stream requires a scheme",
           "More → Network Stream. Try example.com, then http://example.com/video.mkv. Also CANCEL.",
           "Title Network Stream. OK does nothing and stays open if the text has no ://. A URL with :// calls playUrl and closes. CANCEL does not play.",
           "Must not play a schemeless string.",
           "P1")
    matrix(s, "Audio", "Audio track list and disable",
           "Open Audio Track on a file with two tracks, and on a file with none.",
           "Radios use the engine labels. Empty says No audio tracks found. Disable clears the selection where the browser can switch.",
           "Web without native tracks opens Switch audio in {browser} instead of switching. Use SW audio decoder appears only where decoder choice exists.",
           "P0", platform="Android")
    matrix(s, "Decoder", "HW, HW+, and SW",
           "Android. Tap the HW/HW+/SW chip. Pick each row.",
           "Dialog title Select decoder. Choosing one restarts playback at the same position and toasts the label. HW+ behaves like HW.",
           "The chip must not appear on iOS or Web. Desktop shows it only when VLC is available.",
           "P0", platform="Android")
    matrix(s, "Subtitles", "Embedded track, disable, and Open",
           "Open Subtitles. Select a track, tap it again, then Open and pick an .srt and a .txt.",
           "Selected track shows a check. Tapping it disables subtitles. Open accepts a file. .srt and .vtt load and toast Subtitle: name. Other extensions toast Unsupported subtitle format. Read failure toasts Can't read subtitle.",
           "Sync and speed steppers stay disabled for embedded-only subtitles. Same-name .srt/.vtt beside the video loads automatically.",
           "P0", platform="Android")
    matrix(s, "Subtitles", "External sync and speed",
           "Load an external subtitle. Step Synchronization and Speed.",
           "Sync steps by 100 ms and shows N.Ns. Speed steps by 0.1% and stays between 50% and 200%.",
           "Must not change the video playback speed.",
           "P1")
    matrix(s, "Speed", "Stepper, presets, reset, and custom value",
           "Open Speed. Tap Decrease, Increase, 0.25x, 2x, 4x, Reset, and the center value.",
           "Range is 0.25 to 4. Center edit dialog uses an x suffix, OK saves a clamped value, CANCEL discards. Reset returns to 1.00.",
           "Must not accept 0 or 10.",
           "P0", platform="Android")
    matrix(s, "Equalizer", "On, presets, bass, and reverb",
           "Android. Open Equalizer. Toggle On. Pick a preset, move a band, set Bass Boost and Reverb, then Off.",
           "Off disables the sliders. Custom appears when bands are edited. Values persist.",
           "If effects are null, the only copy is Equalizer is not available for this audio output. Must not pretend bands are applied.",
           "P1", platform="Android")
    matrix(s, "Sleep", "Keypad, START, and STOP",
           "Open Sleep Timer. Enter 0, then 15, toggle Play last media to the end, tap START. Reopen and tap STOP.",
           "Leading zero is ignored. START is disabled at 0. A valid START toasts Sleep timer: h m and closes. STOP clears it. The shortcut stays highlighted while it runs.",
           "Must not pause immediately on START. On fire, it pauses unless finish-last is set, in which case it pauses at the end of the item.",
           "P1")
    matrix(s, "A-B", "A then B, and B before A",
           "Open A-B. Tap B first. Tap A, then B earlier than A, then B later.",
           "B is disabled until A is set. B before A toasts B must be after A and does not change. A valid B seeks to A and closes. Close clears A-B entirely.",
           "Must not loop before both points exist.",
           "P1")
    matrix(s, "Aspect", "Each ratio and apply to all",
           "Open Aspect Ratio. Pick Default, 1:1, 4:3, 16:9, and Custom. Toggle Apply to all videos.",
           "The frame uses that ratio. Custom shows Custom N.NN:1 from 0.5 to 3. Apply to all writes the default, except Custom.",
           "Must not change the zoom mode.",
           "P1")
    matrix(s, "Info", "Information rows",
           "Open Information.",
           "Sections File, Media, Video, and Audio as available. Missing video or audio sections are omitted. Subtitle says the external name, Embedded subtitle, or None.",
           "Must not show another file's path.",
           "P2")
    matrix(s, "Bookmark", "Add, seek, delete, and import",
           "Open Bookmark. Tap Add. Tap the row. Delete it. Import a text file with 0:10 and 1:02:00, and an empty file.",
           "Empty says No bookmarks yet. Add stores the current position. Tap seeks. Delete removes that bookmark. Import toasts N bookmarks imported or No bookmarks found in file.",
           "Add is disabled with no current file. Times outside the duration are dropped.",
           "P1")
    matrix(s, "Playlist", "Queue jump",
           "Open Playlists from the player overflow.",
           "Title Playlist (N). The current item is highlighted. Tap another title jumps there and closes the panel.",
           "Must not open the Me video-playlist editor.",
           "P1")
    matrix(s, "Tools", "Delete and rename from the player",
           "More panel. On a content: or file: URI with editing allowed, tap Delete, cancel, then delete and confirm. Rename and cancel.",
           "Delete asks Delete \"name\"? CANCEL keeps the file. DELETE deletes and moves to the next item or closes. Rename keeps the extension and toasts Renamed or Rename failed.",
           "Rows are hidden on Web and iOS, and hidden for http URIs. Lock here is the same as the transport lock.",
           "P0", platform="Android, Desktop")
    matrix(s, "Help", "Settings, FAQ, updates, and bug report",
           "Player More panel. Tap Settings, FAQ, Check for updates, and Bug Report.",
           "Settings opens the app Settings. FAQ opens Help. Check for updates is Android-only and opens the store, or toasts You're on the latest version. Bug Report opens a mailto for Max Player Pro bug report, or toasts No email app found.",
           "Must not show Check for updates on desktop.",
           "P2")
    matrix(s, "Share", "Share the current video",
           "More → Share.",
           "Platform share runs for the current file.",
           "Must not share if there is no current file.",
           "P1")


def settings():
    s = "Settings"
    matrix(s, "Index", "Settings index lists every page",
           "Me → Settings, or the sidebar Settings.",
           "Title Settings. Rows: List, Player, Decoder, Audio, Subtitle, General, Development, App Language.",
           "Must not hide a page. Back returns to the previous screen.",
           "P0")
    matrix(s, "Index", "Development is an explanation, not a lab",
           "Tap Development.",
           "Title Development. Body says developer diagnostics are not exposed in this build.",
           "Must not show codec toggles.",
           "P2")
    matrix(s, "Index", "App Language opens the system screen on Android",
           "Android 13+ tap App Language. Repeat on Android 12, iOS, desktop, and Web.",
           "Android 13+ opens this app's locale settings. Older Android opens system locale settings. Other platforms do nothing and show no toast.",
           "Must not show an in-app language list.",
           "P1")
    # bools: page, section, title, default, summary, pri
    bools = [
        ("List", "Appearance", "Last media in each folder", True, "Marks the last played file in each folder.", "P1"),
        ("List", "Appearance", "Scroll down to last media", False, "List layout scrolls to that file when the folder opens.", "P1"),
        ("List", "Appearance", "Select thumbnail", False, "Thumbnail tap selects instead of playing.", "P1"),
        ("List", "Appearance", "Floating button", True, "Phone Play last button shows or hides.", "P0"),
        ("List", "Scan", "Refresh automatically", True, "Android refreshes the library when the activity resumes only while this is on.", "P1"),
        ("Player", "Interface", "Double-tap the back button", False, "Android player back requires a second press within 2 seconds.", "P0"),
        ("Player", "Interface", "Quick zoom", True, "The checkbox saves only. No player code reads it, so zoom still cycles Fit, Stretch, Crop, Original. Do not fail a missing 150% step.", "P2"),
        ("Player", "Playback", "Resume only the first file", False, "Later files in the same session start at 0 when on.", "P1"),
        ("Player", "Playback", "Remember selections", True, "Per-file audio, subtitle, decoder, speed, and zoom are saved.", "P1"),
        ("Player", "Playback", "Back to list", False, "Player closes when playback completes.", "P1"),
        ("Player", "Playback", "Preview while seeking", False, "The checkbox saves only. No player code reads it, so no preview frame appears. Do not fail a missing thumbnail.", "P2"),
        ("Player", "Playback", "Fast seeking", True, "Seek lands on the nearest keyframe. Applied to a live player.", "P1"),
        ("Player", "Playback", "Play alone", True, "Other players stop. Applied live.", "P1"),
        ("Player", "Playback", "Media buttons", True, "Headset and Bluetooth controls are obeyed.", "P1"),
        ("Player", "Playback", "Double/Triple Press → Next/Prev", True, "Headset hook multi-press maps to next and previous.", "P2"),
        ("Player", "Playback", "Next/Prev → FF/Rew", False, "Media next/prev seek instead of changing items.", "P1"),
        ("Player", "Playback", "Smart Previous Button", True, "Previous restarts the file after 3 seconds.", "P0"),
        ("Player", "Playback", "Suppress error message", False, "A failed file skips to the next one without the error dialog.", "P1"),
        ("Player", "Background Play", "Background play (audio)", True, "Audio-only playback can continue in the background.", "P1"),
        ("Player", "Background Play", "Album art", True, "The checkbox saves only. Lock-screen art does not read this key. Do not fail a missing cover.", "P2"),
        ("Player", "Background Play", "Smooth switch", False, "The checkbox saves only. Leaving the app does not fade because of this row. Fade on start and Fade on seek are separate.", "P2"),
        ("Player", "Miscellaneous", "Loading circle animation", True, "Buffering spinner shows or hides.", "P2"),
        ("Audio", None, "Audio player", False, "Playback is treated as audio.", "P2"),
        ("Audio", None, "Volume boost", True, "Volume gesture can pass 100% up to 200%.", "P0"),
        ("Audio", None, "System volume", True, "Player volume follows the system media volume.", "P1"),
        ("Audio", None, "Pause on headset disconnected", True, "Unplugging a headset pauses. Applied live.", "P0"),
        ("Audio", None, "Fade on start", True, "Audio fades in when playback starts.", "P2"),
        ("Audio", None, "Fade on seek", True, "Audio fades in after a seek.", "P2"),
        ("Audio", None, "Audio equalizer", False, "Enables the effect path. It does not open the equalizer panel.", "P1"),
        ("Subtitle", "Text processing", "Italic effect", False, "Lines starting with / render in italics.", "P2"),
        ("Subtitle", "Text processing", "Force LTR direction", True, "RTL subtitle lines are forced left to right.", "P2"),
        ("General", None, "Play media links", True, "http(s) links from other apps can open the player.", "P1"),
        ("General", None, "Quit Button", False, "Me shows or hides Quit. There is no confirm on Quit.", "P1"),
        ("General", "Edit", "Allow editing", True, "Rename and Delete enable or disable.", "P0"),
        ("General", "Edit", "Delete subtitle files together", True, "Deleting a video also deletes sibling subtitle files.", "P1"),
        ("General", "User data", "Cache thumbnail", True, "Thumbnails are cached.", "P2"),
    ]
    for page, section, title, default, effect, pri in bools:
        where = f"Settings → {page}" + (f" → {section}" if section else "")
        matrix(s, page, f"{title} toggles and persists",
               f"{where}. Note the default ({'on' if default else 'off'}). Toggle it, leave the page, force-stop, return.",
               f"The checkbox matches the saved value. Effect: {effect}",
               "Must not change a setting on another page. Cancel is not involved; the row applies immediately.",
               pri)
    choices = [
        ("Player", "Playback", "Resume", ["Ask at startup", "Resume", "Start over"], "Resume", "P0"),
        ("Player", "Background Play", "Background/PiP mode", ["Stop", "Background play", "Picture-in-Picture"], "Picture-in-Picture", "P0"),
        ("Decoder", None, "Default decoder", ["HW decoder", "HW+ decoder", "SW decoder"], "HW decoder", "P0"),
        ("Subtitle", None, "Character encoding", ["Auto detect", "UTF-8", "Western (ISO-8859-1)", "Arabic (Windows-1256)", "Cyrillic (Windows-1251)"], "Auto detect", "P2"),
    ]
    for page, section, title, options, default, pri in choices:
        matrix(s, page, f"{title} offers every choice",
               f"Settings → {page}. Open {title}.",
               f"Radio dialog title is {title}. Choices: {', '.join(options)}. Default is {default}. A pick applies immediately and closes. Cancel keeps the old value. There is no OK.",
               "Must not apply a choice when Cancel or outside dismiss is used.",
               pri)
    matrix(s, "Decoder", "Decoder page explains HW and SW",
           "Open Decoder.",
           "Static text says HW prefers hardware, SW prefers software, and HW+ behaves like HW.",
           "Must not claim HW+ is a separate custom codec in this build.",
           "P2")
    matrix(s, "Player", "Theme and Style both open the Style tuner",
           "Player page. Tap Theme, back, tap Style.",
           "Both open the tuner tab titled Style, not Theme. The Theme row shows the frame and progress colours.",
           "Must not open App Theme.",
           "P1")
    for label, summary in [
        ("Screen", "Screen settings: Orientation, Full Screen, Brightness, etc."),
        ("Controls", "Input controls: Touch actions, gestures, lock mode, etc."),
        ("Navigation", "Navigation settings: seeking settings, forward/backward buttons, etc."),
    ]:
        matrix(s, "Player", f"{label} opens its tuner tab",
               f"Player page. Tap {label}.",
               f"Summary is {summary}. The tuner tab titled {label} opens.",
               "Must not open a different tab.",
               "P1")
    matrix(s, "Subtitle", "Text and Layout open the subtitle tuner tabs",
           "Subtitle page. Tap Text, back, tap Layout.",
           "Text summary mentions font, size, color, and border. Layout summary mentions alignment, padding, and background. Tabs are titled Text and Layout.",
           "Must not open the player.",
           "P1")
    sliders = [
        ("List", "Period tagged as \"NEW\"", "0 to 365", "7 days", "NEW tags use this many days. The slider row does not show the summary.", "P1"),
        ("Player", "Default playback speed", "25 to 400", "100%", "New playback starts at this percent.", "P1"),
    ]
    for page, title, rng, default, effect, pri in sliders:
        matrix(s, page, f"{title} slider commits on release",
               f"Settings → {page}. Drag {title} and lift. Then drag and leave without finishing.",
               f"Range {rng}. Default {default}. The value saves on release. {effect}",
               "A drag that never finishes must not save.",
               pri)
    texts = [
        ("List", "Folders", "Empty means the whole device. The field is multiline and is not a folder picker.", "P1"),
        ("List", "File extensions", "Comma-separated extra extensions. Empty shows the summary.", "P2"),
        ("Audio", "Preferred audio language", "ISO code such as hi or en. Empty shows the example summary.", "P2"),
        ("Subtitle", "Subtitle Folder", "Extra folder to search. Not a folder picker.", "P2"),
        ("Subtitle", "Preferred subtitle language", "ISO code. Empty shows the summary.", "P2"),
        ("General", "Http User-Agent", "Blank keeps the default agent.", "P2"),
    ]
    for page, title, effect, pri in texts:
        matrix(s, page, f"{title} text dialog",
               f"Open {title}. Change the text. Tap CANCEL. Open again, change it, tap OK. Relaunch.",
               f"CANCEL discards. OK saves the trimmed text. {effect}",
               "Must not save on CANCEL.",
               pri)
    matrix(s, "Audio", "Audio delay accepts only −10 to 10 seconds",
           "Open Audio delay. Enter 12, then −3.5. CANCEL a third edit.",
           "OK coerces into −10 to 10 and shows one decimal. CANCEL keeps the previous value.",
           "Must not store 12.",
           "P2")
    matrix(s, "Subtitle", "Default sync accepts −60 to 60 seconds",
           "Open Default sync. Enter 90 and OK. Enter 1.5 and OK.",
           "90 is coerced. 1.5 is saved. The row shows the value and the summary.",
           "CANCEL must not save.",
           "P2")
    matrix(s, "General", "Clear history confirms",
           "General → Clear history. Tap Cancel, then OK.",
           "Dialog: Clear all history? Cancel changes nothing. OK clears playback history, last played, per-folder last played, and search history. Toast is not required.",
           "Must not delete playlists, private files, or settings.",
           "P0")
    matrix(s, "General", "Clear thumbnail cache has no confirm",
           "Tap Clear thumbnail cache.",
           "Cache clears immediately and a toast says Thumbnail cache cleared. Lists regenerate thumbs.",
           "Must not clear history.",
           "P1")
    matrix(s, "General", "Reset settings confirms and restores defaults",
           "Change several settings. Tap Reset settings. Cancel, then OK.",
           "Dialog: Reset all settings? Cancel keeps edits. OK restores defaults and reapplies the engine.",
           "Must not clear history, playlists, or the private folder.",
           "P0")
    matrix(s, "General", "Export and import round-trip",
           "Android or desktop. Export, change a setting, Import the file.",
           "Suggested name maxplayerpro-settings.txt. Success toasts Settings exported and Settings imported. Cancelling the system picker does nothing and shows no toast. Unknown keys are ignored.",
           "iOS and Web save/open always return null, so they must not pretend a file was written. Failure toasts Export failed or Import failed.",
           "P1")
    matrix(s, "General", "Quit exits without a second confirm",
           "Turn Quit Button on. Me → Quit.",
           "Playback stops, the engine is released, and the queue clears. Android finishes the task. Desktop exits. iOS and Web release the engine but the platform quit is a no-op.",
           "Must not ask for a PIN. Must not appear while the setting is off.",
           "P0")


def tuner():
    s = "Display Tuner"
    matrix(s, "Style", "Preset, colours, and progress style",
           "Open Style from Player → Style or from the player Display Settings.",
           "Preset choices Default, Classic, Minimal. Colour rows: Frame, Controls, Controls (highlight), Progress bar. OK saves a colour. Cancel discards the swatch and opacity edits. Progress bar style: Material, Flat, Thin. Place progress bar below buttons. is a checkbox, including the period.",
               "Must not apply a colour on Cancel.",
               "P1")
    matrix(s, "Screen", "Orientation choices",
           "Open Orientation.",
           "Choices: Use video orientation, System default, Auto rotation, Landscape, Landscape (reverse), Portrait. Default is Use video orientation. Android applies it to the player activity.",
           "Must not rotate the library screens.",
           "P0", platform="Android")
    for title, options, default in [
        ("Full Screen", ["Auto Switch", "On", "Off"], "Auto Switch"),
        ("Soft buttons", ["Auto hide", "Show", "Hide"], "Auto hide"),
        ("Touch action", ["Show/hide interface", "Pause/resume", "Show interface + pause"], "Show/hide interface"),
        ("Lock mode", ["Lock", "Kids lock"], "Lock"),
    ]:
        matrix(s, "Screen" if title in ("Full Screen", "Soft buttons") else "Controls", title,
               f"Open {title}.",
               f"Choices: {', '.join(options)}. Default {default}. A pick applies immediately.",
               "Cancel must keep the previous value.",
               "P1")
    gestures = [
        "Seek position", "Zoom and Pan", "Video zoom", "Video pan", "Volume", "Brightness",
        "Play/Pause (Double tap)", "Video zoom (Double tap)", "FF/RW (Double tap)",
        "Speed FF (Long press)", "Playback Speed", "Subtitle up/down", "Subtitle zoom",
    ]
    for g in gestures:
        matrix(s, "Controls", f"Gesture flag {g}",
               f"Controls → Gestures. Toggle {g}. Return to a video and try that gesture.",
               "On, the gesture works, except Playback Speed, which has no gesture in this build. Off, that gesture must not run.",
               "Must not disable a different gesture. The Brightness checkbox here is not the Screen brightness checkbox.",
               "P1" if g != "Playback Speed" else "P2")
    matrix(s, "Controls", "Speed FF value and auto-hide delay",
           "Edit Speed FF value and Auto hide delay. Try values outside the range.",
           "Speed FF value default 2.0, range 1.25 to 4, unit x. Auto hide delay default 2.0, range 1 to 10 seconds. OK coerces. CANCEL discards.",
           "Must not store 10x.",
           "P1")
    matrix(s, "Controls", "Locked touch can reveal the lock icon only",
           "Turn on Show interface when locked screen is touched. Lock the player and tap the video.",
           "The small lock icon reappears. The transport bar stays hidden because controls are not composed while locked.",
           "Must not show Play/Pause while locked.",
           "P1")
    matrix(s, "Navigation", "Seek speed and move interval",
           "Set Seek Speed (sec/cm) to 30 and Move Interval to 25.",
           "A horizontal swipe uses 30 sec/cm. Double-tap and the optional move buttons use 25 seconds. Ranges are 1–60 and 1–120.",
           "Must not swap the two settings.",
           "P1")
    matrix(s, "Screen", "Brightness level and keep screen on",
           "Set Brightness off, then on, and change Brightness level. Toggle Keep Screen On while paused.",
           "Gesture and window brightness follow the checkbox and the 0–30 slider (default 24). Keep Screen On summary says the screen stays on even while paused.",
           "Pause playback if obstructed by another window, when on, pauses on Android onPause unless PiP or background play.",
           "P1", platform="Android")
    matrix(s, "Text", "Subtitle font, size, colour, and border",
           "Open the Text tuner. Change Font, Size, Scale, Color, Bold, Border, Border thickness, Shadow, and Fade out.",
           "Font choices Default, Sans serif, Serif, Monospace. Size 8–60 default 19. Scale 50–300 default 100%. Two rows are both titled Background Color: a checkbox and a swatch.",
           "Fade out is stored but the renderer does not fade. Do not fail the player for a missing fade. Cancel on a colour must not save.",
           "P1")
    matrix(s, "Layout", "Alignment, margin, and fit",
           "Open Layout. Set Alignment to Left, Right, then Center. Change Bottom margins. Toggle Fit subtitles into video size and Background.",
           "Subtitles follow alignment and the 0–50 margin (default 8). Background here is the full subtitle background, not the Text background colour.",
           "Must not move the video frame.",
           "P1")
    matrix(s, "OSD", "Battery, clock, and corner offset",
           "Screen tuner. Toggle Elapsed time, Battery/clock, Display battery/clock in title bar, Corner offset, and Place at the bottom.",
           "Each OSD part appears only when its flag is on and the controls are hidden. Corner offset size is 0–40.",
           "Must not cover the lock icon permanently.",
           "P2")


def me():
    s = "Me"
    matrix(s, "Open", "Me has no account header",
           "Phone Home → Account, or wide navigation to Me.",
           "Back content description Navigate up. No user name, photo, or login.",
           "Must not show an edit-profile button.",
           "P0")
    tiles = [
        ("File Transfer", "Explanation that MXShare is not implemented.", "P1"),
        ("Private Folder", "PIN gate.", "P0"),
        ("Video Playlists", "VIDEO PLAYLISTS.", "P0"),
        ("Network Stream", "Network Stream dialog, not a new screen.", "P1"),
        ("Local Network", "Networks empty state.", "P1"),
        ("Media Manager", "Media Manager.", "P0"),
        ("Cloud Drive", "Explanation that Drive, Dropbox, and OneDrive are not implemented.", "P1"),
    ]
    for label, expected, pri in tiles:
        matrix(s, "Tiles", label,
               f"Tap the {label} tile.",
               expected,
               "Must not open a different tile. Unavailable tiles must not pretend to transfer or sync.",
               pri)
    matrix(s, "Rows", "WhatsApp Status Saver is not implemented",
           "Tap WhatsApp Status Saver.",
           "Explanation about WhatsApp's private media folder and SAF. Back returns to Me.",
           "Must not read WhatsApp statuses.",
           "P1")
    for label, expected, pri in [
        ("App Theme", "App Theme screen.", "P0"),
        ("Settings", "Settings index.", "P0"),
        ("Legal", "A short independence notice, not a terms document.", "P2"),
        ("Help", "Help topics.", "P1"),
    ]:
        matrix(s, "Rows", label, f"Tap {label}.", expected, "Must not log the user out. There is no account.", pri)
    matrix(s, "Network", "Network stream from Me uses the same URL rule",
           "Me → Network Stream. OK with no scheme, then with http://.",
           "Same dialog as the player. OK without :// stays open. CANCEL closes.",
           "Must not navigate to Local Network.",
           "P1")
    matrix(s, "Quit", "Quit is hidden by default",
           "Fresh install. Open Me and scroll to the bottom.",
           "No Quit row.",
           "Must not exit when the row is absent.",
           "P1")


def theme():
    s = "Theme"
    for label in ("Adaptive", "Dark Theme", "Light Theme", "Amoled Black"):
        matrix(s, "Mode", label,
               f"Open App Theme. Tap {label}.",
               f"That card shows Using and a primary border. The app colours change immediately. Default is Dark Theme.",
               "Must not ask to confirm. Must not change the player frame colour.",
               "P0" if label in ("Dark Theme", "Light Theme") else "P1")
    matrix(s, "Accent", "Each of the 19 swatches selects",
           "Under Classic Themes, tap each swatch, including the first grey one.",
           "The tapped swatch gets a 2 dp border. Accent colour updates immediately. There are no names on the swatches. Default is the first, 0xFFE0E0E0.",
           "Must not require a Done button. Must not change the theme mode.",
           "P1")
    matrix(s, "Persist", "Theme survives relaunch",
           "Choose Light Theme and a blue swatch. Force-stop and relaunch.",
           "Light Theme and that swatch are still selected.",
           "Reset settings must return Dark Theme and the default accent. It must not be required for a normal relaunch.",
           "P0")


def help_page():
    s = "Help"
    topics = [
        ("Gestures", "swipe seek, left brightness, right volume, pinch, pan, double-tap, long-press"),
        ("Lock", "lock button blocks touches; the small top-left lock unlocks"),
        ("Background play", "headphones shortcut or Settings → Player → Background/PiP mode"),
        ("Subtitles", "same-name subtitle loads; Open and Synchronization"),
        ("Private Folder", "⋮ → Lock in Private Folder and a PIN"),
    ]
    matrix(s, "Open", "Help lists all five topics at once",
           "Open Help.",
           "Title Help. All five questions and answers are visible. There is no search and no expand control.",
           "Must not hide an answer behind a tap.",
           "P1")
    for q, hint in topics:
        matrix(s, "Topic", q,
               f"Read the {q} answer.",
               f"The answer mentions {hint}.",
               "Must not be a link that leaves the app. Kids-lock long-press is not in the Lock answer; the code still requires a long-press.",
               "P2")


def playlists():
    s = "Playlists"
    matrix(s, "List", "Empty copy and create",
           "Open Video Playlists with none saved. Tap Create New Playlist. Cancel, then create Holiday.",
           "Empty text is No Playlists found. Dialog title Create New Playlist, label Playlist name, OK and CANCEL. Blank or whitespace does not create. Holiday appears with 0 videos.",
           "Must not create a music playlist. Close returns to the previous screen.",
           "P0")
    matrix(s, "Detail", "Play all, row play, and empty play",
           "Add two videos from a file ⋮ menu. Open the playlist. Tap the second row, back, tap Play all. Create an empty playlist and tap Play all.",
           "Row play starts at that index. Play all starts at 0. Empty play does nothing and shows no toast.",
           "Must not play videos that are not in the playlist.",
           "P0", platform="Android")
    matrix(s, "Detail", "Rename allows a blank name",
           "Tap Rename. CANCEL, then OK with a new name, then OK with an empty field.",
           "Dialog title Rename playlist. CANCEL keeps the name. OK renames, including to a blank trimmed name.",
           "Must not delete the playlist.",
           "P1")
    matrix(s, "Detail", "Delete playlist has no confirm",
           "Tap Delete playlist.",
           "The playlist is removed immediately and the screen goes back.",
           "Must not delete the video files. There is no undo.",
           "P0")
    matrix(s, "Detail", "Remove takes the video out of the list only",
           "Tap Remove on one row.",
           "That URI leaves the playlist immediately. The file remains in the library.",
           "Must not ask, and must not delete the file.",
           "P0")
    matrix(s, "Detail", "Empty detail tells the user how to add",
           "Open a playlist with no videos.",
           "Add videos from a file's ⋮ menu → Add To Playlist.",
           "Must not show a file picker.",
           "P2")
    matrix(s, "List", "Music playlists are not listed here",
           "Create a playlist from Music. Open Video Playlists.",
           "The music playlist is absent.",
           "Must not mix the two lists.",
           "P1")


def private():
    s = "Private Folder"
    matrix(s, "PIN", "First visit creates a 4-digit PIN",
           "Open Private Folder before any PIN. Enter 12, OK. Enter 1234, OK. Enter 0000, OK. Enter 1234, OK.",
           "Title Private Folder. Prompts: Create a 4-digit PIN, then Confirm PIN. Short PIN errors PIN must be 4 digits. Mismatch errors PINs don't match and returns to create. A match unlocks.",
           "Letters are rejected. The field is masked. Cancel leaves the screen and must not save a partial PIN.",
           "P0")
    matrix(s, "PIN", "Wrong PIN stays locked",
           "Reopen Private Folder. Enter a wrong PIN.",
           "Error Wrong PIN. The field clears. The list stays hidden. There is no lockout counter.",
           "Must not show file names. Cancel leaves without unlocking.",
           "P0")
    matrix(s, "PIN", "Correct PIN shows the list",
           "Enter the saved PIN.",
           "The list or the empty state appears.",
           "Must not ask to create a new PIN.",
           "P0")
    matrix(s, "Empty", "Empty state points at the file menu",
           "Unlock with no private files.",
           "You haven't added any files yet. Use ⋮ → Lock in Private Folder on any video.",
           "Must not show library videos.",
           "P1")
    matrix(s, "Files", "Tap plays the private file",
           "Lock a video, unlock the folder, tap the row.",
           "That private file plays. The row shows a lock icon, the name, and the size.",
           "Must not play a different file.",
           "P0", platform="Android")
    matrix(s, "Files", "Move out, rename, and delete",
           "Open Options. Cancel. Then Move out of Private Folder. Lock another file, Rename it, and Delete a third.",
           "Cancel does nothing. Move out copies to Movies/MaxPlayerPro and toasts that path even if the copy fails. Rename uses the Rename dialog. Delete removes the private file immediately with no second confirm.",
           "Must not delete the original if the copy failed. The Add files FAB does not pick files; it goes back.",
           "P0")
    matrix(s, "Platform", "iOS private folder is memory-only",
           "iOS. Lock a file, then relaunch.",
           "Lock can fail with Could not move file. A memory folder does not survive a relaunch.",
           "Must not claim the file was written to Movies.",
           "P1", platform="iOS")


def media():
    s = "Media Manager"
    matrix(s, "Open", "Storage summary and kind tiles",
           "Open Media Manager on a device that reports storage.",
           "Title Media Manager. Device storage, Used N / total, and Video, Music, Image sizes. Tiles Recently Played, Large Files, Recently Added. Row Haven't Played.",
           "The size cards are not buttons. If storage is null, that block is hidden rather than showing zeros as fact.",
           "P1")
    matrix(s, "Lists", "Recently played is history newest first",
           "Play two videos. Open Recently Played.",
           "Both appear, newest first. Empty list says Nothing here.",
           "Must not include never-played files.",
           "P0")
    matrix(s, "Lists", "Large files are over 100 MB",
           "Open Large Files.",
           "Only files larger than 100 MB, largest first.",
           "Must not include a 10 MB clip.",
           "P1")
    matrix(s, "Lists", "Recently added is the newest 50",
           "Open Recently Added.",
           "Newest modified files, at most 50.",
           "Must not sort by title.",
           "P2")
    matrix(s, "Lists", "Haven't played excludes history",
           "Open Haven't Played. Also tap one of the up-to-four preview thumbs.",
           "The list is files with no history. A preview thumb plays that single file, not the whole list.",
           "A played file must disappear from this list after history is written.",
           "P1")
    matrix(s, "Lists", "List layout ignores the home grid setting",
           "Set Home to Grid. Open a media list.",
           "The list is forced to list layout with thumbnail, length, and size.",
           "Must not open in a grid just because Home is a grid.",
           "P2")


def network():
    s = "Local Network"
    matrix(s, "Open", "Empty state and the plus button",
           "Open Local Network from Me or the sidebar.",
           "Title Networks. Copy: Add a server by tapping the PLUS BUTTON. The decorative plus in the empty state is not a button. The FAB plus toasts SMB/LAN servers are not supported yet.",
           "Must not open a server form, scan the LAN, or save a server.",
           "P1")
    matrix(s, "Sidebar", "Sidebar label is Network stream but the screen is Networks",
           "Wide window. Tap Network stream.",
           "The Networks screen opens, not the URL dialog.",
           "Must not be confused with Me → Network Stream, which is the URL dialog.",
           "P1", platform="Desktop and wide windows")


def not_built():
    s = "Not Built"
    items = [
        ("File Transfer", "Me tile and the file sheet", "Explanation page from Me. Sheet toasts that it is not available."),
        ("Cloud Drive", "Me tile", "Explanation about OAuth. No account picker."),
        ("WhatsApp Status Saver", "Me row", "Explanation about SAF. No status list."),
        ("Convert to MP3", "File sheet", "Toast only."),
        ("Cut", "File sheet", "Toast only. The player does not have a cut tool either."),
        ("Online subtitle search", "File sheet Search Subtitle", "Toast only. Local Open subtitle still works in the player."),
        ("SMB / LAN", "Local Network plus", "Toast SMB/LAN servers are not supported yet."),
        ("Development diagnostics", "Settings → Development", "Explanation only."),
        ("Legal documents", "Me → Legal", "A short notice, not terms or a privacy policy."),
        ("Lyrics", "Now Playing lyrics", "Toast No lyrics found."),
        ("iOS share", "Share on iOS", "Silent no-op."),
        ("iOS and Web delete", "Delete confirm", "File remains."),
        ("Desktop, iOS, and Web engines", "Play a video", "UI can open. Playback engines are planned, so a missing picture is not an Android failure."),
    ]
    for title, where, expected in items:
        matrix(s, "Gap", title,
               f"Use {where}.",
               expected,
               "Must not crash, and must not report success for an action the code does not perform.",
               "P1")


def expand():
    """Split grouped controls into one executable case each."""
    s = "Video Player"
    for label, steps, expected, must, pri in [
        ("Shuffle next does not reorder the queue", "Turn Shuffle on. Note the playlist order. Tap Next twice.",
         "Next jumps to a different item. The playlist order is unchanged. Toast said Shuffle on.",
         "Must not reshuffle the whole queue on every next.", "P1"),
        ("Repeat one restarts the same file", "Set Loop to Repeat one. Seek near the end and let it finish.",
         "The same file seeks to 0 and plays again. Toast said Repeat one.",
         "Must not close the player and must not advance.", "P1"),
        ("Repeat all restarts the queue", "Set Loop to Repeat all. Play the last file to the end.",
         "Playback continues at the first item.",
         "Must not stop on the last frame.", "P1"),
        ("Loop off stays on the ended frame", "Loop off and Back to list off. Finish the last file.",
         "Playback ends and the player stays open.",
         "Must not jump to another file.", "P1"),
        ("Smart previous under 3 seconds opens the previous file", "Smart Previous Button on. Seek to 1 second. Tap Previous.",
         "The previous file opens.",
         "Must not merely restart if a previous file exists.", "P0"),
        ("Smart previous after 3 seconds restarts", "Seek past 4 seconds. Tap Previous.",
         "Position returns to 0. The file does not change.",
         "Must not open the previous file.", "P0"),
        ("No previous file restarts even at the start", "First item, position under 3 seconds. Tap Previous.",
         "Position stays or returns to 0. Toast is not Last item.",
         "Must not crash.", "P1"),
        ("Next on the last item toasts Last item", "Last file in the queue. Tap Next.",
         "Toast Last item. The file does not change.",
         "Must not wrap unless Repeat all is on.", "P0"),
        ("PiP hides every control", "Enter picture-in-picture.",
         "Only the surface, scaled subtitles, and the buffering spinner can show.",
         "Must not show play, seek, panels, or dialogs inside the PiP window.", "P0"),
        ("Screenshot saves under Pictures", "Android. Tap Screenshot while a frame is visible, then before the first frame.",
         "A PNG is saved under Pictures/MaxPlayerPro and a snack names the folder. No frame toasts Screenshot unavailable or Screenshot failed.",
         "Must not save a black image and call it success without a frame.", "P1"),
        ("Decoder change keeps the position", "Note the time. Switch HW to SW.",
         "Playback restarts at the same item and position. Toast names the decoder.",
         "Must not restart at 0 unless the engine cannot seek.", "P0"),
        ("TRY SW is hidden for IO errors", "Cause an IO error, or use a missing file.",
         "The error dialog has OK only.",
         "Must not offer TRY SW DECODER when the code name contains _IO_.", "P1"),
        ("Suppress error still shows a dialog on the last file", "Suppress error message on. Fail the last file.",
         "The error dialog still appears because there is no next item.",
         "Must not close silently with no explanation.", "P1"),
        ("Resume is not asked inside 5 seconds of either end", "Save a position at 2 seconds, reopen. Repeat at duration minus 2 seconds.",
         "No resume dialog and no continue notice.",
         "Must not offer RESUME for a point the policy ignores.", "P1"),
        ("Resume only the first file skips later asks", "Resume only the first file on. Ask at startup. Play file one, then next.",
         "File one may ask. The next file starts at the beginning.",
         "Must not ask again for file two in the same session.", "P1"),
        ("HTTP playback is not resumed", "Play an http link, seek, close, reopen the same link.",
         "It starts at 0. No resume dialog.",
         "Must not restore an unsaved network position.", "P1"),
        ("Headset unplug pauses", "Pause on headset disconnected on. Play with a wired or Bluetooth headset. Disconnect it.",
         "Playback pauses.",
         "With the setting off, disconnect must not force a pause.", "P0"),
        ("Media buttons are ignored when off", "Media buttons off. Press a headset play button.",
         "The player does not respond.",
         "Must not steal the button while the setting is off.", "P1"),
        ("Play alone stops other audio", "Play alone on. Start another music app, then play a video.",
         "The other player stops or ducks.",
         "With the setting off, both may play.", "P1"),
        ("Fast seek lands on a keyframe", "Fast seeking on. Seek to a non-keyframe time. Turn it off and repeat.",
         "On, the landing time may be the nearest keyframe. Off, it aims at the exact time.",
         "Must not jump to 0.", "P2"),
        ("Seek preview only when enabled", "Preview while seeking on, then off. Drag the bar.",
         "A preview image shows only while the setting is on and the platform can draw one.",
         "Must not freeze the video if preview is unavailable.", "P2"),
        ("Keep screen on while paused", "Keep Screen On on. Pause for 2 minutes.",
         "The screen stays on.",
         "With the setting off, a paused player may sleep. Playing still keeps the screen on.", "P2"),
        ("Pause if obscured", "Pause playback if obstructed by another window on. Open a dialog from another app over the player, not PiP.",
         "Playback pauses.",
         "Must not pause when entering PiP or background play.", "P1"),
        ("Show interface at startup", "Show interface at the startup on. Open a video. Repeat with it off.",
         "On, controls are visible immediately. Off, they start hidden.",
         "Must not pause just to show controls.", "P2"),
        ("Battery and clock in the title", "Display battery/clock in title bar on. Show controls.",
         "The title row shows the clock and battery percent.",
         "Must not replace the file title.", "P2"),
        ("Remaining time toggle", "Show Remaining Time off. Look at the right time. Turn it on.",
         "Off shows total duration. On shows a leading minus and the remaining time.",
         "Must not show both at once.", "P1"),
        ("Progress bar can sit below the buttons", "Place progress bar below buttons on.",
         "The bar renders under the transport buttons.",
         "Must not hide the bar.", "P2"),
        ("Thin progress style", "Set Progress bar style to Thin, then Flat, then Material.",
         "The bar thickness and shape follow the choice.",
         "Must not change the seek behaviour.", "P2"),
        ("Rotation uses the video orientation by default", "Orientation is Use video orientation. Open a portrait video and a landscape video.",
         "The activity rotates to match the video.",
         "Must not rotate Home.", "P1"),
        ("Each orientation mode", "Set System default, Auto rotation, Landscape, Landscape (reverse), and Portrait, one at a time.",
         "The player activity follows that mode.",
         "Must not stick on the previous mode after the radio is picked.", "P1"),
        ("Soft buttons follow the picker", "Set Soft buttons to Show, Hide, then Auto hide.",
         "System bars follow the choice while controls are visible. Locked playback hides them.",
         "Must not hide the video when bars are shown.", "P2"),
        ("Full screen Auto, On, and Off", "Set Full Screen to each choice and open a video.",
         "On forces fullscreen. Off does not. Auto switches with the layout.",
         "Must not trap the user with no way to see system navigation when Off.", "P1"),
        ("Double-tap back is a single press when off", "Double-tap the back button off. Press system back once.",
         "The player closes immediately, unless a panel is open or the player is locked.",
         "Must not toast Press back again to exit.", "P0"),
        ("Locked back does not exit", "Lock the player. Press system back.",
         "The lock icon is shown. The player stays locked and open.",
         "Must not unlock or finish the activity.", "P0"),
        ("Open panel consumes back", "Open Audio Track. Press back.",
         "The panel closes. Playback continues.",
         "Must not exit.", "P0"),
        ("Ended file play starts at zero", "Let a file end. Tap Play.",
         "It seeks to 0 and plays.",
         "Must not stay stuck on the last frame.", "P1"),
        ("Mute does not change the system slider", "Note the system volume. Tap Mute, then Unmute.",
         "Toasts Mute and Unmute. Engine volume is 0 then 1. The system slider is unchanged.",
         "A volume-up gesture while muted unmutes.", "P1"),
        ("Night mode is an overlay only", "Tap Night Mode on and off.",
         "A dark overlay covers the frame. Toasts Night mode on and Night mode off. Web also dims the HTML video.",
         "Must not change the phone's system night mode or the app theme.", "P1"),
        ("Background play flag is not the settings picker", "Shortcut Background Play on. Leave the app. Then set Settings Background/PiP mode to Stop and leave again.",
         "The shortcut toggles the session flag and toasts on or off. The settings picker still decides Stop versus PiP versus background when leaving the app.",
         "Must not enter PiP only because the shortcut was tapped.", "P1"),
        ("Popup is absent without PiP", "iOS, desktop, and an Android build without the PiP feature.",
         "No Popup button, or a tap toasts Picture-in-picture not supported.",
         "Must not show a dead icon on iOS.", "P1"),
        ("Fullscreen button is absent on Android", "Android player chrome.",
         "No Full screen button. Desktop and Web have Full screen and Exit full screen.",
         "Must not hide Android's own system fullscreen.", "P2"),
    ]:
        matrix(s, "Playback", label, steps, expected, must, pri, platform="Android")

    g = "Gestures"
    for title, steps, expected, must in [
        ("Double-tap badge accumulates in one direction",
         "Double-tap the right third three times within the badge window.",
         "The badge seconds add up. A left double-tap starts a rewind count.",
         "Must not apply three seeks and show only 10s."),
        ("Double-tap does nothing if every double-tap flag is off",
         "Turn off Play/Pause, Video zoom, and FF/RW double-tap. Double-tap.",
         "The tap is treated as a single tap after the window, or does not seek.",
         "Must not seek or pause from a double-tap."),
        ("Center double-tap does not seek",
         "FF/RW on. Double-tap the center third.",
         "Playback toggles and a Playing or Paused pill shows. Position does not jump.",
         "Must not rewind."),
        ("Side double-tap does not pause while FF/RW is on",
         "Double-tap the left third.",
         "It rewinds by the move interval.",
         "Must not also pause."),
        ("Brightness does not run on the right half",
         "Swipe vertically on the right half.",
         "Volume changes. Brightness does not.",
         "Must not move both."),
        ("Volume does not run on the left half",
         "Swipe vertically on the left half.",
         "Brightness changes. Volume does not.",
         "Must not move both."),
        ("Volume boost off stops at 100 percent",
         "Volume boost off. Swipe volume to the top.",
         "The pill stops at the system maximum.",
         "Must not turn orange or exceed 100%."),
        ("Seek flag off ignores a horizontal swipe",
         "Seek position off. Swipe left and right.",
         "Position stays.",
         "Must not show the seek OSD."),
        ("Brightness flag off ignores a left swipe",
         "Brightness gesture off. Swipe the left half.",
         "Brightness stays. The Screen brightness checkbox is a different setting.",
         "Must not dim the video."),
        ("Long-press is ignored while paused",
         "Pause. Long-press the video without moving.",
         "Speed stays. No NX Speed Playing pill.",
         "Must not start playback at 2x."),
        ("Long-press restores speed on release",
         "While playing at 1x, long-press, then lift.",
         "Speed is 2x only while the finger is down, then returns to 1x.",
         "Must not stay at 2x."),
        ("Locked gestures do nothing",
         "Lock. Swipe, pinch, double-tap, and long-press.",
         "Position, volume, brightness, zoom, and speed stay.",
         "Must not show the transport."),
        ("Kids lock tap does not unlock",
         "Lock mode Kids lock. Lock. Tap the lock icon.",
         "Toast Long press to unlock. Still locked.",
         "A long-press unlocks and shows controls."),
        ("Normal lock tap unlocks",
         "Lock mode Lock. Lock. Tap the video so the icon appears. Tap the icon.",
         "Controls return and gestures work.",
         "Must not require a long-press in normal lock."),
        ("Subtitle drag needs a visible line",
         "Subtitles off. Drag the bottom of the video.",
         "The drag does not move a subtitle. It may seek or change volume instead.",
         "Must not crash."),
        ("Playback Speed checkbox is not a gesture",
         "Playback Speed gesture flag on. Swipe in every direction.",
         "Playback speed does not change.",
         "Must not document this checkbox as a working swipe."),
        ("Wheel zoom is desktop and web",
         "Desktop or Web. Scroll the wheel over the video.",
         "Zoom changes by a small step, unless Video zoom is off or the player is locked.",
         "Must not run on Android from a mouse that the activity does not handle."),
    ]:
        matrix(g, "Gesture", title, steps, expected, must, "P1", platform="Android")

    k = "Gestures"
    for title, steps, expected in [
        ("Ctrl or Cmd N goes to the next item", "Press Ctrl+N or Cmd+N, and Shift+N.", "Same as the Next button, including Last item."),
        ("Ctrl or Cmd P goes to the previous item", "Press Ctrl+P or Cmd+P, and Shift+P.", "Same as Previous, including smart previous."),
        ("Shift plus arrows seeks one minute", "Press Shift+Right and Shift+Left.", "Seeks 60 seconds, not the move interval."),
        ("J and L ignore the move interval", "Set move interval to 30. Press J and L.", "Seeks 10 seconds."),
        ("Digits seek to a percent", "Press 0, 5, and 9.", "Seeks to 0%, 50%, and 90%. No seconds badge."),
        ("Esc closes a panel before fullscreen", "Open a panel in fullscreen. Press Esc twice.", "First Esc closes the panel. Second leaves fullscreen."),
        ("Locked keyboard is ignored", "Lock the player. Press Space, arrows, and M.", "Nothing changes. The key is not consumed as a player shortcut."),
        ("S with Ctrl is not a screenshot", "Press Ctrl+S or Cmd+S.", "The player does not take a screenshot."),
        ("C toggles subtitles without unloading a file", "Load an external subtitle. Press C twice.", "Toasts Subtitles off and Subtitles on. The file stays loaded."),
    ]:
        matrix(k, "Keyboard", title, steps, expected, "Must not run on Android or iOS, or while locked.", "P1", platform="Desktop, Web")

    p = "Player Panels"
    for ratio in ("Default", "1:1", "4:3", "16:9", "Custom"):
        matrix(p, "Aspect", f"Aspect {ratio}",
               f"More → Aspect Ratio → {ratio}.",
               "The frame uses that ratio. Custom shows a 0.5 to 3.0 slider labeled Custom N.NN:1.",
               "Must not change the zoom cycle. Apply to all videos must not store Custom as the default.",
               "P1")
    for name in ("None", "Small room", "Medium room", "Large room", "Medium hall", "Large hall", "Plate"):
        matrix(p, "Equalizer", f"Reverb {name}",
               f"Equalizer on, on a device with reverb. Choose {name}.",
               "That reverb is applied and saved.",
               "If the output has no reverb control, the dropdown must not appear.",
               "P2", platform="Android")
    for chip in ("0.25x", "1x", "2x", "3x", "4x"):
        matrix(p, "Speed", f"Preset {chip}",
               f"Open Speed and tap {chip}.",
               f"Speed becomes {chip}. The shortcut label updates when it is not 1x.",
               "Must not close the player.",
               "P1", platform="Android")
    matrix(p, "Speed", "Custom speed outside 0.25 to 4 is clamped",
           "Tap the center speed. Enter 0, then 9, then 1.25. CANCEL a fourth edit.",
           "OK clamps to 0.25 or 4. 1.25 is kept. CANCEL restores the previous speed.",
           "Must not store 0.",
           "P1", platform="Android")
    matrix(p, "Sleep", "Each keypad digit shifts like a microwave",
           "Enter 1, then 5, then 9. Tap Clear.",
           "The display builds hours and minutes. Clear resets the input. START stays disabled at 0.",
           "Must not start the timer while typing.",
           "P2")
    matrix(p, "Sleep", "Finish last waits for the end of the item",
           "Set 1 minute with Play last media to the end. Wait for the timer.",
           "Playback continues until the item ends, then pauses.",
           "Without that checkbox, the timer pauses immediately when it fires.",
           "P1")
    matrix(p, "Bookmark", "Import accepts h:mm:ss and m:ss",
           "Import a text file containing 1:02:03 and 0:10, plus a time past the duration.",
           "In-range times are added. The toast counts them. The late time is dropped.",
           "An empty file toasts No bookmarks found in file.",
           "P2")
    matrix(p, "Bookmark", "Delete removes one bookmark only",
           "Add two bookmarks. Delete the first.",
           "Only that row disappears. The other seek still works.",
           "Must not clear history.",
           "P1")
    matrix(p, "Audio", "Empty audio list",
           "Open Audio Track on a file with no selectable tracks.",
           "No audio tracks found, disabled.",
           "Must not crash or show a blank panel with a working Disable that errors.",
           "P1")
    matrix(p, "Audio", "Web help instead of switching",
           "Web, browser without native audio tracks. Tap a non-selected track.",
           "Switch audio in {browser} explains the one-time browser setting. Open settings may toast Link copied. Paste it in a new tab.",
           "Must not claim the track changed.",
           "P1", platform="Web")
    matrix(p, "Subtitles", "Same-name subtitle loads by itself",
           "Put video.srt next to video.mkv. Open the video.",
           "The subtitle loads without using Open.",
           "A .txt with the same name must not auto-load. Open of .txt toasts Unsupported subtitle format.",
           "P0", platform="Android")
    matrix(p, "Subtitles", "Sync stepper disabled for embedded tracks",
           "Select an embedded subtitle. Try Synchronization and Speed.",
           "Both steppers are disabled.",
           "Must not shift an embedded track from those steppers.",
           "P1")
    matrix(p, "Info", "Information uses this file only",
           "Open Information. Compare File, Location, Size, Date, Length, and Decoder with the file you opened.",
           "They match. Subtitle is the external name, Embedded subtitle, or None. Missing video or audio sections are omitted.",
           "Must not show the previous file after Next.",
           "P1")
    matrix(p, "Playlist", "Highlighted row is the current item",
           "Play the second item. Open the player Playlists panel.",
           "Title includes the queue count. The second title is highlighted. Tapping another title jumps and closes the panel.",
           "Must not edit the Me playlist.",
           "P1")
    matrix(p, "Tools", "Rename keeps the extension and can fail",
           "More → Rename. CANCEL. Then rename to a name that already exists.",
           "CANCEL keeps the name. A collision toasts Rename failed. Success toasts Renamed and the title updates.",
           "The row is hidden for http URIs and on Web and iOS.",
           "P0", platform="Android, Desktop")
    matrix(p, "Tools", "Delete cancel keeps the file",
           "More → Delete → CANCEL, then confirm on a copy.",
           "CANCEL does not delete. DELETE removes it and plays the next item, or closes if there is no next.",
           "Must not wait on screen for the delete result before moving on.",
           "P0", platform="Android, Desktop")
    matrix(p, "Network", "CANCEL does not play",
           "Open Network Stream, type a valid URL, tap CANCEL.",
           "The dialog closes. The current file keeps playing.",
           "Must not call playUrl.",
           "P1")
    matrix(p, "Network", "OK without a scheme stays open",
           "Type example.com/video.mkv and tap OK. Then type http://example.com/a.mkv and tap OK.",
           "The first OK does nothing. The second starts that URL and closes.",
           "Must not toast a vague error and close on the first OK.",
           "P1")
    matrix(p, "Customise", "Unchecked shortcut disappears",
           "Customise Items. Uncheck Mute. Collapse and expand the row.",
           "Mute is gone. Customise itself remains when expanded.",
           "Must not hide Play, Pause, or the seek bar.",
           "P2")
    matrix(p, "Display", "In-player tuner matches Settings",
           "Change Orientation in the player Display panel. Open Settings → Player → Screen.",
           "The same value is shown. applySettings runs.",
           "Must not keep two copies of the setting.",
           "P1")

    fa = "File Actions"
    for label in ("File Transfer", "Convert to MP3", "Cut", "Search Subtitle"):
        matrix(fa, "Sheet", f"{label} toasts and stays on the list",
               f"Open ⋮ and tap {label}.",
               f"Toast says {label} is not available in Max Player Pro, except Search Subtitle which says Online subtitle search is not available in Max Player Pro.",
               "Must not navigate. On Desktop, iOS, and Web the toast may only be visible if a player is collecting notices.",
               "P1")
    matrix(fa, "Share", "Android share sheet",
           "Android. Share one video, then two, then an audio file.",
           "Chooser title Share. Mixed or video uses video/*. All audio uses audio/*. An http URI is shared as text.",
           "Must not upload the file.",
           "P1", platform="Android")
    matrix(fa, "Share", "Desktop reveals the file",
           "Desktop. Share a local file, then a missing path.",
           "The file manager reveals the first existing file. Missing toasts Only local files can be shown in the file manager. Failure toasts Couldn't open the file manager.",
           "Must not share a public link.",
           "P1", platform="Desktop")
    matrix(fa, "Share", "Web refuses local files",
           "Web. Share a picked local file, then an http URL.",
           "Local: Files opened from this computer can't be shared from the browser. http: Sharing… or Link copied.",
           "Must not download the file to share it.",
           "P1", platform="Web")
    matrix(fa, "Share", "iOS share does nothing",
           "iOS. Tap Share.",
           "No share sheet. The function is empty.",
           "Must not crash.",
           "P1", platform="iOS")
    matrix(fa, "Delete", "Sibling subtitles follow the setting",
           "Delete subtitle files together on. Delete a video that has a sibling .srt. Repeat with the setting off.",
           "On, the subtitle file is deleted too. Off, it remains.",
           "Must not delete subtitles in other folders.",
           "P1")
    matrix(fa, "Delete", "Android deny consent leaves the file",
           "Android 11+. Confirm DELETE and deny the system dialog.",
           "The file remains. There may be no app toast, and the selection may already be cleared.",
           "Must not show a success state.",
           "P1", platform="Android")
    matrix(fa, "Lock", "No PIN does not move the file",
           "Never create a PIN. ⋮ → Lock in Private Folder.",
           "Toast Set up the Private Folder first (Me → Private Folder). The file stays.",
           "Must not delete it.",
           "P0")
    matrix(fa, "Lock", "Failed copy does not delete the original",
           "iOS, or a full private store. Lock a file.",
           "Toast Could not move file or only the copied count. The original remains if the copy failed.",
           "Must not report N file(s) locked if nothing was copied.",
           "P0")
    matrix(fa, "Playlist", "Blank new playlist does not create",
           "Add To Playlist → + Create New Playlist → OK with an empty name.",
           "No playlist is created. Both dialogs dismiss.",
           "Must not create a playlist named Playlist.",
           "P1")
    matrix(fa, "Playlist", "Music playlists are excluded",
           "Create a playlist in Music. Open Add To Playlist on a video.",
           "The music playlist is not listed.",
           "Must not add the video to a music playlist from this dialog.",
           "P1")
    matrix(fa, "Select", "Wide grid cannot long-press into selection",
           "Wide grid. Long-press a card. Use the More button instead.",
           "Long-press does nothing. More opens the sheet. Selection can continue only after it was started elsewhere.",
           "Do not file a bug that wide-grid long-press fails to select; it is not wired.",
           "P1", platform="Desktop and wide windows")
    matrix(fa, "Select", "Back clears selection and stays",
           "Select two files. Press system back.",
           "Selection clears. The screen stays.",
           "Must not pop Home or Folder.",
           "P0")
    matrix(fa, "Select", "More is hidden while selecting",
           "Long-press a row.",
           "The row More icon is hidden. The selection bar is the way out.",
           "Must not open the sheet and the selection bar together from that icon.",
           "P2")
    matrix(fa, "Properties", "Multi-select properties sum size and length",
           "Select three files. Tap Info.",
           "Title Properties. Body says 3 files, total size, and total length.",
           "Must not show one file's resolution as if it covered all three.",
           "P2")
    matrix(fa, "Row", "Length over thumbnail moves the duration",
           "Display length over thumbnail on, then off.",
           "On, duration sits on the thumbnail and is omitted from the subtitle. Off, it is in the subtitle.",
           "Must not show it in both places.",
           "P2")
    matrix(fa, "Row", "Hidden files and nomedia",
           "A dot-folder and a folder with .nomedia. Toggle the two advanced options.",
           "Defaults hide both. Show hidden files reveals dot items. Recognize .nomedia off includes those trees.",
           "Must not play a file the filter still excludes.",
           "P1")

    h = "Home"
    matrix(h, "Web", "Needs permission banner",
           "Web, saved folder, permission dropped.",
           "Welcome back. Allow access again… and Restore access.",
           "Must not wipe history.",
           "P1", platform="Web")
    matrix(h, "Web", "Needs re-pick banner",
           "Web browser that cannot keep folder access.",
           "This browser can't keep folder access between visits… and Pick folder. Progress is kept.",
           "Must not say the videos were deleted.",
           "P1", platform="Web")
    matrix(h, "Web", "Missing folder banner",
           "A saved folder was renamed or unplugged.",
           "Quoted name was moved, renamed or its drive is disconnected. Remove drops that source only.",
           "Must not remove every source.",
           "P1", platform="Web")
    matrix(h, "Web", "Scanning chip",
           "Add a large folder.",
           "Chip shows name · scanning… then name · count.",
           "Must not show 0 as a final count while scanning.",
           "P2", platform="Web")
    matrix(h, "Wide", "In-page search is hidden when the title bar has search",
           "Wide Home with a title bar.",
           "No second search field in the page. The title-bar placeholder is Search videos.",
           "Must not have two live queries.",
           "P1", platform="Desktop and wide windows")
    matrix(h, "Wide", "Wide refresh hides while the title bar has Refresh",
           "Wide Home with a title bar, during a scan.",
           "The title-bar Refresh is the control. The page does not also show a refresh icon.",
           "Must not start two scans from two buttons.",
           "P2", platform="Desktop and wide windows")
    matrix(h, "FAB", "Play last is hidden when the setting is off",
           "Floating button off. A last-played file exists.",
           "No Play last button and no Continue watching card.",
           "Must not play on launch.",
           "P0")
    matrix(h, "FAB", "Play last is hidden when the file is gone",
           "Delete the last-played file from outside the app. Refresh.",
           "The FAB disappears.",
           "Must not offer a play button that no-ops.",
           "P1")
    matrix(h, "List", "All folders shows the path",
           "View mode All folders, list layout.",
           "Each row can show its path. Folders mode does not, unless the Path field is on.",
           "Must not flatten subfolders into the file list.",
           "P1")
    matrix(h, "Empty", "Files mode empty copy is No videos found",
           "View mode Files, nothing to show, including while scanning.",
           "Empty title is No videos found.",
           "Must not use the folder scanning sentence in this mode.",
           "P1")

    mu = "Music"
    for tab in ("Tracks", "Playlists", "Albums", "Artists", "Folders"):
        matrix(mu, "Tabs", f"{tab} tab",
               f"Open Music and tap {tab}.",
               "That tab's content shows. Tracks can play. Groups open a drill-in with Play and Shuffle.",
               "Must not show videos. Must not reset the search query unless you clear it.",
               "P1")
    matrix(mu, "Labels", "Unknown artist, album, and folder",
           "Play a file with blank metadata.",
           "The UI says Unknown artist, Unknown album, or Unknown folder. Duration 0 is a dash on wide and hidden on the phone.",
           "Must not show a blank subtitle that looks like a loading error.",
           "P2")
    matrix(mu, "Mini", "Narrow close stops music",
           "Phone mini player. Tap Close.",
           "Playback stops and the queue clears. The bar hides.",
           "Must not only pause.",
           "P0", platform="Phone")
    matrix(mu, "Mini", "Narrow next does not seek",
           "Phone mini player. Tap Next.",
           "The next song starts at 0.",
           "There is no Previous on the narrow bar.",
           "P0", platform="Phone")
    matrix(mu, "Mini", "Wide previous is not smart previous",
           "Wide mini player. Seek past 10 seconds. Tap Previous.",
           "The engine previous runs. It does not use the video 3-second rule.",
           "Must not rewind 10 seconds unless the engine previous does that.",
           "P1", platform="Desktop and wide windows")
    matrix(mu, "Mini", "Wide repeat cycles Off, All, One",
           "Tap Repeat three times.",
           "Off, then All, then One, then Off. Accent when not Off.",
           "Must not open the video loop toasts.",
           "P1", platform="Desktop and wide windows")
    matrix(mu, "Mini", "Wide volume slider is player volume",
           "Move the volume slider and tap the volume icon.",
           "The slider sets player volume from 0 to 1 live. The icon toggles 0 and 1.",
           "Must not boost to 200%.",
           "P2", platform="Desktop and wide windows")
    matrix(mu, "Row", "Current track uses the playing bars",
           "Play a song and look at its row.",
           "The title is primary and the index column shows playing bars instead of a number.",
           "Must not mark every row.",
           "P2")

    np = "Now Playing"
    for chip in ("0.5x", "0.75x", "1x", "1.25x", "1.5x", "2x"):
        matrix(np, "Speed", f"Chip {chip}",
               f"Tap Speed and choose {chip}.",
               "Engine speed changes and the sheet closes. The button shows the speed when it is not 1x.",
               "Must not show 0.25x, 3x, or 4x on this sheet.",
               "P1")
    matrix(np, "Lyrics", "Lyrics always says none",
           "Tap Lyrics on a song that has embedded lyrics and on one that does not.",
           "Toast No lyrics found either way.",
           "Must not open a lyrics panel.",
           "P1")
    matrix(np, "Queue", "Clear search restores the queue",
           "Type a query, then tap Clear.",
           "The full queue returns. No matching tracks shows only while the filter misses.",
           "Must not remove songs from the queue.",
           "P2")
    matrix(np, "A-B", "Second tap sets B at least 500 ms after A",
           "Tap A-B at a position, then again 100 ms later.",
           "B is at least 500 ms after A. Third tap clears it.",
           "Must not toast B must be after A; that toast is the video panel.",
           "P1")
    matrix(np, "Share", "Share uses the current song",
           "Tap Share.",
           "The platform share runs for this file.",
           "Must not share the previous song.",
           "P2")
    matrix(np, "Back", "Back does not stop playback",
           "Tap Back.",
           "Now Playing closes. The mini player still shows and audio continues.",
           "Must not clear the queue.",
           "P0")

    se = "Settings"
    matrix(se, "List", "NEW period 0 hides the tag",
           "Set Period tagged as NEW to 0. Look at a file modified today.",
           "No NEW tag. Set it back to 7 and the tag can return for an unplayed file.",
           "The slider must not show its summary sentence; that text exists only on the key.",
           "P1")
    matrix(se, "List", "Folders text is not a picker",
           "Open Folders. Type a path. OK.",
           "The trimmed text is saved. Empty means the whole device. There is no browse button.",
           "Must not scan only that path unless the library code is later taught to. QA should record whether the typed path actually limits the scan.",
           "P1")
    matrix(se, "Player", "Theme row swatch matches the tuner colours",
           "Change Frame and Progress bar colours. Return to the Player page.",
           "The Theme row's two-tone swatch shows those colours.",
           "Must not open App Theme.",
           "P2")
    matrix(se, "Audio", "Equalizer checkbox does not open the panel",
           "Toggle Audio equalizer.",
           "The checkbox saves. The player equalizer panel does not open.",
           "Must not start playback.",
           "P2")
    matrix(se, "General", "Import of a corrupt file fails",
           "Import a text file that is not a settings export.",
           "Toast Import failed, or unknown keys are ignored and valid keys still apply.",
           "Must not wipe settings on a failed import.",
           "P1")
    matrix(se, "General", "Export cancel is silent",
           "Tap Export and cancel the system picker.",
           "No toast. No file.",
           "Must not toast Settings exported.",
           "P1")
    matrix(se, "General", "iOS and Web import and export do nothing",
           "iOS or Web. Tap Export, then Import.",
           "No file and no toast, because the platform save and open return null.",
           "Must not crash.",
           "P1", platform="iOS, Web")
    matrix(se, "Index", "Each index row opens only its page",
           "From Settings, open List, Player, Decoder, Audio, Subtitle, and General, using back between them.",
           "The top bar title matches the row. Rows on the page match that catalog.",
           "Must not stack five pages so one back skips to Me.",
           "P0")

    t = "Display Tuner"
    for title, default, effect in [
        ("Elapsed time", "off", "Elapsed time shows in the persistent OSD only while controls are hidden."),
        ("Battery/clock", "off", "Clock and battery percent join that OSD."),
        ("Corner offset", "off", "OSD padding grows by the corner offset size."),
        ("Place at the bottom", "off", "The persistent OSD moves to the bottom."),
        ("Screen rotation button", "on", "This checkbox is not read by the player chrome. Do not expect a new button."),
        ("Interface auto hide", "on", "Controls hide after the delay while playing."),
        ("Show interface when locked screen is touched", "off", "A touch shows the lock icon, not the transport."),
    ]:
        matrix(t, "Flag", title,
               f"Toggle {title}. Default is {default}.",
               effect,
               "Must not change an unrelated gesture.",
               "P2" if "not read" in effect else "P1")
    for font in ("Default", "Sans serif", "Serif", "Monospace"):
        matrix(t, "Text", f"Font {font}",
               f"Text tuner → Font → {font}. Play a subtitle.",
               "The subtitle face changes, or stays default if the platform has no that face.",
               "Must not change the UI font of the library.",
               "P2")
    for align in ("Left", "Center", "Right"):
        matrix(t, "Layout", f"Alignment {align}",
               f"Layout → Alignment → {align}.",
               "Subtitle lines align that way.",
               "Must not move the video.",
               "P1")
    matrix(t, "Text", "Two Background Color rows are different",
           "On the Text tab, toggle the Background Color checkbox and edit the Background Color swatch.",
           "The checkbox enables text background. The swatch is its colour. They share a title on purpose.",
           "Must not confuse them with Layout → Background, which is the full subtitle background.",
           "P1")
    matrix(t, "Text", "Fade out is stored but not rendered",
           "Turn Fade out off and on. Play subtitles.",
           "The checkbox persists. The renderer does not fade either way.",
           "Do not file a missing fade as a settings-save bug.",
           "P2")
    matrix(t, "Style", "Colour cancel discards opacity too",
           "Open Frame. Change the swatch and Opacity. Tap Cancel. Open it again.",
           "The old colour is still there.",
           "OK must save both the swatch and the opacity.",
           "P1")

    pf = "Private Folder"
    matrix(pf, "PIN", "PIN field rejects letters and a fifth digit",
           "Create PIN. Type ab12, then 12345.",
           "Only 12 is kept from the first try. The fifth digit is ignored.",
           "Must not accept 12345.",
           "P0")
    matrix(pf, "PIN", "Mismatch returns to the create step",
           "Enter 1234, OK, then 4321, OK.",
           "Error PINs don't match. The prompt returns to Create a 4-digit PIN. Nothing is saved.",
           "Must not unlock.",
           "P0")
    matrix(pf, "PIN", "Cancel during confirm does not save",
           "Enter 1234 and OK, so the prompt is Confirm PIN. Tap Cancel.",
           "The screen closes. Reopening asks to create a PIN again.",
           "Must not accept 1234 as the PIN.",
           "P0")
    matrix(pf, "Files", "Add files FAB goes back",
           "Unlock and tap the Add files FAB.",
           "The screen closes. No picker opens.",
           "Must not be described as an import button in a pass.",
           "P1")
    matrix(pf, "Files", "Options Cancel does not move or delete",
           "Open Options and tap Cancel.",
           "The dialog closes. The file remains.",
           "Rename's own CANCEL keeps the options dialog open until Cancel or another action.",
           "P1")
    matrix(pf, "Files", "Move-out toast is shown even if the copy fails",
           "Move out of Private Folder on a platform where restore returns false.",
           "Toast still says Moved to Movies/MaxPlayerPro. Confirm whether the file actually left the folder.",
           "A pass requires the file to have moved, not only the toast.",
           "P0")

    pl = "Playlists"
    matrix(pl, "List", "Close returns without deleting",
           "Open VIDEO PLAYLISTS and tap Close.",
           "The previous screen returns. Playlists remain.",
           "Must not be confused with Delete playlist.",
           "P1")
    matrix(pl, "Detail", "Rename cancel keeps the name",
           "Tap Rename and CANCEL.",
           "The name is unchanged.",
           "OK with a blank trimmed name does rename to blank. That is what the code does.",
           "P1")
    matrix(pl, "Detail", "Remove does not ask",
           "Tap Remove.",
           "The row disappears immediately. The file is still in the library.",
           "There is no undo.",
           "P0")

    sh = "App Shell"
    matrix(sh, "Title bar", "Escape clears a non-empty search",
           "Wide title bar. Type a query. Press Escape. Press Escape again on an empty field.",
           "The first Escape clears the query. The second does not close the window.",
           "Must not save a blank recent search.",
           "P1", platform="Desktop and wide windows")
    matrix(sh, "Title bar", "Leaving Search clears the title-bar query",
           "Search from the title bar, then open Music.",
           "The title-bar query is empty.",
           "Must not keep filtering Music.",
           "P1", platform="Desktop and wide windows")
    matrix(sh, "Title bar", "Forward list clears on a normal navigation",
           "Open Music, Back, then open Settings instead of Forward.",
           "Forward is disabled.",
           "Must not later Forward into Music.",
           "P2", platform="Desktop and wide windows")
    matrix(sh, "Sidebar", "Selected row uses the primary tint",
           "Wide window. Tap Music, then Videos.",
           "The selected row uses primary at about 16% alpha. Hover uses surfaceVariant.",
           "Must not highlight two LIBRARY rows.",
           "P2", platform="Desktop and wide windows")
    matrix(sh, "Player host", "Overlay swallows taps while the engine starts",
           "Desktop, iOS, or Web. Start a video before the engine is ready.",
           "Black overlay, Initialising player…, and Loading video codecs for the first time. Taps do not reach the library.",
           "If start fails, the overlay closes.",
           "P0", platform="Desktop, iOS, Web")
    matrix(sh, "Launch", "Denial of permission still shows Home",
           "Android. Deny every permission.",
           "Home opens. Empty copy asks to grant media access and has no button.",
           "Must not crash or loop the dialog.",
           "P0", platform="Android")

    f = "Folder"
    matrix(f, "Wide", "Wide subtitle includes a zero count",
           "Open an empty folder on a wide window.",
           "Subtitle is 0 videos · size. Body says This folder is empty.",
           "Must not show the Home empty copy.",
           "P1", platform="Desktop and wide windows")
    matrix(f, "List", "Nested videos stay in the child folder",
           "Open a parent that contains a subfolder with videos.",
           "Those videos are not in the parent list. Opening the subfolder shows them.",
           "Must not flatten.",
           "P0")
    matrix(f, "View", "View menu on a folder does not navigate",
           "Change layout and tap Done, then Cancel on a second edit.",
           "Done saves and stays on the folder. Cancel discards.",
           "Must not pop to Home.",
           "P1")

    sc = "Search"
    matrix(sc, "History", "History caps at 20 and dedupes",
           "Submit 21 different queries, then submit the first one again.",
           "The latest 20 remain, newest first. The repeat moves to the top rather than appearing twice.",
           "Must not grow without a limit.",
           "P2")
    matrix(sc, "Query", "Search is not folder-scoped",
           "From a folder, open Search and type a name that exists only elsewhere.",
           "It is found.",
           "Must not show only the current folder.",
           "P1")
    matrix(sc, "Play", "Playing a result uses the result list as the queue",
           "Search returns three files. Play the second. Tap Next.",
           "Next is the third result, not the next file in the folder.",
           "Must not play a hidden non-match.",
           "P0", platform="Android")

    mm = "Media Manager"
    matrix(mm, "Storage", "Missing storage hides the block",
           "Open Media Manager where the platform returns no storage summary.",
           "The Device storage block is absent. Tiles still work.",
           "Must not show Used 0 / 0 as if that were measured.",
           "P2")
    matrix(mm, "Lists", "Clear history empties Recently Played",
           "Open Recently Played, clear history, return.",
           "Nothing here. Haven't Played grows by those files.",
           "Must not delete the files.",
           "P1")
    matrix(mm, "Preview", "Preview plays a queue of one",
           "Tap a Haven't Played thumbnail under the row.",
           "That file plays alone. Next toasts Last item.",
           "Must not play the whole unplayed list.",
           "P1", platform="Android")

    th = "Theme"
    matrix(th, "Mode", "Adaptive follows the system",
           "Choose Adaptive. Switch the OS between light and dark.",
           "The app follows the system. The Using badge stays on Adaptive.",
           "Must not stick on Dark Theme.",
           "P1")
    matrix(th, "Mode", "Amoled Black is not the same as Dark",
           "Choose Amoled Black, then Dark Theme.",
           "Black uses the black preview. Dark uses the dark preview. Only one card says Using.",
           "Must not leave both selected.",
           "P1")
    matrix(th, "Accent", "Accent does not recolour the player frame",
           "Pick a red swatch. Open a video.",
           "Library accents change. Player frame and progress colours stay on their own settings.",
           "Must not overwrite Frame.",
           "P1")

    me = "Me"
    matrix(me, "Tiles", "Tile order is stable",
           "Open Me.",
           "Row one: File Transfer, Private Folder, Video Playlists. Row two: Network Stream, Local Network, Media Manager. Row three: Cloud Drive. Then WhatsApp Status Saver, App Theme, Settings, Legal, Help.",
           "Must not reorder when Quit appears. Quit is last, and only when its setting is on.",
           "P2")
    matrix(me, "Info", "Back from an explanation returns to Me",
           "Open Cloud Drive, Legal, and WhatsApp Status Saver. Back from each.",
           "Each explanation has only the title and the reason. Back returns to Me.",
           "Must not open a browser.",
           "P1")

    ln = "Local Network"
    matrix(ln, "FAB", "Plus does not create a server",
           "Tap the FAB plus twice.",
           "Each tap toasts SMB/LAN servers are not supported yet. The empty copy remains.",
           "The plus drawn in the empty state is not clickable.",
           "P1")

    nb = "Not Built"
    matrix(nb, "Player", "No video cut control",
           "Search the player overflow and More panel for Cut.",
           "Cut is only the file-sheet toast. The cut icon in the player sources is unused.",
           "Must not expect a trimmer.",
           "P2")
    matrix(nb, "Player", "No external audio, stereo, or audio-sync control",
           "Open Audio Track and look for sync, stereo, or open-audio-file.",
           "Those controls are absent. Audio delay exists only in Settings.",
           "Must not fail Audio Track for missing sync buttons.",
           "P2")
    matrix(nb, "Settings", "Rotation button setting is unwired",
           "Toggle Screen rotation button. Look at the player.",
           "The rotation shortcut still depends on the Android capability, not this checkbox.",
           "Must not expect the shortcut to hide on Android when the checkbox is off.",
           "P2")


def build():
    shell()
    home()
    view_menu()
    folder()
    search()
    file_actions()
    music()
    now_playing()
    video()
    shortcuts()
    gestures()
    panels()
    settings()
    tuner()
    me()
    theme()
    help_page()
    playlists()
    private()
    media()
    network()
    not_built()
    expand()


NAVY = "1B3A4B"
TEAL = "1F6F6A"
P0C = "F4C7C3"
P1C = "FCE4B3"
P2C = "E7EEF2"
HEADER_FONT = Font(name="Calibri", bold=True, color="FFFFFF", size=11)
CELL_FONT = Font(name="Calibri", size=10)
THIN = Border(
    left=Side(style="thin", color="D0D7DE"),
    right=Side(style="thin", color="D0D7DE"),
    top=Side(style="thin", color="D0D7DE"),
    bottom=Side(style="thin", color="D0D7DE"),
)
WRAP = Alignment(wrap_text=True, vertical="top")


def style_header(ws):
    fill = PatternFill("solid", fgColor=NAVY)
    for col in range(1, len(HEADERS) + 1):
        cell = ws.cell(1, col, HEADERS[col - 1])
        cell.font = HEADER_FONT
        cell.fill = fill
        cell.alignment = Alignment(wrap_text=True, vertical="center")
    ws.row_dimensions[1].height = 32
    ws.auto_filter.ref = f"A1:{get_column_letter(len(HEADERS))}1"
    ws.freeze_panes = "A2"
    ws.auto_filter.add_sort_condition(f"J2:J{ws.max_row}")
    widths = [6, 18, 42, 28, 36, 42, 48, 42, 36, 10, 16, 16, 14, 14, 28]
    for i, w in enumerate(widths, 1):
        ws.column_dimensions[get_column_letter(i)].width = w
    ws.page_setup.orientation = "landscape"
    ws.page_setup.fitToPage = True
    ws.page_setup.fitToWidth = 1
    ws.page_setup.fitToHeight = 0
    ws.page_setup.paperSize = ws.PAPERSIZE_A4
    ws.sheet_properties.pageSetUpPr.fitToPage = True
    ws.oddHeader.left.text = "Max Player QA"
    ws.oddFooter.right.text = "Page &P of &N"
    ws.print_title_rows = "1:1"
    ws.page_setup.horizontalCentered = True
    ws.sheet_view.showGridLines = False
    ws.sheet_view.zoomScale = 110


def write_cases(ws, rows):
    rows = sorted(rows, key=lambda r: (PRI_ORDER[r["priority"]], r["section"], r["title"]))
    fills = {"P0": PatternFill("solid", fgColor=P0C), "P1": PatternFill("solid", fgColor=P1C), "P2": PatternFill("solid", fgColor=P2C)}
    for i, r in enumerate(rows, 1):
        values = [i, r["section"], r["title"], r["platform"], r["condition"], r["steps"],
                  r["expected"], r["must_not"], r["note"], r["priority"], "", "", "", "", ""]
        for col, value in enumerate(values, 1):
            cell = ws.cell(i + 1, col, value)
            cell.font = CELL_FONT
            cell.alignment = WRAP
            cell.border = THIN
            if col == 10:
                cell.fill = fills[r["priority"]]
                cell.alignment = Alignment(horizontal="center", vertical="center")
            if col == 1:
                cell.alignment = Alignment(horizontal="center", vertical="top")
        ws.row_dimensions[i + 1].height = 48
    dv = DataValidation(type="list", formula1='"Pass,Fail,Blocked,N/A"', allow_blank=True)
    dv.error = "Use Pass, Fail, Blocked, or N/A"
    dv.errorTitle = "Result"
    dv.add(f"K2:N{len(rows)+1}")
    ws.add_data_validation(dv)
    ws.conditional_formatting.add(f"K2:N{len(rows)+1}", CellIsRule(operator="equal", formula=['"Pass"'], fill=PatternFill("solid", fgColor="C8E6C9")))
    ws.conditional_formatting.add(f"K2:N{len(rows)+1}", CellIsRule(operator="equal", formula=['"Fail"'], fill=PatternFill("solid", fgColor="FFCDD2")))
    ws.conditional_formatting.add(f"K2:N{len(rows)+1}", CellIsRule(operator="equal", formula=['"Blocked"'], fill=PatternFill("solid", fgColor="FFE0B2")))
    ws.auto_filter.ref = f"A1:{get_column_letter(len(HEADERS))}{len(rows)+1}"
    style_header(ws)
    ws.auto_filter.ref = f"A1:{get_column_letter(len(HEADERS))}{len(rows)+1}"
    return len(rows)


def overview(wb, counts):
    ws = wb.active
    ws.title = "Overview"
    ws.sheet_properties.tabColor = TEAL
    ws["A1"] = "Max Player QA"
    ws["A1"].font = Font(name="Calibri", size=20, bold=True, color=NAVY)
    ws["A2"] = "Cases come from the current source, not from a generic MX Player checklist. There is no login screen."
    ws["A2"].font = Font(name="Calibri", size=11, italic=True, color="556068")
    ws.merge_cells("A2:H2")
    headers = ["Screen tab", "Open", "Cases", "P0", "P1", "P2", "Android Pass", "Android Fail", "Not run"]
    for col, h in enumerate(headers, 1):
        cell = ws.cell(4, col, h)
        cell.font = HEADER_FONT
        cell.fill = PatternFill("solid", fgColor=NAVY)
        cell.alignment = Alignment(wrap_text=True, vertical="center")
    total = [0, 0, 0, 0]
    for i, (name, n, p0, p1, p2) in enumerate(counts):
        row = 5 + i
        ws.cell(row, 1, name).font = Font(name="Calibri", size=11, bold=True)
        link = ws.cell(row, 2, f"Go to {name}")
        link.hyperlink = f"#'{name}'!A1"
        link.font = Font(name="Calibri", size=11, color="1155CC", underline="single")
        ws.cell(row, 3, f"=COUNTA('{name}'!C:C)-1")
        ws.cell(row, 4, f"=COUNTIF('{name}'!J:J,\"P0\")")
        ws.cell(row, 5, f"=COUNTIF('{name}'!J:J,\"P1\")")
        ws.cell(row, 6, f"=COUNTIF('{name}'!J:J,\"P2\")")
        ws.cell(row, 7, f"=COUNTIF('{name}'!K:K,\"Pass\")")
        ws.cell(row, 8, f"=COUNTIF('{name}'!K:K,\"Fail\")")
        ws.cell(row, 9, f"=C{row}-G{row}-H{row}-COUNTIF('{name}'!K:K,\"Blocked\")-COUNTIF('{name}'!K:K,\"N/A\")")
        for col in range(1, 10):
            ws.cell(row, col).font = Font(name="Calibri", size=11)
            ws.cell(row, col).alignment = Alignment(vertical="center")
            ws.cell(row, col).border = THIN
        total[0] += n
        total[1] += p0
        total[2] += p1
        total[3] += p2
    last = 4 + len(counts)
    total_row = last + 1
    ws.cell(total_row, 1, "TOTAL").font = Font(name="Calibri", size=11, bold=True)
    for col, letter in enumerate(["C", "D", "E", "F", "G", "H", "I"], 3):
        cell = ws.cell(total_row, col, f"=SUM({letter}5:{letter}{last})")
        cell.font = Font(name="Calibri", size=11, bold=True)
        cell.border = THIN
    ws.cell(total_row, 1).border = THIN
    ws.cell(total_row, 2).border = THIN
    note = total_row + 2
    lines = [
        "How to test: each tab is already sorted P0, then P1, then P2. Finish every P0 before P1. Use the filter arrow on Priority if a sort is cleared.",
        "Write Pass, Fail, Blocked, or N/A in Android Result, Desktop Result, iOS Result, and Web Result. Do not mark Desktop, iOS, or Web playback as Fail only because the engine is not shipped; use N/A and say so in QA Input.",
        "P0 is data loss, playback, permission, PIN, delete, and a control that blocks the main path. P1 is a normal CTA. P2 is cosmetic, rare encoding, or an unwired setting.",
        "Android is the only target with a shipped playback engine. Desktop, iOS, and Web have the UI. Their engines are planned.",
        "There is no bottom tab bar, no login, and no splash. Wide windows use the sidebar. Phones use the Home top bar and chips.",
        "Reference format was QA-UE.xlsx. Columns follow that workbook, with Must Not Happen added because every CTA needs both sides.",
    ]
    for offset, line in enumerate(lines):
        cell = ws.cell(note + offset, 1, line)
        cell.font = Font(name="Calibri", size=11)
        ws.merge_cells(start_row=note + offset, start_column=1, end_row=note + offset, end_column=9)
        ws.row_dimensions[note + offset].height = 32
    widths = [28, 22, 12, 10, 10, 10, 16, 14, 14]
    for i, w in enumerate(widths, 1):
        ws.column_dimensions[get_column_letter(i)].width = w
    ws.row_dimensions[1].height = 28
    ws.row_dimensions[4].height = 22
    ws.freeze_panes = "A5"
    ws.page_setup.orientation = "landscape"
    ws.page_setup.fitToPage = True
    ws.page_setup.fitToWidth = 1
    ws.page_setup.fitToHeight = 1
    ws.sheet_properties.pageSetUpPr.fitToPage = True
    ws.print_title_rows = "4:4"
    ws.oddHeader.left.text = "Max Player QA"
    ws.oddFooter.right.text = "Page &P of &N"
    return total


def p0_index(wb, order):
    ws = wb.create_sheet("P0 First", 1)
    ws.sheet_properties.tabColor = "B71C1C"
    headers = ["#", "Screen", "Section", "Title", "Open"]
    for col, h in enumerate(headers, 1):
        cell = ws.cell(1, col, h)
        cell.font = HEADER_FONT
        cell.fill = PatternFill("solid", fgColor="8C2F2F")
    n = 1
    for name in order:
        rows = sorted(SHEETS[name], key=lambda r: (PRI_ORDER[r["priority"]], r["section"], r["title"]))
        for i, r in enumerate(rows, 1):
            if r["priority"] != "P0":
                continue
            ws.cell(n + 1, 1, n)
            ws.cell(n + 1, 2, name)
            ws.cell(n + 1, 3, r["section"])
            ws.cell(n + 1, 4, r["title"])
            link = ws.cell(n + 1, 5, "Open case")
            link.hyperlink = f"#'{name}'!A{i+1}"
            link.font = Font(name="Calibri", size=10, color="1155CC", underline="single")
            for col in range(1, 5):
                ws.cell(n + 1, col).font = CELL_FONT
                ws.cell(n + 1, col).alignment = WRAP
            n += 1
    ws.auto_filter.ref = f"A1:E{n}"
    ws.freeze_panes = "A2"
    ws.column_dimensions["A"].width = 6
    ws.column_dimensions["B"].width = 20
    ws.column_dimensions["C"].width = 18
    ws.column_dimensions["D"].width = 62
    ws.column_dimensions["E"].width = 14
    ws.page_setup.orientation = "landscape"
    ws.page_setup.fitToPage = True
    ws.page_setup.fitToWidth = 1
    ws.page_setup.fitToHeight = 0
    ws.sheet_properties.pageSetUpPr.fitToPage = True
    ws.print_title_rows = "1:1"
    ws.oddHeader.left.text = "P0 first"
    ws.oddFooter.right.text = "Page &P of &N"
    return n - 1


def main():
    build()
    wb = Workbook()
    wb.calculation.calcMode = "auto"
    wb.calculation.fullCalcOnLoad = True
    order = [
        "App Shell", "Home", "Folder", "Search", "File Actions", "Music", "Now Playing",
        "Video Player", "Gestures", "Player Panels", "Settings", "Display Tuner", "Me",
        "Theme", "Help", "Playlists", "Private Folder", "Media Manager", "Local Network", "Not Built",
    ]
    counts = []
    for name in order:
        ws = wb.create_sheet(name)
        rows = SHEETS[name]
        n = write_cases(ws, rows)
        p0 = sum(1 for r in rows if r["priority"] == "P0")
        p1 = sum(1 for r in rows if r["priority"] == "P1")
        p2 = sum(1 for r in rows if r["priority"] == "P2")
        counts.append((name, n, p0, p1, p2))
        if name in ("Video Player", "Gestures", "Settings", "Private Folder"):
            ws.sheet_properties.tabColor = "C45C26"
        elif name == "Not Built":
            ws.sheet_properties.tabColor = "8D6E63"
        else:
            ws.sheet_properties.tabColor = "4F7C8A"
    p0n = p0_index(wb, order)
    totals = overview(wb, counts)
    wb.save(OUT)
    print("sheets", len(wb.sheetnames))
    print("cases", totals[0], "P0", totals[1], "P1", totals[2], "P2", totals[3], "p0index", p0n)
    for name, n, p0, p1, p2 in counts:
        print(f"{name}\t{n}\t{p0}\t{p1}\t{p2}")


if __name__ == "__main__":
    main()
