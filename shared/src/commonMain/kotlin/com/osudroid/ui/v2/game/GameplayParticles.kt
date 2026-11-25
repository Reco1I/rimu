package com.osudroid.ui.v2.game

import com.osudroid.ui.v2.SkinTextures
import com.reco1l.verktex.graphics.BlendMode
import com.reco1l.verktex.ui.ParticleSystem
import com.reco1l.verktex.ui.container.Container
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * The particles thrown out by the judgements of skins that have them, as osu!stable throws them: one
 * [ParticleSystem] per texture, in [layer], which every hit object emits into.
 */
class GameplayParticles(private val layer: Container) {

    private val systems = mutableMapOf<String, ParticleSystem>()

    /**
     * Removes every particle, e.g. when gameplay seeks.
     */
    fun clear() {
        systems.values.forEach { it.removeSelf() }
        systems.clear()
    }

    /**
     * Throws the particles of the judgement texture [name] out from ([x], [y]) in the layer, for an object of the
     * given [scale], if the skin has a texture for them.
     */
    fun emit(x: Float, y: Float, name: String, scale: Float) {
        // The names are constants: this runs on every judgement.
        val names = when {
            name.startsWith("hit300") -> NAMES_300
            name.startsWith("hit100") -> NAMES_100
            name.startsWith("hit50") -> NAMES_50
            else -> return
        }

        // Only for skins that have their own judgement too.
        if (!SkinTextures.isSkinned(names.judgement) && !SkinTextures.isSkinned(names.firstFrame)) {
            return
        }

        val particleName = names.particle
        val texture = SkinTextures[particleName] ?: return

        // Reference: https://github.com/ppy/osu/blob/master/osu.Game/Skinning/LegacyJudgementPieceNew.cs
        val system = systems.getOrPut(particleName) {
            ParticleSystem(MAX_PARTICLES).apply { blendMode = BlendMode.Additive }
        }

        // Again if the layer was cleared.
        if (system.parent == null) {
            layer += system
        }

        system.texture = texture

        for (i in 0 until PARTICLE_COUNT) {
            val direction = Random.nextFloat() * 2 * PI.toFloat()
            val distance = Random.nextFloat() * PARTICLE_SPREAD * scale

            val velocityX = sin(direction) * distance / PARTICLE_DURATION
            val velocityY = cos(direction) * distance / PARTICLE_DURATION

            // They start some way into their animation, so they don't cluster at the centre.
            system.emit(
                x = x + velocityX * PARTICLE_HEAD_START,
                y = y + velocityY * PARTICLE_HEAD_START,
                lifetime = PARTICLE_DURATION - PARTICLE_HEAD_START,
                velocityX = velocityX,
                velocityY = velocityY,
                startScale = scale,
                startAlpha = 1 - PARTICLE_HEAD_START / PARTICLE_DURATION,
                endAlpha = 0f
            )
        }
    }

    private companion object {
        const val PARTICLE_COUNT = 150
        const val PARTICLE_DURATION = 1.6f
        const val PARTICLE_HEAD_START = 0.1f

        // How far they go at most, in osu!pixels.
        const val PARTICLE_SPREAD = 70f

        // Enough for the particles of a dense stream of hits to last.
        const val MAX_PARTICLES = 2048
    }
}


// The textures of the particles of a judgement, and of the judgement itself.
private class ParticleNames(value: String) {
    val judgement = "hit$value"
    val firstFrame = "hit$value-0"
    val particle = "particle$value"
}

private val NAMES_300 = ParticleNames("300")
private val NAMES_100 = ParticleNames("100")
private val NAMES_50 = ParticleNames("50")
