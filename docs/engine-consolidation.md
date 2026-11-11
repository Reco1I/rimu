# Moving shared features into Verktex

## Context

Now that the game runs on Android and desktop, a lot of the game's code is general-purpose infrastructure
that only exists because Verktex doesn't offer it yet: `Config` on multiplatform-settings, `StringTable`
with one implementation per platform, `SystemFileSystem`, the `Http`/`WebRequest` layer over OkHttp,
`ToastLogger` with native toasts, Room wired separately on each platform, the `OsuPlatform` grab bag,
`SongService` and `SongPlayer`, `GlobalManager` and the singleton scenes, and the edlplan slider-body code.
The goal is to shrink the game: Verktex provides each of these features, and the game only configures
them. Also the goal is to shrink game platform-specific code.

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
| 3. `MusicService` | Done |
| 4. Localization | Done |
| 5. Settings replace `Config` | Done |
| 6. Ktor networking | Done |
| 7. Split `OsuPlatform` | Done |
| 8. No `GlobalManager` | Done |
| 9. Database in Verktex | Not started |
| 10. `Path` and `UIPath` | Done (done before phase 9, as the user asked) |
| 11. Video | Done |

### Phase 10: `Path` and `UIPath`

- Verktex `math/Path`: a polyline packed as x, y pairs with the length up to each point, built
  with `Path.Builder` or `Path.of`. It has `length`, `lengthAt`, `indexAt`, `pointAt`, `cut`,
  `simplified(minDistance)` (drops points closer than that to the previous one kept) and `bounds`.
- Renderer: `GraphicsRenderer.drawVertices` draws a range of a buffer the caller keeps between
  frames, with its own shader and layout, transformed by the shader. GL and GL ES upload a buffer
  again only when it was cleared or written since (`GraphicsBuffer.clearCount` with the word count),
  so a mesh that doesn't change stays on the GPU. Vulkan has no buffers of its own, it copies only
  the drawn range into its frame ring; device-local static buffers there are left for later.
- Verktex `ui/UIPath` (`halfWidth`, `borderWidth`, `hintWidth`, `shadowWidth`, the colours with a
  `bodyCenterColor` for a gradient, `startLength`, `endLength`): one mesh with round caps and joins,
  triangulated as `DrawLinePath` did, every vertex carrying its distance from the centre line.
  - The distance is the depth, as the plan said, but drawn in two passes over the mesh: the first
    only writes the depth (`Less`, no colour), keeping the part nearest to the centre line at every
    pixel, and the second draws with `LessEqual` so only that part colours it, the band and the
    gradient picked by its distance (`shaders/path.frag`). A single pass would blend a translucent
    body twice wherever a later triangle is nearer (the inside of every bend); drawing the bands at
    one depth, as the old layers did, shows seams in a gradient where the path crosses itself.
    Checked on a translucent self-crossing path: only its 3 colours, no holes or double blends.
  - Snaking draws a range of the mesh plus a rebuilt piece (the partial segments, their caps and
    joins), in every direction, replacing the `tryBuildFast*` paths and the full rebuilds.
- Game: `GameHelper.createRenderPath(slider)` makes the body's `Path` in osu!pixels from the
  slider's calculated path, thinned out as before. `GameplaySlider` places the `UIPath` at the
  stacked position scaled to the playfield, so the screen-space render path is gone. The game
  scene keeps a `Path` per slider instead of a `RenderPathCache`; the mesh is built when the slider
  first draws.
- Deleted: `SliderBody`, `DrawLinePath`, `TriangleBuilder`, `UITriangleMesh`, `LinePath`,
  `FloatArraySlice` and their tests. `PathTest` and `UIPathTest` cover what they did.
