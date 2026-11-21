# One way to animate: transforms, transitions and choreography

## Context

Animations were written three ways:

- **By hand in `onUpdate`**: about 15 engine components and several game UI components smoothed values every frame
  with `clock.interpolateFloat`/`interpolateColor`, `FloatAnimation` or `FloatInterpolation.floatAt`, next to the
  Transforms API (`verktex/.../ui/Transforms.kt`) and the interaction transitions (`ui/InteractionState.kt`) instead
  of through them. `Button.onUpdate` scaled and faded by hand when `transitions == null`, for example.
- **Hit effects outside the hit objects**: `GameScene` judges an object, then calls `GameplayEffects`
  (`ru/nsu/ccfit/zuev/osu/game/GameplayEffects.kt`). That class takes pooled `GameEffect` sprites from
  `GameObjectPool`, adds them to the playfield and choreographs them. The objects (`GameplayHitCircle`,
  `GameplaySlider`, `GameplaySpinner`) don't know how they look when judged. On a seek, `GameScene` has to finish
  and detach leftover effect sprites by hand (the "detach lingering hit effects" loop in `GameScene`).
- **Hit objects that aren't Components**: `GameObject` is a plain class that adds loose sprites to
  `GameplayPlayfield.objects`/`underlay`.

The goal is one vocabulary:

- transforms and transitions for anything triggered by a state change;
- `Choreography` for anything on the gameplay clock;
- frame smoothing (`FrameClock.interpolateFloat`) only for values following a target that keeps moving.

Hit objects become Components that own their pieces and play their own judgement effects. They use a
choreography-based API, like transitions, keyed by hit result.

Decided with the user:

- Smoothing is kept for moving targets, through one primitive.
- Hit objects are converted to Components, osu!lazer's `DrawableHitObject` being the model, rather than only owning
  their effect pieces.
- The game's UI is included in the sweep, not only Verktex.

Each phase is committed when it's finished: the engine (`verktex/`) first, then the game with the submodule bump.

---

## Phase 1: transforms for state changes (done)

Engine commit `7413c00` and game commit `da53ca495`.

### Engine API (`ui/Transforms.kt`, `ui/Component.kt`)

- **The latest transform to start on a property wins.** `TransformTracker` keeps transforms sorted by start time and
  applies them in that order. A transform that starts before another on the same property has ended replaces it,
  and the replaced one's `after` callback isn't called. One that ended before the next started just finishes, and
  its callback is called. A transform that starts at once takes its start values when it's added, so retargeting
  starts from the current value. Callbacks run once the list is consistent, and the transforms they add are applied
  in the same frame.
- **Any property can be animated.** `AnimatableFloat`/`AnimatableColor` (`animatableFloat(get, set)`,
  `animatableColor(get, set)`) with `animate`/`animateColor`, on a component and on `TransformSequence`. The
  property instance identifies the property for replacement. `scaleXTo`, `scaleYTo` and `moveToY` animate a single
  axis.
- **`TransitionSlot`** (`transitionSlot()`): running it again cancels what its last run started, still running or
  waiting, without calling their callbacks. The other transforms of the component are left alone. `StateTransitions`
  run through one.
- **`Component.defaultTransitions`**: a component's built-in feedback to its interaction state. The user's
  `transitions` replace it, and having it doesn't make a component `isHoverable`. Set `tracksInteractionState` and
  override `onInteractionStateChanged(state)` to react to the state without transitions (`StateHighlight`).
- `FloatAnimation` was removed. `FrameClock.interpolateFloat`/`interpolateColor` are documented as the smoothing for
  moving targets only. The rule is in `verktex/CLAUDE.md`.

### Migrated

| Component | Now |
|---|---|
| `Button` | `defaultTransitions`: pressed scales to 0.95 and disabled fades to 0.5, over 0.2 s with `OutQuint` |
| `StateHighlight` | Its opacity is a transform of its owner, started from `onStateChanged` (Button, Checkbox, TextInput, SeekBar) |
| `Checkbox` | The check (`OutBounce`) and background color animate from `onValueChanged`; the background is themed again once there |
| `Card` | The content height fraction and chevron rotation animate from `isExpanded`/`isCollapsible` |
| `Modal` | `show()`/`hide()` run `animateShow()`/`animateHide()` (overridable, replacing `onShowAnimation`/`onHideAnimation`) in a slot whose `after` calls `onShown`/`onHidden` |
| `Select` | The chevron animates from the dropdown's `onExpand`/`onCollapse` (`Dropdown` now calls `onCollapse`) |
| `Dropdown` | The fade and scale run in a slot on `show()`/`hide()`, with the removal in `after`. The menu position stays smoothed |
| `FormControl` | The alpha animates from `isEnabled`. The reset button slides when "not default" changes (checked every update, `isDefault` being open) |
| `TextInput` | The border color animates on focus changes. An error sets it red, then animates it back |
| `SeekBar` | The thumb scale and the stretch's spring (`OutElastic`) animate on press and release. `thumbX` stays smoothed |
| `TabSelector` | The indicator fades on selection. Its position and width stay smoothed, through `interpolateFloat` instead of `floatAt` |
| `MainScene` | The logo, menu, gradient and player button animate from `isMenuExpanded`. The music button animates when its visibility changes. The beat flashes fade out from the beat that lights them |
| `OsuLogo` | The click flash and the stop of the effects are transforms. The beat bounce follows the music through `interpolateFloat` |
| `BeatmapPanel`, `BeatmapSetPanel` | The border, extra width, glow and difficulties animate from `isExpanded`. A set panel is recyclable once its slot finishes, and resets at once when recycled |
| `SettingsMenu` | A section fades in when it's generated |

