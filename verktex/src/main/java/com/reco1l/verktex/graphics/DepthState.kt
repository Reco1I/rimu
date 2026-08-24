package com.reco1l.verktex.graphics

/**
 * Represents the depth state of a component.
 */
data class DepthState(
    /**
     * Indicates whether depth testing is enabled.
     */
    val enabled: Boolean,

    /**
     * Indicates whether writing to the depth buffer is enabled.
     */
    val writeEnabled: Boolean,

    /**
     * Specifies the comparison function used for depth testing.
     */
    val function: CompareFunction
) {

    companion object {

        /**
         * The default depth state with depth testing and writing disabled, and the comparison
         * function set to Less.
         */
        val Default = DepthState(
            enabled = false,
            writeEnabled = false,
            function = CompareFunction.Less
        )

    }
}