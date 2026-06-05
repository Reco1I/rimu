package com.reco1l.andengine

data class BufferCache(
    var isDirty: Boolean = true,
    var buffer: VertexBuffer = VertexBuffer(0),
)