# Hit objects in osu!pixels through a ScalingContainer

## Context

Hit objects are placed and judged in `GameResolution` pixels. Every object converts its osu!pixel position with
`HitObject.convertPositionToRealCoordinates` (`screenSpaceGameplay*`), scales itself by `gameplayScale * h / 480`
(`screenSpaceGameplayScale`), and hit tests the cursor in screen pixels (`GameObject.kt:220`). The slider body is
the only thing already drawn in osu!pixels, with its own `MAP_ACTUAL_* / MAP_*` scale (`GameplaySlider.kt:371`).

Goal: objects, follow points, ticks and judgement effects live in a 512x384 osu!pixel playfield inside a
`ScalingContainer`, as in osu!lazer, and look exactly as in osu!droid. Decided with the user:
- **Keep osu!droid's rounding**: the playfield is `MAP_ACTUAL_HEIGHT = int(0.8h)` tall and
  `MAP_ACTUAL_WIDTH = MAP_ACTUAL_HEIGHT / 3 * 4` wide, and objects are scaled by `h / 480`.
- **Judgement moves to osu!pixels**, so replays don't round-trip through screen coordinates.

## Geometry (exact osu!droid mapping)

Let `f = h / 480` and `kx = MAP_ACTUAL_WIDTH / (512 f)`, `ky = MAP_ACTUAL_HEIGHT / (384 f)`. Both are within 0.3% of
1, and both are exactly 1 at 720p.

- `ScalingContainer` (verktex, unchanged): `mode = Height`, `referenceSize = (640, 480)`. It fills the existing
  `playfield` container (GameResolution sized), so its factor is `f`: sprites are scaled by `f`, as
  `screenSpaceGameplayScale` does now.
- Inside it, an osu!pixel `Container` of size 512x384, with `anchor`, `origin` and `scaleOrigin` at `Center`, and
  `scaleX = kx`, `scaleY = ky`. A point `p` lands at `p · MAP_ACTUAL/MAP + (GameResolution − MAP_ACTUAL)/2`, which is
  exactly `convertPositionToRealCoordinates`.
- The residual: a sprite's size picks up `kx`/`ky`, so it is up to about 0.2% anisotropic (about 0.2px on a 100px
  circle). Positions and slider bodies are exact. Everything else follows from the container: the playfield size
  setting (`applyPlayfieldSizeScale`, still on the outer `playfield`), clipping, and input via
  `toLocalSpaceFromScene`.

## GameResolution (removed, see step 8)

`UseLegacyResolutionPolicy` (`OsuSettings.kt:86`) was upstream osu!droid's fixed render resolution, 1280 pixels
wide. That job now belongs to Verktex's `ResolutionScale`: rendering at 1280 wide is a matter of setting the render
scale. Nothing toggles the key, so `GameResolution` is always `1280 x int(1280 · short / long)`. That is now only a
layout space, which a `ScalingContainer` with `Mode.Width` and a reference width of 1280 already provides. Layout
(1280-wide design units) and render resolution (`ResolutionScale`) become independent.

- Delete `UseLegacyResolutionPolicy` and the dead non-legacy branch of `GameResolution.measure`. No migration is
  needed: the database and settings of other versions aren't imported.
- The gameplay root's `playfield` container becomes `ScalingContainer(Mode.Width, 1280)`. That replaces its fixed
  `GameResolution` size and the `root.bounds.width / GameResolution.width` factor in `applyPlayfieldSizeScale` and
  `resetPlayfieldSizeScale`. The playfield size and position offsets stay as translations and scale of an inner
  container.
- osu!droid's rounding happened in upstream's 1280-wide render pixels, so it only needs the integer height
  `int(1280 · h / w)`, floored as `measure` did. `GameplayPlayfield` computes `MAP_ACTUAL_*` from it, and so does
  `CircleSizeCalculator`'s old osu!droid scale (used by `ModReplayV6`), as a small pure function instead of a global.
  It's deliberately independent of `ResolutionScale`, so positions and circle sizes match upstream at any render
  scale. At the render scale that gives 1280 wide, it's pixel for pixel what upstream does.
