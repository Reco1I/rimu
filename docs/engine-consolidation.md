# Moving shared features into Verktex

## Context

Now that the game runs on Android and desktop, a lot of the game's code is general-purpose infrastructure
that only exists because Verktex doesn't offer it yet: `Config` on multiplatform-settings, `StringTable`
with one implementation per platform, `SystemFileSystem`, the `Http`/`WebRequest` layer over OkHttp,
`ToastLogger` with native toasts, Room wired separately on each platform, the `OsuPlatform` grab bag,
`SongService` and `SongPlayer`, `GlobalManager` and the singleton scenes, and the edlplan slider-body code.
The goal is to shrink the game: Verktex provides each of these features, and the game only configures
them.

**Decided with the user:**
- Settings are stored in a JSON file.
- Text components take a `CharSequence`, and a localized string is a `CharSequence` that re-resolves when
  the language changes.

**Rules that still apply:**
- Verktex never depends on the game.
- `:verktex:core` stays common code, checked with `-Pverktex.checkCommon :verktex:core:compileKotlinJs`.
- Every phase builds, passes the tests, runs on device and on desktop, and is committed when verified
  (commits only, no push).

## Keeping this document

The **Progress** section at the end is updated at the end of every phase: what's done, what's left and
the decisions taken. `CLAUDE.md` is updated whenever a phase changes a rule or a module it describes.

## Phases, easiest first

### Phase 1: file system and buffers (easy)

**Evaluation.**
- **Okio or kotlinx.io:** keep Okio.
  - Okio is already used in 28 files.
  - It has hashing (`ByteString.md5()`, used by `MD5Calculator` and `SecurityUtils`), a zip reader
    (`openZip`, for `.osz`/`.osk` imports), `FakeFileSystem` for tests, `listRecursively` and `metadata`.
  - kotlinx.io lacks hashing and zip reading, and its file system API is minimal.
  - Ktor 3 brings kotlinx.io in transitively. Bridge it only at the HTTP boundary (copy a
    `ByteReadChannel` into an Okio `Sink`, or use the `kotlinx-io-okio` adapters if present).
  - `kotlinx-io-core` in `verktex/android/build.gradle.kts` is imported nowhere. Remove it.
- **DitchOoM buffer:** Okio can't replace it. Okio's `Buffer` is made of heap segments, and the GL, Vulkan
  and stb uploads need direct memory without a copy.
  - The library is only used for the texture upload contract (`GraphicsTexture.onWritePixels`/`write`
    taking `ReadBuffer`), plus two duplicated `Buffers.kt` bridges.
  - Replace it with a Verktex type: `expect class PixelBuffer` in core, with
    `actual typealias PixelBuffer = java.nio.ByteBuffer` in the core's `jvmMain`.
  - That's zero-copy, both JVM platforms share it, both `Buffers.kt` files go away, and the dependency is
    dropped.

**Work.**
- Verktex core gets Okio as an `api` dependency.
- Add `Platform.fileSystem: okio.FileSystem` and `Platform.storage` (`dataDirectory`, `cacheDirectory`,
  `externalDirectory?` as Okio `Path`) to `Platform.Implementation`.
  - Implement them in `AndroidPlatform` and `DesktopPlatform`.
  - For JS, the `checkCommon` target uses `okio-nodefilesystem`.
- Delete the game's `com/osudroid/utils/FileSystems*.kt` (the `expect val SystemFileSystem`). Its 14
  callers use `Platform.fileSystem`.
- `OsuPlatform.defaultCorePath`, `cachePath` and `canUseStorage` move to `Platform.storage`.

**Files:** `verktex/core/.../graphics/GraphicsTexture.kt`, `ESTexture.kt`, `ESExternalTexture.kt`,
`VKTexture.kt`, `GLTexture.kt`, `PixelsTextureSource.kt`, both `Buffers.kt`, `Platform.kt`,
`game/src/*/kotlin/com/osudroid/utils/FileSystems*.kt`.

### Phase 2: toasts in Verktex (easy)

