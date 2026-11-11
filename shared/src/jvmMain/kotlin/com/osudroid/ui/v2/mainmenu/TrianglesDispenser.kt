package com.osudroid.ui.v2.mainmenu

import com.reco1l.framework.forEachTolerant
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.data.px
import com.reco1l.verktex.graphics.GraphicsRenderer
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.ui.UICanvas
import kotlin.random.Random

class TrianglesDispenser : UICanvas() {

    /**
     * The minimum scale factor for triangles.
     */
    var triangleMinSize = 0.2f

    /**
     * The maximum scale factor for triangles.
     */
    var triangleMaxSize = 0.4f

    /**
     * The min velocity of the triangles measured in pixels per second.
     */
    var triangleMinVelocity = 40f

    /**
     * The max velocity of the triangles measured in pixels per second.
     */
    var triangleMaxVelocity = 80f

    /**
     * The color palette of the triangles.
     */
    var colorPalette = arrayOf(
        Color4.White.shade(0.1f),
        Color4.White.shade(0.125f),
        Color4.White.shade(0.15f),
    )

    /**
     * The spawn rate in triangles per second.
     */
    var spawnRate = 2f

    /**
     * Whether to draw only the outline of the triangles instead of filling them.
     */
    var isOutlined = false

    /**
     * The width of the outline, only used when [isOutlined].
     */
    var lineWidth: Dimension.Fixed = 1f.px


    /**
     * The base size of the triangle shape relative to the dispenser height.
     */
    private val triangleBaseSize: Float
        get() = bounds.height * 0.6f


    private var activeTriangles = mutableListOf<TriangleInfo>()

    private var spawnTimer = 0f


    override fun onUpdate(clock: FrameClock) {
        val deltaTimeSec = clock.elapsedFrameTime

        // Update spawn timer
        spawnTimer += deltaTimeSec
        val spawnInterval = 1f / spawnRate

        // Spawn new triangles
        while (spawnTimer >= spawnInterval) {
            spawnTimer -= spawnInterval
            spawnTriangle()
        }

        // Update existing triangles
        activeTriangles.forEachTolerant { info ->
            info.update(deltaTimeSec)
        }

        activeTriangles.removeAll { it.y < -(triangleBaseSize * it.scale) }

        super.onUpdate(clock)
    }

    override fun onDraw(renderer: GraphicsRenderer) {
        val thickness = lineWidth.toPixels()

        activeTriangles.forEachTolerant { info ->
            val size = triangleBaseSize * info.scale
            val x = info.x
            val y = info.y

            // A rotated triangle has its tip pointing up instead of down.
            val height = if (info.isFlipped) -size else size

            if (isOutlined) {
                val tipX = x + size / 2f
                val tipY = y + height

                renderer.drawLine(x, y, x + size, y, thickness, info.color)
                renderer.drawLine(x + size, y, tipX, tipY, thickness, info.color)
                renderer.drawLine(tipX, tipY, x, y, thickness, info.color)
            } else {
                renderer.drawTriangle(x, y, size, height, color = info.color)
            }
        }
    }


    private fun spawnTriangle() {
        val scale = Random.nextFloat().let {
            triangleMinSize + it * (triangleMaxSize - triangleMinSize)
        }

        val velocity = Random.nextFloat().let {
            triangleMinVelocity + it * (triangleMaxVelocity - triangleMinVelocity)
        }

        val color = colorPalette.random()
        val x = Random.nextFloat() * bounds.width
        val y = bounds.height + triangleBaseSize * scale

        activeTriangles.add(TriangleInfo(x, y, scale, velocity, color, Random.nextBoolean()))
    }


    data class TriangleInfo(
        var x: Float,
        var y: Float,
        var scale: Float,
        var velocity: Float,
        var color: Color4,
        var isFlipped: Boolean,
    ) {
        fun update(deltaTimeSec: Float) {
            y -= velocity * deltaTimeSec
        }
    }

}
