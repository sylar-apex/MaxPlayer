#!/usr/bin/env python3
"""Append app-shell screens to the existing QA inventory workbook."""
from openpyxl import load_workbook
from openpyxl.styles import Alignment, Border, Font, PatternFill, Side
from openpyxl.utils import get_column_letter
from openpyxl.worksheet.datavalidation import DataValidation
from openpyxl.formatting.rule import FormulaRule

PATH = "qa/MaxPlayer_UI_Control_Inventory.xlsx"
HEADER = Font(name="Calibri", bold=True, color="FFFFFF", size=11)
HEADER_FILL = PatternFill("solid", fgColor="7C3AED")
ALT = PatternFill("solid", fgColor="F5F3FF")
UNAVAIL = PatternFill("solid", fgColor="FDE68A")
WRAP = Alignment(wrap_text=True, vertical="top")
THIN = Border(
    left=Side(style="thin", color="D0D7DE"),
    right=Side(style="thin", color="D0D7DE"),
    top=Side(style="thin", color="D0D7DE"),
    bottom=Side(style="thin", color="D0D7DE"),
)

rows = []

def add(surface, kind, label, shows, behavior, must_not, platform, source):
    rows.append((surface, kind, label, shows, behavior, must_not, platform, source))

ALL = "All"
WIDE = "Wide windows (sidebar / desktop)"
PHONE = "Phone / narrow"
WEB = "Web (folder import available)"

# Sidebar
add("Sidebar", "Brand", "Max Player / PRO", "Wide windows only.", "Brand row. Not a button.", "Must not appear on phones.", WIDE, "Sidebar.kt:99-107")
add("Sidebar", "Section", "LIBRARY", "Wide windows.", "Section header, not tappable.", "", WIDE, "Sidebar.kt:110")
add("Sidebar", "Nav", "Videos", "Wide windows.", "Pops back to Home. Stays highlighted on Home, Folder, and Search.", "", WIDE, "Sidebar.kt:63, 85-89")
add("Sidebar", "Nav", "Music", "Wide windows.", "Opens Music, stacked on Home so history does not pile up.", "", WIDE, "Sidebar.kt:64")
add("Sidebar", "Nav", "Playlists", "Wide windows.", "Opens video playlists. Highlighted on playlist detail too.", "", WIDE, "Sidebar.kt:65")
add("Sidebar", "Nav", "Private folder", "Wide windows.", "Opens Private Folder PIN gate.", "", WIDE, "Sidebar.kt:66")
add("Sidebar", "Section", "MORE", "Wide windows.", "Section header.", "", WIDE, "Sidebar.kt:112")
add("Sidebar", "Nav", "Network stream", "Wide windows.", "Opens Local Network (title Networks), NOT the Network Stream URL dialog.", "Must not open the URL dialog. That dialog is only on the Me page.", WIDE, "Sidebar.kt:70; AppNavGraph.kt:88")
add("Sidebar", "Nav", "Storage", "Wide windows.", "Opens Media Manager.", "", WIDE, "Sidebar.kt:71")
add("Sidebar", "Nav", "Theme", "Wide windows.", "Opens App Theme.", "", WIDE, "Sidebar.kt:72")
add("Sidebar", "Nav", "Settings", "Wide footer.", "Opens Settings index.", "", WIDE, "Sidebar.kt:76")
add("Sidebar", "Nav", "Help", "Wide footer.", "Opens Help FAQ.", "", WIDE, "Sidebar.kt:77")

# Title bar
add("Title bar", "Button", "Back", "macOS / wide title bar when the page sets onBack.", "Goes back.", "", WIDE, "TitleBar.kt:133")
add("Title bar", "Button", "Forward", "Title bar.", "Goes forward in navigation.", "", WIDE, "TitleBar.kt:134")
add("Title bar", "Button", "Refresh", "Title bar. Hidden while refreshing (spinner instead).", "Refreshes the library.", "", WIDE, "TitleBar.kt:136")
add("Title bar", "Field", "Search videos", "Title bar search field. Placeholder when empty. Clear appears when text is present.", "Typing updates the host query. On the Search page, the query is the title-bar field, not a second field on the page.", "Must not show a second Search videos field on the Search page when the title bar is present.", WIDE, "TitleBar.kt:191-194; SearchScreen.kt:44-49")
add("Title bar", "Button", "Clear", "Title bar search, only when the query is not empty.", "Clears the query.", "", WIDE, "TitleBar.kt:194")