- Slider bodies are drawn as osu!stable draws them (osu!lazer's `LegacySliderBody`), always: a
  gradient from the body colour darkened by 10% at the border to it lightened at the centre (in
  linear light), at the skin's body alpha, and a shadow outside the border, 25% black fading out,
  taking 5/64 of the whole width. The skin's slider hint options are no longer used.
- Checked (before the gradient): the engine, desktop and game tests, the common checks,
  `assembleDebug`, the SPIR-V of the new shaders, the `Verktex/Paths` UI test, slider bodies in an autoplay on desktop, and an A/B of the
  old and new bodies drawn side by side for the same paths (a wave, a self-crossing loop with a
  translucent body, sharp angles, a dot, snaking in and out): out of about 300,000 pixels, about
  40,000 differ by 1 or 2 (colours now come as float uniforms instead of 8-bit vertex colours) and 2
  by more, at triangle edges. **Not measured on the Adreno 610**, the plan's back-to-back A/B of
  profile builds is still to do, and the GL ES and Vulkan backends weren't seen on a device.

### Phase 11: video

- Verktex's `UIVideo` (`ui/UIVideo.kt`) is a concrete component over the platform's `VideoDecoder`
  (`video/VideoDecoder.kt`, from `Platform.createVideoDecoder`, null where `Platform.canPlayVideos`
  is false). It keeps the old API (`videoWidth`/`videoHeight`, `setOnReady`, `play`, `pause`,
  `seekTo`, `setPlaybackSpeed`, `release`) and `isAvailable`.
  - **Changed from the plan:** it isn't driven by a `Clock`. `GameScene` keeps driving it as it did
    (play, pause and speed by the gameplay, seek on restart), which works and was left alone.
- Android: `ExoPlayerVideoDecoder` in `verktex:android`, the former `ExoPlayerVideo`, decoding in
  hardware into an external texture. The FFmpeg extension's AAR is in a local Maven repository of
  the engine (`verktex/android/maven`, under Media3's own coordinates
  `androidx.media3:media3-decoder-ffmpeg:1.4.1`, which Google doesn't publish), since a library
  module can't depend on a bare AAR. `AndroidPlatform.canPlayVideos` is whether the backend shows external
  textures (not Vulkan before Android 10).
- Desktop: `FFmpegVideoDecoder` over FFmpeg 8.1 through JNA, with the LGPL shared libraries
  downloaded at build time (as the user chose), see `docs/desktop-platform.md`. The decoder converts
  to RGBA with `sws_scale` (the plan's first step), uploaded into a plain texture: no YUV shader yet.
- Deleted: `OsuGameHost.createVideo`, `ExoPlayerVideo` and the game's abstract `UIVideo`.
- Checked: the game tests, the common checks, `assembleDebug` (with the FFmpeg extension's natives
  for the 4 ABIs), `prepareFFmpeg` on Windows, and the `osu!/Video` UI test on desktop with a
  generated MPEG-4 clip: it plays in real time with the right colours, and seeks and plays at half
  speed. Not checked: a beatmap with a video in gameplay (none in the desktop library), Linux, and
  Android on a device.

### Known regressions

Found along the way, not fixed yet:
- The HUD leaderboard doesn't leave out the score whose replay is watched:
  `ScoringScene.replayID` hasn't been set since the port to Verktex (it was set when a replay was
  opened from a leaderboard score), so it's always -1.
- The engine's `ColorInheritanceTest` (a child gets a different colour than expected) and
  `PointerEventTest` ("Platform implementation not initialized") fail, since before phase 8.

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

### Phase 3: `MusicService`

- `com.osudroid.audio.MusicService` (common code) merges `SongService`, `SongPlayer` and
  `MusicManager`'s playback: the stream and its lock, the FFT cache, "stop at the end while gaming",
  `currentBeatmap` with its listeners, `load(beatmap)`, `play(beatmap)`, `next`, `previous` and
  `update()`, which plays the next song when one ends outside of gameplay.
  - It's an `open class`, not `abstract`: nothing needs a platform yet, so every platform uses it as
    is. `onPlaybackStateChanged` is the hook for a later media session.
  - `next`/`previous` take beatmap sets from a `MusicService.Playlist`, which `LibraryManager`
    implements, since the library isn't common code yet.
  - `play()` does nothing when the song is already playing, as `MusicManager.play()` did; before,
    `SongService.play()` restarted it. `stop()` always releases the song.
  - The stream primitives (`startStream`, `pauseStream`, `stopStream`) are open, so
    `FakeMusicService` replaces them while `play`, `pause` and `stop` keep their bookkeeping.
- `GlobalManager.music` holds it (moving to `OsuGame.music` in phase 8). It's created with
  `GlobalManager`, so the platforms no longer set it.
- The control points stay with the menu: `RythimManager` (now in `ui/v2/mainmenu/`) parses those of
  `music.currentBeatmap` when it changes, and its `onUpdate` also calls `music.update()`.
- Only the callers of `MusicManager` set `currentBeatmap`. Song select, gameplay and the results
  still load songs by path, and the menu catches up when it's entered, until phase 8's
  `BeatmapSelection`.
- Deleted: `AndroidSongService`, its manifest entry, the binding in `MainActivity`, and the song
  service static of `SaveServiceObject` (which stays as the `Application`) and `AndroidGame`. The
  audio driver is disposed by `Verktex.dispose` as before.
- `SongServiceClock` is `MusicClock`, `SongService.defaultFrequency` is
  `MusicService.DEFAULT_FREQUENCY`.
- Checked: the core, desktop and game tests, the common checks and `assembleDebug`. Not run on
  desktop or a device: the desktop checks stay off on this machine while UI tests are worked on
  there, and no phone was connected. `:verktex:android:testDebugUnitTest` fails before and after
  this phase on JDK 25 (Robolectric).

### Phase 4: localization

- Verktex has `localization/`:
  - `Localization` holds the `locale`, its `fallbackChain` (`pt-BR`, `pt`, `en`), the `sources`, a
    `version` increased on every change, `get`, `getArray` and locale listeners. `normalize` turns
    Android's `pt-rBR` and `pt_BR` into `pt-BR`.
  - `JsonStringSource` reads `strings/<locale>.json` through the new `Provider.read`. It's the
    built-in source, and `addSource` puts others first.
  - `formatString` (also `Localization.format`) handles `%s %d %x %f %.Nf %n$s %% %n` with the `-`,
    `0`, `+` and `,` flags and a width. It's built on `math/toFixed`, which now rounds halves like
    `1.005` up, as `java.util.Formatter` does. The decimal point is always `.`.
  - `LocalizedString` is a `Localized` `CharSequence` resolved on read and cached per `version`;
    `format(args)` gives a formatted one. `LocalizedStringArray` has `values`.
  - `Platform.systemLocale` is the system's language (`Locale.getDefault()` on both platforms).
- Components re-resolve on their own: `UIComponent.bindLocale` bindings run again on the component's
  next update after the `version` changed. That's pulled rather than pushed, so scenes that aren't
  shown (the singleton ones) catch up when they come back.
  - `UIText.text` and `UITextureText.text` are `CharSequence`. A `Localized` one is bound, and reading
    it gives back what was set. Through them, so are the texts of `UITextButton`, `UIDialog`,
    `UIMessageDialog`, `FormControl.label` and `UIToast`.
  - `UIPromptDialog.text`, `UICard.title`, `UISelect.placeholder`, `UISelect.Option.text` and the
    toasts (`Toasts.show`, `showPersistent`, `Toast.text/title`, the action) take a `CharSequence`.
- In the game:
  - `:game:generateStringResources` also writes every language of the pack (and English's
    `strings_temporary_for_locals.xml`) as `strings/<locale>.json` into
    `game/build/generated/resources/assets`, which the app's assets and the desktop's resources
    include. `Res` keeps its shape with `@JvmField` `LocalizedString`/`LocalizedStringArray`, so Java
    uses `Res.string.x` too.
  - `StringTable`, `AndroidStringTable`, `DesktopStringTable`, `StringResource`, the `setText`
    extensions and the desktop's `copyStrings` are gone. Call sites pass `Res.string.x` (or
    `.format(...)`) and take `toString()` only where a `String` is needed (chat history, Android
    dialogs, comparisons, spans).
  - `OptionInfo.title/summary` and `CategoryInfo.title` are `CharSequence`.
  - `Languages` lists the languages and applies the `appLanguage` setting, from `Config.loadConfig`
    and when it's changed in the settings, which updates the text in place. The setting is shown on
    every platform; `OsuPlatform.canChooseLanguage`/`setLanguage` and the restart are gone.
- Left on Android resources: `ImportReplayActivity` and the crash dialog of `AppException`, which run
  without the engine, and the XML layouts of the old fragments, which follow the system's language
  rather than the game's.
- Tests: `LocalizationTest` (Verktex) covers the fallback, the re-resolving and the formatter against
  `java.util.Formatter`; `GeneratedStringsTest` (game) loads the generated files and formats every
  pattern of every language.
- Checked: the core, desktop and game tests, the common checks and `assembleDebug` (the APK has the
  14 `assets/strings/*.json`). Not run on desktop or a device, so changing the language live wasn't
  seen on screen. No Verktex UI test was added for it, since the UI tests are being worked on
  separately.

### Phase 5: settings

- Verktex's `Settings` is typed and persistent:
  - A `SettingKey<T>(name, default, serializer)` is declared with `setting(name, default)`. It's read
    with `Settings[key]` and written with `Settings[key] = value`, and it's a property delegate.
    `observe`/`removeObserver` replace `on`/`off`.
  - The values are a JSON object in `settings.json` in `Platform.storage.dataDirectory`, read on first
    use. Decoded values are cached per key. A value that can't be read as its key's type reads as
    the default (strings like `"true"` and `"75"` do read as booleans and numbers). Unknown keys
    are kept.
  - Saving happens one second after the last change (`Settings.update()`, called by the update
    loop), and right away on `Verktex.pause()`/`dispose()` and on `Settings.flush()`. The file is
    written to `settings.json.tmp`, then `atomicMove`d.
  - Values set before there's a platform (Android's backend choice before `super.onCreate`) are
    kept in memory and go over the file once it's read.
  - `isReadOnly` keeps a process from writing. The Android and desktop UI test browsers set it: they
    read the game's settings but never change them. `isNew` says there was no file.
  - `toJson()` and `putAll()` are for backups and imports.
  - `VerktexSettings` are keys: the overlays, the resolution scale, the graphics backend, and the
    Vulkan crash guard (`VulkanStarting`, flushed right away), which replaces the
    `verktex_graphics` SharedPreferences. The old string-keyed API and `Setting` are gone.
  - Verktex core exposes kotlinx-serialization-json (`api`), since keys carry their serializer.
