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
import kotlin.random.Random
import kotlinx.coroutines.Job
import com.reco1l.verktex.graphics.GraphicsTexture
import com.reco1l.verktex.Platform
import com.reco1l.verktex.ui.UIParticleSystem
import com.reco1l.verktex.ui.particleSystem
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
import com.reco1l.verktex.ui.UICrossfadeSprite
import com.reco1l.verktex.ui.crossfadeSprite
import com.reco1l.verktex.ui.UITextButton
import com.reco1l.verktex.ui.container.UIContainer
import com.reco1l.verktex.ui.container.UILinearContainer
import com.reco1l.verktex.ui.container.UIScrollableContainer
import com.reco1l.verktex.ui.container.clickableContainer
import com.reco1l.verktex.ui.container.container
import com.reco1l.verktex.ui.container.linearContainer
import com.reco1l.verktex.ui.container.scrollableContainer
import com.reco1l.verktex.ui.dialog.UIDialog
import com.reco1l.verktex.ui.fadeIn
import com.reco1l.verktex.ui.fadeOut
import com.reco1l.verktex.ui.rotateTo
import com.reco1l.verktex.ui.scaleTo
import com.reco1l.verktex.ui.shape.box
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
import com.reco1l.verktex.ui.themed


object MainScene : UIScene() {

    private lateinit var playerButton: PlayerButton
    private lateinit var musicButton: UITextButton
    // Animated instead of the button, which drives its own alpha.
    private lateinit var musicButtonHolder: UIContainer
    private lateinit var musicPlayer: MusicPlayer
    private lateinit var kiaiParticles: UIParticleSystem
    private lateinit var backgroundSprite: UICrossfadeSprite
    private lateinit var leftFlash: UIGradientBox
    private lateinit var rightFlash: UIGradientBox
    private lateinit var logo: OsuLogo
    private lateinit var menuContainer: UIContainer
    private lateinit var menuGradientBox: UIGradientBox

    private var isMenuExpanded = false
    private var isExiting = false
    private var hasEntered = false

    // Seconds the menu has been expanded without being used.
    private var menuIdleTime = 0f

    private var lastMusicChange = System.currentTimeMillis()

    // The texture key the next background is loaded under. It alternates as the crossfade sprite
    // does, so the background still shown while fading isn't released.
    private var backgroundKeyIndex = 0
    private var backgroundLoadJob: Job? = null

    // Seconds left emitting stars after a kiai section started, and the emission not done yet.
    private var kiaiParticleTime = 0f
    private var kiaiParticleBacklog = 0f
    private var wasKiai = false

    // Seconds the expanded menu waits without being used before folding back.
    private const val MENU_IDLE_TIMEOUT = 10f

    private const val BACKGROUND_FADE_DURATION = 1.5f
    private const val KIAI_PARTICLE_DURATION = 2f
    // Stars per second from each corner.
    private const val KIAI_PARTICLE_RATE = 40f

    private var menuWidth = 0f
    private var menuContentWidth = 0f
    private var musicButtonTranslation = 0f
    private var playerButtonTranslation = 0f