# Home phone
add("Home", "Title", "Videos / Folders", "Phone home. Videos when view mode is Files, otherwise Folders.", "Top bar title follows view mode.", "", PHONE, "HomeScreen.kt:86")
add("Home", "Button", "Refresh", "Phone top bar, and wide header when not using the title-bar refresh.", "Rescans the library. Wide header shows a spinner instead of the button while scanning.", "", ALL, "HomeScreen.kt:87; HomeWide.kt:70-72")
add("Home", "Button", "Open folder", "Only if FolderImport.available (web). Top bar icon and empty-state button.", "Opens the folder picker. Empty state text: Choose a folder to add its videos. Every subfolder is included.", "Must not appear on Android/iOS/desktop that scan storage themselves.", WEB, "HomeScreen.kt:88, 127-132")
add("Home", "Button", "Search", "Phone home and phone folder screen.", "Opens Search.", "Wide home uses the header/title-bar search instead of this icon.", PHONE, "HomeScreen.kt:89; FolderScreen.kt:55")
add("Home", "Button", "View Menu", "Phone home and phone folder.", "Opens the view/sort/filter dialog.", "Wide uses View options (grid icon) for the same dialog.", PHONE, "HomeScreen.kt:90")
add("Home", "Button", "Account", "Phone home only.", "Opens the Me / profile page.", "Wide uses the sidebar instead. There is no account login.", PHONE, "HomeScreen.kt:91; AppNavGraph.kt:39")
add("Home", "Chip", "Music", "Phone shortcut chips. Top unless Show shortcuts at bottom is on.", "Opens Music.", "", PHONE, "HomeComponents.kt:58")
add("Home", "Chip", "Privacy", "Phone shortcut chips.", "Opens Private Folder.", "", PHONE, "HomeComponents.kt:59")
add("Home", "Chip", "Video Playlist", "Phone shortcut chips.", "Opens video playlists. Label is Video Playlist, not Playlists.", "", PHONE, "HomeComponents.kt:60")
add("Home", "Button", "Play last", "Phone FAB, only if a last-played file exists and floating button setting is on (default true).", "Plays the last played media.", "Must not show if there is no last played file.", PHONE, "HomeScreen.kt:108-113; AppKeys floatingButton")
add("Home", "Empty", "Scanning your drives…", "Library empty and still loading.", "Progress empty state.", "", ALL, "HomeScreen.kt:124")
add("Home", "Empty", "Your videos will appear here once your folders are connected.", "Web, remembered folders waiting for permission.", "Does not offer Open folder; the restore card above explains the action.", "", WEB, "HomeScreen.kt:126")
add("Home", "Empty", "No videos found.", "Scan finished, no videos, and folder import is not available.", "Also says Grant media access to scan your device.", "", "Android / iOS / desktop without import", "HomeScreen.kt:134")
add("Home", "Wide title", "All videos / Library", "Wide home. All videos in Files mode, otherwise Library.", "Subtitle is Scanning your drives… while loading, or '{n} videos in {m} folders'.", "", WIDE, "HomeWide.kt:61-65")
add("Home", "Button", "View options", "Wide home and wide folder.", "Opens the same View Menu dialog. A11y name is View options, not View Menu.", "", WIDE, "HomeWide.kt:74; FolderScreen.kt:50")
add("Home", "Card", "Continue watching", "Wide grid layout, only if lastPlayed is set.", "Plays last watched.", "Must not appear in list layout.", WIDE, "HomeWide.kt:104-107")
add("Home", "Section", "Folders", "Wide grid. Trailing count is the folder count.", "Folder cards. Path shown only in All folders mode.", "", WIDE, "HomeWide.kt:109-111")
add("Home", "Folder row", "{n} video / videos", "Folder list.", "Optional size if Size field is on. Path if All folders or Path field is on. NEW ribbon if the folder is in newFolders.", "", ALL, "HomeComponents.kt:146-150")

