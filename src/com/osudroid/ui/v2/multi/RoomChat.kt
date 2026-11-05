package com.osudroid.ui.v2.multi

import com.reco1l.verktex.ui.text.spans
import com.osudroid.ui.v2.SkinSounds
import android.icu.text.SimpleDateFormat
import com.osudroid.multiplayer.*
import com.osudroid.multiplayer.api.*
import com.osudroid.multiplayer.api.data.*
import com.osudroid.resources.R
import com.osudroid.ui.v2.setText
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
import com.reco1l.verktex.data.toPadding
import com.reco1l.verktex.input.InputEvent
import com.reco1l.verktex.math.Easing
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.ui.FAIcon
import com.reco1l.verktex.ui.UIButton
import com.reco1l.verktex.ui.clearTransforms
import com.reco1l.verktex.ui.container.UIClickableContainer
import com.reco1l.verktex.ui.container.UILinearContainer
import com.reco1l.verktex.ui.container.container
import com.reco1l.verktex.ui.container.linearContainer
import com.reco1l.verktex.ui.container.scrollableContainer
import com.reco1l.verktex.ui.control.UITextInput
import com.reco1l.verktex.ui.moveTo
import com.reco1l.verktex.ui.text.UIText
import com.reco1l.verktex.ui.text.faSolidIcon
import com.reco1l.verktex.ui.text.text
import com.reco1l.verktex.ui.textButton
import ru.nsu.ccfit.zuev.osu.*
import ru.nsu.ccfit.zuev.osu.helper.StringTable
import java.util.LinkedList
import com.reco1l.verktex.ui.themed

/**
 * Because we're pros we want to highlight us.
 */
private val DEVELOPERS = longArrayOf(
    51076, // Rian8337
    55374, // Acivev
    307054 // Reco1l
)


class RoomChat : UILinearContainer() {

    /**
     * Height of the chat button.
     */
    val buttonHeight
        get() = button.bounds.height

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

    private val messages = LinkedList<Message>()
    private val timestampFormat = SimpleDateFormat("HH:mm:ss")

    private val button = ChatButton()

    private lateinit var content: UILinearContainer
    private lateinit var input: UITextInput
    private lateinit var messageContainer: UILinearContainer

    private var messagesChanged = false


    init {

        // Force the main container to fill the entire screen so that the chat can be closed by
        // tapping outside of it (see onInputEvent).
        width = Dimension.FillAvailable
        height = Dimension.FillAvailable

        content = linearContainer {
            width = Dimension.FillAvailable
            orientation = Orientation.Vertical
            anchor = Anchor.BottomCenter
            origin = Anchor.BottomCenter
            translationY = BODY_HEIGHT.dip
            backgroundColor = themed { (palette.accent * 0.1f).copy(alpha = 0.9f) }

            +button

            linearContainer {
                width = Dimension.FillAvailable
                height = BODY_HEIGHT.dip
                orientation = Orientation.Vertical

                scrollableContainer {
                    width = Dimension.FillAvailable
                    height = Dimension.FillAvailable
                    scrollAxes = Axis.Y
                    clipping = ClipMode.Bounds

                    messageContainer = linearContainer {
                        width = Dimension.FillAvailable
                        orientation = Orientation.Vertical

                        repeat(MAX_MESSAGES) {
                            +MessageComponent()
                        }
                    }
                }

                linearContainer {
                    width = Dimension.FillAvailable
                    spacing = 8f.dip
                    padding = Verktex.graphics.window.insets.safeArea.toPadding().copy(top = 8f.dip, bottom = 12f.dip)

                    +UITextInput("").apply {
                        width = Dimension.FillAvailable
                        height = Dimension.FillAvailable
                        maxCharacters = 200
                        placeholder = "Type a message..."
                        onConfirm = { sendMessage() }

                        input = this
                    }

                    textButton {
                        trailingIcon = faSolidIcon(FAIcon.PaperPlane)
                        variant = UIButton.Variant.Solid
                        setText(R.string.multiplayer_room_chat_send)
                        onActionUp = { sendMessage() }
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
                    StringTable.format(
                        R.string.multiplayer_room_chat_error,
                        e.message
                    ), "#FFBFBF"
                )
                e.printStackTrace()
            }
        }
    }

    private fun appendMessage(message: Message) {

        if (Verktex.scene != GlobalManager.getInstance().gameScene.scene) {
            SkinSounds["heartbeat"].play(0.75f)
        }

        messages.add(message)
        if (messages.size > MAX_MESSAGES) {
            messages.pollFirst()
        }

        button.updateState()
        messagesChanged = true
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
        }
    }

