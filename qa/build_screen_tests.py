#!/usr/bin/env python3
"""Screen-by-screen QA workbook. One sheet = one screen, walked top to bottom."""
from collections import defaultdict
from openpyxl import Workbook, load_workbook
from openpyxl.styles import Alignment, Border, Font, PatternFill, Side
from openpyxl.utils import get_column_letter
from openpyxl.worksheet.datavalidation import DataValidation
from openpyxl.formatting.rule import FormulaRule
from openpyxl.chart.shapes import GraphicalProperties
from openpyxl.drawing.line import LineProperties
from openpyxl.chart.legend import Legend
from openpyxl.workbook.properties import CalcProperties

SRC = "qa/MaxPlayer_UI_Control_Inventory.xlsx"
OUT = "qa/MaxPlayer_Screen_by_Screen_Tests.xlsx"

HEADER = Font(name="Calibri", bold=True, color="FFFFFF", size=11)
WRAP = Alignment(wrap_text=True, vertical="top")
THIN = Border(
    left=Side(style="thin", color="D0D7DE"), right=Side(style="thin", color="D0D7DE"),
    top=Side(style="thin", color="D0D7DE"), bottom=Side(style="thin", color="D0D7DE"),
)
COLS = ["ID", "Section", "Test case", "Steps", "Expected result", "Must not happen", "Platform", "Result", "Notes"]
WIDTHS = [10, 22, 36, 42, 48, 36, 22, 12, 28]

# Walk order. Tester finishes a sheet, fixes bugs, then opens the next sheet.
ORDER = [
    ("01 Home", "1F4E79"),
    ("02 Search", "0F766E"),
    ("03 Folder", "0F766E"),
    ("04 Player", "9F1239"),
    ("05 Music", "6D28D9"),
    ("06 Now Playing", "6D28D9"),
    ("07 Privacy", "92400E"),
    ("08 Video Playlist", "1D4ED8"),
    ("09 Profile", "334155"),
    ("10 Theme", "7C3AED"),
    ("11 Settings", "166534"),
    ("12 Settings List", "166534"),
    ("13 Settings Player", "166534"),
    ("14 Settings Decoder", "166534"),
    ("15 Settings Audio", "166534"),
    ("16 Settings Subtitle", "166534"),
    ("17 Settings General", "166534"),
    ("18 Media Manager", "155E75"),
    ("19 Networks", "155E75"),
    ("20 Help", "475569"),
    ("21 Wide window", "334155"),
]

LIST_KEYS = {
    "mark_last_played_media_for_each_folders", "scroll_down_to_last_media", "selection_mode",
    "list.floating_action_button", "new_tagged_period", "list.scan_folders", "list.extensions", "list.refresh_auto",
}
PLAYER_PAGE_KEYS = {
    "double_tap_back_key", "quick_zoom", "resume_last", "resume_only_first", "default_playback_speed",
    "remember_selections", "back_to_list", "seek_previews", "fast_seek", "audio_focus", "media_buttons",
    "honour_headset_hook_multi_press", "prevnext_to_rewff", "smart_prev", "suppress_error_message",
    "sticky_video", "sticky_audio", "album_art", "sticky_smooth_switch", "loading_circle_animation",
    "player.default_aspect",
}
DECODER_KEYS = {"default_decoder"}
AUDIO_KEYS = {
    "audio_player", "volume_boost", "sync_system_volume", "pause_on_headset_disconnected",
    "audio_fade_in_on_start", "audio_fade_in_on_seek", "audio_language", "audio_delay", "audio_effects_enabled",
    "eq.preset", "eq.bands", "eq.bass_boost", "eq.reverb",
}
SUB_PAGE_KEYS = {
    "subtitle_folder", "subtitle_charset", "subtitle_language", "subtitle_default_sync", "italic_tag", "force_ltr",
}
GENERAL_KEYS = {
    "play_video_links", "http_user_agent", "quit_button", "allow_edits", "delete_subtitle_files_together", "cache_thumbnail",
}
# Opened from the player (Display settings) and from Settings links. Kept on the Player sheet
# so the player pass does not jump to Settings.
DISPLAY_PREFIXES = ("osd.", "g.", "sub.", "screen_", "corner_offset", "full_screen", "soft_buttons",
                    "touch_action", "lock_mode", "interface_auto_hide", "gesture_seek_speed",
                    "show_move_buttons", "move_interval", "show_prev_next", "display_seeking_position",
                    "battery_clock", "show_left_time", "keep_screen_on", "pause_if_obscured", "show_interface",
                    "long_press_speed")
