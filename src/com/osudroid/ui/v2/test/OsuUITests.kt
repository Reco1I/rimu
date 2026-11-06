package com.osudroid.ui.v2.test

import com.osudroid.mods.ModDoubleTime
import com.osudroid.mods.ModEasy
import com.osudroid.mods.ModFlashlight
import com.osudroid.mods.ModHardRock
import com.osudroid.mods.ModHidden
import com.osudroid.mods.ModNoFail
import com.osudroid.mods.ModRelax
import com.osudroid.ui.v2.SkinTextures
import com.osudroid.ui.v2.SpriteFont
import com.osudroid.ui.v2.StarRatingBadge
import com.osudroid.OsuPlatform
import com.reco1l.verktex.ui.UIBadge
import com.osudroid.ui.v2.game.CursorSprite
import com.osudroid.ui.v2.game.NumberedCirclePiece
import com.osudroid.ui.v2.mainmenu.OsuLogo
import com.osudroid.ui.v2.mainmenu.TrianglesDispenser
import com.osudroid.ui.v2.modmenu.ModIcon
import com.osudroid.ui.v2.ModsIndicator
import com.osudroid.ui.v2.songselect.BeatmapCarrousel
import com.osudroid.ui.v2.songselect.BeatmapSetModel
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.data.fill
import com.reco1l.verktex.testing.UITestSuite
import com.reco1l.verktex.testing.column
import com.reco1l.verktex.testing.section
import com.reco1l.verktex.ui.UISprite
import com.reco1l.verktex.ui.container.container
import com.reco1l.verktex.ui.container.linearContainer
import com.reco1l.verktex.ui.control.slider
import com.reco1l.verktex.ui.text.text
import com.reco1l.verktex.ui.textButton
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.skins.OsuSkin
import java.io.File

/**
 * Tests for the osu!droid components.
 */
object OsuUITests : UITestSuite("osu!") {

    private val VIDEO_EXTENSIONS = setOf("mp4", "avi", "flv", "m4v", "mkv", "webm", "mov")

    private val COMBO_COLORS = listOf(
        Color4(1f, 0.75f, 0f),
        Color4(0f, 0.8f, 0f),
        Color4(0.07f, 0.49f, 1f),
        Color4(0.95f, 0.1f, 0.22f),
    )


