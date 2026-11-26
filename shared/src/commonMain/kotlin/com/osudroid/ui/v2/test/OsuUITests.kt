package com.osudroid.ui.v2.test

import com.reco1l.verktex.ui.iconButton
import com.reco1l.verktex.ui.container.LinearContainer
import com.reco1l.verktex.ui.FAIcon
import com.osudroid.utils.currentTimeMillis
import com.osudroid.notifications.Notification
import com.osudroid.notifications.Notifications
import com.osudroid.ui.v2.notifications.NotificationCenter
import kotlin.math.sin
import kotlin.math.cos
import com.reco1l.verktex.ui.Video
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
import com.reco1l.verktex.ui.Badge
import com.osudroid.ui.v2.game.CursorEntity
import com.osudroid.game.HitResult
import com.osudroid.ui.v2.skin.DefaultSkin
import com.osudroid.ui.v2.skin.FollowCirclePiece
import com.osudroid.ui.v2.skin.SliderBallPiece
import com.osudroid.ui.v2.skin.GameplaySkin
import com.osudroid.ui.v2.skin.HitLightingPiece
import com.reco1l.verktex.ui.control.Checkbox
import com.osudroid.ui.v2.skin.JudgementPiece
import com.osudroid.ui.v2.skin.MainCirclePiece
import com.osudroid.ui.v2.skin.legacy.LegacySkin
import com.reco1l.verktex.data.px
import com.osudroid.ui.v2.skin.SkinComponent
import com.osudroid.ui.v2.skin.SkinPieces
import com.reco1l.verktex.ui.Component
import com.osudroid.ui.v2.mainmenu.RimuLogo
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
import com.osudroid.beatmaplisting.BeatmapDownloader
import com.osudroid.beatmaplisting.BeatmapSetModel as ListingSet
import com.osudroid.ui.v2.beatmaplisting.BeatmapListingState
import com.osudroid.ui.v2.beatmaplisting.BeatmapSetCard
import com.osudroid.ui.v2.beatmaplisting.BeatmapSetDetails
import com.osudroid.ui.v2.beatmaplisting.archiveName
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.ui.container.RecyclerContainer
import com.reco1l.verktex.data.fill
import com.reco1l.verktex.testing.UITestSuite
import com.reco1l.verktex.testing.column
import com.reco1l.verktex.testing.section
import com.reco1l.verktex.ui.Sprite
import com.reco1l.verktex.ui.container.container
import com.reco1l.verktex.ui.container.linearContainer
import com.reco1l.verktex.ui.control.seekBar
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
            val pieces = mutableListOf<MainCirclePiece>()
            val approachCircles = mutableListOf<Component>()

            linearContainer {
                anchor = Anchor.Center
                origin = Anchor.Center
                spacing = 24f.dip

                COMBO_COLORS.forEachIndexed { index, color ->
                    container {
                        width = 140f.dip
                        height = 140f.dip

                        pieces += SkinPieces.current.create(SkinComponent.HitCircle).also {
                            it.anchor = Anchor.Center
                            it.setCircleColor(color)
                            it.setComboNumber(index + 1)
                            +it
                        }

                        approachCircles += SkinPieces.current.create(SkinComponent.ApproachCircle).also {
                            it.anchor = Anchor.Center
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

        test("Default skin") {
            // The pieces of the default skin under those of the default textures, which they stand for: they have
            // the same sizes. Loops an approach, a hit and every judgement, with the hit lighting and the circle's
            // burst unless they're turned off, as the settings do in gameplay.
            class Row(
                val circle: MainCirclePiece,
                val approach: Component,
                val judgements: List<JudgementPiece>,
                val lightings: List<HitLightingPiece?>,
            )

            val results = listOf("hit300" to HitResult.Great, "hit100" to HitResult.Ok, "hit50" to HitResult.Meh, "hit0" to HitResult.Miss)
            val rows = mutableListOf<Row>()
            var isHitLightingShown = true
            var areBurstsShown = true

            column {
                section("Effects") {
                    +Checkbox(isHitLightingShown).apply { onValueChange = { isHitLightingShown = it } }
                    text { text = "Hit lighting"; anchor = Anchor.CenterLeft; origin = Anchor.CenterLeft }
                    +Checkbox(areBurstsShown).apply { onValueChange = { areBurstsShown = it } }
                    text { text = "Burst effects"; anchor = Anchor.CenterLeft; origin = Anchor.CenterLeft }
                }

                for ((name, skin) in listOf<Pair<String, GameplaySkin>>("Skin's textures" to LegacySkin, "Default skin" to DefaultSkin)) {
                    section(name) {
                        linearContainer {
                            spacing = 24f.dip

                            val circle = skin.create(SkinComponent.HitCircle)!!
                            val approach = skin.create(SkinComponent.ApproachCircle)!!
                            val judgements = mutableListOf<JudgementPiece>()
                            val lightings = mutableListOf<HitLightingPiece?>()

                            container {
                                width = 160f.px
                                height = 160f.px

                                circle.anchor = Anchor.Center
                                circle.setCircleColor(COMBO_COLORS[2])
                                circle.setComboNumber(1)
                                +circle

                                approach.anchor = Anchor.Center
                                approach.color = COMBO_COLORS[2]
                                +approach
                            }

                            for ((texture, result) in results) {
                                container {
                                    width = 160f.px
                                    height = 160f.px

                                    // Under the judgement, and only for hits, as in gameplay. A texture skin without
                                    // its lighting has none.
                                    lightings += if (result == HitResult.Miss) null else {
                                        skin.create(SkinComponent.HitLighting)?.also {
                                            it.alpha = 0f
                                            +it
                                        }
                                    }
                                    judgements += skin.create(SkinComponent.Judgement(texture))!!.also { +it }
                                }
                            }

                            rows += Row(circle, approach, judgements, lightings)
                        }

                        linearContainer {
                            spacing = 24f.dip

                            // The pieces of sliders, the follow circle at half its size to fit.
                            for (component in listOf(
                                SkinComponent.SliderBall, SkinComponent.FollowCircle, SkinComponent.ReverseArrow,
                                SkinComponent.SliderTick,
                            )) {
                                container {
                                    width = 160f.px
                                    height = 160f.px

                                    +skin.create(component)!!.also {
                                        it.anchor = Anchor.Center
                                        if (it is SliderBallPiece) it.setComboColor(COMBO_COLORS[2])
                                        if (it is FollowCirclePiece) {
                                            it.setComboColor(COMBO_COLORS[2])
                                            it.scale = 0.5f
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            var time = 0f
            var wasHit = false
            onEachFrame { clock ->
                time = (time + clock.elapsedFrameTime) % 2f

                for (row in rows) {
                    if (time < 1f) {
                        if (wasHit) {
                            row.circle.clearAnimations()
                            row.circle.setComboNumber(1)
                            row.circle.showNumber()
                        }
                        row.circle.alpha = 1f
                        row.circle.scale = 1f
                        row.approach.alpha = 1f
                        row.approach.scale = 4f - 3f * time
                    } else if (!wasHit) {
                        val now = row.circle.currentClockTime
                        row.approach.alpha = 0f
                        // Without bursts, a hit circle disappears as soon as it's hit.
                        if (areBurstsShown) row.circle.playHit(1f, now) else row.circle.alpha = 0f
                        row.judgements.forEachIndexed { index, piece ->
                            piece.play(80f, 80f, results[index].second, 1f, now)
                        }
                        if (isHitLightingShown) {
                            row.lightings.forEach { it?.play(80f, 80f, COMBO_COLORS[2], 1f, now) }
                        }
                    }
                }
                wasHit = time >= 1f
            }
        }

        test("Skin sprites") {
            column {
                for (group in listOf(
                    listOf("hitcircle", "hitcircleoverlay", "approachcircle", "sliderb0", "sliderfollowcircle", "reversearrow"),
                    listOf("hit300", "hit100", "hit50", "hit0"),
                    listOf("cursor", "cursortrail", "spinner-circle", "spinner-approachcircle"),
                    listOf("play-skip", "ranking-S-small", "ranking-A-small", "play-unranked"),
                )) {
                    section(group.joinToString(", ")) {
                        for (name in group) {
                            val texture = SkinTextures[name]

                            if (texture == null) {
                                text { text = "$name missing"; color = Color4.Red400 }
                            } else {
                                +Sprite(texture).apply {
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
                    seekBar {
                        width = 400f.dip
                        min = 0f
                        max = 12f
                        onValueChange = { badge.rating = it.toDouble() }
                    }
                }

                for (density in Badge.Density.entries) {
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
            // Without music its glow stays at rest.
            +RimuLogo().apply {
                anchor = Anchor.Center
                origin = Anchor.Center
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

            val video = Video(file.toString()).apply {
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

        test("Notification center") {
            Notifications.reset()
            onDispose { Notifications.reset() }

            Notifications.post("Library", "Skin Refreshed Skin imported.")
            Notifications.post("Import failed", "broken.osz isn't a valid archive.", isError = true)
            Notifications.startTask("Calculating beatmap difficulties", "42% of the beatmaps calculated.", isIndeterminate = false).progress = 0.42f
            Notifications.startTask("123 Artist - Title", "Connecting...").setAction("Cancel") { log("Canceled") }

            lateinit var center: NotificationCenter

            linearContainer {
                orientation = LinearContainer.Orientation.Horizontal
                anchor = Anchor.TopRight
                origin = Anchor.TopRight
                spacing = 8f.dip
                padding = Padding(16f.dip)

                textButton {
                    text = "Post"
                    onActionUp = { Notifications.post("Message", "Posted at ${currentTimeMillis() % 100000}") }
                }

                textButton {
                    text = "Task"
                    onActionUp = {
                        val task = Notifications.startTask("Task", "Running", isIndeterminate = false)
                        var progress = 0f
                        onEachFrame { clock ->
                            if (task.state == Notification.State.Active) {
                                progress += clock.elapsedFrameTime / 3f
                                if (progress >= 1f) task.complete("Done") else task.progress = progress
                            }
                        }
                    }
                }

                val bell = iconButton {
                    icon = FAIcon.Bell
                    onActionUp = { center.show() }
                }
                center = NotificationCenter(bell)
            }

            center.show()
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

        test("Beatmap listing cards") {
            val sets = TestData.listingSets(count = 8)

            // Previewing the first, downloading the second, the third downloaded, the others idle.
            val state = object : BeatmapListingState() {
                override fun togglePreview(set: ListingSet) {
                    playingSetId = if (playingSetId == set.id) null else set.id
                    log("Preview of ${set.title}: ${playingSetId == set.id}")
                }

                override fun download(set: ListingSet, withVideo: Boolean) = log("Download ${set.archiveName(withVideo)}")

                override fun showDetails(set: ListingSet) = log("Details of ${set.title}")
            }
            state.playingSetId = sets[0].id
            state.download = BeatmapDownloader.Download(sets[1].id, BeatmapDownloader.Phase.Downloading, 0.4f)
            state.downloadedSetIds = setOf(sets[2].id)

            // As wide as a small phone in landscape, which fits two columns.
            +RecyclerContainer<ListingSet, BeatmapSetCard>().apply {
                width = 600f.dip
                height = fill()
                padding = Padding(12f.dip)
                minColumnWidth = BeatmapSetCard.MIN_WIDTH
                columnSpacing = 8f.dip
                spacing = 8f.dip

                onCreateComponent = { BeatmapSetCard(state) }
                data = sets

                state.onChange = { boundComponents.values.forEach { it.refresh() } }
            }
        }

        test("Beatmap set details") {
            val sets = TestData.listingSets(count = 8)
            lateinit var details: BeatmapSetDetails

            val state = object : BeatmapListingState() {
                override fun togglePreview(set: ListingSet) {
                    playingSetId = if (playingSetId == set.id) null else set.id
                }

                override fun download(set: ListingSet, withVideo: Boolean) {
                    download = BeatmapDownloader.Download(set.id, BeatmapDownloader.Phase.Downloading, 0.6f)
                    log("Download ${set.archiveName(withVideo)}")
                }
            }
            details = BeatmapSetDetails(state)
            state.onChange = { details.refresh() }

            column {
                section("Sets") {
                    sets.forEach { set ->
                        textButton {
                            text = "${set.title} (${set.beatmaps.size}${if (set.hasVideo) ", video" else ""})"
                            onActionUp = { details.show(set) }
                        }
                    }
                }
            }

            // The set with 20 difficulties.
            details.show(sets[0])
            onDispose { details.hide() }
        }
    }
}
