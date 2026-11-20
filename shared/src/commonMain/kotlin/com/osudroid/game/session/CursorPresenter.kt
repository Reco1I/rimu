package com.osudroid.game.session

import com.osudroid.game.Cursor
import com.osudroid.game.CursorEvent
import com.osudroid.game.GameplayContext
import com.osudroid.game.PlayfieldMapping
import com.osudroid.ui.v2.game.AutoCursor
import com.osudroid.ui.v2.game.CursorEntity
import com.osudroid.ui.v2.game.FlashLightEntity
import com.reco1l.verktex.ui.container.Container
import ru.nsu.ccfit.zuev.osu.Utils

/**
 * What a play shows of its [cursors]: their sprites, the cursor of autoplay and autopilot, and Flashlight's light,
 * which follows the main cursor.
 *
 * @param layer The layer the cursors and the light are added to.
 * @param mapping The mapping of the playfield, where the cursor of autoplay starts at the center of.
 * @param showsCursors Whether the cursors of the player, or of the replay, are shown.
 */
class CursorPresenter(
    private val cursors: Array<Cursor?>,
    private val context: GameplayContext,
    layer: Container,
    mapping: PlayfieldMapping,
    showsCursors: Boolean
) {

    // The sprites of the cursors, null when they aren't shown.
    private val cursorSprites: Array<CursorEntity>? = if (showsCursors) Array(cursors.size) { CursorEntity() } else null

    /**
     * The cursor of autoplay and autopilot, null for the player's.
     */
    val autoCursor: AutoCursor?

    /**
     * Flashlight's light, null without it.
     */
    val flashlight: FlashLightEntity?

    // The cursor the light follows, -1 for none.
    private var mainCursorId = -1


    init {
        cursorSprites?.forEach { it.attachToScene(layer) }

        autoCursor = if (context.isAutoplay || context.isAutopilot) {
            // From the center of the playfield.
            val start = mapping.toResolution(PlayfieldMapping.PLAYFIELD_WIDTH / 2, PlayfieldMapping.PLAYFIELD_HEIGHT / 2)
            AutoCursor(start.x, start.y).also { it.attachToScene(layer) }
        } else null

        // Below everything else of the layer.
        flashlight = context.flashlight?.let { FlashLightEntity(it).also { light -> layer.add(0, light) } }
    }


    /**
     * Updates the cursors and the light for a frame [deltaTime] seconds long.
     *
     * @param replaying Whether a replay is watched: its cursors are where it moves them.
     * @param combo The combo of the play, which sizes the light.
     */
    fun update(deltaTime: Float, replaying: Boolean, combo: Int) {
        if (autoCursor != null) {
            autoCursor.update(deltaTime)
        } else if (cursorSprites != null) {
            for (i in cursorSprites.indices) {
                val sprite = cursorSprites[i]
                sprite.update(deltaTime)

                val cursor = cursors[i]!!
                val latestEvent = cursor.latestEvent

                if (replaying && latestEvent != null) {
                    sprite.setPosition(latestEvent.position.x, latestEvent.position.y)
                    sprite.setShowing(!latestEvent.isActionUp)
                }

                sprite.setPressed(cursor.getLatestNonUpEvent() != null)
            }
        }

        if (flashlight != null) {
            if (autoCursor == null) {
                // Check if the main cursor is still valid.
                if (mainCursorId >= 0 && !cursors[mainCursorId]!!.mouseDown) {
                    mainCursorId = -1
                }

                // If no cursor is valid, check for the latest pressed cursor.
                if (mainCursorId < 0) {
                    var index = -1
                    var latestDownEvent: CursorEvent? = null

                    for (i in cursors.indices) {
                        val c = cursors[i]!!
                        val latestCursorDownEvent = c.getLatestDownEvent() ?: continue

                        if (latestDownEvent == null || latestDownEvent.systemTime < latestCursorDownEvent.systemTime) {
                            latestDownEvent = latestCursorDownEvent
                            index = i
                        }
                    }

                    if (latestDownEvent != null) {
                        mainCursorId = index
                    }
                }

                if (mainCursorId != -1) {
                    followLatestEvent()
                }
            }

            flashlight.updateCombo(combo)
        }
    }

    /**
     * The sprite of the cursor [id], null when the cursors aren't shown.
     */
    fun sprite(id: Int): CursorEntity? = cursorSprites?.get(id)

    /**
     * The cursor pressed nearest to [x] and [y], in the playfield, -1 for none.
     */
    fun nearestCursor(x: Float, y: Float): Int {
        var nearestDistance = Float.POSITIVE_INFINITY
        var id = -1

        for (i in cursors.indices) {
            val latestEvent = cursors[i]!!.getLatestNonUpEvent()

            if (latestEvent != null) {
                val distance = Utils.squaredDistance(x, y, latestEvent.playfieldPosition.x, latestEvent.playfieldPosition.y)

                if (distance < nearestDistance) {
                    id = i
                    nearestDistance = distance
                }
            }
        }

        return id
    }

    /**
     * Makes the light follow the cursor [id], which hit an object.
     */
    fun followCursor(id: Int) {
        mainCursorId = id
        followLatestEvent()
    }

    private fun followLatestEvent() {
        val latestNonUpEvent = cursors[mainCursorId]!!.getLatestNonUpEvent()

        if (latestNonUpEvent != null) {
            flashlight!!.onMouseMove(latestNonUpEvent.position.x, latestNonUpEvent.position.y)
        }
    }
}
