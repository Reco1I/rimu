package com.osudroid.ui.v2.mainmenu

import android.content.*
import android.net.*
import android.util.*
import androidx.core.content.*
import com.edlplan.framework.easing.*
import com.osudroid.*
import com.osudroid.beatmaplisting.*
import com.osudroid.multiplayer.*
import com.osudroid.resources.R
import com.osudroid.ui.v1.*
import com.osudroid.ui.v2.LoaderScene
import com.osudroid.ui.v2.multi.*
import com.osudroid.utils.*
import com.reco1l.verktex.*
import com.reco1l.verktex.component.*
import com.reco1l.verktex.ui.container.*
import com.reco1l.verktex.shape.*
import com.reco1l.verktex.ui.sprite.*
import com.reco1l.verktex.ui.text.*
import com.reco1l.verktex.theme.*
import com.reco1l.verktex.theme.Colors
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.ui.*
import com.reco1l.verktex.ui.container.Orientation
import com.reco1l.verktex.ui.container.UIContainer
import com.reco1l.verktex.ui.container.UILinearContainer
import com.reco1l.verktex.ui.container.UIScrollableContainer
import com.reco1l.verktex.ui.dialog.UIDialog
import com.reco1l.verktex.ui.dialog.UIMessageDialog
import com.reco1l.verktex.ui.sprite.ScaleType
import com.reco1l.verktex.ui.UISprite
import com.reco1l.framework.*
import com.reco1l.framework.math.*
import com.reco1l.verktex.data.Vec4
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Axis
import com.reco1l.verktex.math.FloatInterpolation
import com.reco1l.verktex.texture.Textures
import org.anddev.andengine.engine.camera.*
import ru.nsu.ccfit.zuev.osu.*
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.helper.*
import ru.nsu.ccfit.zuev.osu.menu.*
import ru.nsu.ccfit.zuev.osu.online.*
import ru.nsu.ccfit.zuev.osuplus.BuildConfig
import java.io.*
import javax.microedition.khronos.opengles.*
import kotlin.math.*


object MainScene : UIScene() {

    private lateinit var playerButton: PlayerButton
    private lateinit var musicButton: UITextButton
    private lateinit var leftFlash: UIGradientBox
    private lateinit var rightFlash: UIGradientBox
    private lateinit var background: UISprite
    private lateinit var logo: OsuLogo
    private lateinit var menuContainer: UIContainer
    private lateinit var menuGradientBox: UIGradientBox

    private var isMenuExpanded = false

    private var lastMusicChange = System.currentTimeMillis()


