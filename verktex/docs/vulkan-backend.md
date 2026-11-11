# Vulkan backend for Verktex — feasibility and roadmap

## Verdict
**Feasible, medium-to-large effort.** The draw-command level is already backend neutral: a
`DrawCommand` carries the whole state of a draw (shader, texture, scissor, blend, depth, vertex
layout, uniforms, quads/instancing, depth clear, framebuffer capture), which maps cleanly onto a
Vulkan pipeline + dynamic state. The UI tree, layout, `Choreography` and the app never touch GL
directly. What is *not* neutral are the resource and upload contracts below them; those need a
refactor (useful for any future backend, e.g. Metal/WebGPU) before a Vulkan backend can be written.

Constraints: `minSdkVersion 24` (Vulkan 1.0 exists from API 24, but drivers before API 28–29 are
often poor), NDK 22.1.7171670 (ships Vulkan headers, `libvulkan.so` and `glslc` under
`shader-tools/`). Android has **no Java/Kotlin Vulkan API**, so a JNI/C++ layer is mandatory.

## Current architecture (what exists)
- `verktex/core/src/main/kotlin/com/reco1l/verktex/graphics/` — backend-neutral API:
  `GraphicsRenderer` (records `DrawCommand`s in `commandBatch`, flushes them in `end()` via
  `drawCommand()`), `GraphicsDevice`, `GraphicsContext`, `GraphicsWindow`, `GraphicsBuffer`,
  `GraphicsShader`, `GraphicsTexture`, `GraphicsResource`, `DrawCommand`, `VertexLayout`,
  `BlendMode`, `DepthState`, `GraphicsBackend` (enum stored in settings).
- `verktex/.../android/gles/` — `ESRenderer` (+ `es2/ES2Renderer`, `es3/ES3Renderer`),
  `ESCommandExecutor` (+ ES2/ES3 variants), `ESContext` (EGL),
  `ESDevice`, `ESBuffer`, `ESShader`, `ESTexture`, `ESExternalTexture` (SurfaceTexture/OES),
  `ES3GpuTimer`, `es2/GLSLES100Translator` (GLSL ES 3.00 → 1.00, tested in
  `../android/src/test/kotlin/com/reco1l/verktex/GLSLES100TranslatorTest.kt`).
- Shared Android types: `android/AndroidWindow` (the `SurfaceView`), `android/AndroidExternalTexture`
  (what video producers see of an external texture).
- Backend selection: `verktex/.../android/Android.kt` (`initializeAndroid`), setting in
  `../src/com/osudroid/ui/v2/settings/SettingsSections.kt` and read in
  `../src/ru/nsu/ccfit/zuev/osu/MainActivity.java`.
- Frame loop: `Verktex.draw()` (`verktex/.../Verktex.kt`) on the draw worker:
  `prepareFrame() → begin() → tree records → end()`.
- Shaders: GLSL ES 3.00 in `../core/assets/shaders` (`main.vert`, `ui.vert/frag`, `quad`,
  `color`, `texture`, `video` (samplerExternalOES), `blur`, `particle.vert/frag`). Loaded as
  strings by `AndroidProvider.createShaderSource`. The app creates no shaders of its own today.

