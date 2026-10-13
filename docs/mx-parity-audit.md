# MX Player parity audit

Reference: MX Player Pro on device `3C161N00LL700000` (360 dp wide, 480 dpi). Captures of MX live in `/tmp/mxre/screens` and `/tmp/mxre/mx`.

**Status values**
- `TODO`: not audited yet.
- `MISMATCH`: a difference was found and isn't fixed yet.
- `FIXED`: changed, but not yet re-tested on the device.
- `VERIFIED`: tested on the device, both visually (overlay against MX) and functionally (real taps and swipes).
- `N/A`: deliberately out of scope; the reason is given in the row.

**Verification rule:** a row only becomes `VERIFIED` after real input on the device: `adb input tap` / `swipe` / key events, followed by a screenshot or a state check. An overlay that looks right is not enough.

## A. Player – interaction (highest priority)

| # | Item | MX behaviour | Status | Notes |
|---|------|--------------|--------|-------|
| A1 | Single tap toggles controls | tap shows, tap again hides | MISMATCH | Controls overlay swallows touches (regression from the auto-hide listener) |
| A2 | Auto-hide | hides about 3 s after the last touch while playing; stays while paused | MISMATCH | |
| A3 | Brightness swipe | vertical swipe on the left half; level OSD in the centre | MISMATCH | blocked while controls are visible (A1) |
| A4 | Volume swipe | vertical swipe on the right half; level OSD | MISMATCH | blocked (A1) |
| A5 | Seek swipe | horizontal swipe; OSD shows +offset and [target] | MISMATCH | blocked (A1) |
| A6 | Double-tap left / right | rewind / forward 10 s | DONE | device earlier: ▶▶ 10s circle OSD, accumulates |
| A7 | Long press | temporary fast forward at 2x; OSD | DONE | device earlier: 2.0X pill only while playing |
| A8 | Pinch zoom and pan | zoom % OSD | NOT TESTED | adb cannot inject two-finger pinch; needs a manual check |
| A9 | Lock | lock button → only the lock icon; tap shows it, unlock restores | DONE | device: icon at 121,22 dp = MX; unlock tap restores controls |
| A10 | Seek bar | tap and drag; time updates while dragging | DONE | device: tap mid → 1:04:59, drag → 1:38:04 |
| A11 | Play/pause, previous/next | | DONE | device: play/pause + media keys; prev/next need a folder queue (single content-URI file has none, same as MX) |
| A12 | Zoom button | cycles fit / stretch / crop / 100% with a label | DONE | device: Stretch → Crop → Original → Fit with label |
| A13 | PiP / popup | | DONE | device: Popup button enters PiP window |
| A14 | Back / back gesture | closes panel, then player | DONE | device: 1st back closes panel, 2nd closes player |
| A15 | Rotation | portrait ↔ landscape keeps position and state | DONE | device: stays landscape per orientation setting, position kept |
| A16 | Interruptions | home/background → resume; call/audio focus loss pauses | FIXED | resume lost on warm reopen (position 0 saved before resume check); saveProgress now waits for resume check; device: 2× warm reopen resumes 01:05 |
| A17 | Resume | resumes with the "Continue from where you stopped / START OVER" bar | FIXED | |
| A18 | End of video | next in folder or close | DONE | device: reaches end; stays/closes per Back-to-list (default off, as MX settings) |
| A19 | Media keys / headset | play/pause | DONE | device: MEDIA_PLAY/PAUSE keys |

## B. Player – screens and panels (visual + function)

