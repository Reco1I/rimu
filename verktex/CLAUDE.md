# CLAUDE.md

Guidance for working on Verktex, a Kotlin Multiplatform game engine. It is its own Gradle build:
applications include it (`includeBuild`) and depend on `com.reco1l.verktex:<module>`, see
`docs/extraction.md`.

## Build Commands

```bash
# Everything
./gradlew build

# Tests
./gradlew :core:jvmTest :android:testDebugUnitTest :shadertools:test :desktop:test

# The core must compile as common code
./gradlew -Pverktex.checkCommon :core:compileKotlinJs

# The UI test browser on desktop; --test opens a test, --screenshot saves a frame and exits
./gradlew :desktop:run --args="--test 'Verktex/Text' --screenshot build/text.png"
```

Requires **Java 17**, and for `:android` the **Android NDK 22.1.7171670** and the **android-36.1**
SDK platform, with `sdk.dir` in `local.properties` or `ANDROID_HOME`. From an application that
includes this build the same tasks are `:verktex:<project>:<task>`.

## Modules

- `:core` (`core/`): everything platform independent: rendering, audio, input, clocks (`time/`),
  the UI components (`UIComponent`, `UIContainer`, `UIScene`, `UISprite`…), settings, localization
  and networking. Kotlin Multiplatform with only the JVM as a target for now. Its code is in
  `src/commonMain/`, with a few expected declarations actual in `src/jvmMain/`. The built-in shaders
  and fonts are in `core/assets/`, a plain folder each platform packages.
- `:android` (`android/`): the Android platform (`com/reco1l/verktex/android/`): the graphics
  backends (OpenGL ES 2 and 3, Vulkan with its native code in `src/main/cpp`), the BASS audio
  library (`com.un4seen.bass` and its natives), ExoPlayer video, and the task that compiles the
  shaders to SPIR-V. Brings `:core` as an `api` dependency.
- `:desktop` (`desktop/`): the desktop platform: OpenGL 3.3 through LWJGL, BASS through JNA, FFmpeg
  video, and the UI test browser, see `docs/desktop-platform.md`.
- `:shadertools` (`shadertools/`): the shader translator, used by the Vulkan backend and the build.

## Conventions

- Verktex never depends on an application, in code or in its build: no paths outside this folder.
- `:core` never depends on Android or the JVM (including `org.json`). What it needs from a
  platform goes through `Platform.Implementation`, everything else through multiplatform libraries.
- API only the platforms should use is public under `@VerktexPlatformApi`.
- Dependency and plugin versions go in `gradle/libs.versions.toml`.
- UI is a tree of `UIComponent`s measured and laid out through `LayoutConstraints`, animated with
  transforms, and with `Choreography` for anything whose state must depend only on the time.
- Textures and shader outputs are premultiplied, see `docs/texture-blending.md`.
- UI tests are `UITestSuite`s (`core/.../testing/`), browsed by `UITestActivity` on Android and
  `UITestApplication` on desktop; `VerktexUITests` covers the engine's components.

## Docs

`docs/extraction.md` (layout and rules), `docs/desktop-platform.md`, `docs/vulkan-backend.md`,
`docs/frame-rate.md`, `docs/texture-blending.md`, `docs/engine-consolidation.md`.
