package com.reco1l.andengine

data class RenderLayer(
    val zIndex: Int = 0,
    val commands: MutableList<DrawCommand> = mutableListOf(),
)
