# Frame rate

How fast Verktex draws, on each platform, and what's left to do about it.

## Current state

- **Draw loop:** capped at 1000 frames per second (`DRAW_INTERVAL` in `Verktex.kt`) on every platform.
  Without vsync, it would otherwise draw thousands of frames a second for nothing.
- **Update loop:** capped at 240 updates per second (`UPDATE_INTERVAL`).
- Both caps are minimum intervals of `ThreadLoopWorker`, which schedules each loop from the previous
  deadline, so the time sleeping overshoots is made up and the rate holds.
- **Desktop:** vsync is off by default, so the draw cap is the limit. `--vsync` (or `GLContext.vsync`,
  which can change at any time) caps it to the display's refresh rate.
- **Android:** vsync is always on, so the frame rate is the display's refresh rate.
  - OpenGL ES never calls `eglSwapInterval`, which stays at its default of 1.
  - Vulkan presents with `VK_PRESENT_MODE_FIFO_KHR` (`vk_context.cpp`).

## Pending

1. **Turning vsync off on Android**, as a setting. It can't show more frames than the display's
   refresh rate: the compositor (SurfaceFlinger) shows at most one per refresh and drops the others.
   What it gains is latency, since the frame shown is a newer one, at the cost of battery and heat.
   So it would be offered as a lower latency option, rather than a higher frame rate.
   - **OpenGL ES:** call `eglSwapInterval(display, 0)` once the context is current. Most devices
     respect it, some ignore it.
   - **Vulkan:** present with `VK_PRESENT_MODE_MAILBOX_KHR` when the surface supports it (see
     `vkGetPhysicalDeviceSurfacePresentModesKHR`), keeping FIFO otherwise. Many devices only support
     FIFO. Changing it means recreating the swapchain.
   - Without vsync the 1000 frames per second cap applies, as on desktop.
2. **A configurable frame limiter**, e.g. a multiple of the refresh rate or a fixed value, in place
   of the fixed 1000 frames per second, for battery and heat on laptops and phones.