THEME_KEYS = {"app_theme", "accent_color"}


def is_display(key):
    return key.startswith(DISPLAY_PREFIXES) or key in {
        "screen_orientation", "full_screen", "soft_buttons", "screen_brightness_enabled", "screen_brightness",
        "screen_rotation_button", "battery_clock_in_title_bar", "show_left_time", "keep_screen_on",
        "pause_if_obscured", "show_interface_at_startup", "touch_action", "lock_mode",
        "interface_auto_hide", "interface_auto_hide_delay", "lock_show_interface",
        "gesture_seek_speed", "show_move_buttons", "move_interval", "show_prev_next", "display_seeking_position",
        "long_press_speed_value",
    }


def setting_sheet(key):
    if key in THEME_KEYS:
        return "10 Theme"
    if key in LIST_KEYS or key == "list.view_options":
        return "12 Settings List"
    if is_display(key):
        return "04 Player"
    if key in PLAYER_PAGE_KEYS:
        return "13 Settings Player"
    if key in DECODER_KEYS:
        return "14 Settings Decoder"
    if key in AUDIO_KEYS:
        return "15 Settings Audio"
    if key in SUB_PAGE_KEYS:
        return "16 Settings Subtitle"
    if key in GENERAL_KEYS:
        return "17 Settings General"
    return "13 Settings Player"


def control_sheet(surface):
    return {
        "Home": "01 Home", "View menu": "01 Home", "File list": "01 Home", "File actions": "01 Home",
        "Library sources": "01 Home",
        "Search": "02 Search", "Music search": "05 Music", "Music filter": "05 Music",
        "Folder": "03 Folder",
        "Video player": "04 Player", "Video OSD": "04 Player", "Video controls": "04 Player",
        "Video shortcuts": "04 Player", "Video transport": "04 Player", "Video lock": "04 Player",
        "Video dialog": "04 Player", "Video overflow": "04 Player", "Video panel": "04 Player",
        "Video keyboard": "04 Player", "Video system": "04 Player", "Host": "04 Player",
        "Music library": "05 Music", "Music mini player": "05 Music",
        "Now Playing": "06 Now Playing", "Music player search": "06 Now Playing",
        "Private folder": "07 Privacy",
        "Playlists": "08 Video Playlist", "Playlist detail": "08 Video Playlist",
        "Profile": "09 Profile", "Unavailable": "09 Profile",
        "Theme": "10 Theme",
        "Settings": "11 Settings",
        "Media manager": "18 Media Manager", "Media list": "18 Media Manager",
        "Local network": "19 Networks",
        "Help": "20 Help",
        "Sidebar": "21 Wide window", "Title bar": "21 Wide window",
    }.get(surface, "01 Home")


def section_rank(sheet, section):
    ranks = {
        "01 Home": ["Top bar", "Bottom chips", "List", "View menu", "File actions", "Empty / web folders", "Play last"],
        "04 Player": ["Open", "Gestures", "Controls", "Shortcuts", "Lock", "Panels", "Dialogs", "Keyboard", "Display settings"],
        "05 Music": ["Header", "Search and sort", "Tracks", "Playlists", "Albums Artists Folders", "Group page", "Mini player"],
        "06 Now Playing": ["Header", "Transport", "Queue", "Speed and tools"],
        "08 Video Playlist": ["List", "Detail"],
        "09 Profile": ["Top", "Tiles", "Rows", "Unavailable pages"],
    }
    order = ranks.get(sheet, [])
    return order.index(section) if section in order else 50