- Add `verktex/core/.../ui/toast/`:
  - `UIToast` is a themeable component with text, an optional progress bar and an icon slot.
  - `Toasts` is a thread-safe queue: `show(text: CharSequence, duration)` returns a `ToastHandle` with
    `text`, `progress`, `isIndeterminate` and `dismiss()`.
- Toasts are drawn on Verktex's overlay layer (`Verktex.addOverlay`, `Verktex.kt:510`), and posted to the
  update thread so background callers are safe.
- Add a "Verktex/Toasts" UI test to `VerktexUITests`.
- In the game:
  - Replace `ToastLogger.showText` (81 calls in 22 files) with `Toasts.show`, then delete `ToastLogger`,
    `AndroidToasts` and the desktop logging stub.
  - `OsuPlatform.showProgressBadge`/`ProgressBadge` become a toast with progress.
  - `showConnectionFailure` becomes common code.
- `ImportReplayActivity` (an Android activity without the engine surface) keeps `Toast.makeText`.

### Phase 3: `MusicService` (easy to medium)

- Merge `SongService` (`game/.../ru/nsu/ccfit/zuev/audio/serviceAudio/SongService.kt`) and `SongPlayer`
  (`game/.../com/osudroid/audio/SongPlayer.kt`) into `abstract class MusicService` in `:game`'s common
  code, package `com.osudroid.audio`.
  - It owns the `AudioStream` and its lock, the FFT cache and the "stop at end while gaming" behaviour.
  - Its open hooks default to no-ops (`onPlaybackStateChanged`) for platforms that later add a media
    session.
- Fold `MusicManager`'s playback state into it: `currentBeatmap`, play, next and previous.
  - This makes it the single owner of "what's playing" and removes one of the three copies of the
    selected beatmap.
  - The control points stay where they are used.
- Delete `AndroidSongService` (a bound service with no foreground notification or media session, so it
  adds nothing), the binding in `MainActivity.java:551-573`, its manifest entry and the static in
  `SaveServiceObject`.
- `FakeSongService` becomes `FakeMusicService`.
- About 125 `songService` call sites are renamed. `SongServiceClock` becomes `MusicClock`.

### Phase 4: localization in Verktex (medium)

- Add `verktex/core/.../localization/`:
  - `Localization` holds the current `locale`, a fallback chain (for example `pt-BR` → `pt` → `en`),
    `get(key)`, `getArray(key)` and observers for locale changes.
  - Strings come from registered `StringSource`s. The built-in source reads `strings/<locale>.json`
    through `Platform.provider`, and a game can add more sources.
  - A common `format(pattern, args)` handles `%s %d %f %.Nf %n$s %%` (built on `toFixed`).
- Text is a `CharSequence`:
  - `LocalizedString(key)` and `LocalizedString.format(args)` implement `CharSequence` by resolving
    against the current locale.
  - `UIText.text`, `UITextButton.text`, `UIMessageDialog.text`, `UIPromptDialog.text` and
    `UITextureText.text` become `CharSequence`.
  - When the value is localized, the component registers a locale binding, the same way `bindTheme`
    does in `ui/Themed.kt` and `UIComponent`. A language change then re-resolves the text in place,
    without restarting the app.
  - Reads that need a `String` use `toString()`.
- The game's `StringResource`/`StringArrayResource` (`game/.../com/osudroid/resources/StringResources.kt`)
  become Verktex's `LocalizedString`/`LocalizedStringArray`. The generated `Res` keeps its shape.
- `GenerateStringResources` (`game/build.gradle.kts`) also converts every `values-xx/strings.xml` in the
  language pack AAR into `strings/<locale>.json` game assets, so the language pack is injected as an
  external source.
- Delete `StringTable`, `AndroidStringTable` and `DesktopStringTable`.
  - `OptionInfo.title`/`summary: Any` become `CharSequence`.
  - The `UIText.setText(StringResource)` extensions go.
  - The remaining `R.string` uses in Java and the app go through `Res`.
- `OsuPlatform.canChooseLanguage`/`setLanguage` become `Localization.locale`. Desktop gets every language.

### Phase 5: Settings replace `Config` (medium; large but mechanical)

