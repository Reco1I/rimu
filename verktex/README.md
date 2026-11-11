# Verktex

A Kotlin Multiplatform game engine: scenes, a UI tree with layout and transforms, graphics backends
(OpenGL ES 2 and 3 and Vulkan on Android, OpenGL 3.3 on desktop), audio over BASS, input, clocks,
settings, localization, networking and video.

| Module | |
| --- | --- |
| `:verktex` | The engine, with an `android` and a `desktop` target over common code. |
| `:android-natives` | The native code of the Android platform, and its compiled shaders. |
| `:shadertools` | The shader translator, used by the Vulkan backend and by the build. |

## Building

Requires Java 17, and for the Android target the Android SDK (platform `android-36.1`) and NDK
`22.1.7171670`, with the SDK's path in `local.properties` (`sdk.dir=…`) or in `ANDROID_HOME`.

```bash
./gradlew build

# Tests
./gradlew :verktex:desktopTest :verktex:testAndroidHostTest :shadertools:test

# The UI test browser, on desktop
./gradlew :verktex:run
./gradlew :verktex:run --args="--test 'Verktex/Text' --screenshot build/text.png"
```

## Using it

Applications include this build and depend on `com.reco1l.verktex:verktex`, whose Android or
desktop variant Gradle picks for each of their modules, see [docs/extraction.md](docs/extraction.md).

## Documentation

- [docs/extraction.md](docs/extraction.md): the layout of the project and its rules.
- [docs/desktop-platform.md](docs/desktop-platform.md): the desktop platform.
- [docs/vulkan-backend.md](docs/vulkan-backend.md): the Vulkan backend.
- [docs/frame-rate.md](docs/frame-rate.md): how the frame rate is capped on each platform.
- [docs/texture-blending.md](docs/texture-blending.md): premultiplied textures and the atlas.
- [docs/engine-consolidation.md](docs/engine-consolidation.md): features moved from osu!droid.