def classify_control(surface, kind, label):
    sheet = control_sheet(surface)
    section = surface
    if sheet == "01 Home":
        if surface == "Home" and kind in ("Chip",):
            section = "Bottom chips"
        elif surface == "Home" and kind in ("Button", "Title"):
            section = "Top bar" if label not in ("Play last",) else "Play last"
        elif surface == "Home" and kind in ("Empty", "Wide title", "Card", "Section", "Folder row"):
            section = "Empty / web folders" if kind == "Empty" else "List"
        elif surface == "View menu":
            section = "View menu"
        elif surface == "File list":
            section = "List"
        elif surface == "File actions":
            section = "File actions"
        elif surface == "Library sources":
            section = "Empty / web folders"
        elif label == "Play last":
            section = "Play last"
    elif sheet == "04 Player":
        section = {
            "Video player": "Gestures", "Video OSD": "Controls", "Video controls": "Controls",
            "Video transport": "Controls", "Video shortcuts": "Shortcuts", "Video lock": "Lock",
            "Video panel": "Panels", "Video dialog": "Dialogs", "Video overflow": "Panels",
            "Video keyboard": "Keyboard", "Video system": "Open", "Host": "Open",
        }.get(surface, "Controls")
    elif sheet == "05 Music":
        if surface in ("Music search", "Music filter") or kind in ("Field", "Menu") and "Search" in (label or "") or "Sort" in (label or ""):
            section = "Search and sort"
        elif "mini" in surface.lower():
            section = "Mini player"
        else:
            section = "Tracks"
    elif sheet == "06 Now Playing":
        section = "Queue" if "queue" in (label or "").lower() or "Search in queue" in (label or "") else "Transport"
    elif sheet == "08 Video Playlist":
        section = "Detail" if surface == "Playlist detail" else "List"
    elif sheet == "09 Profile":
        section = "Unavailable pages" if surface == "Unavailable" else ("Tiles" if kind == "Tile" else "Rows")
    return sheet, section


src = load_workbook(SRC, data_only=True)
buckets = defaultdict(list)


def add(sheet, section, title, steps, expected, must, platform, notes=""):
    buckets[sheet].append({
        "section": section, "title": title, "steps": steps, "expected": expected,
        "must": must or "", "platform": platform or "All", "notes": notes,
    })


controls = src["Controls"]
for r in range(2, controls.max_row + 1):
    surface, kind, label = controls.cell(r, 2).value, controls.cell(r, 3).value, controls.cell(r, 4).value
    shows, expected, must, platform = [controls.cell(r, c).value or "" for c in (5, 6, 7, 8)]
    source = controls.cell(r, 10).value or ""
    sheet, section = classify_control(surface, kind, label)
    add(sheet, section, f"{label}", f"On {surface}. {shows}", expected, must, platform, source)

apps = src["App screens"]
for r in range(2, apps.max_row + 1):
    surface, kind, label = apps.cell(r, 2).value, apps.cell(r, 3).value, apps.cell(r, 4).value
    shows, expected, must, platform = [apps.cell(r, c).value or "" for c in (5, 6, 7, 8)]
    source = apps.cell(r, 9).value or ""
    if surface in ("Music search", "Music filter", "Music player search"):
        continue  # already taken from Controls
    sheet, section = classify_control(surface, kind, label)
    add(sheet, section, f"{label}", f"On {surface}. {shows}", expected, must, platform, source)

# Copy the shared file-list cases onto screens that show the same list.
shared = [x for x in buckets["01 Home"] if x["section"] in ("List", "File actions", "View menu")]
for sheet, note in (
    ("02 Search", "Results list uses the same file row as Home."),
    ("03 Folder", "Folder list uses the same file row and View Menu as Home."),
    ("18 Media Manager", "Each media list uses the same file row as Home."),
):
    for item in shared:
        if item["section"] == "View menu" and sheet == "18 Media Manager":
            continue
        copy = dict(item)
        copy["notes"] = (note + " " + copy["notes"]).strip()
        buckets[sheet].append(copy)

# Settings, one row per setting, on the page where the tester will see it.
settings = src["Every setting"]
for r in range(2, settings.max_row + 1):
    reg, kind, key, title, default, summary, choices, how = [settings.cell(r, c).value or "" for c in range(1, 9)]
    sheet = setting_sheet(key)
    section = "Display settings" if sheet == "04 Player" else kind
    expected = how
    if choices:
        expected = f"Choices: {choices}. {how}"
    if default != "":
        expected = f"Default: {default}. {expected}"
    add(sheet, section, title, f"Open this settings page and find \"{title}\" (key {key}).", expected, "", "All", summary)

# Cases that were easy to miss because they are states, not buttons.
add("01 Home", "Bottom chips", "There is no Home tab",
    "Look at the bottom of the phone Home screen.",
    "The only bottom chips are Music, Privacy, and Video Playlist. Home is the screen you are already on. Account is the top-right icon, not a bottom tab.",
    "Do not expect a bottom tab labelled Home.", "Phone", "HomeComponents.kt chips")