Values following moving targets stay smoothed: `Verktex.imeShift`, the `Dropdown` position, the `SeekBar` thumb, the
`TabSelector` indicator, the logo's bounce, `HUDHealthBar`, `HUDBackButton`'s hold, `RadialVisualizer` and the
flashlight's follow.

### Known limitations

- `Component.scaleX` and the other properties ignore changes smaller than 1e-3 (`almostEquals`), so an eased
  transform can end up to 1e-3 short of its target. This was the case before too.
- The focus border of `TextInput` and the borders of the song select panels don't follow a theme change made while
  they're shown.
- The `TabSelector` indicator doesn't show in the `Verktex/Controls` test, already before this work.

---

## Phase 2: engine support for gameplay pieces (done)

Engine commit `d9bc5b9`.

1. **`ChoreographyTransitions<S>`** (`verktex/.../ui/ChoreographyTransitions.kt`, `docs/choreography.md`). The
   gameplay-clock counterpart of `StateTransitions`: blocks keyed by any state, played into a component's
   `Choreography` at a given time.

   ```kotlin
   val transitions = choreographyTransitions<HitResult> {
       this[HitResult.Miss] = { fadeInFromZero(0.15f).moveToY(40f, 0.5f, Easing.InQuad) }
       otherwise { … }
   }
   val endTime = transitions.play(piece, result, time)   // piece.choreography().at(time).block()
   ```

   - Blocks are `Choreography.() -> Unit`. A state without a block plays `otherwise`, or nothing.
   - It keeps no state, so one instance (e.g. in a companion) serves every pooled piece. Nothing is polled or
     cancelled: the steps stay in the choreography, so seeking stays correct. A pooled piece clears its choreography.
   - `play` returns the choreography's `endTime`, or `time` when nothing played, for callers to extend lifetimes with.
   - It's separate from `StateTransitions` on purpose: those run transforms, which are removed once they end and
     can't be seeked back.

2. **`Proxy` component** (`verktex/.../ui/Proxy.kt`, `docs/proxies.md`). It draws a target component at another
   point of the draw order. Each hit object owns all its pieces as a Container, but some pieces must draw in other
   layers: approach circles and judgements above every object, hit lighting below every object.
   - `Container.proxy(target)` adds one. While the proxy is attached, the target's `proxy` is set and
     `Component.draw` skips it unless the proxy draws it. Detaching the proxy, or changing its `target`, gives the
     target back to its parent. One proxy per target, the last attached.
   - The target's transformation chain is brought up to date before it's drawn (`validateTransformationChain`), so a
     layer drawn before the owner doesn't use last frame's matrices.
   - Alpha and tint: the proxy pushes the accumulated alpha, and the tint of `tintChildren` ancestors, of the
     target's parent chain, on top of its own layer's. An invisible ancestor hides the target.
   - Culling goes through the target's bounds. A layer of proxies must be laid out over its targets (`fill()`).
   - `ProxyTest`, `ChoreographyTransitionsTest`, and the `Verktex/Proxies` UI test: cards whose dots overlap the
     next card, with buttons to toggle the proxies and fade the cards.

---

## Phase 3: hit objects as Components (done)

Files: `ru/nsu/ccfit/zuev/osu/game/GameObject.kt`, `GameplayHitCircle.kt`, `GameplaySlider.kt`, `GameplaySpinner.kt`,
`GameplayModernSpinner.kt`, `GameObjectPool.kt`, `GameScene.kt`, `com/osudroid/ui/v2/game/GameplayPlayfield.kt`,
`com/osudroid/ui/v2/test/GameplayObjectHarness.kt`.

