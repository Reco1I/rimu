# Verktex

A Kotlin Multiplatform game engine: scenes, a UI tree with layout and transforms, graphics backends
(OpenGL ES 2 and 3 and Vulkan on Android, OpenGL 3.3 on desktop), audio over BASS, input, clocks,
settings, localization, networking and video.

| Module | |
| --- | --- |
| `:core` | Everything platform independent, common Kotlin. |
| `:android` | The Android platform. Brings `:core`. |
| `:desktop` | The desktop platform (LWJGL), which also runs the UI test browser. |
| `:shadertools` | The shader translator, used by the Vulkan backend and by the build. |

## Building

Requires Java 17, and for `:android` the Android SDK (platform `android-36.1`) and NDK
`22.1.7171670`, with the SDK's path in `local.properties` (`sdk.dir=…`) or in `ANDROID_HOME`.

```bash
./gradlew build

# Tests
./gradlew :core:jvmTest :android:testDebugUnitTest :shadertools:test :desktop:test

# The core compiles as common code, without the JVM's API
./gradlew -Pverktex.checkCommon :core:compileKotlinJs

# The UI test browser, on desktop
./gradlew :desktop:run
./gradlew :desktop:run --args="--test 'Verktex/Text' --screenshot build/text.png"
```

## Using it

Applications include this build and depend on `com.reco1l.verktex:android` or
`com.reco1l.verktex:desktop`, see [docs/extraction.md](docs/extraction.md).

## Documentation

- [docs/extraction.md](docs/extraction.md): the layout of the project and its rules.
- [docs/desktop-platform.md](docs/desktop-platform.md): the desktop platform.
- [docs/vulkan-backend.md](docs/vulkan-backend.md): the Vulkan backend.
- [docs/frame-rate.md](docs/frame-rate.md): how the frame rate is capped on each platform.
- [docs/texture-blending.md](docs/texture-blending.md): premultiplied textures and the atlas.
- [docs/engine-consolidation.md](docs/engine-consolidation.md): features moved from osu!droid.