- In the game:
  - `OsuSettings` declares the 84 settings as `OsuSettingKey`s, with their old names and storage
    types (e.g. `spinnerstyle` stays a string). `bounds` and `isSensitive` are properties of the
    keys and replace `PREFERENCE_BOUNDS` and `SENSITIVE_KEYS`; `FLOAT_PREFERENCES` isn't needed,
    since a float key reads an integer too.
    - Helpers cover the values read in another unit or form: `musicVolume`, `soundVolume`,
      `backgroundBrightness`, `offset`, `cursorSize`, `playfieldSize` and its position,
      `spinnerStyle`, `metronomeSwitch`, `isStoryboardEnabled`, `isStayOnline`, `onlineUsername`,
      `difficultyAlgorithm`, `beatmapLeaderboardScoringMode`, `comboColors` and `installId`.
    - `OsuSettings.load()` replaces `Config.loadConfig()`. It imports the legacy preferences once,
      keeps the folders of a new installation, measures the viewport, subscribes to announcements
      and applies the language.
  - `Config`'s derived state moved out:
    - `GameFolders` has `corePath`, `beatmapPath`, `scorePath`, `cachePath` and `defaultCorePath`,
      computed from the settings with a trailing slash.
    - `SkinFolders` has the skin `path`, the `topPath` and the list of skins.
    - `GameResolution` has the viewport `width`/`height` (the old `RES_WIDTH`/`RES_HEIGHT`). It still
      measures `OsuPlatform.displayWidth/Height`, the real size of the display: Android's
      `Platform.currentDisplay` leaves out the system bars, so moving to it waits for phase 7.
  - `Config` is deleted. Its ~400 references in 77 files use the keys and helpers. The values it
    cached are read live, so the settings menu no longer reloads anything when it's left.
  - `PreferenceControls` take a `SettingKey`. `OptionInfo.setting` replaces `defaultValue` and
    `isFloat` (a `Float` key makes a float slider). The graphics backend option uses the engine's
    `VerktexSettings.GraphicsBackend` directly.
  - Changed behavior:
    - `bursts` and `displayScoreStatistics` default to false, which the game used, while the settings
      showed true.
    - The beatmap mirror, the star rating versions, the vibrations and the song select's sort and
      grouping are settings too.
  - Migration: the first time without `settings.json`, `OsuPlatform.legacyPreferences()` is imported
    (Android: every entry of the default SharedPreferences; desktop: the `osu!droid` node of
    `java.util.prefs`, as strings). After that the old store is left untouched. On Android the
    backend chosen before is read from the old preferences until `settings.json` exists, since the
    engine picks it before the game starts.
  - `SettingsBackup` (common) replaces `ConfigBackup`. It writes the settings minus the sensitive ones
    to `osudroid.cfg` in the game folder, and restores them clamped to their bounds; backups of the
    old preferences restore too. It's offered on every platform.
  - Removed: `OsuPlatform.preferences` and the backup hooks, `MainActivity`'s raw preference writes
    (including the unused `qualitySet`/`lowtextures`/`onlineSet`), `multiplatform-settings`,
    `androidx.preference`, and the old preference screen's styles and layouts.
