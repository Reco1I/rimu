# Extracting Verktex

Verktex is its own Gradle build, in the `verktex` folder of osu!droid for now and meant to move to
its own repository, which the game will then check out in the same place as a git submodule:

```
verktex/
  settings.gradle.kts, build.gradle.kts, gradle/libs.versions.toml, gradlew
  verktex/          :verktex          The engine, Kotlin Multiplatform with an android and a desktop target.
    src/commonMain    Platform independent: scenes, UI, graphics abstractions, audio, input, clocks.
    src/jvmMain       What both platforms share because they run on a JVM.
    src/androidMain   The Android platform: graphics backends (GLES, Vulkan), windowing, input, the
                      BASS audio library (com.un4seen.bass and its natives).
    src/desktopMain   The desktop platform: OpenGL 3.3 (LWJGL, GLFW), BASS through JNA. Runs the UI
                      tests, see desktop-platform.md.
    assets/           The built-in shaders and fonts, which each platform packages.
  android-natives/  :android-natives  The Vulkan backend's native library, SPIR-V compilation of the
                                      built-in shaders, the validation layer of debug builds.
  shadertools/      :shadertools      The shader translator, used by the Vulkan backend and by the build.
  maven/            Libraries published nowhere, see its README.
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
implementation 'com.reco1l.verktex:verktex'
```

- The dependency resolves to the Android variant in an Android module and to the desktop one in
  a JVM module. A Kotlin Multiplatform module needs the same two targets, `android` and
  `jvm("desktop")`, and can share code between them in a `jvm` source set as Verktex does.
- An Android application also lists `verktex/maven` as a repository, see its README.
- Verktex's tasks run from the application as `:verktex:<project>:<task>`, e.g.
  `./gradlew :verktex:verktex:desktopTest`, and from a script as
  `gradle.includedBuild("verktex").task(":verktex:prepareFFmpeg")`.
- The Android Gradle plugin reads the SDK's path from the `local.properties` of each build, so
  Verktex needs its own (or `ANDROID_HOME`). osu!droid's `settings.gradle` copies the game's when
  it's missing. `verktex.validationLayers` is read from it too.
- Both builds have to use the same versions of the Android and Kotlin Gradle plugins, and the same
  `android.*` flags in `gradle.properties`.

## Rules

- Verktex never depends on the game.
- `commonMain` never depends on Android or the JVM. With only JVM targets it could still use the
  JVM's API, so check it with `./gradlew -Pverktex.checkCommon :verktex:compileKotlinJs`, which
  compiles it for JavaScript as well.
- What the common code needs from a platform is expected there and actual in the source set of each
  target. `Platform` is an `expect object`: resources (`Provider`), audio (`AudioDriver`), the
  display, the keyboard, threads (`LoopWorker`, which also paces its loop), the storage folders
  (`Platform.storage`), the clipboard, haptics, file picking and video decoders. Its actual objects
  are `Platform.android.kt` and `Platform.desktop.kt`, which the host of each platform attaches
  itself to when it starts the engine (`Platform.attach`, internal to the module). Until then only
  what needs no host works, which is what unit tests rely on.
- An interface is kept instead where one platform has several implementations, or where tests
  replace it: `GraphicsRenderer` and `GraphicsDevice` (GLES 2, GLES 3, Vulkan), `AudioDriver`,
  `VideoDecoder`, `Logger.Implementation`.
- Everything else comes from multiplatform libraries or small expected declarations: Okio for files
  (`Platform.fileSystem`), `PixelBuffer` for the pixels textures upload (a `java.nio.ByteBuffer` on
  the JVM), atomicfu for locks (`fairReentrantLock()`), `kotlin.time` for time, and small helpers
  (`toFixed`, the code points in `ui/text/CodePoints.kt`).
- `PixelBuffer` is expected as an abstract class, which `java.nio.ByteBuffer` is on Android and up
  to Java 16; from Java 17 it's sealed, which wouldn't match. The desktop target is therefore
  compiled against the API of Java 11 (`-Xjdk-release=11`).
- What the Kotlin Multiplatform Android plugin can't build lives in `:android-natives`, a plain
  Android library `androidMain` depends on: the CMake build of the Vulkan backend, and the tasks
  that need the NDK or build types (`compileVulkanShaders`, `copyValidationLayers`).
- Everything the engine needs at runtime, such as shaders and fonts, ships in its own modules.
- The API only the platform sources should use is public under `@VerktexPlatformApi`, which the
  module opts into. Applications shouldn't.

## What's left

1. **Building on its own.** Done, see above.
2. **More targets.** Adding a target takes its actual declarations (`Platform`, `PixelBuffer`,
   `fairReentrantLock()`, `systemFileSystem`) and its graphics and audio implementations, in a
   source set of its own. The JavaScript target of `verktex.checkCommon` has stubs of them.
3. **Shared shaders.** Done: the built-in shaders and fonts are in `../verktex/assets`, a plain
   folder rather than the core's resources, so they aren't packaged twice in the APK (as assets and
   as jar resources). Each platform packages it itself, see `desktop-platform.md`.

4. **Its own repository.** Move this folder, with its history, to a repository, and check it out
   here as a submodule. Nothing else changes for the game.

Features the game still implements itself (settings, localization, toasts, networking, the database,
paths, video) are being moved into Verktex, see `engine-consolidation.md`.
