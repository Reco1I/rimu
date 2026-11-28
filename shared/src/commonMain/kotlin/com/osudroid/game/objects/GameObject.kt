package com.osudroid.game.objects

import com.osudroid.beatmaps.HitWindow
import com.osudroid.beatmaps.hitobjects.HitObject
import com.osudroid.game.CursorEvent
import com.osudroid.game.GameplayContext
import com.osudroid.math.MathUtils
import com.osudroid.math.Vector2
import com.osudroid.game.HitResult
import com.osudroid.mods.ModRelax
import com.osudroid.settings.OsuSettings
import com.osudroid.ui.v2.game.GameplayPlayfield
import com.osudroid.ui.v2.skin.MainCirclePiece
import com.osudroid.ui.v2.skin.SkinComponent
import com.osudroid.ui.v2.skin.HitLightingPiece
import com.osudroid.ui.v2.skin.legacy.LegacySkin
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.settings.Settings
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.ui.ChoreographyStates
import com.reco1l.verktex.ui.latestChoreographyEndTime
import com.reco1l.verktex.ui.Component
import com.reco1l.verktex.ui.Proxy
import com.reco1l.verktex.ui.container.Container
import kotlin.math.max
import ru.nsu.ccfit.zuev.osu.Utils
import ru.nsu.ccfit.zuev.osu.scoring.Replay
import kotlin.jvm.JvmField

/**
 * A hit object of gameplay, as osu!lazer's `DrawableHitObject`: a container filling the layer it's added to, at its
 * origin, so its pieces keep the positions of the layer (osu!pixels in the playfield). Pieces that draw in another
 * layer than the object's (approach circles above every object, for example) are drawn there through a [Proxy], see
 * [drawIn].
 *
 * The game drives its judgement with [updateJudgement], in its own order. Its pieces are updated, animated and
 * drawn as the children of any container.
 */
abstract class GameObject : Container() {
    @JvmField
    protected var endsCombo = false
    @JvmField
    protected var autoPlay = false
    @JvmField
    protected var hitTime = 0f
    @JvmField
    protected var id = -1
    @JvmField
    protected var replayObjectData: Replay.ReplayObjectData? = null
    @JvmField
    protected var startHit = false
    /**
     * The position of this object in the playfield, in osu!pixels.
     */
    @JvmField
    protected var position = Vector2(0f)

    /**
     * What the gameplay of the play this object is in depends on, from its listener as it's initialized.
     */
    protected var context = GameplayContext()

    fun getReplayData(): Replay.ReplayObjectData? {
        return replayObjectData
    }

    fun setReplayData(replayObjectData: Replay.ReplayObjectData?) {
        this.replayObjectData = replayObjectData
    }

    fun setAutoPlay() {
        autoPlay = true
    }

    init {
        width = Dimension.FillAvailable
        height = Dimension.FillAvailable
    }

    /**
     * Judges the object and updates what follows its judgement, [dt] seconds after the previous update.
     */
    abstract fun updateJudgement(dt: Float)

    fun getHitTime(): Float {
        return hitTime
    }

    fun getId(): Int {
        return id
    }

    fun setId(id: Int) {
        this.id = id
    }

    fun getPosition(): Vector2 {
        return position
    }

    fun isStartHit(): Boolean {
        return startHit
    }

    open fun playLoopingSamples() {}

    open fun stopLoopingSamples() {}

