package com.osudroid.ui.v2.test

import ru.nsu.ccfit.zuev.osu.game.GameScene
import com.reco1l.verktex.data.Background
import com.reco1l.verktex.data.CornerRadius
import com.osudroid.ui.v2.SkinTextures
import com.reco1l.verktex.settings.Settings
import com.osudroid.beatmaps.hitobjects.HitCircle
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
import com.reco1l.verktex.ui.UIButton
import com.reco1l.verktex.ui.textButton
import com.osudroid.ui.v2.test.GameplayObjectHarness.Play
import ru.nsu.ccfit.zuev.osu.game.GameHelper
import ru.nsu.ccfit.zuev.osu.game.GameplayHitCircle
import ru.nsu.ccfit.zuev.osu.game.GameplayModernSpinner
import ru.nsu.ccfit.zuev.osu.game.GameplaySlider
import ru.nsu.ccfit.zuev.osu.game.GameplaySpinner
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
                    GameHelper.convertSliderPath(slider, null),
                    GameHelper.createRenderPath(slider, null)
                )
                slider to gameplaySlider
            }
        }

        test("Spinners") {
            val harness = GameplayObjectHarness(this)
            var isModern = SkinTextures.isNewStyleSpinner()

            fun play() = harness.loop { startTime ->
                val spinner = harness.prepare(Spinner(startTime, startTime + 3000, true))
                val gameplaySpinner = if (isModern) GameplayModernSpinner() else GameplaySpinner()
                val rps = 2 + 2 * harness.difficulty.od / 10f

                gameplaySpinner.init(harness, harness.playfield, spinner, rps, harness.stat)
                spinner to gameplaySpinner
            }

            harness.playButton(Play.Autoplay, Play.Spin, Play.Miss)

            textButton {
                anchor = Anchor.TopRight
                origin = Anchor.TopRight
                x = (-16f).dip
                y = 72f.dip
                variant = UIButton.Variant.Surface
                text = styleName(isModern)
                onActionUp = {
                    isModern = !isModern
                    text = styleName(isModern)
                    play()
                }
            }

            play()
        }

        test("HUD elements") {
            useGameContext()

            val elements = mutableListOf<HUDElement>()
            val gameScene = GameScene()

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

    private fun styleName(isModern: Boolean) = if (isModern) "Modern style" else "Classic style"


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

    private val SAMPLE_LEADERBOARD = listOf(
        ScoreBoardItem("Rei", 9_870_000, 900, 0.995f, true),
        ScoreBoardItem("Asuka", 9_210_000, 850, 0.981f, true),
        ScoreBoardItem("Shinji", 7_540_000, 610, 0.947f, true),
    )
}
