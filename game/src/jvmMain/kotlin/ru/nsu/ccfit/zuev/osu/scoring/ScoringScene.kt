package ru.nsu.ccfit.zuev.osu.scoring

import ru.nsu.ccfit.zuev.osu.helper.StringTable
import com.osudroid.OsuPlatform
import com.osudroid.GameMode
import com.osudroid.beatmaps.Beatmap
import com.osudroid.beatmaps.BeatmapCache
import com.osudroid.data.BeatmapInfo
import com.osudroid.data.DatabaseManager
import com.osudroid.data.toScoreInfo
import com.osudroid.difficulty.BeatmapDifficultyCalculator
import com.osudroid.difficulty.attributes.DifficultyAttributes
import com.osudroid.difficulty.attributes.DroidDifficultyAttributes
import com.osudroid.difficulty.attributes.PerformanceAttributes
import com.osudroid.difficulty.attributes.StandardDifficultyAttributes
import com.osudroid.mods.ModAutoplay
import com.osudroid.mods.ModCustomSpeed
import com.osudroid.mods.ModDifficultyAdjust
import com.osudroid.mods.ModFlashlight
import com.osudroid.mods.ModNightCore
import com.osudroid.mods.ModOldNightCore
import com.osudroid.multiplayer.Multiplayer
import com.osudroid.multiplayer.isTeamStatistic
import com.osudroid.ui.SendingPanel
import com.osudroid.ui.v2.SkinSounds
import com.osudroid.ui.v2.SkinTextures
import com.osudroid.ui.v2.StatisticSelector
import com.osudroid.ui.v2.game.ScoreNumber
import com.osudroid.ui.v2.modmenu.ModIcon
import com.osudroid.ui.v2.multi.LobbyScene
import com.osudroid.utils.updateThread
import com.reco1l.verktex.Logger
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.input.InputEvent
import com.reco1l.verktex.input.PointerEvent
import com.reco1l.verktex.texture.Textures
import com.reco1l.verktex.ui.UIScene
import com.reco1l.verktex.ui.UISprite
import com.reco1l.verktex.ui.container.UIContainer
import com.reco1l.verktex.ui.container.UILinearContainer
import com.reco1l.verktex.ui.fadeIn
import com.reco1l.verktex.ui.scaleTo
import com.reco1l.verktex.ui.shape.UIBox
import com.reco1l.verktex.ui.text.UIText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.osudroid.audio.MusicService
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.DifficultyAlgorithm
import com.reco1l.verktex.ui.toast.Toasts
import ru.nsu.ccfit.zuev.osu.Utils
import ru.nsu.ccfit.zuev.osu.game.GameScene
import ru.nsu.ccfit.zuev.osu.online.OnlineManager
import ru.nsu.ccfit.zuev.osu.online.OnlineScoring

class ScoringScene(pGame: GameScene) {
    private val game: GameScene = pGame
    var scene: UIScene? = null
        private set
    private var content: UIContainer? = null
    private var music: MusicService? = null
    var replayStat: StatisticV2? = null
    var replayID = -1
    @JvmField
    var beatmapInfo: BeatmapInfo? = null

    // Multiplayer
    @JvmField
    var currentStatistic: StatisticV2? = null

    private var selector: StatisticSelector? = null


    private fun createSprite(texture: String, x: Float, y: Float): UISprite {
        val sprite = UISprite(SkinTextures.get(texture))
        sprite.setPosition(x, y)
        return sprite
    }

    /**
     * Creates a sprite centered in the given position with a size relative to its texture.
     */
    private fun createHitSprite(texture: String, centerX: Float, centerY: Float): UISprite {
        val sprite = createSprite(texture, Utils.toRes(centerX), Utils.toRes(centerY))
        sprite.origin = Anchor.Center
        return sprite
    }

    private fun createText(value: String, fontSize: Float): UIText {
        val text = UIText()
        text.fontSize = Dimension.Fixed.Pixels(fontSize)
        text.text = value
        return text
    }

    private fun textureWidth(texture: String): Float {
        val region = SkinTextures.get(texture)
        return region?.width?.toFloat() ?: 0f
    }

    private fun textureHeight(texture: String): Float {
        val region = SkinTextures.get(texture)
        return region?.height?.toFloat() ?: 0f
    }

