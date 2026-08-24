package com.reco1l.verktex

import com.reco1l.verktex.ui.control.UITextInput

/**
 * An interface that defines the contract for a game host, which is responsible for managing the
 * lifecycle of the game engine.
 */
interface GameHost {

    /**
     * Called when the game engine has been initialized but not yet started. See [Verktex.start] for more details.
     */
    fun onEngineInitialized()

    /**
     * Called when the game engine has been started. i.e. after [Verktex.start] has been called.
     */
    fun onEngineStarted()


    /**
     * Starts listening to IME events.
     */
    fun attachTextInput(textInput: UITextInput)

    /**
     * Stops listening to IME events.
     */
    fun detachTextInput()
}