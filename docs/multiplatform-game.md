# Making the osu!droid game code platform agnostic

## Context

Verktex (the engine) is already multiplatform: `:verktex:core` is common Kotlin, and Android and
desktop are just implementations of `Platform.Implementation`. The game in the root module
(`src/`, about 394 Kotlin and 68 Java files) still only runs on Android. 98 files import `android.*`,
57 import `androidx.*`, 31 use `org.json`, 45 use `R.*` resources, and the gameplay core
(`GameScene`, `Config`, `GameplaySlider`, `StatisticV2`, `Replay`…) is Java, which common Kotlin
can't hold.

The goal: move the game into a Kotlin Multiplatform module whose `commonMain` has no Android or JVM
code, the same rule Verktex follows. The Android app then becomes a thin shell, and a desktop
launcher becomes possible.

Decisions already made with the user:
- **Java files are converted to Kotlin**, keeping them as close to the original as possible: same
  structure, names, order and comments, translated line by line, with no refactors along the way.
  The CLAUDE.md rule "modify in place, avoid moving" is updated to match.
- **Android View UI is rewritten on Verktex** where nothing exists yet. Dialogs map to Verktex's
  existing `UIDialog`/`UIMessageDialog`. The WebView is replaced by a multiplatform "open web page"
  function: an in-app web view on Android, the system browser on desktop.

## Inventory: what needs replacing, and with what

### Libraries to swap for multiplatform ones