# View menu / filter
add("View menu", "Dialog", "View Mode", "View Menu / View options.", "Choices: All folders, Files, Folders. Changes the home title and list.", "Cancel discards. Done saves.", ALL, "ViewMenuDialog.kt:87-91; Library.kt:12")
add("View menu", "Dialog", "Layout", "Same dialog.", "List or Grid.", "", ALL, "ViewMenuDialog.kt:96-100")
add("View menu", "Sort", "Title, Date, Played time, Status, Length, Size, Resolution, Path, Frame rate, Type", "Same dialog. All ten keys, unlike Music which only offers four.", "Selecting a key does not apply until Done.", "Must not treat this as the music Sort menu.", ALL, "ViewMenuDialog.kt:106-115; Library.kt:16-19")
add("View menu", "Sort", "A to Z / Z to A", "Under sort keys.", "Sets ascending or descending.", "", ALL, "ViewMenuDialog.kt:117-119")
add("View menu", "Filter fields", "Fields", "Expander. Checkboxes: Thumbnail, Length, File extension, Played time, Resolution, Frame rate, Path, Size, Date.", "Unchecked fields are hidden from rows.", "", ALL, "ViewMenuDialog.kt:123-127; Library.kt:21-24")
add("View menu", "Advanced", "Display length over thumbnail", "Advanced expander.", "Puts duration on the thumbnail instead of the detail line.", "", ALL, "ViewMenuDialog.kt:132")
add("View menu", "Advanced", "Show hidden files and folders", "Advanced. Default off.", "Includes hidden names when on.", "", ALL, "ViewMenuDialog.kt:133")
add("View menu", "Advanced", "Recognize .nomedia", "Advanced. Default on.", "Honors .nomedia when on.", "", ALL, "ViewMenuDialog.kt:134")
add("View menu", "Advanced", "Show thumbnail on videos", "Advanced. Default on.", "Off shows a plain video icon.", "", ALL, "ViewMenuDialog.kt:135")
add("View menu", "Advanced", "Show thumbnail on folders", "Advanced. Default on.", "Off shows a folder icon.", "", ALL, "ViewMenuDialog.kt:136")
add("View menu", "Advanced", "Show shortcuts at bottom", "Advanced, phones only.", "Moves Music / Privacy / Video Playlist chips to the bottom.", "Must not appear on wide.", PHONE, "ViewMenuDialog.kt:137")
add("View menu", "Button", "Cancel / Done", "Dialog footer.", "Cancel closes without saving. Done saves options and closes.", "", ALL, "ViewMenuDialog.kt:141-144")

