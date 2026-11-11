# Desktop platform

Verktex's second platform, the desktop JVM, in `:verktex:desktop`: OpenGL 3.3 for graphics (LWJGL
and GLFW) and BASS for audio (through JNA). It proves the split of `Platform.Implementation` works,
and makes it quick to try components without a phone. For now it only runs Verktex's own UI tests
(`UITestScene` with `VerktexUITests`), not the game.

## Status

Every step below is done. The UI tests run on Windows with OpenGL and BASS:

```bash
./gradlew :verktex:desktop:run --args="--test 'Verktex/Text'"
```

In Android Studio, the shared run configuration "UI Tests (Desktop)" runs the same thing. Add
`--args="…"` to its script parameters to pass the options.

- `--test <suite/test>` opens a test first, `suite/test` or just its name. Without it, the last
  opened test is shown again.
- `--screenshot <file.png>` saves the frame shown 1.5 seconds after starting and exits.
- `--vsync` caps the frame rate to the display's refresh rate. Without it, frames are drawn as
  fast as possible.

Checked by unit tests and by screenshots of the Text, Buttons, Controls, Shapes, Dialogs,
Transforms, Scrollable and Loading indicators tests. Not checked yet:
- Interactive input, typing into text inputs, and hearing audio.
- The instanced draws (particles), which no UI test uses.
- Linux and macOS.

Found while checking, in the core and so on every platform:
- `UIBox` fills with `drawRect`'s transparent default. Its fill comes from `backgroundColor`, but
  the Shapes test sets `color`, so the test's two boxes are invisible.
- The medium "Soft" button of the Buttons test wraps its label ("Sof" / "t"). Text is measured and
  then laid out in the measured width, and the two passes disagree by a fraction of a pixel with
  fractional glyph advances.

## Decisions

- **A separate OpenGL backend.** The desktop backend is a GL 3.3 core port of the OpenGL ES 3 one,
  using `org.lwjgl.opengl.GL33` where Android uses `android.opengl.GLES20/30`. The Android backends
  stay untouched, so their performance isn't at risk. Until they're merged behind a common GL
  interface, a fix to the GL logic has to be made in both.
- **Shared assets.** The built-in shaders and fonts live in `../core/assets`. It's a plain
  folder, not a resources directory of the core, so the core's jar (and the APK) doesn't carry a
  second copy. Android packages it as assets, desktop as classpath resources.
- **Same shaders.** The GLSL ES 3.00 shaders are rewritten at load time to `#version 330 core`.
  `samplerExternalOES` becomes `sampler2D`, so `video.frag` compiles, though desktop videos don't use it.
- **BASS natives committed**, as the Android ones are: Windows, Linux and macOS, from un4seen
  (bass24 and bass_fx24), laid out the way JNA extracts them from the classpath.