| Current (Android/JVM) | Where | Replacement |
|---|---|---|
| `org.json` (31 files), `com/reco1l/framework/Json.kt` | skins, HUD, multiplayer, online, DB, mods | **kotlinx.serialization-json** (already a dependency): `@Serializable` models where the shape is fixed, `JsonObject`/`buildJsonObject` elsewhere. Rewrite the `Json.kt` helpers over `JsonElement`. |
| OkHttp (12 files), `WebRequest`/`JsonRequest`, `PostBuilder`/`FormDataPostBuilder`/`URLEncodedPostBuilder`, `OnlineFileOperator` | online, beatmap listing, mirrors | **Ktor client** (OkHttp engine on Android, Java/CIO on desktop). Keep the `WebRequest`/`JsonRequest` API so callers barely change. |
| `io.socket:socket.io-client` | `multiplayer/api/RoomAPI.kt`, `LobbyAPI`, `MockMultiplayer` | A small **Socket.IO v4 client over Ktor WebSockets**. Only connect with auth/query, `emit` and `on` are used, so it's a few hundred lines. |
| `java.io.File` (32 files), commons-io, `FileUtils.java`, `MD5Calculator.java`, `MessageDigest` | library, skins, replays, DB, downloads | **Okio** (`FileSystem.SYSTEM`, `Path`, `BufferedSource`, `ByteString.md5()`). It's a mature multiplatform FS and also covers hashing. |
| zip4j, `java.util.zip` | `OsuDroidReplayPack`, `.osz` import (`LibraryManager`, `BeatmapDownloader`) | A small `Archives` expect/actual in the game, with zip4j as the JVM actual. Okio reads zips but can't write them. |
| `SharedPreferences`/`PreferenceManager` (`Config.java`, `ConfigBackup`, `DifficultyCalculationManager`, `SettingsSections`…) | settings | **multiplatform-settings** (`com.russhwolf:multiplatform-settings`), reached through `OsuPlatform.preferences`: `SharedPreferencesSettings` over the same preferences on Android, so users lose nothing. Verktex's `Settings` is only an in-memory map without typed storage, so it doesn't fit. |
| Room 2.7 (kapt) + `SupportSQLiteDatabase` migrations | `com/osudroid/data/` | **Room KMP** (same 2.7+ artifacts) with **KSP** instead of kapt and `BundledSQLiteDriver`. Port `Migrations.kt` to `Migration.migrate(SQLiteConnection)`. The `getDatabaseBuilder()` stays expect/actual. |
| `android.util.Log` (33 files), `Debug.java` | everywhere | Verktex **`Logger`**. |
| `SystemClock`, `java.util.Date/Calendar/TimeZone`, `SimpleDateFormat`, `android.text.format`, `android.icu.text` | cursor, replays, scores, chat, logger | **`kotlin.time`** (`TimeSource.Monotonic`) and **kotlinx-datetime** with its format DSL. |
| `String.format`, `DecimalFormat`, `NumberFormat`, `BigDecimal`, `java.util.Formatter` | UI text, `StringTable.format` | Extend Verktex's `math/Formatting.kt` (`toFixed`) with grouping and padding, plus a small `%s/%d/%.2f/%1$s` formatter for the localized strings. |
| `@Synchronized` (36), `synchronized`, `ReentrantLock`, `Atomic*`, `ConcurrentHashMap`, `Executors`, `CompletableFuture`, `Handler/Looper` | `ExecutionOverloads.kt`, managers | **atomicfu** (locks, atomics, as Verktex does) and **kotlinx.coroutines**. `Job.stopAsync()` returns a `Deferred`. `mainThread {}` becomes the Verktex update thread once the Android Views are gone. |
| `kotlin-reflect` (`createInstance`, `primaryConstructor`, `::class.java`) | mod system (`Mods.kt`, `ModHashMap`, `APIMod`) | An explicit **mod registry** (`Map<KClass<out Mod>, () -> Mod>`, keyed by acronym too). Then drop kotlin-reflect. |
| EdlJavaExt (`com.edlplan.framework.math/easing/utils`: `Vec`, `FMath`, `LinePath`, `Easing`, `FloatArraySlice`…) | slider body, path math | Port the handful of used classes to Kotlin in the game, or swap them for Verktex `math/Easing.kt` and `Vector2`. Drop the jar. EdlOsbSupport isn't used by `src/`, so drop it. |
| `android.graphics.PointF` (21 files) | gameplay, HUD, replays | The existing `com.osudroid.math.Vector2` (remove its `PointF` constructor). |
| `android.graphics.Color`, `Bitmap`, `BitmapFactory` | `OsuColors`, `SkinConversion`, avatars, listing thumbnails | Verktex `Color4`, and Verktex texture sources built from bytes (add `Provider.createTextureSource(bytes)` if missing). |
| `TextUtils`, `androidx.annotation.*` (`@IntRange`, `@NonNull`, `@StringRes`…) | various | Kotlin stdlib, and nullability in the type system. |
| `MotionEvent` (`BlockArea.kt`, `IGameplayEvents.kt`), `KeyEvent` | input | Verktex input events. |

### Android resources and localization

`R.string` is used 338 times, `R.array` 16, `R.drawable` 16, plus `R.layout`/`R.id` in the View UI.
The strings come from the `com.github.osudroid:language-pack` AAR (`values-xx/strings.xml`) and
`res/values`.
- Add a Gradle task in the game module that reads the language pack's `strings.xml` files (and
  `res/values`), writes one file per locale into the game assets, and generates a Kotlin `Strings`
  object of typed keys (`Strings.menu_play`…), the way `R.string` works today.
- `StringTable` (converted to Kotlin) loads the current locale's file through Verktex's `Provider`
  and formats with the common formatter above. `StringResources.kt` (`UIText.setText(...)`) keeps
  its shape but takes a key instead of an `@StringRes Int`.
