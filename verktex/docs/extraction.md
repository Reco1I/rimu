# Extracting Verktex

Verktex lives in osu!droid for now, but it's meant to become its own Kotlin Multiplatform project, of
which the game only uses the core and the Android platform. The `verktex/` folder is already laid out
as that project:

```
verktex/
  core/         :verktex:core         Platform independent: scenes, UI, graphics abstractions, audio, input, clocks.
                                      Kotlin Multiplatform, its code is in src/commonMain. Its assets/ folder
                                      holds the built-in shaders and fonts, which each platform packages.
  android/      :verktex:android      The Android platform: graphics backends (GLES, Vulkan), windowing, input,
                                      SPIR-V compilation of the built-in shaders.
  bass/         :verktex:bass         The BASS audio library, used by the Android audio driver.
  shadertools/  :verktex:shadertools  The shader translator, used by the Vulkan backend and by the build.
  docs/
```

The game depends only on `:verktex:android`, which exposes `:verktex:core` as an `api` dependency.

## Rules

- Verktex never depends on the game.
- `:verktex:core` never depends on Android or the JVM. Its code is common Kotlin, in `commonMain`,
  and the few platform specific declarations are expected there and actual in `jvmMain`, e.g.
  `fairReentrantLock()`. The JVM is its only target for now, which also lets common code use the
  JVM's API, so check it with `./gradlew -Pverktex.checkCommon :verktex:core:compileKotlinJs`, which
  compiles it for JavaScript as well.
- What the core needs from a platform goes through `Platform.Implementation`: resources (`Provider`),
  audio (`AudioDriver`), the display, the keyboard, logging and threads (`LoopWorker`, which also
  paces its loop). Everything else comes from multiplatform libraries: DitchOoM's `buffer` for the
  pixels textures upload (`ReadBuffer`, `asReadBuffer()`/`asByteBuffer()` on Android), atomicfu for
  locks, `kotlin.time` for time, and small helpers in the core (`toFixed`, the code points in
  `ui/text/CodePoints.kt`).
- Everything the engine needs at runtime, such as shaders and fonts, ships in its own modules, and so
  do the tasks that build it, such as `compileVulkanShaders` in `:verktex:android`.
- The core keeps the API only the platforms should use public under `@VerktexPlatformApi`, which
  both engine modules opt into. Applications shouldn't.

## What's left

1. **Building on its own.** Give `verktex/` its own `settings.gradle.kts` and plugin versions, and
   have the game consume it with `includeBuild("verktex")` and dependency substitution, which is how
   it'll look once it's a separate repository. The lookup of `verktex.validationLayers` in
   `local.properties` will then need a fallback to the game's.
2. **More targets.** Adding a target to `:verktex:core` only takes the actual declarations of
   `jvmMain` (for now `fairReentrantLock()`), and a platform module implementing
   `Platform.Implementation`, like `:verktex:android` does.
3. **Shared shaders.** Done: the built-in shaders and fonts are in `verktex/core/assets/`, a plain
   folder rather than the core's resources, so they aren't packaged twice in the APK (as assets and
   as jar resources). Each platform packages it itself, see `desktop-platform.md`.
