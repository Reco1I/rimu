package com.osudroid.ui.v2.mainmenu

import com.reco1l.verktex.ui.text.spans
import com.reco1l.verktex.data.LayoutConstraints
import com.acivev.ui.addSnowfallWithPeriod
import com.acivev.ui.addFireworksWithPeriod
import ru.nsu.ccfit.zuev.osu.scoring.Replay
import com.reco1l.verktex.ui.dialog.UIConfirmDialog
import com.osudroid.data.BeatmapInfo
import com.osudroid.ui.v2.SkinSounds
import android.content.*
import android.net.*
import android.util.*
import androidx.core.content.*
import com.osudroid.*
import com.osudroid.beatmaplisting.*
import com.osudroid.multiplayer.*
import com.osudroid.resources.R
import com.osudroid.ui.v2.LoaderScene
import com.osudroid.ui.v2.SkinTextures
import com.osudroid.ui.v2.multi.*
import com.osudroid.ui.v2.settings.SettingsMenu
import com.osudroid.ui.v2.songselect.SongSelect
import com.osudroid.utils.*
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Axis
import com.reco1l.verktex.data.ClipMode
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.data.pct
import com.reco1l.verktex.data.px
import com.reco1l.verktex.math.Easing
import com.reco1l.verktex.math.FloatInterpolation
import com.reco1l.verktex.texture.Textures
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.ui.FAIcon
import com.reco1l.verktex.ui.UIButton
import com.reco1l.verktex.ui.UIGradientBox
import com.reco1l.verktex.ui.UIScene
import com.reco1l.verktex.ui.UISprite
import com.reco1l.verktex.ui.UITextButton
import com.reco1l.verktex.ui.container.UIContainer
import com.reco1l.verktex.ui.container.UILinearContainer
import com.reco1l.verktex.ui.container.UIScrollableContainer
import com.reco1l.verktex.ui.container.clickableContainer
import com.reco1l.verktex.ui.container.container
import com.reco1l.verktex.ui.container.linearContainer
import com.reco1l.verktex.ui.container.scrollableContainer
import com.reco1l.verktex.ui.dialog.UIDialog
import com.reco1l.verktex.ui.dialog.UIMessageDialog
import com.reco1l.verktex.ui.fadeIn
import com.reco1l.verktex.ui.fadeOut
import com.reco1l.verktex.ui.rotateTo
import com.reco1l.verktex.ui.scaleTo
import com.reco1l.verktex.ui.shape.box
import com.reco1l.verktex.ui.sprite
import com.reco1l.verktex.ui.text.faSolidIcon
import com.reco1l.verktex.ui.text.text
import com.reco1l.verktex.ui.textButton
import ru.nsu.ccfit.zuev.osu.*
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.helper.*
import ru.nsu.ccfit.zuev.osu.online.*
import ru.nsu.ccfit.zuev.osuplus.BuildConfig
import java.io.*
import kotlin.math.*


object MainScene : UIScene() {

    private lateinit var playerButton: PlayerButton
    private lateinit var musicButton: UITextButton
    private lateinit var leftFlash: UIGradientBox
    private lateinit var rightFlash: UIGradientBox
    private lateinit var backgroundSprite: UISprite
    private lateinit var logo: OsuLogo
    private lateinit var menuContainer: UIContainer
    private lateinit var menuGradientBox: UIGradientBox

    private var isMenuExpanded = false
    private var isExiting = false
    private var areSeasonalEffectsAdded = false

    private var lastMusicChange = System.currentTimeMillis()

    private var menuWidth = 0f
    private var menuContentWidth = 0f
    private var musicButtonTranslation = 0f
    private var playerButtonTranslation = 0f


