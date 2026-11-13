package com.osudroid.ui.v2.test

import kotlin.math.sin
import kotlin.math.cos
import com.reco1l.verktex.ui.UIVideo
import com.reco1l.verktex.Platform
import com.osudroid.OsuGameHost
import com.osudroid.settings.GameFolders
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
import com.reco1l.verktex.ui.UIBadge
import com.osudroid.ui.v2.game.CursorEntity
import com.osudroid.ui.v2.game.NumberedCirclePiece
import com.osudroid.ui.v2.mainmenu.OsuLogo
import com.osudroid.ui.v2.mainmenu.TrianglesDispenser
import com.osudroid.ui.v2.modmenu.ModIcon
import com.osudroid.ui.v2.ModsIndicator
import com.osudroid.ui.v2.songselect.BeatmapCarrousel
import com.osudroid.ui.v2.songselect.BeatmapSetModel
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Background
import com.reco1l.verktex.data.Border
import com.reco1l.verktex.data.CornerRadius
import com.reco1l.verktex.data.ScaleType
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
import ru.nsu.ccfit.zuev.skins.OsuSkin
import okio.Path.Companion.toPath

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

        test("Texture backgrounds") {
            val texture = SkinTextures["menu-background"]

            column {
                if (texture == null) {
                    text { text = "menu-background missing"; color = Color4.Red400 }
                    return@column
                }

                // Wider and taller than the texture, so cropping and letterboxing show on both axes.
                for ((title, width, height) in listOf(Triple("Wide", 220f, 90f), Triple("Tall", 90f, 160f))) {
                    section(title) {
                        for (scaleType in ScaleType.entries) {
                            container {
                                this.width = width.dip
                                this.height = height.dip
                                background = Background.Texture(texture, scaleType)
                                radius = CornerRadius(24f.dip, 4f.dip, 24f.dip, 4f.dip)
                                border = Border(3f.dip, Color4.Red400, Color4.Green400, Color4.Blue400, Color4.Yellow400)

                                text {
                                    text = scaleType.name
                                    anchor = Anchor.Center
                                    origin = Anchor.Center
                                }
                            }
                        }
                    }
                }

                section("Tinted, aligned to the left") {
                    container {
                        width = 220f.dip
                        height = 90f.dip
                        background = Background.Texture(texture, ScaleType.Fit, Anchor.CenterLeft, Color4.Blue400)
                        radius = CornerRadius(16f.dip)
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
            val fileSystem = Platform.fileSystem
            val file = fileSystem.listOrNull(GameFolders.beatmapPath.toPath()).orEmpty()
                .filter { fileSystem.metadataOrNull(it)?.isDirectory == true }
                .flatMap { fileSystem.listOrNull(it).orEmpty() }
                .filter { it.name.substringAfterLast('.', "").lowercase() in VIDEO_EXTENSIONS }
                // MP4 first, it's what the decoders support best, then by name so it's always the same.
                .sortedWith(compareBy({ it.name.substringAfterLast('.', "").lowercase() != "mp4" }, { it.toString() }))
                .firstOrNull()

            if (file == null) {
                text {
                    anchor = Anchor.Center
                    origin = Anchor.Center
                    text = "No beatmap video found in ${GameFolders.beatmapPath}"
                }
                return@test
            }

            log("Loading ${file.parent?.name}/${file.name}")

            if (!Platform.canPlayVideos) {
                text {
                    anchor = Anchor.Center
                    origin = Anchor.Center
                    text = "This platform can't play videos"
                }
                return@test
            }

            val video = UIVideo(file.toString()).apply {
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
            // Moving in a circle, so the trail shows.
            val cursor = CursorEntity()
            cursor.attachToScene(this)
            cursor.setShowing(true)

            var angle = 0f
            var isPressed = false

            textButton {
                anchor = Anchor.BottomCenter
                origin = Anchor.BottomCenter
                y = (-16f).dip
                text = "Press"
                onActionUp = {
                    isPressed = !isPressed
                    cursor.setPressed(isPressed)
                    text = if (isPressed) "Release" else "Press"
                }
            }

            onEachFrame {
                angle += it.elapsedFrameTime * 3f
                cursor.setPosition(bounds.width / 2f + cos(angle) * 150f, bounds.height / 2f + sin(angle) * 150f)
                cursor.update(it.elapsedFrameTime)
            }
        }
    }
}