**Verktex**
- `Settings` becomes typed and persistent:
  - Declare a key with `class SettingKey<T>(name, default, serializer: KSerializer<T>)` through
    `setting("showfps", false)` helpers, so enums and small data classes are typed through
    kotlinx.serialization.
  - Access goes through `Settings[key]`, `Settings[key] = v`, a `by key` property delegate and typed
    observers.
- Storage is `settings.json` in `Platform.storage.dataDirectory`.
  - It's a `JsonObject` written atomically (a temporary file, then `atomicMove`) with a debounced save
    and a flush on pause and exit.
  - Unknown keys are kept, and malformed values fall back to their defaults.
- `VerktexSettings` declares its keys the same way: the graphics backend, the resolution scale, the debug
  overlays and the Vulkan crash guard (which today is the raw `"verktex_graphics"` SharedPreferences in
  `Android.kt:73`).

**Game**
- An `OsuSettings` object declares the roughly 85 keys under their current names.
- `Config`'s derived, non-setting state moves out:
  - paths go to `GameFolders` (over `Platform.storage`);
  - the display size goes to `Platform.currentDisplay`;
  - the skin paths go to the skin code.
- Migrate the roughly 400 `Config.` references in 78 files, then delete `Config`.
- `PreferenceControls`/`SettingsMenu` take a `SettingKey` instead of a string key.
- Keep `SENSITIVE_KEYS`, the bounds and the float metadata as properties of the keys.

**Migration**
- On first start without `settings.json`:
  - Android imports every entry of the default SharedPreferences;
  - desktop imports `java.util.prefs` (`osu!droid` node).
- After that, the old store is left untouched.

**Cleanup**
- `ConfigBackup` becomes common: copy `settings.json` out and back in.
- Delete `OsuPlatform.preferences` and the backup hooks.
- Drop `multiplatform-settings` and `androidx.preference`.
- `MainActivity.java:255,279,526` raw preference accesses go through `Settings`.

### Phase 6: Ktor networking (medium)

**Verktex**
- Add Ktor 3 client (`ktor-client-core`).
- `Platform.Implementation.httpEngine: HttpClientEngineFactory<*>`: OkHttp on Android, the Java engine
  or OkHttp on desktop.
- `Network.createClient(configure: HttpClientConfig<*>.() -> Unit): HttpClient` builds on the platform
  engine, and `Network.client` is a shared default.
- `Platform.openWebPage`/`openUrl` stay as they are.

**Game**
- An `OsuHttp.client = Network.createClient { ... }` sets JSON content negotiation (the existing lenient
  `Json`), the user agent and timeouts.
- Replace every use of `Http`, `WebRequest`, `JsonRequest`, `FileRequest` and the post builders with
  suspend Ktor calls (`client.get/post`, `submitForm`, `submitFormWithBinaryData`), and run them in
  coroutines.
  - Callers: `OnlineManager`, `OnlineFileOperator`, `PostBuilder`, `IBanchoAPI`, `LobbyAPI` and
    `BannerManager`.
  - App callers: `UpdateManager`, `BeatmapDownloader`, `BeatmapListing` and `DownloadFragment`.
  - Downloads stream `bodyAsChannel()` into an Okio sink.
- Delete `com/reco1l/framework/net/` and `OkHttpClientImplementation`. Remove the `Http.implementation`
  setup in `MainActivity`, `OsuDesktop` and `OsuUITestActivity`.

**Follow-up, separate commit**
- A Socket.IO v4 client over Ktor WebSockets, implementing `RoomSocket`.
- This gives desktop multiplayer and removes `socket.io-client`. OkHttp then remains only as Ktor's
  Android engine.

### Phase 7: split `OsuPlatform` (medium; mostly done by phases 1 to 6)

Several members are already gone by now: the preferences, the paths, the toasts and progress badge, the
language and the backup.

**Moves to Verktex `Platform`**
- `isInForeground`
- `exit()` (from `exitGame`)
- `clipboard`
- `isLowEndDevice`
- `runOnMainThread`
- the uncaught-exception hook (`Platform.onUncaughtException`, wired to `Logger`)
- `versionName`/`isDebug` (as `Platform.application`)
- the display refresh-rate control (`currentDisplay`)
- `availableGraphicsBackends`, which replaces `isVulkanAvailable`; `resetVulkanCrashGuard` becomes
  internal to `verktex:android`
