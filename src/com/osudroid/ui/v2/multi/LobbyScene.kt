package com.osudroid.ui.v2.multi

import android.util.Log
import com.osudroid.multiplayer.*
import com.osudroid.multiplayer.api.LobbyAPI
import com.osudroid.resources.R.string
import com.osudroid.ui.v2.SkinTextures
import com.osudroid.ui.v2.setText
import com.osudroid.utils.updateThread
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Axis
import com.reco1l.verktex.data.ClipMode
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.texture.Textures
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.ui.FAIcon
import com.reco1l.verktex.ui.UIIcon
import com.reco1l.verktex.ui.UILoadingIndicator
import com.reco1l.verktex.ui.UIScene
import com.reco1l.verktex.ui.UISprite
import com.reco1l.verktex.ui.UITextButton
import com.reco1l.verktex.ui.clearTransforms
import com.reco1l.verktex.ui.container.UIContainer
import com.reco1l.verktex.ui.container.UILinearContainer
import com.reco1l.verktex.ui.container.container
import com.reco1l.verktex.ui.container.linearContainer
import com.reco1l.verktex.ui.container.scrollableContainer
import com.reco1l.verktex.ui.control.UITextInput
import com.reco1l.verktex.ui.fadeIn
import com.reco1l.verktex.ui.fadeOut
import com.reco1l.verktex.ui.scaleTo
import com.reco1l.verktex.ui.text.faSolidIcon
import com.reco1l.verktex.ui.text.text
import com.reco1l.verktex.ui.textButton
import kotlinx.coroutines.*
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.SecurityUtils
import ru.nsu.ccfit.zuev.osu.ToastLogger
import kotlin.coroutines.cancellation.CancellationException
import ru.nsu.ccfit.zuev.osu.helper.StringTable
import ru.nsu.ccfit.zuev.osu.online.OnlineManager
import com.reco1l.verktex.ui.themed

class LobbyScene : UIScene() {

    private var isFetching = false
    private var shouldFetch = true
    private var lastTimeUpdateMillis: Long = 0L

    private var searchQuery: String? = null
        set(value) {
            if (field != value) {
                field = value
                lastTimeUpdateMillis = System.currentTimeMillis()
                shouldFetch = true
            }
        }

    private lateinit var messageContainer: UILinearContainer
    private lateinit var roomContainer: UILinearContainer
    private lateinit var refreshButton: UITextButton

    private val fetchScope = CoroutineScope(Dispatchers.Default)


    init {
        background = UISprite().apply {
            width = Dimension.FillAvailable
            height = Dimension.FillAvailable
            scaleType = UISprite.ScaleType.Crop
            textureRegion = SkinTextures["menu-background"]

            if (!Config.isSafeBeatmapBg()) {
                textureRegion = Textures["::background"] ?: textureRegion
            }
        }

        linearContainer {
            orientation = UILinearContainer.Orientation.Vertical
            width = Dimension.FillAvailable
            height = Dimension.FillAvailable
            spacing = 16f.dip
            padding = Padding(top = 8f.dip, bottom = 8f.dip)
            backgroundColor = themed { (palette.accent.shade(0.1f)).alpha(0.9f) }

            container {
                width = Dimension.FillAvailable
                padding = Padding(0f.dip, 16f.dip)

                linearContainer {
                    orientation = UILinearContainer.Orientation.Horizontal
                    spacing = 8f.dip

                    textButton {
                        leadingIcon = faSolidIcon(FAIcon.ArrowLeft)
                        setText(string.multiplayer_lobby_back)
                        onActionUp = { back() }
                    }

                    textButton {
                        leadingIcon = faSolidIcon(FAIcon.Plus)
                        setText(string.multiplayer_lobby_create_room)
                        onActionUp = {
                            RoomCreateDialog(this@LobbyScene).show()
                        }
                    }

                    refreshButton = textButton {
                        leadingIcon = faSolidIcon(FAIcon.Rotate)
                        setText(string.multiplayer_lobby_refresh)
                        onActionUp = { shouldFetch = true }
                    }
                }

                container {
                    anchor = Anchor.TopRight
                    origin = Anchor.TopRight
                    height = Dimension.FillAvailable

                    +UITextInput("").apply {
                        key = "search"
                        width = 208f.dip
                        height = Dimension.FillAvailable
                        placeholder = StringTable.get(string.multiplayer_lobby_search_rooms)
                        onValueChange = { value ->
                            searchQuery = value
                        }
                    }

                    container {
                        anchor = Anchor.CenterRight
                        origin = Anchor.CenterRight
                        padding = Padding(8f.dip, 0f.dip)

                        +UIIcon(FAIcon.MagnifyingGlass).apply {
                            color = themed { palette.accent }
                        }
                    }
                }
            }

            container {
                width = Dimension.FillAvailable
                height = Dimension.FillAvailable

                messageContainer = linearContainer {
                    orientation = UILinearContainer.Orientation.Vertical
                    anchor = Anchor.Center
                    origin = Anchor.Center
                    spacing = 8f.dip
                }

                scrollableContainer {
                    scrollAxes = Axis.Y
                    width = Dimension.FillAvailable
                    height = Dimension.FillAvailable
                    clipping = ClipMode.Bounds

                    roomContainer = linearContainer {
                        width = Dimension.FillAvailable
                        orientation = UILinearContainer.Orientation.Vertical
                        scaleOrigin = Anchor.Center
                        spacing = 6f.dip
                    }
                }
            }

        }

    }

