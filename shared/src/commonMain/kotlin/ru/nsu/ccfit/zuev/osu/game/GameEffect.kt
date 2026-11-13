package ru.nsu.ccfit.zuev.osu.game

import com.osudroid.math.Vector2
import com.osudroid.ui.v2.skinTexture
import com.osudroid.ui.v2.skinFrames
import com.osudroid.utils.updateThread
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.graphics.BlendMode
import com.reco1l.verktex.ui.ChoreographyBlock
import com.reco1l.verktex.ui.UIAnimatedSprite
import com.reco1l.verktex.ui.UISprite
import com.reco1l.verktex.ui.choreographAt
import com.reco1l.verktex.ui.choreographNow
import com.reco1l.verktex.ui.choreography
import com.reco1l.verktex.ui.container.UIContainer
import ru.nsu.ccfit.zuev.skins.OsuSkin
import kotlin.jvm.JvmField

class GameEffect(texname: String) : GameObject() {
    @JvmField
    var hit: UISprite
    var texname: String = texname
        private set

    init {
        if (isAnimationEffect(texname)) {
            // Animated or not depending on the skin, which may change while the effect is pooled.
            val hit = UIAnimatedSprite(skinFrames(texname, orStatic = true), OsuSkin.get().animationFramerate)
            hit.isLoop = false
            this.hit = hit
        } else {
            hit = UISprite()
            hit.texture = skinTexture(texname)
        }
    }

    fun setColor(color: Color4?) {
        if (color == null) {
            hit.setColor(1f, 1f, 1f)
        } else {
            hit.color = color
        }
    }

    fun init(scene: UIContainer, pos: Vector2, scale: Float,
             vararg sequenceConsumers: ChoreographyBlock) {
        val hit = hit
        if (hit is UIAnimatedSprite) {
            hit.stop()
            hit.play()
        }
        hit.setPosition(pos.x, pos.y)
        hit.origin = Anchor.Center
        hit.scale = scale
        hit.alpha = 1f
        hit.removeSelf()
        hit.blendMode = BlendMode.Alpha
        scene += hit

        hit.clearAnimations()

        for (i in sequenceConsumers.indices) {
            hit.choreographNow(sequenceConsumers[i])
        }

        // Fired once every sequence has finished.
        hit.choreographAt(hit.choreography().endTime) { sequence ->
            sequence.after {
                updateThread {
                    hit.removeSelf()
                    hit.clearAnimations()
                    GameObjectPool.getInstance().putEffect(this@GameEffect)
                }
            }
        }
    }

    fun setBlendMode(blendMode: BlendMode) {
        hit.blendMode = blendMode
    }

    override fun update(dt: Float) {
    }

    companion object {
        private val animationEffects = HashSet(listOf(
            "hit0", "hit50", "hit100", "hit100k", "hit300", "hit300k", "hit300g"
        ))

        private fun isAnimationEffect(textureName: String): Boolean {
            return animationEffects.contains(textureName)
        }
    }
}
