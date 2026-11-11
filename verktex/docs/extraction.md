# Extracting Verktex

Verktex is its own Gradle build, in the `verktex` folder of osu!droid for now and meant to move to
its own repository, which the game will then check out in the same place as a git submodule:

```
verktex/
  settings.gradle.kts, build.gradle.kts, gradle/libs.versions.toml, gradlew
  core/         :core         Platform independent: scenes, UI, graphics abstractions, audio, input, clocks.
                              Kotlin Multiplatform, its code is in src/commonMain. Its assets/ folder
                              holds the built-in shaders and fonts, which each platform packages.
  android/      :android      The Android platform: graphics backends (GLES, Vulkan), windowing, input,
                              the BASS audio library (com.un4seen.bass and its natives),
                              SPIR-V compilation of the built-in shaders.
  shadertools/  :shadertools  The shader translator, used by the Vulkan backend and by the build.
  desktop/      :desktop      The desktop platform: OpenGL 3.3 (LWJGL, GLFW), BASS through JNA. Runs
                              the UI tests, see desktop-platform.md.
  docs/
```

It builds alone (`./gradlew build` in this folder), with its own plugin versions, repositories and
version catalog, and nothing in it refers to the build that includes it.

## Using it from an application

The modules have the coordinates `com.reco1l.verktex:<module>`. An application includes the build
and depends on them, and Gradle substitutes the projects:

```groovy
// settings.gradle
includeBuild('verktex')

// build.gradle
implementation 'com.reco1l.verktex:android'   // brings :core as an api dependency
```

- An Android application also lists `verktex/android/maven` as a repository, see its README.
- Verktex's tasks run from the application as `:verktex:<project>:<task>`, e.g.
  `./gradlew :verktex:core:jvmTest`, and from a script as
  `gradle.includedBuild("verktex").task(":desktop:prepareFFmpeg")`.
- The Android Gradle plugin reads the SDK's path from the `local.properties` of each build, so
  Verktex needs its own (or `ANDROID_HOME`). osu!droid's `settings.gradle` copies the game's when
  it's missing. `verktex.validationLayers` is read from it too.
- Both builds have to use the same versions of the Android and Kotlin Gradle plugins, and the same
  `android.*` flags in `gradle.properties`.

## Rules

- Verktex never depends on the game.
- `:verktex:core` never depends on Android or the JVM. Its code is common Kotlin, in `commonMain`,
  and the few platform specific declarations are expected there and actual in `jvmMain`, e.g.
  `fairReentrantLock()`. The JVM is its only target for now, which also lets common code use the
  JVM's API, so check it with `./gradlew -Pverktex.checkCommon :core:compileKotlinJs`, which
  compiles it for JavaScript as well.
- What the core needs from a platform goes through `Platform.Implementation`: resources (`Provider`),
  audio (`AudioDriver`), the display, the keyboard, logging and threads (`LoopWorker`, which also
  paces its loop), and the storage folders (`Platform.storage`). Everything else comes from
  multiplatform libraries or small expected declarations: Okio for files (`Platform.fileSystem`, which
  needs no implementation), `PixelBuffer` for the pixels textures upload (a `java.nio.ByteBuffer` on
  the JVM), atomicfu for locks, `kotlin.time` for time, and small helpers in the core (`toFixed`, the
  code points in `ui/text/CodePoints.kt`).
- Everything the engine needs at runtime, such as shaders and fonts, ships in its own modules, and so
  do the tasks that build it, such as `compileVulkanShaders` in `:verktex:android`.
- The core keeps the API only the platforms should use public under `@VerktexPlatformApi`, which
  both engine modules opt into. Applications shouldn't.

## What's left

1. **Building on its own.** Done, see above.
2. **More targets.** Adding a target to `:verktex:core` only takes the actual declarations of
   `jvmMain` (for now `fairReentrantLock()`), and a platform module implementing
   `Platform.Implementation`, like `:verktex:android` and `:verktex:desktop` do. Both JVM platforms
   share the core's `jvmMain`, e.g. `ThreadLoopWorker`.
3. **Shared shaders.** Done: the built-in shaders and fonts are in `../core/assets`, a plain
   folder rather than the core's resources, so they aren't packaged twice in the APK (as assets and
   as jar resources). Each platform packages it itself, see `desktop-platform.md`.

4. **Its own repository.** Move this folder, with its history, to a repository, and check it out
   here as a submodule. Nothing else changes for the game.

Features the game still implements itself (settings, localization, toasts, networking, the database,
paths, video) are being moved into Verktex, see `engine-consolidation.md`.
