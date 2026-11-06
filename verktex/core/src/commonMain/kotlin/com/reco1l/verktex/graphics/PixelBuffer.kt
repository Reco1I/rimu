package com.reco1l.verktex.graphics

/**
 * Bytes handed to the graphics backends, e.g. the pixels a texture uploads, see
 * [GraphicsTexture.PixelTarget]. On the JVM it's a [java.nio.ByteBuffer], and its data lies between
 * its position and limit, so platforms pass direct buffers to their native APIs without a copy.
 */
expect sealed class PixelBuffer
