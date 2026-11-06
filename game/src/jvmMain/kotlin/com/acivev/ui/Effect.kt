package com.acivev.ui

import com.osudroid.settings.GameResolution
import com.osudroid.OsuPlatform
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.graphics.BlendMode
import com.reco1l.verktex.texture.Textures
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.ui.UIParticleSystem
import com.reco1l.verktex.ui.container.UIContainer
import java.util.Calendar
import kotlin.random.Random

private fun Random.nextFloat(from: Float, to: Float) = from + nextFloat() * (to - from)

/**
 * Snow falling from the top of the screen.
 */
private class Snowfall(isLowEndDevice: Boolean) : UIParticleSystem(if (isLowEndDevice) 15 else 75) {

    private var spawnTimer = 0f

    init {
        texture = Textures["snow"] ?: Textures.load("snow", "snow.png", isInternal = true)
    }

    override fun onUpdate(clock: FrameClock) {
        super.onUpdate(clock)

        spawnTimer += clock.elapsedFrameTime

        val interval = 1f / Random.nextFloat(10f, 15f)

        while (spawnTimer >= interval) {
            spawnTimer -= interval

            emit(
                x = Random.nextFloat() * GameResolution.width,
                y = 0f,
                lifetime = Random.nextFloat(1f, 7f),
                velocityX = Random.nextFloat(-50f, 50f),
                velocityY = Random.nextFloat(100f, 300f),
                accelerationY = 10f,
                startScale = 0.2f,
                endScale = 0.8f,
                startAlpha = 1f,
                endAlpha = 0f,
                rotation = Random.nextFloat(0f, 360f)
            )
        }
    }
}

/**
 * Fireworks launched from the bottom of the screen.
 */
private class Fireworks(private val isLowEndDevice: Boolean) : UIContainer() {

    private val trail = UIParticleSystem(TRAIL_PARTICLES).also {
        it.blendMode = BlendMode.Additive
        it.texture = loadTexture("firework_rocket")
        +it
    }

    private val explosions = List(3) { i ->
        UIParticleSystem(MAX_EXPLOSION_PARTICLES).also {
            it.blendMode = BlendMode.Additive
            it.texture = loadTexture("firework_explosion${i + 1}")
            +it
        }
    }

    private var launchTimer = 0f
    private var launchX = 0f
    private var trailTimer = -1f
    private var trailSpawnTimer = 0f


    private fun loadTexture(name: String) = Textures[name] ?: Textures.load(name, "$name.png", isInternal = true)

    override fun onUpdate(clock: FrameClock) {
        super.onUpdate(clock)

        val elapsed = clock.elapsedFrameTime

        launchTimer -= elapsed

        if (launchTimer <= 0f) {
            launchTimer = Random.nextFloat(1.5f, 4f)
            launchX = GameResolution.width * Random.nextFloat(0.05f, 0.95f)
            trailTimer = 0.8f
        }

        if (trailTimer > 0f) {
            trailTimer -= elapsed
            trailSpawnTimer += elapsed

            while (trailSpawnTimer >= TRAIL_INTERVAL) {
                trailSpawnTimer -= TRAIL_INTERVAL

                trail.emit(
                    x = launchX,
                    y = GameResolution.height * 0.9f,
                    lifetime = Random.nextFloat(0.3f, 0.6f),
                    velocityX = Random.nextFloat(-20f, 20f),
                    velocityY = Random.nextFloat(-800f, -600f),
                    startScale = 0.8f,
                    endScale = 0.3f,
                    startAlpha = 1f,
                    endAlpha = 0f
                )
            }

            if (trailTimer <= 0f) {
                explode()
            }
        }
    }

    private fun explode() {
        val particles = if (isLowEndDevice) 10 else 25
        val system = explosions.random()
        val color = COLORS.random()

        repeat(particles) {
            system.emit(
                x = launchX,
                y = GameResolution.height * 0.2f,
                lifetime = Random.nextFloat(0.8f, 1.5f),
                velocityX = Random.nextFloat(-400f, 400f),
                velocityY = Random.nextFloat(-400f, 400f),
                accelerationX = -50f,
                accelerationY = 50f,
                startScale = 0.6f,
                endScale = 0.1f,
                startAlpha = 1f,
                endAlpha = 0f,
                color = color
            )
        }
    }

    private companion object {
        const val TRAIL_PARTICLES = 10
        const val TRAIL_INTERVAL = 1f / 5f
        const val MAX_EXPLOSION_PARTICLES = 25

        val COLORS = listOf(
            Color4(1f, 0.84f, 0f),   // Gold
            Color4(1f, 0f, 0f),      // Red
            Color4(0f, 0.5f, 1f),    // Blue
            Color4(0f, 1f, 0f),      // Green
            Color4(0.8f, 0f, 1f),    // Purple
            Color4(1f, 0.5f, 0f),    // Orange
            Color4(1f, 1f, 1f)       // White
        )
    }
}

/**
 * Adds snowfall between December 15 and January 6, excluding the time of the fireworks.
 */
fun addSnowfallWithPeriod(container: UIContainer) {
    val calendar = Calendar.getInstance()
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    val hour = calendar.get(Calendar.HOUR_OF_DAY)

    val isFireworksPeriod = month == Calendar.JANUARY && day == 1 && hour < 20
    val isSnowfallPeriod = (month == Calendar.DECEMBER && day >= 15) || (month == Calendar.JANUARY && day <= 6)

    if (isSnowfallPeriod && !isFireworksPeriod) {
        container += Snowfall(OsuPlatform.implementation.isLowEndDevice)
    }
}

/**
 * Adds fireworks during the first 20 hours of January 1st.
 */
fun addFireworksWithPeriod(container: UIContainer) {
    val calendar = Calendar.getInstance()

    if (calendar.get(Calendar.MONTH) == Calendar.JANUARY
        && calendar.get(Calendar.DAY_OF_MONTH) == 1
        && calendar.get(Calendar.HOUR_OF_DAY) < 20) {
        container += Fireworks(OsuPlatform.implementation.isLowEndDevice)
    }
}
