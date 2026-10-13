# MX Player Pro parity checklist

Reference: `com.mxtech.videoplayer.pro` 3.0.12 (versionCode 2001003512), APK SHA-256
`b5e743a930a415446d483b0bfd733cb8a40dbfb332aea26209235e7c473c55cc`, inspected on a
Realme RMX5107 (1080x2362) on 2026-10-13.

Evidence sources:

- **Manifest** – component list and permissions (`aapt2 dump xmltree`).
- **Prefs** – `res/xml/frag_*.xml` preference screens with resolved English labels and defaults.
- **UI** – live UI-hierarchy dumps and screenshots taken on the device.
- **Unknown** – static code was not decompiled (JADX provider not installed), so internal
  algorithms are inferred from observed behaviour, never copied.

Status: `done` implemented and tested · `partial` implemented with a noted limitation ·
`gap` not implemented (reason given).

## 1. Home / Folders (`ActivityMediaList`) – UI

| Feature | Observed behaviour | Status |
|---|---|---|
| Title bar | "Folders" title, Search, View Menu, Account (Me) actions | done |
| Shortcut chips | Music, Privacy, MXShare, Video Playlist horizontal chips | done (MXShare → gap page) |
| Folder rows | Folder icon, name, "N videos" | done |
| Floating play-last button | Bottom-right FAB resumes last played media (pref `list.floating_action_button`) | done |
| Clipboard URL detection | Toast "pasted from your clipboard" on launch | gap (privacy-sensitive; network stream dialog covers URL play) |

## 2. View Menu dialog – UI

| Feature | Observed | Status |
|---|---|---|
| View mode | All folders / Files / Folders | done |
| Layout | List / Grid | done |
| Sort | Title, Date, Played time, Status, Length, Size, Resolution, Path, Frame rate, Type | done (frame rate sorts as unknown when MediaStore lacks it) |
| Direction | A to Z / Z to A | done |
| Fields | Thumbnail, Length, File extension, Played time, Resolution, Frame rate, Path, Size, Date | done |
| Advanced | Display length over thumbnail, Show hidden files and folders, Recognize .nomedia | done |
| Cancel / Done | Changes apply only on Done | done |

## 3. Folder file list & file actions – UI

| Feature | Observed | Status |
|---|---|---|
| File row | Thumbnail with duration badge, title, subtitle tag (e.g. "SSA"), more button | done (tag shows when a same-name subtitle file exists) |
| "NEW" tag | Pref `new_tagged_period` default 7 days | done |
| Last-played highlight | Last played item coloured (pref `mark_last_played_media_for_each_folders`) | done |
| More sheet | File Transfer, Lock in Private Folder, Convert to MP3, Share, Rename, Cut, Search Subtitle, Properties, Add To Playlist, Delete | partial – Share, Rename, Lock in Private Folder, Properties, Add To Playlist, Delete done; File Transfer / Convert to MP3 / Cut / Search Subtitle are gaps |
| Long-press selection | "N / M Selected", Play, Info, More; bottom bar Select all, Move, Copy, Play, Tag, Rename, Delete | partial – select all, play, info, share, add to playlist, rename (single), delete done; Move/Copy/Tag gaps |
| Search | Search videos by name | done |

## 4. Me page (`MeActivity`) – UI

| Tile | Observed | Status |
|---|---|---|
| File Transfer | SEND / RECEIVE, Share with Jio/iOS/PC/Tablet, History (MXShare P2P) | gap (proprietary P2P protocol) |
| Private Folder | Empty state + add FAB, More options | done (app-private storage, moves files in/out) |
| Video Playlists | Create New Playlist, empty state | done |
| Network Stream | Dialog "Please enter a network URL" with example, Cancel/OK | done |
| Local Network | "Networks", add-server FAB (SMB) | gap (needs SMB client library) |
| Media Manager | Storage used, Video/Music/Image sizes, Clean up, Recently Played, Large Files, Recently Added, Haven't Played | partial – storage, category sizes, Recently Played, Large Files, Recently Added, Haven't Played done; cleaner gap |
| Cloud Drive | Google Drive / Dropbox / OneDrive login | gap (needs OAuth app registrations) |
| WhatsApp Status Saver | | gap |
| App Theme | Adaptive, Dark, Light, Amoled Black + Classic theme swatches | done (4 modes + accent swatches) |
| Settings / Legal / Help | | done (Settings); Legal/Help are info pages |