# File list
add("File list", "Row", "Tap video", "Home files, folder, search results, media lists.", "Plays that file in the video player. Long-press starts selection.", "", ALL, "FileList.kt:183-185")
add("File list", "Tag", "NEW", "File modified within new-tagged period (default 7 days) and never played.", "Tag on the row or card.", "", ALL, "FileList.kt:166, 178; AppKeys.newTaggedPeriod")
add("File list", "Tag", "SUB", "A sibling file shares the title and has a subtitle extension.", "Tag on list rows.", "", ALL, "FileList.kt:177")
add("File list", "Button", "More", "Each row or card, when not selecting.", "Opens the file actions sheet.", "", ALL, "FileList.kt:186, 263")
add("File list", "Selection", "Select all", "Selection bar after long-press, or thumbnail tap if Select thumbnail is on.", "Selects or clears every visible file. Back exits selection.", "", ALL, "FileList.kt:197")
add("File list", "Selection", "Share", "Selection bar.", "Shares selected files.", "", ALL, "FileList.kt:198")
add("File list", "Selection", "Add to playlist", "Selection bar.", "Opens Add To Playlist for video playlists only (isMusic false).", "Must not list music playlists.", ALL, "FileList.kt:199, 219")
add("File list", "Selection", "Lock", "Selection bar.", "Moves selected files into Private Folder and clears selection.", "", ALL, "FileList.kt:200")
add("File list", "Selection", "Rename", "Selection bar. Enabled only when exactly one file is selected.", "Opens Rename dialog prefilled with the title.", "Must stay disabled for multi-select.", ALL, "FileList.kt:201")
add("File list", "Selection", "Delete", "Selection bar.", "Dialog title Delete. One file: Delete \"{name}\"?. Many: Delete {n} files?. DELETE / CANCEL.", "", ALL, "FileList.kt:202, 225-232")
add("File actions", "Sheet", "File Transfer", "More sheet. Always shown.", "Toast: File Transfer is not available in Max Player Pro.", "Must not start a transfer.", ALL, "FileList.kt:210, 310")
add("File actions", "Sheet", "Lock in Private Folder", "More sheet.", "Moves the file into private storage.", "", ALL, "FileList.kt:311")
add("File actions", "Sheet", "Convert to MP3", "More sheet.", "Toast: Convert to MP3 is not available in Max Player Pro.", "Must not convert.", ALL, "FileList.kt:312")
add("File actions", "Sheet", "Share", "More sheet.", "Shares the file.", "", ALL, "FileList.kt:313")
add("File actions", "Sheet", "Rename", "More sheet. Disabled if Allow editing is off.", "Rename dialog.", "Must not rename when editing is disabled.", ALL, "FileList.kt:314; AppKeys.allowEdits")
add("File actions", "Sheet", "Cut", "More sheet.", "Toast: Cut is not available in Max Player Pro.", "Must not open a video cutter. The player overflow also has no cut control.", ALL, "FileList.kt:315")
add("File actions", "Sheet", "Search Subtitle", "More sheet.", "Toast: Online subtitle search is not available in Max Player Pro.", "Must not search the web for subtitles.", ALL, "FileList.kt:316")
add("File actions", "Sheet", "Properties", "More sheet.", "Dialog Properties. Rows File, Location, Size, Date, Length, Resolution, Format. OK.", "", ALL, "FileList.kt:317, 325-350")
add("File actions", "Sheet", "Add To Playlist", "More sheet.", "Video playlists only.", "", ALL, "FileList.kt:318")
add("File actions", "Sheet", "Delete", "More sheet. Disabled if editing is off.", "Delete confirmation.", "", ALL, "FileList.kt:319")

# Search
add("Search", "Title", "Search", "Search page.", "Phone has a back bar. Wide uses WideHeader Search and a result count subtitle '{n} results' once the query is not blank.", "", ALL, "SearchScreen.kt:49-50")
add("Search", "Field", "Search videos", "Phone search page only.", "Filters as you type. Keyboard search submits and stores history.", "Must not duplicate this field when the title bar already has Search videos.", PHONE, "SearchScreen.kt:51-53")
add("Search", "List", "Recent searches", "Query blank and history is not empty.", "Tap a history row fills the query. Does not auto-play.", "", ALL, "SearchScreen.kt:54-61")
add("Search", "Empty", "No results for \"{query}\".", "Query not blank and no matches.", "Uses the shared file list empty state.", "", ALL, "SearchScreen.kt:63")

# Profile / Me
add("Profile", "Button", "Navigate up", "Me page top.", "Back.", "", ALL, "ProfileScreen.kt:72")
add("Profile", "Tile", "File Transfer", "Me card.", "Unavailable page. Reason mentions MXShare is not implemented.", "Must not transfer files.", ALL, "ProfileScreen.kt:76")
add("Profile", "Tile", "Private Folder", "Me card.", "Opens Private Folder.", "", ALL, "ProfileScreen.kt:77")
add("Profile", "Tile", "Video Playlists", "Me card.", "Opens VIDEO PLAYLISTS.", "", ALL, "ProfileScreen.kt:78")
add("Profile", "Tile", "Network Stream", "Me card.", "Opens the URL dialog (Please enter a network URL). This is not the sidebar Network stream item.", "", ALL, "ProfileScreen.kt:79, 108")
add("Profile", "Tile", "Local Network", "Me card.", "Opens Networks.", "", ALL, "ProfileScreen.kt:80")
add("Profile", "Tile", "Media Manager", "Me card.", "Opens Media Manager.", "", ALL, "ProfileScreen.kt:81")
add("Profile", "Tile", "Cloud Drive", "Me card.", "Unavailable. Google Drive, Dropbox and OneDrive are not implemented.", "Must not sign in to a cloud account.", ALL, "ProfileScreen.kt:82")
add("Profile", "Row", "WhatsApp Status Saver", "Me page.", "Unavailable. Needs WhatsApp folder permission; not implemented.", "Must not save statuses.", ALL, "ProfileScreen.kt:94-96")
add("Profile", "Row", "App Theme", "Me page.", "Opens App Theme.", "", ALL, "ProfileScreen.kt:99")
add("Profile", "Row", "Settings", "Me page.", "Opens Settings.", "", ALL, "ProfileScreen.kt:100")
add("Profile", "Row", "Legal", "Me page.", "Unavailable info page. Not a license viewer.", "", ALL, "ProfileScreen.kt:103")
add("Profile", "Row", "Help", "Me page.", "Opens Help.", "", ALL, "ProfileScreen.kt:104")
add("Profile", "Row", "Quit", "Me page only if Quit Button setting is on (default off).", "Quits the app.", "Must not show unless the setting is on.", ALL, "ProfileScreen.kt:105; AppKeys.quitButton")

