package com.reco1l.verktex.desktop

import com.reco1l.verktex.input.KeyboardEvent.Key
import org.junit.Assert.assertEquals
import org.junit.Test
import org.lwjgl.glfw.GLFW.*

class KeysTest {

    @Test
    fun rangesMapInOrder() {
        assertEquals(Key.A, glfwKeyToInputKey(GLFW_KEY_A))
        assertEquals(Key.M, glfwKeyToInputKey(GLFW_KEY_M))
        assertEquals(Key.Z, glfwKeyToInputKey(GLFW_KEY_Z))
        assertEquals(Key.Digit0, glfwKeyToInputKey(GLFW_KEY_0))
        assertEquals(Key.Digit9, glfwKeyToInputKey(GLFW_KEY_9))
        assertEquals(Key.Digit5, glfwKeyToInputKey(GLFW_KEY_KP_5))
        assertEquals(Key.F1, glfwKeyToInputKey(GLFW_KEY_F1))
        assertEquals(Key.F12, glfwKeyToInputKey(GLFW_KEY_F12))
    }

    @Test
    fun namedKeys() {
        assertEquals(Key.Shift, glfwKeyToInputKey(GLFW_KEY_RIGHT_SHIFT))
        assertEquals(Key.Ctrl, glfwKeyToInputKey(GLFW_KEY_LEFT_CONTROL))
        assertEquals(Key.Meta, glfwKeyToInputKey(GLFW_KEY_LEFT_SUPER))
        assertEquals(Key.Enter, glfwKeyToInputKey(GLFW_KEY_KP_ENTER))
        assertEquals(Key.Backspace, glfwKeyToInputKey(GLFW_KEY_BACKSPACE))
        assertEquals(Key.Escape, glfwKeyToInputKey(GLFW_KEY_ESCAPE))
        assertEquals(Key.PageDown, glfwKeyToInputKey(GLFW_KEY_PAGE_DOWN))
        assertEquals(Key.Grave, glfwKeyToInputKey(GLFW_KEY_GRAVE_ACCENT))
        assertEquals(Key.Unknown, glfwKeyToInputKey(GLFW_KEY_PRINT_SCREEN))
    }
}
