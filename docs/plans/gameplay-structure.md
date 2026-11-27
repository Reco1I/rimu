# Gameplay structure

Gameplay was one class, `GameScene` (`ru/nsu/ccfit/zuev/osu/game/`, about 4,000 lines), with its state shared
through the `GameHelper` singleton. It's now split by what each part does. Older plans in this folder name the
classes as they were; this is where their code went.

| Before | Now |
|---|---|
| `GameScene.startGame`, `restartGame`, `cancelLoading`, `quit`, `scoring` | `Gameplay` (`com/osudroid/game/`): `start(PlayRequest)`, `retry`, `watchReplay`, `cancelLoading`, `quit`, `scoring` |
| `GameScene.loadGame` (the beatmap) | `PlayLoader` → `LoadedPlay` (`game/loading/`), reused while the same beatmap and mods are retried |
| `GameScene.loadGame` (the attempt), `prepareScene`, `start`, `update`, the judgement callbacks, pause, fail, seek | `GameplaySession` (`game/session/`), one per attempt |
| The spawning and follow points of `update` | `ObjectSpawner` |
| The replay playback of `update` | `ReplayInputSource` |
| Cursor sprites, autoplay's cursor, Flashlight | `CursorPresenter` |
| Timing and effect points | `ControlPointTracker` |
| Breaks | `BreakController` |
| Video | `VideoController` |
| Live score, skip request, leaving a match | `MultiplayerSync` |
| `reconstructStatAtTime`, `apply*Result` (seeking a replay) | `ScoreReconstruction` |
| `registerHit`, the hit windows, HP drain and Easy's lives | `ScoreProcessor`, `HitJudging`, `HealthProcessor` (`game/rules/`) |
| `GameHelper` (mods, OD/HP, rate, beat, kiai, replay version) | `GameplayContext` and its `GameplayTiming` (`game/`), one per play, given to the objects by their listener |
| `GameHelper.createRenderPath`, `convertSliderPath`, `SliderPath` | `createSliderRenderPath`, `convertSliderPath`, `SliderPath` (`game/SliderPaths.kt`) |
| `GameObject`, `GameplayHitCircle`, `GameplaySlider`, `GameplaySpinner`, `GameObjectPool`, `GameObjectListener` (`ru/nsu/ccfit/zuev/osu/game/`) | The same classes in `com/osudroid/game/objects/`, `GameObjectPool` being `PiecePool`, which pools the pieces rather than the objects; the listener is `JudgementListener`, `GameplayCursors` and `GameplaySamples`, with typed judgements (`JudgementResult`, `SliderPart`) |
| `StatisticV2` (`ru/nsu/ccfit/zuev/osu/scoring/`) | `Statistic` (`com/osudroid/scoring/`) |
| The HUD reading `GameScene` | `GameplayState` (`ui/v2/hud/`), which the session implements |

What's left:

- `GameplaySession` is still the largest class, about 2,500 lines: its scene (`GameUIScene`, an inner class), the
  judgement callbacks, pause and fail, and the seek. The scene reaches into most of the parts, so it stays inside.
- The gameplay objects (`GameplaySlider` is about 1,500 lines) were moved, not restructured.
