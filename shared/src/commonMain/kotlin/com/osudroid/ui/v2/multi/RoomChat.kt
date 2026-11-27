package com.osudroid.ui.v2.multi

import com.reco1l.verktex.data.extendIntoSafeArea
import com.osudroid.game.Gameplay
import com.reco1l.verktex.data.Background
import com.osudroid.OsuGame
import com.osudroid.multiplayer.Multiplayer
import com.reco1l.verktex.ui.text.spans
import com.osudroid.ui.v2.SkinSounds
import com.osudroid.multiplayer.*
import com.osudroid.multiplayer.api.*
import com.osudroid.multiplayer.api.data.*
import com.osudroid.resources.Res
import com.osudroid.utils.*
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.ui.container.RecyclableComponent
import com.reco1l.verktex.ui.container.RecyclerContainer
import com.reco1l.verktex.ui.fadeIn
import com.reco1l.verktex.input.KeyboardEvent
import com.reco1l.verktex.data.Border
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Axis
import com.reco1l.verktex.data.ClipMode
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.data.px
import com.reco1l.verktex.input.InputEvent
import com.reco1l.verktex.math.Easing
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.ui.FAIcon
import com.reco1l.verktex.ui.Button
import com.reco1l.verktex.ui.clearTransforms
import com.reco1l.verktex.ui.container.ClickableContainer
import com.reco1l.verktex.ui.container.LinearContainer
import com.reco1l.verktex.ui.container.LinearContainer.Orientation
import com.reco1l.verktex.ui.container.Container
import com.reco1l.verktex.ui.container.container
import com.reco1l.verktex.ui.container.linearContainer
import com.reco1l.verktex.ui.control.TextInput
import com.reco1l.verktex.ui.moveTo
import com.reco1l.verktex.ui.text.Text
import com.reco1l.verktex.ui.text.faSolidIcon
import com.reco1l.verktex.ui.text.text
import com.reco1l.verktex.ui.textButton
import ru.nsu.ccfit.zuev.osu.*
import com.reco1l.verktex.ui.themed
import com.osudroid.utils.currentTimeMillis
import com.osudroid.utils.formatTimeOfDay

/**
 * Because we're pros we want to highlight us.
 */
private val DEVELOPERS = longArrayOf(
    51076, // Rian8337
    55374, // Acivev
    307054 // Reco1l
)


// A container rather than a linear one: the dismiss area and the panel overlap.
class RoomChat : Container() {

    /**
     * Height of the chat button, in pixels, see [BAR_HEIGHT].
     */
    val buttonHeight
        get() = BAR_HEIGHT.toPixels()

    /**
     * Whether the chat is currently expanded.
     */
    var isExpanded = false
        private set(value) {
            if (field != value) {
                field = value
                button.updateState()
            }
        }

    private val messages = ArrayDeque<Message>()

    private val button = ChatButton()

    private var content: LinearContainer
    private var dismissArea: Container
    private lateinit var input: TextInput
    private lateinit var messageList: RecyclerContainer<ChatEntry, MessageRow>

    // Numbers the entries, which alternate their background by it.
    private var nextEntryNumber = 0L


    init {

        // Fills the screen so that the chat can be closed by tapping outside of it.
        width = Dimension.FillAvailable
        height = Dimension.FillAvailable

        // Behind the panel while it's expanded, dimming the whole screen: a tap outside of the panel
        // collapses it. Hidden while collapsed, so what's below the chat gets the input.
        dismissArea = container {
            padding = extendIntoSafeArea()
            width = Dimension.FillAvailable
            height = Dimension.FillAvailable
            background = Background.Color(Color4.Black.alpha(0.2f))
            isVisible = false

            +ClickableContainer().apply {
                width = Dimension.FillAvailable
                height = Dimension.FillAvailable
                onActionDown = { collapse() }
            }
        }

        // The bar and the body below it, out of the screen while collapsed. Their backgrounds
        // reach the edges of the screen, their content staying in the safe area: the body's covers
        // the bottom of the safe area below the bar while collapsed.
        content = linearContainer {
            width = Dimension.FillAvailable
            orientation = Orientation.Vertical
            anchor = Anchor.BottomCenter
            origin = Anchor.BottomCenter
            translationY = BODY_HEIGHT.dip

            container {
                padding = extendIntoSafeArea()
                width = Dimension.FillAvailable
                background = Background.Color(themed { palette.buttonBackgroundSurface })
                border = Border(1f.dip, top = Color4.White.alpha(0.15f))

                +button
            }

            container {
                padding = extendIntoSafeArea()
                width = Dimension.FillAvailable
                background = Background.Color(themed { palette.inset.alpha(0.8f) })

                linearContainer {
                    width = Dimension.FillAvailable
                    height = BODY_HEIGHT.dip
                    orientation = Orientation.Vertical

                    // Only the rows shown are kept, from the bottom as the latest message is.
                    messageList = RecyclerContainer<ChatEntry, MessageRow>(ROW_POOL_SIZE).apply {
                        width = Dimension.FillAvailable
                        height = Dimension.FillAvailable
                        scrollAxes = Axis.Y
                        clipping = ClipMode.Bounds
                        isAnchoredToEnd = true
                        onCreateComponent = { MessageRow() }
                    }
                    +messageList

                    linearContainer {
                        width = Dimension.FillAvailable
                        spacing = 8f.dip
                        padding = Padding(12f.dip, 8f.dip, 12f.dip, 12f.dip)

                        +TextInput("").apply {
                            width = Dimension.FillAvailable
                            height = Dimension.FillAvailable
                            maxCharacters = 200
                            placeholder = "Type a message..."
                            onConfirm = { sendMessage() }

                            input = this
                        }

                        textButton {
                            trailingIcon = faSolidIcon(FAIcon.PaperPlane)
                            variant = Button.Variant.Solid
                            text = Res.string.multiplayer_room_chat_send
                            onActionUp = { sendMessage() }
                        }
                    }
                }
            }
        }
    }