# Folder
add("Folder", "Title", "Folder name", "Opened from a home folder.", "Wide subtitle '{n} videos · {size}'. Empty: This folder is empty.", "", ALL, "FolderScreen.kt:47-58")
add("Folder", "Button", "Search / View Menu", "Phone folder bar.", "Search opens global Search, not an in-folder filter. View Menu is the same dialog as home.", "Must not filter only this folder's name in a local search box; there is no folder search field.", PHONE, "FolderScreen.kt:54-56")

# Playlists
add("Playlists", "Title", "VIDEO PLAYLISTS", "Video playlists page.", "Close dismisses.", "", ALL, "PlaylistsScreen.kt:62")
add("Playlists", "Button", "Create New Playlist", "Under the title.", "Dialog label Playlist name. Blank name does not create.", "", ALL, "PlaylistsScreen.kt:67-82")
add("Playlists", "Row", "{name} / {n} videos", "Each video playlist.", "Opens detail.", "", ALL, "PlaylistsScreen.kt:75")
add("Playlists", "Empty", "No Playlists found.", "No video playlists.", "", "", ALL, "PlaylistsScreen.kt:69")
add("Playlist detail", "Button", "Play all", "Detail bar.", "Plays from index 0.", "", ALL, "PlaylistsScreen.kt:106")
add("Playlist detail", "Button", "Rename", "Detail bar.", "Dialog title Rename playlist.", "", ALL, "PlaylistsScreen.kt:107, 122")
add("Playlist detail", "Button", "Delete playlist", "Detail bar.", "Deletes and goes back.", "", ALL, "PlaylistsScreen.kt:108")
add("Playlist detail", "Button", "Remove", "Each video row.", "Removes that URI from the playlist. Does not delete the file.", "Must not delete the video file.", ALL, "PlaylistsScreen.kt:116")
add("Playlist detail", "Empty", "Add videos from a file's ⋮ menu → Add To Playlist.", "Playlist has no files.", "", "", ALL, "PlaylistsScreen.kt:120")

# Private folder
add("Private folder", "Dialog", "Private Folder PIN", "Until unlocked.", "First use: Create a 4-digit PIN then Confirm PIN. Later: Enter PIN. OK / Cancel. Errors: PIN must be 4 digits, PINs don't match, Wrong PIN.", "Must not open the file list before a correct PIN.", ALL, "PinGate.kt:44-68")
add("Private folder", "Empty", "You haven't added any files yet.", "Unlocked and empty.", "Tells the user to use ⋮ → Lock in Private Folder.", "", ALL, "PrivateFolderScreen.kt:70")
add("Private folder", "Button", "Options", "Each private file.", "Move out of Private Folder, Rename, Delete, Cancel.", "", ALL, "PrivateFolderScreen.kt:80, 94-99")
add("Private folder", "Button", "Add files", "FAB after unlock.", "Goes back to the previous screen. It does not open a picker.", "Must not expect a file picker from this button.", ALL, "PrivateFolderScreen.kt:85")

