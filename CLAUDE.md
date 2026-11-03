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

Requires **Java 17** and **Android NDK 22.1.7171670**. The project has three Gradle modules: the main app, `:verktex` (the engine), and `:LibBASS` (native audio). Engine tests live under `verktex/src/test/`, run with `./gradlew :verktex:testDebugUnitTest`. Tests live under `tests/test/kotlin/` (not the standard `src/test/`).

## Architecture Overview

### Package Structure

The codebase is split across multiple main package roots:

- **`ru/nsu/ccfit/zuev/osu/`** — Legacy Java code. Contains the original `MainActivity`, `GameScene`, `MainScene`, `ResourceManager`, and `Config`. Most subsystems here are singletons.
- **`com/acivev/`** — Vibration and haptic feedback management (`VibratorManager`, `VibratorCheckUtils`, etc.) as well as snow effects (`ui/Effect.kt`).
- **`com/edlplan/andengine/`** — `TriangleBuilder`, used to build slider body geometry.
- **`com/edlplan/framework/`** — Math, easing and utility classes.
- **`legacy/storyboard/`** — The storyboard renderer, outside the source set until it's rewritten on Verktex.
- **`com/edlplan/osu/`** — Slider body rendering (`AbstractSliderBody`, `DrawLinePath`, `SliderBody`, etc.) and some beatmap parsing.
- **`com/edlplan/replay/`** — Replay imports and exports.
- **`com/edlplan/ui/`** — Fragments and utilities to manage their lifecycle (`ActivityOverlay`, `BaseFragment`, `BeatmapPropertiesFragment`, etc.).
- **`com/osudroid/`** — Modern Kotlin code. Contains beatmap parsing, difficulty calculation, the mod system, multiplayer, Room database, and newer UI scenes.
- **`verktex/`** — The engine: rendering, audio, input, clocks (`time/`), and the UI components (`UIComponent`, `UIContainer`, `UIScene`, `UISprite`, etc.).
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

### Asset / Resource Loading

`SkinTextures` and `SkinSounds` look up skin assets, preferring the beatmap skin, then the custom skin folder, then the default skin. `Skin` loads the configuration of a skin folder. Other textures and sounds go through Verktex's `Textures` and `Sounds`.

### Audio

BASS native library, wrapped in `LibBASS` and reached through Verktex's audio API. `SongService` plays songs, and `BassSoundProvider` plays sound effects.

### Beatmaps & Difficulty

Beatmap files are parsed in `com/osudroid/beatmaps/`. Difficulty calculation (star rating and PP) lives in `com/osudroid/difficulty/` and implements both the osu!droid and osu!standard algorithms. `BeatmapDifficultyCalculator` is the main entry point.

### Mod System

Mods are defined in `com/osudroid/mods/`. Each mod implements modifier interfaces and can alter difficulty attributes, hit windows, and gameplay behavior. `ModCombination` aggregates active mods.

### Data Persistence

Room database accessed via `DatabaseManager`. DAOs and entities are under `com/osudroid/data/`. Schemas are versioned in `schemas/`. `Config` (SharedPreferences) stores user settings.

### Multiplayer

Socket.IO for real-time lobby/room events. REST calls use OkHttp3. Firebase Messaging handles push notifications. Multiplayer logic lives in `com/osudroid/multiplayer/`.

## Key Conventions

- New gameplay/beatmap/difficulty code goes in `com/osudroid/` (Kotlin).
- Generic UI components belong in `verktex/`, and game-specific ones in `com/osudroid/ui/v2/` (Kotlin).
- Legacy Java files in `ru/nsu/ccfit/zuev/osu/` are modified in-place; avoid moving them.

@CLAUDE.local.md