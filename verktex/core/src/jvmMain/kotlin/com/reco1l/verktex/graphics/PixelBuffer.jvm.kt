package com.reco1l.verktex.graphics

import java.nio.ByteBuffer
import java.nio.ByteOrder

actual typealias PixelBuffer = java.nio.ByteBuffer

internal actual fun extrudePixels(pixels: PixelBuffer, width: Int, height: Int, padding: Int): PixelBuffer {
    val paddedWidth = width + 2 * padding
    val paddedHeight = height + 2 * padding

    // Whole pixels are copied as ints, their byte order doesn't matter as long as both sides share it.
    val source = pixels.duplicate().order(ByteOrder.LITTLE_ENDIAN).asIntBuffer()
    val extruded = ByteBuffer.allocateDirect(paddedWidth * paddedHeight * 4).order(ByteOrder.LITTLE_ENDIAN)
    val target = extruded.asIntBuffer()
    val row = IntArray(width)

    for (paddedY in 0 until paddedHeight) {
        source.position((paddedY - padding).coerceIn(0, height - 1) * width)
        source.get(row)

        val start = paddedY * paddedWidth
        for (i in 0 until padding) {
            target.put(start + i, row[0])
            target.put(start + padding + width + i, row[width - 1])
        }
        target.position(start + padding)
        target.put(row)
    }

    return extruded
}
