package com.reco1l.verktex.desktop

import com.reco1l.verktex.ui.ITextInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextInputEditorTest {

    private class Input(
        override var value: String = "",
        override val maxCharacters: Int = 0,
        val allowed: (String) -> Boolean = { true },
        val valid: (String) -> Boolean = { true },
    ) : ITextInput {
        var errors = 0
        override fun isCharacterAllowed(char: String) = allowed(char)
        override fun isTextValid(text: String) = valid(text)
        override fun notifyInputError() { errors++ }
    }

    @Test
    fun insertsAtTheEnd() {
        val input = Input("ab")
        assertTrue(TextInputEditor.insert(input, "c"))
        assertEquals("abc", input.value)
    }

    @Test
    fun limitCountsCodePoints() {
        val input = Input("😀", maxCharacters = 2)
        assertTrue(TextInputEditor.insert(input, "😀"))
        assertFalse(TextInputEditor.insert(input, "a"))
        assertEquals("😀😀", input.value)
        assertEquals(1, input.errors)
    }

    @Test
    fun rejectsDisallowedCharactersOfAPaste() {
        val input = Input("1", allowed = { it.all(Char::isDigit) })
        assertFalse(TextInputEditor.insert(input, "23x"))
        assertEquals("1", input.value)
        assertEquals(1, input.errors)
    }

    @Test
    fun deletesWholeSurrogatePairs() {
        val input = Input("a😀")
        assertTrue(TextInputEditor.deleteLast(input))
        assertEquals("a", input.value)
        assertTrue(TextInputEditor.deleteLast(input))
        assertFalse(TextInputEditor.deleteLast(input))
        assertEquals("", input.value)
    }

    @Test
    fun deletingKeepsTheTextValid() {
        val input = Input("12", valid = { it.isNotEmpty() })
        assertTrue(TextInputEditor.deleteLast(input))
        assertFalse(TextInputEditor.deleteLast(input))
        assertEquals("1", input.value)
        assertEquals(1, input.errors)
    }
}
