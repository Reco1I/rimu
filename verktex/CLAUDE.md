# CLAUDE.md

Guidance for working on Verktex, a Kotlin Multiplatform game engine. It is its own Gradle build:
applications include it (`includeBuild`) and depend on `com.reco1l.verktex:<module>`, see
`docs/extraction.md`.

## Build Commands

```bash
# Everything
./gradlew build

# Tests
./gradlew :verktex:desktopTest :verktex:testAndroidHostTest :shadertools:test

# The UI test browser on desktop; --test opens a test, --screenshot saves a frame and exits
./gradlew :verktex:run --args="--test 'Verktex/Text' --screenshot build/text.png"
```

Requires **Java 17**, and for the Android target the **Android NDK 22.1.7171670** and the **android-36.1**
SDK platform, with `sdk.dir` in `local.properties` or `ANDROID_HOME`. From an application that
includes this build the same tasks are `:verktex:<project>:<task>`.

## Modules

- `:verktex` (`verktex/`): the engine, one Kotlin Multiplatform module with an `android` and a
  `desktop` target. Its source sets:
  - `commonMain`: everything platform independent: rendering, audio, input, clocks (`time/`), the UI
    components (`UIComponent`, `UIContainer`, `UIScene`, `UISprite`…), settings, localization and
    networking.
  - `jvmMain`: what Android and desktop share because both run on a JVM (`ThreadLoopWorker`, the
    actual `PixelBuffer`, the HTTP engine).
  - `androidMain`: the Android platform (`com/reco1l/verktex/android/`): the graphics backends
    (OpenGL ES 2 and 3, Vulkan), the BASS audio library (`com.un4seen.bass`, in Java, and its
    natives in `jniLibs/`) and ExoPlayer video.
  - `desktopMain`: the desktop platform: OpenGL 3.3 through LWJGL, BASS through JNA, FFmpeg video,
    and the UI test browser, see `docs/desktop-platform.md`.
  - `desktopTest` (the common and desktop tests) and `androidHostTest` (Robolectric).
  - `assets/`: the built-in shaders and fonts, a plain folder each platform packages.
- `:android-natives`: what the Android platform needs from the NDK, which the Kotlin Multiplatform
  Android plugin can't build: the native library of the Vulkan backend (`src/main/cpp`), the task
  that compiles the shaders to SPIR-V, and the Vulkan validation layer of debug builds.
- `:shadertools` (`shadertools/`): the shader translator, used by the Vulkan backend and the build.

## Conventions

- Verktex never depends on an application, in code or in its build: no paths outside this folder.
- `commonMain` never depends on Android or the JVM (including `org.json`). What differs per
  platform is expected there and actual in each target's source set: `Platform` is an
  `expect object`, which `AndroidGameHost` and `DesktopGameHost` attach themselves to
  (`Platform.attach`). Everything else comes from multiplatform libraries.
- Interfaces are kept where a platform has several implementations or tests replace them:
  `GraphicsRenderer`, `AudioDriver`, `VideoDecoder`, `Logger.Implementation`.
- The desktop target compiles against the API of Java 11 (`-Xjdk-release=11`): `PixelBuffer` is
  expected abstract, as `java.nio.ByteBuffer` is on Android and up to Java 16.
- API only the platform sources should use is public under `@VerktexPlatformApi`.
- Dependency and plugin versions go in `gradle/libs.versions.toml`.
- UI is a tree of `UIComponent`s measured and laid out through `LayoutConstraints`, animated with
  transforms, and with `Choreography` for anything whose state must depend only on the time.
- Textures and shader outputs are premultiplied, see `docs/texture-blending.md`.
- UI tests are `UITestSuite`s (`commonMain/.../testing/`), browsed by `UITestActivity` on Android
  and `UITestApplication` on desktop; `VerktexUITests` covers the engine's components.

## Docs

`docs/extraction.md` (layout and rules), `docs/desktop-platform.md`, `docs/vulkan-backend.md`,
`docs/frame-rate.md`, `docs/texture-blending.md`, `docs/engine-consolidation.md`.
