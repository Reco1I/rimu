package com.reco1l.verktex.ui

import com.reco1l.verktex.BuiltIn
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Vec2
import com.reco1l.verktex.data.Vec4
import com.reco1l.verktex.graphics.GraphicsBuffer
import com.reco1l.verktex.graphics.GraphicsRenderer
import com.reco1l.verktex.graphics.GraphicsShader
import com.reco1l.verktex.graphics.GraphicsTexture
import com.reco1l.verktex.graphics.VertexLayout
import com.reco1l.verktex.graphics.VertexLayout.Attribute
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.ui.container.UIContainer

inline fun UIContainer.particleSystem(maxParticles: Int = 256, builder: UIParticleSystem.() -> Unit): UIParticleSystem {
    return UIParticleSystem(maxParticles).apply(builder).also(::plusAssign)
}

/**
 * Draws particles whose movement and fading are computed by the vertex shader as a function of time, so nothing
 * has to be updated for them after they are emitted.
 *
 * Positions are in the local space of this component. When there are more than [maxParticles] the oldest one
 * gets replaced.
 */
open class UIParticleSystem(val maxParticles: Int = 256) : UICanvas() {

    /**
     * The texture of the particles.
     */
    var texture: GraphicsTexture.Region? = null


    private val particles = FloatArray(maxParticles * FLOATS_PER_PARTICLE)
    private val colors = Array(maxParticles) { Color4.White }

    private var nextParticle = 0
    private var particleCount = 0

    private var time = 0f

    private var instanceBuffer: GraphicsBuffer? = null


    /**
     * Emits a particle at the given position.
     *
     * @param lifetime How long the particle lasts, in seconds.
     * @param rotation The initial rotation, in degrees.
     * @param angularVelocity The rotation speed, in degrees per second.
     */
    fun emit(
        x: Float,
        y: Float,
        lifetime: Float,
        velocityX: Float = 0f,
        velocityY: Float = 0f,
        accelerationX: Float = 0f,
        accelerationY: Float = 0f,
        startScale: Float = 1f,
        endScale: Float = startScale,
        startAlpha: Float = 1f,
        endAlpha: Float = startAlpha,
        rotation: Float = 0f,
        angularVelocity: Float = 0f,
        color: Color4 = Color4.White
    ) {
        var i = nextParticle * FLOATS_PER_PARTICLE

        particles[i++] = x
        particles[i++] = y
        particles[i++] = velocityX
        particles[i++] = velocityY
        particles[i++] = accelerationX
        particles[i++] = accelerationY
        particles[i++] = time
        particles[i++] = lifetime
        particles[i++] = startScale
        particles[i++] = endScale
        particles[i++] = startAlpha
        particles[i++] = endAlpha
        particles[i++] = Math.toRadians(rotation.toDouble()).toFloat()
        particles[i] = Math.toRadians(angularVelocity.toDouble()).toFloat()

        colors[nextParticle] = color

        nextParticle = (nextParticle + 1) % maxParticles

        if (particleCount < maxParticles) {
            particleCount++
        }
    }

    /**
     * Removes all the particles.
     */
    fun clearParticles() {
        nextParticle = 0
        particleCount = 0
    }


    override fun onUpdate(clock: FrameClock) {
        time += clock.elapsedFrameTime
        super.onUpdate(clock)
    }

    override fun onDraw(renderer: GraphicsRenderer) {
        val region = texture ?: return

        val buffer = instanceBuffer ?: renderer.createVertexBuffer().also { instanceBuffer = it }
        val instanceCount = writeAliveParticles(buffer)

        if (instanceCount == 0) {
            return
        }

        val transform = renderer.currentTransform

        renderer.drawInstanced(
            shader = BuiltIn.Shaders.Particle,
            texture = region.texture,
            vertexBuffer = quadBuffer(renderer),
            vertexLayout = QUAD_LAYOUT,
            vertexCount = 6,
            instanceBuffer = buffer,
            instanceLayout = PARTICLE_LAYOUT,
            instanceCount = instanceCount,
            uniformSet = GraphicsShader.UniformSet().apply {
                set("uTime", time)
                set("uParticleSize", Vec2(region.width, region.height))
                set("uUVRect", Vec4(region.u0, region.v0, region.u1, region.v1))
                set("uColorMultiplier", renderer.transformColor(contentColor))

                // Column-major, with the 2D transformation in the X and Y axes.
                set("uTransform", floatArrayOf(
                    transform.a, transform.b, 0f, 0f,
                    transform.c, transform.d, 0f, 0f,
                    0f, 0f, 1f, 0f,
                    transform.tx, transform.ty, 0f, 1f
                ))
            }
        )
    }

    private fun writeAliveParticles(buffer: GraphicsBuffer): Int {
        buffer.clear()

        var count = 0

        for (particle in 0 until particleCount) {
            val i = particle * FLOATS_PER_PARTICLE

            val birth = particles[i + BIRTH_OFFSET]
            val lifetime = particles[i + BIRTH_OFFSET + 1]

            if (time - birth >= lifetime) {
                continue
            }

            for (j in 0 until FLOATS_PER_PARTICLE) {
                buffer.putFloat(particles[i + j])
            }

            val color = colors[particle]

            buffer.putPackedBytes(color.packedRGBA)

            count++
        }

        return count
    }

    override fun onDetached() {
        super.onDetached()

        val buffer = instanceBuffer ?: return
        instanceBuffer = null

        Verktex.scheduleOnDraw {
            buffer.dispose()
            Verktex.graphics.unregisterResource(buffer)
        }
    }


    private companion object {

        const val FLOATS_PER_PARTICLE = 14
        const val BIRTH_OFFSET = 6

        val QUAD_LAYOUT = VertexLayout(listOf(Attribute(0, 2)))

        val PARTICLE_LAYOUT = VertexLayout(listOf(
            Attribute(1, 2, isPerInstance = true),
            Attribute(2, 2, isPerInstance = true),
            Attribute(3, 2, isPerInstance = true),
            Attribute(4, 2, isPerInstance = true),
            Attribute(5, 2, isPerInstance = true),
            Attribute(6, 2, isPerInstance = true),
            Attribute(7, 2, isPerInstance = true),
            Attribute(8, 4, VertexLayout.Type.NormalizedByte, isPerInstance = true)
        ))

        private var quad: GraphicsBuffer? = null

        fun quadBuffer(renderer: GraphicsRenderer): GraphicsBuffer {
            return quad ?: renderer.createVertexBuffer().also { buffer ->
                for (corner in floatArrayOf(-0.5f, -0.5f, 0.5f, -0.5f, 0.5f, 0.5f, -0.5f, -0.5f, 0.5f, 0.5f, -0.5f, 0.5f)) {
                    buffer.putFloat(corner)
                }
                quad = buffer
            }
        }
    }
}