## 5. Player screen (`ActivityScreen`) – UI

| Feature | Observed | Status |
|---|---|---|
| Top bar | Back, title, Audio track, Subtitles, decoder label (HW/HW+/SW), overflow | done |
| Shortcut row (collapsed) | Equalizer, Speed (1X), Screenshot, Background play, Rotation, expand chevron | done |
| Shortcut row (expanded) | Night Mode, Customise Items, Shuffle, Loop, Mute, Sleep Timer, A-B Repeat, Equalizer, Speed, Screenshot, Background Play, Screen Rotation | done |
| Seek bar | Elapsed left, remaining (`-h:mm:ss`) right | done |
| Bottom bar | Lock, Prev, Play/Pause, Next, Zoom/fit cycle, Popup (PiP) | done |
| Lock | Hides everything; small lock icon top-left unlocks | done |
| Zoom cycle | Fit → Stretch → Crop → 100% | done |
| Popup / PiP | `sticky_video` default `pip` | done (system PiP) |
| Panels are translucent right-side sheets over video | | done |

### Player panels

| Panel | Observed contents | Status |
|---|---|---|
| Overflow | Aspect Ratio, Speed FF (Long press) 2.0X, Display Settings, Playlists, Network Stream, Information, Share, Cut, Bookmark, Tutorial, More; Video Display & Shortcuts switches | partial – all except Cut and Tutorial |
| Audio Track | Track radio list, Disable, Use SW audio decoder, Open (external audio), Stereo mode, Synchronization / AV sync | partial – tracks, disable, SW decoder switch, volume/boost, open external audio done; AV sync and stereo mode are gaps |
| Subtitle | Track checkboxes, Open, Online subtitles, AI Subtitle Translation, Sync (±0.1 s), Speed (%), Panel | partial – embedded tracks (verified on device), Open external .srt/.vtt, sync & speed for external files done; sync of embedded tracks, online & AI are gaps; auto-loading a same-name subtitle beside the video only works where MediaStore exposes it (limited on Android 13+) |
| Decoder | HW / HW+ / SW | partial – label & selection stored; ExoPlayer uses platform decoders (prefer software decoders when SW chosen) |
| Speed | 0.25x–4.0x slider, −/+ , editable value, reset | done |
| Equalizer | On/off, presets (Custom, Normal, Classical, Dance, …), 5 bands 60 Hz–14 kHz, Reverb, Bass Boost % | done (android.media.audiofx) |
| Sleep Timer | h:mm keypad, clear, "Play last media to the end", STOP/START | done |
| A-B Repeat | A and B buttons, close | done |
| Aspect Ratio | Default, 1:1, 4:3, 16:9, Custom, Apply to all videos | done |
| Information | File (name, location, size, date, subtitle), Media (title, format), streams | done |
| Bookmark | Empty state, Import, current position, Add | done (import is gap) |
| More | Delete, Rename, Lock, Settings, FAQ, Check for updates, Bug report | partial – Delete, Rename, Lock, Settings |

### Display Settings tabs (also Settings → Player)

| Tab | Options observed | Status |
|---|---|---|
| Style | Preset, Frame colour, Control normal/highlight colours, Progress bar colour & style, Place progress bar below buttons | done |
| Screen | Orientation, Full Screen, Soft buttons, Brightness (+level), Elapsed time, Battery/clock, Corner offset, OSD text/background colours, Place at bottom, Screen rotation button, Battery/clock in title bar, Show remaining time, Keep screen on, Pause if obstructed, Show interface at startup | done (corner offset & soft buttons are stored only) |
| Controls | Touch action, Lock mode; gestures: Seek position, Zoom and Pan, Video zoom, Video pan, Volume, Brightness, Double-tap play/pause, Double-tap zoom, Double-tap FF/RW, Long-press Speed FF 2x, Playback speed, Subtitle scroll, Subtitle up/down, Subtitle zoom; Quick Shortcuts toggles; Interface auto hide (2.0 s); Show interface when locked screen touched | done (subtitle scroll gap) |
| Navigation | Seek speed (10 sec/cm), Forward/backward buttons, Move interval (10 s), Previous/next button, Display position while seeking | done |
| Subtitle Text | Font, Size (19), Scale (100%), Colour, Bold, Background colour, Border + colour + thickness, Improve stroke, Shadow, Fade out, SSA options | done (SSA-specific options gap) |
| Subtitle Layout | Alignment (Center), Bottom margins (8), Background, Fit subtitles into video | done |