- Tests: `SettingsTest` (Verktex) covers saving, unknown keys, lenient reading, read-only and
  observers. `OsuSettingsTest` (game) covers the legacy import on both platforms, the backup and the
  folders.
- Checked: the core, desktop and game tests, the app's unit tests compiling, the common checks and
  `assembleDebug`. Not run on desktop or a device, so the upgrade from existing preferences wasn't
  seen on a real install.

### Phase 6: Ktor networking

- Verktex core has Ktor's client (`api`, 3.1.3). The engine is Ktor's default, found on the classpath:
  OkHttp on Android and desktop (`ktor-client-okhttp` in each platform module). It was `Platform.Implementation.httpEngine` first.
  `Network.createClient(configure)` builds a client over it, and `Network.client` is a shared default.
- In the game:
  - The game uses Ktor directly. `httpClient` (`com/osudroid/online/HttpClient.kt`) is the game's
    client:
    - `expectSuccess`, so an unsuccessful response throws Ktor's `ResponseException`.
    - `ContentNegotiation` with `NetworkJson`: the lenient JSON, ignoring unknown keys, coercing nulls
      to defaults and leaving nulls out of requests. It's registered for `ContentType.Any`, since some
      servers answer JSON as `text/html`.
    Responses are `@Serializable` classes read with `body<T>()`: the leaderboard
    (`LeaderboardResponse`), the beatmap status, the banners, the update check, room creation (its
    request too) and the mirrors' searches (`MirrorBeatmapSet`, shared by both mirrors, read by the
    `BeatmapMirrorSearchResponseModel`s from the `HttpResponse`). The lobby's rooms stay a
    `JsonArray`, since `Room` is made from JSON objects as the socket's events make it.
  - The post builders (`PostBuilder`, `URLEncodedPostBuilder`, `FormDataPostBuilder`) are gone: Ktor
    builds the forms. What they added for the osu!droid server is `OnlineManager.post`: it signs the
    URL-encoded values joined with `_`, sends them with `submitForm` (or `submitFormWithBinaryData`
    with the replay of a score), and tries up to 3 times, 3 s apart, until the first line is
    `SUCCESS` or `FAIL`. The unused `getTop` went with them (the leaderboard is `IBanchoAPI`'s).
  - `downloadTo` is the one helper: an `HttpClient` extension streaming `bodyAsChannel()` into an
    Okio sink with a `DownloadProgress` callback. It deletes the file if it doesn't finish, and
    returns false for a `304` (for `If-Modified-Since`).
  - A first version had an `OsuHttp` wrapper with JSON helpers returning `JsonObject`s, which was
    replaced by the above.
  - `async {}` and `delayed {}` take a `suspend` block, so their bodies call the requests directly.
  - Suspend now: `OnlineManager`'s requests (login, score submission, leaderboards, beatmap status,
    avatar and banner downloads; the login lock is a `Mutex`), `OnlineFileOperator.downloadFile`, `IBanchoAPI`, `LobbyAPI`
    and `BannerManager.loadBannerSprite`. `OnlineScoring` uses a `Mutex` and `delay` instead of
    `synchronized` and `Thread.sleep`.
  - The mirrors build their URLs with Ktor's `URLBuilder`. It writes a space in a query as `+`
    rather than `%20`; both decode the same.
  - App callers: `UpdateManager`, `BeatmapDownloader`, `BeatmapListing` (search and covers) and
    `DownloadFragment`, which cancels a download by cancelling its coroutine.
  - Blocking bridges stay only where callers are legacy code on a background thread:
    `GameScene.loadGame`'s replay download (`runBlocking`) and the Java `PushNotificationService`
    (`OnlineFiles.downloadFileBlocking`).
  - Deleted: `com/reco1l/framework/net/` (`Http`, `WebRequest`, `JsonRequest`, `FileRequest`,
    `OkHttpClientImplementation`), its `Http.implementation` setup in the three hosts, `sleepBlocking`,
    and the direct OkHttp dependencies of `:game`, `:game-desktop` and the app (socket.io-client still
    brings it to Android).
  - Not added: a global user agent or timeouts, so the servers see the requests as before (OkHttp's
    defaults; the mirrors still get `Chrome/Android`, `MIRROR_USER_AGENT`).
