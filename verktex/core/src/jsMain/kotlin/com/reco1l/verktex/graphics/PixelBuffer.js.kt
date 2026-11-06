package com.reco1l.verktex.graphics

// No JavaScript platform yet, this target only checks that the common code is common.
actual sealed class PixelBuffer

internal actual fun extrudePixels(pixels: PixelBuffer, width: Int, height: Int, padding: Int): PixelBuffer =
    throw UnsupportedOperationException("There's no JavaScript platform yet.")