- a `haptics` primitive (vibrate with duration and amplitude)
- `pickFiles(extensions)`, for the replay import

The game's `GameplayVibrations` becomes common code over `Platform.haptics`.

**Becomes `interface OsuGameHost : GameHost`** (in `:game`'s common code, reached through
`Verktex.host as OsuGameHost`)
- Game-specific hooks, with defaults meaning "not available": `buildType`, analytics,
  `setAnnouncementsSubscribed`, `loadBeatmapLibrary`/`checkNewSkins`, `checkSkinTexture`, the beatmap
  listing and downloads, the block area editor, `verifyFileIntegrity`, updates and `openChangelog`.
- `MainActivity` (an `AndroidGameHost`) and `OsuDesktop` (a `DesktopGameHost`) implement it.
- Delete `OsuPlatform`, `AndroidOsuPlatform` and `DesktopOsuPlatform`.

### Phase 8: no `GlobalManager`, no singleton scenes (medium to hard)

- An `OsuGame` object owns startup and the game-wide services: `init()` (skin, sounds, music), `music`
  and `host`.
- `BeatmapSelection` is the single owner of the selected beatmap. It replaces
  `GlobalManager.selectedBeatmap`, `SongSelect.selectedBeatmap` and, after phase 3, `MusicService`'s copy,
  plus the manual syncing in `MainScene.kt:223,381-383,475-482` and `SongSelect.kt:289,391,506`.
- Loading progress and info become state of the loading and splash scenes.
- `isHeadless` moves to `OsuGame`.
- `MainScene`, `SongSelect`, `CalibrationScene`, `SplashScene` and `ModMenu` become classes created when
  pushed.
  - What must persist moves to objects outside the scene: the song select filter, sort and scroll
    position, MainScene's background index, and the selected mods (`ModSelection`).
  - `LibraryManager` already holds the library.
- `GameScene` and `ScoringScene` are created per play by `GameLoaderScene`.
  - `GameScene`'s `sliderRenderPaths` cache is per-beatmap state, so it moves to the loader result.
- Measure the time to open song select with a large library before and after, since it's now rebuilt on
  every visit. If it regresses, keep the carousel data in a driver object.
- Delete `GlobalManager`, which has about 119 references in 38 files.

### Phase 9: database in Verktex (hard)

**`:verktex:database`**
- A new optional Verktex module, so that `:verktex:core` keeps passing the JS check (Room has no JS
  target).
- It provides `Database.open<T : RoomDatabase>(name, factory, migrations)` with `BundledSQLiteDriver`
  on every platform, the file in `Platform.storage.dataDirectory`, and the Android context supplied
  internally by `verktex:android`.
- It also provides an abstraction for engine tables: Verktex entities and DAOs plus a
  `VerktexTables` interface. A game lists those entities in its own `@Database`, so Verktex tables live
  alongside game tables. The first of them is an optional key-value store; Settings stay in JSON as
  decided.

**Game**
- One `@Database @ConstructedBy class OsuDatabase` in `:game`, processed by KSP on both platforms.
  - It replaces the kapt `DroidDatabase`, the desktop `DesktopDatabase` and the
    `:game-desktop:generateSuspendDaos` task.
  - The DAOs become `suspend`, and callers use coroutines.
- `Migrations.kt` is ported to `Migration.migrate(SQLiteConnection)`, so desktop migrates instead of
  recreating the database.
- The legacy import (`DBOpenHelper`, the `ObjectInputStream` cache) stays an Android step.

**Spike first:** `:game` has only a JVM target and Android consumes it, so check that Room KMP's JVM
artifacts work inside the APK. If they don't, add `androidTarget()` to `:game`.

**Verify** by upgrading from a real install (schemas 1 to 5 in `schemas/`), and fix the app's
`MigrationTest`.

### Phase 10: `Path` and `UIPath`; sliders without the edlplan code (hard; performance sensitive)

