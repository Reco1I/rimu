package com.reco1l.verktex.input

import com.reco1l.verktex.Logger
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.ui.control.UITextInput

/**
 * The InputManager class is responsible for managing and dispatching input events such as keyboard,
 * character, pointer, and scroll events.
 *
 * Input dispatching should be done by the game host of the specific platform, and this class
 * provides a centralized way to handle these events.
 */
object Input {

    /**
     * The pool for reusable [KeyboardEvent] objects.
     */
    private val keyboardEventPool = poolOf { KeyboardEvent() }

    /**
     * The pool for reusable [PointerEvent] objects.
     */
    private val pointerEventPool = poolOf { PointerEvent() }

    /**
     * The pool for reusable [ScrollEvent] objects.
     */
    private val scrollEventPool = poolOf { ScrollEvent() }


    /**
     * A map that keeps track of the current state of pointer events, indexed by pointer ID.
     * This is used to calculate deltas for pointer movement.
     */
    private val pointerState = mutableMapOf<Int, PointerEvent>()


    //region Utils

    private fun <T> poolOf(initialize: () -> T): ArrayDeque<T> {
        return ArrayDeque<T>().apply {
            repeat(10) { add(initialize()) }
        }
    }

    private fun <T> ArrayDeque<T>.removeLastOrElse(initialize: () -> T): T {
        val last = removeLastOrNull()
        if (last == null) {
            Logger.w("InputManager", "Pool exhausted, allocating new entry...")
            return initialize()
        }
        return last
    }

    //endregion


    /**
     * Dispatches a keyboard event to the engine component stack.
     *
     * @param action The action of the keyboard event (e.g., key down, key up).
     * @param key The key associated with the event.
     * @param shiftPressed Whether the Shift key is pressed.
     * @param ctrlPressed Whether the Control key is pressed.
     * @param altPressed Whether the Alt key is pressed.
     * @param metaPressed Whether the Meta key is pressed.
     *
     * @return True if the event was consumed by any component, false otherwise.
     */
    fun dispatchKeyboardEvent(
        action: KeyboardEvent.Action,
        key: KeyboardEvent.Key,
        shiftPressed: Boolean,
        ctrlPressed: Boolean,
        altPressed: Boolean,
        metaPressed: Boolean
    ): Boolean {
        val event = keyboardEventPool.removeLastOrElse { KeyboardEvent() }
        event.action = action
        event.key = key
        event.shiftPressed = shiftPressed
        event.ctrlPressed = ctrlPressed
        event.altPressed = altPressed
        event.metaPressed = metaPressed

        val consumed = Verktex.onInputEvent(event)
        keyboardEventPool.addLast(event)
        return consumed
    }

    /**
     * Dispatches a pointer event to the engine component stack.
     *
     * @param action The action of the pointer event (e.g., pointer down, pointer up).
     * @param button The button associated with the event.
     * @param pointerId The ID of the pointer.
     * @param x The x-coordinate of the pointer.
     * @param y The y-coordinate of the pointer.
     * @return True if the event was consumed by any component, false otherwise.
     */
    fun dispatchPointerEvent(
        action: PointerEvent.Action,
        button: PointerEvent.Button,
        pointerId: Int,
        x: Float,
        y: Float
    ): Boolean {
        val event = pointerEventPool.removeLastOrElse { PointerEvent() }
        event.action = action
        event.button = button
        event.pointerId = pointerId
        event.x = x
        event.y = y

        val previousEvent = pointerState[pointerId]
        if (previousEvent != null) {
            event.deltaX = x - previousEvent.x
            event.deltaY = y - previousEvent.y

            previousEvent.action = action
            previousEvent.button = button
            previousEvent.x = x
            previousEvent.y = y
        } else {
            event.deltaX = 0f
            event.deltaY = 0f
            pointerState[pointerId] = event.copy(deltaX = 0f, deltaY = 0f)
        }

        val consumed = Verktex.onInputEvent(event)
        pointerEventPool.addLast(event)
        return consumed
    }

    /**
     * Dispatches a scroll event to the engine component stack.
     *
     * @param pointerId The ID of the pointer.
     * @param deltaX The horizontal scroll amount.
     * @param deltaY The vertical scroll amount.
     * @return True if the event was consumed by any component, false otherwise.
     */
    fun dispatchScrollEvent(
        pointerId: Int,
        deltaX: Float,
        deltaY: Float
    ): Boolean {
        val event = scrollEventPool.removeLastOrElse { ScrollEvent() }
        event.pointerId = pointerId
        event.deltaX = deltaX
        event.deltaY = deltaY

        val consumed = Verktex.onInputEvent(event)
        scrollEventPool.addLast(event)
        return consumed
    }

}