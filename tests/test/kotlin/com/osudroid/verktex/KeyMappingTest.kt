package com.osudroid.verktex

import android.view.KeyEvent
import com.reco1l.verktex.android.toInputKey
import com.reco1l.verktex.input.KeyboardEvent
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class KeyMappingTest {
    @Test
    fun backKeyMapsToKeyBack() {
        val event = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK)
        assertEquals(KeyboardEvent.Key.Back, event.toInputKey())
    }

    @Test
    fun escapeStillMapsToEscape() {
        assertEquals(KeyboardEvent.Key.Escape, KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ESCAPE).toInputKey())
    }

    @Test
    fun menuKeyMapsToKeyMenu() {
        val event = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MENU)
        assertEquals(KeyboardEvent.Key.Menu, event.toInputKey())
    }
}
