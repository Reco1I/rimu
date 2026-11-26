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
import com.reco1l.verktex.data.LayoutConstraints
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
import com.reco1l.verktex.ui.animatableFloat
import com.reco1l.verktex.ui.animate
import com.reco1l.verktex.ui.fadeTo
import com.reco1l.verktex.ui.moveToX
import com.reco1l.verktex.ui.transitionSlot
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
import com.reco1l.verktex.ui.Scene
import com.reco1l.verktex.ui.Sprite
import com.reco1l.verktex.ui.TextButton
import com.reco1l.verktex.ui.container.Container
import com.reco1l.verktex.ui.container.LinearContainer
import com.reco1l.verktex.ui.container.ScrollableContainer
import com.reco1l.verktex.ui.container.clickableContainer
import com.reco1l.verktex.ui.container.container
import com.reco1l.verktex.ui.container.linearContainer
import com.reco1l.verktex.ui.container.scrollableContainer
import com.reco1l.verktex.ui.dialog.Dialog
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

    // Animated instead of the button, which drives its own alpha.
    private lateinit var musicButtonHolder: Container
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
    private lateinit var logo: OsuLogo
    private lateinit var menuContainer: Container
    private var menuGradientBox: GradientBox

    private var isMenuExpanded = false
        set(value) {
            if (field != value) {
                field = value
                animateMenu(value)
            }
        }
    private var isExiting = false
    private var hasEntered = false

    // Seconds the menu has been expanded without being used.
    private var menuIdleTime = 0f

    private var lastMusicChange = currentTimeMillis()

    // Seconds left emitting stars after a kiai section started, and the emission not done yet.
    private var kiaiParticleTime = 0f
    private var kiaiParticleBacklog = 0f
    private var wasKiai = false

    private var menuWidth = 0f
    private var menuContentWidth = 0f
    private var isMusicButtonShown = false

    private val logoTransition by lazy { logo.transitionSlot() }
    private val menuTransition by lazy { menuContainer.transitionSlot() }
    private val menuGradientTransition by lazy { menuGradientBox.transitionSlot() }
    private val playerButtonTransition by lazy { playerButton.transitionSlot() }
    private val musicButtonTransition by lazy { musicButtonHolder.transitionSlot() }

    private val menuWidthProperty = animatableFloat({ menuWidth }, {
        menuWidth = it
        menuContainer.width = it.px
    })


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
                alpha = 0f
                colorStart = Verktex.theme.palette.accent.shade(0.1f)
                colorEnd = (Verktex.theme.palette.accent.shade(0.1f)).alpha(0.5f)
                menuGradientBox = this
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

                // Bigger while the menu is folded, see animateMenu.
                scale = 1.3f
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
        container {
            width = fill()
            padding = Padding(16f.dip)

            +PlayerButton().apply {
                anchor = Anchor.TopLeft
                origin = Anchor.TopLeft
                scaleOrigin = Anchor.Center
                // Hidden with the menu, see animateMenu.
                alpha = 0f
                translationX = (-32f).dip
                playerButton = this
            }

            musicButtonHolder = container {
                anchor = Anchor.TopRight
                origin = Anchor.TopRight
                alpha = 0f
                // Left of the notifications button.
                padding = Padding(right = 52f.dip)

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
        isMenuExpanded = false

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
        lastMusicChange = currentTimeMillis()
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

    // Resets the time until the expanded menu folds back.
    private fun onMenuInteraction() {
        menuIdleTime = 0f
    }

    // A menu button that clicks and keeps the menu expanded before running its action.
    private fun menuButton(icon: Int, title: String, action: () -> Unit) =
        MenuButton(icon, title).apply {
            onActionUp = {
                SkinSounds["menuhit"].play()
                onMenuInteraction()
                action()
            }
        }


    override fun onUpdate(clock: FrameClock) {
        val deltaTimeSec = clock.elapsedFrameTime

        RythimManager.onUpdate(deltaTimeSec)

        // The expanded menu folds back after a while without being used, as the old menu did.
        if (isMenuExpanded) {
            menuIdleTime += deltaTimeSec
            if (menuIdleTime >= MENU_IDLE_TIMEOUT) {
                isMenuExpanded = false
            }
        }

        // Shown while it might be used: checked every update as it also depends on the time.
        val mightShowMusicButton =
            isMenuExpanded || musicPlayer.isShown || currentTimeMillis() - lastMusicChange < 3000

        if (mightShowMusicButton != isMusicButtonShown) {
            isMusicButtonShown = mightShowMusicButton
            musicButtonTransition.run {
                moveToX(if (mightShowMusicButton) 0f else 32f.dip.toPixels(), BUTTON_SLIDE_DURATION, Easing.OutQuint)
                fadeTo(if (mightShowMusicButton) 1f else 0f, BUTTON_SLIDE_DURATION, Easing.OutQuint)
            }
        }

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

    private fun animateMenu(expanded: Boolean) {
        // The content is measured by itself, inside the container it would be limited to the width being animated.
        if (menuContentWidth <= 0f && menuContainer.childCount > 0) {
            val unbounded = LayoutConstraints(0f, Float.POSITIVE_INFINITY, 0f, Float.POSITIVE_INFINITY)
            menuContentWidth = menuContainer[0].measure(unbounded).x + menuContainer.paddingLeft.toPixels()
        }

        logoTransition.run { scaleTo(if (expanded) 1f else 1.3f, LOGO_SCALE_DURATION, Easing.OutQuint) }

        menuTransition.run {
            animate(menuWidthProperty, if (expanded) menuContentWidth else 0f, MENU_DURATION, Easing.OutQuint)
            fadeTo(if (expanded) 1f else 0f, MENU_DURATION, Easing.OutQuint)
        }
        menuGradientTransition.run { fadeTo(if (expanded) 1f else 0f, MENU_GRADIENT_DURATION, Easing.OutQuint) }

        playerButtonTransition.run {
            moveToX(if (expanded) 0f else (-32f).dip.toPixels(), BUTTON_SLIDE_DURATION, Easing.OutQuint)
            fadeTo(if (expanded) 1f else 0f, BUTTON_SLIDE_DURATION, Easing.OutQuint)
        }
    }

    // On a beat, a flash lights up and fades out over the length of the beat.
    private fun flash(flash: GradientBox, alpha: Float) {
        val beatLengthSeconds =
            RythimManager.beatLength.toFloat() / 1000f * (if (RythimManager.isKiai) 1f else RythimManager.beatSignature.toFloat())

        flash.alpha = alpha
        flash.fadeOut(beatLengthSeconds * 2f, Easing.OutQuad)
    }


    companion object {

        // Seconds the expanded menu waits without being used before folding back.
        private const val MENU_IDLE_TIMEOUT = 10f

        private const val LOGO_SCALE_DURATION = 0.4f
        private const val MENU_DURATION = 0.3f
        private const val MENU_GRADIENT_DURATION = 0.5f
        private const val BUTTON_SLIDE_DURATION = 0.25f

        private const val BACKGROUND_FADE_DURATION = 1.5f
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


class CarrouselLinearContainer(private val logo: OsuLogo) : LinearContainer() {

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