    /**
     * Obtains the [CursorEvent] that hits this [GameObject], if any.
     *
     * @param listener The [GameObjectListener] to get cursors from.
     * @param hitObject The [HitObject] associated with this [GameObject].
     * @param objectElapsedTime The elapsed time since the start of this [GameObject], in seconds.
     * @return The [CursorEvent] that hits this [GameObject], or `null` if none.
     */
    protected fun getHittingCursor(listener: GameObjectListener, hitObject: HitObject,
                                   objectElapsedTime: Double): CursorEvent? {
        val cursorCount = listener.getCursorsCount()

        // For Autopilot, we only need to check if a cursor is pressed.
        if (context.isAutopilot) {
            for (i in 0 until cursorCount) {
                val cursor = listener.getCursor(i)
                val events = cursor.downEvents
                val size = events.size

                while (cursor.latestProcessedDownEventIndex < size) {
                    val event = events[cursor.latestProcessedDownEventIndex++]

                    if (canHit(event, hitObject.hitWindow!!)) {
                        return event
                    }
                }
            }

            return null
        }

        // For Relax, we need to check two cases:
        // 1. If a cursor flows over the object when it is hittable.
        // 2. If a cursor is pressed while it is over the object when it is hittable.
        if (context.isRelax) {
            for (i in 0 until cursorCount) {
                val cursor = listener.getCursor(i)
                val events = cursor.events
                val size = events.size

                if (size > 0) {
                    while (cursor.latestProcessedEventIndex < size) {
                        val event = events[cursor.latestProcessedEventIndex++]

                        if (event.isActionUp) {
                            continue
                        }

                        val isHit = isHit(hitObject, event)

                        // Case 1
                        if (event.isActionDown && isHit && canHit(event, hitObject.hitWindow!!)) {
                            return event
                        }

                        // Case 2
                        if (objectElapsedTime >= -ModRelax.RELAX_LENIENCY / 1000 && isHit) {
                            return event
                        }
                    }
                } else {
                    // If there are no new events, check the latest event.
                    val event = cursor.latestEvent

                    // Only consider case 2 in this scenario, as the event should logically be marked as a move event
                    // even if it's a down event (no new events mean the user keeps pressing on the same spot).
                    if (event != null && !event.isActionUp && objectElapsedTime >= -ModRelax.RELAX_LENIENCY / 1000 && isHit(hitObject, event)) {
                        return event
                    }
                }
            }

            return null
        }

        // In regular gameplay, we need to iteratively check whether one or more cursors is on the object while a cursor
        // is being pressed.
        for (i in 0 until cursorCount) {
            val cursor = listener.getCursor(i)
            val downEvents = cursor.downEvents
            val downEventsSize = downEvents.size

            // Consume down events that have not been processed yet, even those that are already past the hit threshold.
            // This ensures that start time ordered hit policy (aka "notelock") is enforced correctly.
            while (cursor.latestProcessedDownEventIndex < downEventsSize) {
                val downEvent = downEvents[cursor.latestProcessedDownEventIndex++]

                if (!canHit(downEvent, hitObject.hitWindow!!)) {
                    continue
                }

                for (j in 0 until cursorCount) {
                    if (i == j) {
                        // For the cursor that generated the down event, check if the event position is on the object.
                        if (isHit(hitObject, downEvent)) {
                            return downEvent
                        }
                    } else {
                        // For the other case, we need to check the event closest to the down event and check if it is
                        // on the object.
                        val otherCursor = listener.getCursor(j)
                        val closestEvent = otherCursor.getClosestEventBefore(downEvent.systemTime)

                        if (closestEvent == null || closestEvent.isActionUp) {
                            continue
                        }

                        if (isHit(hitObject, closestEvent)) {
                            return downEvent
                        }
                    }
                }
            }
        }

        return null
    }

    /**
     * Determines whether a [CursorEvent] can hit this [GameObject].
     *
     * @param cursorEvent The [CursorEvent] to check.
     * @param hitWindow The [HitWindow] of this [GameObject].
     * @return `true` if the [CursorEvent] can hit this [GameObject], `false` otherwise.
     */
    private fun canHit(cursorEvent: CursorEvent, hitWindow: HitWindow): Boolean {
        if (cursorEvent.isActionUp) {
            return false
        }

        val hittableRange = if (context.isAutopilot)
            MathUtils.clamp(hitWindow.mehWindow + 100, 200.0, HitWindow.MISS_WINDOW).toFloat() / 1000
        else
            HitWindow.MISS_WINDOW.toFloat() / 1000

        return cursorEvent.hitTime / 1000 >= (hitTime - hittableRange).toDouble()
    }