**Verktex `math/Path`**
- A polyline stored as a packed `FloatArray` (x, y) with cumulative lengths, accessed as `Vec2`.
- Operations: `length`, `pointAt(distance)`, `indexAt`, `cut(start, end)`, `simplified(tolerance)` and
  `bounds`. This ports `com.osudroid.math.LinePath`.

**Verktex `ui/UIPath`** draws a stroked path with round caps and joins.
- **Geometry:** one triangulation per path, built once.
  - Each vertex carries its distance from the centre line, 0 at the centre and 1 at the edge.
  - Its depth equals that distance, so overlapping segments resolve with `DEPTH_LESS` (the trick already
    used in `SliderBody`).
- **Shader:** a fragment shader maps the distance to colour (body gradient, border width and colour).
  - This replaces the three `UITriangleMesh` layers (background, border, hint) with one draw.
  - The hint is a second `UIPath`, or a band in the same gradient.
- **Snaking:** the vertices are ordered along the path, so snaking in or out draws a vertex range plus
  one rebuilt partial segment and cap. This replaces the `tryBuildFast*` paths and the per-frame
  rebuilds.
- **Renderer:** `GraphicsRenderer.drawTriangles` (`:740`) transforms vertices on the CPU every frame.
  Add a static mesh (a vertex buffer kept between frames, with an extra attribute) for UIPath on GLES,
  GL and Vulkan.
- **Evaluation:** this is the same approach as osu!lazer's slider bodies. Rendering the path to an
  off-screen buffer is only needed for alpha fading of overlapping bodies, which UIPath can provide as an
  option.

**Game**
- `SliderPath` exposes its calculated path as a Verktex `Path` in osu!pixels.
  - The difficulty calculation keeps its own `Double` lengths, so the 377 tests stay exact.
- `GameplaySlider` places the `UIPath` inside the scaled playfield, so
  `GameHelper.convertSliderPath`'s screen-space rescaling goes away. Point thinning becomes
  `Path.simplified`.
- Delete `SliderBody`, `DrawLinePath`, `TriangleBuilder`, `UITriangleMesh` and `LinePath`, with their
  tests moved to `Path`.

**Verify:** compare screenshots of a slider-heavy map against the old renderer, and measure on the Adreno
610 test phone with back-to-back A/B profile builds.

### Phase 11: video in Verktex (hardest)

**Evaluation**
- **Android:** keep ExoPlayer, including the FFmpeg extension it already uses for codecs.
  - It decodes in hardware straight into the external texture with zero copies.
  - Plain FFmpeg on Android would decode in software and upload YUV every frame, which costs too much on
    low-end phones.