    @JvmOverloads
    fun collapse(immediate: Boolean = false) {
        if (isExpanded) {
            isExpanded = false
            input.blur()

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

    fun onSystemChatMessage(message: String, color: String) = updateThread {
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

    override fun onUpdate(clock: FrameClock) {

        if (messagesChanged) {
            messagesChanged = false

            for (i in MAX_MESSAGES - 1 downTo 0) {
                val messageComponent = messageContainer[i] as MessageComponent
                messageComponent.message = if (i < messages.size) messages[i] else null
            }
        }

        super.onUpdate(clock)
    }

    override fun onInputEvent(event: InputEvent): Boolean {
        if (super.onInputEvent(event)) {
            return true
        }

        collapse()
        return false
    }


    private fun getPlayerTagColor(player: RoomPlayer): Color4 {
        return Color4(
            when {
                player.id in DEVELOPERS -> "#F280FF"
                player.isHost -> "#00FFEA"
                else -> "#8282A8"
            }
        )
    }


    inner class ChatButton : UIClickableContainer() {

        private lateinit var summaryText: UIText


        init {
            width = Dimension.FillAvailable
            height = 45.6f.dip
            backgroundColor = themed { (palette.accent * 0.15f).copy(alpha = 0.5f) }
            padding = Verktex.graphics.window.insets.safeArea.toPadding().copy(top = 0f.dip, bottom = 0f.dip)

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
            val lastMessage = messages.peekLast()

            summaryText.spans {
                icon(FAIcon.Message)
                space(8f.dip)

                if (isExpanded || lastMessage == null) {
                    +StringTable.get(R.string.multiplayer_room_chat)
                } else if (lastMessage is PlayerMessage) {
                    color(getPlayerTagColor(lastMessage.player)) { +"${lastMessage.player.name}: " }
                    +lastMessage.content.substringBefore('\n')
                } else {
                    +"${StringTable.get(R.string.multiplayer_room_chat_system)}: "
                    +lastMessage.content.substringBefore('\n')
                }
            }
        }

    }


    inner class MessageComponent : UILinearContainer() {

        var message: Message? = null
            set(value) {
                if (field != value) {
                    field = value
                    messageChanged = true
                }
            }


        private var messageChanged = false


        init {
            width = Dimension.FillAvailable
            orientation = Orientation.Horizontal
            spacing = 8f.dip
            backgroundColor = themed { (palette.accent * 0.09f).copy(alpha = 0f) }
            padding = Verktex.graphics.window.insets.safeArea.toPadding().copy(top = 0f.dip, bottom = 0f.dip)
        }


        override fun onUpdate(clock: FrameClock) {

            if (messageChanged) {
                messageChanged = false

                clearChildren()

                val message = message ?: return
                val messageIndex = messages.indexOf(message)

                backgroundColor =
                    backgroundColor.copy(alpha = if (messageIndex % 2 == 0) 0.5f else 0f)

                if (message is SystemMessage) {
                    text {
                        width = Dimension.FillAvailable
                        anchor = Anchor.CenterLeft
                        origin = Anchor.CenterLeft
                        text = message.content
                        padding = Padding(12f.px)
                        color = message.color
                        alignment = Anchor.Center
                    }
                } else if (message is PlayerMessage) {

                    val showSender = messageIndex == 0
                            || messages[messageIndex - 1] !is PlayerMessage
                            || (messages[messageIndex - 1] as PlayerMessage).player.id != message.player.id

                    container {
                        width = 0.25f.pct
                        anchor = Anchor.CenterLeft
                        origin = Anchor.CenterLeft

                        text {
                            anchor = Anchor.CenterLeft
                            origin = Anchor.CenterLeft
                            color = themed { palette.accent * 0.5f }
                            text = timestampFormat.format(message.time)
                        }

                        if (showSender) {
                            text {
                                anchor = Anchor.CenterRight
                                origin = Anchor.CenterRight
                                color = getPlayerTagColor(message.player)
                                text = message.player.name
                            }
                        }
                    }

                    text {
                        width = Dimension.FillAvailable
                        color = themed { palette.accent }
                        text = message.content
                    }
                } else {
                    // Technically should never happen.
                    isVisible = false
                }
            }

            super.onUpdate(clock)
        }

    }


    companion object {
        private const val MAX_MESSAGES = 50
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
    override val time: Long = System.currentTimeMillis(),
) : Message

data class SystemMessage(
    val color: Color4,

    override val content: String,
    override val time: Long = System.currentTimeMillis(),
) : Message

//endregion