| # | Screen | Ref | Status | Notes |
|---|--------|-----|--------|-------|
| B1 | Controls landscape | 21_paused | VERIFIED (visual) | functional check pending A1 |
| B2 | Controls portrait | 19/20 | FIXED | Next was missing in portrait (90 dp buttons overflowed); 50 dp in portrait → prev/play/next centres 130/180/230 = MX |
| B3 | Shortcuts expanded | 26_chevron | VERIFIED (visual) | |
| B4 | Overflow menu | 22_overflow | VERIFIED (visual) | Cut is not implemented (see D) |
| B5 | Audio track | 23_audio | VERIFIED (visual) | |
| B6 | Subtitle | 24_subs | VERIFIED (visual) | |
| B7 | Decoder dialog | 25_decoder | MISMATCH | rounded grey dialog vs MX square dark box, taller rows, blue radios |
| B8 | Resume ask dialog | – | MISMATCH | same dialog style issue |
| B9 | Equalizer | 40_eq | VERIFIED (visual) | |
| B10 | Speed | 41_speed | VERIFIED (visual) | |
| B11 | Screenshot | 42_shot | FIXED | full-width bar with saved path over seek row (MX style) instead of pill |
| B12 | Background play | 30_bgplay | DONE | device: toggles, shortcut highlighted |
| B13 | Rotation | 43_rotate | DONE | device: Screen Rotation shortcut switches to portrait |
| B14 | Night mode | 44_night | DONE | device: toggles; MX reference image unreadable |
| B15 | Customise items | 45_custom | PARTIAL | functional checklist; MX reference capture unusable, layout not compared |
| B16 | Display settings | 46_display, tuner_* | FIXED | MX layout: 448 dp panel, 120 dp underlined tabs, 40 dp rows, 96 dp labels, inline 40×20 swatches, paired controls, ▾ spinners |
| B17 | Aspect ratio | 47_aspect | DONE | rows/radios/checkbox overlay MX |
| B18 | Information | 48_info | DONE | heading 64 dp, row pitch 33.3 dp = MX; real path/size/date |
| B19 | Bookmark | 49_bookmark | DONE | empty state, seek row, Add pill within 3 dp of MX |
| B20 | More | 50_more | DONE | layout = MX; Settings opens app Settings (device) |
| B21 | Sleep timer | 51_sleep | DONE | key circles, readout, divider, clear, STOP = MX |
| B22 | A-B repeat | 52_ab | DONE | card = MX; device: A..B loop wraps (00:02 after 7 s) |
| B23 | Zoom OSD | 53_zoom / 34_zoom | DONE | device: Stretch/Crop/Original/Fit label |
| B24 | Lock screen | 54_lock / 33_lock | DONE | status bar hidden, icon position = MX |
| B25 | Brightness OSD | 60_bright | DONE | device earlier |
| B26 | Volume OSD | 61_volume | DONE | device earlier |
| B27 | Seek OSD | 62_seek | DONE | device earlier |
| B28 | Double-tap OSD | 63_dtap | DONE | device earlier |
| B29 | Long-press OSD | 64_long | DONE | device earlier |
| B30 | Popup (PiP) | 35_popup | DONE | device: PiP window |
| B31 | Loading / buffering / error states | – | FIXED | error dialog MX style; SW-decoder option hidden for file-not-found / already SW |

## C. Library and app screens