# Media manager
add("Media manager", "Title", "Media Manager", "From Me or sidebar Storage.", "Device storage, Used {used} / {total}. Cards Video, Music, Image.", "", ALL, "MediaManagerScreen.kt:47-58")
add("Media manager", "Card", "Recently Played", "List cards.", "Opens that list in FileList.", "", ALL, "MediaListKind.kt:4; MediaManagerScreen.kt:63")
add("Media manager", "Card", "Large Files", "List cards.", "Opens that list.", "", ALL, "MediaListKind.kt:4")
add("Media manager", "Card", "Recently Added", "List cards.", "Opens that list.", "", ALL, "MediaListKind.kt:4")
add("Media manager", "Row", "Haven't Played  ›", "Below the cards.", "Opens the unplayed list. Preview thumbnails play that file.", "", ALL, "MediaManagerScreen.kt:69-74")
add("Media list", "Empty", "Nothing here.", "A media list with no files.", "", "", ALL, "MediaManagerScreen.kt:86")

# Local network
add("Local network", "Title", "Networks", "Me Local Network, or sidebar Network stream.", "Empty prompt: Add a server by tapping the PLUS BUTTON.", "", ALL, "LocalNetworkScreen.kt:36-42")
add("Local network", "Button", "+", "FAB.", "Toast: SMB/LAN servers are not supported yet.", "Must not add a server.", ALL, "LocalNetworkViewModel.kt:14")

# Theme
add("Theme", "Title", "App Theme", "Me or sidebar.", "Modes Adaptive, Dark Theme, Light Theme, Amoled Black. Selected card says Using.", "", ALL, "ThemeScreen.kt:43-57; AppKeys.appTheme")
add("Theme", "Swatch", "Classic Themes", "Below modes.", "Accent colour swatches. No text label on each swatch.", "", ALL, "ThemeScreen.kt:59-65")

# Settings
add("Settings", "Index", "List, Player, Decoder, Audio, Subtitle, General", "Settings index.", "Each opens its page.", "", ALL, "SettingsPages.kt:7; SettingsScreen.kt:68")
add("Settings", "Index", "Development", "Settings index.", "Unavailable page: Developer diagnostics are not exposed in this build.", "Must not show codec logs.", ALL, "AppNavGraph.kt:73; SettingsScreen.kt:69")
add("Settings", "Index", "App Language", "Settings index.", "Opens the platform language settings.", "", ALL, "SettingsScreen.kt:70")
add("Settings", "Player", "Theme / Style / Screen / Controls / Navigation", "Player settings page.", "Theme and Style both open tuner tab 0. Screen, Controls, Navigation open tabs 1–3.", "", ALL, "SettingsScreen.kt:108-115")
add("Settings", "Subtitle", "Text / Layout", "Subtitle settings page.", "Text opens tuner tab 4. Layout opens tab 5.", "", ALL, "SettingsScreen.kt:119-120")
add("Settings", "Decoder", "Decoder note", "Decoder page.", "Explains HW, SW, and that HW+ behaves like HW. The only setting row is the default decoder.", "", ALL, "SettingsScreen.kt:122-124")
add("Settings", "Action", "Clear history", "General → User data.", "Confirm Clear all history? OK / Cancel.", "", ALL, "SettingsScreen.kt:157-158")
add("Settings", "Action", "Clear thumbnail cache", "General → User data.", "Clears immediately. No confirm dialog.", "", ALL, "SettingsScreen.kt:160")
add("Settings", "Action", "Reset settings", "General → User data.", "Confirm Reset all settings?", "", ALL, "SettingsScreen.kt:163-164")
add("Settings", "Action", "Export / Import", "General → User data.", "Export or import settings and activity records.", "", ALL, "SettingsScreen.kt:166-167")

# Help and unavailable
add("Help", "FAQ", "Gestures, Lock, Background play, Subtitles, Private Folder", "Help page. Questions are the row titles; answers are the body text.", "Read-only. Lock answer says tap the small lock icon; kids lock actually requires a long press. QA should follow code, not this answer, for kids lock.", "", ALL, "HelpTopics.kt:6-10")
add("Unavailable", "Page", "File Transfer / Cloud Drive / Legal / Development / WhatsApp Status Saver", "Reached from Me or Settings.", "Info page with the reason string. Back only.", "Must not implement the feature.", ALL, "AppNavGraph.kt:30, 43, 73; Common.kt InfoScreen")