## 6. Settings (`pro_preference_header`) – Prefs

Every key below is modelled in `shared/.../core/Settings.kt` with the observed default.

- **List**: Theme, Last media in each folder, Scroll down to last media, Select thumbnail, Floating
  button, NEW period, Folders, File extensions, Recognize .nomedia, Show hidden, Refresh method – done.
- **Player**: Theme, tuner tabs, Double-tap back, Quick zoom, Resume (ask/always/never), Resume only
  first, Default speed, Remember selections, Back to list, Preview while seeking, Fast seeking, Play
  alone (audio focus), Media buttons, Double/Triple press, Next/Prev→FF/Rew, Smart previous, Suppress
  error, Custom PiP, Background/PiP mode, Background audio, Album art, Smooth switch – done; Misc HW
  resizing options – stored only.
- **Decoder**: HW+/SW toggles, CPU core limit, colour format, deinterlace, custom codec – gap (MX
  ships its own FFmpeg decoder; we use ExoPlayer/MediaCodec).
- **Audio**: Audio player, Output, Volume boost (200%), System volume, Pause on headset disconnect,
  Fade on start/seek, Preferred audio language, Audio delay, Bluetooth delay, Passthrough, Equalizer
  – done except output method/passthrough (stored only).
- **Subtitle**: Folder, Charset, Preferred language, HW subtitle, Default sync, appearance, Italic
  effect, Force LTR – done (charset auto/UTF-8 only).
- **General**: App language, HW acceleration, Play media links, User-Agent, Quit button, TV mode,
  Allow editing, Delete subtitles together, Clear history, Clear thumbnails, Reset, Export/Import –
  done except TV mode and font cache (no SSA renderer).

## 7. Other subsystems (Manifest)

| Subsystem | Status |
|---|---|
| Background play foreground service + media notification + lock-screen art (`PlayService`, `MediaButtonReceiver`) | done (Media3 `MediaSessionService`) |
| Music library (Tracks, Playlists, Albums, Artists, Folders, search, Shuffle all, mini player) | done |
| Music widgets, lyrics, Gaana | gap |
| Video playlists | done |
| Private folder with PIN (`PrivateVerifyActivity`) | done |
| Casting (`CastActivity`) | gap (needs Google Cast app ID) |
| Video clip editor / MP3 convert | gap (needs FFmpeg) |
| Help / bug report / legal | partial (static pages) |
| TV mode activities | gap |
| MXShare / File Transfer, SMB local network, cloud drives, WhatsApp status saver | gap (shown as "not available" pages) |
| Move / copy / tag in multi-select | gap |

## 8. On-device verification (Realme RMX5107, Android 16)

Verified by hand on the device: permission flow, folder scan, file list with thumbnails and progress bars,
view menu, file "more" sheet, multi-select bar, Me page, Settings → Player page, Music tabs + mini player,
Private Folder PIN prompt, play-last FAB and resume prompt, player controls, collapsed/expanded shortcuts,
Speed (2x applied), Equalizer (on, vertical bands), Audio track panel, Subtitle panel (embedded English track
rendered), decoder switch HW → SW (playback continues), Sleep Timer keypad, A-B card, Customise Items,
lock/unlock, picture-in-picture (playback + scaled subtitles), background audio after Home.
Not yet exercised on device: drag gestures (seek/volume/brightness), pinch zoom, screenshot file output,
network streams, playlist/bookmark persistence across restarts, settings export/import.
