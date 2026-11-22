package com.osudroid.ui.v2

import com.osudroid.BeatmapSelection
import com.osudroid.data.BeatmapInfo
import com.osudroid.settings.OsuSettings
import com.osudroid.utils.async
import com.reco1l.verktex.Platform
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.data.Background
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.ScaleType
import com.reco1l.verktex.data.fill
import com.reco1l.verktex.graphics.Texture
import com.reco1l.verktex.settings.Settings
import com.reco1l.verktex.texture.Textures
import com.reco1l.verktex.ui.CrossfadeSprite
import com.reco1l.verktex.ui.Transform
import com.reco1l.verktex.ui.clearTransforms
import com.reco1l.verktex.ui.container.Container
import com.reco1l.verktex.ui.fadeTo
import com.reco1l.verktex.ui.shape.Box
import kotlinx.coroutines.Job

/**
 * The background of the whole game, the engine's ([Verktex.background]): the background of the
 * selected beatmap ([BeatmapSelection]), or the skin's `menu-background` without one or with safe
 * backgrounds, crossfading as the selection changes whatever scene is shown. Scenes don't draw
 * their own, they set how bright it is ([fadeBrightness]) and draw over it.
 *
 * It's dimmed by its alpha, over the black of the window, rather than covered with black, which
 * would cost a fullscreen layer, and a fully dimmed background isn't drawn at all.
 */
object BeatmapBackground : Container() {

    /**
     * How long the fade to another background takes by default, in seconds, see [crossfadeDuration].
     */
    const val CROSSFADE_DURATION = 0.5f

    /**
     * How long [fadeBrightness] takes by default, in seconds.
     */
    const val BRIGHTNESS_DURATION = 0.3f

    // Shown where there's no texture, or around one that doesn't fill the window.
    private val fallback = Box().apply {
        width = fill()
        height = fill()
        color = Color4.Black
    }

    private val sprite = CrossfadeSprite().apply {
        width = fill()
        height = fill()
        scaleType = ScaleType.Crop
    }

    // The background path shown or being loaded, null for the skin's.
    private var shownPath: String? = null
    private var isShown = false
    private var loadJob: Job? = null

    // The texture key the next background is loaded under. It alternates as the crossfade sprite
    // does, so the background still shown while fading isn't released.
    private var keyIndex = 0


    /**
     * How long the fade to the background of another selection takes, in seconds. The main menu
     * fades slower while it's shown.
     */
    var crossfadeDuration = CROSSFADE_DURATION

    /**
     * How the background is scaled to the window, cropped by default.
     */
    var scaleType: ScaleType
        get() = sprite.scaleType
        set(value) {
            sprite.scaleType = value
        }

    /**
     * The color shown where there's no texture, or around one that doesn't fill the window, e.g.
     * the one of a beatmap's events. Black by default.
     */
    var fallbackColor: Color4
        get() = fallback.color
        set(value) {
            fallback.color = value
        }


    init {
        width = fill()
        height = fill()
        +fallback
        +sprite
    }


    /**
     * Makes this the engine's background, which [com.osudroid.OsuGame.start] does: it shows the
     * selected beatmap's background, and follows the selection from then on.
     */
    fun install() {
        loadJob?.cancel()
        isShown = false
        shownPath = null

        // Its textures don't outlive the engine, the sprite may still have the ones of a previous start.
        sprite.crossfadeTo(null, 0f)
        alpha = 1f

        Verktex.background = Background.Component(this)

        BeatmapSelection.addListener(this) { show(it) }
        show(BeatmapSelection.beatmap, 0f)
    }

    /**
     * Fades to the background of [beatmap], or the skin's without it or with safe backgrounds, in
     * [duration] seconds. Nothing changes if it's the one shown. The texture is decoded off the
     * update thread, so a selection doesn't freeze the game.
     */
    fun show(beatmap: BeatmapInfo?, duration: Float = crossfadeDuration) {
        val path = beatmap?.backgroundPath?.takeUnless { Settings[OsuSettings.SafeBeatmapBackground] }

        if (isShown && path == shownPath) {
            return
        }
        isShown = true
        shownPath = path

        loadJob?.cancel()

        if (path == null) {
            Verktex.scheduleOnDraw { crossfadeTo(SkinTextures["menu-background"], duration) }
            return
        }

        loadJob = async {
            val source = Platform.provider.createTextureSource(path, true)

            Verktex.scheduleOnDraw {
                // A later selection may have been shown meanwhile.
                if (shownPath != path) {
                    return@scheduleOnDraw
                }
                val region = source?.let { Textures.load("::background-$keyIndex", it) }
                crossfadeTo(region ?: SkinTextures["menu-background"], duration)
            }
        }
    }

    /**
     * Shows the background of the selection again, e.g. after the safe backgrounds setting or the
     * skin changed.
     */
    fun refresh() {
        isShown = false
        show(BeatmapSelection.beatmap)
    }

    private fun crossfadeTo(region: Texture.Region?, duration: Float) {
        sprite.crossfadeTo(region, duration)
        keyIndex = 1 - keyIndex
    }

    /**
     * Fades how bright the background is to [brightness], from 0 (black) to 1, in [duration]
     * seconds. Each scene sets the one it's drawn over when it's entered.
     */
    fun fadeBrightness(brightness: Float, duration: Float = BRIGHTNESS_DURATION): Transform {
        clearTransforms()
        return fadeTo(brightness, duration)
    }

    /**
     * Sets how bright the background is at once, stopping a [fadeBrightness], e.g. as gameplay
     * dims it every frame through a break.
     */
    fun setBrightness(brightness: Float) {
        clearTransforms()
        alpha = brightness
    }

    /**
     * The texture shown, or being faded to, null for none.
     */
    val textureRegion: Texture.Region?
        get() = sprite.textureRegion
}
