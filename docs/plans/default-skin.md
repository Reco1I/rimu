# A component-based default skin

Once approved, this plan is saved as `docs/plans/default-skin.md` and committed, and each phase is committed when
it's finished (any engine change in `verktex/` first, then the game with the submodule bump).

## Context

Every gameplay visual is a sprite of a skin texture. When the custom skin lacks a texture, `SkinTextures` falls
back to the default skin's `gfx/` folder in `shared/assets/`, which has 164 PNGs at a single resolution with no
`@2x`. The goal is a default skin drawn in code, the way osu!lazer's Argon skin is: shapes, gradients, Torus text and
choreography, at any resolution and in combo colours, with nothing to load.

Hit objects can now take pieces like these. They are Components that own their pieces and play their judgement
through `ChoreographyTransitions` (phases 2–4 of `animations.md`).

Decided with the user:

- **Scope.** Gameplay and the HUD: hit circles, sliders, spinners, approach circles, follow points, the cursor and
  its trail, judgements and their effects, and the HUD's score, combo, accuracy and pp numbers and its health bar.
  The countdown, skip, pause, break, fail, results and menu textures stay as assets, for a later plan.
- **Fallback per component, as in osu!lazer.** Each piece is looked up in the custom or beatmap skin first. If that
  skin doesn't have the piece's textures, the procedural piece is drawn instead. A skin with only a cursor then gets
  procedural circles.
- **Only visuals.** Sounds, gameplay and the skin.json format don't change.

When this is done, the textures the procedural skin replaces are deleted from `gfx/`.

## Design

### Pieces are looked up by component

- **`SkinComponent`** (`com/osudroid/ui/v2/skin/`) is an enum of what a skin can draw:
  - `HitCircle`, `SliderHeadCircle`, `SliderTailCircle`, `ApproachCircle`, `ReverseArrow`, `SliderTick`,
    `SliderBall`, `FollowCircle`, `SliderBody`
  - `Spinner`, `Judgement`, `HitLighting`, `FollowPoint`, `Cursor`, `CursorTrail`
  - The HUD's `ScoreFont`, `ComboFont` and `HealthBar`.
- **`GameplaySkin`** is an interface with `fun create(component): Component?`. It has two implementations:
  - `LegacySkin` builds today's texture pieces. It returns null when the custom or beatmap skin has none of the
    component's textures, which is checked with `SkinTextures.isSkinned`, ignoring the default assets.
  - `DefaultSkin` builds the procedural pieces and always returns one, except where it has no counterpart, such as
    `HitLighting`.
- **`SkinPieces.create(component)`** asks `LegacySkin` first, then `DefaultSkin`. This one place replaces today's
  piecemeal checks:
  - `isNewStyleSpinner` now only decides between the two legacy spinners.
  - The `sliderb-nd`/`-spec` layers, the default's `defaultapproachcircle` under Traceable, and the
    `isSkinned("hitXXX")` check for particles become choices inside `LegacySkin`.

### What each piece must do

Each piece implements a small contract, so the objects don't care which skin drew it:

| Contract | What the object calls | Implementations |
|---|---|---|
| `MainCirclePiece` (circles, slider head and tail) | `comboColor`, `comboNumber`, `showNumber`/`hideNumber`, `playJudged(result, time): Float` | `LegacyMainCirclePiece` (today's `CirclePiece`/`NumberedCirclePiece`, its hit is the burst of `GameObject.burst`); `DefaultMainCirclePiece` |
| `ApproachCirclePiece` | `comboColor`; the object keeps animating its scale and alpha | `Sprite` of `approachcircle`; a procedural ring |
| `SliderBallPiece` | `comboColor`, `rotation` / flip from the path's direction | today's ball (`sliderb` + `-nd`/`-spec`); a procedural ball |
| `FollowCirclePiece` | `playTracking(isTracking, time)`, `playJudged(result, time)` | today's follow circle; a procedural follow circle |
| `ReverseArrowPiece`, `SliderTickPiece` | `comboColor`, `playJudged` | `reversearrow` / `sliderscorepoint`; procedural |
| `SliderBodyStyle` | not a Component: it sets the `PathShape`'s widths and colours from the combo colour | today's skin.json `Slider` settings; the default style |
| `SpinnerPiece` | `progress`, `rotation`, `rpm`, `bonus`, `playCleared(time)`, `playJudged(result, time)` | `GameplaySpinner`'s classic and modern pieces, moved inside it; a procedural spinner |
| `JudgementPiece` | `play(result, time): Float`, as today | today's `JudgementPiece` (renamed `LegacyJudgementPiece`); a procedural one |
| `FollowPointPiece`, `CursorPiece`, `CursorTrail` | as the classes in `FollowPoints.kt` and `Cursors.kt` use them today | texture; procedural |

The rules for every piece:

- **What reacts to a result or a state** (`playJudged`, `playTracking`, `playCleared`) plays a
  `ChoreographyTransitions` at the time given and returns its end time, which the object's lifetime extends to,
  as `JudgementPiece` does today. It's seek-safe and never computed in `onUpdate`.
- **Values that follow the gameplay state** (spinner rotation and progress, ball rotation) are set by the object
  every frame. They depend only on the time.
- **The object keeps the animations that don't depend on the skin**: fade-in, Hidden and Traceable, the approach
  circle shrinking, and the fail animation. It plays them on the piece as a whole.
- **Where the piece is drawn.** Pieces drawn above every object (approach circles, the ball, the follow circle) go
  through `GameObject.drawIn` as today. Judgements go into the `judgements` layer.
- **Pooling.** Objects are pooled, so `GameObjectPool.preload` drops pooled objects and judgement pieces when
  `SkinTextures.revision` has changed since they were made. It already does that for spinners of the wrong style.

### Dimensions follow osu!stable

A fallback piece takes the place of a texture, so it has the size, origin and position the texture would have in
osu!stable, at 1x (the `@2x` size halved).

- **Mixing skins.** In a partial skin, a procedural piece lines up with the texture pieces around it. A procedural
  approach circle meets a texture hit circle at the same size, and a procedural follow circle surrounds a texture
  ball as stable's does.
- **The objects' code doesn't depend on the skin.** It keeps today's scales and origins: approach circles from 4x
  to 1x, the follow circle's tracking scales, the judgement scales of `JudgementPiece`, the spinner's layout in
  stable's 640×480 space, the cursor's `CursorSize` and expansion, and the HUD's layout positions.

Each piece has a fixed size, given as constants in `DefaultSkin`, with the texture size each one stands for. The
pieces draw inside those bounds.

| Piece | Size (stable, 1x) |
|---|---|
| Hit circle, slider head and tail, approach circle, slider ball | 128×128 |
| Follow circle | 256×256, so at its 1x scale it's twice the ball |
| Reverse arrow | 128×128 |
| Slider tick | 16×16 |
| Judgements | about 128 tall, centred where stable centres `hit300`… |
| Follow point | 128×64, as `FollowPoints.kt` already caps it |
| Cursor | 128×128 |
| Spinner | stable's pieces in the 640×480 space: `spinner-circle` 512, the approach circle 320 at 1x, the metre over the playfield, "clear" and the bonus at stable's positions |
| Health bar | `scorebar-bg`'s size, with the fill and marker where the textures put them |
| Score, combo and accuracy numbers | the height and digit advance of the default `score-*` digits, so a Torus number takes the space of a texture one |

The sizes in this table are checked against stable and against today's default textures in phase 1, before any
procedural piece is drawn. A unit test checks that each procedural piece reports the size of the texture it
replaces.

### The procedural pieces (modelled on Argon)

The pieces are built from what Verktex already has:

- `Box` with `radius = size / 2` and `border`: discs and rings drawn by the SDF in `ui.frag`, batched;
- `GradientBox`: linear gradients with radius and border;
- `Shadow(cutout = true)`: glows;
- `Circle.setPortion` and `drawArcStroke`: arcs;
- `PathShape`'s bands: the slider body;
- `Text` in Torus (`torus-bold.otf`, already in Verktex) and FontAwesome icons (`fa-solid.otf`): numbers and
  arrows;
- `BlendMode.Additive`: flashes.

| Piece | How it looks |
|---|---|
| Hit circle | A white outer ring (`border`). The inner fill is a vertical gradient from the combo colour to a darker one. The number is in Torus Bold. On a hit it flashes white, the ring expands and the fill fades. On a miss it fades. Kiai adds a flash on each beat, unless `isDisableKiai`. |
| Approach circle | A thin ring in the combo colour. |
| Slider body | `PathShape`: a light border, and a body in the darkened combo colour at about 0.7 alpha, with a gradient across its width. |
| Slider head and tail | The hit circle; the tail has no number. |
| Reverse arrow | A FontAwesome chevron on a small white disc. It pulses on the beat, in choreography. |
| Slider tick | A small ring. |
| Slider ball | A disc in the combo colour. |
| Follow circle | A ring with a soft glow. It scales up while tracking and down when tracking is lost or the slider ends. |
| Spinner | A ring with a progress arc (`drawArcStroke`), short ticks around it that rotate with the spin, and a centre disc that grows with the progress. The RPM, the bonus and "clear" are in Torus. There is one style: classic or modern is only a choice for legacy skins. |
| Judgement | "GREAT", "OK", "MEH" or "MISS" in Torus, coloured by the result. Hits grow slowly as they fade, and a miss drops and rotates. Slider points and ticks get a small dot, or nothing. Hit lighting and particles stay legacy-only. |
| Follow points | Small chevrons, or rounded boxes, fading in sequence. |
| Cursor | A white ring with a glow. It expands while pressed, following `isExpandCursor`. |
| Cursor trail | Soft dots through the trail's `ParticleSystem`, from a small texture generated once (a falloff into a `PixelBuffer`), with no asset. |

Colours:

- Combo colours stay the beatmap's. When it has none, or `forceOverride` is set, the skin's are used, and the
  default skin's colours move from its skin.json into `DefaultSkin`.
- Most skin.json settings only affect `LegacySkin`: the fonts' prefixes and overlaps, `comboTextScale`,
  `hitCircleOverlayAboveNumber`, the `Slider` widths, `allowSliderBallTint`, `spinnerNoBlink`.
- The slider border colour is used by both skins.

### Engine gaps (`verktex/`)

They are small, and each is added only when a phase needs it:

- **An arc component** over `drawArcStroke`, with `progress` and rounded caps, for the spinner and the health bar.
- **Fixed-width digits in `Text`** (Torus has tabular figures; otherwise each digit gets a fixed advance), so
  counters don't jitter. `TextureText.fixedCharWidths` already does this for texture fonts.
- **A texture from a `PixelBuffer`**, made at runtime, for the trail's particles. Check first whether `Textures`
  can already do it.
- **No radial gradient, blur or new shader is needed**: glows are `Shadow`, and gradients are linear.

### HUD

The HUD already has a registry of components (`HUDElements` and its skin data).

- **Numbers:** `HUDScoreCounter`, `HUDAccuracyCounter`, `HUDPPCounter` and `HUDComboCounter` use the skin's
  `SpriteFont` when it has the font's digits (`score-0`… with its prefix), and otherwise a Torus `Text` with
  fixed-width digits.
- **Health bar:** `HUDHealthBar` gets a procedural style for skins without `scorebar-bg`: a rounded track with a fill
  gradient and a glowing marker at its end. It sits next to the existing new and old texture styles.
- **Unchanged:** the HUD layout and the editor. A procedural element must keep the size the layout expects, within
  reason.

## Phases

1. **Lookup and contracts, with no visual change** (done).
   - Add `SkinComponent`, `GameplaySkin`, `SkinPieces` and the contracts.
   - Wrap today's pieces as `LegacySkin`. The default assets still count as skinned in this phase, so the result
     is identical.
   - Move the circle burst into `LegacyMainCirclePiece.playJudged`.
   - Make `GameplayHitCircle`, `GameplaySlider`, the spinners, follow points and cursors create their pieces through
     `SkinPieces`.
   - Make the pools drop pieces when the skin changes.
   - Record the stable dimension of each component, checked against the current textures, in `DefaultSkin`'s
     constants.
   - Unit tests for the order of lookup and the fallback.

   ### Phase 1 as done

   - `SkinComponent` is a sealed class rather than an enum: each component is typed by its contract
     (`SkinComponent<MainCirclePiece>`…), and `Judgement` carries the texture it's for. `SkinPieces.current` asks
     `LegacySkin` and then `DefaultSkin`: a component comes from the first skin that `provides` it, otherwise from
     the first that can create it at all, which is `LegacySkin` with the default textures until the default skin
     draws it. `createOrNull` is for what not every skin has (the hit lighting).
   - The contracts are in `skin/Pieces.kt`: `MainCirclePiece`, `SliderBallPiece`, `FollowCirclePiece`,
     `SliderBodyStyle` and `JudgementPiece`. Texture skins implement them in `skin/legacy/`:
     `LegacyMainCirclePiece` (was `CirclePiece`/`NumberedCirclePiece`), `LegacySliderBallPiece` (the ball with its
     `-nd`/`-spec` layers, one piece drawn through one proxy), `LegacyFollowCirclePiece`, `LegacySliderBodyStyle`
     and `LegacyJudgementPiece` (was `JudgementPiece`, now a container around its sprite). `HitLightingPiece` moved
     there too.
   - The approach circle, reverse arrows and slider ticks are plain Components: the objects tint, scale and fade
     them, whatever the skin. As osu!lazer's `DrawableSliderTick`, `GameplaySliderTick` (was `SliderTickSprite`)
     animates the tick and shows the skin's piece inside.
   - A circle's hit is `MainCirclePiece.playHit`: `GameObject.burst` draws the piece above every object and lets it
     play its hit. Texture skins burst, through `LegacySkin.burst`, which reverse arrows use too.
   - The follow circle's reactions moved into `LegacyFollowCirclePiece` as they were, with transforms. They become
     choreography with the default skin's follow circle, in phase 3.
   - The pools didn't need a revision check: `GameScene` already empties them, and renews the ticks' and follow
     points', each time a beatmap loads, after loading the beatmap's skin.
   - `SkinDimensions` (`skin/DefaultSkin.kt`) records the sizes of the default textures at 1x. They're osu!stable's
     sizes, though not every one is round: the ball is 118, the cursor 76 and the spinner's textures 666 to 852.
   - The spinners, follow points, cursors and the HUD don't go through `SkinPieces` yet. Their contracts are shaped
     in their own phases (4 to 6), next to the default skin's piece they need to fit.
   - `SkinPiecesTest` covers the order of lookup and the fallbacks.

2. **Procedural hit circles, approach circles and judgements** (done), the pieces seen most, so the look can be tuned
   early. `DefaultSkin` becomes the fallback: the default assets no longer count as skinned for the components it
   covers.

   ### Phase 2 as done

   - `DefaultSkin` provides the circles (hit circles and the slider's head and end, which are the same piece), the
     approach circles (Traceable's too) and the judgements. With the default skin, or a skin without these textures,
     they're drawn in code. Sliders' bodies, balls, follow circles, arrows and ticks still use the default textures.
   - `DefaultMainCirclePiece` (`skin/DefaultCirclePieces.kt`), modelled on `ArgonMainCirclePiece`, has the size of
     `hitcircle` (128). It has:
     - a dark disc;
     - a band in a vertical gradient of the combo colour;
     - a dark centre with the number in Torus;
     - a white outer ring.

     Its hit is choreography: a white additive flash, the ring growing out and fading, the whole circle growing and
     fading in 0.4 s. It's undone when the piece gets its number again, or shown as the head again, so pooled pieces
     start clean. Kiai lightens the band through `setCircleColor`, as with textures.
   - `DefaultApproachCircle` is a white ring with a soft glow (`Shadow` with `cutout`), 128 across, which the
     object tints.
   - `DefaultJudgementPiece` (`skin/DefaultJudgementPiece.kt`) shows "GREAT", "OK", "MEH" or "MISS" in Torus, in
     osu!lazer's colours for the results. It's played through `ChoreographyTransitions` keyed by `HitResult`.
     - A miss drops and turns.
     - Slider points and ticks show nothing, as in Argon.
     - It is a hit circle's size (128×128), its text `hit300`'s, and lasts at most 1.1 s, within `JudgementPiece.DURATION`.
   - Texts are rasterised at twice their size (`DefaultSkin.TEXT_RESOLUTION`) and scaled down, as the playfield
     scales the pieces up on screen.
   - The hit lighting still shows under the default skin's judgements, from the default `lighting` texture, until
     phase 7 removes it. The bursts follow the Burst effects setting for both skins.
   - The UI test `osu!/Default skin` shows the texture pieces above the default skin's, at the same size, looping
     an approach, a hit and every judgement. `DefaultSkinTest` covers what it provides and the approach circle's
     size. Pieces with text can't be built in unit tests, which have no fonts.

3. **Procedural sliders** (done): body style, head, tail, reverse arrow, ticks, ball and follow circle.

   ### Phase 3 as done

   - `DefaultSkin` provides every piece of sliders (`skin/DefaultSliderPieces.kt`):
     - **Body** (`DefaultSliderBodyStyle`): continues the circles at its ends, as `ArgonSliderBody` does. Its
       border is the circles' band of the combo colour, from 0.7 to 0.86 of their size. Inside is their dark centre,
       at 0.9 alpha. There is no shadow. Argon ignores the slider border colour, and so does this body: its band is
       the combo colour.
     - **Ball** (`DefaultSliderBallPiece`, 118 like `sliderb`): a gradient disc of the combo colour in a white
       ring, with a white dot ahead of it turning with the direction. It isn't mirrored on the spans going back.
     - **Follow circle** (`DefaultFollowCirclePiece`, 256): a white ring with a glow around a faint disc of the
       combo colour. Its reactions to tracking, ticks and the end are choreography at the gameplay clock's time.
     - **Reverse arrow** (`DefaultReverseArrow`, 128): Font Awesome's white chevron with a soft shadow, which the
       slider turns and pulses as before.
     - **Tick** (`DefaultSliderTick`, 16): a white ring around a dark centre.
   - `FollowCirclePiece` got `setComboColor`, called when the span starts.
   - The `osu!/Default skin` UI test also shows the slider pieces of both skins.

4. **Procedural spinner** (done). Adds the arc component in the engine.

   ### Phase 4 as done

   - **Engine:** `Arc` (`ui/shape/Arc.kt`, engine commit `555c9b9`), a ring or a part of one with `progress`,
     `startAngle` and round caps, over `drawArcStroke`. It's shown in the Shapes UI test.
   - **One spinner, its look in pieces.** `GameplaySpinner` is the only spinner: `GameplayModernSpinner`, which
     repeated its judging with its own pieces, is gone. The spinner drives a `SpinnerPiece` (`skin/Pieces.kt`) with
     `start`, then `update(rotation, progress)` on every spin. `playCleared` and `playBonus` give back what to show
     on top of the layer (the clear text, the bonus number), and `end` takes the pieces out. The spinner shows the
     judgement and the RPM counter, whatever the skin.
   - **Texture skins** keep both styles (`LegacyClassicSpinnerPiece`, `LegacyModernSpinnerPiece` in
     `skin/legacy/`). `LegacySkin` picks between them with `isNewStyleSpinner`. The classic style is still added
     below its layer (`isBelowLayer`), the modern one above.
   - **Judging now follows the classic spinner's for both styles.** A spinner shorter than 0.05 s is always
     cleared, which the modern one didn't do. A replay's spins no longer reset an already cleared modern spinner.
     The pool no longer checks the style of its spinners.
   - **`DefaultSpinnerPiece`** is as big as `spinner-top` and grows from 0.9 to 1 with the progress, as the new
     style does. It's built from:
     - a dark disc, hidden by Traceable, in a white ring;
     - 12 ticks turning with the spins;
     - a white progress `Arc` with round caps, filling clockwise from the top;
     - a centre growing with the progress;
     - a blue glow lighting up.

     "SPIN", "CLEAR" and the bonus are in Torus, and the centre flashes on each bonus spin. It has no approach
     circle, as Argon's.
   - The `osu!/Spinners` UI test cycles through the skin's spinner, the default skin's and both texture styles.

5. **Procedural follow points, cursor and trail** (done). Adds the texture made at runtime, if the engine needs it.

   ### Phase 5 as done

   - **Engine:** `PixelsSource` (`graphics/PixelsSource.kt`, engine commit `2e6f884`) is a texture source of
     pixels made in code, with `pixelBufferOf` to hand them to the backends. `PixelsSource.softDot` makes a white
     dot fading to its edge.
   - **Follow points:** they show the skin's `FollowPointPiece` inside the pooled follow point, which still places,
     turns and fades them for every skin, as osu!lazer's `FollowPoint` does.
     - `LegacyFollowPointPiece` is the `followpoint` animation, synced to the clock from `animationStartTime`, at
       most 128×64.
     - `DefaultFollowPointPiece` is a white chevron with a glow, 16×22.
   - **Cursor:** `CursorSprite` is now a container growing and turning the skin's `CursorPiece`, placed from its
     centre or its top left corner as the piece says (`isCentred`).
     - `LegacyCursorPiece` is the `cursor` texture.
     - `DefaultCursorPiece` (76) is a white ring with a glow around a dark disc and a white dot, always centred.
     - `cursormiddle` stays a texture of skins that have it.
   - **Trail:** the trail is drawn in the skin's `CursorTrailStyle`.
     - `LegacyCursorTrailStyle` keeps osu!stable's rules: separate images every frame, or a continuous trail with a
       `cursormiddle`.
     - `DefaultCursorTrailStyle` is a continuous trail of soft dots, a texture made once with
       `PixelsSource.softDot` and kept in `Textures` as `default-skin:cursortrail`.

6. **HUD** (done): Torus counters with fixed-width digits (an engine change), and the procedural health bar.

   ### Phase 6 as done

   - **Numbers:** the HUD's numbers (score, accuracy, pp, combo and the statistics counters) are `NumberText`s from
     the skin, `SkinComponent.ScoreFont` and `ComboFont`.
     - `LegacyNumberText` wraps the texture font (`SpriteFont`) when the skin has its digits.
     - `DefaultNumberText` is Torus, its digits as tall as the default `score-0` (46), so it takes a texture
       font's space in the layout. Each character is a text in its own cell, so `useFixedDigitWidth` gives every
       digit the widest one's width.
     - This needed no engine change: `Text` didn't get fixed digit widths, the cells give them.
   - **Health bar:** `HUDHealthBar` draws the skin's `HealthBarPiece`.
     - `LegacyHealthBarPiece` is the old element, both texture styles.
     - `DefaultHealthBarPiece` is laid out as `scorebar-bg` (695×44), with its fill where `scorebar-colour` is. It
       has a rounded dark track, a glowing fill up to the health and a glowing marker at its end that bulges and
       flashes with hits. It turns red under 30% health.
   - Spinners' bonus and RPM numbers in texture skins still use their score font (`ScoreNumber`).

7. **Remove the replaced assets** (done).
   - Delete from `gfx/`:
     - hitcircle and overlay, approachcircle, the `default-*` and `score-*` digits;
     - the slider textures (start, end, ball, follow circle, score point, points, reverse arrow, border, track);
     - the spinner textures (`spinner-*`);
     - the judgements (`hit*`), `lighting`;
     - `followpoint`, `cursor` and `cursortrail`;
     - `scorebar-*`.
   - Remove what pointed at them in `SkinTextures`: the `gfx/` fallback for these names,
     `GAMEPLAY_TEXTURES`/`GAMEPLAY_ANIMATIONS`, `defaultAliases`, and the default branch of `isNewStyleSpinner`.
   - The default skin's skin.json keeps only what still applies.
   - Update the game's `CLAUDE.md` (Rendering and Asset loading) and this plan's "As done" sections.

   ### Phase 7 as done

   - **Before deleting, the gaps were filled:**
     - **Hit lighting:** `DefaultSkin` provides it. `HitLightingPiece`, now in `skin/`, shows a soft glow made with
       `PixelsSource.softDot`, as big as `lighting`, so the Hit lighting setting still works without the texture.
     - **Numbers:** `ScoreNumber` (the results screen, the spinners' bonus and the RPM counter) draws the skin's
       `ScoreFont`: the skin's digits, or Torus without them.
     - **Results screen:** it shows the skin's judgement pieces for its counts, still, rather than the `hit300`…
       textures.
   - **Asked by the user on the way:**
     - The default ball shows a chevron in its middle, pointing where it goes, instead of a dot. The reverse arrow is
       the same chevron (`chevron()` in `DefaultSliderPieces.kt`), drawn with lines so it's centred exactly.
     - A slider's end has no circle in the default skin, as in osu!lazer's Argon Pro (`DefaultSliderTailPiece`): the
       body's round end shows it. The head hides once it's hit and becomes the end of a repeat; reverse arrows are
       still shown.
   - **Deleted:** the 78 gameplay and HUD textures, including `hitcircleselect`, `sliderborder`, `slidertrack` and
     `spinner-osu`, which nothing drew. What's left in `gfx/` is for the menus, the results, pause, fail, break, skip
     and countdown screens.
   - **`SkinTextures`:** the `defaultapproachcircle` alias is gone: Traceable's approach circle is the default skin's,
     drawn in code. The other lookups stay: their fallback to `gfx/` still serves the textures that are left.
   - **Not changed:**
     - There was no default `skin.json` in the assets to trim.
     - `isNewStyleSpinner` still treats the default skin as new style, which only matters for a beatmap skin with some
       spinner textures.
   - **On screen** (desktop UI tests, after the deletion): the default skin draws every piece, sliders end on their
     body, and the HUD and spinners show their numbers in Torus.
   - **Found by checking on screen**, before the deletion:
     - Texts of the default skin are placed by the ink of their glyphs, by hand (`placeText`). They're laid out at
       the size they're rasterised at, and their piece's size clamped them, which moved the circles' number down and
       GREAT to the side.
     - A component's `color` carries its alpha, so tinting the follow circle's fill made it opaque.

## Files

- **New:**
  - `com/osudroid/ui/v2/skin/`: `SkinComponent`, `GameplaySkin`, `SkinPieces`, the contracts, `LegacySkin`, and
    `DefaultSkin` with its pieces (`DefaultMainCirclePiece`, `DefaultSpinner`, `DefaultJudgementPiece`…).
- **Changed:**
  - `ru/.../game/GameplayHitCircle.kt`, `GameplaySlider.kt`, `GameplaySpinner.kt`, `GameplayModernSpinner.kt`;
  - `com/osudroid/ui/v2/game/CirclePiece.kt`, `JudgementPiece.kt`, `GameObject.kt` (burst into the legacy piece),
    `FollowPoints.kt`, `Cursors.kt`, `SliderTicks.kt`;
  - `ru/.../game/GameObjectPool.kt`;
  - `com/osudroid/ui/v2/SkinTextures.kt`, `ru/.../skins/OsuSkin.kt`;
  - the HUD elements in `com/osudroid/ui/v2/hud/elements/`;
  - `GameScene.kt` (combo colours, where pieces are created);
  - the UI tests in `com/osudroid/ui/v2/test/` (`OsuGameplayUITests`, `GameplayObjectHarness`).
- **Engine:** an arc component in `ui/shape/`, fixed digits in `ui/text/Text.kt`, and maybe a runtime texture in
  `texture/`, each with a desktop test and a `VerktexUITests` entry.

## Verification

- **Each phase:** `./gradlew :shared:desktopTest :shared:compileAndroidMain :verktex:shared:desktopTest
  :verktex:shared:compileAndroidMain :verktex:desktopApp:compileKotlin`, checking the exit code. The Android host
  tests can't run on this machine's JDK.
- **Unit tests:**
  - The lookup order: the beatmap skin, then the custom skin, then `DefaultSkin`.
  - A skin with only some textures gets procedural pieces for the rest.
  - Pools drop pieces after a skin change.
  - The procedural pieces' judgement choreography returns the right end times, and it gives the same state when
    seeking back.
- **UI tests (run by the user):**
  - `osu!/Hit circles`, Sliders and Spinners get a toolbar toggle between the default skin and a texture skin, and a
    new `osu!/Default skin` test shows every piece and judgement side by side.
  - Engine changes get their entries in `VerktexUITests`.
- **On screen (by the user):**
  - A quickplay with the default skin, then with a full custom skin and a partial one.
  - Seeking back and forth, Hidden, Traceable, kiai, autoplay spinners, and failing.
  - After phase 7, check that no gameplay piece falls back to the "missing" texture.