- `R.array` entries (`SettingsMenu`'s `@ArrayRes`) go through the same pipeline.
- Drawables used by Verktex move into `assets/`. The ones only used by Views, notifications or the
  launcher stay in the Android `res/`. `R.layout`/`R.id` disappear with the View rewrite.

### Platform services (expect/interfaces, implemented per platform)

Generic services go in **Verktex's `Platform.Implementation`**, because they aren't game specific
(CLAUDE.md: generic stuff goes in Verktex):
- `openWebPage(url)` replaces `WebViewFragment` (used in `PlayerButton`, `RoomPlayerCard`): an
  in-app WebView activity on Android, the system browser on desktop. `openUrl` covers
  `BannerManager`/`OnlineScoring` intents.
- Clipboard (`SettingsSections`), haptics (`VibratorManager`, `VibratorCheckUtils`), file picker
  (`ReplayImport`'s activity result), storage directories (`Environment`/`FileUtils`/`Config`
  paths), toasts (`ToastLogger`, 23 files: add a `UIToast` to Verktex), and video decoding
  (`UIVideo`, media3 ExoPlayer plus ffmpeg on Android).

Game-specific services go in a new **`OsuPlatform`** interface in the game's `commonMain`,
implemented by the Android app:
- crash reporting and analytics (Firebase Crashlytics/Analytics, `AppException`)
- push notifications (`PushNotificationService`, Firebase Messaging)
- the background song service (`SongService`, `SaveServiceObject`)
- integrity checks (`SecurityUtils`, `AccessibilityDetector`)
- app updates (`UpdateManager`, APK install)
- device performance (`DevicePerfomanceUtils`)
- imports from intents (`ImportReplayActivity`, `.osz` open)

### Stays Android-only (the app module)

`MainActivity` (extends `AndroidGameHost`), `PermissionActivity`, `ImportReplayActivity`, the
Android services, the Firebase setup, the `OsuPlatform` implementation, and the legacy DB migration
code (`helper/sql/DBOpenHelper`, `DatabaseContext`, and the `ObjectInputStream` cache import in
`Database.kt`), which moves to an Android migration step.
`AndroidManifest.xml`, `res/` (launcher, notifications), `google-services.json`, and the
dependencies easypermissions, appcompat, material and legacy-support also stay.

### View UI to rewrite on Verktex (in `com/osudroid/ui/v2/`)

| Android View | Verktex replacement |
|---|---|
| `MessageDialog` (11 usages), `PromptDialog` (4), `Dialog.kt`, `ListDialog` (unused) | `UIDialog`, `UIMessageDialog`, plus `UIDialog` + `UITextInput` for prompts. Delete `ListDialog`. |
| `BeatmapListing` (888 lines), `DownloadFragment`, `LoadingBadgeFragment` | A new beatmap listing scene with download progress |
| `CollectionsManagerFragment` | Done: `BeatmapCollectionsDialog` |
| `BlockAreaEditorFragment` | A block area editor scene/overlay |
| `LoadingFragment`, `ActivityOverlay`, `BaseFragment`, `BaseAnimationListener`, `EasingHelper`, `com/reco1l/framework/android/*` | Deleted once nothing uses them |
| `WebViewFragment` | `Platform.openWebPage` |
| `Preference.kt`, `EditableSeekBarPreference`, `SettingsFragment` | They look unused already (`SettingsMenu` is on Verktex). Confirm and delete them. |

## Module layout

```
:game     (new, game/)  KMP: androidTarget() + jvm(). commonMain = all game code, packages unchanged.
                        androidMain/jvmMain = thin actuals (DB builder, Archives). Depends on :verktex:core.
                        -Pgame.checkCommon adds a linuxX64 target to prove commonMain is truly common
                        (JVM-only targets let java.* leak into common; JS isn't an option because Room
                        doesn't support it). Run with compileKotlinLinuxX64.
:          (app)        Android application: MainActivity, services, Firebase, OsuPlatform impl,
                        depends on :game and :verktex:android.
:game-desktop (later)   Desktop launcher over :verktex:desktop, not part of this plan.
```

The app's tests (`tests/test/kotlin/`) move to `game/src/jvmTest` (JUnit as today), and to
`commonTest`/kotlin.test once their dependencies are common.

## Phases (each one ships, builds and runs on device)

1. **Scaffold `:game`.** Set up the KMP module, KSP, the `checkCommon` target and the dependency
   wiring. Move nothing yet.
2. **Pure logic first.** `beatmaps/`, `difficulty/`, `mods/`, `math/`, `scoring/`, `replay/`,
   `GameMode`, the rian/reco1l framework utils. Fix the few blockers here: `Log` → `Logger`,
   `TextUtils`, `@IntRange`, `PointF`, the mod registry instead of reflection, `ModSetting`'s
   org.json, `BeatmapParser`'s `File` → Okio. Move their tests. This proves the setup works.
3. **Foundations.** Logger, Okio, kotlinx.serialization JSON, Ktor-backed `WebRequest`, atomicfu
   and coroutines in `ExecutionOverloads`, kotlinx-datetime, number/string formatting, the string
   generation task and `StringTable`, and `Config` on multiplatform-settings.
4. **Platform services.** Add the Verktex `Platform.Implementation` additions (Android and desktop
   implementations) and `OsuPlatform` (Android implementation). Point callers at them.
5. **Database.** Room KMP with KSP, `SQLiteConnection` migrations, and the expect builder. Verify
   against an existing user DB (schemas in `schemas/`).
6. **Java → Kotlin, faithful conversion.** One file per commit, translated literally, then moved to
   `:game` when its dependencies are common. Go bottom-up: enums and small value classes
   (`RankedStatus`, `ResultType`, `TouchType`, `Constants`…), `Config`, skins (`OsuSkin`,
   `SkinJsonReader`, `SkinLayout`), `StatisticV2`, `Replay`, `edlplan/osu/support/slider/*`, the
   game objects, `GameHelper`, then `GameScene`, `ScoringScene`, `OnlineManager`/`OnlineScoring`,
   `LibraryManager`, `GlobalManager`. `MainActivity` and the other Android classes are converted
   too but stay in the app.
7. **View UI rewrite** on Verktex, as in the table above, and delete the dead Android UI.
8. **Multiplayer.** The Socket.IO client over Ktor WebSockets; switch `RoomAPI`/`LobbyAPI`.
9. **Cleanup.** Drop the OkHttp, socket.io-client, org.json helpers, gson (declared but unused),
   commons-io, kotlin-reflect, EdlJavaExt/EdlOsbSupport, and kapt dependencies. Update CLAUDE.md
   (module layout, the Java rule, common-code rule for the game).

## Verification

- At every phase: `./gradlew :game:jvmTest test assembleDebug`, then install and smoke test on
  device (menu, song select, play a map with sliders and spinners, results, replay, online login,
  a multiplayer room).
- At the end: `./gradlew -Pgame.checkCommon :game:compileKotlinLinuxX64` passes, and commonMain has
  no `java.`, `javax.`, `android.` or `org.json` imports.
- Database: upgrade from a real existing install and check scores, collections and options survive.
- Settings: existing SharedPreferences values are still read after the move.
- Optional end check: a throwaway desktop launcher that opens `OsuUITests` scenes through
  `:verktex:desktop`.

## Progress

- `:game` exists (JVM target, `-Pverktex.checkCommon -Pgame.checkCommon :game:compileKotlinJs`
  checks the common code).
- Logging goes through Verktex's `Logger`, which prints to the standard output until a platform sets it.
- `Config`, `Debug`, `BuildType`, `BuildSettings`, `DifficultyAlgorithm`,
  `BeatmapLeaderboardScoringMode` and `GameMode` are Kotlin and live in `:game`. What they need from
  Android goes through `OsuPlatform` (`com.osudroid.android.AndroidOsuPlatform` on Android).
- `com.osudroid.math` and its tests are in `:game`; `Vector2`'s `PointF` conversions stay in the app
  (`com/osudroid/math/PointFVectors.kt`) until `PointF` is gone.
- `Constants`, `GameMod`, `TouchType` and `ResultType` are Kotlin and in `:game`.
- `StringTable` is common: `:game` generates `com.osudroid.resources.Res` (the language pack's string
  and array names) and the platform provides the text (`AndroidStringTable` reads the language pack's
  resources by name, so Android still picks the language). The `Int` overloads stay for app code.
- `ToastLogger` is common, the platform shows the messages (`AndroidToasts`).
- The mod system has no JVM reflection: `ModHashMap` is a copy-on-write map keyed by `KClass`, settings
  register themselves through `provideDelegate`, and new instances come from `modFactories`.
- `Config.isRemoveSliderLock()` no longer looks at the room: `Multiplayer.isRemoveSliderLock()` does.
- The app's Robolectric tests need Java 17 (Robolectric can't read Java 25 class files), and some
  assume an English locale. Tests that move to `:game` run as plain JVM tests.