## Gaps (where the abstraction leaks GL)
| # | Gap | Where | Why it blocks Vulkan |
|---|-----|-------|----------------------|
| 1 | **Texture uploads are done by the source**, `GraphicsTexture.Source.uploadToHardware(x, y)` | `GraphicsTexture.kt`, `AndroidTextureSource.kt` (calls `GLUtils.texSubImage2D` / `glTexSubImage2D`), `FramedTextureRegion.kt`, `AndroidFont.kt` (glyphs), `UICaptureContainer.CaptureSource` | Vulkan needs a staging buffer copy + image layout transitions recorded in a command buffer. The source must *provide pixels*, the backend texture must upload them. |
| 2 | **Immediate bind semantics**, `GraphicsResource.bind()` = load + bind + (for textures) flush pending uploads | `GraphicsResource.kt`, `GraphicsTexture.onBind` | Vulkan has no bound state; "bind" becomes "make sure it's resident and pick its descriptor". Uploads must happen before the render pass, not mid-pass. |
| 3 | **Buffer lifetime across frames** (✅ phase 2) — `GraphicsRenderer.bufferPool` reuses buffers every frame; `uploadDataToGPU()` overwrites in place | `GraphicsRenderer.kt`, `ESBuffer.kt` | GL drivers orphan implicitly; with 2–3 Vulkan frames in flight, overwriting a buffer still read by the GPU corrupts draws. Needs per-frame ring (linear) allocation. |
| 4 | **Name-based loose uniforms** (`UniformSet` of `String → Any`, defaults like `uViewportSize`, `uColor`, `uTransform` injected in `ESCommandExecutor.defaultUniforms`) | `GraphicsShader.kt`, `ESShader.applyUniform`, `ESCommandExecutor` | Vulkan has only UBOs / push constants. Needs reflection (name → offset/type) and packing into a per-draw dynamic UBO slice. The default-uniform logic is backend-agnostic and should move up to common code. |
| 5 | **Pipeline state is set piecemeal** (blend, depth, attributes) | `ESCommandExecutor.execute` | Vulkan bakes blend, depth, vertex input and topology into a `VkPipeline`. Needs a pipeline cache keyed by (shader, `VertexLayout`, `BlendMode`, `DepthState`, render pass/format). `DrawCommand` already has all the inputs. |
| 6 | **Framebuffer capture mid-frame** with `glCopyTexSubImage2D` (bottom-left origin) | `DrawCommand.captureTexture`, `ESCommandExecutor`, `UICaptureContainer` | Requires ending the render pass, copying, and resuming. Easiest if Vulkan always renders to an offscreen color image and blits/draws it to the swapchain at the end. The capture's origin convention must be made explicit (GL is bottom-left, Vulkan top-left) so `UICaptureContainer`'s UVs stay correct. |
| 7 | **External (video) textures** are `SurfaceTexture` + `GL_TEXTURE_EXTERNAL_OES` | `ESExternalTexture.kt`, `video.frag`, `GraphicsDevice.createExternalTexture` | GL-only. Vulkan path: `ImageReader` (API 26+, `USAGE_GPU_SAMPLED_IMAGE`) → `AHardwareBuffer` → `VK_ANDROID_external_memory_android_hardware_buffer` + `VkSamplerYcbcrConversion`. The public API leaks `surfaceTexture`; expose only a `Surface` + "frame available" to producers. |
| 8 | **Context/window types are EGL-shaped** — `GraphicsContext.makeCurrent/swapBuffers`, window class named `ESWindow` | `GraphicsContext.kt`, `ESWindow.kt`, `ESContext.prepare` | `ESWindow` is a generic `SurfaceView`; rename/move to `android/AndroidWindow` and share it. `GraphicsContext` should become `acquireFrame(): Boolean` / `present()`; Vulkan recreates `VkSurfaceKHR` + swapchain on `surfaceGeneration` change and on `VK_ERROR_OUT_OF_DATE_KHR`/`SUBOPTIMAL`. |
| 9 | **Clip-space / NDC conventions** | `main.vert`, `ui.vert`, `particle.vert` | Vulkan Y is flipped and depth is 0..1. Use a negative-height viewport (core in 1.1 / `VK_KHR_maintenance1`) instead of touching shaders. |
| 10 | **GPU timing** | `es3/ES3GpuTimer.kt`, `PerformanceStats.gpuTime` | Replace with `vkCmdWriteTimestamp` queries. |
| 11 | **Capabilities** only `maxTextureSize` from `GL_MAX_TEXTURE_SIZE` | `GraphicsDevice.Capabilities`, `ESDevice.refreshCapabilities` | Fill from `VkPhysicalDeviceLimits.maxImageDimension2D`; consider adding `supportsExternalTextures`, `maxVertexAttributes`. |
| 12 | **Backend selection is GL-only** | `GraphicsBackend.kt`, `Android.kt`, `SettingsSections.kt`, `MainActivity.java`, `../src/com/acivev/utils/DevicePerfomanceUtils.kt` (GLES3 check) | Add `Vulkan` entry, fallback chain Vulkan → ES3 → ES2 on init failure. |

Out of scope: `../legacy/storyboard` (`GLWrapped` etc.) is outside the source set and pending a rewrite on Verktex.

