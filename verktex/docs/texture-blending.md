# Texture blending

How Verktex stores textures and blends what it draws, and what's left to do about it. This came out of
a dark border around some sprites (e.g. faded hit circles) on Android, which the gutter and
premultiplied alpha below fixed.

## Current state

- **Premultiplied alpha everywhere.** Textures hold premultiplied pixels (colour already multiplied by
  alpha) and every built-in fragment shader outputs premultiplied colours. Linear filtering then never
  mixes the colour of transparent texels into a sprite's edges.
  - **Android:** bitmaps are premultiplied by default and are uploaded as they are.
    `AndroidTextureSource.premultiplyRows` only runs for bitmaps with `isPremultiplied == false`.
  - **Desktop:** `stb_image` gives straight alpha, `PixelsTextureSource.decode` premultiplies it once.
    `DesktopFontSource` writes glyphs as `(coverage, coverage, coverage, coverage)`.
  - **Colours given to shaders** (vertex colours, `uColor`, border colours) keep straight alpha, so code
    calling the engine is unchanged. Each shader premultiplies them, e.g.
    `texture(uTexture, vUV) * vec4(vColor.rgb * vColor.a, vColor.a)`.
- **Blend modes** (`BlendMode.kt`) blend premultiplied sources:

  | Mode       | RGB                          | Alpha                         |
  |------------|------------------------------|-------------------------------|
  | `Opaque`   | `One, Zero`                  | `One, Zero`                   |
  | `Alpha`    | `One, OneMinusSrcAlpha`      | `One, OneMinusSrcAlpha`       |
  | `Additive` | `One, One`                   | `SrcAlpha, One`               |
  | `Multiply` | `DstColor, OneMinusSrcAlpha` | `DstColor, OneMinusSrcAlpha`  |

  A `Custom` mode ported from straight alpha uses `One` where it had `SrcAlpha` as the RGB source
  factor (see the hit lighting in `GameplayEffects`).
- **Atlas gutter.** Every atlas region keeps `GraphicsTexture.regionPadding` (1) pixels free around it,
  filled with copies of its edge pixels when it's uploaded (`extrudePixels`), so filtering at the edge
  of a sprite never reads its neighbour or uninitialized memory. Standalone textures (one region the
  size of the texture) have no gutter, clamping to the edge does the same.

## Pending

1. **Checking the rest on Android.** The border is gone on the Adreno 610 phone. What the desktop UI
   tests didn't cover still needs a look there, on each backend (OpenGL ES 3, OpenGL ES 2, Vulkan):
   - hit lighting (custom blend), the snow effect and the health bar (`Additive`),
   - blurred backdrops (`UICaptureContainer`, `blur.frag`),
   - videos (`video.frag`) and particles,
   - anything drawn with `Multiply` (nothing uses it at the moment).

   A mistake shows as translucent things too bright or too dark, or with a dark or light halo.
2. **Uninitialized texture storage.** `ESTexture`/`GLTexture` create textures with `glTexImage2D(...,
   null)` and Vulkan images start in `VK_IMAGE_LAYOUT_UNDEFINED`, so free space holds whatever the
   driver left. The gutter makes this harmless for sources written in one piece, but a source written
   in several parts (`GraphicsTexture.Source.writePixels` allows it) gets no extruded gutter, so its
   edges can still read garbage. Clear textures to transparent when they're created, or extrude the
   bounding box of the parts.
3. **Merging additive and alpha batches.** With premultiplied colours, additive is `Alpha` with a
   source alpha of 0: `One, OneMinusSrcAlpha` then adds the colour. Drawing additive sprites that way
   lets them share batches with alpha ones instead of splitting on every blend change (the health bar,
   effects and particles alternate). It needs a way to tell the shader to zero the alpha (a vertex
   attribute or a flag in the colour), and changes the destination alpha, which only matters when
   drawing into a framebuffer that is composited later.
4. **`Additive`'s alpha factors.** They stayed `SrcAlpha, One` so the destination alpha is what it was
   with straight alpha (`a²` added). `One, One` would be the consistent premultiplied choice. It only
   matters for framebuffers whose alpha is used afterwards.
5. **Simpler border compositing in `ui.frag`.** It still composites the border over the fill in
   straight alpha (with a division by the resulting alpha) and premultiplies at the end. In premultiplied
   space it's `border + fill * (1 - border.a)`, without the division.
6. **Tests.**
   - `PixelsTextureSource.premultiply` and the premultiplied glyphs of `DesktopFontSource` have no tests.
   - `AndroidTextureSourceTest` can't check the channel order, Robolectric copies ARGB ints instead of
     RGBA bytes.
   - A test drawing a translucent sprite through a real backend (the desktop one) and reading the
     pixels back would catch a shader that forgets to premultiply.
7. **Desktop screenshots keep the framebuffer's alpha.** `DesktopGameHost` saves 4 channels from
   `glReadPixels`. The window is opaque so it looks right, but anything that leaves an alpha below 1 in
   the back buffer makes those pixels translucent in the PNG. Save 3 channels, or force alpha to 255.
8. **Mipmaps, if they're ever added.** A 1 pixel gutter is enough for bilinear filtering without
   mipmaps. Mipmapped atlases need gutters of `2^levels` pixels, and the mip levels must be generated
   from the premultiplied pixels (which `glGenerateMipmap` does, as it averages what's stored).
9. **Rules for new code.**
    - A new shader must output premultiplied colours and premultiply the straight colours it's given.
    - A new texture source must write premultiplied pixels (`GraphicsTexture.PixelTarget` says so).
    - The storyboard renderer (`legacy/storyboard/`), once rewritten on Verktex, must map its additive
      parameter to `BlendMode.Additive` and not port straight alpha factors.
10. **Unrelated failing tests.** `ColorInheritanceTest`, `PointerEventTest` and `ThemedValuesTest` in
    `:verktex:core:jvmTest` failed before any of this (`PointerEventTest` with "Platform implementation
    not initialized").
11. **History.** `6ca611ee0` (colour bleeding) and its revert `aed09b787` cancel out and aren't pushed,
    so they can be dropped before pushing.