## Next steps

Done since the previous notes:

- EdlJavaExt's `Vec2`, `LinePath`, `FMath`, `FloatArraySlice`, `SmartIterator` and `Consumer` were
  replaced by common code (`Vector2`, `com.osudroid.math.LinePath`, `MathUtils`,
  `com.osudroid.utils.FloatArraySlice`) and the JDK's. The jar stays only for the easings of the
  Android views. EdlOsbSupport was dropped.
- `android.graphics.PointF` was replaced by `Vector2` everywhere.
- The execution helpers are common; the main thread and uncaught exceptions go through `OsuPlatform`.
- The core logic is in `:game`: beatmaps, difficulty, mods, scoring, the replay checks, `Replay`,
  `StatisticV2`, `GameHelper` and `Utils`, with their 377 tests running as plain JVM tests. Replays
  still use Java's object serialization through `ReplayInput`/`ReplayOutput`, which only the JVM
  implements so far; `StatisticV2`'s multiplayer and database conversions are app extensions.

- The skin configuration is in `:game`: `OsuSkin`, `SkinJsonReader`, `SkinLayout`, the skin data,
  the skin.ini reader and its conversion, read with kotlinx.serialization. `com.osudroid.utils.JsonObjects`
  coerces values as Android's org.json does (use it when moving other org.json code) and `LenientJson`
  parses as leniently. `HUDSkinData` names element types instead of referencing the HUD's classes.

