package com.reco1l.verktex.graphics

/**
 * Fills the colour of fully transparent pixels from their visible neighbours, keeping them transparent.
 *
 * Textures have straight alpha and are sampled with linear filtering, which mixes the colour of the
 * texels around a sample regardless of their alpha. Transparent texels are usually black (premultiplied
 * images can't store anything else), so the edges of a sprite blend towards black and show a dark
 * fringe. Once transparent texels take the colour of the closest visible ones, the filtered edge keeps
 * the sprite's colour and only its alpha fades.
 */
object AlphaBleeding {

    /**
     * Bleeds the [width] x [height] pixels, tightly packed, in place. Each pixel has its alpha in the
     * top 8 bits, the three colour channels are treated alike so their order doesn't matter.
     *
     * Rings of transparent pixels are filled outwards from the visible ones, each with the average of
     * its neighbours filled before it, until every pixel has a colour. Nothing changes when the image
     * is either fully opaque or fully transparent.
     */
    fun bleed(pixels: IntArray, width: Int, height: Int) {
        require(pixels.size >= width * height) { "The array doesn't hold $width x $height pixels." }

        val count = width * height
        val filled = BooleanArray(count)
        var transparentCount = 0

        for (i in 0 until count) {
            if (pixels[i] ushr 24 != 0) filled[i] = true else transparentCount++
        }

        if (transparentCount == 0 || transparentCount == count) return

        // A pixel is queued once at most, so a ring never holds more than the transparent pixels.
        val queued = BooleanArray(count)
        var ring = IntArray(transparentCount)
        var next = IntArray(transparentCount)
        val colours = IntArray(transparentCount)
        var ringSize = 0

        fun queueNeighbours(index: Int, target: IntArray, size: Int): Int {
            var targetSize = size
            val x = index % width
            val y = index / width

            for (ny in maxOf(y - 1, 0)..minOf(y + 1, height - 1)) {
                for (nx in maxOf(x - 1, 0)..minOf(x + 1, width - 1)) {
                    val neighbour = ny * width + nx
                    if (filled[neighbour] || queued[neighbour]) continue

                    queued[neighbour] = true
                    target[targetSize++] = neighbour
                }
            }
            return targetSize
        }

        for (i in 0 until count) {
            if (filled[i]) ringSize = queueNeighbours(i, ring, ringSize)
        }

        while (ringSize > 0) {
            // Averages first, so a ring only reads the pixels filled before it.
            for (r in 0 until ringSize) {
                val index = ring[r]
                val x = index % width
                val y = index / width
                var c0 = 0
                var c1 = 0
                var c2 = 0
                var samples = 0

                for (ny in maxOf(y - 1, 0)..minOf(y + 1, height - 1)) {
                    for (nx in maxOf(x - 1, 0)..minOf(x + 1, width - 1)) {
                        val neighbour = ny * width + nx
                        if (!filled[neighbour]) continue

                        val pixel = pixels[neighbour]
                        c0 += (pixel ushr 16) and 0xFF
                        c1 += (pixel ushr 8) and 0xFF
                        c2 += pixel and 0xFF
                        samples++
                    }
                }

                colours[r] = ((c0 / samples) shl 16) or ((c1 / samples) shl 8) or (c2 / samples)
            }

            for (r in 0 until ringSize) {
                pixels[ring[r]] = colours[r]
                filled[ring[r]] = true
            }

            var nextSize = 0
            for (r in 0 until ringSize) {
                nextSize = queueNeighbours(ring[r], next, nextSize)
            }

            val swap = ring
            ring = next
            next = swap
            ringSize = nextSize
        }
    }
}