1. **`GameObject` becomes an abstract `Container`.** It fills the playfield layer at (0, 0) with the playfield's
   size, so pieces keep their absolute osu!pixel positions.
   - `update(dt: Float)` is renamed `updateJudgement(dt)`, so it doesn't overload `Component.update(clock)`.
     `GameScene` keeps driving judgement in the same order; `onUpdate` doesn't judge.
   - The lifetime API (`lifetimeEnd`, `extendLifetime`, `onExpire`, `isJudged`) is unchanged.
2. **Pieces become children of their object.** The object is added to `playfield.objects` at index 0, which keeps
   today's order: the `scene.add(0, …)` calls put newer objects below older ones. Within an object, pieces keep
   their relative order (for sliders: body < ticks < tail < arrows < head, then the ball and follow circle).
3. **Playfield layers.** `GameplayPlayfield` goes from `underlay` and `objects` to:
   - `underlay`: follow points and lighting proxies;
   - `objects`;
   - `approachCircles`: proxies;
   - `judgements`: proxies, above everything.

   Approach circles are proxied into `approachCircles`. The slider ball and follow circle are added with `+=` today,
   which draws them above every object's body, so they're proxied there too. Proxies are created when an object is
   initialized and removed by `onExpire`, then pooled with the object.
4. **Pooling and seeking.** `GameObjectPool` keeps pooling objects. `onExpire` removes the object's container and
   its proxies at once. The "detach lingering hit effects" loop in `GameScene` goes away.
5. **No visual change** in this phase, verified before Phase 4.

### As done

Engine commit `9ff29f2` and the game commit after it. What differs from the steps above:

- `GameObject` fills its layer with `FillAvailable` (the spinners' layer is the 1280 wide space, not the playfield).
  `GameObject.drawIn(layer, piece)` adds a pooled `Proxy` on top of `layer`, and `removeProxies()` takes them all
  out; `onExpire` calls it and removes the object.
- A proxy doesn't draw a target without a parent (engine): pieces are still hidden with `removeSelf()`, and their
  proxies follow.
- The slider ball, its layers and the follow circle are proxied when the ball shows (the span starts), as they were
  added to the layer then. Approach circles are proxied in `init`.
- Spinners: the classic spinner adds itself at the bottom of its layer and the modern one on top, as their pieces
  were. Their clear text and bonus score are drawn on top of the layer through proxies (`showOnTop`), above the
  combo bursts as before. The RPM counter stays a child of the layer.
- The judgement effects (`GameEffect`, still from `GameplayEffects` until Phase 4) moved to the `judgements` layer.
  So did the "detach lingering hit effects" loop of the seek, which goes away in Phase 4. They now always draw above
  the approach circles, instead of in the order they were added.
- `GameEffect`, `BreakAnimator` and `Countdown` aren't `GameObject`s anymore: they only used it for `update(dt)`.
- The fail animation goes into the objects: each of their pieces falls and turns as the layers' children do. It also
  applies to the `judgements` layer.
- `GameplaySpinner.background` is `spinnerBackground`, as it clashed with `Component.background`.

---

## Phase 4: judgement effects inside the hit objects (done, to be verified on screen)

1. **Results go back to the object.**
   - `GameObjectListener.onCircleHit`, `onSliderHit` and `onSpinnerHit` return the texture name of the judgement
     that `GameScene.registerHit` already computes (`"hit300g"`, `"hit100k"`, `"sliderpoint30"`, `"hit0"`, or null
     for none).
   - A small `HitResult` (Great, Ok, Meh, Miss, SliderPoint, Tick) keys the `ChoreographyTransitions`.
   - The flashlight, vibrations, HUD and statistics stay in `GameScene`. `GameScene` and `GameplayObjectHarness`
     stop calling `effects.*`.
2. **New pieces** in `com/osudroid/ui/v2/game/`, each with a `ChoreographyTransitions` holding the choreography
   `GameplayEffects` has today:
   - **`JudgementPiece`**: a `Sprite` or `AnimatedSprite` chosen by the skin and the texture (what `GameEffect.init`
     does now). It holds the legacy judgement: the miss's drop and rotation, the hit's pop, the animated variant
     with fades only, and the spinner's miss variant.
   - **`HitLightingPiece`**: custom blend mode and combo color, proxied into `underlay`.
   - **Particles** stay one `ParticleSystem` per texture, a resource of the playfield
     (`GameplayPlayfield.particles`) that objects emit into. `emitParticles` moves there unchanged.
3. **Bursts reuse the object's own pieces**, as osu!lazer's `LegacyMainCirclePiece` does. On a hit, the circle piece
   (or the slider's head or tail) hides its number, then scales by 1.4 and fades out over 0.24 s. Today a separate
   `hitcircle`/`hitcircleoverlay` copy does it.
   - Reverse arrow bursts are separate pieces, since the arrow stays for the next repeats. Each slider keeps a few.
   - The `areBurstsShown()` rules (the setting, Hidden, Traceable) move into the objects.
4. **Each object:**
   - Circles and spinners own a `JudgementPiece`, plus lighting.
   - Sliders get the head's `sliderpoint30`, one judgement per repeat and tick, and the end's judgement, from a
     `JudgementPiece` pool in `GameObjectPool`. They own them until `onExpire`, proxied into `judgements`.
   - Every transition played calls `extendLifetime`, so an object expires once its effects have ended.
   - `GameScene`'s estimate of the most active objects adds the judgement duration (about 1.22 s) to each lifetime.
5. **Deleted:** `GameEffect.kt`, `ru/.../game/GameplayEffects.kt`, and `GameObjectPool.effects`, `getEffect` and
   `putEffect`.


### As done

Game commit after `0149249ab`, no engine change.

- `GameObjectListener.onCircleHit`, `onSliderHit` and `onSpinnerHit` return the judgement's texture (null for none,
  e.g. a missed tick or a bonus spin), and `onSliderReverse` is gone: the slider bursts its reverse arrow itself.
  `HitResult` (`com/osudroid/game/`) is read from the texture (`HitResult.fromTexture`).
- `GameObject.showJudgement` shows a judgement: its particles (`GameplayPlayfield.particles`, a
  `GameplayParticles` in `judgements`, cleared on a seek), its lighting (one `HitLightingPiece` per object, proxied
  into `underlay`, a plain choreography: it has a single animation) and the `JudgementPiece` from the pool, proxied
  into `judgements` when it's played, so the order is the order of the judgements, as before.
- `JudgementPiece` is an `AnimatedSprite` (one frame for the textures that aren't animated), pooled by texture
  (`GameObjectPool.getJudgement`/`putJudgement`). Its `ChoreographyTransitions` is a member, not shared: its blocks
  read the scale of the object, the miss's random rotation and whether it's animated or a spinner's miss, which a
  stateless instance couldn't be given.
- Bursts (`GameObject.burst`) are proxied into `judgements`, below the judgement as before. Circles burst their own
  circle piece, without its number. Sliders have their own burst pieces for the head and the end (both stay shown
  for the repeats) and a pool of reverse arrows.
- The lifetime of an object never ends before the effects it played (`extendLifetime(endTime)`), whatever it sets
  `lifetimeEnd` to afterwards. `releaseEffects()` in `onExpire` gives the pieces back. Spinners stay in their layer
  until they expire, so their effects keep the gameplay clock, and skip `updateJudgement` once judged. Their
  judgement is placed at the centre of the playfield, in osu!pixels, as the other objects place theirs.
- As judged objects stay active while their effects are shown, `GameObject.hasFinalJudgement()` tells them apart:
  the end of the beatmap (lead out, flashlight) waits for it instead of an empty list, and a failed replay doesn't
  count judged objects as misses anymore (it did for the ones still fading out).
- `GameScene`'s estimate of the active objects adds `JudgementPiece.DURATION` to each lifetime.

---

## Verification

- **Engine:** `./gradlew :verktex:shared:desktopTest :verktex:shared:testAndroidHostTest :verktex:shadertools:test`.
  The Android host tests need a JDK that Robolectric's ASM can read (class version 70 can't be read).
- **Engine UI:** `./gradlew :verktex:desktopApp:run --args="--test 'Verktex/Controls' --screenshot build/c.png"`, and
  likewise for Buttons, Forms, Cards, Dialogs and "Hover and transitions". Pressing, hovering, disabling and
  expanding have to be tried by hand.
- **Game:** `./gradlew :shared:desktopTest` (`checkCommonSources` included) and `./gradlew lint`. Game scenes:
  `./gradlew :desktopApp:runUITests --args="--test 'Scenes/Main menu' --screenshot build/menu.png"`.
- **Gameplay (Phases 3 and 4):**
  - `--test 'osu!/Hit circles'` and the slider and spinner tests of `OsuGameplayUITests`, with screenshots before and
    after: bursts, 300/100/50/miss, lighting on and off, animated judgement skins, slider points and reverse arrow
    bursts.
  - `./gradlew :desktopApp:run --args="--quickplay '<beatmap>'"` with autoplay:
    - layering: approach circles and judgements above every object, lighting below;
    - Hidden and Traceable: no bursts;
    - seeking back and forth: no leftover effects and no doubled judgements;
    - `GameObjectPool.objectsCreated` stays bounded.
- **Android:** `./gradlew assembleDebug`, then one gameplay test in `OsuUITestActivity` on each GPU backend.