    /**
     * Determines whether a [CursorEvent] hits this [GameObject]. The determination is solely based on
     * distance, not considering timing.
     *
     * @param hitObject The [HitObject] associated with this [GameObject].
     * @param cursorEvent The [CursorEvent] to check.
     * @return `true` if the [CursorEvent] hits this [GameObject], `false` otherwise.
     */
    private fun isHit(hitObject: HitObject, cursorEvent: CursorEvent): Boolean {
        if (cursorEvent.isActionUp) {
            return false
        }

        return Utils.squaredDistance(position, cursorEvent.playfieldPosition) <= Utils.sqr(hitObject.gameplayRadius.toFloat())
    }

    //region Proxies

    private val proxies = ArrayList<Proxy>()
    private val freeProxies = ArrayList<Proxy>()

    /**
     * Draws [piece], one of this object's, in [layer] instead of in this object, through a [Proxy] added on top of
     * the layer. It's removed by [removeProxies].
     */
    protected fun drawIn(layer: Container, piece: Component) {
        val proxy = freeProxies.removeLastOrNull() ?: Proxy()
        proxy.target = piece
        layer += proxy
        proxies += proxy
    }

    /**
     * Removes the proxies [drawIn] added, giving the pieces back to this object.
     */
    private fun removeProxies() {
        for (i in proxies.indices) {
            val proxy = proxies[i]
            proxy.removeSelf()
            proxy.target = null
            freeProxies += proxy
        }
        proxies.clear()
    }

    //endregion

    //region Pieces

    // The pieces borrowed from the pool, given back as the object expires, and the component each is of.
    private val ownedComponents = ArrayList<SkinComponent<*>>()
    private val ownedPieces = ArrayList<Any>()

    /**
     * A piece of [component] for this object, borrowed from the [PiecePool] until it expires: it's given back with
     * the others, taken out of its parent, by [releasePieces].
     */
    protected fun <T : Any> obtain(component: SkinComponent<T>): T = own(component, PiecePool.instance.obtain(component))

    /**
     * Same as [obtain], null if no skin draws [component].
     */
    protected fun <T : Any> obtainOrNull(component: SkinComponent<T>): T? =
        PiecePool.instance.obtainOrNull(component)?.let { own(component, it) }

    private fun <T : Any> own(component: SkinComponent<T>, piece: T): T {
        ownedComponents += component
        ownedPieces += piece
        return piece
    }

    /**
     * Gives back every piece this object borrowed, its judgements' included, and the proxies drawing them. Called
     * as it expires, once: the object isn't used afterwards.
     */
    protected fun releasePieces() {
        removeProxies()

        val pool = PiecePool.instance
        for (i in ownedPieces.indices) {
            pool.release(ownedComponents[i], ownedPieces[i])
        }
        ownedComponents.clear()
        ownedPieces.clear()
        lighting = null
    }

    //endregion

    //region Judgement effects

    private var lighting: HitLightingPiece? = null
    private var isLightingLookedUp = false

    /**
     * Shows the judgement with the texture [textureName] (`hit300`, `hit0`, `sliderpoint30`…) at ([x], [y]) in this
     * object, for an object of the given [scale], now: the judgement, drawn above every object, its particles and its
     * hit lighting, in [color]. The particles are thrown in the playfield, at ([particleX], [particleY]) and the
     * given [particleScale]. A [spinnerMiss] only fades. This object lives until they end.
     */
    protected fun showJudgement(
        playfield: GameplayPlayfield,
        textureName: String,
        x: Float,
        y: Float,
        scale: Float,
        color: Color4?,
        particleX: Float = x,
        particleY: Float = y,
        particleScale: Float = scale,
        spinnerMiss: Boolean = false
    ) {
        val time = timing.time
        val result = HitResult.fromTexture(textureName)

        playfield.particles.emit(particleX, particleY, textureName, particleScale)

        if (result != HitResult.Miss && result != HitResult.SliderPoint && result != HitResult.Tick &&
            Settings[OsuSettings.HitLighting]) {

            if (!isLightingLookedUp) {
                // Only texture skins have it.
                lighting = obtainOrNull(SkinComponent.HitLighting)
                isLightingLookedUp = true
            }

            val lighting = lighting
            if (lighting != null) {
                if (lighting.parent == null) {
                    this += lighting
                    drawIn(playfield.underlay, lighting)
                }
                lighting.play(x, y, color, scale, time)
            }
        }

        val piece = obtain(PiecePool.instance.judgement(textureName))
        this += piece
        drawIn(playfield.judgements, piece)

        piece.play(x, y, result, scale, time, spinnerMiss)
    }