- The gameplay objects are Kotlin: `GameObject`, `GameplayHitCircle`, `GameplaySlider`, both spinners,
  `GameEffect`, `GameObjectPool`, `GameplayEffects`, the listeners, `SliderBody`, `DrawLinePath` and
  `TriangleBuilder`. Their protected fields stay `@JvmField` while Java (`GameScene`) uses them.

- The skin assets and the audio are in `:game`: `SkinTextures`, `SkinSounds`, `BeatmapSkinManager`,
  `ISkinnable`, `MD5Calculator`, `BassSoundProvider`, `Metronome`, `Status`, `SongPlayer`,
  `SongServiceClock` and `SongService`. `SongService` is a plain class now; on Android
  `AndroidSongService` (the bound service) creates it and stops itself through `SongService.onExit`.
  Verktex's `Provider.list` lists bundled assets, so `SkinSounds` no longer needs Android's `AssetManager`.

- The gameplay objects are in `:game`: hit objects, slider body, cursors, effects, hit samples and the
  replay controls. They reach the scene only through `GameObjectListener` (which also answers
  `isRemoveSliderLock`) and callbacks (`BreakAnimator.onEnding`, `Countdown.onFinish`). Verktex's
  `Platform.uptimeMillis` is the time base of cursor events.

- The database entities and DAOs are in `:game`, annotated through `room-common` (which has a variant
  for every platform, JavaScript included), and `DatabaseManager` reaches the tables through
  `GameDatabase`. Room itself stays a platform implementation: on Android, `AndroidDatabase` opens
  Room's `DroidDatabase` (generated by kapt from the classes in `:game`, the same code as before), runs
  the migrations and imports the legacy data. DAOs stay blocking, so callers didn't change. Another
  platform would implement `GameDatabase` itself, with Room on desktop JVM.