    init {

        background = UIContainer().apply {
            width = Dimension.FillAvailable
            height = Dimension.FillAvailable

            backgroundSprite = sprite {
                width = Dimension.FillAvailable
                height = Dimension.FillAvailable
                scaleType = UISprite.ScaleType.Crop
                textureRegion = SkinTextures["menu-background"]
            }

            +UIGradientBox().apply {
                height = Dimension.FillAvailable
                gradientAngle = 0f
                colorStart = Color4.White
                colorEnd = Color4.Transparent
                width = 192f.dip

                alpha = 0f
                leftFlash = this
            }

            +UIGradientBox().apply {
                height = Dimension.FillAvailable
                anchor = Anchor.TopRight
                origin = Anchor.TopRight
                gradientAngle = 180f
                colorStart = Color4.White
                colorEnd = Color4.Transparent
                width = 192f.dip

                alpha = 0f
                rightFlash = this
            }

            +UIGradientBox().apply {
                width = Dimension.FillAvailable
                height = Dimension.FillAvailable
                gradientAngle = 270f
                alpha = 0f
                colorStart = Verktex.theme.palette.accent * 0.1f
                colorEnd = (Verktex.theme.palette.accent * 0.1f).copy(alpha = 0.5f)
                menuGradientBox = this
            }
        }

        linearContainer {
            orientation = UILinearContainer.Orientation.Horizontal
            anchor = Anchor.Center
            origin = Anchor.Center

            +OsuLogo().apply {
                anchor = Anchor.CenterLeft
                origin = Anchor.CenterLeft
                scaleOrigin = Anchor.Center
                rotationOrigin = Anchor.Center
                width = 256f.dip
                height = 256f.dip

                onActionUp = {
                    playClickEffect()
                    isMenuExpanded = !isMenuExpanded
                }

                logo = this
            }

            menuContainer = scrollableContainer {
                scrollAxes = Axis.Y
                showVerticalIndicator = false
                anchor = Anchor.CenterLeft
                origin = Anchor.CenterLeft
                padding = Padding(left = 32f.dip)
                width = 0f.px
                alpha = 0f

                +CarrouselLinearContainer(logo).apply {
                    orientation = UILinearContainer.Orientation.Vertical
                    spacing = 4.6f.dip
                    padding = Padding(0f.dip, 128f.dip)

                    +MenuButton(FAIcon.User, "Solo").apply {
                        onActionUp = {
                            async {
                                LoaderScene().show()

                                GlobalManager.getInstance().mainActivity.checkNewSkins()
                                GlobalManager.getInstance().mainActivity.loadBeatmapLibrary()
                                SongSelect.loadBeatmaps()

                                if (LibraryManager.getLibrary().isEmpty()) {
                                    Verktex.withTreeLock { Verktex.popScene() }
                                    BeatmapListing().show()
                                } else {
                                    GlobalManager.getInstance().songService.isGaming = true

                                    Verktex.withTreeLock {
                                        Verktex.popScene()
                                        Verktex.pushScene(SongSelect)
                                    }
                                }
                            }
                        }
                    }

                    +MenuButton(FAIcon.UserGroup, "Multi").apply {
                        onActionUp = action@{
                            if (!OnlineManager.getInstance().isStayOnline && !BuildSettings.MOCK_MULTIPLAYER) {
                                ToastLogger.showText(StringTable.format(R.string.multiplayer_not_online), true)
                                return@action
                            }

                            GlobalManager.getInstance().songService.isGaming = true
                            Multiplayer.isMultiplayer = true

                            async {
                                LoaderScene().show()

                                GlobalManager.getInstance().mainActivity.checkNewSkins()
                                GlobalManager.getInstance().mainActivity.loadBeatmapLibrary()

                                SongSelect.loadBeatmaps()

                                Verktex.withTreeLock {
                                    Verktex.popScene()
                                    Verktex.pushScene(LobbyScene())
                                }
                            }
                        }
                    }

                    +MenuButton(FAIcon.TableList, "Browse").apply {
                        onActionUp = {
                            mainThread { BeatmapListing().show() }
                        }
                    }

                    +MenuButton(FAIcon.Gear, "Settings").apply {
                        onActionUp = {
                            Verktex.pushScene(SettingsMenu())
                        }
                    }

                    +MenuButton(FAIcon.ArrowRightFromBracket, "Exit").apply {
                        onActionUp = {
                            UIMessageDialog().apply {
                                title = "Exit game"
                                text = "Are you sure you want to exit?"
                                addButton {
                                    text = "Yes"
                                    onActionUp = {
                                        hide()
                                        exit()
                                    }
                                }

                                addButton {
                                    text = "No"
                                    onActionUp = {
                                        hide()
                                    }
                                }
                            }.show()
                        }
                    }

                }

            }

        }

        container {
            width = Dimension.FillAvailable
            padding = Padding(top = 8f.dip, bottom = 8f.dip)

            +PlayerButton().apply {
                anchor = Anchor.TopLeft
                origin = Anchor.TopLeft
                scaleOrigin = Anchor.Center
                playerButton = this
            }

            musicButton = textButton {
                anchor = Anchor.TopRight
                origin = Anchor.TopRight
                scaleOrigin = Anchor.Center
                leadingIcon = faSolidIcon(FAIcon.Music)

                val musicPlayer = MusicPlayer(this)
                onActionUp = {
                    if (alpha > 0f)
                        musicPlayer.show()
                }
            }
        }

        clickableContainer {
            anchor = Anchor.BottomLeft
            origin = Anchor.BottomLeft
            paddingBottom = 16f.dip

            text {
                text = "osu!droid ${BuildConfig.VERSION_NAME}"
                style = Style { theme ->
                    backgroundColor = Color4.Black.copy(alpha = 0.5f)
                    padding = Padding(8f.dip, 4f.dip)
                    radius = theme.radius.md
                    fontSize = theme.typography.smallBody.size
                }
            }

            onActionUp = {
                BuildInformationDialog().show()
            }
        }


        RythimManager.addOnBeatChangeListener(this) {
            if (!RythimManager.isKiai) {
                if (RythimManager.beatIndex == 0) {
                    leftFlash.alpha = 0.35f
                    rightFlash.alpha = 0.35f
                }
            } else {
                // +1 Because 0 is also even and we don't want double flashes on the first beat
                if ((RythimManager.beatIndex + 1) % 2 == 0) {
                    leftFlash.alpha = 0.4f
                } else {
                    rightFlash.alpha = 0.4f
                }
            }

        }

        MusicManager.addOnBeatmapChangeListener(this) { beatmap ->
            val backgroundPath = beatmap?.backgroundPath
            val textureRegion = if (backgroundPath != null) Textures.load("::background", backgroundPath) else null

            if (textureRegion != null && !Config.isSafeBeatmapBg()) {
                backgroundSprite.textureRegion = textureRegion
            } else {
                backgroundSprite.textureRegion = SkinTextures["menu-background"]
            }

            musicButton.text = "${MusicManager.currentBeatmap?.titleText} - ${MusicManager.currentBeatmap?.artistText}"
        }
    }


