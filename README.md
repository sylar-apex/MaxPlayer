<div align="center">

<img src="appcore/appres/src/commonMain/composeResources/drawable/app_logo.png" width="160" alt="Max Player logo" />

# Max Player

**A fast, gesture-driven video and music player built with Kotlin Multiplatform and Compose.**

![Kotlin](https://img.shields.io/badge/Kotlin-2.4-7F52FF?logo=kotlin&logoColor=white)
![Compose Multiplatform](https://img.shields.io/badge/Compose_Multiplatform-1.12-4285F4?logo=jetpackcompose&logoColor=white)
![Media3](https://img.shields.io/badge/Media3-ExoPlayer-3DDC84?logo=android&logoColor=white)
![Koin](https://img.shields.io/badge/DI-Koin-F88909)
![Architecture](https://img.shields.io/badge/architecture-MVI%20%2B%20modular-152438)

</div>

---

## ✨ Features

| | |
|---|---|
| 🎬 **Video player** | Swipe to seek, brightness and volume gestures, pinch zoom, double-tap skip, long-press fast-forward |
| 🎵 **Music player** | Tracks, albums, artists, folders and playlists, mini player, Now Playing, background playback |
| 📝 **Subtitles** | Embedded tracks, auto-loaded `.srt` / `.vtt`, sync and speed, fully styled text |
| 🎚️ **Audio** | Equalizer with presets, bass boost, reverb, volume boost to 200% |
| ⏱️ **Playback tools** | Speed 0.25×–4×, A-B repeat, sleep timer, bookmarks, resume where you stopped |
| 🖼️ **Display** | Zoom modes, aspect ratios, picture-in-picture, night mode, lock and kids lock |
| 🔒 **Private Folder** | PIN-protected storage hidden from other apps |
| 📂 **Library** | Folder and file views, sorting, grid / list, NEW and SUB tags, search, media manager |
| 🎨 **Themes** | Dark, light, AMOLED black or adaptive, with 19 accent colours |

## 📸 Screenshots

### Mobile

| Home | Music | Now Playing | Equalizer | Up next |
|:---:|:---:|:---:|:---:|:---:|
| <img src="docs/screenshots/mobile/home.png" width="180"> | <img src="docs/screenshots/mobile/music.png" width="180"> | <img src="docs/screenshots/mobile/now.png" width="180"> | <img src="docs/screenshots/mobile/eq.png" width="180"> | <img src="docs/screenshots/mobile/queue.png" width="180"> |

### Desktop

<img src="docs/screenshots/desktop/library.png" width="820">

## 🏗️ Architecture

Max Player is split into small Gradle modules. Every screen is its own feature with a **domain** module (MVI contract, ViewModel, Koin wiring — no Compose) and a **presentation** module (Compose UI only).

```text
MaxPlayer/
├── androidApp/  iosApp/  desktopApp/  webApp/    platform entry points
├── shared/                 app root: navigation graph + Koin modules
│
├── kmpcore/                ⟶ submodule: MVI, navigation, DI, utils, storage
├── kmediaplayer/           ⟶ submodule: reusable media player library
│   ├── core/               engine (Media3), background service, settings, PlayerHost
│   └── ui/                 video player, panels, mini player, Now Playing
│
├── appcore/                theme, colour tokens, shared components
│   ├── appres/             icons, fonts, logo
│   └── sharedlogic/        media library, user data, private folder, file actions
│
└── appfeatures/            one module per screen → domain + presentation
    ├── home        ├── folder       ├── search      ├── music
    ├── player      ├── settings     ├── profile     ├── theme
    ├── playlists   ├── privatefolder├── mediamanager├── help
    └── localnetwork
```

```mermaid
flowchart LR
    App[androidApp / desktop / iOS / web] --> Shared[shared<br/>navigation + DI]
    Shared --> Features[appfeatures<br/>domain + presentation]
    Features --> AppCore[appcore<br/>theme · components · sharedlogic]
    AppCore --> KMP[kmediaplayer<br/>core + ui]
    AppCore --> Core[kmpcore<br/>mvi · navigation · di · utils]
```

**Flow of a screen:** the Compose `Screen` collects `State` from its `ViewModel` and sends `Event`s; the ViewModel reads repositories from `sharedlogic` and emits one-off `Effect`s. The player library knows nothing about Max Player — the app plugs in through `PlayerHost`.

## 🚀 Getting started

```bash
git clone --recurse-submodules https://github.com/sylar-apex/MaxPlayer.git
cd MaxPlayer
```

Already cloned without submodules?

```bash
git submodule update --init --recursive
```

| Target | Command |
|---|---|
| Android | `./gradlew :androidApp:assembleDebug` |
| Desktop | `./gradlew :desktopApp:run` |
| Web (Wasm) | `./gradlew :webApp:wasmJsBrowserDevelopmentRun` |
| iOS | open `iosApp/` in Xcode and run |

> Building next to an existing install signed with another key? Add `-PdevInstall` to install as `org.app.maxplayerpro.dev`.

## 📱 Platform status

| Platform | Library & UI | Playback |
|---|---|---|
| Android | ✅ full | ✅ Media3 ExoPlayer |
| Desktop | ✅ | 🚧 engine planned |
| iOS | ✅ | 🚧 AVPlayer engine planned |
| Web | ✅ | 🚧 HTML video engine planned |

## 🧩 Built with

[Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html) ·
[Compose Multiplatform](https://www.jetbrains.com/compose-multiplatform/) ·
[Media3 ExoPlayer](https://developer.android.com/media/media3) ·
[Koin](https://insert-koin.io) ·
[Navigation Compose](https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-navigation.html) ·
[kmpcore](https://github.com/sylar-apex/kmpcore) ·
[KMediaPlayer](https://github.com/sylar-apex/kmediaplayer)

---

<div align="center">Made with ❤️ by <a href="https://github.com/sylar-apex">sylar-apex</a></div>
