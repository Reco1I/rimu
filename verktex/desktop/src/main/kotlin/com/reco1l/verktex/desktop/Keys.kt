package com.reco1l.verktex.desktop

import com.reco1l.verktex.input.KeyboardEvent.Key
import org.lwjgl.glfw.GLFW.*

/**
 * The engine's key for a GLFW key code (GLFW_KEY_*), [Key.Unknown] for the ones it has none for.
 */
fun glfwKeyToInputKey(key: Int): Key = when (key) {
    in GLFW_KEY_A..GLFW_KEY_Z -> Key.entries[Key.A.ordinal + (key - GLFW_KEY_A)]
    in GLFW_KEY_0..GLFW_KEY_9 -> Key.entries[Key.Digit0.ordinal + (key - GLFW_KEY_0)]
    in GLFW_KEY_KP_0..GLFW_KEY_KP_9 -> Key.entries[Key.Digit0.ordinal + (key - GLFW_KEY_KP_0)]
    in GLFW_KEY_F1..GLFW_KEY_F12 -> Key.entries[Key.F1.ordinal + (key - GLFW_KEY_F1)]

    GLFW_KEY_LEFT_SHIFT, GLFW_KEY_RIGHT_SHIFT -> Key.Shift
    GLFW_KEY_LEFT_CONTROL, GLFW_KEY_RIGHT_CONTROL -> Key.Ctrl
    GLFW_KEY_LEFT_ALT, GLFW_KEY_RIGHT_ALT -> Key.Alt
    GLFW_KEY_LEFT_SUPER, GLFW_KEY_RIGHT_SUPER -> Key.Meta

    GLFW_KEY_UP -> Key.Up
    GLFW_KEY_DOWN -> Key.Down
    GLFW_KEY_LEFT -> Key.Left
    GLFW_KEY_RIGHT -> Key.Right
    GLFW_KEY_HOME -> Key.Home
    GLFW_KEY_END -> Key.End
    GLFW_KEY_PAGE_UP -> Key.PageUp
    GLFW_KEY_PAGE_DOWN -> Key.PageDown
    GLFW_KEY_INSERT -> Key.Insert
    GLFW_KEY_DELETE -> Key.Delete

    GLFW_KEY_BACKSPACE -> Key.Backspace
    GLFW_KEY_ENTER, GLFW_KEY_KP_ENTER -> Key.Enter
    GLFW_KEY_ESCAPE -> Key.Escape
    GLFW_KEY_MENU -> Key.Menu
    GLFW_KEY_TAB -> Key.Tab
    GLFW_KEY_SPACE -> Key.Space

    GLFW_KEY_MINUS, GLFW_KEY_KP_SUBTRACT -> Key.Minus
    GLFW_KEY_EQUAL, GLFW_KEY_KP_EQUAL -> Key.Equal
    GLFW_KEY_LEFT_BRACKET -> Key.LeftBracket
    GLFW_KEY_RIGHT_BRACKET -> Key.RightBracket
    GLFW_KEY_BACKSLASH -> Key.Backslash
    GLFW_KEY_SEMICOLON -> Key.Semicolon
    GLFW_KEY_APOSTROPHE -> Key.Apostrophe
    GLFW_KEY_COMMA -> Key.Comma
    GLFW_KEY_PERIOD, GLFW_KEY_KP_DECIMAL -> Key.Period
    GLFW_KEY_SLASH, GLFW_KEY_KP_DIVIDE -> Key.Slash
    GLFW_KEY_GRAVE_ACCENT -> Key.Grave

    else -> Key.Unknown
}
