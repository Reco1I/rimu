package com.osudroid.ui.v2.mainmenu

import com.reco1l.verktex.texture.reference
import com.osudroid.game.Gameplay
import com.reco1l.verktex.data.Background
import com.reco1l.verktex.data.CornerRadius
import com.osudroid.beatmaplisting.BeatmapListing
import com.osudroid.beatmaps.LibraryImport
import com.osudroid.BeatmapSelection
import com.reco1l.verktex.settings.Settings
import com.reco1l.verktex.ui.toast.Toasts
import com.reco1l.verktex.ui.text.spans
import com.acivev.ui.addSnowfallWithPeriod
import com.acivev.ui.addFireworksWithPeriod
import ru.nsu.ccfit.zuev.osu.scoring.Replay
import com.reco1l.verktex.ui.dialog.ConfirmDialog
import com.osudroid.ui.v2.SkinSounds
import com.osudroid.*
import com.osudroid.multiplayer.*
import com.osudroid.resources.Res
import com.osudroid.ui.v2.BeatmapBackground
import com.osudroid.ui.v2.LoaderScene
import com.osudroid.ui.v2.skinTexture
import com.osudroid.ui.v2.multi.*
import com.osudroid.ui.v2.settings.SettingsMenu
import com.osudroid.ui.v2.songselect.SongSelect
import com.osudroid.utils.*
import com.reco1l.verktex.Verktex
import kotlin.random.Random
import com.reco1l.verktex.Platform
import com.reco1l.verktex.ui.ParticleSystem
import com.reco1l.verktex.ui.particleSystem
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Axis
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.data.fill
import com.reco1l.verktex.data.px
import com.reco1l.verktex.data.wrap
import com.reco1l.verktex.math.Easing
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.ui.FAIcon
import com.reco1l.verktex.ui.Button
import com.reco1l.verktex.ui.Badge
import com.reco1l.verktex.ui.IconButton
import com.reco1l.verktex.ui.badge
import com.reco1l.verktex.ui.iconButton
import com.osudroid.notifications.Notifications
import com.osudroid.ui.v2.notifications.NotificationCenter
import com.reco1l.verktex.ui.GradientBox
import com.reco1l.verktex.ui.Component
import com.reco1l.verktex.ui.Scene
import com.reco1l.verktex.ui.TextButton
import com.reco1l.verktex.ui.container.Container
import com.reco1l.verktex.ui.container.LinearContainer
import com.reco1l.verktex.ui.container.ScrollableContainer
import com.reco1l.verktex.ui.container.clickableContainer
import com.reco1l.verktex.ui.container.container
import com.reco1l.verktex.ui.container.linearContainer
import com.reco1l.verktex.ui.container.scrollableContainer
import com.reco1l.verktex.ui.dialog.Dialog
import com.reco1l.verktex.ui.clearTransforms
import com.reco1l.verktex.ui.fadeIn
import com.reco1l.verktex.ui.fadeOut
import com.reco1l.verktex.ui.rotateTo
import com.reco1l.verktex.ui.scaleTo
import com.reco1l.verktex.ui.shape.box
import com.reco1l.verktex.ui.spans
import com.reco1l.verktex.ui.text.buildSpannedText
import com.reco1l.verktex.ui.text.faSolidIcon
import com.reco1l.verktex.ui.text.text
import com.reco1l.verktex.ui.textButton
import com.reco1l.verktex.ui.transformSequence
import ru.nsu.ccfit.zuev.osu.*
import ru.nsu.ccfit.zuev.osu.helper.*
import ru.nsu.ccfit.zuev.osu.online.*
import kotlin.math.*
import com.reco1l.verktex.ui.themed
import com.osudroid.utils.currentTimeMillis
import com.reco1l.verktex.input.InputEvent
import com.reco1l.verktex.input.KeyboardEvent


/**
 * The main menu, created by the host once the game has started.
 */
class MainScene : Scene() {

