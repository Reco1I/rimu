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
- `verktex/src/main/java/com/reco1l/verktex/graphics/` — backend-neutral API:
  `GraphicsRenderer` (records `DrawCommand`s in `commandBatch`, flushes them in `end()` via
  `drawCommand()`), `GraphicsDevice`, `GraphicsContext`, `GraphicsWindow`, `GraphicsBuffer`,
  `GraphicsShader`, `GraphicsTexture`, `GraphicsResource`, `DrawCommand`, `VertexLayout`,
  `BlendMode`, `DepthState`, `GraphicsBackend` (enum stored in settings).
- `verktex/.../android/gles/` — `ESRenderer` (+ `es2/ES2Renderer`, `es3/ES3Renderer`),
  `ESCommandExecutor` (+ ES2/ES3 variants), `ESContext` (EGL),
  `ESDevice`, `ESBuffer`, `ESShader`, `ESTexture`, `ESExternalTexture` (SurfaceTexture/OES),
  `ES3GpuTimer`, `es2/GLSLES100Translator` (GLSL ES 3.00 → 1.00, tested in
  `tests/test/kotlin/com/osudroid/verktex/GLSLES100TranslatorTest.kt`).
- Shared Android types: `android/AndroidWindow` (the `SurfaceView`), `android/AndroidExternalTexture`
  (what video producers see of an external texture).
- Backend selection: `verktex/.../android/Android.kt` (`initializeAndroid`), setting in
  `src/com/osudroid/ui/v2/settings/SettingsSections.kt` and read in
  `src/ru/nsu/ccfit/zuev/osu/MainActivity.java`.
- Frame loop: `Verktex.draw()` (`verktex/.../Verktex.kt`) on the draw worker:
  `prepareFrame() → begin() → tree records → end()`.