- Tests: `UrlParametersTest` replaces `HttpUrlBuilderTest`. `HttpClientTest` checks the client's
  configuration over Ktor's `MockEngine`: JSON as `text/html`, unknown fields, coerced nulls, a
  number read as a string, nulls left out of requests, failures, the leaderboard and mirror shapes,
  and downloads with a `304`. `ServerRequestTest` checks `OnlineManager.post`: the form and its
  signature, the multipart replay, and trying again after a failed answer.
- Checked: the core, desktop and game tests, the app's unit tests compiling, the common checks and
  `assembleDebug`. Not run against the servers: login, score submission, leaderboards, the beatmap
  listing, downloads and updates weren't tried.
- Socket.IO follow-up:
  - `KtorRoomSocket` (common code) implements `RoomSocket` over a Ktor WebSocket to
    `/socket.io/?EIO=4&transport=websocket`, without the polling transport. It covers what the rooms
    use:
    - the Engine.IO open, pings and close;
    - the Socket.IO connect to the namespace in the URL's path, with the `auth` payload;
    - events with JSON arguments, and connect errors;
    - the server's disconnect, and the client's (`io client disconnect`).
    It doesn't reconnect by itself (as before), acknowledge events or exchange binary data, and
    dies on a missed ping deadline (`ping timeout`).
  - Listeners get strings, numbers (an `Int` when it fits, as the Java client gave), booleans, null,
    `JsonObject`s and `JsonArray`s, as they did. They're called from the connection's coroutine in
    order.
  - It's `RoomSocket.factory` by default, so desktop shows multiplayer now (`RoomSocket.isAvailable`, which hid it
    without an HTTP engine, is gone); it connects, but nothing beyond the socket was
    tried.
  - Deleted: `SocketIORoomSocket`, its setup in `MainActivity` and `OsuUITestActivity`, and
    `socket.io-client`. OkHttp now remains only as Ktor's engine.
  - `KtorRoomSocketTest` runs against a real Socket.IO server given with
    `-PsocketIoTestUrl=http://...`, and is skipped without one. It passed against socket.io 4.8.1
    (Node), with room namespaces, auth, events in both directions, a server disconnect, a refused
    connection, a client disconnect and 300 ms pings. The game's server wasn't tried.

