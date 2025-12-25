package com.reco1l.verktex.graphics

/**
 * A factor applied to the source or destination color when blending.
 */
enum class BlendFactor {
    Zero,
    One,
    SrcAlpha,
    OneMinusSrcAlpha,
    DstAlpha,
    OneMinusDstAlpha,
    DstColor,
    SrcColor,
    OneMinusSrcColor,
    OneMinusDstColor
}