    /**
     * Asks for confirmation before leaving the game.
     */
    fun showExitDialog() {
        if (isExiting) {
            return
        }

        UIConfirmDialog().apply {
            title = "Exit"
            text = GlobalManager.getInstance().mainActivity.getString(R.string.dialog_exit_message)
            onConfirm = { exit() }
        }.show()
    }

    /**
     * Sets the beatmap being played in the menu.
     */
    fun setBeatmap(beatmapInfo: BeatmapInfo) {
        GlobalManager.getInstance().selectedBeatmap = beatmapInfo

        MusicManager.currentBeatmap = beatmapInfo
        MusicManager.load()
        MusicManager.play()
    }

    /**
     * Shows the results screen of a replay file.
     */
    fun watchReplay(replayFile: String) {
        val replay = Replay()

        if (!replay.load(replayFile, false) || replay.replayVersion < 3) {
            return
        }

        val beatmap = LibraryManager.findBeatmapByMD5(replay.md5) ?: return
        val global = GlobalManager.getInstance()

        setBeatmap(beatmap)

        val stat = replay.stat
        stat.migrateLegacyMods(beatmap.getBeatmapDifficulty())
        stat.calculateModScoreMultiplier(beatmap.getBeatmapDifficulty())

        global.scoring.load(stat, null, global.songService, replayFile, null, beatmap)
        Verktex.pushScene(global.scoring.scene)
    }

    /**
     * Plays the exit animation and closes the game.
     */
    fun exit() {
        if (isExiting) {
            return
        }

        isExiting = true
        isMenuExpanded = false

        MusicManager.stop()
        SkinSounds["seeya"].play()

        backgroundSprite.fadeOut(0.4f)

        logo.playEffects = false
        logo.scaleTo(0.5f, 3f)
        logo.rotateTo(-15f, 3f)

        leftFlash.removeSelf()
        rightFlash.removeSelf()

        box {
            width = Dimension.FillAvailable
            height = Dimension.FillAvailable
            backgroundColor = Color4.Black
            alpha = 0f

            fadeIn(3f).after {
                GlobalManager.getInstance().mainActivity.finish()
            }
        }
    }


