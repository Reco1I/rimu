package com.reco1l.verktex.data

/**
 * A set of constants representing the anchor points of a component.
 */
object Anchor {
    @JvmField val TopLeft = Vec2(0f, 0f)
    @JvmField val TopCenter = Vec2(0.5f, 0f)
    @JvmField val TopRight = Vec2(1f, 0f)
    @JvmField val CenterLeft = Vec2(0f, 0.5f)
    @JvmField val Center = Vec2(0.5f, 0.5f)
    @JvmField val CenterRight = Vec2(1f, 0.5f)
    @JvmField val BottomLeft = Vec2(0f, 1f)
    @JvmField val BottomCenter = Vec2(0.5f, 1f)
    @JvmField val BottomRight = Vec2(1f, 1f)

    /**
     * All the anchor points.
     */
    @JvmField val all = listOf(TopLeft, TopCenter, TopRight, CenterLeft, Center, CenterRight, BottomLeft, BottomCenter, BottomRight)

    private val names = listOf("TopLeft", "TopCenter", "TopRight", "CenterLeft", "Center", "CenterRight", "BottomLeft", "BottomCenter", "BottomRight")


    /**
     * Gets the name of an anchor point.
     */
    fun getName(anchor: Vec2): String {
        val index = all.indexOf(anchor)
        require(index >= 0) { "Unknown anchor: $anchor" }
        return names[index]
    }

    /**
     * Gets an anchor point from its name.
     */
    fun getFromName(name: String): Vec2 {
        val index = names.indexOf(name)
        require(index >= 0) { "Unknown anchor name: $name" }
        return all[index]
    }
}