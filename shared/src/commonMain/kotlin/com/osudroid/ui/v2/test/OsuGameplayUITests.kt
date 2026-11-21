package com.osudroid.ui.v2.test

import com.reco1l.verktex.data.Background
import com.reco1l.verktex.data.CornerRadius
import com.osudroid.ui.v2.SkinTextures
import com.osudroid.game.convertSliderPath
import com.osudroid.game.createSliderRenderPath
import com.reco1l.verktex.settings.Settings
import com.osudroid.beatmaps.hitobjects.HitCircle
import com.osudroid.beatmaps.hitobjects.HitObject
import com.osudroid.ui.v2.game.FollowPointConnection
import com.osudroid.beatmaps.hitobjects.Slider
import com.osudroid.beatmaps.hitobjects.SliderPath
import com.osudroid.beatmaps.hitobjects.SliderPathType
import com.osudroid.beatmaps.hitobjects.Spinner
import com.osudroid.math.Vector2
import com.osudroid.ui.v2.hud.HUDElement
import com.osudroid.ui.v2.hud.HUDElementSkinData
import com.osudroid.ui.v2.hud.HUDElements
import com.osudroid.ui.v2.hud.elements.HUDLeaderboard
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.ClipMode
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.data.fill
import com.reco1l.verktex.testing.UITestSuite
import com.reco1l.verktex.testing.column
import com.reco1l.verktex.testing.section
import com.reco1l.verktex.ui.container.container
import com.reco1l.verktex.ui.text.text
import com.reco1l.verktex.ui.Button
import com.reco1l.verktex.ui.textButton
import com.osudroid.ui.v2.test.GameplayObjectHarness.Play
import com.osudroid.game.objects.GameplayHitCircle
import com.osudroid.game.objects.GameplaySlider
import com.osudroid.game.objects.GameplaySpinner
import com.osudroid.ui.v2.skin.DefaultSpinnerPiece
import com.osudroid.ui.v2.skin.SkinComponent
import com.osudroid.ui.v2.skin.SkinPieces
import com.osudroid.ui.v2.skin.SpinnerPiece
import com.osudroid.ui.v2.skin.legacy.LegacyClassicSpinnerPiece
import com.osudroid.ui.v2.skin.legacy.LegacyModernSpinnerPiece
import ru.nsu.ccfit.zuev.osu.menu.ScoreBoardItem
import ru.nsu.ccfit.zuev.skins.BeatmapSkinManager
import ru.nsu.ccfit.zuev.skins.OsuSkin

/**
 * Tests for the gameplay objects and the HUD, played by autoplay and a made up play instead of a
 * beatmap.
 */
object OsuGameplayUITests : UITestSuite("Gameplay") {

    init {
        test("Gameplay circles") {
            val harness = GameplayObjectHarness(this)
            harness.playButton(Play.Autoplay, Play.Miss)
            var index = 0

            harness.loop { startTime ->
                val position = CIRCLE_POSITIONS[index % CIRCLE_POSITIONS.size]
                val circle = harness.prepare(HitCircle(startTime, position, true, 0))
                val gameplayCircle = GameplayHitCircle()

                gameplayCircle.init(harness, harness.playfield, circle, harness.comboColor(index++))
                circle to gameplayCircle
            }
        }

        test("Follow points") {
            val harness = GameplayObjectHarness(this)
            harness.playButton(Play.Autoplay, Play.Miss)

            // The connections still to add, each when the time to add it comes.
            val pending = mutableListOf<Pair<HitObject, HitObject>>()

            harness.onRestart = {
                pending.clear()
                FollowPointConnection.clearAll(harness.playfield.underlay)
            }
            onDispose { FollowPointConnection.clearAll(harness.playfield.underlay) }

            harness.loopAll { startTime ->
                var previous: HitObject? = null
                val circles = FOLLOW_POINT_POSITIONS.mapIndexed { i, position ->
                    // At an even interval, so the further apart, the faster the follow points between them move.
                    val circle = HitCircle(startTime + i * FOLLOW_POINT_INTERVAL, position, i == 0, 0)
                    circle.updateComboInformation(previous)
                    previous = circle
                    harness.prepare(circle)
                }
                circles.last().isLastInCombo = true

                pending += circles.zipWithNext()
                circles.map { circle ->
                    val gameplayCircle = GameplayHitCircle()
                    gameplayCircle.init(harness, harness.playfield, circle, harness.comboColor(0))
                    circle to gameplayCircle
                }
            }

            onEachFrame {
                val iterator = pending.iterator()
                while (iterator.hasNext()) {
                    val (start, end) = iterator.next()
                    // As the game adds them: once they start to fade in, which can be before the circle they start from.
                    if (harness.elapsedTime < FollowPointConnection.fadeInStartTime(start)) continue

                    log("Follow points from ${start.indexInCurrentCombo + 1} to ${end.indexInCurrentCombo + 1}")
                    FollowPointConnection.addConnection(harness.playfield.underlay, start, end)
                    iterator.remove()
                }
            }
        }

        test("Sliders") {
            val harness = GameplayObjectHarness(this)
            harness.playButton(Play.Autoplay, Play.Miss)
            var index = 0

            harness.loop { startTime ->
                val shape = SLIDER_SHAPES[index % SLIDER_SHAPES.size]
                log("Shape: ${shape.name}")

                val slider = harness.prepare(Slider(
                    startTime,
                    shape.position,
                    shape.repeats,
                    SliderPath(shape.type, shape.controlPoints, shape.length),
                    true,
                    0,
                    MutableList(shape.repeats + 2) { mutableListOf() }
                ))

                val gameplaySlider = GameplaySlider()
                gameplaySlider.init(
                    harness,
                    harness.playfield,
                    harness.stat,
                    slider,
                    harness.controlPoints,
                    harness.comboColor(index++),
                    sliderBorderColor(),
                    convertSliderPath(slider, null),
                    createSliderRenderPath(slider, null)
                )
                slider to gameplaySlider
            }
        }

        test("Spinners") {
            val harness = GameplayObjectHarness(this)
            // The skin's spinner, then each style of spinner in turn.
            var style = 0

            fun play() = harness.loop { startTime ->
                val spinner = harness.prepare(Spinner(startTime, startTime + 3000, true))
                val gameplaySpinner = GameplaySpinner(spinnerStyles[style].second())
                val rps = 2 + 2 * harness.difficulty.od / 10f

                gameplaySpinner.init(harness, harness.spinnerLayer, harness.playfield, spinner, rps, harness.stat)
                spinner to gameplaySpinner
            }

            harness.playButton(Play.Autoplay, Play.Spin, Play.Miss)

            textButton {
                anchor = Anchor.TopRight
                origin = Anchor.TopRight
                x = (-16f).dip
                y = 72f.dip
                variant = Button.Variant.Surface
                text = spinnerStyles[style].first
                onActionUp = {
                    style = (style + 1) % spinnerStyles.size
                    text = spinnerStyles[style].first
                    play()
                }
            }

            play()
        }

        test("HUD elements") {
            useGameContext()

            val elements = mutableListOf<HUDElement>()
            val gameScene = FakeGameplayState()

            column {
                for (entry in HUDElements.entries) {
                    section(entry.name.replace('_', ' ')) {
                        container {
                            width = 280f.dip
                            height = 110f.dip
                            padding = Padding(8f.dip)
                            radius = CornerRadius(8f.dip)
                            background = Background.Color(Color4(0.16f, 0.16f, 0.22f))
                            // Some elements are bigger than the cell, e.g. the leaderboard.
                            clipping = ClipMode.Bounds

                            val element = try {
                                entry.create(gameScene)
                            } catch (e: Exception) {
                                text {
                                    width = fill()
                                    color = Color4.Red400
                                    text = "Failed to create: ${e.cause ?: e}"
                                }
                                return@container
                            }

                            element.setSkinData(HUDElementSkinData(type = entry.name))
                            +element
                            elements += element

                            if (element is HUDLeaderboard) {
                                element.nextItems = SAMPLE_LEADERBOARD
                            }
                        }
                    }
                }
            }

            FakeGameplayDriver(this, gameScene, elements)
        }
    }


