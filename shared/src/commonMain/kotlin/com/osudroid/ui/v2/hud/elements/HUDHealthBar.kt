package com.osudroid.ui.v2.hud.elements

import com.osudroid.scoring.Statistic
import com.osudroid.ui.v2.hud.HUDElement
import com.osudroid.ui.v2.skin.SkinComponent
import com.osudroid.ui.v2.skin.SkinPieces
import com.osudroid.ui.v2.hud.GameplayState

/**
 * The health bar, drawn by the skin's piece.
 */
class HUDHealthBar : HUDElement() {

    private val piece = SkinPieces.current.create(SkinComponent.HealthBar)

    init {
        +piece
    }

    override fun onGameplayUpdate(game: GameplayState, secondsElapsed: Float) {
        piece.update(game.stat.hp, secondsElapsed)
    }

    override fun onNoteHit(statistics: Statistic) {
        piece.flash(statistics.hp)
    }

    override fun onBreakStateChange(isBreak: Boolean) {
        isVisible = !isBreak
    }
}