## Recommended design
### Native layer
- New CMake target inside `:verktex:android` (`../android/src/main/cpp/vulkan`, `externalNativeBuild`), linking `vulkan`, `android`, `log`.
- **Coarse-grained JNI**: per-call JNI overhead is too high for per-draw calls. Kotlin side serializes the frame's `DrawCommand`s into a direct `ByteBuffer` command stream (opcodes + handles + offsets); native side decodes and records one `VkCommandBuffer`. Resource creation/destruction are separate JNI calls returning `Long` handles.
- Vertex/uniform data: native maps a host-visible, coherent ring buffer per frame in flight and exposes it with `NewDirectByteBuffer`, so `VKBuffer.uploadDataToGPU()` copies `IntArray` words straight into GPU-visible memory (one `IntBuffer.put`), recording `(offset, size)`.
- Memory: Vulkan Memory Allocator (VMA, header-only) for images; the ring buffers can be hand-allocated.
- Debuggable builds enable `VK_LAYER_KHRONOS_validation` when it's packaged. The NDK r22 layer
  (1.2.158) crashes in `vkCreateInstance` on Android 15, so it isn't used: a current
  `android-binaries-*.zip` from Vulkan-ValidationLayers' releases is packaged in debug builds when
  `-Pverktex.validationLayers=<extracted dir>` (or the same key in `../local.properties`) is set.

### Binding libraries considered
- **`technoir-lab/vulkan-kotlin`** (`io.technoirlab.vulkan:vulkan-kotlin`, Apache-2.0) is
  **rejected for now**:
  - It targets **Kotlin/Native only** (`androidNativeArm64`, plus iOS/Linux/macOS/Windows), with
    no Kotlin/JVM target. Verktex and the app run on ART (Kotlin/JVM), so it can't be called from
    `GraphicsRenderer` directly. Using it means building a separate K/N shared library that the app
    still reaches through JNI, so the JNI boundary stays and a second runtime and GC are added.
  - It **requires Vulkan 1.4**. Few Android devices have that (recent flagships only), while the
    app supports `minSdkVersion 24`. A useful Vulkan backend has to run on 1.1 (API 28–29+).
  - It ships only an arm64 Android target (the app also builds other ABIs). It has no render
    passes, which means dynamic rendering only, and no custom allocators.
  - It is young (about 11 stars, no published releases) and requires Kotlin 2.4+.
  - Worth checking again if it adds a JVM target or lowers its minimum to Vulkan 1.1. Its main
    appeal is writing the native layer in Kotlin instead of C++, and the coarse JNI design below
    is the same either way.
- **LWJGL** has Vulkan bindings for the JVM but no official Android support, so it's rejected too.
- **Decision:** a hand-written C++ layer on the NDK's Vulkan headers, behind a small JNI surface.
  It supports Vulkan 1.0/1.1 and every ABI and adds no new toolchain.

### Kotlin layer — `../android/src/main/java/com/reco1l/verktex/android/vulkan`
`VKRenderer : GraphicsRenderer`, `VKDevice : GraphicsDevice`, `VKContext : GraphicsContext`
(instance, device, swapchain, frames-in-flight sync), `VKBuffer`, `VKShader`, `VKTexture`,
`VKExternalTexture`, `VKCommandEncoder` (the `DrawCommand` → stream serializer, mirroring
`ESCommandExecutor`), `VKPipelineKey`, `VKGpuTimer`, `VKNative` (JNI declarations).

### Shaders (implemented in phase 3)
- GLSL ES 3.00 stays the single source of truth. `GLSL450VulkanTranslator` lives in the plain JVM
  module `:verktex:shadertools` (`../shadertools`), so the build and the engine at runtime use
  the same code. Each stage translates on its own: `#version 450`, varyings and fragment outputs
  located in declaration order, the non-sampler uniforms of a stage → one std140 block
  (`VertexUniforms` at set 0 binding 0, `FragmentUniforms` at binding 1), samplers (fragment only) →
  combined image samplers in set 1 from binding 0 (a set per texture in phase 5), `samplerExternalOES` → `sampler2D` reported as external.
  `checkInterface` verifies the varyings of a vertex/fragment pair line up.