    // As the game scene picks it for a beatmap without colors of its own.
    private fun sliderBorderColor() =
        if (OsuSkin.get().isForceOverrideSliderBorderColor) OsuSkin.get().sliderBorderColor!!
        else BeatmapSkinManager.getInstance().sliderColor

    // The spinners the Spinners test shows, by name.
    private val spinnerStyles = listOf<Pair<String, () -> SpinnerPiece>>(
        "Skin's style" to { SkinPieces.current.create(SkinComponent.Spinner) },
        "Default skin" to { DefaultSpinnerPiece() },
        "Classic style" to { LegacyClassicSpinnerPiece() },
        "Modern style" to { LegacyModernSpinnerPiece() },
    )


    private class SliderShape(
        val name: String,
        val type: SliderPathType,
        val position: Vector2,
        val controlPoints: List<Vector2>,
        val length: Double,
        val repeats: Int = 0
    )

    private val SLIDER_SHAPES = listOf(
        SliderShape("Linear", SliderPathType.Linear, Vector2(96f, 192f), listOf(Vector2(0f), Vector2(320f, 0f)), 320.0),
        SliderShape("Perfect curve", SliderPathType.PerfectCurve, Vector2(128f, 256f),
            listOf(Vector2(0f), Vector2(128f, -160f), Vector2(256f, 0f)), 380.0),
        SliderShape("Bezier with a repeat", SliderPathType.Bezier, Vector2(96f, 96f),
            listOf(Vector2(0f), Vector2(160f, 240f), Vector2(320f, -40f), Vector2(320f, 200f)), 420.0, repeats = 1),
        SliderShape("Catmull", SliderPathType.Catmull, Vector2(64f, 300f),
            listOf(Vector2(0f), Vector2(120f, -180f), Vector2(240f, 0f), Vector2(380f, -200f)), 480.0),
    )

    private val CIRCLE_POSITIONS = listOf(
        Vector2(128f, 96f),
        Vector2(384f, 96f),
        Vector2(384f, 288f),
        Vector2(128f, 288f),
        Vector2(256f, 192f),
    )

    // A combo zigzagging across the playfield, further apart each time.
    private val FOLLOW_POINT_POSITIONS = listOf(
        Vector2(64f, 288f),
        Vector2(160f, 160f),
        Vector2(288f, 300f),
        Vector2(448f, 64f),
    )

    private const val FOLLOW_POINT_INTERVAL = 500.0

    private val SAMPLE_LEADERBOARD = listOf(
        ScoreBoardItem("Rei", 9_870_000, 900, 0.995f, true),
        ScoreBoardItem("Asuka", 9_210_000, 850, 0.981f, true),
        ScoreBoardItem("Shinji", 7_540_000, 610, 0.947f, true),
    )
}