- **Desktop:** use FFmpeg, through JNA bindings to bundled `libavformat`/`libavcodec`/`libswscale`
  shared libraries (packaged like BASS's natives).
  - Use LGPL builds only, dynamically linked.
  - Upload the YUV planes to three textures with a YUV→RGB shader (or `sws_scale` to RGBA as a first
    step).
  - The bytedeco/JavaCV presets would be quicker to wire but add about 100 MB, so avoid them.

**Work**
- `UIVideo` moves to Verktex as a concrete component over `Platform.createVideoDecoder(path)`, with
  load, play, pause, seek, speed and release, driven by a `Clock`.
- The ExoPlayer decoder moves into `verktex:android`. The local `media3-decoder-ffmpeg.aar` needs a
  local Maven repository in the repo, so a library module can consume it.
- Delete `OsuPlatform.createVideo` and `ExoPlayerVideo`.
- `GLDevice` stops rejecting video on desktop.

## Verification (every phase)

- `./gradlew :verktex:core:jvmTest :verktex:android:testDebugUnitTest :verktex:shadertools:test :verktex:desktop:test :game:jvmTest test assembleDebug`
- `./gradlew -Pverktex.checkCommon -Pgame.checkCommon :verktex:core:compileKotlinJs :game:compileKotlinJs`
- Desktop: `./gradlew :game-desktop:run --args="--screenshot build/game.png --delay 10"`, plus the UI test
  of the new component (`:verktex:desktop:run --args="--test 'Verktex/Toasts' --screenshot ..."`).
- Android: install, then smoke test the menu, song select, a play with sliders and a video, the results,
  online login, a multiplayer room and the settings.
- Phase-specific checks:
  - Phase 5: existing settings survive the upgrade.
  - Phase 4: changing the language live updates the text.
  - Phase 9: the database upgrades from a real install.
  - Phase 10: screenshots and A/B performance.
- Update `docs/engine-consolidation.md` (Progress) and commit.

## Progress

Updated at the end of every phase: what's done, what's left, and the decisions taken along the way.

| Phase | State |
|---|---|
| 1. File system and buffers | Done |
| 2. Toasts | Done |
| 3. `MusicService` | Not started |
| 4. Localization | Not started |
| 5. Settings replace `Config` | Not started |
| 6. Ktor networking | Not started |
| 7. Split `OsuPlatform` | Not started |
| 8. No `GlobalManager` | Not started |
| 9. Database in Verktex | Not started |
| 10. `Path` and `UIPath` | Not started |
| 11. Video | Not started |

### Phase 1: file system and buffers

- DitchOoM's buffer is gone. Textures take a `PixelBuffer`, an `expect sealed class` that is
  `java.nio.ByteBuffer` on the JVM (sealed, since recent JDKs seal `ByteBuffer`). Both platforms'
  `Buffers.kt` bridges were deleted. Backends pass the buffer to GL, GLES and Vulkan as before.
- The unused `kotlinx-io-core` was dropped from `:verktex:android`.
- Verktex core depends on Okio. `Platform.fileSystem` replaces the game's `SystemFileSystem`; it
  doesn't need `Platform.implementation`, so tests can use it.
- `Platform.storage` (`dataDirectory`, `cacheDirectory`, `externalDirectory`) replaces
  `OsuPlatform.defaultCorePath`, `cachePath` and `canUseStorage`.
  - Android: `filesDir`, `cacheDir`, and the external storage when it's mounted.
  - Desktop: the system's application data folder (`%APPDATA%`, `Application Support`,
    `$XDG_DATA_HOME`) named after the host's title, the temporary folder, and the home folder. Both
    folders are created at startup.
  - The game's default folder is `<external or data>/osu!droid/`. That's the same path as before,
    except on an Android device whose storage isn't mounted, which now falls back to the private folder.
- Checked: the core, desktop and game tests, the common checks, `assembleDebug`, and the desktop game.
  Not checked on a device; no phone was connected.
- Already failing before this phase: `ColorInheritanceTest`, `PointerEventTest` and
  `ThemedValuesTest` in `:verktex:core:jvmTest`.

### Phase 2: toasts

- Verktex has `UIToast` (title, text, progress bar or loading indicator, optional action button) and
  `Toasts`, which stacks them at the bottom of the window on the overlay layer.
  - `Toasts.show(text)` and `showLong(text)` are for Java. `show(text, duration, action, onAction)`
    covers the rest.
  - `showPersistent` keeps a toast until it's dismissed.
  - A `Toasts.Toast` can be changed from any thread: changes take the tree lock.
  - Nothing is built until the engine is initialized, so the game's JVM tests don't need it.
  - The "Verktex/Toasts" UI test shows each kind.
- `Verktex.isInitialized` is public (read only).
- In the game:
  - `ToastLogger` and `AndroidToasts` are gone, and the 80 calls use `Toasts`. Its unused log and
    percentage went with it.
  - The difficulty calculation's progress badge is a persistent toast. `OsuPlatform.showProgressBadge`,
    `ProgressBadge`, `LoadingBadgeFragment` and its layout are deleted.
  - The login connection failure is common code in `OnlineScoring`: a 10 second toast with a "Check"
    button opening the guide, instead of Android's Snackbar.
    `OsuPlatform.showConnectionFailure`/`LOGIN_FAIL_HELP_URL` are gone.
  - `MainActivity`'s low storage warning uses `Toasts`. `ImportReplayActivity` keeps Android toasts,
    since it runs without the engine.
- Checked: the tests (only the 3 failures from before), the common checks, `assembleDebug`, the toasts
  UI test and the game on desktop. Not checked on a device.