- **Video with FFmpeg, downloaded at build time.** `FFmpegVideoDecoder` decodes with FFmpeg's shared
  libraries through JNA (`desktop/video/FFmpeg.kt`) on a thread of its own, pacing the frames by the
  video's clock (play, pause, speed and seek), converting the ones it shows to RGBA with `sws_scale`
  and having the texture upload the newest one when it's next drawn (`GraphicsTexture.invalidateRegion`).
  - The libraries are LGPL shared builds of [BtbN/FFmpeg-Builds](https://github.com/BtbN/FFmpeg-Builds),
    pinned with their checksums for Windows and Linux (x64 and arm64). `prepareFFmpeg` downloads the
    one of the host into Gradle's cache and extracts avutil, swresample, swscale, avcodec and avformat
    to `build/ffmpeg/<platform>`, and the run tasks pass that folder as `verktex.ffmpeg.path`. They're
    loaded by full path, each after the ones it needs. Elsewhere (macOS), or without that property,
    the system's FFmpeg of the same versions is used, if there is one.
  - Committing them like BASS was the alternative: 30 to 90 MB a platform.
  - Structs are read at the offsets of FFmpeg 8.1 (avformat 62, avcodec 62, avutil 60), found with
    `offsetof` on its headers, and the decoder refuses other versions. Pinning a newer build of
    another major version means finding the offsets again.
  - The dated autobuilds get pruned. When the download fails, pin a newer one of the same versions.
- **X11 on Wayland sessions.** When XWayland is available, GLFW uses X11. On Wayland, GLFW draws
  the window's title bar and buttons through libdecor, whose GTK plugin only runs on the process's
  first thread (it compares `getpid()` and `gettid()`). Java's `main` never runs on it, and GNOME
  doesn't draw decorations for Wayland apps, so the window had none. Through XWayland the
  compositor draws them. It costs, measured on GNOME at 125% with two 4096x2304 displays:
  - **Scale.** XWayland only knows integer scales, so the window gets a scale of 2 (`Xft.dpi` 192)
    instead of 1.25. The window opens at 2560x1440 rather than 1600x900, and the UI is 60% larger.
    It's still sharp, thanks to Mutter's `xwayland-native-scaling`.
  - **Frame rate.** The Text UI test draws at about 344 frames per second (2.9 ms a frame), where
    native Wayland reached the 1000 cap (and 1646 uncapped, 0.6 ms). Part of it is the 2.5 times as
    many pixels, part likely XWayland's extra copy of each frame. Far above the display's refresh
    rate, but it's headroom lost.

  See "What's left" for the alternatives.

## Steps

1. **Shared assets.** Move `verktex/android/src/main/assets/{shaders,fonts}` to
   `../core/assets`. Android adds it as an assets directory, and `compileVulkanShaders` reads the
   shaders from there. The asset paths (`shaders/main.vert`, `fonts/…`) don't change.
2. **A shared JVM loop worker.** `AndroidLoopWorker` is plain JVM code, so it moves to the core's
   `jvmMain` as `ThreadLoopWorker`, used by both platforms.
3. **`GraphicsBackend.OpenGL`**, the backend desktop reports.
4. **The module.** `:verktex:desktop`, a Kotlin JVM application depending on `:verktex:core`, with
   LWJGL (lwjgl, glfw, opengl, stb) and JNA. The app never depends on it.
5. **The platform.**
   - `DesktopPlatform`: the display density is the window's content scale. There's no virtual
     keyboard, and the workers are `ThreadLoopWorker`.
   - `DesktopLogger` prints to the console.
   - `DesktopProvider`: internal paths are classpath resources and external ones are files. Textures
     are decoded with stb_image.
   - Fonts are rasterised with stb_truetype, the same way `AndroidFont` does with `Paint`:
     - Sizes scale by em, and the metrics come from hhea.
     - Glyphs are white with their coverage as alpha, inside a transparent 1-pixel border. Glyphs
       are packed edge to edge in the atlas, and drawn at fractional positions, linear filtering
       would otherwise read the neighbouring glyph's edge.
     - There's no kerning, as on Android.
     - stb can't synthesise weights or italics. Oblique is a skew, as on Android.
     - Collections (`.ttc`) load their first font.
     - System fonts (`DesktopSystemFontSource`, behind `Fonts.loadSystem` and `BuiltIn.Fonts.System`)
       look up a file per weight and italic, since stb can't synthesise them: `fc-match` on Linux,
       the Segoe UI files on Windows, and `SFNS.ttf` or `Helvetica.ttc` (regular only) on macOS.
       Without a file for a weight, the regular one is used.
6. **The OpenGL backend** (`desktop/gl/`), ported from the OpenGL ES 3 one:
   - `GLContext` owns the GLFW window, created on the main thread and made current on the draw
     thread, where LWJGL gets its capabilities. It binds one VAO, which the core profile requires.
   - The draw thread is the only one calling GL, which never happens in a constructor. So the
     version and capabilities are read once the context is current.
   - `GLSL330Translator` rewrites the shaders.
   - There are no external textures, videos go through plain textures (see Decisions), and no
     GPU timer yet.
7. **BASS** (`desktop/bass/`):
   - JNA direct-mapped bindings for the functions the Android driver uses, plus `BASS_FX_TempoCreate`
     (`BassNative`, imported as `BASS` so the code reads like the Android one).
   - The libraries are copied out of the classpath under their own names before they're loaded
     (`BassLibraries`). JNA's own extraction renames them, and bass_fx then can't find bass.
   - The driver, device, sample and stream are adapted from the Android ones.
   - Internal audio is loaded from memory.
   - The sync callbacks are kept referenced, so JNA doesn't drop them.
8. **`DesktopGameHost`**, the counterpart of `AndroidGameHost`:
   - It runs GLFW's event loop on the main thread.
   - The mouse is pointer 0, and it moves only while a button is held, like a touch. The scroll
     wheel is dispatched as scroll events.
   - Keys are mapped from GLFW's.
   - Minimising pauses the engine.
   - A text input that's attached gets the typed characters, with the same validation as on Android.
     Backspace, Enter and pasting with Ctrl+V edit it.
9. **The UI tests.** `UITestApplication`, like `UITestActivity`, remembers the last test. Run them
   with:

   ```bash
   ./gradlew :verktex:desktop:run --args="--test 'Verktex/<test>'"
   ```

   `--screenshot <file.png>` saves a frame after a second and exits, for smoke checks.
10. **Docs:** `extraction.md` and `../CLAUDE.md`.

## Verification

- **Unit tests:** the shader translation of every built-in shader, the key mapping, the text
  validation, and a BASS smoke test on the "no sound" device.
- **The other modules:** their tests, the APK (still with its SPIR-V and fonts), lint, and the common
  check all stay as they were.
- **Screenshots** of a few UI tests (text, buttons, containers, particles, blur), checked by reading
  the PNGs.
- **Not checked here:** input, text input and audio interactively, and Linux and macOS.

## What's left

- The GPU timer.
- Packaging the FFmpeg libraries with a desktop distribution, which doesn't exist yet: only the run
  tasks pass them.
- Font weights and italics of bundled fonts, which would need FreeType or separate font files.
  System fonts already use a file per weight.
- Sharing the GL logic with the Android OpenGL ES backends.
- Running the game itself on desktop.
- **Native Wayland with window decorations**, so the window no longer goes through XWayland (see
  "X11 on Wayland sessions" in Decisions for why it does and what it costs). The options:
  - **An opt-out**, a `--wayland` flag or an environment variable, keeping native Wayland with its
    right scale and frame rate, but no title bar or buttons. Cheap, a stopgap.
  - **Drawing the decorations ourselves.** Disable libdecor (`GLFW_WAYLAND_LIBDECOR` set to
    `GLFW_WAYLAND_DISABLE_LIBDECOR`) and draw a title bar with close, maximise and minimise buttons
    as Verktex UI, moving and resizing the window through GLFW. The clean fix, but real work, and it
    has to follow the system's look somewhat.
  - **GLFW on the process's first thread**, where libdecor's GTK plugin agrees to run. Java can't do
    it on Linux (there's no `-XstartOnFirstThread`), so it needs a small native launcher that starts
    the JVM through JNI on a second thread and gives the first one to GLFW. It changes how the app is
    packaged and started.