    // Back asks to leave the game, once no component consumed it.
    override fun onInputEvent(event: InputEvent): Boolean {
        if (super.onInputEvent(event)) {
            return true
        }

        if (event is KeyboardEvent && event.isBackPress) {
            showExitDialog()
            return true
        }
        return false
    }

    private lateinit var playerButton: PlayerButton
    private lateinit var musicButton: TextButton

    private lateinit var musicPlayer: MusicPlayer
    private lateinit var notificationsButton: IconButton
    private lateinit var notificationsBadge: Badge
    private lateinit var notificationCenter: NotificationCenter
    private var notificationsRevision = -1
    // The scene's background: the kiai flashes and particles, and the seasonal effects.
    private val backgroundLayer: Container
    private var kiaiParticles: ParticleSystem
    private var leftFlash: GradientBox
    private var rightFlash: GradientBox
    private lateinit var logo: RimuLogo
    private lateinit var menuContainer: Container
    private lateinit var topBar: Container

    private var isExiting = false
    private var hasEntered = false

    // Seconds left emitting stars after a kiai section started, and the emission not done yet.
    private var kiaiParticleTime = 0f
    private var kiaiParticleBacklog = 0f
    private var wasKiai = false


    init {

        backgroundLayer = Container().apply {
            width = Dimension.FillAvailable
            height = Dimension.FillAvailable

            +GradientBox().apply {
                height = Dimension.FillAvailable
                gradientAngle = 0f
                colorStart = Color4.White
                colorEnd = Color4.Transparent
                width = 192f.dip

                alpha = 0f
                leftFlash = this
            }

            +GradientBox().apply {
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

            +GradientBox().apply {
                width = Dimension.FillAvailable
                height = Dimension.FillAvailable
                gradientAngle = 270f
                colorStart = Verktex.theme.palette.accent.shade(0.1f)
                colorEnd = (Verktex.theme.palette.accent.shade(0.1f)).alpha(0.5f)
            }

            // The music's spectrum along the top and bottom edges.
            +LinearVisualizer().apply {
                width = Dimension.FillAvailable
                height = SPECTRUM_HEIGHT
                anchor = Anchor.TopLeft
                origin = Anchor.TopLeft
                fromTop = true
                alpha = SPECTRUM_ALPHA
            }

            +LinearVisualizer().apply {
                width = Dimension.FillAvailable
                height = SPECTRUM_HEIGHT
                anchor = Anchor.BottomLeft
                origin = Anchor.BottomLeft
                alpha = SPECTRUM_ALPHA
            }

            kiaiParticles = particleSystem(maxParticles = 128) {
                width = Dimension.FillAvailable
                height = Dimension.FillAvailable
                textureReference = skinTexture("star")
            }
        }
        background = Background.Component(backgroundLayer)

        linearContainer {
            orientation = LinearContainer.Orientation.Horizontal
            anchor = Anchor.Center
            origin = Anchor.Center

            +RimuLogo().apply {
                anchor = Anchor.CenterLeft
                origin = Anchor.CenterLeft
                scaleOrigin = Anchor.Center
                rotationOrigin = Anchor.Center
                logo = this
            }

            menuContainer = scrollableContainer {
                scrollAxes = Axis.Y
                showVerticalIndicator = false
                anchor = Anchor.CenterLeft
                origin = Anchor.CenterLeft
                padding = Padding(left = 32f.dip)

                +CarrouselLinearContainer(logo).apply {
                    orientation = LinearContainer.Orientation.Vertical
                    spacing = 4.6f.dip
                    padding = Padding(0f.dip, 128f.dip)

                    +menuButton(FAIcon.User, "Solo") {
                        // Archives added since are imported in the background, song select shows
                        // them once they are.
                        LibraryImport.refresh()

                        async {
                            // With nothing to show yet, what's being imported is waited for.
                            if (LibraryManager.getLibrary().isEmpty() && LibraryImport.isRefreshing) {
                                LoaderScene().show()
                                LibraryImport.awaitIdle()
                                Verktex.withTreeLock { Verktex.popScene() }
                            }

                            // Song select opens on the beatmap playing in the menu, the selection.
                            val songSelect = SongSelect()
                            songSelect.loadBeatmaps()

                            if (LibraryManager.getLibrary().isEmpty()) {
                                BeatmapListing.show()
                            } else {
                                Verktex.withTreeLock { Verktex.pushScene(songSelect) }

                                // After the menu was entered again if the loader was shown, which resets it.
                                OsuGame.music.isGaming = true
                            }
                        }
                    }

                    +menuButton(FAIcon.UserGroup, "Multi") action@{
                        if (!OnlineManager.getInstance().isStayOnline && !BuildSettings.MOCK_MULTIPLAYER) {
                            Toasts.showLong(Res.string.multiplayer_not_online.format())
                            return@action
                        }

                        Multiplayer.isMultiplayer = true

                        LibraryImport.refresh()

                        Verktex.pushScene(LobbyScene())
                        OsuGame.music.isGaming = true
                    }

                    +menuButton(FAIcon.TableList, "Browse") {
                        BeatmapListing.show()
                    }

                    +menuButton(FAIcon.Gear, "Settings") {
                        Verktex.pushScene(SettingsMenu())
                    }

                    +menuButton(FAIcon.ArrowRightFromBracket, "Exit") { showExitDialog() }

                }

            }

        }

        // Top bar
        topBar = container {
            width = fill()
            padding = Padding(16f.dip)

            +PlayerButton().apply {
                anchor = Anchor.TopLeft
                origin = Anchor.TopLeft
                scaleOrigin = Anchor.Center
                playerButton = this
            }

            container {
                anchor = Anchor.TopRight
                origin = Anchor.TopRight
                // Left of the notifications button.
                padding = Padding(right = 52f.dip)

                musicButton = textButton {
                    scaleOrigin = Anchor.Center
                    leadingIcon = faSolidIcon(FAIcon.Music)

                    onActionUp = { musicPlayer.show() }
                }
                musicPlayer = MusicPlayer(musicButton)
            }

            container {
                anchor = Anchor.TopRight
                origin = Anchor.TopRight

                notificationsButton = iconButton {
                    icon = FAIcon.Bell
                    scaleOrigin = Anchor.Center
                    onActionUp = { notificationCenter.show() }
                }
                notificationCenter = NotificationCenter(notificationsButton)

                notificationsBadge = badge {
                    anchor = Anchor.TopRight
                    origin = Anchor.Center
                    variant = Badge.Variant.Solid
                    density = Badge.Density.Small
                    isVisible = false
                }
            }
        }

        RythimManager.addOnBeatChangeListener(this) {
            if (!RythimManager.isKiai) {
                if (RythimManager.beatIndex == 0) {
                    flash(leftFlash, 0.35f)
                    flash(rightFlash, 0.35f)
                }
            } else {
                // +1 Because 0 is also even and we don't want double flashes on the first beat
                if ((RythimManager.beatIndex + 1) % 2 == 0) {
                    flash(leftFlash, 0.4f)
                } else {
                    flash(rightFlash, 0.4f)
                }
            }

        }
    }


    /**
     * Asks for confirmation before leaving the game.
     */
    fun showExitDialog() {
        if (isExiting) {
            return
        }

        ConfirmDialog().apply {
            title = "Exit"
            text = Res.string.dialog_exit_message
            onConfirm = { exit() }
        }.show()
    }

    /**
     * Plays the exit animation and closes the game.
     */
    fun exit() {
        if (isExiting) {
            return
        }

        // Headless (e.g. in the UI test browser) there's no game to leave, the menu stays usable.
        if (OsuGame.isHeadless) {
            Toasts.show("Would exit the game")
            return
        }

        isExiting = true
        menuContainer.fadeOut(0.3f, Easing.OutQuint)

        OsuGame.music.stop()
        SkinSounds["seeya"].play()

        BeatmapBackground.fadeBrightness(0f, 0.4f)

        logo.playEffects = false
        logo.scaleTo(0.5f, 3f)
        logo.rotateTo(-15f, 3f)

        leftFlash.removeSelf()
        rightFlash.removeSelf()

        box {
            width = Dimension.FillAvailable
            height = Dimension.FillAvailable
            color = Color4.Black
            alpha = 0f

            fadeIn(3f).after {
                Platform.exit()
            }
        }
    }


    override fun onEnter() {
        super.onEnter()


        // Songs end on their own in the menu instead of stopping as in gameplay.
        OsuGame.music.isGaming = false

        BeatmapSelection.addListener(this) { updateMusicButton() }

        // The background follows the selection on its own, slower in the menu.
        BeatmapBackground.crossfadeDuration = BACKGROUND_FADE_DURATION
        BeatmapBackground.fadeBrightness(1f)

        if (!hasEntered) {
            hasEntered = true

            addSnowfallWithPeriod(backgroundLayer)
            addFireworksWithPeriod(backgroundLayer)

            // Only logs in when staying online is enabled.
            OnlineScoring.getInstance().login()
        }

        val music = OsuGame.music

        // A song left playing (e.g. by song select) goes on, it's the selection's.
        if (!music.isPlaying) {
            // Null with an empty library, the menu stays silent.
            val beatmap = BeatmapSelection.beatmap ?: LibraryManager.getLibrary()
                .randomOrNull()?.beatmaps?.randomOrNull()
            beatmap?.let { music.play(it) }
        }

        updateMusicButton()

        // Entrance: the top bar slides down, the logo and then the menu rise.
        topBar.apply {
            clearTransforms()
            alpha = 0f
            translationY = (-20f).dip
            transformSequence {
                fadeIn(0.4f, Easing.OutCubic)
                moveToY(0f, 0.55f, Easing.OutCubic)
            }
        }
        listOf(logo, menuContainer).forEachIndexed { index, part ->
            part.clearTransforms()
            part.alpha = 0f
            part.translationY = 32f.dip
            part.transformSequence(0.05f + index * 0.05f) {
                fadeIn(0.4f, Easing.OutCubic)
                moveToY(0f, 0.55f, Easing.OutCubic)
            }
        }
    }

    override fun onLeave() {
        super.onLeave()
        BeatmapSelection.removeListener(this)
        BeatmapBackground.crossfadeDuration = BeatmapBackground.CROSSFADE_DURATION
    }

    private fun updateMusicButton() {
        val beatmap = BeatmapSelection.beatmap
        musicButton.text =
            if (beatmap != null) "${beatmap.titleText} - ${beatmap.artistText}" else ""
    }

    /**
     * Bursts stars from the bottom corners for a while when a kiai section starts, as the old menu did.
     */
    private fun updateKiaiParticles(deltaTimeSec: Float) {
        val isKiai = OsuGame.music.isPlaying && RythimManager.isKiai
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

    // A menu button that clicks before running its action.
    private fun menuButton(icon: Int, title: String, action: () -> Unit) =
        MenuButton(icon, title).apply {
            onActionUp = {
                SkinSounds["menuhit"].play()
                action()
            }
        }


    override fun onUpdate(clock: FrameClock) {
        val deltaTimeSec = clock.elapsedFrameTime

        RythimManager.onUpdate(deltaTimeSec)

        updateKiaiParticles(deltaTimeSec)
        updateNotificationsBadge()

        super.onUpdate(clock)
    }

    // The tasks running, or else the notifications not seen yet.
    private fun updateNotificationsBadge() {
        val revision = Notifications.revision
        if (revision == notificationsRevision) {
            return
        }
        notificationsRevision = revision

        val active = Notifications.activeCount
        val count = if (active > 0) active else Notifications.unreadCount

        notificationsBadge.isVisible = count > 0
        notificationsBadge.text = if (count > 9) "9+" else count.toString()
        notificationsButton.icon = if (active > 0) FAIcon.Spinner else FAIcon.Bell
    }

    /**
     * Called when the library was refreshed: the menu plays a song if it was silent, e.g. as the
     * game starts for the first time with a full beatmaps folder.
     */
    fun onLibraryChanged() {
        if (isExiting || OsuGame.music.isPlaying) {
            return
        }

        val beatmap = BeatmapSelection.beatmap ?: LibraryManager.getLibrary().randomOrNull()?.beatmaps?.randomOrNull()
        beatmap?.let { OsuGame.music.play(it) }
        updateMusicButton()
    }

    // On a beat, a flash lights up and fades out over the length of the beat.
    private fun flash(flash: GradientBox, alpha: Float) {
        val beatLengthSeconds =
            RythimManager.beatLength.toFloat() / 1000f * (if (RythimManager.isKiai) 1f else RythimManager.beatSignature.toFloat())

        flash.alpha = alpha
        flash.fadeOut(beatLengthSeconds * 2f, Easing.OutQuad)
    }


    companion object {

        private val SPECTRUM_HEIGHT = 96f.dip
        private const val SPECTRUM_ALPHA = 0.4f

        private const val BACKGROUND_FADE_DURATION = 1f
        private const val KIAI_PARTICLE_DURATION = 2f

        // Stars per second from each corner.
        private const val KIAI_PARTICLE_RATE = 40f

        /**
         * Shows the results screen of a replay file.
         */
        @JvmStatic
        fun watchReplay(replayFile: String) {
            val replay = Replay()

            if (!replay.load(replayFile, false) || replay.replayVersion < 3) {
                return
            }

            val beatmap = LibraryManager.findBeatmapByMD5(replay.md5) ?: return

            // Which selects it too.
            OsuGame.music.play(beatmap)

            val stat = replay.stat!!
            stat.migrateLegacyMods(beatmap.getBeatmapDifficulty())
            stat.calculateModScoreMultiplier(beatmap.getBeatmapDifficulty())

            // The replay is played by this game when asked from the results.
            val results = Gameplay().results
            results.load(stat, null, OsuGame.music, replayFile, null, beatmap)
            Verktex.pushScene(results.scene!!)
        }
    }
}

private fun Random.nextFloat(from: Float, until: Float) = from + nextFloat() * (until - from)

class MenuButton(icon: Int, title: String) : TextButton() {

    init {
        width = 256f.dip
        height = wrap()
        radius = CornerRadius(10.4f.dip)
        background = Background.Color(themed { palette.buttonBackgroundSurface.alpha(0.6f) })
        density = Density.Large
        padding = Padding(24f.dip)

        textComponent.alignment = Anchor.CenterLeft
        spannedText = buildSpannedText {
            size(20f.dip) { icon(icon) }
            space(16f.dip)
            +title
        }
    }

}


/**
 * A column whose items lean away from the vertical center of [logo] the farther they are from it,
 * [shear] at [shearDistance] away.
 */
class CarrouselLinearContainer(private val logo: Component) : LinearContainer() {

    var shear: Dimension.Fixed = 32f.dip

    var shearDistance: Dimension.Fixed = 128f.dip

    override fun onUpdate(clock: FrameClock) {
        val logoHalfHeight = logo.bounds.height / 2f
        val shearDistance = shearDistance.toPixels()

        // Before the first layout the logo has no size yet.
        if (logoHalfHeight <= 0f || shearDistance <= 0f) {
            super.onUpdate(clock)
            return
        }

        val logoCenterY = logo.toSceneSpace(0f, logoHalfHeight).y
        val shear = shear.toPixels()

        forEach { component ->
            val componentMiddleY = component.toSceneSpace(0f, component.bounds.height / 2f).y

            val percentageDistance = (componentMiddleY - logoCenterY) / shearDistance

            component.translationX = (-shear * abs(percentageDistance)).px
        }

        super.onUpdate(clock)
    }

}