    override fun onAttached() {
        super.onAttached()

        if (!areSeasonalEffectsAdded) {
            areSeasonalEffectsAdded = true

            val context = GlobalManager.getInstance().mainActivity

            (background as? UIContainer)?.let {
                addSnowfallWithPeriod(it, context)
                addFireworksWithPeriod(it, context)
            }
        }

        if (!MusicManager.isPlaying || MusicManager.currentBeatmap == null) {
            if (MusicManager.currentBeatmap == null) {
                MusicManager.currentBeatmap = LibraryManager.getLibrary().random().beatmaps.random()
            }
            MusicManager.load()
            MusicManager.play()

            musicButton.text = "${MusicManager.currentBeatmap?.titleText} - ${MusicManager.currentBeatmap?.artistText}"
            lastMusicChange = System.currentTimeMillis()
        }
    }


    override fun onUpdate(clock: FrameClock) {
        val deltaTimeSec = clock.elapsedFrameTime

        MusicManager.onUpdate(deltaTimeSec)

        logo.scale = FloatInterpolation.floatAt(deltaTimeSec.coerceIn(0f, 0.1f), logo.scaleX, if (isMenuExpanded) 1f else 1.3f, 0f, 0.1f)

        // Music button
        val mightShowMusicButton = isMenuExpanded || System.currentTimeMillis() - lastMusicChange < 3000

        musicButtonTranslation = FloatInterpolation.floatAt(deltaTimeSec.coerceIn(0f, 0.05f), musicButtonTranslation, if (mightShowMusicButton) 0f else 32f.dip.toPixels(), 0f, 0.05f)
        musicButton.translationX = musicButtonTranslation.px
        musicButton.alpha = FloatInterpolation.floatAt(deltaTimeSec.coerceIn(0f, 0.05f), musicButton.alpha, if (mightShowMusicButton) 1f else 0f, 0f, 0.05f)

        // Beat animations
        val beatLengthSeconds = RythimManager.beatLength.toFloat() / 1000f * (if (RythimManager.isKiai) 1f else RythimManager.beatSignature.toFloat())
        leftFlash.alpha = FloatInterpolation.floatAt(deltaTimeSec.coerceIn(0f, beatLengthSeconds), leftFlash.alpha, 0f, 0f, beatLengthSeconds)
        rightFlash.alpha = FloatInterpolation.floatAt(deltaTimeSec.coerceIn(0f, beatLengthSeconds), rightFlash.alpha, 0f, 0f, beatLengthSeconds)

        // Menu expansion animations
        // The content is measured by itself, inside the container it would be limited to the width being animated.
        if (menuContentWidth <= 0f && menuContainer.childCount > 0) {
            val unbounded = LayoutConstraints(0f, Float.POSITIVE_INFINITY, 0f, Float.POSITIVE_INFINITY)
            menuContentWidth = menuContainer[0].measure(unbounded).x + menuContainer.paddingLeft.toPixels()
        }

        menuWidth = FloatInterpolation.floatAt(deltaTimeSec.coerceIn(0f, 0.2f), menuWidth, if (isMenuExpanded) menuContentWidth else 0f, 0f, 0.2f, Easing.OutQuint)
        menuContainer.width = menuWidth.px
        menuContainer.alpha = FloatInterpolation.floatAt(deltaTimeSec.coerceIn(0f, 0.2f), menuContainer.alpha, if (isMenuExpanded) 1f else 0f, 0f, 0.2f, Easing.OutQuint)
        menuGradientBox.alpha = FloatInterpolation.floatAt(deltaTimeSec.coerceIn(0f, 0.4f), menuGradientBox.alpha, if (isMenuExpanded) 1f else 0f, 0f, 0.4f, Easing.OutQuint)

        playerButtonTranslation = FloatInterpolation.floatAt(deltaTimeSec.coerceIn(0f, 0.05f), playerButtonTranslation, if (isMenuExpanded) 0f else (-32f).dip.toPixels(), 0f, 0.05f)
        playerButton.translationX = playerButtonTranslation.px
        playerButton.alpha = FloatInterpolation.floatAt(deltaTimeSec.coerceIn(0f, 0.05f), playerButton.alpha, if (isMenuExpanded) 1f else 0f, 0f, 0.05f)

        super.onUpdate(clock)
    }
}

class MenuButton(icon: Int, title: String) : UIButton() {

    init {
        style += Style {
            width = 256f.dip
            height = Dimension.WrapContent
            radius = 10.4f.dip
            backgroundColor = backgroundColor.copy(alpha = 0.6f)
            padding = Padding(24f.dip)
        }

        text {
            width = Dimension.FillAvailable
            fontSize = 16f.dip
            spans {
                size(20f.dip) { icon(icon) }
                space(16f.dip)
                +title
            }
        }
    }

}