    private fun sendMessage() {

        val text = input.value.trim().take(200)
        if (text.isEmpty()) {
            return
        }

        input.value = ""

        async {
            try {
                RoomAPI.sendMessage(text)
            } catch (e: Exception) {
                onSystemChatMessage(
                    Res.string.multiplayer_room_chat_error.format(e.message), "#FFBFBF"
                )
                e.printStackTrace()
            }
        }
    }

    private fun appendMessage(message: Message) {

        if (Gameplay.current == null) {
            SkinSounds["heartbeat"].play(0.75f)
        }

        // The sender is shown unless the previous message is from the same player.
        val previous = messages.lastOrNull()
        val showSender = message is PlayerMessage
                && (previous !is PlayerMessage || previous.player.id != message.player.id)

        messages.add(message)
        messageList.addData(listOf(ChatEntry(nextEntryNumber++, message, showSender)))

        if (messages.size > MAX_MESSAGES) {
            messages.removeFirstOrNull()
            messageList.removeFirstData(1)
        }

        button.updateState()
    }


    fun show() {
        if (parent == null) {
            Verktex.addOverlay(this)
        }
    }

    fun hide() {
        removeSelf()
    }

    fun expand() {
        if (!isExpanded) {
            isExpanded = true

            content.clearTransforms()
            content.moveTo(0f, 0f, 0.4f, Easing.OutExpo)

            dismissArea.apply {
                clearTransforms()
                isVisible = true
                alpha = 0f
                fadeIn(0.2f, Easing.OutCubic)
            }
        }
    }

    @JvmOverloads
    fun collapse(immediate: Boolean = false) {
        if (isExpanded) {
            isExpanded = false
            input.blur()

            dismissArea.clearTransforms()
            dismissArea.isVisible = false

            content.clearTransforms()

            if (immediate) {
                content.translationY = BODY_HEIGHT.dip
            } else {
                content.moveTo(0f, BODY_HEIGHT.dip.toPixels(), 0.4f, Easing.OutExpo)
            }
        }
    }


    fun onRoomChatMessage(player: RoomPlayer, message: String) = updateThread {
        appendMessage(
            PlayerMessage(
                player = player,
                content = message
            )
        )
    }

    fun onSystemChatMessage(text: CharSequence, color: String) = updateThread {
        val message = text.toString()
        Multiplayer.log("System message: $message")

        appendMessage(
            SystemMessage(
                color = Color4(color),
                content = message
            )
        )
    }

    override fun onDetached() {
        collapse(true)
    }


    // Back collapses the expanded chat before the scene gets it (e.g. the room asking to leave).
    override fun onInputEvent(event: InputEvent): Boolean {
        if (super.onInputEvent(event)) {
            return true
        }

        if (isExpanded && event is KeyboardEvent && event.isBackPress) {
            collapse()
            return true
        }
        return false
    }


    private fun getPlayerTagColor(player: RoomPlayer): Color4 {
        return Color4(
            when {
                player.id in DEVELOPERS -> "#F280FF"
                Multiplayer.room?.host == player.id -> "#00FFEA"
                else -> "#8282A8"
            }
        )
    }