    private fun createButton(texture: String, onClick: Runnable): UISprite {
        return object : UISprite(SkinTextures.get(texture)) {
            private var isPressed = false

            override fun onInputEvent(event: InputEvent): Boolean {
                if (event !is PointerEvent) {
                    return false
                }

                if (event.action == PointerEvent.Action.Down && containsScenePoint(event.x, event.y)) {
                    isPressed = true
                    setColor(0.7f, 0.7f, 0.7f)
                    SkinSounds.get("menuback")!!.play()
                    return true
                }

                if (isPressed && event.action == PointerEvent.Action.Up) {
                    isPressed = false
                    onClick.run()
                    return true
                }

                return false
            }
        }
    }

    private fun leaveAndRun(action: Runnable) {
        Verktex.withTreeLock {
            Verktex.popScene()
        }

        action.run()
    }

    fun load(stat: StatisticV2, beatmap: BeatmapInfo?,
             player: MusicService?, replayPath: String?, mapMD5: String?,
             beatmapToReplay: BeatmapInfo?) {
        val wasShown = scene != null && Verktex.scene === scene
        val previousScene = scene

        val scene = UIScene()
        this.scene = scene
        music = player
        currentStatistic = stat
        if (replayPath != null && beatmap == null) {
            replayStat = stat
        }

        val backgroundSprite = UISprite(if (Textures.get("::background") != null)
            Textures.get("::background")
        else
            SkinTextures.get("menu-background"))
        backgroundSprite.width = Dimension.FillAvailable
        backgroundSprite.height = Dimension.FillAvailable
        backgroundSprite.scaleType = UISprite.ScaleType.Crop
        scene.background = backgroundSprite

        // Everything is placed in the coordinates of the game's resolution and scaled to fit the window.
        val content = UIContainer()
        this.content = content
        content.setSize(Config.getRES_WIDTH().toFloat(), Config.getRES_HEIGHT().toFloat())
        content.scaleOrigin = Anchor.TopLeft
        content.scale = Verktex.graphics.window.windowWidth.toFloat() / Config.getRES_WIDTH()
        scene += content

        val bgTopRect = UIBox()
        bgTopRect.setSize(Config.getRES_WIDTH().toFloat(), Utils.toRes(100).toFloat())
        bgTopRect.backgroundColor = Color4(0f, 0f, 0f, 0.8f)
        content += bgTopRect

        beatmapInfo = beatmapToReplay
        if (beatmapToReplay == null && beatmap != null) {
            beatmapInfo = beatmap
        }

        val paddingButtom = if (Multiplayer.isConnected)
            Multiplayer.roomScene!!.chat.buttonHeight
        else
            0f

        val scaledContainer = UIContainer()
        scaledContainer.setSize(Config.getRES_WIDTH().toFloat(), Config.getRES_HEIGHT().toFloat())
        scaledContainer.setPosition(0f, 100f)
        scaledContainer.scaleOrigin = Anchor.TopLeft
        scaledContainer.scale = (Config.getRES_HEIGHT() - paddingButtom) / Config.getRES_HEIGHT()
        content += scaledContainer

        val x = 0
        val y = 0
        val panel = createSprite("ranking-panel", x.toFloat(), y.toFloat())
        panel.setSize(Utils.toRes(textureWidth("ranking-panel") * 0.9f), Utils.toRes(textureHeight("ranking-panel") * 0.9f))
        scaledContainer += panel

        scaledContainer += createHitSprite("hit300", (70 + x).toFloat(), (130 + y).toFloat())
        scaledContainer += createHitSprite("hit100", (70 + x).toFloat(), (130 + 92 + y).toFloat())
        scaledContainer += createHitSprite("hit50", (70 + x).toFloat(), (130 + 92 * 2 + y).toFloat())
        scaledContainer += createHitSprite("hit300g", (340 + x).toFloat(), (130 + y).toFloat())
        scaledContainer += createHitSprite("hit100k", (340 + x).toFloat(), (130 + 92 + y).toFloat())
        scaledContainer += createHitSprite("hit0", (340 + x).toFloat(), (130 + 92 * 2 + y).toFloat())

        content += createSprite("ranking-title",
            (Config.getRES_WIDTH() * 5).toFloat() / 6 - textureWidth("ranking-title") / 2, 0f)

        val scoreStr = StringBuilder(stat.totalScoreWithMultiplier.toString())
        while (scoreStr.length < 8) {
            scoreStr.insert(0, "0")
        }
        val scoreNum = ScoreNumber(Utils.toRes(220 + x).toFloat(),
            Utils.toRes(18 + y).toFloat(), scoreStr.toString(), 1f, false)
        scaledContainer += scoreNum

        val hit300num = ScoreNumber(Utils.toRes(138 + x).toFloat(),
            Utils.toRes(110 + y).toFloat(), stat.hit300.toString() + "x", 1f,
            false)
        scaledContainer += hit300num
        val hit100num = ScoreNumber(Utils.toRes(138 + x).toFloat(),
            Utils.toRes(110 + 85 + y).toFloat(), stat.hit100.toString() + "x",
            1f, false)
        scaledContainer += hit100num
        val hit50num = ScoreNumber(Utils.toRes(138 + x).toFloat(),
            Utils.toRes(110 + 85 * 2 + y).toFloat(), stat.hit50.toString() + "x",
            1f, false)
        scaledContainer += hit50num

        val hit300knum = ScoreNumber(Utils.toRes(400 + x).toFloat(),
            Utils.toRes(110 + y).toFloat(), stat.hit300k.toString() + "x", 1f,
            false)
        scaledContainer += hit300knum
        val hit100knum = ScoreNumber(Utils.toRes(400 + x).toFloat(),
            Utils.toRes(110 + 85 + y).toFloat(), stat.hit100k.toString() + "x",
            1f, false)
        scaledContainer += hit100knum
        val hit0num = ScoreNumber(Utils.toRes(400 + x).toFloat(),
            Utils.toRes(110 + 85 * 2 + y).toFloat(), stat.misses.toString() + "x",
            1f, false)
        scaledContainer += hit0num

        scaledContainer += createSprite("ranking-maxcombo", Utils.toRes(20 + x).toFloat(), Utils.toRes(332 + y).toFloat())
        scaledContainer += createSprite("ranking-accuracy", Utils.toRes(260 + x).toFloat(), Utils.toRes(332 + y).toFloat())
        val maxCombo = ScoreNumber(Utils.toRes(20 + x).toFloat(),
            Utils.toRes(332 + y + 38).toFloat(), stat.scoreMaxCombo.toString() + "x", 1f,
            false)
        scaledContainer += maxCombo
        val accStr = String.format(Locale.ENGLISH, "%2.2f%%", stat.accuracy * 100)
        val accuracy = ScoreNumber(Utils.toRes(260 + x).toFloat(),
            Utils.toRes(332 + y + 38).toFloat(), accStr, 1f, false)
        scaledContainer += accuracy

        val markX = (Config.getRES_WIDTH() * 5).toFloat() / 6 - textureWidth("ranking-" + stat.mark) / 2
        val mark = createSprite("ranking-" + stat.mark, markX, 80f)

        if (beatmap != null) {
            mark.alpha = 0f
            mark.scale = 2f
            mark.fadeIn(2f)
            mark.scaleTo(1f, 2f)
        }

        content += mark

        val backBtn = createButton("ranking-back") { back() }
        val buttonX = Config.getRES_WIDTH() - textureWidth("ranking-back") - 10
        val backY = Config.getRES_HEIGHT() - textureHeight("ranking-back") - 10 - paddingButtom
        backBtn.setPosition(buttonX, backY)
        content += backBtn

        val mods = stat.mod

        if (!Multiplayer.isMultiplayer && beatmap != null) {
            val retryBtn = createButton("ranking-retry") {
                SkinSounds.get("applause")!!.stop()
                stopMusic()
                this.scene = null
                leaveAndRun { game.startGame(null, null, null) }
            }
            retryBtn.setPosition(buttonX, backY - textureHeight("ranking-retry") - 10)
            content += retryBtn
        } else if (!Multiplayer.isMultiplayer && replayPath != null) {
            val replayBtn = createButton("ranking-replay") {
                SkinSounds.get("applause")!!.stop()
                stopMusic()
                this.scene = null
                leaveAndRun { game.startGame(beatmapToReplay, replayPath, mods) }
            }
            replayBtn.setPosition(buttonX, backY - textureHeight("ranking-replay") - 10)
            content += replayBtn
        }

        if (stat.isPerfect) {
            scaledContainer += createSprite("ranking-perfect", 0f, Utils.toRes(332 + y + 38) + accuracy.letterHeight + 10)
        }

        var modX = markX - 30
        val modY = 80 + textureHeight("ranking-" + stat.mark) * 2 / 3
        for (mod in mods.values) {
            if (!mod.isUserPlayable) {
                continue
            }

            val modIcon = ModIcon(mod)
            modIcon.setPosition(modX, modY)
            modIcon.setSize(68f, 66f)
            modX -= 30f
            content += modIcon
        }

        val beatmapInfo = beatmapInfo!!
        val infoStr = beatmapInfo.artistText + " - " + beatmapInfo.titleText + " [" + beatmapInfo.version + "]"

        var mapperStr = "Beatmap by " + beatmapInfo.creator
        var playerStr = "Played by " + stat.playerName + " on " +
            SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(Date(stat.time))
        playerStr += String.format("  %s(%s)", OsuPlatform.implementation.versionName, OsuPlatform.implementation.buildType)
        if (ModCustomSpeed::class in mods ||
            ModDifficultyAdjust::class in mods ||
            (ModFlashlight::class in mods && !mods.ofType(ModFlashlight::class)!!.usesDefaultSettings)) {

            val customSpeed = mods.ofType(ModCustomSpeed::class)
            val difficultyAdjust = mods.ofType(ModDifficultyAdjust::class)
            val flashlight = mods.ofType(ModFlashlight::class)

            mapperStr += " ["
            if (customSpeed != null && customSpeed.trackRateMultiplier != 1f) {
                mapperStr += String.format(Locale.ENGLISH, "%.2fx,", customSpeed.trackRateMultiplier)
            }

            if (difficultyAdjust != null) {
                if (difficultyAdjust.ar != null) {
                    mapperStr += String.format(Locale.ENGLISH, "AR%.1f,", difficultyAdjust.ar)
                }

                if (difficultyAdjust.od != null) {
                    mapperStr += String.format(Locale.ENGLISH, "OD%.1f,", difficultyAdjust.od)
                }

                if (difficultyAdjust.cs != null) {
                    mapperStr += String.format(Locale.ENGLISH, "CS%.1f,", difficultyAdjust.cs)
                }

                if (difficultyAdjust.hp != null) {
                    mapperStr += String.format(Locale.ENGLISH, "HP%.1f,", difficultyAdjust.hp)
                }
            }

            if (flashlight != null && !flashlight.usesDefaultSettings) {
                if (flashlight.followDelay != ModFlashlight.DEFAULT_FOLLOW_DELAY) {
                    mapperStr += String.format(Locale.ENGLISH, "FLD%.2f,", flashlight.followDelay)
                }

                if (flashlight.sizeMultiplier != ModFlashlight.DEFAULT_SIZE_MULTIPLIER) {
                    mapperStr += String.format(Locale.ENGLISH, "%.2fx,", flashlight.sizeMultiplier)
                }
            }

            if (mapperStr.endsWith(",")) {
                mapperStr = mapperStr.substring(0, mapperStr.length - 1)
            }

            mapperStr += "]"
        }
        val infoContainer = UILinearContainer()
        infoContainer.orientation = UILinearContainer.Orientation.Vertical
        infoContainer.setPosition(Utils.toRes(4).toFloat(), Utils.toRes(2).toFloat())
        infoContainer += createText(infoStr, 28f)
        infoContainer += createText(mapperStr, 21f)
        infoContainer += createText(playerStr, 21f)

        if (Config.isDisplayScoreStatistics() && !currentStatistic!!.isTeamStatistic()) {

            val ppinfo = StringBuilder()
            var beatmapData: Beatmap?

            try {
                beatmapData = BeatmapCache.getBeatmap(
                    beatmapInfo,
                    true,
                    when (Config.getDifficultyAlgorithm()) {
                        DifficultyAlgorithm.droid -> GameMode.Droid
                        DifficultyAlgorithm.standard -> GameMode.Standard
                    }
                )
            } catch (e: Exception) {
                Logger.e("ScoringScene", "Failed to parse beatmap for difficulty calculation", e)
                beatmapData = null
            }

            if (beatmapData != null) {
                var difficultyAttributes: DifficultyAttributes? = null
                var performanceAttributes: PerformanceAttributes? = null

                // Don't try to load online replay
                if (replayPath != null && beatmapToReplay != null && !replayPath.startsWith("https://")) {
                    val replay = Replay()
                    replay.setObjectCount(beatmapToReplay.totalHitObjectCount)
                    replay.setBeatmap(beatmapToReplay.fullBeatmapsetName, beatmapToReplay.fullBeatmapName, mapMD5!!)

                    if (replay.load(replayPath, true)) {
                        // If statistics information is not complete, we populate it with information from replay.
                        if (
                            stat.sliderHeadHits == -1 ||
                            stat.sliderTickHits == -1 ||
                            stat.sliderRepeatHits == -1 ||
                            stat.sliderEndHits == -1
                        ) {
                            replay.populateStatistics(beatmapData, stat)
                        }

                        var copiedMods = mods

                        if (ModNightCore::class in mods && replay.replayVersion <= 3) {
                            copiedMods = mods.deepCopy()
                            copiedMods.removeOfType(ModNightCore::class)
                            copiedMods.put(ModOldNightCore::class)
                        }

                        when (Config.getDifficultyAlgorithm()) {
                            DifficultyAlgorithm.droid -> {
                                val playableBeatmap = beatmapData.createDroidPlayableBeatmap(copiedMods.values)

                                difficultyAttributes = BeatmapDifficultyCalculator.calculateDroidDifficultyForReplay(beatmapData, playableBeatmap)

                                performanceAttributes = BeatmapDifficultyCalculator.calculateDroidPerformance(
                                    playableBeatmap, difficultyAttributes, replay, stat
                                )
                            }

                            DifficultyAlgorithm.standard -> {
                                val playableBeatmap = beatmapData.createStandardPlayableBeatmap(copiedMods.values)

                                difficultyAttributes = BeatmapDifficultyCalculator.calculateStandardDifficulty(beatmapData, playableBeatmap)

                                @Suppress("UNCHECKED_CAST")
                                performanceAttributes = BeatmapDifficultyCalculator.calculateStandardPerformance(
                                    playableBeatmap, difficultyAttributes,
                                    replay.objectData as Array<Replay.ReplayObjectData>, stat
                                )
                            }
                        }
                    }
                }

                if (difficultyAttributes == null) {
                    difficultyAttributes = when (Config.getDifficultyAlgorithm()) {
                        DifficultyAlgorithm.droid -> BeatmapDifficultyCalculator.calculateDroidDifficulty(beatmapData, mods.values)
                        DifficultyAlgorithm.standard -> BeatmapDifficultyCalculator.calculateStandardDifficulty(beatmapData, mods.values)
                    }

                    performanceAttributes = when (Config.getDifficultyAlgorithm()) {
                        DifficultyAlgorithm.droid -> BeatmapDifficultyCalculator.calculateDroidPerformance(
                            beatmapData, difficultyAttributes as DroidDifficultyAttributes, stat
                        )

                        DifficultyAlgorithm.standard -> BeatmapDifficultyCalculator.calculateStandardPerformance(
                            beatmapData, difficultyAttributes as StandardDifficultyAttributes, stat
                        )
                    }
                }

                val maxPerformanceAttributes = when (Config.getDifficultyAlgorithm()) {
                    DifficultyAlgorithm.droid -> BeatmapDifficultyCalculator.calculateDroidPerformance(
                        difficultyAttributes as DroidDifficultyAttributes
                    )

                    DifficultyAlgorithm.standard -> BeatmapDifficultyCalculator.calculateStandardPerformance(
                        difficultyAttributes as StandardDifficultyAttributes
                    )
                }

                ppinfo.append(String.format(
                    Locale.ENGLISH,
                    "%.2f★ | %.2f/%.2f" + (if (Config.getDifficultyAlgorithm() == DifficultyAlgorithm.droid) "dpp" else "pp"),
                    difficultyAttributes.starRating,
                    performanceAttributes!!.total,
                    maxPerformanceAttributes.total
                ))
            }

            if (stat.unstableRate > 0) {
                if (beatmapData != null) {
                    ppinfo.append("\n")
                }
                ppinfo.append(String.format(Locale.ENGLISH, "Error: %.2fms - %.2fms avg", stat.negativeHitError, stat.positiveHitError))
                ppinfo.append("\n")
                ppinfo.append(String.format(Locale.ENGLISH, "Unstable Rate: %.2f", stat.unstableRate))
            }

            val ppInfo = createText(ppinfo.toString(), 21f)
            ppInfo.backgroundColor = Color4(0f, 0f, 0f, 0.5f)
            ppInfo.origin = Anchor.BottomLeft
            ppInfo.setPosition(Utils.toRes(240).toFloat(), Config.getRES_HEIGHT() - Utils.toRes(2) - paddingButtom)
            content += ppInfo
        }
        content += infoContainer

        // In case the scene was reloaded
        if (Multiplayer.isMultiplayer) {
            updateLeaderboard()
        }

        if (wasShown && previousScene !== scene) {
            Verktex.withTreeLock {
                Verktex.popScene()
                Verktex.pushScene(scene)
            }
        }

        if (beatmap != null && beatmap.md5 == mapMD5) {
            SkinSounds.get("applause")!!.play()

            // Save score
            if (stat.totalScoreWithMultiplier <= 0 || ModAutoplay::class in mods ||
                (Multiplayer.isMultiplayer &&
                    ((Multiplayer.room != null && Multiplayer.room!!.isTeamVersus) || game.hasFailed))) {
                return
            }

            val totalNotes = stat.hit300 + stat.hit100 + stat.hit50 + stat.misses

            // Do not save and submit score if note count does not match, since it indicates a corrupted score
            // (potentially from bugging the gameplay by any unnecessary means).
            if (totalNotes != beatmap.totalHitObjectCount) {
                Toasts.showLong(StringTable.get(com.osudroid.resources.Res.string.replay_corrupted))
                return
            }

            stat.replayFilename = replayPath?.let { java.io.File(it).name }.orEmpty()
            stat.beatmapMD5 = beatmap.md5

            try {
                DatabaseManager.scoreInfoTable.insertScore(stat.toScoreInfo())
            } catch (e: Exception) {
                Logger.e("GameScene", "Failed to save score to database", e)
            }

            // Upload score
            if (!OnlineManager.getInstance().isStayOnline ||
                (Multiplayer.isMultiplayer && !Config.isSubmitScoreOnMultiplayer())) {
                return
            }

            val hasUnrankedMod = mods.values.any { m -> !m.isRanked }

            if (hasUnrankedMod || Multiplayer.isRemoveSliderLock()) {
                return
            }

            val sendingPanel = SendingPanel(OnlineManager.getInstance().rank,
                OnlineManager.getInstance().score, OnlineManager.getInstance().accuracy,
                OnlineManager.getInstance().pp)

            content += sendingPanel

            OnlineScoring.getInstance().sendRecord(beatmap, stat, sendingPanel, replayPath)
        }
    }

    fun updateLeaderboard() {

        if (Multiplayer.finalData != null) {

            if (selector != null) {
                val oldSelector = selector!!
                updateThread { oldSelector.removeSelf() }
            }

            selector = StatisticSelector(Multiplayer.finalData!!)

            if (scene != null) {
                content!! += selector!!
            }
        }
    }

    fun back() {
        SkinSounds.get("applause")!!.stop()
        Multiplayer.finalData = null
        currentStatistic = null

        if (Multiplayer.isMultiplayer) {
            // Preventing NPEs when player gets disconnected while playing
            if (!Multiplayer.isConnected) {
                leaveAndRun { Verktex.pushScene(LobbyScene()) }
            } else {
                leaveAndRun {
                    if (Verktex.scene !== Multiplayer.roomScene) {
                        Multiplayer.roomScene!!.show()
                    }
                }
            }
            return
        }

        replayMusic()
        leaveAndRun {}
        replayID = -1
    }

    fun stopMusic() {
        music?.stop()
    }

    fun replayMusic() {
        if (music != null) {
            music!!.stop()
//            music.preLoadWithLoop(game.filePath);
            music!!.preLoad(beatmapInfo!!.audioPath)
            music!!.play()
        }
    }
}