    init {

        container {
            width = Dimension.FillAvailable
            height = Dimension.FillAvailable

            background = sprite {
                width = Dimension.FillAvailable
                height = Dimension.FillAvailable
                scaleType = ScaleType.Crop
                textureRegion = Textures["menu-background"]
            }

            +UIGradientBox().apply {
                height = Dimension.FillAvailable
                gradientAngle = 0f
                colorStart = Colors.White
                colorEnd = Colors.Transparent
                style = {
                    width = 12f.rem
                }

                alpha = 0f
                leftFlash = this
            }

            +UIGradientBox().apply {
                height = Dimension.FillAvailable
                anchor = Anchor.TopRight
                origin = Anchor.TopRight
                gradientAngle = 180f
                colorStart = Colors.White
                colorEnd = Colors.Transparent
                style = {
                    width = 12f.rem
                }

                alpha = 0f
                rightFlash = this
            }

            +UIGradientBox().apply {
                width = Dimension.FillAvailable
                height = Dimension.FillAvailable
                gradientAngle = 270f
                alpha = 0f
                style = {
                    colorStart = it.accentColor * 0.1f
                    colorEnd = (it.accentColor * 0.1f).copy(alpha = 0.5f)
                }
                menuGradientBox = this
            }
        }

        linearContainer {
            orientation = Orientation.Horizontal
            anchor = Anchor.Center
            origin = Anchor.Center

            +OsuLogo().apply {
                anchor = Anchor.CenterLeft
                origin = Anchor.CenterLeft
                scaleOrigin = Anchor.Center
                rotationOrigin = Anchor.Center
                style = {
                    width = 16f.rem
                    height = 16f.rem
                }

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
                style = {
                    padding = Vec4(8f.srem, 0f, 0f, 0f)
                    maxHeight = 1f.vh
                }
                width = 0f
                alpha = 0f

                +CarrouselLinearContainer(logo).apply {
                    orientation = Orientation.Vertical
                    style = {
                        spacing = 1.15f.srem
                        padding = Vec4(0f, 8f.rem)
                    }

                    +MenuButton(FAIcon.User, "Solo").apply {
                        onActionUp = {
                            async {
                                LoaderScene().show()

                                GlobalManager.getInstance().mainActivity.checkNewSkins()
                                GlobalManager.getInstance().mainActivity.loadBeatmapLibrary()

                                if (LibraryManager.getLibrary().isEmpty()) {
                                    Engine.current.scene = this@MainScene
                                    BeatmapListing().show()
                                } else {
                                    GlobalManager.getInstance().songService.isGaming = true
                                    GlobalManager.getInstance().songMenu.reload()
                                    GlobalManager.getInstance().songMenu.show()
                                    GlobalManager.getInstance().songMenu.select()
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

                                GlobalManager.getInstance().songMenu.reload()
                                GlobalManager.getInstance().engine.scene = LobbyScene()
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
                            mainThread { SettingsFragment().show() }
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
            style = {
                padding = Engine.current.safeArea.copy(y = 2f.srem, w = 2f.srem)
            }

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
                leadingIcon = UIIcon(FAIcon.Music)

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
            style = {
                paddingLeft = Engine.current.safeArea.x
                paddingBottom = 4f.srem
            }

            text {
                text = "osu!droid ${BuildConfig.VERSION_NAME}"
                style = {
                    backgroundColor = Colors.Black.copy(alpha = 0.5f)
                    padding = Vec4(2f.srem, 1f.srem)
                    radius = Radius.MD
                    fontSize = FontSize.SM
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
            val textureRegion = if (beatmap != null) Textures.getInstance().loadBackground(beatmap.backgroundPath) else null

            if (textureRegion != null && !Config.isSafeBeatmapBg()) {
                background.textureRegion = textureRegion
            } else {
                background.textureRegion = Textures.getInstance().getTexture("menu-background")
            }
        }
    }


    private fun exit() {
        isMenuExpanded = false

        MusicManager.stop()
        Textures.getInstance().getSound("seeya")?.play()

        background.fadeOut(0.4f)

        logo.playEffects = false
        logo.scaleTo(0.5f, 3f)
        logo.rotateTo(-15f, 3f)

        leftFlash.detachSelf()
        rightFlash.detachSelf()

        box {
            width = Dimension.FillAvailable
            height = Dimension.FillAvailable
            color = Colors.Black
            alpha = 0f

            fadeIn(3f).after {
                GlobalManager.getInstance().mainActivity.finish()
            }
        }
    }


    override fun onAttached() {
        super.onAttached()

        if (!MusicManager.isPlaying || MusicManager.currentBeatmap == null) {
            if (MusicManager.currentBeatmap == null) {
                MusicManager.currentBeatmap = LibraryManager.getLibrary().random().beatmaps.random()
            }
            MusicManager.load()
            //MusicManager.play()
        }
    }


    override fun onManagedUpdate(deltaTimeSec: Float) {

        logo.setScale(FloatInterpolation.floatAt(deltaTimeSec.coerceIn(0f, 0.1f), logo.scaleX, if (isMenuExpanded) 1f else 1.3f, 0f, 0.1f))

        // Music button
        val mightShowMusicButton = isMenuExpanded || System.currentTimeMillis() - lastMusicChange < 3000

        musicButton.text = "${MusicManager.currentBeatmap?.titleText} - ${MusicManager.currentBeatmap?.artistText}"
        musicButton.translationX = FloatInterpolation.floatAt(deltaTimeSec.coerceIn(0f, 0.05f), musicButton.translationX, if (mightShowMusicButton) 0f else 8f.srem, 0f, 0.05f)
        musicButton.alpha = FloatInterpolation.floatAt(deltaTimeSec.coerceIn(0f, 0.05f), musicButton.alpha, if (mightShowMusicButton) 1f else 0f, 0f, 0.05f)

        // Beat animations
        val beatLengthSeconds = RythimManager.beatLength.toFloat() / 1000f * (if (RythimManager.isKiai) 1f else RythimManager.beatSignature.toFloat())
        leftFlash.alpha = FloatInterpolation.floatAt(deltaTimeSec.coerceIn(0f, beatLengthSeconds), leftFlash.alpha, 0f, 0f, beatLengthSeconds)
        rightFlash.alpha = FloatInterpolation.floatAt(deltaTimeSec.coerceIn(0f, beatLengthSeconds), rightFlash.alpha, 0f, 0f, beatLengthSeconds)

        // Menu expansion animations
        menuContainer.width = FloatInterpolation.floatAt(deltaTimeSec.coerceIn(0f, 0.2f), menuContainer.width, if (isMenuExpanded) menuContainer.intrinsicWidth else 0f, 0f, 0.2f, Easing.OutQuint)
        menuContainer.alpha = FloatInterpolation.floatAt(deltaTimeSec.coerceIn(0f, 0.2f), menuContainer.alpha, if (isMenuExpanded) 1f else 0f, 0f, 0.2f, Easing.OutQuint)
        menuGradientBox.alpha = FloatInterpolation.floatAt(deltaTimeSec.coerceIn(0f, 0.4f), menuGradientBox.alpha, if (isMenuExpanded) 1f else 0f, 0f, 0.4f, Easing.OutQuint)

        playerButton.translationX = FloatInterpolation.floatAt(deltaTimeSec.coerceIn(0f, 0.05f), playerButton.translationX, if (isMenuExpanded) 0f else (-8f).srem, 0f, 0.05f)
        playerButton.alpha = FloatInterpolation.floatAt(deltaTimeSec.coerceIn(0f, 0.05f), playerButton.alpha, if (isMenuExpanded) 1f else 0f, 0f, 0.05f)

        super.onManagedUpdate(deltaTimeSec)
    }
}

class MenuButton(icon: Int, title: String) : UIButton() {

    private lateinit var iconComponent: UIIcon

    init {
        style += {
            width = 16f.rem
            radius = 0.65f.rem
            backgroundColor = backgroundColor.copy(alpha = 0.6f)
            padding = Vec4(6f.srem)
        }

        fillContainer {
            width = Density.Full
            orientation = Orientation.Horizontal
            style = {
                spacing = 4f.srem
            }

            +UIIcon(icon).apply {
                iconSize = FontSize.XL
                anchor = Anchor.CenterLeft
                origin = Anchor.CenterLeft
                scaleOrigin = Anchor.Center

                iconComponent = this
            }

            text {
                width = Density.Full
                text = title
                style = {
                    fontSize = FontSize.MD
                }
            }
        }
    }

    override fun onManagedUpdate(deltaTimeSec: Float) {
        val beatLengthSeconds = RythimManager.beatLength.toFloat() / 1000f

        if (RythimManager.beatElapsed / 1000f < beatLengthSeconds * 0.75f) {
            val threeQuarts = beatLengthSeconds * 0.75f
            iconComponent.setScale(FloatInterpolation.floatAt(deltaTimeSec.coerceIn(0f, threeQuarts), iconComponent.scaleX, 1f, 0f, threeQuarts))
        } else {
            val oneQuart = beatLengthSeconds * 0.25f
            iconComponent.setScale(FloatInterpolation.floatAt(deltaTimeSec.coerceIn(0f, oneQuart), iconComponent.scaleX, 0.9f, 0f, oneQuart))
        }

        super.onManagedUpdate(deltaTimeSec)
    }

}


class CarrouselLinearContainer(private val logo: OsuLogo) : UILinearContainer() {

    var shear = 2f.rem

    override fun onDrawChildren(gl: GL10, camera: Camera) {

        val (_, logoTopY) = logo.convertLocalToSceneCoordinates(0f, 0f)
        val (_, logoBottomY) = logo.convertLocalToSceneCoordinates(0f, logo.height)
        val logoCenterY = (logoTopY + logoBottomY) / 2f

        forEach { component ->
            component as UIComponent
            val (_, componentMiddleY) = component.convertLocalToSceneCoordinates(0f, component.height / 2f)

            val percentageDistance = (componentMiddleY - logoCenterY) / (logo.height / 2f)
            val shearX = -shear * abs(percentageDistance)

            component.translationX = shearX
            component.onDraw(gl, camera)
        }

        super.onDrawChildren(gl, camera)
    }

}


class BuildInformationDialog : UIDialog<UIScrollableContainer>(UIScrollableContainer().apply {
    width = Dimension.FillAvailable
    clipToBounds = true
    scrollAxes = Axis.Y
    style = {
        maxHeight = 0.7f.vh
    }
}) {
    init {
        title = "About"

        innerContent.apply {

            linearContainer {
                width = Dimension.FillAvailable
                orientation = Orientation.Vertical
                style = {
                    spacing = 4f.srem
                    paddingTop = 4f.srem
                    paddingBottom = 4f.srem
                }

                text {
                    text = "osu!droid"
                    anchor = Anchor.TopCenter
                    origin = Anchor.TopCenter
                    style = {
                        fontSize = FontSize.XL
                        fontFamily = Fonts.TorusBold
                    }
                }

                text {
                    anchor = Anchor.TopCenter
                    origin = Anchor.TopCenter
                    text = "Version: ${BuildConfig.VERSION_NAME}"
                    style = { fontSize = FontSize.MD }
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
                    colorVariant = ColorVariant.Tertiary
                    trailingIcon = UIIcon(FAIcon.ArrowUpRightFromSquare)
                    text = "Visit official osu! website"
                    onActionUp = {
                        goToLink("https://osu.ppy.sh")
                    }
                }

                textButton {
                    anchor = Anchor.TopCenter
                    origin = Anchor.TopCenter
                    scaleOrigin = Anchor.Center
                    colorVariant = ColorVariant.Tertiary
                    trailingIcon = UIIcon(FAIcon.ArrowUpRightFromSquare)
                    text = "Visit official osu!droid website"
                    onActionUp = {
                        goToLink("https://osudroid.moe")
                    }
                }

                textButton {
                    anchor = Anchor.TopCenter
                    origin = Anchor.TopCenter
                    scaleOrigin = Anchor.Center
                    colorVariant = ColorVariant.Tertiary
                    trailingIcon = UIIcon(FAIcon.ArrowUpRightFromSquare)
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
