# Desktop platform

Verktex's second platform, the desktop JVM, in `:verktex:desktop`: OpenGL 3.3 for graphics (LWJGL
and GLFW) and BASS for audio (through JNA). It proves the split of `Platform.Implementation` works,
and makes it quick to try components without a phone. For now it only runs Verktex's own UI tests
(`UITestScene` with `VerktexUITests`), not the game.

## Decisions

- **A separate OpenGL backend.** The desktop backend is a GL 3.3 core port of the OpenGL ES 3 one,
  using `org.lwjgl.opengl.GL33` where Android uses `android.opengl.GLES20/30`. The Android backends
  stay untouched, so their performance isn't at risk. Until they're merged behind a common GL
  interface, a fix to the GL logic has to be made in both.
- **Shared assets.** The built-in shaders and fonts live in `verktex/core/assets/`. It's a plain
  folder, not a resources directory of the core, so the core's jar (and the APK) doesn't carry a
  second copy. Android packages it as assets, desktop as classpath resources.
- **Same shaders.** The GLSL ES 3.00 shaders are rewritten at load time to `#version 330 core`.
  `samplerExternalOES` becomes `sampler2D`, so `video.frag` compiles, though desktop has no video yet.
- **BASS natives committed**, as the Android ones are: Windows, Linux and macOS, from un4seen
  (bass24 and bass_fx24), laid out the way JNA extracts them from the classpath.

## Steps

1. **Shared assets.** Move `verktex/android/src/main/assets/{shaders,fonts}` to
   `verktex/core/assets/`. Android adds it as an assets directory, and `compileVulkanShaders` reads the
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
     - Glyphs are white with their coverage as alpha.
     - There's no kerning, as on Android.
     - stb can't synthesise weights or italics. Oblique is a skew, as on Android.
6. **The OpenGL backend** (`desktop/gl/`), ported from the OpenGL ES 3 one:
   - `GLContext` owns the GLFW window, created on the main thread and made current on the draw
     thread, where LWJGL gets its capabilities. It binds one VAO, which the core profile requires.
   - The draw thread is the only one calling GL, which never happens in a constructor. So the
     version and capabilities are read once the context is current.
   - `GLSL330Translator` rewrites the shaders.
   - There are no external textures (video) and no GPU timer yet.
7. **BASS** (`desktop/bass/`):
   - JNA direct-mapped bindings for the functions the Android driver uses, plus `BASS_FX_TempoCreate`.
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
10. **Docs:** `extraction.md` and `CLAUDE.md`.

## Verification

- **Unit tests:** the shader translation of every built-in shader, the key mapping, the text
  validation, and a BASS smoke test on the "no sound" device.
- **The other modules:** their tests, the APK (still with its SPIR-V and fonts), lint, and the common
  check all stay as they were.
- **Screenshots** of a few UI tests (text, buttons, containers, particles, blur), checked by reading
  the PNGs.
- **Not checked here:** input, text input and audio interactively, and Linux and macOS.

## What's left

- Video (external textures) and the GPU timer.
- Font weights and italics, which would need FreeType or separate font files.
- Sharing the GL logic with the Android OpenGL ES backends.
- Running the game itself on desktop.
