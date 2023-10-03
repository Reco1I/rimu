package com.reco1l.framework.annotation

import androidx.annotation.IntDef

@IntDef(
    value = [
        Anchor.CENTER,

        Anchor.TOP,
        Anchor.BOTTOM,
        Anchor.LEFT,
        Anchor.RIGHT,

        Anchor.TOP_LEFT,
        Anchor.TOP_RIGHT,
        Anchor.TOP_CENTER,

        Anchor.BOTTOM_LEFT,
        Anchor.BOTTOM_RIGHT,
        Anchor.BOTTOM_CENTER,
    ]
)
annotation class Anchor
{
    companion object
    {
        const val CENTER = 0

        const val LEFT = 1
        const val RIGHT = 2
        const val TOP = 3
        const val BOTTOM = 4

        const val TOP_LEFT = 5
        const val TOP_RIGHT = 6
        const val TOP_CENTER = 7

        const val BOTTOM_LEFT = 8
        const val BOTTOM_RIGHT = 9
        const val BOTTOM_CENTER = 10

    }
}

@IntDef(
    value = [
        Anchor.TOP,
        Anchor.BOTTOM,
        Anchor.LEFT,
        Anchor.RIGHT,
    ]
)
annotation class BasicAnchor

@IntDef(
    value = [
        Anchor.TOP_LEFT,
        Anchor.TOP_RIGHT,
        Anchor.BOTTOM_LEFT,
        Anchor.BOTTOM_RIGHT,
    ]
)
annotation class CornerAnchor