### Phase 7: split `OsuPlatform`

- Verktex's `Platform` has, each with a default for platforms without it:
  - `application` (`versionName`, `isDebug`), from the package on Android and from the host on
    desktop (`DesktopGameHost.application`). Readable without a platform (the defaults).
  - `isInForeground`, which `AndroidGameHost` tracks between `onResume` and `onPause`.
  - `isLowEndDevice`: without OpenGL ES 3 or with less than 2 GB of memory, on Android.
  - `availableGraphicsBackends`: Auto, OpenGL ES 3 and 2, and Vulkan where the device reports
    Vulkan 1.1, on Android; none on desktop. `isVulkanAvailable` and `resetVulkanCrashGuard` are
    internal to `verktex:android`, which resets the guard by itself when Vulkan is picked again.
  - `haptics` (`Platform.Haptics`): one-shot and waveform vibrations with amplitudes, null without
    a vibrator. `AndroidHaptics` covers Android 7 without amplitudes.
  - `canForceMaxRefreshRate` and `forceMaxRefreshRate`, moved from `MainActivity` to
    `AndroidPlatform` (Android 12 and up).
  - `canPickFiles` and `pickFiles(mimeType)`, which copies the picked files to the cache and gives
    their paths, on Android.
  - `runOnMainThread`, `exit()` (finishes the activity; closes the window on desktop) and
    `copyToClipboard`.
  - `onUncaughtException`, defaulting to the platform's `handleUncaughtException` (Android: the
    thread's handler, which reports the crash; elsewhere: logged). The Android UI test browser sets
    it to log, replacing the headless check.
  - Android's `currentDisplay` is the real size of the display, system bars included, which the game
    used for its viewport.