- Shaders: GLSL ES 3.00 in `assets/verktex/shaders/` (`main.vert`, `ui.vert/frag`, `quad`,
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
| 12 | **Backend selection is GL-only** | `GraphicsBackend.kt`, `Android.kt`, `SettingsSections.kt`, `MainActivity.java`, `src/com/acivev/utils/DevicePerfomanceUtils.kt` (GLES3 check) | Add `Vulkan` entry, fallback chain Vulkan → ES3 → ES2 on init failure. |

Out of scope: `legacy/storyboard/` (`GLWrapped` etc.) is outside the source set and pending a rewrite on Verktex.

## Recommended design
### Native layer
- New CMake target inside `:verktex` (`verktex/src/main/cpp/vulkan/`, `externalNativeBuild`), linking `vulkan`, `android`, `log`.
- **Coarse-grained JNI**: per-call JNI overhead is too high for per-draw calls. Kotlin side serializes the frame's `DrawCommand`s into a direct `ByteBuffer` command stream (opcodes + handles + offsets); native side decodes and records one `VkCommandBuffer`. Resource creation/destruction are separate JNI calls returning `Long` handles.
- Vertex/uniform data: native maps a host-visible, coherent ring buffer per frame in flight and exposes it with `NewDirectByteBuffer`, so `VKBuffer.uploadDataToGPU()` copies `IntArray` words straight into GPU-visible memory (one `IntBuffer.put`), recording `(offset, size)`.
- Memory: Vulkan Memory Allocator (VMA, header-only) for images; the ring buffers can be hand-allocated.
- Debug builds enable `VK_LAYER_KHRONOS_validation` when present (pushed via `adb` or bundled in `jniLibs` for debug only).

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

### Kotlin layer — `verktex/src/main/java/com/reco1l/verktex/android/vulkan/`
`VKRenderer : GraphicsRenderer`, `VKDevice : GraphicsDevice`, `VKContext : GraphicsContext`
(instance, device, swapchain, frames-in-flight sync), `VKBuffer`, `VKShader`, `VKTexture`,
`VKExternalTexture`, `VKCommandEncoder` (the `DrawCommand` → stream serializer, mirroring
`ESCommandExecutor`), `VKPipelineKey`, `VKGpuTimer`, `VKNative` (JNI declarations).

### Shaders
- Keep GLSL ES 3.00 as the single source of truth. Add a `GLSL450VulkanTranslator` (sibling of
  `GLSLES100Translator`, same regex approach): `#version 450`, explicit `layout(location)` on
  varyings, loose non-sampler uniforms → one `layout(set=0, binding=0) uniform Uniforms { … }`
  block in declaration order (std140), `sampler2D`/`samplerExternalOES` →
  `layout(set=0, binding=1)` combined image sampler.
- Compile to SPIR-V **at build time**: a Gradle task runs the translator + NDK `glslc` over
  `assets/verktex/shaders/*` and emits `*.spv` plus a small reflection JSON (uniform name →
  offset/type, std140). `GraphicsShader.Source` gains optional `spirvVertex/spirvFragment/reflection`;
  `AndroidProvider.createShaderSource` loads them when present. Runtime GLSL compilation (shaderc)
  is not needed while the app defines no custom shaders; `VKDevice.createShader` throws for a
  source without SPIR-V.

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
     Keep the unpremultiply/pack logic of `AndroidTextureSource` (it's backend-neutral) and its tests.
   - Split `GraphicsTexture.onBind` into "flush uploads" (backend) and "bind".
   - Move default-uniform injection from `ESCommandExecutor` into `GraphicsRenderer`/a shared helper.
   - Rename `ESWindow` → `android/AndroidWindow`; change `GraphicsContext` to `acquireFrame`/`present`.
   - Make external textures expose only `surface` + frame callback.
   - Make the `captureTexture` origin explicit in `DrawCommand`/`UICaptureContainer`.
   - Verify: `./gradlew test`, `./gradlew :verktex:testDebugUnitTest`, ES2 and ES3 visual checks in `OsuUITestActivity`.
2. **Buffer lifetime.** ✅ Done: the CPU-side storage (`words`/`wordCount`, `put*`) moved from
   `ESBuffer` into `GraphicsBuffer`, so a `VKBuffer` only copies `words` into its frame's ring in
   `uploadDataToGPU`. The lifetime is documented on `GraphicsBuffer`: a buffer used by a recorded
   draw keeps its data until the frame is executed, then may be rewritten at once, and the backend
   keeps in-flight copies valid. The renderer marks recorded buffers and `clear()` warns on a
   violation (`BufferLifetimeTest`). Static buffers (the particle quad) are re-uploaded every frame;
   a `Static` usage that Vulkan keeps device-local is a later optimization.
3. **Shader tooling.** `GLSL450VulkanTranslator` + JVM unit tests (pattern of `GLSLES100TranslatorTest`), Gradle SPIR-V task, reflection JSON, `GraphicsShader.Source` extension.
4. **Vulkan bring-up.** CMake/JNI module, instance/device/swapchain, `VKContext`, `GraphicsBackend.Vulkan`, fallback chain in `Android.kt`, solid-color quads only (`SolidQuad`, `Color`).
5. **Feature parity.** Textures + uploads, UI shader/`VertexLayout.UI`, quads index buffer, instancing (particles), depth state & clears, scissor, blend modes, capture/blur, timestamps → `PerformanceStats.gpuTime`, capabilities.
6. **Video.** `VKExternalTexture` via `ImageReader` + AHB + YCbCr sampler (API 26+, extension required); where unsupported, report it through `Capabilities` and let callers degrade (no video background).
7. **Ship.** Settings entry + `DevicePerfomanceUtils` check, `Auto` stays GL until Vulkan is proven; consider enabling Vulkan for `Auto` only on API ≥ 29 with Vulkan 1.1 after field testing.

## Risks
- JNI/encoding overhead could erase Vulkan's CPU gains for a 2D engine — measure `PerformanceStats.submitTime` vs ES3 early in phase 4.
- Android driver quality (especially pre-API 29, Mali/Adreno older parts); keep automatic fallback to ES3.
- Video path depends on optional extensions.
- Mid-frame captures split the render pass (cost on tilers); the offscreen target mitigates correctness, not cost.

## Verification for every phase
- `./gradlew test` and `./gradlew :verktex:testDebugUnitTest` (translator, ring allocator, pipeline-key and command-encoder tests are pure JVM).
- `./gradlew assembleDebug`, check the exit code (Java errors too), `adb install -r -d`.
- Visual parity: open the same UI tests on each backend, e.g.
  `adb shell am start -S -n ru.nsu.ccfit.zuev.osuplus.debug/com.osudroid.ui.v2.test.OsuUITestActivity --es test "'osu!/Hit circles'"`
  after switching the Graphics backend setting; compare screenshots (`adb exec-out screencap -p`).
- Vulkan phases: zero validation-layer errors in `adb logcat` on debug builds; background/foreground and rotation (swapchain recreation) without crashes.
