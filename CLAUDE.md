# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Assemble debug APK
./gradlew assembleDebug

# Assemble pre-release APK
./gradlew assemblePre_release

# Assemble release APK
./gradlew assembleRelease

# Run the game's tests
./gradlew :shared:desktopTest

# Run a single test class
./gradlew :shared:desktopTest --tests "com.osudroid.beatmaps.BeatmapTest"

# Run Android Lint
./gradlew lint
```

Requires **Java 17**, **Android NDK 22.1.7171670** and the **android-36.1** SDK platform (`compileSdk`, `targetSdk` stays 29). The main app depends on Verktex, the engine, which is its own repository (https://github.com/Reco1I/verktex), checked out as a git submodule in `verktex/` and included with `includeBuild('verktex')` (see `verktex/docs/extraction.md` and `verktex/CLAUDE.md`). Clone with `--recurse-submodules`, or run `git submodule update --init` after cloning. A change to the engine is committed and pushed in `verktex/` first, and then the game commits the submodule's new commit. The game depends on it as `com.reco1l.verktex:shared`, and its tasks run from here as `:verktex:<project>:<task>` (or as `:<project>:<task>` with `./gradlew -p verktex`). It needs its own `verktex/local.properties`, which `settings.gradle` copies from the game's when it's missing. Verktex's modules are:

- `:verktex:shared` (`verktex/shared/`): the engine, one Kotlin Multiplatform module with an `android` and a `desktop` target. `src/commonMain/` is everything platform independent and must not depend on Android or the JVM. `src/jvmMain/` is what both platforms share because they run on a JVM. `src/androidMain/` is the Android platform, with its graphics backends and the BASS audio library (`com.un4seen.bass` and its natives), and `src/desktopMain/` the desktop one (OpenGL 3.3 through LWJGL, BASS through JNA), see `verktex/docs/desktop-platform.md`. The built-in shaders and fonts are in `assets/`.
- `:verktex:androidApp` and `:verktex:desktopApp`: Verktex's own applications, the browsers of its UI tests alone (`./gradlew :verktex:desktopApp:run`, `:verktex:androidApp:installDebug`). The game never depends on them.
- `:verktex:android-natives`: what the Android platform needs from the NDK: the native library of the Vulkan backend, and the tasks that compile the shaders to SPIR-V and package the validation layer.
- `:verktex:shadertools`: the shader translator, used by the Vulkan backend and by the build.

Engine tests live in `verktex/shared/src/desktopTest/` (the common and desktop ones) and `src/androidHostTest/` (Robolectric), and run with `./gradlew :verktex:shared:desktopTest :verktex:shared:testAndroidHostTest :verktex:shadertools:test`.

The game follows the structure Kotlin recommends for multiplatform projects (https://kotlinlang.org/docs/multiplatform/multiplatform-project-recommended-structure.html): a shared module and one module per application with its entry points, see `docs/multiplatform-game.md`:

- `:shared` (`shared/`): the game itself, Kotlin Multiplatform with the targets of Verktex, `android` and `desktop`. Everything but its Android side is common code, in `src/commonMain/`, which can't depend on Android or the JVM: both targets are JVMs, so the compiler doesn't stop it, and `./gradlew :shared:checkCommonSources` (part of `check` and `desktopTest`) fails on Java imports and the stdlib members only the JVM has. `src/androidMain/` is the Android side of the game that isn't an entry point: `AndroidOsuGameHost`, Firebase, the platform implementations and the features only Android has so far (updates, the accessibility check), with its resources (`res/`: the launcher and notification icons and the activities' theme). The game has no Android UI: everything is drawn with Verktex, and Android dialogs are only used where the engine can't be, such as `AppException`'s crash report. The beatmap listing and the block area editor were Android views and were removed until they're rewritten on Verktex; their non-UI parts (`BeatmapMirror` and the mirror models) are common code. What the game needs from the platform goes through Verktex's `Platform` (clipboard, haptics, file picking, refresh rate…), and what only its hosts provide through `OsuGameHost` (`OsuGameHost.current`), implemented by `AndroidOsuGameHost` (which `MainActivity` extends), `OsuDesktop` and the UI test browsers; its optional features (the beatmap listing, updates…) default to "not available". Videos are Verktex's `UIVideo`, over the platform's decoder (ExoPlayer on Android, FFmpeg on desktop). Its assets (the default skin, sounds, fonts) are in `shared/assets/`. Its tests are in `src/desktopTest/` (`./gradlew :shared:desktopTest`).
- `:androidApp` (`androidApp/`): the Android application. It has the entry points (`MainActivity`, the application class `OsuApplication`, `PermissionActivity`, `OsuUITestActivity`, `PushNotificationService`, `AppException`), the manifest, and what an application decides: its id, version, signing (`debug.jks`), build types (`debug`, `release`, `pre_release`, `profile`) and the Firebase plugins (`google-services.json`). `:shared` is a library with a single variant and no `BuildConfig`, so `OsuApplication` passes the application's to `AppBuild` (`com/osudroid/android/`, in `:shared`). APKs are built in `androidApp/build/outputs/apk/`.
- `:desktopApp` (`desktopApp/`): the desktop application: `OsuDesktop` and the UI test browser, over the desktop target of `:shared` and of Verktex. Run it with `./gradlew :desktopApp:run`; `--args="--screenshot build/game.png --delay 10"` saves a frame after the delay and exits, and `--quickplay '<beatmap words>' [--no-autoplay]` plays a beatmap once the game starts (`QuickPlay`, debug builds; on Android, `--es quickplay` on `MainActivity`). Its multiplayer connects but is untried, it plays videos with FFmpeg libraries `:verktex:shared:prepareFFmpeg` downloads (see `verktex/docs/desktop-platform.md`), and its database, as Android's, is recreated when its version changes.

## Architecture Overview

### Package Structure

The codebase is split across multiple main package roots, each in `shared/src/commonMain/kotlin/` and, for its Android parts, in `shared/src/androidMain/{kotlin,java}/` and `androidApp/src/main/{kotlin,java}/`:

- **`ru/nsu/ccfit/zuev/osu/`** — Legacy code, mostly converted to Kotlin: `GameScene`, `ScoringScene` and `LibraryManager` are in `:shared`, `MainActivity` (Java, only a launcher) in `:androidApp`. Most subsystems here are singletons.
- **`com/acivev/`** — Vibration and haptic feedback management (`VibratorManager`, `VibratorCheckUtils`, etc.) as well as snow effects (`ui/Effect.kt`).
- **`legacy/storyboard/`** — The storyboard renderer, outside the source set until it's rewritten on Verktex.
- **`com/osudroid/replay/`** — Replay imports (`ReplayImporter`, from the settings' file picker). Replays themselves are JSON (`ru/nsu/ccfit/zuev/osu/scoring/ReplayData.kt`, version 10); the Java-serialized ones before them aren't read. Opening a replay from another app on Android plays it (`AndroidOsuGameHost`).
- **`com/osudroid/`** — Modern Kotlin code. Contains beatmap parsing, difficulty calculation, the mod system, multiplayer, Room database, and newer UI scenes.
- **`verktex/`** — The engine. `verktex/src/commonMain/` holds rendering, audio, input, clocks (`time/`) and the UI components (`UIComponent`, `UIContainer`, `UIScene`, `UISprite`, etc.). `src/androidMain/` holds the Android platform (`com/reco1l/verktex/android/`) and `src/desktopMain/` the desktop one. The built-in shaders (`shaders/`) and fonts (`fonts/`) are in `verktex/assets/`, which each platform packages.
- **`com/reco1l/framework/`** — Kotlin extensions of existing classes, such as the tolerant iteration of lists.
- **`com/rian/framework/`** — A couple of utility classes such as `RollingNumber`.
- **`shared/src/desktopTest/`** — Unit tests for beatmap parsing, difficulty calculation, and other core logic, with their resources (beatmap and storyboard files).

### Game Loop

`OsuGame.start` starts the game on every host: the hosts (`AndroidOsuGameHost`, which extends Verktex's `AndroidGameHost` and which `MainActivity` only launches, and `OsuDesktop`) call it once the engine is initialized and give it their platform parts through `OsuGameHost` (`loadBeatmapLibrary`, `onGameStarted`). Scenes are kept in a stack through `Verktex.pushScene` and `Verktex.popScene`, and `Verktex.resetScenes` replaces the whole stack (the hosts do it with the main menu once the splash ends). The scene flow for singleplayer is:

```
SplashScene → LoadingScreen → MainScene (menu hub)
                                  ↓
                              SongMenu → GameLoaderScene → GameScene (gameplay) → ScoringScene (results)