    init {

        background = UIContainer().apply {
            width = Dimension.FillAvailable
            height = Dimension.FillAvailable

            backgroundSprite = crossfadeSprite {
                width = Dimension.FillAvailable
                height = Dimension.FillAvailable
                scaleType = UISprite.ScaleType.Crop
                crossfadeDuration = BACKGROUND_FADE_DURATION
                crossfadeTo(SkinTextures["menu-background"], 0f)
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
                colorStart = Verktex.theme.palette.accent.shade(0.1f)
                colorEnd = (Verktex.theme.palette.accent.shade(0.1f)).alpha(0.5f)
                menuGradientBox = this
            }

            kiaiParticles = particleSystem(maxParticles = 128) {
                width = Dimension.FillAvailable
                height = Dimension.FillAvailable
                texture = SkinTextures["star"]
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
                    SkinSounds["menuhit"].play()
                    isMenuExpanded = !isMenuExpanded
                    onMenuInteraction()
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

                    +menuButton(FAIcon.User, "Solo") {
                        async {
                            LoaderScene().show()

                            GlobalManager.getInstance().mainActivity?.checkNewSkins()
                            GlobalManager.getInstance().mainActivity?.loadBeatmapLibrary()

                            // Song select opens on the beatmap playing in the menu.
                            MusicManager.currentBeatmap?.let { GlobalManager.getInstance().selectedBeatmap = it }
                            SongSelect.loadBeatmaps()

                            if (LibraryManager.getLibrary().isEmpty()) {
                                Verktex.withTreeLock { Verktex.popScene() }
                                BeatmapListing().show()
                            } else {
                                Verktex.withTreeLock {
                                    Verktex.popScene()
                                    Verktex.pushScene(SongSelect)
                                }

                                // After the menu was entered again by removing the loader, which resets it.
                                GlobalManager.getInstance().songService.isGaming = true
                            }
                        }
                    }

                    +menuButton(FAIcon.UserGroup, "Multi") action@{
                        if (!OnlineManager.getInstance().isStayOnline && !BuildSettings.MOCK_MULTIPLAYER) {
                            ToastLogger.showText(StringTable.format(R.string.multiplayer_not_online), true)
                            return@action
                        }

                        Multiplayer.isMultiplayer = true

                        async {
                            LoaderScene().show()

                            GlobalManager.getInstance().mainActivity?.checkNewSkins()
                            GlobalManager.getInstance().mainActivity?.loadBeatmapLibrary()

                            SongSelect.loadBeatmaps()

                            Verktex.withTreeLock {
                                Verktex.popScene()
                                Verktex.pushScene(LobbyScene())
                            }

                            // After the menu was entered again by removing the loader, which resets it.
                            GlobalManager.getInstance().songService.isGaming = true
                        }
                    }

                    +menuButton(FAIcon.TableList, "Browse") {
                        mainThread { BeatmapListing().show() }
                    }

                    +menuButton(FAIcon.Gear, "Settings") {
                        Verktex.pushScene(SettingsMenu())
                    }

                    +menuButton(FAIcon.ArrowRightFromBracket, "Exit") { showExitDialog() }

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

            musicButtonHolder = container {
                anchor = Anchor.TopRight
                origin = Anchor.TopRight
                alpha = 0f

                musicButton = textButton {
                    scaleOrigin = Anchor.Center
                    leadingIcon = faSolidIcon(FAIcon.Music)

                    onActionUp = {
                        if (musicButtonHolder.alpha > 0f) {
                            musicPlayer.show()
                        }
                    }
                }
                musicPlayer = MusicPlayer(musicButton)
            }
        }

        clickableContainer {
            anchor = Anchor.BottomLeft
            origin = Anchor.BottomLeft
            paddingBottom = 16f.dip

            text {
                text = "osu!droid ${BuildConfig.VERSION_NAME}"
                backgroundColor = Color4.Black.alpha(0.5f)
                padding = Padding(8f.dip, 4f.dip)
                radius = themed { radius.md }
                fontSize = themed { typography.smallBody.size }
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
            changeBackground(beatmap)

            updateMusicButton()
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
            text = GlobalManager.getInstance().context.getString(R.string.dialog_exit_message)
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

        val stat = replay.stat!!
        stat.migrateLegacyMods(beatmap.getBeatmapDifficulty())
        stat.calculateModScoreMultiplier(beatmap.getBeatmapDifficulty())

        global.scoring.load(stat, null, global.songService, replayFile, null, beatmap)
        Verktex.pushScene(global.scoring.scene!!)
    }

    /**
     * Plays the exit animation and closes the game.
     */
    fun exit() {
        if (isExiting) {
            return
        }

        // Headless (e.g. in the UI test browser) there's no game to leave, the menu stays usable.
        if (GlobalManager.getInstance().isHeadless) {
            ToastLogger.showText("Would exit the game", false)
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


    override fun onEnter() {
        super.onEnter()

        val global = GlobalManager.getInstance()

        // Songs end on their own in the menu instead of stopping as in gameplay.
        global.songService.isGaming = false

        if (!hasEntered) {
            hasEntered = true

            (background as? UIContainer)?.let {
                addSnowfallWithPeriod(it, global.context)
                addFireworksWithPeriod(it, global.context)
            }

            // Only logs in when staying online is enabled.
            OnlineScoring.getInstance().login()
        }

        val selectedBeatmap = global.selectedBeatmap

        if (MusicManager.isPlaying && selectedBeatmap != null && selectedBeatmap != MusicManager.currentBeatmap) {
            // Song select left its beatmap playing, the menu follows it from where it is.
            MusicManager.currentBeatmap = selectedBeatmap
            MusicManager.load(reloadAudio = false)
        } else if (!MusicManager.isPlaying) {
            if (MusicManager.currentBeatmap == null) {
                // Null with an empty library, the menu stays silent.
                MusicManager.currentBeatmap = LibraryManager.getLibrary().randomOrNull()?.beatmaps?.randomOrNull()
            }

            if (MusicManager.currentBeatmap != null) {
                MusicManager.load()
                MusicManager.play()
            }
        }

        updateMusicButton()
    }

    private fun updateMusicButton() {
        val beatmap = MusicManager.currentBeatmap
        musicButton.text = if (beatmap != null) "${beatmap.titleText} - ${beatmap.artistText}" else ""
        lastMusicChange = System.currentTimeMillis()
    }

    /**
     * Fades to the background of [beatmap], or the skin's one without it or with safe backgrounds.
     * It is decoded off the update thread, the old menu froze while loading it.
     */
    private fun changeBackground(beatmap: BeatmapInfo?) {
        backgroundLoadJob?.cancel()

        val path = beatmap?.backgroundPath
        if (path == null || Config.isSafeBeatmapBg()) {
            fadeToBackground(SkinTextures["menu-background"])
            return
        }

        backgroundLoadJob = async {
            val source = Platform.provider.createTextureSource(path, true)

            Verktex.scheduleOnDraw {
                val region = source?.let { Textures.load("::background-$backgroundKeyIndex", it) }
                fadeToBackground(region ?: SkinTextures["menu-background"])
            }
        }
    }

    private fun fadeToBackground(region: GraphicsTexture.Region?) {
        backgroundSprite.crossfadeTo(region)
        backgroundKeyIndex = 1 - backgroundKeyIndex
    }

    /**
     * Bursts stars from the bottom corners for a while when a kiai section starts, as the old menu did.
     */
    private fun updateKiaiParticles(deltaTimeSec: Float) {
        val isKiai = MusicManager.isPlaying && RythimManager.isKiai
        if (isKiai && !wasKiai) {
            kiaiParticleTime = KIAI_PARTICLE_DURATION
        }
        wasKiai = isKiai

        if (kiaiParticleTime <= 0f) {
            return
        }
        kiaiParticleTime -= deltaTimeSec

        // The old menu was laid out for a 720 pixels tall screen.
        val scale = kiaiParticles.bounds.height / 720f
        val y = kiaiParticles.bounds.height * 0.75f

        kiaiParticleBacklog += deltaTimeSec * KIAI_PARTICLE_RATE

        while (kiaiParticleBacklog >= 1f) {
            kiaiParticleBacklog -= 1f

            for (side in 0..1) {
                val direction = if (side == 0) 1f else -1f

                kiaiParticles.emit(
                    x = if (side == 0) -40f * scale else kiaiParticles.bounds.width,
                    y = y,
                    lifetime = 1f,
                    velocityX = direction * Random.nextFloat(150f, 430f) * scale,
                    velocityY = -Random.nextFloat(480f, 520f) * scale,
                    accelerationX = direction * Random.nextFloat(10f, 30f) * scale,
                    accelerationY = 30f * scale,
                    startScale = 0.5f,
                    endScale = 2f,
                    startAlpha = 1f,
                    endAlpha = 0f,
                    rotation = Random.nextFloat(0f, 360f)
                )
            }
        }
    }

    // Resets the time until the expanded menu folds back.
    private fun onMenuInteraction() {
        menuIdleTime = 0f
    }

    // A menu button that clicks and keeps the menu expanded before running its action.
    private fun menuButton(icon: Int, title: String, action: () -> Unit) = MenuButton(icon, title).apply {
        onActionUp = {
            SkinSounds["menuhit"].play()
            onMenuInteraction()
            action()
        }
    }


    override fun onUpdate(clock: FrameClock) {
        val deltaTimeSec = clock.elapsedFrameTime

        MusicManager.onUpdate(deltaTimeSec)

        // The expanded menu folds back after a while without being used, as the old menu did.
        if (isMenuExpanded) {
            menuIdleTime += deltaTimeSec
            if (menuIdleTime >= MENU_IDLE_TIMEOUT) {
                isMenuExpanded = false
            }
        }

        logo.scale = FloatInterpolation.floatAt(deltaTimeSec.coerceIn(0f, 0.1f), logo.scaleX, if (isMenuExpanded) 1f else 1.3f, 0f, 0.1f)

        // Music button
        val mightShowMusicButton = isMenuExpanded || musicPlayer.isShown || System.currentTimeMillis() - lastMusicChange < 3000

        musicButtonTranslation = FloatInterpolation.floatAt(deltaTimeSec.coerceIn(0f, 0.05f), musicButtonTranslation, if (mightShowMusicButton) 0f else 32f.dip.toPixels(), 0f, 0.05f)
        musicButtonHolder.translationX = musicButtonTranslation.px
        musicButtonHolder.alpha = FloatInterpolation.floatAt(deltaTimeSec.coerceIn(0f, 0.05f), musicButtonHolder.alpha, if (mightShowMusicButton) 1f else 0f, 0f, 0.05f)

        updateKiaiParticles(deltaTimeSec)

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

private fun Random.nextFloat(from: Float, until: Float) = from + nextFloat() * (until - from)

class MenuButton(icon: Int, title: String) : UIButton() {

    init {
        width = 256f.dip
        height = Dimension.WrapContent
        radius = 10.4f.dip
        backgroundColor = themed { palette.buttonBackgroundSurface.alpha(0.6f) }
        padding = Padding(24f.dip)

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
                    fontSettings = themed { typography.h2 }
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
                    val context = GlobalManager.getInstance().context
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
                    val context = GlobalManager.getInstance().context
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
