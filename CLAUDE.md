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

# Run all unit tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.osudroid.beatmaps.BeatmapTest"

# Run Android Lint
./gradlew lint
```

Requires **Java 17**, **Android NDK 22.1.7171670** and the **android-36.1** SDK platform (`compileSdk`, `targetSdk` stays 29). The main app depends on Verktex, the engine, which is meant to become its own project (see `docs/extraction.md`). Verktex's modules are:

- `:verktex:core`: everything platform independent. Kotlin Multiplatform with only the JVM as a target for now. Its code lives in `src/commonMain/`, must not depend on Android or the JVM, and is checked with `./gradlew -Pverktex.checkCommon :verktex:core:compileKotlinJs`.
- `:verktex:android`: the Android platform, with its graphics backends, the BASS audio library (`com.un4seen.bass` and its natives), the built-in shaders and fonts, and the task that compiles the shaders to SPIR-V. The app depends only on this module, which brings in core.
- `:verktex:shadertools`: the shader translator, used by the Vulkan backend and by the build.
- `:verktex:desktop`: the desktop platform (OpenGL 3.3 through LWJGL, BASS through JNA), which runs Verktex's UI tests, see `docs/desktop-platform.md`. The app never depends on it.

Engine tests live in `verktex/core/src/jvmTest/` and in the other modules' `src/test/`, and run with `./gradlew :verktex:core:jvmTest :verktex:android:testDebugUnitTest :verktex:shadertools:test :verktex:desktop:test`. The app's tests live under `tests/test/kotlin/` (not the standard `src/test/`).

The game is split in three modules, see `docs/multiplatform-game.md`:

- `:game` (`game/`): the game itself, Kotlin Multiplatform with only the JVM as a target for now. What doesn't depend on the JVM is in `src/commonMain/` and is checked with `./gradlew -Pverktex.checkCommon -Pgame.checkCommon :game:compileKotlinJs`. What still does (the scenes, `GameScene`, `GlobalManager`, `LibraryManager`, most of the UI) is in `src/jvmMain/` until it's rewritten as common code. What it needs from the platform goes through Verktex's `Platform` (clipboard, haptics, file picking, refresh rate…), and what only the game's hosts provide through `OsuGameHost` (`OsuGameHost.current`), implemented by `MainActivity` (over `AndroidOsuGameHost`), `OsuDesktop` and the UI test browsers; its optional features (the beatmap listing, videos, updates…) default to "not available". Its tests are in `src/jvmTest/` and run with `./gradlew :game:jvmTest`.
- The root module (`src/`): the Android app, a shell around `:game`: `MainActivity`, the services, Firebase, the Android database, the platform implementations and the features only Android has so far (the beatmap listing, the block area editor, the old fragments and dialogs).
- `:game-desktop`: the desktop launcher, over `:verktex:desktop`. Run it with `./gradlew :game-desktop:run`; `--args="--screenshot build/game.png --delay 10"` saves a frame after the delay and exits. It has no beatmap listing or videos yet (multiplayer connects but is untried), and its database is recreated instead of migrated.

## Architecture Overview

### Package Structure

The codebase is split across multiple main package roots, each in `game/src/{commonMain,jvmMain}/kotlin/` and, for its Android parts, in `src/`:

- **`ru/nsu/ccfit/zuev/osu/`** — Legacy code, mostly converted to Kotlin: `GameScene`, `ScoringScene`, `GlobalManager` and `LibraryManager` are in `:game`, `MainActivity` (still Java) in the app. Most subsystems here are singletons.
- **`com/acivev/`** — Vibration and haptic feedback management (`VibratorManager`, `VibratorCheckUtils`, etc.) as well as snow effects (`ui/Effect.kt`).
- **`com/edlplan/andengine/`** — `TriangleBuilder`, used to build slider body geometry.
- **`com/edlplan/framework/`** — Math, easing and utility classes.
- **`legacy/storyboard/`** — The storyboard renderer, outside the source set until it's rewritten on Verktex.
- **`com/edlplan/osu/`** — Slider body rendering (`AbstractSliderBody`, `DrawLinePath`, `SliderBody`, etc.) and some beatmap parsing.
- **`com/edlplan/replay/`** — Replay imports and exports.
- **`com/edlplan/ui/`** — Fragments and utilities to manage their lifecycle (`ActivityOverlay`, `BaseFragment`, `BeatmapPropertiesFragment`, etc.).
- **`com/osudroid/`** — Modern Kotlin code. Contains beatmap parsing, difficulty calculation, the mod system, multiplayer, Room database, and newer UI scenes.
- **`verktex/`** — The engine. `core/` holds rendering, audio, input, clocks (`time/`) and the UI components (`UIComponent`, `UIContainer`, `UIScene`, `UISprite`, etc.). `android/` holds the Android implementation (`com/reco1l/verktex/android/`). The built-in shaders (`shaders/`) and fonts (`fonts/`) are in `core/assets/`, which each platform packages.
- **`com/reco1l/framework/`** — Kotlin extensions for some Android classes and existing classes, such as bitmaps and easing. 
- **`com/reco1l/osu/ui`** — New UI components, primarily dialogs and settings preferences.
- **`com/rian/framework/`** — A couple of utility classes such as `RollingNumber`.
- **`tests/test/kotlin/`** — Unit tests for beatmap parsing, difficulty calculation, and other core logic.
- **`tests/test/resources/`** — Resources for unit tests, such as beatmap and storyboard files.

### Game Loop

`MainActivity` extends Verktex's `AndroidGameHost`. Scenes are kept in a stack through `Verktex.pushScene` and `Verktex.popScene`. The scene flow for singleplayer is:

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

Verktex handles all rendering. UI is a tree of `UIComponent`s measured and laid out through `LayoutConstraints`, animated with transforms for UI, and with `Choreography` for anything on the gameplay clock, whose state depends only on the time so that seeking works.

A Vulkan backend roadmap lives in `docs/vulkan-backend.md`. How the frame rate is capped on each platform, and the pending work on it, is in `docs/frame-rate.md`.

### UI Tests

Verktex's abstract `UITestActivity` (`verktex/.../android/`) and `UITestApplication` (`verktex/.../desktop/testing/`) browse `UITestSuite`s (`verktex/.../testing/`) to try components in isolation; `VerktexUITests` covers the engine components. The game's suites (`OsuUITests`, `OsuSceneUITests`, `OsuGameplayUITests`) are in `:game` (`com/osudroid/ui/v2/test/`), with `TestGameContext` standing in for the game. Each browser has a run configuration:

- **Verktex: UI Tests (Android)**: the app's `VerktexUITestActivity`, Verktex's suites only.
- **Verktex: UI Tests (Desktop)**: `./gradlew :verktex:desktop:run`.
- **osu!droid: UI Tests (Android)**: the app's `OsuUITestActivity`, every suite.
- **osu!droid: UI Tests (Desktop)**: `./gradlew :game-desktop:runUITests`, every suite.

The Android browsers are enabled only in debug builds and run in their own processes. Open a test directly with:

```bash
adb shell am start -S -n ru.nsu.ccfit.zuev.osuplus.debug/com.osudroid.ui.v2.test.OsuUITestActivity --es test "'osu!/Hit circles'"
```

Add `--es backend <name>` (a `GraphicsBackend`: `OpenGLES3`, `OpenGLES2`, `Vulkan`) to open it with another graphics backend.

On desktop, `--test` opens a test and `--screenshot <file.png>` saves a frame and exits:

```bash
./gradlew :verktex:desktop:run --args="--test 'Verktex/Text' --screenshot build/text.png"
./gradlew :game-desktop:runUITests --args="--test 'osu!/Hit circles' --screenshot build/hit-circles.png"
```

The game itself runs with **osu!droid: Game (Android)** and **osu!droid: Game (Desktop)** (`./gradlew :game-desktop:run`).

### Asset / Resource Loading

`SkinTextures` and `SkinSounds` look up skin assets, preferring the beatmap skin, then the custom skin folder, then the default skin. `Skin` loads the configuration of a skin folder. Other textures and sounds go through Verktex's `Textures` and `Sounds`.

Text goes through Verktex's `Localization` (`verktex/core/.../localization/`). `:game:generateStringResources` turns the language pack into `strings/<locale>.json` assets and the generated `Res`, whose `Res.string.*` are `LocalizedString`s: a `CharSequence` that components such as `UIText` show in the current language and re-resolve when it changes. Format one with `Res.string.x.format(args)`, and use `toString()` where a `String` is needed.

### Audio

BASS native library, bundled in `:verktex:android` and reached through Verktex's audio API. `MusicService` (`com/osudroid/audio/`, reached through `GlobalManager.music`) plays songs and knows their beatmap, and `BassSoundProvider` plays sound effects.

### Beatmaps & Difficulty

Beatmap files are parsed in `com/osudroid/beatmaps/`. Difficulty calculation (star rating and PP) lives in `com/osudroid/difficulty/` and implements both the osu!droid and osu!standard algorithms. `BeatmapDifficultyCalculator` is the main entry point.

### Mod System

Mods are defined in `com/osudroid/mods/`. Each mod implements modifier interfaces and can alter difficulty attributes, hit windows, and gameplay behavior. `ModCombination` aggregates active mods.

### Data Persistence

Room database accessed via `DatabaseManager`. DAOs and entities are under `com/osudroid/data/`. Schemas are versioned in `schemas/`.

Settings go through Verktex's `Settings` (`verktex/core/.../settings/`), a typed store saved as `settings.json` in the data directory. Each setting is a `SettingKey` (`setting(name, default)`), read with `Settings[key]` and written with `Settings[key] = value`. The engine's are in `VerktexSettings`, the game's in `OsuSettings` (`com/osudroid/settings/`) under the names the old preferences had, with helpers for the ones read in other units (e.g. `OsuSettings.musicVolume`). The folders are in `GameFolders` and `SkinFolders`, and the viewport size in `GameResolution`.

### Multiplayer

Socket.IO for real-time room events, through `KtorRoomSocket`: a Socket.IO v4 client over Ktor WebSockets, in common code (`KtorRoomSocketTest` runs against a server given with `-PsocketIoTestUrl`). HTTP requests go through Ktor: Verktex's `Network` creates clients over the platform's engine (`Platform.Implementation.httpEngine`, OkHttp on Android and desktop), and the game's `httpClient` (`com/osudroid/online/HttpClient.kt`) is one over it with content negotiation: requests use Ktor directly (`httpClient.get(url) { parameter(...) }.body<T>()`) with `@Serializable` responses read leniently whatever their content type, and unsuccessful responses throw. `downloadTo` streams a download with progress. They're called from coroutines such as `async {}` blocks. Firebase Messaging handles push notifications. Multiplayer logic lives in `com/osudroid/multiplayer/`.

## Key Conventions

- New gameplay/beatmap/difficulty code goes in `com/osudroid/` (Kotlin).
- Generic UI components belong in `verktex/core/`, and game-specific ones in `com/osudroid/ui/v2/` (Kotlin).
- Verktex never depends on the game, and `:verktex:core` never depends on Android or the JVM (including `org.json`, which ships with the Android SDK). What it needs from a platform goes through `Platform.Implementation`, everything else through multiplatform libraries; see `docs/extraction.md`.
- Legacy Java files in `ru/nsu/ccfit/zuev/osu/` are modified in-place; avoid moving them.
- Game code goes in `:game`, never depending on Android. What any platform could do goes through Verktex's `Platform`; what's specific to the game's hosts through `OsuGameHost`, with a default for hosts that don't have it.

@CLAUDE.local.md