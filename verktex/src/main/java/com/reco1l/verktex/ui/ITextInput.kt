package com.reco1l.verktex.ui

/**
 * A component that can receive text from the platform's IME through [com.reco1l.verktex.GameHost.attachTextInput].
 */
interface ITextInput {

    /**
     * The current text of the input.
     */
    var value: String

    /**
     * The maximum number of characters allowed. If set to 0, there is no limit.
     */
    val maxCharacters: Int

    /**
     * Checks if a character is allowed to be appended to this input.
     */
    fun isCharacterAllowed(char: String): Boolean

    /**
     * Checks whether a text is valid as a [value] for this input.
     */
    fun isTextValid(text: String): Boolean

    /**
     * Notifies the input that the last change was rejected.
     */
    fun notifyInputError()

    /**
     * Called when the IME action (e.g. the enter key) is pressed.
     */
    fun onImeAction() {}
}