add("01 Home", "Bottom chips", "Chips move to the bottom only when the option is on",
    "Open View Menu → Advanced → Show shortcuts at bottom. Turn it on, press Done. Then turn it off and press Done.",
    "On: Music, Privacy, Video Playlist sit at the bottom. Off: they sit under the top bar.",
    "Wide windows do not show these chips. Use the sidebar there.", "Phone", "")
add("04 Player", "Open", "Initialising player",
    "On desktop, web, or iOS, play a video before the engine is ready.",
    "A black cover shows Initialising player… and Loading video codecs for the first time. Taps do not reach the library underneath.",
    "Must not flash the library controls through this cover.", "Desktop, web, iOS", "App.kt PlayerInitialising")
add("04 Player", "Open", "Android player is its own screen",
    "On Android, play a video.",
    "The player opens as its own screen (PlayerActivity), not inside the library. Back returns to the library.",
    "Must not leave the library visible behind the player on Android.", "Android", "PlayerActivity.kt")
add("05 Music", "Playlists", "Music playlist cards",
    "Open Music → Playlists.",
    "Cards: New playlist, Favourites, Recently Played, then each saved music playlist. New playlist asks for a name. Favourites and Recently Played open that group's songs.",
    "Must not list video playlists here.", "All", "MusicScreen.kt")
add("05 Music", "Albums Artists Folders", "Album, artist, and folder cards",
    "Open Albums, then Artists, then Folders.",
    "Each card opens a group page of that name. Unknown names show as Unknown album, Unknown artist, or the folder name.",
    "Sort and grid toggle stay off on these tabs. They are only on Tracks.", "All", "MusicScreen.kt")
add("05 Music", "Group page", "Group page back, play, and shuffle",
    "Open any album, artist, folder, Favourites, Recently Played, or music playlist.",
    "Header shows that name, Back, Play all, and Shuffle. Back (or system back) returns to the tab you came from. Mini player stays at the bottom.",
    "This is not a new app route. It must not add a second Music entry in history.", "All", "MusicScreen.kt:116-128")
add("05 Music", "Search and sort", "Search does not search the queue",
    "Type a song name in Search songs, artists, albums… while something else is in the Now Playing queue.",
    "Only the current tab's library tracks filter. The playing queue is unchanged.",
    "Queue search is Search in queue on the Now Playing screen.", "All", "")
add("07 Privacy", "PIN", "First open creates a PIN",
    "Open Privacy with no PIN saved.",
    "Dialog title Private Folder. First prompt Create a 4-digit PIN, then Confirm PIN. OK only accepts 4 digits. Mismatch says PINs don't match.",
    "Must not show the file list before the PIN is created.", "All", "PinGate.kt")
add("07 Privacy", "PIN", "Later opens ask for the PIN",
    "Leave and open Privacy again.",
    "Prompt is Enter PIN. Wrong PIN clears the field and says Wrong PIN. Cancel goes back.",
    "Must not remember the unlock after leaving the screen.", "All", "PinGate.kt")
add("11 Settings", "Index", "Finish the index, then the page sheets",
    "Open Settings from Profile or the sidebar.",
    "Rows: List, Player, Decoder, Audio, Subtitle, General, Development, App Language. Test each row's destination, then do sheets 12 to 17 in order.",
    "Development must open the unavailable page, not a log screen.", "All", "SettingsScreen.kt")
add("21 Wide window", "Sidebar", "Phone has no sidebar",
    "Open the app on a phone.",
    "No sidebar. Music, Privacy, and Video Playlist are the Home chips. Profile is the Account icon.",
    "Do not fail the phone build for a missing sidebar.", "Phone", "Sidebar.kt")

# Stable order inside each sheet.
for sheet, items in buckets.items():
    items.sort(key=lambda x: (section_rank(sheet, x["section"]), x["section"], x["title"]))

wb = Workbook()
wb.calculation.calcMode = "auto"
cover = wb.active
cover.title = "Start here"

