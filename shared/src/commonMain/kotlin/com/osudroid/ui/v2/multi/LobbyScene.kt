package com.osudroid.ui.v2.multi

import com.osudroid.OsuGame
import com.osudroid.multiplayer.Multiplayer
import com.osudroid.multiplayer.api.LobbyAPI
import com.osudroid.resources.Res.string
import com.osudroid.settings.OsuSettings
import com.osudroid.ui.v2.SkinTextures
import com.osudroid.utils.updateThread
import com.reco1l.verktex.Logger
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.data.*
import com.reco1l.verktex.settings.Settings
import com.reco1l.verktex.texture.Textures
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.ui.*
import com.reco1l.verktex.ui.UISprite.ScaleType
import com.reco1l.verktex.ui.container.*
import com.reco1l.verktex.ui.container.UILinearContainer.Orientation
import com.reco1l.verktex.ui.control.textInput
import com.reco1l.verktex.ui.text.faSolidIcon
import com.reco1l.verktex.ui.text.text
import com.reco1l.verktex.ui.toast.Toasts
import kotlinx.coroutines.*
import ru.nsu.ccfit.zuev.osu.SecurityUtils
import ru.nsu.ccfit.zuev.osu.online.OnlineManager
import kotlin.coroutines.cancellation.CancellationException
import com.osudroid.utils.currentTimeMillis
import com.reco1l.verktex.input.InputEvent
import com.reco1l.verktex.input.KeyboardEvent

class LobbyScene : UIScene() {

    // Back leaves the lobby, once no component consumed it.
    override fun onInputEvent(event: InputEvent): Boolean {
        if (super.onInputEvent(event)) {
            return true
        }

        if (event is KeyboardEvent && event.isBackPress) {
            back()
            return true
        }
        return false
    }

    private var isFetching = false
    private var shouldFetch = true
    private var lastTimeUpdateMillis: Long = 0L

    private var searchQuery: String? = null
        set(value) {
            if (field != value) {
                field = value
                lastTimeUpdateMillis = currentTimeMillis()
                shouldFetch = true
            }
        }

    private lateinit var messageContainer: UILinearContainer
    private lateinit var roomContainer: UILinearContainer
    private lateinit var refreshButton: UITextButton

    private val fetchScope = CoroutineScope(Dispatchers.Default)


    init {
        background = UISprite().apply {
            width = fill()
            height = fill()
            scaleType = ScaleType.Crop
            textureRegion = SkinTextures["menu-background"]

            if (!Settings[OsuSettings.SafeBeatmapBackground]) {
                textureRegion = Textures["::background"] ?: textureRegion
            }
        }

        linearContainer {
            width = fill()
            height = fill()
            orientation = Orientation.Vertical
            backgroundColor = themed { palette.accent.shade(0.1f).alpha(0.9f) }

            // Top bar
            extendIntoSafeAreaContainer {
                width = fill()
                backgroundColor = themed { palette.accent.shade(0.5f).alpha(0.1f) }
                border = Border(1f.dip, colorBottom = Color4.White.alpha(0.15f))
                padding = Padding(16f.dip)

                linearContainer {
                    anchor = Anchor.CenterLeft
                    origin = Anchor.CenterLeft
                    orientation = Orientation.Horizontal
                    spacing = 8f.dip

                    textButton {
                        leadingIcon = faSolidIcon(FAIcon.ArrowLeft)
                        text = string.multiplayer_lobby_back
                        onActionUp = { back() }
                    }

                    textButton {
                        leadingIcon = faSolidIcon(FAIcon.Plus)
                        text = string.multiplayer_lobby_create_room
                        onActionUp = {
                            RoomCreateDialog(this@LobbyScene).show()
                        }
                    }

                    refreshButton = textButton {
                        leadingIcon = faSolidIcon(FAIcon.Rotate)
                        text = string.multiplayer_lobby_refresh
                        onActionUp = { shouldFetch = true }
                    }
                }

                textInput {
                    minWidth = 160f.dip
                    maxWidth = 320f.dip
                    height = fill()
                    anchor = Anchor.CenterRight
                    origin = Anchor.CenterRight
                    key = "search"
                    placeholder = string.multiplayer_lobby_search_rooms
                    trailingIcon = faSolidIcon(FAIcon.MagnifyingGlass)
                    onValueChange = { value ->
                        searchQuery = value
                    }
                }
            }

            // Room list
            extendIntoSafeAreaContainer {
                width = fill()
                height = fill()

                messageContainer = linearContainer {
                    orientation = Orientation.Vertical
                    anchor = Anchor.Center
                    origin = Anchor.Center
                    spacing = 8f.dip
                }

                scrollableContainer {
                    scrollAxes = Axis.Y
                    width = fill()
                    height = fill()
                    clipping = ClipMode.Bounds

                    roomContainer = linearContainer {
                        width = fill()
                        orientation = Orientation.Vertical
                        scaleOrigin = Anchor.Center
                        padding = Padding(16f.dip)
                        spacing = 6f.dip
                    }
                }
            }

        }

    }

    override fun onUpdate(clock: FrameClock) {

        val currentTimeMillis = currentTimeMillis()

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
            Logger.w("LobbyScene", "Already fetching room list, skipping request.")
            return
        }
        isFetching = true

        fetchScope.launch(CoroutineExceptionHandler { _, throwable ->
            Logger.e("LobbyScene", "Error fetching multiplayer rooms", throwable)

            Toasts.showLong(string.multiplayer_lobby_fetch_error_toast.format(throwable.message))

            updateThread {
                messageContainer.apply {
                    clearChildren()

                    text {
                        fontSize = themed { typography.smallBody.size }
                        text = string.multiplayer_lobby_fetch_error
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
                            text = if (searchQuery.isNullOrEmpty()) string.multiplayer_lobby_no_rooms else string.multiplayer_lobby_no_results
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
        OsuGame.music.isGaming = false
        Verktex.withTreeLock { Verktex.popScene() }
    }


    companion object {
        private const val FETCH_TIMEOUT = 1000L
    }
}