- The game:
  - `OsuGameHost : GameHost` (common code) has the game's hooks: `buildType`, `legacyPreferences`,
    announcements, analytics, the library and skin imports, `checkSkinTexture`, the beatmap listing
    and downloads, the replay import, the block area editor, `createVideo`, `verifyFileIntegrity`,
    updates and the changelog. `OsuGameHost.current` is `Verktex.host`, or a host without features
    when there's no engine (unit tests).
  - `AndroidOsuGameHost` (app) is the activity with the Android hooks, which `MainActivity` extends;
    its own `loadBeatmapLibrary`/`checkNewSkins` implement those hooks. `OsuDesktop`,
    `OsuUITestActivity` and `OsuUITestsDesktop` implement `OsuGameHost` with what they offer.
  - `GameplayVibrations` is common, over `Platform.haptics` and the settings, which it reads on each
    vibration (so the settings menu no longer pushes them). Before, `VibratorManager.init` was never
    called, so gameplay vibrations didn't happen on Android at all; now they do.
  - Deleted: `OsuPlatform`, `AndroidOsuPlatform`, `DesktopOsuPlatform`, `VibratorManager` and the
    `com.acivev.utils` device checks, `MainActivity`'s refresh-rate and visibility code, and
    `initializeDesktopPlatform`. Desktop exits the process after the window closes, as before.
  - The replay import picks its files with `Platform.pickFiles` and shows its progress with toasts
    instead of a loading fragment and snackbars. It stays in the app behind the
    `pickAndImportReplays` hook until `ReplayImporter` (which needs the app's replay pack code) is
    common.
  - The graphics backend option is hidden where there's no choice (desktop).
- Not done: a desktop clipboard (GLFW's must be set from the main thread) and file picker.
- Checked: the core, desktop and game tests, the app's unit tests compiling, the common checks and
  `assembleDebug`. Not run on a device or desktop: vibrations, the refresh rate, exiting, the
  clipboard and the low-end effects weren't seen.

### Phase 8: no `GlobalManager`, no singleton scenes

Done in steps, each committed on its own.

**Step 1: `OsuGame` and `BeatmapSelection`.**
- `OsuGame` (jvmMain, since the scenes are) replaces `GlobalManager`. It has `init()`, `music`,
  `isHeadless`, and for now `gameScene`/`scoring`, which step 3 replaces.
- `BeatmapSelection` (common) is the single owner of the selected beatmap, with listeners.
  - It replaces `GlobalManager.selectedBeatmap` and `SongSelect.selectedBeatmap`.
  - `MusicService.currentBeatmap` reads and sets it, and its beatmap listeners became the
    selection's.
  - The syncing in `MainScene` (the Solo button and `onEnter`) and in `SongSelect` is gone. Song
    select now sets the selection right away rather than from its loading coroutine.
- The startup progress and message are `SplashScene.progress`/`info`.
- `GlobalManager` is deleted (about 150 references in 34 files).

- Checked: the game tests, the common check, the app's unit tests compiling and `assembleDebug`. Not
  seen on screen.

**Step 2: the selected mods without a singleton.**
- The selected mods are a plain `ModHashMap` kept by who selects them: song select's for solo play
  (`SongSelect.mods`, which is the room's in multiplayer) and `RoomScene.mods` in a room.
  `GameScene` gets them in `startGame`, which copies them. Once song select is created when pushed,
  the solo mods reset each time it's opened, which the user accepted.
- `ModHashMap.version` increases on every change of its mods, and what shows them (the mod menu,
  song select's mods indicator) compares it each frame instead of listening. This also makes the
  changes from the room's socket thread safe to show.
- The room rules are extensions in `multiplayer/PlayerMods.kt`: `clearPlayerMods()` keeps what
  only the host can change and ScoreV2; `setRoomMods(mods, isFreeMod)` replaces `ModMenu.setMods`.
- `ModMenu(mods)` is created by its owner, which sets `onBack` and `onDifficultyCalculated`. The
  room's `closeModMenu`/`sendMods` replace the room code `ModMenu.back` had, and the back key goes
  through `SongSelect.back` and `RoomScene.onBackPressed`. `SongSelect.updateMusicEffects` became
  `OsuGame.applyModsToMusic(mods)`.
- Checked: the game tests (with a `ModHashMap.version` test), the common check, `assembleDebug` and
  the `Scenes/Mod menu` UI test on desktop with mods selected before it opened. Not run on a device
  or in a room. The app's `MigrationTest` fails on Firebase not being initialized, unrelated.

**Step 3: `SplashScene`, `CalibrationScene` and `MainScene` as classes.**
- The hosts create them. `SplashScene.progress`/`info` are in its companion, since the startup
  code reports to them before and without the scene.
- `Verktex.resetScenes(scene)` replaces the whole stack (through `SceneHost.resetScenes` when a
  host is set). The hosts reset it to the main menu once the splash ends, so the splash no longer
  stays below it, and `forcedExit` does too instead of pushing the menu a second time.
- `MainActivity` keeps the `MainScene` it created, for the exit dialog and `forcedExit`.
  `MainScene.watchReplay` is in its companion, since it doesn't need the menu.
- `MainScene` follows the selected beatmap only while it's the current scene, and catches up on
  enter. Its background texture key index is in its companion, so that a new menu doesn't load
  over a texture an old one may still show.
- `CalibrationScene(offsetMin, offsetMax)` takes its range instead of the settings setting it on
  the object. Its BPM resets to 60 each time it's opened.
- The HUD editor returns to the scene under the settings rather than to `MainScene`.
- Checked: the game and desktop tests, the common check, `assembleDebug`, and the desktop game
  reaching the menu with the beatmap's background. The engine's `ColorInheritanceTest` and
  `PointerEventTest` fail before and after this step. Calibration wasn't opened.

**Step 4: `SongSelect` as a class.**
- Created when opened (by the menu and the room), with `loadBeatmaps()` called by its
  creator as before. The startup no longer preloads one.
- What it keeps across visits is what the settings and the selection already kept: the sort
  order, the grouping and the selected beatmap, which it scrolls to when opened. The search text,
  the favourites and collection filters and the solo mods start over.
- It hands its leaderboard to the play (`GameScene.leaderboardItems`/`isLeaderboardOnline`) instead
  of the HUD reading song select. `DifficultyCalculationManager` reloads the shown song select only.
- Measured on desktop with 21 beatmap sets: creating it takes about 7 ms warm (115 ms the first
  time, class loading) and `loadBeatmaps` about 1.5 ms, the filter running off the update thread as
  before. Not measured on a device or with a large library.

**Step 5: `GameScene` and `ScoringScene` per play.**
- `OsuGame.gameScene` and `OsuGame.scoring` are gone. Who starts a play (song select, the room, the
  HUD editor, `QuickPlay`, `MainScene.watchReplay`) creates its `GameScene` with
  `OsuGame.createGameScene()`, which the UI test browser replaces with its `FakeGameScene`.
- A `GameScene` is one play of a beatmap with its retries and the replays watched from its results,
  since it keeps the parsed beatmap and the slider caches for them. Its `ScoringScene` is its
  `scoring`. So `sliderRenderPaths` stays in the scene instead of moving to the loader.
- `GameScene.current` and `ScoringScene.current` are the play and the results of the current scene,
  read from `Verktex.scene`, for the activity's keys, the chat and multiplayer events. The room
  keeps the match it started (`RoomScene.game`) for its final leaderboard.
- The HUD takes its game, and so do the elements that need it (`HUDElements.create(game)`): the
  hit error meters, the back button and the leaderboard.
- A new play stops the loading of the play started before it, which the shared scene used to do
  for itself: both loads use global state (the object pool, `GameHelper`).
- `GameScene.setOldScene` is removed, nothing read it.
- `ScoringScene.replayID` is never set since the port to Verktex, see the known regressions.
- Checked: the game and desktop tests, the common check, `assembleDebug`, an autoplay on desktop
  with its HUD, and the HUD elements UI test. Not checked: the results screen, retries, replays,
  multiplayer and anything on a device.


