package com.osudroid.ui.v2.multi

import com.reco1l.verktex.ui.container.container
import com.reco1l.verktex.data.extendIntoSafeArea
import com.reco1l.verktex.data.fill
import com.osudroid.OsuGame
import com.reco1l.verktex.data.Background
import com.osudroid.multiplayer.Multiplayer
import com.osudroid.multiplayer.api.LobbyAPI
import com.osudroid.resources.Res.string
import com.osudroid.ui.v2.BeatmapBackground
import com.osudroid.utils.updateThread
import com.reco1l.verktex.Logger
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.data.*
import com.reco1l.verktex.math.Easing
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.ui.*
import com.reco1l.verktex.ui.container.*
import com.reco1l.verktex.ui.container.LinearContainer.Orientation
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

class LobbyScene : Scene() {

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

    private lateinit var messageContainer: LinearContainer
    private lateinit var roomContainer: LinearContainer
    private lateinit var refreshButton: TextButton
    private lateinit var topBar: Container
    private lateinit var roomList: Container

    private val fetchScope = CoroutineScope(Dispatchers.Default)


    override fun onEnter() {
        super.onEnter()

        // Tinted by the scene, it's as bright as in the menu (gameplay may have dimmed it).
        BeatmapBackground.fadeBrightness(1f)

        // Entrance: the top bar slides down, the room list rises.
        topBar.apply {
            clearTransforms()
            alpha = 0f
            translationY = (-20f).dip
            transformSequence {
                fadeIn(0.4f, Easing.OutCubic)
                moveToY(0f, 0.55f, Easing.OutCubic)
            }
        }
        roomList.apply {
            clearTransforms()
            alpha = 0f
            translationY = 32f.dip
            transformSequence(0.05f) {
                fadeIn(0.4f, Easing.OutCubic)
                moveToY(0f, 0.55f, Easing.OutCubic)
            }
        }
    }

    init {
        // On the scene, so it also covers the safe area its padding keeps the content out of.
        background = Background.Color(themed { palette.inset.alpha(0.75f) })

        linearContainer {
            width = fill()
            height = fill()
            orientation = Orientation.Vertical


            // Top bar
            topBar = container {
                width = fill()
                background = Background.Color(themed { palette.inset.alpha(0.5f) })
                border = Border(1f.dip, top = Color4.Transparent, bottom = Color4.White.alpha(0.15f))
                padding = Padding(16f.dip).extendIntoSafeArea()

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
            roomList = container {
                padding = extendIntoSafeArea()
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


    private fun switchContainers(target: Container) {

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

                    +LoadingIndicator().apply {
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