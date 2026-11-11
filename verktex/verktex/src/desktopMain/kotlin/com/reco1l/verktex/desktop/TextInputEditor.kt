package com.reco1l.verktex.desktop

import com.reco1l.verktex.ui.ITextInput

/**
 * Edits the text of an [ITextInput] from a physical keyboard, the desktop counterpart of the
 * hidden EditText the Android platform types into. The caret is always at the end, as it's put
 * there on Android.
 *
 * The calls must be made under the engine's tree lock.
 */
object TextInputEditor {

    /**
     * Inserts [text] (typed or pasted) at the end, unless the input rejects it.
     *
     * @return Whether it was inserted.
     */
    fun insert(input: ITextInput, text: String): Boolean {
        if (text.isEmpty()) return false

        val result = input.value + text
        if (!isTextAccepted(input, text, result)) {
            input.notifyInputError()
            return false
        }

        input.value = result
        return true
    }

    /**
     * Deletes the last code point, a surrogate pair at once.
     *
     * @return Whether there was one to delete.
     */
    fun deleteLast(input: ITextInput): Boolean {
        val value = input.value
        if (value.isEmpty()) return false

        val end = value.length
        val start = if (end >= 2 && value[end - 1].isLowSurrogate() && value[end - 2].isHighSurrogate()) end - 2 else end - 1
        val result = value.substring(0, start)

        // Deleting can make the text invalid as well (e.g. a minimum value), as on Android.
        if (!input.isTextValid(result)) {
            input.notifyInputError()
            return false
        }

        input.value = result
        return true
    }

    /**
     * The same checks as the Android platform's filter: the character limit, every inserted
     * character, then the whole text.
     */
    fun isTextAccepted(input: ITextInput, inserted: String, result: String): Boolean {
        if (input.maxCharacters > 0 && result.codePointCount(0, result.length) > input.maxCharacters) {
            return false
        }

        var index = 0
        while (index < inserted.length) {
            val charCount = Character.charCount(inserted.codePointAt(index))
            if (!input.isCharacterAllowed(inserted.substring(index, index + charCount))) {
                return false
            }
            index += charCount
        }

        return input.isTextValid(result)
    }
}