- The Kotlin UI uses the common `Res` string keys instead of Android's `R` (the app's temporary strings
  are generated into `Res` too); Java still uses `R`.

- Networking is common code over blocking platform clients:
  - `Http` runs `HttpRequest`s (built with `HttpRequestBuilder`, `HttpUrlBuilder` and `HttpBody`); OkHttp
    implements it on Android (`OkHttpClientImplementation`).
  - `WebRequest`, `JsonRequest` and `FileRequest` read kotlinx JSON, with strict getters
    (`getString`, `getInt`…) that mirror org.json's next to the `opt*` ones.
  - `OnlineManager`, `OnlineFileOperator` and the post builders are Kotlin in `:game`. `SecurityUtils`
    signs with Okio; the APK signature and asset checks stay in `AndroidSecurityUtils`.
  - Multiplayer talks through `RoomSocket`, which `SocketIORoomSocket` implements on Android over the
    socket.io Java client, converting org.json at that boundary.
  - Calls stay blocking, so callers didn't change. **Later:** move to Ktor (suspend calls) and a Socket.IO
    client over Ktor WebSockets, which would drop OkHttp and socket.io-client.

- Android views, started: Verktex has `UIPromptDialog`, and the calibration inputs and the HUD editor's
  exit dialog use Verktex dialogs. `Platform.openWebPage` replaced `WebViewFragment` (Verktex's
  `WebPageActivity` on Android, the browser on desktop). The unused preference views and settings fragment
  are deleted. Song select's "Manage collections" opens `BeatmapCollectionsDialog` (add the set to or remove
  it from collections, create and delete them) instead of `CollectionsManagerFragment`, whose unused
  folder picker went with it.

What's left, roughly in order:

1. **Skins, the rest**: `Skin` (reading the skin folder, which reports to the loading screen and
   checks textures with Android bitmaps).
2. **`GameScene`, the HUD and the pause menu**: they still need `GlobalManager` (a hub of Android
   state: the activity, its context, the `Application`), multiplayer, online scoring, `ScoreBoardItem`,
   the video background, the vibrations and several scenes.
3. **Scenes**: `GameScene` and `ScoringScene` are Kotlin (`ScoringScene` still formats with
   `java.lang.String.format` and `SimpleDateFormat`); the rest of the Java (`MainActivity`, `LibraryManager`,
   `OnlineManager`, `GlobalManager`…) is still to convert.
4. **Database**: done as above; a desktop `GameDatabase` when there's a desktop launcher.
5. **Networking**: done as above, except `OnlineScoring` (Java, shows an Android Snackbar). Ktor later.
6. **Android views**: rewrite on Verktex the beatmap listing (with its download and loading fragments),
   the block area editor, the replay import's loading fragment, the difficulty calculation badge, and
   the remaining `MessageDialog`s (`BlockAreaEditorFragment`;
   the ones in `UpdateManager`, `AndroidDatabase`, `AccessibilityDetector` and `MainActivity` are Android only).
7. A Java serialization reader/writer in common code, if replays must load outside the JVM.

## Desktop game first

Decided with the user on 2026-11-05: get a desktop build of the game compiling (then running)
before finishing the smaller items above. What isn't a big scope is skipped for now:

- **Features left on Android, behind `OsuPlatform` hooks.** Desktop doesn't offer them, and the
  game hides their buttons or does nothing:
  - the beatmap listing, with its download and loading fragments;
  - the block area editor;
  - the difficulty calculation badge;
  - the replay import's file picker and its loading fragment;
  - the video background (`UIVideo` decodes with ExoPlayer);
  - vibrations, app updates, push notifications, the accessibility check and Crashlytics.
