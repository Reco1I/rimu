package com.reco1l.verktex.input

/**
 * Represents a generic input event, which can be a keyboard event, character event, pointer event,
 * or scroll event. This sealed interface allows for type-safe handling of different input events
 * in a unified manner.
 *
 * All properties of this class must be mutable, as they are reused from a pool to avoid unnecessary
 * allocations in frequent input types.
 */
interface InputEvent

/**
 * Represents a keyboard input event, typically generated when a user presses or releases a key on
 * the keyboard.
 */
data class KeyboardEvent(

    /**
     * Indicates the type of action associated with the keyboard event.
     */
    var action: Action = Action.Unknown,

    /**
     * Represents the specific key associated with the keyboard event.
     */
    var key: Key = Key.Unknown,

    /**
     * Indicates whether the Shift key is pressed during the keyboard event.
     */
    var shiftPressed: Boolean = false,

    /**
     * Indicates whether the Control (Ctrl) key is pressed during the keyboard event.
     */
    var ctrlPressed: Boolean = false,

    /**
     * Indicates whether the Alt key is pressed during the keyboard event.
     */
    var altPressed: Boolean = false,

    /**
     * Indicates whether the Meta key (e.g., Command key on macOS) is pressed during the keyboard event.
     */
    var metaPressed: Boolean = false,

) : InputEvent {

    /**
     * Represents the type of action associated with a keyboard event.
     */
    enum class Action {
        Down,
        Up,
        Unknown
    }

    /**
     * Represents the keys on a standard keyboard. This enum includes letters, digits, function keys,
     * modifier keys, navigation keys, and other common keys.
     *
     * The `Unknown` value is used for keys that are not recognized or not mapped in this enum.
     */
    enum class Key {

        A, B, C, D, E, F, G,
        H, I, J, K, L, M, N,
        O, P, Q, R, S, T, U,
        V, W, X, Y, Z,

        Digit0,
        Digit1,
        Digit2,
        Digit3,
        Digit4,
        Digit5,
        Digit6,
        Digit7,
        Digit8,
        Digit9,

        F1, F2, F3, F4,
        F5, F6, F7, F8,
        F9, F10, F11, F12,

        // TODO: Maybe differentiate between left and right keys for modifiers ?
        Shift,
        Ctrl,
        Alt,
        Meta,

        // Navegación
        Up,
        Down,
        Left,
        Right,
        Home,
        End,
        PageUp,
        PageDown,
        Insert,
        Delete,

        Backspace,
        Enter,
        Escape,
        Tab,
        Space,

        Minus,
        Equal,
        LeftBracket,
        RightBracket,
        Backslash,
        Semicolon,
        Apostrophe,
        Comma,
        Period,
        Slash,
        Grave,

        Unknown
    }
}

/**
 * Represents a pointer (mouse or touch) event.
 */
data class PointerEvent(

    /**
     * Indicates the type of action associated with the pointer event, such as a button press, release, or movement.
     */
    var action: Action = Action.Unknown,

    /**
     * Represents the button associated with the pointer event, such as left or right mouse button.
     */
    var button: Button = Button.Unknown,

    /**
     * Indicates the unique identifier for the pointer (e.g., finger or mouse) associated with the event.
     * This is useful for tracking multiple pointers in multitouch scenarios.
     */
    var pointerId: Int = -1,

    /**
     * The x-coordinate of the pointer event in the window or screen space.
     */
    var x: Float = 0f,

    /**
     * The y-coordinate of the pointer event in the window or screen space.
     */
    var y: Float = 0f,

    /**
     * The change in the x-coordinate since the last pointer event, useful for detecting movement or drag gestures.
     */
    var deltaX: Float = 0f,

    /**
     * The change in the y-coordinate since the last pointer event, useful for detecting movement or drag gestures.
     */
    var deltaY: Float = 0f,
) : InputEvent {


    /**
     * Represents the type of action associated with an input event.
     */
    enum class Action {
        Down,
        Up,
        Move,
        Unknown
    }

    /**
     * Represents the buttons on a pointer device (e.g., mouse). This enum includes left and right buttons,
     * as well as an unknown button type for cases where the button is not recognized or not specified.
     */
    enum class Button {
        Left,
        Right,
        Unknown
    }
}

/**
 * Represents a scroll event, typically generated when a user scrolls using a mouse wheel or touchpad.
 */
data class ScrollEvent(
    /**
     * Indicates the unique identifier for the pointer (e.g., finger or mouse) associated with the scroll event.
     */
    var pointerId: Int = -1,

    /**
     * The change in the x-coordinate of the scroll event, representing horizontal scrolling.
     */
    var deltaX: Float = 0f,

    /**
     * The change in the y-coordinate of the scroll event, representing vertical scrolling.
     */
    var deltaY: Float = 0f,
) : InputEvent