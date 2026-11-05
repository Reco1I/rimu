# Extracting Verktex

Verktex lives in osu!droid for now, but it's meant to become its own Kotlin Multiplatform project, of
which the game only uses the core and the Android platform. The `verktex/` folder is already laid out
as that project:

```
verktex/
  core/         :verktex:core         Platform independent: scenes, UI, graphics abstractions, audio, input, clocks.
  android/      :verktex:android      The Android platform: graphics backends (GLES, Vulkan), windowing, input,
                                      the built-in shaders and fonts (src/main/assets), SPIR-V compilation.
  bass/         :verktex:bass         The BASS audio library, used by the Android audio driver.
  shadertools/  :verktex:shadertools  The shader translator, used by the Vulkan backend and by the build.
  docs/
```

The game depends only on `:verktex:android`, which exposes `:verktex:core` as an `api` dependency.

## Rules

- Verktex never depends on the game.
- `:verktex:core` never depends on Android, including `org.json`, which ships with the Android SDK.
  It's a plain Kotlin/JVM module, so the compiler enforces this.
- Everything the engine needs at runtime, such as shaders and fonts, ships in its own modules, and so
  do the tasks that build it, such as `compileVulkanShaders` in `:verktex:android`.
- The core keeps the API only the platforms should use public under `@VerktexPlatformApi`, which
  both engine modules opt into. Applications shouldn't.

## What's left

1. **Building on its own.** Give `verktex/` its own `settings.gradle.kts` and plugin versions, and
   have the game consume it with `includeBuild("verktex")` and dependency substitution, which is how
   it'll look once it's a separate repository. The lookup of `verktex.validationLayers` in
   `local.properties` will then need a fallback to the game's.
2. **Kotlin Multiplatform.** Turn `:verktex:core` into a `kotlin("multiplatform")` module, with
   `commonMain` and the `jvm`/`androidTarget` targets. Its JVM-only APIs have to move to expected
   declarations or multiplatform libraries first:
   - `java.nio` buffers (`ByteBuffer`, `FloatBuffer`, …), used throughout `graphics/`.
   - `java.util.concurrent` locks and atomics, `@Synchronized` and `Thread` (`Verktex.kt`,
     `GraphicsRenderer.kt`, `GraphicsTexture.kt`).
   - `java.util.function`.
3. **Shared shaders.** The shaders only ship with the Android platform, since it's the only one
   loading them. Once there's a second platform, they move to the core's resources.