- **JVM code goes to `:game`'s `jvmMain`.** Code that still needs `java.*` (files, executors, date
  formats, Java serialization) moves there instead of being rewritten as common code. Desktop is a
  JVM too, so it compiles for both, and `-Pgame.checkCommon` keeps checking `commonMain`. Rewriting
  that code as common code comes later.
- **Multiplayer on desktop**: the code moves to `:game`, but desktop has no `RoomSocket` yet, so
  multiplayer isn't offered there.

The big pieces are done for real, in order:

1. The hooks above in `OsuPlatform`, and their Android implementations.
2. `GlobalManager` in Kotlin without the activity or its context. The platform opens the database
   and the song service, then the common `init` loads the skin and the scenes.
3. The app's game code moves to `:game` in batches, following its dependencies: `LibraryManager`,
   `ScoreBoardItem`, `OnlineScoring`, `Multiplayer`, then the UI (`hud`, `songselect`, `modmenu`,
   `multi`, `mainmenu`, `settings`), `GameScene`, `ScoringScene` and `GameLoaderScene`. Packages
   don't change. What stays in the app is the Android shell: `MainActivity`, the services, Firebase,
   the Android database, the platform implementations and the skipped features.
4. `:game-desktop`, the desktop launcher over `:verktex:desktop`: the desktop `OsuPlatform`
   (preferences, folders), the strings read from the language pack, the database (Room on the
   JVM), HTTP, and a game host doing what `MainActivity` does at startup.

### Progress

All four steps are done, and the game starts on desktop: the main menu and the settings render, at
60 FPS on OpenGL (`./gradlew :game-desktop:run`, `--screenshot` saves a frame).

- `OsuPlatform` has a hook for each Android-only feature, with a default for platforms without it:
  the beatmap listing and set downloads, the block area editor, the progress badge, the replay
  picker, `createVideo` (`UIVideo` is abstract, `ExoPlayerVideo` is Android's), vibrations, file
  integrity, updates, the preferences backup, the language, the clipboard, the changelog, the skin
  texture checks and the login failure message. Settings hide the categories a platform lacks, and
  `RoomSocket.isAvailable` hides multiplayer. Verktex has `Platform.openUrl` for links.
- `GlobalManager`, `LibraryManager`, `ScoreBoardItem` and `OnlineScoring` are Kotlin. The activity,
  its context and the application are in the app's `AndroidGame`.
- 82 files moved to `game/src/jvmMain/` (all the game's scenes and UI). HUD elements are created by
  factories instead of kotlin-reflect. OkHttp's `Http` implementation is in `jvmMain` for both
  platforms.
- Desktop database: Room only implements suspend DAO functions outside Android, so
  `:game-desktop:generateSuspendDaos` copies the game's DAOs with suspend functions and generates
  adapters that block on them, keeping the queries defined once. Desktop databases are recreated
  instead of migrated for now.
- Running it found an engine bug, fixed: the desktop image decoder freed a moved buffer, which
  corrupted the heap.

Left for desktop, roughly in order:

1. Importing `.osz` and `.osk` files (Android's `loadBeatmapLibrary`/`checkNewSkins` use `FileUtils`,
   which is Android code); desktop only scans folders already extracted into `Songs/`.
2. Toasts: Verktex has no toast component yet, desktop logs them.
3. Multiplayer: a `RoomSocket` for desktop (or the planned Socket.IO client over Ktor).
4. Videos, the beatmap listing, the block area editor and the replay picker, as Verktex rewrites.
5. Moving `jvmMain` code to `commonMain` as its JVM uses are replaced.
6. The app's `MigrationTest` fails since `DatabaseManager.databasePath` reads `OsuPlatform`, which
   the test doesn't set (since `057734a75`, before this work).

## Next: moving shared features into Verktex

The game's own infrastructure (`Config`, `StringTable`, `ToastLogger`, `Http`, `OsuPlatform`,
`SongService`, `GlobalManager`, the slider body code, `UIVideo`, the database setup) is being replaced
by Verktex features, see `engine-consolidation.md`.