class CarrouselLinearContainer(private val logo: OsuLogo) : UILinearContainer() {

    var shear: Dimension.Fixed = 32f.dip

    override fun onUpdate(clock: FrameClock) {
        val logoHalfHeight = logo.bounds.height / 2f

        // Before the first layout the logo has no size yet, and dividing by it would leave the buttons at NaN.
        if (logoHalfHeight <= 0f) {
            super.onUpdate(clock)
            return
        }

        val logoCenterY = logo.toSceneSpace(0f, logoHalfHeight).y
        val shear = shear.toPixels()

        forEach { component ->
            val componentMiddleY = component.toSceneSpace(0f, component.bounds.height / 2f).y

            val percentageDistance = (componentMiddleY - logoCenterY) / logoHalfHeight

            component.translationX = (-shear * abs(percentageDistance)).px
        }

        super.onUpdate(clock)
    }

}


class BuildInformationDialog : UIDialog<UIScrollableContainer>(UIScrollableContainer().apply {
    width = Dimension.FillAvailable
    clipping = ClipMode.Bounds
    scrollAxes = Axis.Y
    maxHeight = 0.7f.pct
}) {
    init {
        title = "About"

        innerContent.apply {

            linearContainer {
                width = Dimension.FillAvailable
                orientation = UILinearContainer.Orientation.Vertical
                spacing = 16f.dip
                paddingTop = 16f.dip
                paddingBottom = 16f.dip

                text {
                    text = "osu!droid"
                    anchor = Anchor.TopCenter
                    origin = Anchor.TopCenter
                    style = Style { theme ->
                        fontSettings = theme.typography.h2
                    }
                }

                text {
                    anchor = Anchor.TopCenter
                    origin = Anchor.TopCenter
                    text = "Version: ${BuildConfig.VERSION_NAME}"
                    fontSize = 16f.dip
                }

                text {
                    anchor = Anchor.TopCenter
                    origin = Anchor.TopCenter
                    alignment = Anchor.TopCenter
                    text = "Made by the osu!droid team\nosu! is © peppy 2007-2026"
                }


                fun goToLink(link: String) {
                    hide()
                    val context = GlobalManager.getInstance().mainActivity
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse(link)
                    context.startActivity(intent)
                }

                textButton {
                    anchor = Anchor.TopCenter
                    origin = Anchor.TopCenter
                    scaleOrigin = Anchor.Center
                    variant = UIButton.Variant.Ghost
                    trailingIcon = faSolidIcon(FAIcon.ArrowUpRightFromSquare)
                    text = "Visit official osu! website"
                    onActionUp = {
                        goToLink("https://osu.ppy.sh")
                    }
                }

                textButton {
                    anchor = Anchor.TopCenter
                    origin = Anchor.TopCenter
                    scaleOrigin = Anchor.Center
                    variant = UIButton.Variant.Ghost
                    trailingIcon = faSolidIcon(FAIcon.ArrowUpRightFromSquare)
                    text = "Visit official osu!droid website"
                    onActionUp = {
                        goToLink("https://osudroid.moe")
                    }
                }

                textButton {
                    anchor = Anchor.TopCenter
                    origin = Anchor.TopCenter
                    scaleOrigin = Anchor.Center
                    variant = UIButton.Variant.Ghost
                    trailingIcon = faSolidIcon(FAIcon.ArrowUpRightFromSquare)
                    text = "Join the official Discord server"
                    onActionUp = {
                        goToLink("https://discord.gg/nyD92cE")
                    }
                }

            }
        }

        addButton {
            text = "Changelog"
            onActionUp = {
                hide()

                try {
                    val context = GlobalManager.getInstance().mainActivity
                    val changelogFile = File(context.cacheDir, "changelog.html")

                    context.assets.open("app/changelog.html").use { inputStream ->
                        changelogFile.outputStream().use { outputStream ->
                            val buffer = ByteArray(1024)
                            var length: Int
                            while ((inputStream.read(buffer).also { length = it }) > 0) {
                                outputStream.write(buffer, 0, length)
                            }
                        }
                    }

                    val changelogUri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".fileProvider", changelogFile)

                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.setDataAndType(changelogUri, "text/html")
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                    context.startActivity(intent)
                } catch (e: Exception) {
                    Log.e("MainScene", "Failed to load changelog", e)
                }
            }
        }

        addButton {
            text = "Close"
            onActionUp = {
                hide()
            }
        }
    }
}