# Library sources
add("Library sources", "Notice", "Restore access", "Web, a remembered folder needs permission.", "Welcome back. Allow access again… Button Restore access.", "", WEB, "LibrarySourcesBar.kt:59-63")
add("Library sources", "Notice", "Pick folder", "Web, browser cannot keep folder access.", "Pick {name} again. Button Pick folder.", "", WEB, "LibrarySourcesBar.kt:64-68")
add("Library sources", "Notice", "Remove", "A folder is missing.", "\"{name}\" was moved, renamed or its drive is disconnected. Button Remove.", "", WEB, "LibrarySourcesBar.kt:69-70")
add("Library sources", "Chip", "Add folder", "Web, at least one source exists.", "Opens the folder picker. Ready chips have a remove control.", "Hidden when folder import is unavailable.", WEB, "LibrarySourcesBar.kt:74-80")

# Music search/filter called out so it is not "missing"
add("Music search", "Field", "Search songs, artists, albums…", "Music library, under the header on phones and in the header on wide.", "Filters the current tab's tracks by title, artist, or album. Does not search the Now Playing queue.", "Queue search is a different field: Search in queue.", ALL, "MusicScreen.kt search field")
add("Music filter", "Menu", "Sort: Title / Date / Length / Size", "Music Tracks tab only.", "Only those four. Not the home View Menu's ten sort keys.", "Must not offer Resolution, Path, Frame rate, Status, or Played time here.", ALL, "MusicScreen.kt sort menu")
add("Music player search", "Field", "Search in queue", "Now Playing queue, wide always, narrow from Playing queue.", "Filters the playing queue. Clear clears it. Empty: No matching tracks.", "Must not change the music library search.", ALL, "NowPlayingScreen.kt:387-438")

wb = load_workbook(PATH)
if "App screens" in wb.sheetnames:
    del wb["App screens"]
ws = wb.create_sheet("App screens", 1)
cols = ["ID", "Surface", "Kind", "Exact label", "When it shows", "Expected behavior", "Must not happen", "Platform", "Source", "QA result", "Notes"]
ws.append(cols)
for i, row in enumerate(rows, 1):
    ws.append((f"A{i:03d}",) + row + ("", ""))

for cell in ws[1]:
    cell.font = HEADER
    cell.fill = HEADER_FILL
    cell.alignment = Alignment(wrap_text=True, vertical="center")
widths = [8, 20, 16, 42, 40, 52, 46, 28, 36, 14, 28]
for i, w in enumerate(widths, 1):
    ws.column_dimensions[get_column_letter(i)].width = w
for r in range(2, ws.max_row + 1):
    ws.row_dimensions[r].height = 46
    kind = ws.cell(r, 3).value or ""
    must = ws.cell(r, 7).value or ""
    fill = UNAVAIL if "not available" in must or "not implemented" in must or "Must not" in must and "not " in must and kind in ("Sheet", "Tile", "Row", "Page", "Button") and "not " in (ws.cell(r, 6).value or "") else (ALT if r % 2 == 0 else None)
    for c in range(1, 12):
        cell = ws.cell(r, c)
        cell.alignment = WRAP
        cell.border = THIN
        cell.font = Font(name="Calibri", size=10)
        if "not available" in (ws.cell(r, 6).value or "") or "not supported" in (ws.cell(r, 6).value or "") or "not implemented" in (ws.cell(r, 6).value or ""):
            cell.fill = UNAVAIL
        elif fill:
            cell.fill = fill
ws.freeze_panes = "A2"
ws.auto_filter.ref = f"A1:K{ws.max_row}"
ws.auto_filter.add_sort_condition("B2:B2")
ws.page_setup.orientation = "landscape"
ws.page_setup.fitToPage = True
ws.page_setup.fitToWidth = 1
ws.page_setup.fitToHeight = 0
ws.page_setup.paperSize = ws.PAPERSIZE_TABLOID
ws.sheet_properties.pageSetUpPr.fitToPage = True
ws.print_title_rows = "1:1"
ws.page_setup.horizontalDpi = 300
ws.page_setup.verticalDpi = 300
ws.oddHeader.left.text = "App screens"
ws.oddFooter.right.text = "Page &P of &N"
ws.sheet_properties.tabColor = "7C3AED"
dv = DataValidation(type="list", formula1='"Pass,Fail,Blocked,Not run"', allow_blank=True)
dv.add(f"J2:J{ws.max_row}")
ws.add_data_validation(dv)
ws.conditional_formatting.add(f"J2:J{ws.max_row}", FormulaRule(formula=['$J2="Fail"'], fill=PatternFill("solid", fgColor="FECACA")))
ws.conditional_formatting.add(f"J2:J{ws.max_row}", FormulaRule(formula=['$J2="Pass"'], fill=PatternFill("solid", fgColor="BBF7D0")))