- SPIR-V is compiled **at build time** by the `compileVulkanShaders` task of `:verktex:android` (NDK `glslc`,
  `--target-env=vulkan1.0`) into generated assets as `<name>.spv`; without the NDK it skips with a
  warning. There is no reflection file: the runtime gets the layout by running the translator on the
  GLSL it already loads. `GraphicsShader.Source` gained `spirvVertex/spirvFragment`, filled by
  `AndroidProvider.createShaderSource`. `VKDevice.createShader` throws for a source without SPIR-V.
- The built-in programs are listed once in `BuiltIn.Shaders.Programs` (created with
  `GraphicsRenderer.createBuiltInShaders`), and `BuiltInShadersVulkanTest` checks they all link.

### Per-frame flow
`prepareFrame` = wait fence of frame N, acquire swapchain image (recreate on out-of-date / new
surface generation). `begin` = reset frame ring, begin cmd buffer, flush pending texture uploads
(staging + barriers), begin render pass on offscreen target. `end` = encode commands → native
records draws (pipeline from cache, dynamic scissor/viewport, descriptor for texture + dynamic UBO
offset), handles `clearDepth` with `vkCmdClearAttachments` and `captureTexture` by splitting the
render pass, then blit to the swapchain image and present.

## Roadmap (each phase is a separately mergeable PR; phases 1–3 keep GL as the only backend)
1. **Neutralize resource contracts (GL only, no behavior change).** ✅ Done:
   `Source.writePixels(PixelTarget, x, y)` + `GraphicsTexture.onWritePixels`/`flushUploads`,
   `graphics/DefaultUniforms`, `AndroidWindow` + `GraphicsContext.acquireFrame/present`,
   `AndroidExternalTexture`, `GraphicsRenderer.framebufferOrigin`. Gaps 1, 2, 7 (API side) and 8
   are closed; for gap 4 only the default uniforms moved, the reflection is phase 3.
   - Replace `Source.uploadToHardware(x, y)` with a pixel-providing API (e.g.
     `writePixels(target: ByteBuffer)` / `pixelFormat`), moving the GL upload into `ESTexture`.
     Keep the premultiply/pack logic of `AndroidTextureSource` (it's backend-neutral) and its tests.
   - Split `GraphicsTexture.onBind` into "flush uploads" (backend) and "bind".
   - Move default-uniform injection from `ESCommandExecutor` into `GraphicsRenderer`/a shared helper.
   - Rename `ESWindow` → `android/AndroidWindow`; change `GraphicsContext` to `acquireFrame`/`present`.
   - Make external textures expose only `surface` + frame callback.
   - Make the `captureTexture` origin explicit in `DrawCommand`/`UICaptureContainer`.
   - Verify: `./gradlew test`, `./gradlew :verktex:core:test :verktex:android:testDebugUnitTest`, ES2 and ES3 visual checks in `OsuUITestActivity`.
2. **Buffer lifetime.** ✅ Done: the CPU-side storage (`words`/`wordCount`, `put*`) moved from
   `ESBuffer` into `GraphicsBuffer`, so a `VKBuffer` only copies `words` into its frame's ring in
   `uploadDataToGPU`. The lifetime is documented on `GraphicsBuffer`: a buffer used by a recorded
   draw keeps its data until the frame is executed, then may be rewritten at once, and the backend
   keeps in-flight copies valid. The renderer marks recorded buffers and `clear()` warns on a
   violation (`BufferLifetimeTest`). Static buffers (the particle quad) are re-uploaded every frame;
   a `Static` usage that Vulkan keeps device-local is a later optimization.
3. **Shader tooling.** ✅ Done, see Shaders above. Requires the NDK installed for the `.spv` to be built.
4. **Vulkan bring-up.** ✅ Done: `../android/src/main/cpp/vulkan` (CMake 3.22.1, `libverktex_vulkan.so`,
   every ABI) behind `VKNative`; `VKContext`/`VKRenderer`/`VKDevice`/`VKShader` in `android/vulkan/`;
   2 frames in flight, each with a mapped 4 MB ring for vertices and uniforms (`VKFrameRing`,
   `VKUniformPacker`) and one int command stream (`VKCommandEncoder`); pipelines cached by
   `VKPipelineKey`. `GraphicsBackend.Vulkan` falls back to ES3 when initialization fails (`Auto` stays
   GL, no settings entry yet); `UITestActivity` takes `--es backend <name>`. Draws triangles of
   shaders without samplers (`SolidQuad`, `Color`), the rest is skipped and logged once. Verified on
   an Adreno 610 (Vulkan 1.1.128): Shapes test matches ES3 for those draws, background/resume
   recreates the swapchain. The swapchain doesn't pre-rotate (the compositor rotates, Android then
   reports `VK_SUBOPTIMAL_KHR`, which is ignored). With validation layer 1.4.363.1: no errors over the
   Shapes, Main menu, Gameplay circles and Mod menu tests and 3 background/resume cycles, only the
   expected pre-rotation performance warning. Comparing `PerformanceStats.submitTime` with ES3 moves
   to the start of phase 5: until then Vulkan skips most of each frame, so the numbers don't compare.
