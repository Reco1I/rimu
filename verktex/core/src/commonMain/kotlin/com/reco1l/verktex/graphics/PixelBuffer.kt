package com.reco1l.verktex.graphics

/**
 * Bytes handed to the graphics backends, e.g. the pixels a texture uploads, see
 * [GraphicsTexture.PixelTarget]. On the JVM it's a [java.nio.ByteBuffer], and its data lies between
 * its position and limit, so platforms pass direct buffers to their native APIs without a copy.
 */
expect sealed class PixelBuffer

/**
 * Returns the [width] x [height] RGBA8 [pixels], tightly packed, surrounded by [padding] pixels on
 * every side that repeat the closest edge pixel, as a new buffer of (width + 2 padding) x
 * (height + 2 padding) pixels. [pixels] is left as it was.
 */
internal expect fun extrudePixels(pixels: PixelBuffer, width: Int, height: Int, padding: Int): PixelBuffer