    override fun onUpdate(clock: FrameClock) {

        val currentTimeMillis = System.currentTimeMillis()

        refreshButton.isEnabled = !isFetching && currentTimeMillis - lastTimeUpdateMillis > FETCH_TIMEOUT

        if (shouldFetch && currentTimeMillis - lastTimeUpdateMillis > FETCH_TIMEOUT) {
            shouldFetch = false
            lastTimeUpdateMillis = currentTimeMillis
            fetchRooms()
        }

        super.onUpdate(clock)
    }


    override fun onDetached() {
        fetchScope.cancel(CancellationException())
    }


    private fun switchContainers(target: UIContainer) {

        val opposite = if (target == roomContainer) messageContainer else roomContainer

        opposite.apply {
            clearTransforms()
            scaleTo(0.95f, 0.3f)
            fadeOut(0.3f)
        }

        target.apply {
            clearTransforms()

            scaleX = 0.95f
            scaleY = 0.95f
            alpha = 0f

            scaleTo(1f, 0.3f)
            fadeIn(0.3f)
        }
    }

    private fun fetchRooms() {

        if (isFetching) {
            Log.w("LobbyScene", "Already fetching room list, skipping request.")
            return
        }
        isFetching = true

        fetchScope.launch(CoroutineExceptionHandler { _, throwable ->
            Log.e("LobbyScene", "Error fetching multiplayer rooms", throwable)

            ToastLogger.showText(
                StringTable.format(string.multiplayer_lobby_fetch_error_toast, throwable.message),
                true
            )

            updateThread {
                messageContainer.apply {
                    clearChildren()

                    text {
                        fontSize = themed { typography.smallBody.size }
                        color = themed { palette.accent }
                        setText(string.multiplayer_lobby_fetch_error)
                    }
                }
            }

            isFetching = false
        }) {
            updateThread {
                messageContainer.apply {
                    clearChildren()

                    +UILoadingIndicator().apply {
                        anchor = Anchor.Center
                        origin = Anchor.Center

                    }
                }

                switchContainers(messageContainer)
                roomContainer.clearChildren()
            }

            val list = LobbyAPI.getRooms(
                query = searchQuery,
                sessionId = OnlineManager.getInstance().sessionId,
                uid = OnlineManager.getInstance().userId,
                sign = SecurityUtils.signRequest("${searchQuery ?: ""}_${OnlineManager.getInstance().userId}_${OnlineManager.getInstance().sessionId}")
            )

            updateThread {

                list.forEach {
                    roomContainer += RoomButton(this@LobbyScene, it)
                }

                if (list.isEmpty()) {
                    messageContainer.apply {
                        clearChildren()

                        text {
                            fontSize = themed { typography.smallBody.size }
                            color = themed { palette.accent }
                            setText(if (searchQuery.isNullOrEmpty()) string.multiplayer_lobby_no_rooms else string.multiplayer_lobby_no_results)
                        }
                    }
                } else {
                    switchContainers(roomContainer)
                }

                isFetching = false
            }
        }
    }


    fun back() {
        Multiplayer.isMultiplayer = false
        GlobalManager.getInstance().songService.isGaming = false
        Verktex.withTreeLock { Verktex.popScene() }
    }


    companion object {
        private const val FETCH_TIMEOUT = 1000L
    }
}