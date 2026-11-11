package com.osudroid.ui.v2.hud

import com.osudroid.OsuGame
import com.osudroid.ui.v2.SkinFolders
import com.osudroid.utils.LenientJson
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

import com.reco1l.verktex.Verktex
import com.reco1l.verktex.input.InputEvent
import com.reco1l.verktex.input.PointerEvent
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.ui.UIComponent
import com.reco1l.verktex.data.Vec2

import com.osudroid.math.Vector2
import com.osudroid.resources.Res.string
import com.osudroid.ui.v2.hud.elements.HUDAccuracyCounter
import com.osudroid.ui.v2.hud.elements.HUDPieSongProgress
import com.osudroid.ui.v2.hud.elements.HUDScoreCounter
import com.reco1l.verktex.ui.container.UIContainer
import com.osudroid.ui.v2.hud.editor.HUDElementSelector
import com.reco1l.verktex.ui.dialog.UIMessageDialog
import com.osudroid.utils.updateThread
import com.reco1l.verktex.data.Dimension
import com.reco1l.framework.forEachTolerant
import com.osudroid.beatmaps.constants.HitObjectType
import com.osudroid.beatmaps.hitobjects.HitObject
import com.reco1l.verktex.ui.toast.Toasts
import ru.nsu.ccfit.zuev.osu.game.GameScene
import ru.nsu.ccfit.zuev.osu.scoring.*
import ru.nsu.ccfit.zuev.skins.OsuSkin
import ru.nsu.ccfit.zuev.skins.SkinJsonReader
import java.io.File
import kotlin.reflect.*