- It follows the window rather than the display. On a desktop window with another aspect ratio, that's more correct
  than now.

## Steps

1. **`GameplayPlayfield`** (new, `shared/.../com/osudroid/ui/v2/game/GameplayPlayfield.kt`): a `ScalingContainer`
   that owns the osu!pixel container (geometry above, recomputed when it's measured) and two layers in it:
   - `underlay`: follow points and hit lighting.
   - `objects`: circles, sliders, judgements, particles and bursts.

   API:
   - `toPlayfield(sceneX, sceneY): Vector2`, through `toLocalSpaceFromScene`.
   - `toResolution(osuPx): Vector2`, for what stays in screen pixels: cursors, autoplay cursor, flashlight.

   The clipping that `mgScene` had (`ClipMode.Bounds`) goes on the scaling container. The 512x384 box must not
   clip, because circles overflow it.
2. **GameScene layering** (`GameScene.kt:382-399`): `bgScene` keeps spinners, countdown and combo burst. It is
   followed by `GameplayPlayfield` (replacing `mgScene`, including the clear at `:3279`), then `fgScene`.
   - `effects.setLayers(playfield.underlay, playfield.objects)`.
   - `FollowPointConnection.addConnection` and `clearAll` use `underlay`.
   - The fail animation (`:2802`) applies its `y*1.01` drift in screen units, so it stays identical: convert the
     delta through `ky·f` and the offset.
   - Z-order change: follow points and hit lighting now draw above spinners instead of below. They don't overlap in
     practice.
3. **Objects in osu!pixels**:
   - `GameplayHitCircle`, `GameplaySlider` (head, tail, ball, follow circle, reverse arrows, ticks), `SliderTicks`
     and `FollowPoints` use `gameplayStackedPosition`/`EndPosition` and `gameplayScale` instead of the
     `screenSpace*` ones.
   - Slider body: drop the `widthScale`/`heightScale` scale. `halfWidth = sliderBodyWidth * gameplayScale / kx`, and
     the same for the border, so the width is the same in pixels.
   - `GameplayEffects.setScale(gameplayScale)` (`GameScene.kt:755`), and the harness's call at `:251`.
   - Spinners stay screen-space in `bgScene`, unchanged.
4. **CursorEvent** (`com/osudroid/game/CursorEvent.kt`):
   - `obtain` takes the osu!pixel point from `GameplayPlayfield.toPlayfield`, and is called at `GameScene.kt:2626`.
   - New `playfieldPosition` (gameplay space, HR flipped like the objects are).
   - `trackPosition` becomes `playfieldPosition` with HR undone (`y → 384 − y`), the same values replays store now.
   - `position` (screen) stays for the flashlight, cursor sprites and spinner rotation.
5. **Judgement in osu!pixels**:
   - `GameObject.kt:220`: distance from `gameplayStackedPosition` to `cursorEvent.playfieldPosition` against
     `gameplayRadius`.
   - Slider tracking and follow radius (`GameplaySlider.kt:1062`, `:1165`, `:1188`, `:1299`) use osu!pixel ball and
     tick positions.
   - Listener callbacks (`onCircleHit`, `onSliderHit`, `onSliderReverse`…) carry osu!pixel positions to
     `getNearestCursorId` (compare `playfieldPosition`) and `hud.onAimJudgement` with `gameplayRadius`, plus to the
     effects.
   - The flashlight call converts with `toResolution`.
   - Check autoplay and autopilot, which move cursors to object positions: convert with `toResolution` for the
     sprite and keep the event in osu!pixels.
6. **Replays** (`Replay.kt:506`, `fromCursor`): playback builds cursor events from stored track coordinates (HR
   flipped into gameplay space) directly, instead of `trackToRealCoords` and back. Recording is unchanged (it stores
   `trackPosition`).
7. **Cleanup**:
   - Remove `HitObject.screenSpaceGameplay*` and `Slider.screenSpaceGameplayStackedEndPosition` once unused.
   - `ModReplayV6:70` keeps its value as `gameplayScale * GameResolution.height / 480` inline, since it's a
     stacking-compat threshold and not drawing.
   - Remove `Utils` conversions that end up unused (`trackToRealCoords` remains for spinners and `Countdown`).
8. **Remove `GameResolution` entirely** (decided with the user). Each user gets its 1280-wide space from a
   `ScalingContainer(Mode.Width, referenceSize.x = 1280)` and reads sizes from its own layout, not a global:
   - **Scenes that already scale by `windowWidth / GameResolution.width`** (`ScoringScene.kt:176-200`,
     `GameplayMenus.kt:49`, GameScene's `playfield`): use a ScalingContainer. Replace `GameResolution.width/height`
     with the container's reference size, as `measuredSize` or fill dimensions.
   - **Placed at the centre or edges** (`Cursors.kt:284`, `GameplayEffects.kt:71`, `:95`, `:191`, `:496`
     (Countdown), `SpinnerRpmCounter.kt`, the spinners' `trackToRealCoords(256,192)` and `background.scale`, the
     harness's `:290` and `:388`): use anchors (`Anchor.Center`, `BottomRight`…) or the parent's bounds. The
     `SpinnerRpmCounter` and spinner scales that come from the screen height and width use the scaling container's
     reference size.
   - **`SkinLayout.kt:46-53`** (bottom-relative y) and the snow effect (`com/acivev/ui/Effect.kt`): the parent's size.
   - **`CircleSizeCalculator`** (old osu!droid scale, also used by difficulty and `ModReplayV6`): it's not in the UI
     tree, so it gets the integer 1280-normalized height from a pure function of the window's aspect, kept next to
     it. Upstream depends on that same height, so values don't change. Tests keep the `max(1, …)` fallback.
   - **Delete** `Constants.MAP_ACTUAL_*` (moved into `GameplayPlayfield`) and the unused `Utils` conversions
     (`trackToRealCoords`, `realToTrackCoords`, `changeTrackToRealCoords`, `scaleToRealC`, `scaleToTrack`,
     `flipRealCoordsVertically`, `flipY` once unused, and `toRes`, which is the identity), then
     `GameResolution.kt` and its `measure()` call (`OsuSettings.kt:286`).
   - **Update** `CLAUDE.md`'s mention of `GameResolution` and `verktex/docs/engine-consolidation.md:580`. The doc
     is in the engine repository, so it needs an engine commit first and then a submodule bump.
9. **Test harness** (`GameplayObjectHarness.kt:83-121`): use `GameplayPlayfield` in place of its
   `background`/`playfield` containers, so the UI tests show the real layering and input.

## Verification

- `./gradlew :shared:desktopTest` (includes `ReplayTest`, which checks track↔real conversion; update it to the new
  playback path) and `:shared:checkCommonSources`.
- Screenshots before and after, at the same frame, compared pixel-wise:
  - `./gradlew :desktopApp:runUITests --args="--test 'Gameplay/Hit circles' --screenshot build/hc.png --delay N"`.
  - The same for sliders, follow points and spinners.
  - Run both at 16:9 and at a non-16:9 window (e.g. 2400x1080) to exercise the rounding. Expect identical
    positions and ≤1px size differences.
- Screenshots of `ScoringScene`, the pause menu, and the main menu with the snow effect, compared with before.
- `./gradlew :desktopApp:run --args="--quickplay '<beatmap>'"`: autoplay hits everything at SS. Then play
  `--no-autoplay` with HR and with playfield size < 100% to check that hits register where the circles are.
- Record a replay, then play it back with and without HR. The results must match the live play.
- Commits, never pushed (per memory): the engine doc first, then the submodule bump. On the game side, split
  into (1) the playfield and osu!pixel objects and judgement, then (2) the removal of `GameResolution`.