    /**
     * Whether hit circles, slider ends and reverse arrows burst when they're hit: the setting, unless Hidden or
     * Traceable hide them.
     */
    protected fun areBurstsShown(): Boolean {
        return Settings[OsuSettings.BurstEffects] &&
            !context.fadesOutObjects &&
            !context.isTraceable
    }

    /**
     * Plays [piece], one of this object's, being hit at the given [scale] now, drawn above every object as the
     * judgements are: a circle as its skin plays it ([MainCirclePiece.playHit]), any other piece (a reverse arrow)
     * bursting as osu!stable's do. This object lives until it ends.
     */
    protected fun burst(playfield: GameplayPlayfield, piece: Component, scale: Float) {
        if (piece.parent == null) {
            this += piece
        }
        drawIn(playfield.judgements, piece)

        piece.clearAnimations()
        piece.scale = scale
        piece.alpha = 1f

        val time = timing.time
        if (piece is MainCirclePiece) piece.playHit(scale, time) else LegacySkin.burst(piece, scale, time)
    }

    //endregion

    //region State

    /**
     * What a hit object shows of its judgement, as osu!lazer's `ArmedState`.
     */
    enum class ArmedState {
        /**
         * Not judged yet.
         */
        Idle,

        /**
         * Hit, with any result but a miss.
         */
        Hit,

        /**
         * Missed.
         */
        Miss
    }

    /**
     * The state of this object, set as it's judged, see [applyState].
     */
    var state = ArmedState.Idle
        private set

    /**
     * Plays what this object shows in [state] from [time], see [ChoreographyStates]. Called by [applyState].
     */
    protected open fun playState(state: ArmedState, time: Float) {}

    /**
     * Sets the [state] of this object and plays it from [time], now by default.
     */
    protected fun applyState(state: ArmedState, time: Float = timing.time) {
        this.state = state
        playState(state, time)
    }

    //endregion

    //region Lifetime management

    /**
     * Whether the underlying [HitObject] of this [GameObject] has been judged in its entirety, including
     * nested [HitObject]s.
     */
    open fun isJudged(): Boolean {
        return false
    }

    /**
     * Whether the underlying [HitObject] got its final judgement, which [isJudged] may not wait for (sliders and
     * spinners without their lock). The object can stay alive afterwards, until its effects end.
     */
    open fun hasFinalJudgement(): Boolean {
        return isJudged()
    }

    /**
     * Called when this [GameObject]'s lifetime expires, once: it gives back its pieces and isn't used afterwards.
     */
    open fun onExpire() {}

    // The least time the lifetime ends at, set once the object is judged. NaN before.
    private var judgedLifetimeEnd = Float.NaN

    /**
     * Ends the lifetime of this object at [time] or once everything it and its pieces play ends, whichever is
     * later, see [lifetimeEnd].
     */
    protected fun endLifetime(time: Float) {
        judgedLifetimeEnd = time
    }

    /**
     * The time in seconds since the beatmap started at which this [GameObject]'s lifetime ends: never before it's
     * judged ([endLifetime]), then once what it and its pieces play in their choreographies ends, as osu!lazer's
     * `LatestTransformEndTime`.
     */
    val lifetimeEnd: Float
        get() = if (judgedLifetimeEnd.isNaN()) Float.MAX_VALUE else max(judgedLifetimeEnd, latestChoreographyEndTime())

    //endregion
}