    init {
        test("Hit circles") {
            val pieces = mutableListOf<NumberedCirclePiece>()
            val approachCircles = mutableListOf<UISprite>()

            linearContainer {
                anchor = Anchor.Center
                origin = Anchor.Center
                spacing = 24f.dip

                COMBO_COLORS.forEachIndexed { index, color ->
                    container {
                        width = 140f.dip
                        height = 140f.dip

                        pieces += NumberedCirclePiece("hitcircle", "hitcircleoverlay").also {
                            it.anchor = Anchor.Center
                            it.setCircleColor(color)
                            it.setNumberText(index + 1)
                            +it
                        }

                        approachCircles += UISprite(SkinTextures["approachcircle"]).also {
                            it.anchor = Anchor.Center
                            it.origin = Anchor.Center
                            it.color = color
                            +it
                        }
                    }
                }
            }

            // Loops an approach, hit and fade on every circle, one after another.
            var time = 0f
            onEachFrame { clock ->
                time += clock.elapsedFrameTime

                pieces.forEachIndexed { index, piece ->
                    val t = ((time - index * 0.25f) % 2f + 2f) % 2f
                    val approach = approachCircles[index]

                    when {
                        t < 1f -> {
                            approach.scale = 3f - 2f * t
                            approach.alpha = t.coerceAtMost(0.5f) * 2f
                            piece.alpha = (t * 3f).coerceAtMost(1f)
                            piece.scale = 1f
                        }
                        t < 1.25f -> {
                            val p = (t - 1f) / 0.25f
                            approach.alpha = 0f
                            piece.alpha = 1f - p
                            piece.scale = 1f + 0.4f * p
                        }
                        else -> {
                            approach.alpha = 0f
                            piece.alpha = 0f
                        }
                    }
                }
            }
        }

        test("Skin sprites") {
            column {
                for (group in listOf(
                    listOf("hitcircle", "hitcircleoverlay", "approachcircle", "sliderb0", "sliderfollowcircle", "reversearrow"),
                    listOf("hit300", "hit100", "hit50", "hit0"),
                    listOf("cursor", "cursortrail", "spinner-circle", "spinner-approachcircle"),
                    listOf("play-skip", "ranking-S", "ranking-A", "selection-mods"),
                )) {
                    section(group.joinToString(", ")) {
                        for (name in group) {
                            val texture = SkinTextures[name]

                            if (texture == null) {
                                text { text = "$name missing"; color = Color4.Red400 }
                            } else {
                                +UISprite(texture).apply {
                                    width = 96f.dip
                                    height = 96f.dip
                                }
                            }
                        }
                    }
                }
            }
        }

        test("Sprite fonts") {
            column {
                section("Score") {
                    +SpriteFont(OsuSkin.get().scorePrefix).apply { text = "0123456789" }
                }
                section("Combo") {
                    +SpriteFont(OsuSkin.get().comboPrefix).apply { text = "727" }
                }
                section("Hit circle numbers") {
                    +SpriteFont(OsuSkin.get().hitCirclePrefix).apply { text = "0123456789" }
                }
            }
        }

        test("Star rating badge") {
            column {
                val badge = StarRatingBadge()

                section("Badge") { +badge }

                section("Rating") {
                    slider {
                        width = 400f.dip
                        min = 0f
                        max = 12f
                        onValueChange = { badge.rating = it.toDouble() }
                    }
                }

                for (density in UIBadge.Density.entries) {
                    section("Presets ($density)") {
                        for (rating in listOf(1.5, 3.2, 5.4, 6.8, 8.2, 10.0)) {
                            +StarRatingBadge().apply {
                                this.density = density
                                this.rating = rating
                                applyColorsInstantly()
                            }
                        }
                    }
                }
            }
        }

        test("Mods") {
            val mods = listOf(ModEasy(), ModNoFail(), ModHidden(), ModHardRock(), ModDoubleTime(), ModFlashlight(), ModRelax())

            column {
                section("Icons") {
                    for (mod in mods) {
                        +ModIcon(mod).apply {
                            width = 48f.dip
                            height = 48f.dip
                        }
                    }
                }

                section("Indicator") {
                    +ModsIndicator().apply {
                        iconSize = 32f.dip
                        this.mods = mods
                    }
                }
            }
        }

        test("Logo") {
            +OsuLogo(withExternalEffects = false).apply {
                anchor = Anchor.Center
                origin = Anchor.Center
                // It fills its size, which must be square and fit the stage.
                width = 240f.dip
                height = 240f.dip
                onActionUp = {
                    playClickEffect()
                    log("Logo clicked")
                }
            }
        }

        test("Video") {
            // The first beatmap video found, there's none bundled.
            val file = File(Config.getBeatmapPath()).listFiles().orEmpty()
                .filter { it.isDirectory }
                .flatMap { it.listFiles().orEmpty().asList() }
                .filter { it.extension.lowercase() in VIDEO_EXTENSIONS }
                // MP4 first, it's what the decoders support best, then by name so it's always the same.
                .sortedWith(compareBy({ it.extension.lowercase() != "mp4" }, { it.path }))
                .firstOrNull()

            if (file == null) {
                text {
                    anchor = Anchor.Center
                    origin = Anchor.Center
                    text = "No beatmap video found in ${Config.getBeatmapPath()}"
                }
                return@test
            }

            log("Loading ${file.parentFile?.name}/${file.name}")

            val video = (OsuPlatform.implementation.createVideo(file.path) ?: return@test).apply {
                anchor = Anchor.Center
                origin = Anchor.Center
            }
            +video
            video.play()

            video.setOnReady {
                // Its size is the video's until then, it's fit in the stage keeping the aspect ratio.
                val width = 480f
                video.width = width.dip
                video.height = (width * video.videoHeight / video.videoWidth).dip
                log("Playing ${file.name}, ${video.videoWidth}x${video.videoHeight}")
            }

            textButton {
                anchor = Anchor.TopCenter
                origin = Anchor.TopCenter
                y = 16f.dip
                text = "Restart"
                onActionUp = { video.seekTo(0) }
            }

            onDispose { video.release() }
        }

        test("Triangles") {
            +TrianglesDispenser().apply {
                width = fill()
                height = fill()
            }
        }

        test("Beatmap carrousel") {
            container {
                width = fill(0.5f)
                height = fill()
                anchor = Anchor.TopRight
                origin = Anchor.TopRight

                +BeatmapCarrousel().apply {
                    data = TestData.beatmapSets().map { BeatmapSetModel(it) }
                    onBeatmapSelected = { log("Selected ${it?.version}") }
                    onBeatmapConfirmed = { log("Confirmed ${it.version}") }
                    onBeatmapSetLongPress = { log("Long pressed ${it.boundData?.beatmapSetInfo?.directory}") }
                }
            }
        }

        test("Cursor") {
            val cursor = CursorSprite().apply {
                anchor = Anchor.Center
                origin = Anchor.Center
            }
            +cursor

            textButton {
                anchor = Anchor.BottomCenter
                origin = Anchor.BottomCenter
                y = (-16f).dip
                text = "Click"
                onActionUp = { cursor.handleClick() }
            }

            onEachFrame { cursor.update(it.elapsedFrameTime) }
        }
    }
}
