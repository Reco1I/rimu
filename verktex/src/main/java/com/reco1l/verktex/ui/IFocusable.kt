package com.reco1l.verktex.ui

interface IFocusable {

    /**
     * Whether this component is focused.
     */
    val canFocus: Boolean


    /**
     * Called when the component gained focus.
     */
    fun onFocusGained()

    /**
     * Called when the component has lost focus.
     */
    fun onFocusLost()

}