    inner class ChatButton : ClickableContainer() {

        private var summaryText: Text


        init {
            width = Dimension.FillAvailable
            height = BAR_HEIGHT
            padding = Padding(12f.dip, 0f.dip)

            onActionUp = {
                if (!isExpanded) {
                    expand()
                } else {
                    collapse()
                }
            }

            // The icon, who sent the last message and the message.
            summaryText = text {
                width = Dimension.FillAvailable
                anchor = Anchor.CenterLeft
                origin = Anchor.CenterLeft
                maxLines = 1
                color = themed { palette.accent }
            }

            updateState()
        }


        fun updateState() {
            val lastMessage = messages.lastOrNull()

            summaryText.spans {
                icon(FAIcon.Message)
                space(8f.dip)

                if (isExpanded || lastMessage == null) {
                    +Res.string.multiplayer_room_chat.toString()
                } else if (lastMessage is PlayerMessage) {
                    color(getPlayerTagColor(lastMessage.player)) { +"${lastMessage.player.name}: " }
                    +lastMessage.content.substringBefore('\n')
                } else {
                    +"${Res.string.multiplayer_room_chat_system}: "
                    +lastMessage.content.substringBefore('\n')
                }
            }
        }

    }


    /**
     * A message in the list, with what its row shows of it besides the message.
     */
    class ChatEntry(

        /**
         * The number of the entry since the chat was created, every other one is highlighted.
         */
        val number: Long,

        val message: Message,

        /**
         * Whether the row shows who sent it, which it doesn't for consecutive messages of a player.
         */
        val showSender: Boolean,
    )


    // Built once, filled with the entry it's bound to.
    inner class MessageRow : RecyclableComponent<ChatEntry>() {

        override val isRecyclable = true

        private lateinit var playerRow: LinearContainer
        private lateinit var timeText: Text
        private lateinit var nameText: Text
        private lateinit var contentText: Text
        private lateinit var systemText: Text


        init {
            width = Dimension.FillAvailable
            padding = Padding(12f.dip, 4f.dip)

            // Only one of them is shown, and a linear container doesn't measure the hidden one.
            linearContainer {
                width = Dimension.FillAvailable
                orientation = Orientation.Vertical

                playerRow = linearContainer {
                    width = Dimension.FillAvailable
                    orientation = Orientation.Horizontal
                    spacing = 8f.dip

                    timeText = text {
                        anchor = Anchor.CenterLeft
                        origin = Anchor.CenterLeft
                        fontSettings = themed { typography.smallCaption }
                        color = themed { palette.contentSubtle }
                    }

                    // Keeps its width without the sender, so the messages of a player line up.
                    nameText = text {
                        width = NAME_WIDTH
                        anchor = Anchor.CenterLeft
                        origin = Anchor.CenterLeft
                        alignment = Anchor.CenterRight
                        maxLines = 1
                    }

                    contentText = text {
                        width = Dimension.FillAvailable
                        color = themed { palette.accent }
                    }
                }

                systemText = text {
                    width = Dimension.FillAvailable
                    padding = Padding(0f.dip, 8f.dip)
                    alignment = Anchor.Center
                }
            }
        }


        override fun onBind(data: ChatEntry) {
            val message = data.message

            // Every other message is highlighted, lighter than the panel.
            val alpha = if (data.number % 2 == 0L) ROW_HIGHLIGHT_ALPHA else 0f
            background = themed { Background.Color(palette.accent.alpha(alpha)) }

            playerRow.isVisible = message is PlayerMessage
            systemText.isVisible = message is SystemMessage

            when (message) {
                is PlayerMessage -> {
                    timeText.text = formatTimeOfDay(message.time)
                    nameText.text = if (data.showSender) message.player.name else ""
                    nameText.color = getPlayerTagColor(message.player)
                    contentText.text = message.content
                }

                is SystemMessage -> {
                    systemText.text = message.content
                    systemText.color = message.color
                }
            }
        }
    }


    companion object {

        /**
         * The height of the chat's bar, which stays at the bottom of the screen while collapsed:
         * what's shown along with the chat is padded by it so it isn't covered.
         */
        val BAR_HEIGHT = 45.6f.dip

        private val NAME_WIDTH = 128f.dip

        private const val MAX_MESSAGES = 50

        // About the rows the body shows at once, so binding them doesn't create more.
        private const val ROW_POOL_SIZE = 16

        private const val ROW_HIGHLIGHT_ALPHA = 0.05f
        private const val BODY_HEIGHT = 288f
    }
}


//region Messages

interface Message {
    val content: String
    val time: Long
}

data class PlayerMessage(
    val player: RoomPlayer,

    override val content: String,
    override val time: Long = currentTimeMillis(),
) : Message

data class SystemMessage(
    val color: Color4,

    override val content: String,
    override val time: Long = currentTimeMillis(),
) : Message

//endregion