5. **Feature parity.** ✅ Done, except external (video) textures, which are phase 6 (their draws
   are skipped). Textures are RGBA8 device-local images with a set-1 descriptor each and one linear
   clamp sampler; a 1×1 white texture stands in for samplers of draws without a texture (UI boxes).
   Uploads go through a per-frame staging area and are recorded before the frame's render pass, so a
   texture repacked mid-frame shows its new layout to the whole frame (OpenGL ES uploads between
   draws). Images and pipelines are destroyed once every submission that may use them completed,
   from any thread. Quads draw through a growing `uint32` index buffer, instances from the ring at
   binding 1, depth state is part of `VKPipelineKey` and clears use `vkCmdClearAttachments`. A
   capture ends the render pass, copies (or blits, for BGRA windows) the swapchain image into the
   texture with a top-left origin, and resumes with a pass that loads color and depth; depth is only
   stored when a later pass loads it. Timestamps around the command buffer give
   `PerformanceStats.gpuTime`; submit and present are separate calls so `submitTime` leaves out the
   vsync wait, as `eglSwapBuffers` does on OpenGL ES.

   Verified on the Adreno 610 against ES3 screenshots: Text, Skin sprites, Shapes, Buttons, Main
   menu (instanced particles), Sliders (depth) and Mod menu (capture + blur) match. Profile build, back
   to back: CPU submit time is much lower (main menu 0.34 vs 1.63 ms, mod menu ~1.6 vs 5.5–7 ms, draw
   time 2.1 vs 14.6 ms on the main menu), so JNI and encoding don't erase the gains. GPU time is
   slightly higher on the main menu (13.6 vs 12.2 ms), worth investigating (render pass ops,
   pre-rotation); ES3's timer gives impossible values on the mod menu, so it can't be compared there.
   Not yet rerun under the validation layer.
6. **Video.** ✅ Done. `VKExternalTexture` owns an `ImageReader` (`PRIVATE`, GPU sampled usage,
   so Android 10+) whose surface the player draws into. Each new frame's `AHardwareBuffer` is
   imported as an image of its own (dedicated memory, `PREINITIALIZED`, acquired from
   `VK_QUEUE_FAMILY_FOREIGN_EXT`) and sampled through a `VkSamplerYcbcrConversion` made from the
   buffer's suggested format. The conversion is an immutable sampler, so it has its own set and
   pipeline layout, and `VKPipelineKey` includes it. A replaced frame's image is destroyed, and its
   `Image` closed (returning the buffer to the decoder), once the submissions that sampled it
   completed. The crop rectangle goes into the texture transform. Needs Vulkan 1.1,
   `VK_ANDROID_external_memory_android_hardware_buffer`, `VK_EXT_queue_family_foreign` and the
   `samplerYcbcrConversion` feature; otherwise `Capabilities.supportsExternalTextures` is false and
   `GameScene` leaves the video out. The UI test `osu!/Video` plays the first beatmap video found.
   Verified on the Adreno 610 (external format, no drops at 24 fps, matching colors with ES3).
   Not done: waiting on the producer's acquire fence (`Image.getFence`, API 33), a validation run,
   and keeping imports of recycled buffers instead of importing every frame.