intro = [
    ("Start here", "Ek screen khatam karo, bugs fix karo, phir agla sheet."),
    ("", ""),
    ("Kaise use karein", "Neeche wale order mein jao. Ek sheet = ek screen. Us sheet ke saare test cases wahin hain. Dusre screen pe mat jao jab tak yeh sheet khatam na ho."),
    ("", ""),
    ("Order", "01 Home → 02 Search → 03 Folder → 04 Player → 05 Music → 06 Now Playing → 07 Privacy → 08 Video Playlist → 09 Profile → 10 Theme → 11 Settings → 12–17 Settings pages → 18 Media Manager → 19 Networks → 20 Help → 21 Wide window"),
    ("", ""),
    ("Home ke neeche", "Phone par Home tab nahi hai. Neeche sirf teen chips hain: Music, Privacy, Video Playlist. Home pehle se khula hota hai. Account upar right mein hai."),
    ("", ""),
    ("Wide window", "Laptop / desktop / wide browser par chips nahi hote. Sidebar se Videos, Music, Playlists, Private folder khulte hain. Yeh cases sheet 21 par hain. Phone test mein 21 skip kar sakte ho."),
    ("", ""),
    ("Result", "Har row ke Result column mein Pass, Fail, Blocked, ya Not run likho. Fail par Notes mein bug likho."),
    ("", ""),
    ("Source", "Labels source se hain. Jo button kaam nahi karta (Cut, Convert to MP3, File Transfer, Cloud Drive, Networks +) uska expected result toast ya unavailable page hai. Usse Pass mat samjho agar feature chal jaye, aur Fail mat samjho agar toast aaye."),
]
for row in intro:
    cover.append(row)
cover["A1"].font = Font(name="Calibri", bold=True, size=18, color="1F4E79")
cover.merge_cells("A1:B1")
cover.column_dimensions["A"].width = 22
cover.column_dimensions["B"].width = 120
for r in range(1, 16):
    cover.row_dimensions[r].height = 22
    cover.cell(r, 1).font = Font(name="Calibri", bold=True, size=12, color="1F4E79")
    cover.cell(r, 2).alignment = Alignment(wrap_text=True, vertical="center")
    cover.cell(r, 2).font = Font(name="Calibri", size=12)
cover.row_dimensions[1].height = 28
cover.row_dimensions[3].height = 36
cover.row_dimensions[5].height = 48
cover.row_dimensions[7].height = 36
cover.row_dimensions[9].height = 36
cover.row_dimensions[13].height = 48
cover.page_setup.orientation = "landscape"
cover.page_setup.fitToPage = True
cover.page_setup.fitToWidth = 1
cover.page_setup.fitToHeight = 1
cover.page_setup.paperSize = cover.PAPERSIZE_TABLOID
cover.sheet_properties.pageSetUpPr.fitToPage = True
cover.page_setup.horizontalDpi = 300
cover.page_setup.verticalDpi = 300
cover.oddFooter.right.text = "Page &P of &N"
cover.sheet_properties.tabColor = "1F4E79"
cover.oddHeader.left.text = "Max Player screen tests"
cover.print_title_rows = "1:1"
cover.page_setup.horizontalCentered = True
cover.sheet_view.showGridLines = False
cover.sheet_view.zoomScale = 120
cover.freeze_panes = "A3"

# Index with counts.
cover["A16"] = "Sheet"
cover["B16"] = "Test cases"
cover["C16"] = "Done when"
for cell in cover[16]:
    cell.font = Font(name="Calibri", bold=True, color="FFFFFF")
    cell.fill = PatternFill("solid", fgColor="1F4E79")
done = {
    "01 Home": "Top bar, three chips, list, view menu, and file menu pass.",
    "02 Search": "Search field, recent searches, and result rows pass.",
    "03 Folder": "Folder title, search, view menu, and file rows pass.",
    "04 Player": "Gestures, controls, panels, keyboard, and Display settings pass.",
    "05 Music": "Search, sort, all five tabs, group page, and mini player pass.",
    "06 Now Playing": "Play, queue search, speed, lyrics, and A-B pass.",
    "07 Privacy": "PIN create, PIN enter, and file actions pass.",
    "08 Video Playlist": "Create, open, play, rename, and remove pass.",
    "09 Profile": "Every tile and row opens the right page.",
    "10 Theme": "Four modes and accent swatches pass.",
    "11 Settings": "Index rows open the right page.",
    "12 Settings List": "Every List setting row passes.",
    "13 Settings Player": "Every Player setting row passes.",
    "14 Settings Decoder": "Decoder choice passes.",
    "15 Settings Audio": "Audio and equalizer rows pass.",
    "16 Settings Subtitle": "Subtitle rows pass.",
    "17 Settings General": "General rows and Clear / Reset / Export pass.",
    "18 Media Manager": "Storage cards and four lists pass.",
    "19 Networks": "Plus button shows the not-supported toast.",
    "20 Help": "Five FAQ topics are readable.",
    "21 Wide window": "Sidebar and title bar pass. Skip on a phone.",
}
for i, (name, color) in enumerate(ORDER):
    row = 17 + i
    cover.cell(row, 1, name).font = Font(name="Calibri", bold=True, color=color)
    cover.cell(row, 2, f"=COUNTA('{name}'!A:A)-1")
    cover.cell(row, 3, done[name])
    cover.cell(row, 3).alignment = WRAP
    cover.row_dimensions[row].height = 20