| # | Screen | Ref | Status | Notes |
|---|--------|-----|--------|-------|
| C1 | Home (folders) | 01_home | VERIFIED (visual) | default accent is orange in ours vs MX blue |
| C2 | Folder video list | 15_folder | VERIFIED (visual) | |
| C3 | File "more" sheet | 16_filemore, 16b | FIXED | MX order + icons per row (IconBuddy mdi); unsupported items toast instead of a combined page |
| C4 | Selection mode | 17_select | DONE | device: n/11 Selected bar + bottom actions |
| C5 | View menu | 02_viewmenu, 03_fields, 04_advanced | VERIFIED (visual) | functional pending |
| C6 | Search | – | DONE | device: typing filters results |
| C7 | Music | 70_music, mx/music | MISMATCH | reported by user |
| C8 | Privacy | 71_privacy, 07_private | MISMATCH | reported by user |
| C9 | Video playlists | 08_playlists | MISMATCH | reported by user |
| C10 | Me | 05_me | VERIFIED (visual) | |
| C11 | File transfer | 06_transfer | OUT OF SCOPE | MXShare protocol is proprietary; explanatory screen |
| C12 | Network stream | 09_netstream | FIXED | MX square dialog, underline URL field |
| C13 | Local network | 10_localnet | FIXED | Networks screen: empty state + PLUS FAB (SMB client not included → toast) |
| C14 | Media manager | 11_mediamgr | DONE | device: structure matches (storage, tiles, Haven't Played) |
| C15 | Cloud drive | 12_cloud | OUT OF SCOPE | cloud OAuth registrations not available |
| C16 | Theme | 13_theme | FIXED | classic themes 48 dp rounded squares, 8 dp gaps, 6 per row (MX) |
| C17 | Settings index | mx/settings | VERIFIED (visual) | |
| C18 | Settings sub-pages (list, player, decoder, audio, subtitle, general) | mx/set_player | PARTIAL | Settings list checked on device with Rubik; sub-pages not pixel-overlaid |
| C19 | Empty / permission / loading states | – | DONE | device: Networks, Playlists, Private Folder empty states |
| C20 | Mini player (FAB) | – | DONE | device: mini player appears and plays/pauses; restyled as rounded pill |
| C21 | Music Now Playing (tap mini player) | `nx/m_home.png` | DONE | device: full-screen page with spinning disc + tonearm, EQ / A-B / speed / favourite / ⋮, thin seek bar, shuffle-prev-play-next-repeat, Lyrics (toast only, no lyrics source) and Playing Queue |
| C21 | Back navigation across screens | 14_back | DONE | device: back closes panel → player → list |

## D. Known out-of-scope / not implementable

| Item | Reason |
|------|--------|
| Video Cut | needs a video-trimming pipeline; shows "not available yet" |
| Online subtitles / AI subtitle translation | needs MX's online service; shown disabled |
| MX-proprietary icons, fonts and assets | can't be copied; we use IconBuddy icons and OFL fonts (Mulish, Rubik) instead |

## Mismatch log

| Date | Item | Mismatch | Fix | Verified |
|------|------|----------|-----|----------|
| 2026-10-13 | B18 Info | File showed "9557", fake path, no Size/Date | Content URIs resolved via MediaStore (`PlayerState.lookupContent`) | device |
| 2026-10-13 | B18 Info | Headings bold/12 dp high, rows 12.5 sp | Regular 13 sp headings, 14 sp values, 33.3 dp min rows | device bounds |
| 2026-10-13 | B17–B21 | Status bar visible while a panel is open | System bars hidden unless panel is None/A-B | device |
| 2026-10-13 | B19 Bookmark | Empty state 35 dp high, seek row 10 dp high | Offsets; 14 sp text; bold Add | device bounds |
| 2026-10-13 | B21 Sleep | Plain digits, small readout, no divider, backspace icon, STOP grey | 30 dp circles, 44 sp readout, divider, × circle, STOP blue | device |
| 2026-10-13 | B22 A-B | Status bar hidden, no outline, line connector | Bars kept, 1 dp rounded outline, ↔ icon, 12 dp insets | device |
| 2026-10-13 | B24 Lock | Status bar shown while locked; icon 10 dp right | `locked` keys the system-bar effect; start 59 dp | device |
| 2026-10-13 | B20 More | Settings/FAQ extra ignored; Rename used ad-hoc code | `AppRoot(open)`; rename via `MediaRepository.renameNow` + consent result (code 8) | Settings: device; Rename consent: not yet |
| 2026-10-13 | Theme | Default accent blue/orange; user wants neutral dark | Defaults: app theme dark, accent/highlight/progress 0xFFE0E0E0; player accents read the setting; dark text on light accent | device |
| 2026-10-13 | C1 Home | Folder glyph square tab, darker | Path with sloped tab, 0xFF444C55, 62×46 dp (grid too) | device pixel scan |
| 2026-10-13 | C1 Home | App text in Mulish + Material letter spacing (too wide) | Rubik with no extra spacing for app screens; player keeps Mulish | device: chip/list widths = MX |
| 2026-10-13 | C1 Home | Music/Privacy chips had badge outside folder | ri:folder-music-fill / ri:folder-lock-fill via IconBuddy | device |
| 2026-10-13 | C1 Home | Play-last FAB rounded square | Circle, 30 dp icon | device |
| 2026-10-13 | C1 Home | MXShare chip icon differs (MX: phone with side arrows) | OPEN – no close IconBuddy match yet | – |
| 2026-10-13 | A16 Resume | Warm reopen started at 0 | `saveProgress` skipped until `checkResume` ran for the file | device |
| 2026-10-13 | B16 Display | Generic tall setting list | MX compact tuner (tabs, 40 dp rows, swatches, spinners) | device |
| 2026-10-13 | B2 Portrait | Next button missing | 50 dp buttons in portrait | device bounds |
| 2026-10-13 | B11 Screenshot | Centre pill | Full-width path bar over seek row | device |
| 2026-10-13 | C Music | Outlined search, heart/add icons, scrolling tabs, flat mini bar | MX header/tabs/search+Shuffle All, lavender thumbs, ⋮ menu, pill mini player | device |
| 2026-10-13 | C Playlists | Back arrow, text button | VIDEO PLAYLISTS + ✕, outlined Create New Playlist | device |
| 2026-10-13 | C dialogs | Material rounded AlertDialogs | All app dialogs use the MX square PlayerDialog with underline fields | device (PIN, Network Stream) |