# Coverage map
if "Screen map" in wb.sheetnames:
    del wb["Screen map"]
sm = wb.create_sheet("Screen map", 2)
sm.append(["Screen", "Where it opens", "Inventoried in", "Notes"])
coverage = [
    ("Home (phone)", "App start", "App screens", "Videos/Folders, chips, view menu, play last"),
    ("Home (wide)", "Sidebar Videos", "App screens", "Library header, continue watching, folder cards"),
    ("View menu / filter / sort", "View Menu or View options", "App screens", "10 sort keys, fields, advanced filters"),
    ("Search", "Home search or title bar", "App screens", "Search videos, recent searches"),
    ("Folder", "Home folder row", "App screens", "Same file list and view menu"),
    ("File actions", "Row more menu", "App screens", "Includes unavailable Cut, MP3, subtitle search"),
    ("Profile / Me", "Account or not in sidebar", "App screens", "Tiles and rows"),
    ("Music library", "Chip, sidebar, Me is not music", "Controls sheet", "Search songs and 4-key sort"),
    ("Music mini player", "While music is playing", "Controls sheet", "Narrow and wide"),
    ("Now Playing", "Tap mini player", "Controls sheet", "Search in queue, speed, lyrics, A-B"),
    ("Video player", "Play a video", "Controls sheet", "Gestures, panels, keyboard, lock"),
    ("Video playlists", "Chip, sidebar, Me", "App screens", "Separate from music playlists"),
    ("Private folder", "Privacy chip, sidebar, Me", "App screens", "PIN gate"),
    ("Media manager", "Me or sidebar Storage", "App screens", "Recent, large, added, unplayed"),
    ("Local network", "Me, or sidebar item named Network stream", "App screens", "Plus button is not implemented"),
    ("Network Stream dialog", "Me tile only", "Controls sheet", "Not the sidebar Network stream item"),
    ("Theme", "Me or sidebar", "App screens", "Four modes and accent swatches"),
    ("Settings", "Me, sidebar, player More", "App screens", "Pages plus tuner tabs"),
    ("Help", "Me, sidebar, player FAQ", "App screens", "Five FAQ topics"),
    ("Unavailable pages", "File Transfer, Cloud, Legal, Development, Status Saver", "App screens", "Explanation only"),
]
for row in coverage:
    sm.append(row)
for cell in sm[1]:
    cell.font = HEADER
    cell.fill = PatternFill("solid", fgColor="1F4E79")
for i, w in enumerate([28, 42, 22, 55], 1):
    sm.column_dimensions[get_column_letter(i)].width = w
for r in range(2, sm.max_row + 1):
    for c in range(1, 5):
        sm.cell(r, c).alignment = WRAP
        sm.cell(r, c).border = THIN
    sm.row_dimensions[r].height = 28
sm.freeze_panes = "A2"
sm.auto_filter.ref = f"A1:D{sm.max_row}"
sm.page_setup.orientation = "landscape"
sm.page_setup.fitToPage = True
sm.page_setup.fitToWidth = 1
sm.page_setup.fitToHeight = 1
sm.sheet_properties.pageSetUpPr.fitToPage = True
sm.print_title_rows = "1:1"
sm.oddFooter.right.text = "Page &P of &N"

cover = wb["Read me"]
cover["A16"] = "App screens added"
cover["A16"].font = Font(bold=True, color="7C3AED")
cover["A17"] = "Home, search, view menu filters, profile, folder, playlists, private folder, media manager, settings, and sidebar are on the App screens sheet. Yellow rows are controls that exist but do not perform the named feature."
cover["A17"].alignment = WRAP
cover.merge_cells("A17:F17")
cover.row_dimensions[17].height = 32
cover["B18"] = f"=COUNTA('App screens'!A:A)-1"
cover["A18"] = "App-screen controls"
cover["A18"].font = Font(bold=True)

wb.save(PATH)
print(f"app_rows={len(rows)}")