cover.column_dimensions["C"].width = 62
cover.auto_filter.ref = f"A16:C{16+len(ORDER)}"
cover.freeze_panes = "A17"
cover.print_title_rows = "16:16"
cover.page_setup.fitToHeight = 1
cover.sheet_view.view = "pageBreakPreview"
cover.page_setup.orientation = "landscape"
cover.oddHeader.center.text = "Screen order"

for name, color in ORDER:
    ws = wb.create_sheet(name)
    ws.sheet_properties.tabColor = color
    ws.append(COLS)
    items = buckets.get(name, [])
    prefix = name.split()[0]
    for n, item in enumerate(items, 1):
        ws.append([
            f"{prefix}-{n:03d}", item["section"], item["title"], item["steps"],
            item["expected"], item["must"], item["platform"], "", item["notes"],
        ])
    if ws.max_row == 1:
        ws.append(["", "", "No cases", "", "", "", "", "", ""])
    for cell in ws[1]:
        cell.font = HEADER
        cell.fill = PatternFill("solid", fgColor=color)
        cell.alignment = Alignment(wrap_text=True, vertical="center")
    for i, w in enumerate(WIDTHS, 1):
        ws.column_dimensions[get_column_letter(i)].width = w
    for r in range(2, ws.max_row + 1):
        ws.row_dimensions[r].height = 48
        for c in range(1, 10):
            cell = ws.cell(r, c)
            cell.alignment = WRAP
            cell.border = THIN
            cell.font = Font(name="Calibri", size=10)
            if r % 2 == 0:
                cell.fill = PatternFill("solid", fgColor="F8FAFC")
    ws.freeze_panes = "A2"
    ws.auto_filter.ref = f"A1:I{ws.max_row}"
    ws.auto_filter.add_sort_condition("B2:B2000")
    ws.page_setup.orientation = "landscape"
    ws.page_setup.fitToPage = True
    ws.page_setup.fitToWidth = 1
    ws.page_setup.fitToHeight = 0
    ws.page_setup.paperSize = ws.PAPERSIZE_TABLOID
    ws.sheet_properties.pageSetUpPr.fitToPage = True
    ws.print_title_rows = "1:1"
    ws.page_setup.horizontalDpi = 300
    ws.page_setup.verticalDpi = 300
    ws.page_setup.leftMargin = 0.4
    ws.page_setup.rightMargin = 0.4
    ws.page_setup.topMargin = 0.6
    ws.page_setup.bottomMargin = 0.5
    ws.sheet_view.zoomScale = 110
    ws.oddHeader.left.text = name
    ws.oddFooter.right.text = "Page &P of &N"
    ws.oddFooter.left.text = "Result: Pass / Fail / Blocked / Not run"
    dv = DataValidation(type="list", formula1='"Pass,Fail,Blocked,Not run"', allow_blank=True)
    dv.error = "Use Pass, Fail, Blocked, or Not run"
    dv.errorTitle = "Result"
    dv.add(f"H2:H{max(ws.max_row, 2)}")
    ws.add_data_validation(dv)
    last = max(ws.max_row, 2)
    ws.conditional_formatting.add(f"A2:I{last}", FormulaRule(formula=['$H2="Fail"'], fill=PatternFill("solid", fgColor="FECACA")))
    ws.conditional_formatting.add(f"A2:I{last}", FormulaRule(formula=['$H2="Pass"'], fill=PatternFill("solid", fgColor="BBF7D0")))
    ws.conditional_formatting.add(f"A2:I{last}", FormulaRule(formula=['$H2="Blocked"'], fill=PatternFill("solid", fgColor="FDE68A")))
    ws.sheet_view.view = "pageBreakPreview"

wb.properties.title = "Max Player screen-by-screen test cases"
wb.properties.creator = "Max Player QA"
wb.save(OUT)
print("sheets", len(wb.sheetnames))
for name, _ in ORDER:
    print(f"{name}: {len(buckets.get(name, []))}")