```

For multiplayer, the flow is:

```
SplashScene → LoadingScreen → MainScene (menu hub) → LobbyScene
                                                         ↓
                                                     RoomScene → GameLoaderScene → GameScene (gameplay) → ScoringScene (results)
```

`GameScene` drives the main gameplay loop: hit object spawning, input polling, scoring, and replay recording. It is the central class for gameplay logic.

### Rendering

Verktex handles all rendering. UI is a tree of `UIComponent`s measured and laid out through `LayoutConstraints`, animated with transforms for UI, and with `Choreography` for anything on the gameplay clock, whose state depends only on the time so that seeking works. Slider bodies are Verktex `UIPath`s, a `Path` stroked from a mesh kept on the GPU, in osu!pixels inside the playfield.

A Vulkan backend roadmap lives in `verktex/docs/vulkan-backend.md`. How the frame rate is capped on each platform, and the pending work on it, is in `verktex/docs/frame-rate.md`. Textures and shader outputs are premultiplied, and the atlas keeps a gutter around each region; how, and what's left, is in `verktex/docs/texture-blending.md`.

### UI Tests

Verktex's abstract `UITestActivity` (`verktex/.../android/`) and `UITestApplication` (`verktex/.../desktop/testing/`) browse `UITestSuite`s (`verktex/.../testing/`) to try components in isolation; `VerktexUITests` covers the engine components, and the engine's repository has the run configuration of its own desktop browser (`./gradlew :verktex:desktopApp:run` from here). The game's suites (`OsuUITests`, `OsuSceneUITests`, `OsuGameplayUITests`) are in `:shared` (`com/osudroid/ui/v2/test/`), with `TestGameContext` standing in for the game. Each browser has a run configuration:

- **osu!droid: UI Tests (Android)**: `:androidApp`'s `OsuUITestActivity`, every suite, Verktex's included.
- **osu!droid: UI Tests (Desktop)**: `./gradlew :desktopApp:runUITests`, every suite, Verktex's included.

The Android browser is enabled only in debug and profile builds and runs in its own process. Open a test directly with:

```bash
adb shell am start -S -n ru.nsu.ccfit.zuev.osuplus.debug/com.osudroid.ui.v2.test.OsuUITestActivity --es test "'osu!/Hit circles'"
```

Add `--es backend <name>` (a `GraphicsBackend`: `OpenGLES3`, `OpenGLES2`, `Vulkan`) to open it with another graphics backend.

On desktop, `--test` opens a test and `--screenshot <file.png>` saves a frame and exits. Relative paths resolve against the root of the build the task belongs to, so Verktex's browser saves under `verktex/`:

```bash
./gradlew :verktex:desktopApp:run --args="--test 'Verktex/Text' --screenshot build/text.png"
./gradlew :desktopApp:runUITests --args="--test 'osu!/Hit circles' --screenshot build/hit-circles.png"
```

The game itself runs with **osu!droid: Game (Android)** and **osu!droid: Game (Desktop)** (`./gradlew :desktopApp:run`).

### Asset / Resource Loading

`SkinTextures` and `SkinSounds` look up skin assets, preferring the beatmap skin, then the custom skin folder, then the default skin. `Skin` loads the configuration of a skin folder. Other textures and sounds go through Verktex's `Textures` and `Sounds`. The game's own assets (the default skin, sounds, fonts) are in `shared/assets/`, which Android packages as assets and desktop loads from the classpath.

Text goes through Verktex's `Localization` (`verktex/shared/.../localization/`). `:shared:generateStringResources` turns the language pack into `strings/<locale>.json` assets and the generated `Res`, whose `Res.string.*` are `LocalizedString`s: a `CharSequence` that components such as `UIText` show in the current language and re-resolve when it changes. Format one with `Res.string.x.format(args)`, and use `toString()` where a `String` is needed.

### Audio

BASS native library, bundled in Verktex's Android target and reached through its audio API. `MusicService` (`com/osudroid/audio/`, reached through `OsuGame.music`) plays songs and knows their beatmap, and `BassSoundProvider` plays sound effects.

### Beatmaps & Difficulty

Beatmap files are parsed in `com/osudroid/beatmaps/`. `BeatmapDownloader` (`com/osudroid/beatmaplisting/`) downloads a set from the selected mirror into the cache directory and imports it. Beatmap set (`.osz`) and skin (`.osk`) archives are extracted by `LibraryImport` (Okio's `openZip`), from the game's folders, the device's downloads folder (Verktex's `PlatformStorage.downloadsDirectory`) or a file the game was opened with, on both platforms; `LibraryManager` then scans the beatmaps folder into the database. Difficulty calculation (star rating and PP) lives in `com/osudroid/difficulty/` and implements both the osu!droid and osu!standard algorithms. `BeatmapDifficultyCalculator` is the main entry point.

### Mod System

Mods are defined in `com/osudroid/mods/`. Each mod implements modifier interfaces and can alter difficulty attributes, hit windows, and gameplay behavior. `ModCombination` aggregates active mods.

### Data Persistence

Room database accessed via `DatabaseManager`. The database (`OsuDatabase`), its DAOs and entities are common code under `com/osudroid/data/`, generated by Room's KSP for each target. Verktex provides Room's runtime and opens databases over the bundled SQLite (`Databases`, `verktex/shared/.../database/`), and the game opens its own in common code (`DatabaseManager.open`, or `openInMemory` for tests and the UI test browsers); Room's compiler in `shared/build.gradle.kts` must be of the version of Verktex's runtime. DAO functions suspend, as Room only implements those outside Android: call them from coroutines (`async {}`, then `updateThread {}` for the UI). This project is separate from upstream osu!droid: there are no migrations, and a database of another version (`DATABASE_VERSION`) is recreated. Nothing is imported from upstream's data either (its old database, its preferences).

Settings go through Verktex's `Settings` (`verktex/shared/.../settings/`), a typed store saved as `settings.json` in the data directory. Each setting is a `SettingKey` (`setting(name, default)`), read with `Settings[key]` and written with `Settings[key] = value`. The engine's are in `VerktexSettings`, the game's in `OsuSettings` (`com/osudroid/settings/`), with helpers for the ones read in other units (e.g. `OsuSettings.musicVolume`). The folders are in `GameFolders` and `SkinFolders`, and the viewport size in `GameResolution`.

### Multiplayer

Socket.IO for real-time room events, through `KtorRoomSocket`: a Socket.IO v4 client over Ktor WebSockets, in common code (`KtorRoomSocketTest` runs against a server given with `-PsocketIoTestUrl`). HTTP requests go through Ktor: Verktex's `Network` creates clients over Ktor's default engine (found on the classpath: OkHttp, brought by the platform modules), and the game's `httpClient` (`com/osudroid/online/HttpClient.kt`) is one over it with content negotiation: requests use Ktor directly (`httpClient.get(url) { parameter(...) }.body<T>()`) with `@Serializable` responses read leniently whatever their content type, and unsuccessful responses throw. `downloadTo` streams a download with progress. They're called from coroutines such as `async {}` blocks. Firebase Messaging handles push notifications. Multiplayer logic lives in `com/osudroid/multiplayer/`.

## Key Conventions

- New gameplay/beatmap/difficulty code goes in `com/osudroid/` (Kotlin).
- Generic UI components belong in Verktex's `commonMain`, and game-specific ones in `com/osudroid/ui/v2/` (Kotlin).
- Each scene handles the back key in its `onInputEvent`, once its components didn't consume the event (`event is KeyboardEvent && event.isBackPress`: Android's back button or Escape). On Android a back press no scene consumes finishes the activity.
- Verktex never depends on the game, and its `commonMain` never depends on Android or the JVM (including `org.json`, which ships with the Android SDK). What it needs from a platform is expected there and actual in each target's source set, as `Platform` is (an `expect object`, attached to its host by `AndroidGameHost` and `DesktopGameHost`); interfaces are kept for what has several implementations on one platform (`GraphicsRenderer`, `AudioDriver`, `Logger.Implementation`). Everything else comes from multiplatform libraries; see `verktex/docs/extraction.md`. It is a separate repository: its build never refers to the game's, its versions are in `verktex/gradle/libs.versions.toml`, and nothing in it may read the game's files.
- Legacy Java files in `ru/nsu/ccfit/zuev/osu/` are modified in-place; avoid moving them. They're in `shared/src/androidMain/java/` and `androidApp/src/main/java/`, apart from the Kotlin sources in `kotlin/`.
- `:shared` is a library with a single variant: it has no `BuildConfig`. What depends on the application (its id, its build type, whether it's a debug build) comes from `AppBuild`, and what depends on a build type is declared by `:androidApp`.
- Entry points go in the application modules, and nothing in `:shared` names them: it reaches the Android activity through `AndroidOsuGameHost` (`AndroidGame.mainActivity`).
- Game code goes in `:shared`'s `commonMain`, never depending on Android or the JVM: files through Okio (`Platform.fileSystem`), dates through kotlinx-datetime, numbers through `formatString`/`formatNumber` (not `String.format`), time through `kotlin.time` (`currentTimeMillis()` for the wall clock), locks and atomics through atomicfu and `kotlin.concurrent.atomics`, background work through coroutines. What any platform could do goes through Verktex's `Platform`; what's specific to the game's hosts through `OsuGameHost`, with a default for hosts that don't have it.

@CLAUDE.local.md