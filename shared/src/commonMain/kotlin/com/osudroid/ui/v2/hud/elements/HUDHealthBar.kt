package com.osudroid.ui.v2.hud.elements

import com.osudroid.ui.v2.hud.HUDElement
import com.osudroid.ui.v2.skin.SkinComponent
import com.osudroid.ui.v2.skin.SkinPieces
import ru.nsu.ccfit.zuev.osu.game.GameScene
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2

/**
 * The health bar, drawn by the skin's piece.
 */
class HUDHealthBar : HUDElement() {

    private val piece = SkinPieces.current.create(SkinComponent.HealthBar)

    init {
        +piece
    }

    override fun onGameplayUpdate(game: GameScene, secondsElapsed: Float) {
        piece.update(game.stat.hp, secondsElapsed)
    }

    override fun onNoteHit(statistics: StatisticV2) {
        piece.flash(statistics.hp)
    }

    override fun onBreakStateChange(isBreak: Boolean) {
        isVisible = !isBreak
    }
}