7. **Ship (experimental).** ✅ Done as an opt-in. The graphics settings list "Vulkan (experimental)"
   only where the device reports Vulkan 1.1 hardware (`isVulkanAvailable`), and picking it shows a
   warning. `Auto` stays on OpenGL ES. A crash guard covers driver crashes in native code, which the
   fallback can't catch: a flag is kept from before Vulkan starts until its first frame is presented,
   and finding it on the next launch makes it use OpenGL ES until Vulkan is picked again
   (`resetVulkanCrashGuard`). `DevicePerformanceUtil` only sizes particle effects, so it has no
   Vulkan check. The experimental label goes once the Follow-ups below are all solved; enabling
   Vulkan for `Auto` (e.g. API ≥ 29 with Vulkan 1.1) is decided after field testing.

## Follow-ups

Open points found while doing the phases above. Vulkan stays experimental until all of them are
solved, then the label is removed:
- the "Vulkan (experimental)" entry of `opt_graphics_backend_names_with_vulkan` and the
  `opt_graphics_backend_vulkan_experimental` warning, shown by `graphicsBackendOption`
  (`SettingsSections.kt`);
- "Experimental" in the KDoc of `GraphicsBackend.Vulkan` and "experimental" in the backend log line
  of `createVulkanRenderer` (`Android.kt`);
- this document's mentions of it.

The crash guard stays, it protects against driver bugs whatever the state of the backend.

1. **Validation run of phases 5 and 6.** Phase 4 ran clean under `VK_LAYER_KHRONOS_validation`
   1.4.363.1; textures, uploads, quads, instancing, depth, captures, timestamps and video frames
   haven't been run under it. Set `verktex.validationLayers` (see Native layer), run the Text, Skin
   sprites, Shapes, Sliders, Main menu, Mod menu and Video UI tests on Vulkan and check logcat for
   `VerktexVulkan: Validation`. The video path's layout and queue family transfer
   (`PREINITIALIZED`, `VK_QUEUE_FAMILY_FOREIGN_EXT`) is the most likely to need adjusting.
2. **Decoder fence.** Video frames are imported without waiting for the producer's acquire fence
   (`Image.getFence`, API 33): the GPU may sample a buffer the decoder is still writing. No glitch was
   seen on the Adreno 610, but nothing guarantees it. Import the fence as a semaphore
   (`VK_KHR_external_semaphore_fd`, sync fd) waited on by the frame's submission, or wait on it on the
   CPU before importing.
3. **Re-importing every frame.** Each new video frame creates an image, imports its memory and
   allocates a descriptor set, although an `ImageReader` cycles through a handful of buffers. Keep the
   imports by buffer (until the reader is closed) and only record the acquire barrier for a reused one.
4. **Memory over long playback.** Imported frames and their `Image`s are released through deferred
   destruction and submission counts; only a short playback was checked. Watch
   `adb shell dumpsys meminfo <pid>` (Graphics, EGL mtrack) over a full beatmap with video to confirm
   nothing grows.


## Risks
- JNI/encoding overhead could erase Vulkan's CPU gains for a 2D engine — measure `PerformanceStats.submitTime` vs ES3 early in phase 4.
- Android driver quality (especially pre-API 29, Mali/Adreno older parts); keep automatic fallback to ES3.
- Video path depends on optional extensions.
- Mid-frame captures split the render pass (cost on tilers); the offscreen target mitigates correctness, not cost.

## Verification for every phase
- `./gradlew test` and `./gradlew :verktex:core:test :verktex:android:testDebugUnitTest` (translator, ring allocator, pipeline-key and command-encoder tests are pure JVM).
- `./gradlew assembleDebug`, check the exit code (Java errors too), `adb install -r -d`.
- Visual parity: open the same UI tests on each backend, e.g.
  `adb shell am start -S -n ru.nsu.ccfit.zuev.osuplus.debug/com.osudroid.ui.v2.test.OsuUITestActivity --es test "'osu!/Hit circles'"`
  after switching the Graphics backend setting; compare screenshots (`adb exec-out screencap -p`).
- Vulkan phases: zero validation-layer errors in `adb logcat` on debug builds; background/foreground and rotation (swapchain recreation) without crashes.