class GameplayHUD(
    /**
     * The play this HUD shows.
     */
    val game: GameScene
) : UIContainer(), IGameplayEvents {

    /**
     * The currently selected element.
     */
    var selected: HUDElement? = null
        set(value) {
            if (field != value) {
                field = value
                forEachElement { it.onSelectionStateChange(it == value) }

                // Move to front
                if (value != null) {
                    bringToFront(value)

                    // Preserve overlay on top of the element.
                    value.editorOverlay?.let(::bringToFront)

                    elementSelector?.collapse()
                }
            }
        }

    /**
     * The element selector used in edit mode.
     */
    var elementSelector: HUDElementSelector? = null
        private set


    private var isInEditMode = false

    private var pendingDefaultLayout = false


    init {
        width = Dimension.FillAvailable
        height = Dimension.FillAvailable
    }


    override fun onUpdate(clock: FrameClock) {
        if (Verktex.scene != game.scene) {
            // Detach on the next tick to ensure that parent's entity list stays stable throughout this tick.
            updateThread { removeSelf() }
            return
        }

        super.onUpdate(clock)

        if (pendingDefaultLayout) {
            applyDefaultLayout()
        }
    }


    private fun bringToFront(component: UIComponent) {
        if (children.remove(component)) {
            children.add(component)
        }
    }

    /**
     * Checks if the HUD has an element of the specified type.
     */
    fun hasElement(type: KClass<out HUDElement>): Boolean {
        return children.any { type.isInstance(it) }
    }

    /**
     * Adds an element to the HUD.
     */
    fun addElement(data: HUDElementSkinData, inEditMode: Boolean = isInEditMode) {
        // Skins may name element types this version doesn't have.
        val element = HUDElements[data.type]?.create?.invoke(game) ?: return

        // In edit mode, all elements should be shown.
        if (!inEditMode && !element.shouldBeShown) {
            return
        }

        +element
        element.restoreData = data
        element.setSkinData(data)
        element.setEditMode(inEditMode)
    }

    /**
     * Called when the back button is pressed.
     */
    fun onBackPress() {

        if (elementSelector?.isExpanded == true) {
            elementSelector?.collapse()
            return
        }

        UIMessageDialog().apply {
            title = string.hudEditor_modal_title

            addButton {
                text = string.hudEditor_modal_save
                onActionUp = {
                    hide()
                    Toasts.showLong(string.hudEditor_saving)
                    setEditMode(false)
                    saveToSkinJSON()
                    Toasts.showLong(string.hudEditor_saved)
                }
            }
            addButton {
                text = string.hudEditor_modal_discard
                onActionUp = {
                    hide()
                    setSkinData(OsuSkin.get().hudSkinData)
                    Toasts.showLong(string.hudEditor_discarded)
                }
            }
            addButton {
                text = string.hudEditor_modal_reset
                onActionUp = {
                    hide()
                    setSkinData(HUDSkinData.Default)
                    Toasts.showLong(string.hudEditor_reset)
                }
            }
            addButton {
                text = string.hudEditor_modal_cancel
                onActionUp = { hide() }
            }
        }.show()
    }


    //region Skinning

    /**
     * Saves the current HUD layout to the skin JSON file.
     */
    fun saveToSkinJSON() {

        val data = getSkinData()

        val jsonFile = File(SkinFolders.path, "skin.json")
        var json: JsonObject

        if (jsonFile.exists()) {
            json = LenientJson.parseToJsonElement(jsonFile.reader().readText()).jsonObject
        } else {
            jsonFile.createNewFile()

            // We use the current skin data as a base to avoid losing any other skin data.
            json = SkinJsonReader.getReader().currentData
        }

        json = JsonObject(json + ("HUD" to HUDSkinData.writeToJSON(data)))
        jsonFile.writeText(prettyJson.encodeToString(JsonObject.serializer(), json))

        SkinJsonReader.getReader().currentData = json
        OsuSkin.get().hudSkinData = data
    }

    /**
     * Obtains the skin data of the HUD.
     */
    fun getSkinData(): HUDSkinData {
        return HUDSkinData(
            elements = children.filterIsInstance<HUDElement>().map { it.getSkinData() }
        )
    }

    /**
     * Sets the skin data of the HUD.
     */
    fun setSkinData(layoutData: HUDSkinData) {
        selected = null
        children.filterIsInstance<HUDElement>().forEach { it.removeSelf() }

        // First pass: We attach everything so that elements can reference between them when
        // applying default layout.
        layoutData.elements.forEach { data -> addElement(data) }

        // The elements need to be measured first as the layout depends on their size.
        pendingDefaultLayout = layoutData == HUDSkinData.Default
    }

    private inline fun <reified T : HUDElement>getFirstOf() : T? {
        return children.firstOrNull { it is T } as? T
    }

    private fun applyDefaultLayout() {
        // The default layout is hardcoded to keep the original layout before the HUD editor was
        // implemented, as it used cross-references between elements that are not possible to be
        // set in the editor.
        val scoreCounter = getFirstOf<HUDScoreCounter>()
        val accuracyCounter = getFirstOf<HUDAccuracyCounter>()
        val pieSongProgress = getFirstOf<HUDPieSongProgress>()

        if (scoreCounter != null && scoreCounter.measuredSize.y <= 0f) {
            return
        }

        pendingDefaultLayout = false

        if (accuracyCounter != null) {
            accuracyCounter.position += Vec2(0f, (scoreCounter?.position?.y ?: 0f) + (scoreCounter?.measuredSize?.y ?: 0f))
            accuracyCounter.restoreData = accuracyCounter.getSkinData()
        }

        if (pieSongProgress != null) {
            val accuracyPosition = accuracyCounter?.position ?: Vec2.Zero
            val accuracySize = accuracyCounter?.transformedSize ?: Vec2.Zero

            pieSongProgress.position = Vec2(accuracyPosition.x - accuracySize.x - 18f, accuracyPosition.y + accuracySize.y / 2f)
            pieSongProgress.restoreData = pieSongProgress.getSkinData()
        }
    }

    //endregion

    //region Elements events

    /**
     * Sets the editor mode of the HUD.
     */
    fun setEditMode(value: Boolean) {
        isInEditMode = value
        game.isHUDEditorMode = value

        if (value) {
            elementSelector = HUDElementSelector(this)
        } else {
            elementSelector?.removeSelf()
            elementSelector = null
        }


        // Cannot use forEachElement {} because we're modifying the list.
        children.filterIsInstance<HUDElement>().forEach { it.setEditMode(value) }
    }

    override fun onAttached() {
        if (isInEditMode) {
            Verktex.addOverlay(elementSelector!!)
        }
        super.onAttached()
    }

    //endregion

    //region Gameplay Events

    /**
     * Iterates over all the elements in the HUD.
     *
     * Note: If you need to remove elements you have to copy the list temporarily
     * using another method such as `filterIsInstance<HUDElement>()`.
     */
    inline fun forEachElement(action: (HUDElement) -> Unit) {
        // `action` may reorder `children` (e.g. selecting an  element moves it to front), which
        // would cause a `ConcurrentModificationException` if we used a regular `forEach`.
        // Inlined so that the per-frame callers below don't allocate a capturing lambda.
        elementChildren.forEachTolerant { if (it is HUDElement) action(it) }
    }

    /**
     * Exposes [children] to [forEachElement], since a public inline function cannot access protected members.
     */
    @PublishedApi
    internal val elementChildren: List<UIComponent>
        get() = children

    override fun onGameplayUpdate(gameScene: GameScene, secondsElapsed: Float) {
        forEachElement { it.onGameplayUpdate(gameScene, secondsElapsed) }
        elementSelector?.onGameplayUpdate(gameScene, secondsElapsed)
    }

    override fun onGameplayTouchDown(time: Float) {
        forEachElement { it.onGameplayTouchDown(time) }
        elementSelector?.onGameplayTouchDown(time)
    }

    override fun onHitObjectLifetimeStart(obj: HitObject) {
        forEachElement { it.onHitObjectLifetimeStart(obj) }
        elementSelector?.onHitObjectLifetimeStart(obj)
    }

    override fun onNoteHit(statistics: StatisticV2) {
        forEachElement { it.onNoteHit(statistics) }
        elementSelector?.onNoteHit(statistics)
    }

    override fun onBreakStateChange(isBreak: Boolean) {
        forEachElement { it.onBreakStateChange(isBreak) }
        elementSelector?.onBreakStateChange(isBreak)
    }

    override fun onAccuracyRegister(type: HitObjectType, accuracy: Float) {
        forEachElement { it.onAccuracyRegister(type, accuracy) }
        elementSelector?.onAccuracyRegister(type, accuracy)
    }

    override fun onAimJudgement(objectPosition: Vector2, cursorPosition: Vector2, objectRadius: Float) {
        forEachElement { it.onAimJudgement(objectPosition, cursorPosition, objectRadius) }
        elementSelector?.onAimJudgement(objectPosition, cursorPosition, objectRadius)
    }

    override fun onSeek() {
        forEachElement { it.onSeek() }
        elementSelector?.onSeek()
    }

    //endregion

    override fun onInputEvent(event: InputEvent): Boolean {

        if (event !is PointerEvent) {
            return super.onInputEvent(event)
        }

        // Ignoring touches on the element selector area when expanded.
        if (elementSelector?.isExpanded == true && event.x <= HUDElementSelector.SELECTOR_WIDTH) {
            return false
        }

        if (!super.onInputEvent(event)) {
            if (event.action == PointerEvent.Action.Down) {
                selected = null
            }
            return false
        }
        return true
    }
}

/**
 * Writes skin JSON indented by four spaces, as it always was.
 */
@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
private val prettyJson = Json {
    prettyPrint = true
    prettyPrintIndent = "    "
}
