package com.osudroid.ui.v2.hud

import com.reco1l.verktex.ui.clearTransforms
import com.reco1l.verktex.ui.fadeTo

import com.osudroid.math.Vector2
import com.osudroid.ui.v2.hud.elements.*
import com.reco1l.verktex.ui.container.UIContainer
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Vec2
import com.osudroid.ui.v2.hud.editor.HUDElementOverlay
import com.osudroid.beatmaps.constants.HitObjectType
import com.osudroid.beatmaps.hitobjects.HitObject
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.ui.shape.UILine
import com.reco1l.verktex.data.px
import com.reco1l.verktex.input.InputEvent
import com.reco1l.verktex.input.PointerEvent
import kotlin.math.abs
import kotlin.reflect.KClass
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.game.GameScene
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2


/**
 * List all the elements that can be added to the HUD.
 */
@Suppress("EnumEntryName")
enum class HUDElements(val type: KClass<out HUDElement>) {

    accuracy_counter(HUDAccuracyCounter::class),
    combo_counter(HUDComboCounter::class),
    health_bar(HUDHealthBar::class),
    pie_song_progress(HUDPieSongProgress::class),
    pp_counter(HUDPPCounter::class),
    score_counter(HUDScoreCounter::class),
    ur_counter(HUDUnstableRateCounter::class),
    avg_offset_counter(HUDAverageOffsetCounter::class),
    hit_error_meter(HUDBarHitErrorMeter::class),
    color_hit_error_meter(HUDColorHitErrorMeter::class),
    aim_error_meter(HUDAimErrorMeter::class),
    linear_song_progress(HUDLinearSongProgress::class),
    great_counter(HUDGreatCounter::class),
    ok_counter(HUDOkCounter::class),
    meh_counter(HUDMehCounter::class),
    miss_counter(HUDMissCounter::class),
    bpm_counter(HUDBPMCounter::class),
    notes_per_second_counter(HUDNotesPerSecondCounter::class),
    taps_per_second_counter(HUDTapsPerSecondCounter::class),
    back_button(HUDBackButton::class),
    leaderboard(HUDLeaderboard::class);

    companion object {
        operator fun get(type: KClass<out HUDElement>) = entries.first { it.type == type }
        operator fun get(name: String) = entries.find { it.name == name }
    }
}


abstract class HUDElement : UIContainer(), IGameplayEvents {

    /**
     * Returns the name of this element.
     */
    open val name
        get() = HUDElements[this::class].name.replace('_', ' ').capitalize()

    /**
     * Indicates whether the element is selected.
     */
    val isSelected
        get() = (parent as? GameplayHUD)?.selected == this

    /**
     * Whether this [HUDElement] should be shown.
     */
    open val shouldBeShown = !Config.isHideInGameUI()

    /**
     * The overlay for this element to be used in edit mode.
     */
    var editorOverlay: HUDElementOverlay? = null

    /**
     * The restore data of the element.
     */
    var restoreData: HUDElementSkinData? = null


    protected var isInEditMode = false
        private set


    private var connectionLine: UILine? = null


    /**
     * The position of this element relative to its anchor, in pixels.
     */
    var position = Vec2.Zero
        set(value) {
            field = value
            x = value.x.px
            y = value.y.px
        }

    /**
     * The point of the parent this element is anchored to, in pixels.
     */
    val anchoredPosition: Vec2
        get() {
            val parentBounds = parent?.bounds ?: return Vec2.Zero
            return Vec2(parentBounds.width * anchor.x, parentBounds.height * anchor.y)
        }

    /**
     * The size of this element accounting for its scale, in pixels.
     */
    val transformedSize
        get() = measuredSize * scale


    //region Skinning

    /**
     * Sets the skin data of the element.
     */
    open fun setSkinData(data: HUDElementSkinData?) {
        if (data == null) {
            return
        }

        position = data.position
        anchor = data.anchor
        origin = data.origin
        scale = data.scale
        scaleOrigin = data.origin

        // When the element is restored it's usually selected so we need to update the connection line.
        if (isSelected) {
            editorOverlay?.updateOutline()
            updateConnectionLine()
        }
    }

    /**
     * Creates a [HUDElementSkinData] object with the current element's skin data.
     */
    open fun getSkinData() = HUDElementSkinData(
        type = this::class,
        anchor = anchor,
        origin = origin,
        scale = scale,
        position = position
    )

    /**
     * Restores the element to its original state before any changes were made.
     */
    fun restore() {
        setSkinData(restoreData)
    }

    //endregion

    //region Element events

    /**
     * Sets the edit mode of the element.
     */
    open fun setEditMode(value: Boolean) {
        isInEditMode = value

        if (value) {
            backgroundColor = Color4(0x29F27272).alpha(0.15f)
            editorOverlay = HUDElementOverlay(this)

            parent!! += editorOverlay!!
        } else {
            connectionLine?.removeSelf()
            editorOverlay?.removeSelf()

            backgroundColor = Color4.Transparent
            connectionLine = null
            editorOverlay = null
        }
    }

    /**
     * Called when the selection state of the element changes.
     */
    open fun onSelectionStateChange(isSelected: Boolean) {

        backgroundColor = backgroundColor.alpha(if (isSelected) 0.5f else 0.15f)

        editorOverlay?.clearTransforms()
        editorOverlay?.fadeTo(if (isSelected) 1f else 0f, 0.1f)

        if (isSelected) {
            updateConnectionLine()
        } else {
            wasSelected = false
        }

        connectionLine?.clearTransforms()
        connectionLine?.fadeTo(if (isSelected) 1f else 0f, 0.1f)
    }

    //endregion

    //region Gameplay events

    override fun onGameplayUpdate(gameScene: GameScene, secondsElapsed: Float) {}

    override fun onGameplayTouchDown(time: Float) {}

    override fun onHitObjectLifetimeStart(obj: HitObject) {}

    override fun onNoteHit(statistics: StatisticV2) {}

    override fun onBreakStateChange(isBreak: Boolean) {}

    override fun onAccuracyRegister(type: HitObjectType, accuracy: Float) {}

    override fun onAimJudgement(objectPosition: Vector2, cursorPosition: Vector2, objectRadius: Float) {}

    override fun onSeek() {}

    //endregion

    //region Entity events

    private var initialX = 0f
    private var initialY = 0f

    private var isDragging = false
    private var wasSelected = false
    private var wasMoved = false


    override fun onInputEvent(event: InputEvent): Boolean {

        if (!isInEditMode) {
            return super.onInputEvent(event)
        }

        if (event !is PointerEvent) {
            return false
        }

        val hud = parent as GameplayHUD

        when (event.action) {
            PointerEvent.Action.Down -> {
                if (!containsScenePoint(event.x, event.y)) {
                    return false
                }

                initialX = event.x
                initialY = event.y

                isDragging = true
                wasMoved = false
                wasSelected = hud.selected == this

                hud.selected = this
                return true
            }

            PointerEvent.Action.Move -> {
                // Preventing from moving the element if it's not selected.
                if (isDragging && isSelected) {
                    move(event.x - initialX, event.y - initialY)
                    wasMoved = true

                    initialX = event.x
                    initialY = event.y
                    return true
                }
            }

            PointerEvent.Action.Up -> {
                if (!isDragging) {
                    return false
                }
                isDragging = false

                if (wasSelected && isSelected && !wasMoved) {
                    hud.forEachElement { element ->

                        if (element != this && element.containsScenePoint(event.x, event.y)) {
                            hud.selected = element
                            return@forEachElement
                        }
                    }
                }
                return true
            }

            else -> isDragging = false
        }

        return false
    }

    //endregion

    //region Edit mode

    private fun applyClosestAnchorOrigin() {

        val drawSize = transformedSize
        val drawPosition = anchoredPosition + position - drawSize * origin
        val parentDrawSize = parent?.measuredSize ?: return

        val relativeTopLeft = drawPosition / parentDrawSize
        val relativeTopRight = (drawPosition + Vec2(drawSize.x, 0f)) / parentDrawSize
        val relativeBottomRight = (drawPosition + drawSize) / parentDrawSize
        val relativeBottomLeft = (drawPosition + Vec2(0f, drawSize.y)) / parentDrawSize

        val closest = Anchor.all.minBy {
            minOf(
                abs(relativeTopLeft.distance(it)),
                abs(relativeTopRight.distance(it)),
                abs(relativeBottomRight.distance(it)),
                abs(relativeBottomLeft.distance(it))
            )
        }

        if (anchor != closest) {
            val previousAnchorOffset = anchoredPosition
            anchor = closest
            position -= anchoredPosition - previousAnchorOffset
        }

        if (origin != closest) {
            val previousOriginOffset = -(transformedSize * origin)
            val originOffset = -(transformedSize * closest)
            origin = closest
            scaleOrigin = closest
            position -= originOffset - previousOriginOffset
        }
    }

    /**
     * Updates the line connecting this element with its anchor.
     */
    fun updateConnectionLine() {


        if (connectionLine == null) {
            connectionLine = UILine().apply {
                color = Color4(0xFFF27272)
                lineWidth = 10f.px
                alpha = 0f
            }
            parent!! += connectionLine!!
        }

        connectionLine!!.fromPoint = anchoredPosition
        connectionLine!!.toPoint = (editorOverlay?.outlinePosition ?: Vec2.Zero) + transformedSize * origin
    }


    override fun onDetached() {
        super.onDetached()

        editorOverlay?.removeSelf()
        connectionLine?.removeSelf()
    }

    /**
     * Moves the element by the specified delta.
     */
    fun move(deltaX: Float, deltaY: Float) {
        position += Vec2(deltaX, deltaY)
        applyClosestAnchorOrigin()
        updateConnectionLine()
    }

    /**
     * Removes the element from the HUD.
     */
    fun remove() {
        setEditMode(false)
        removeSelf()
    }

    //endregion
}
