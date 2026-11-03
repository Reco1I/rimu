package com.osudroid.ui.v2.modmenu

import com.osudroid.ui.v2.SkinSounds
import com.osudroid.GameMode
import com.osudroid.beatmaps.BeatmapCache
import com.osudroid.data.BeatmapInfo
import com.osudroid.data.DatabaseManager
import com.osudroid.difficulty.BeatmapDifficultyCalculator.calculateDroidDifficulty
import com.osudroid.difficulty.BeatmapDifficultyCalculator.calculateStandardDifficulty
import com.osudroid.mods.*
import com.osudroid.multiplayer.Multiplayer
import com.osudroid.multiplayer.api.RoomAPI.setPlayerMods
import com.osudroid.multiplayer.api.RoomAPI.setRoomMods
import com.osudroid.multiplayer.api.data.RoomMods
import com.osudroid.scoring.ScoreMultiplierCalculator
import com.osudroid.ui.v2.ModsIndicator
import com.osudroid.ui.v2.StarRatingBadge
import com.osudroid.ui.v2.songselect.*
import com.osudroid.utils.ModHashMap
import com.osudroid.utils.allModsInstances
import com.osudroid.utils.applyMods
import com.osudroid.utils.async
import com.osudroid.utils.calculateRate
import com.osudroid.utils.updateThread
import com.reco1l.framework.*
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Axis
import com.reco1l.verktex.data.ClipMode
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.data.px
import com.reco1l.verktex.data.toPadding
import com.reco1l.verktex.math.Easing
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.ui.FAIcon
import com.reco1l.verktex.ui.UIBadge
import com.reco1l.verktex.ui.UIIcon
import com.reco1l.verktex.ui.UILabeledBadge
import com.reco1l.verktex.ui.UITextButton
import com.reco1l.verktex.ui.badge
import com.reco1l.verktex.ui.clearTransforms
import com.reco1l.verktex.ui.colorTo
import com.reco1l.verktex.ui.container.UIContainer
import com.reco1l.verktex.ui.container.UILinearContainer
import com.reco1l.verktex.ui.container.UIScrollableContainer
import com.reco1l.verktex.ui.container.container
import com.reco1l.verktex.ui.text.faSolidIcon
import com.rian.framework.RollingFloatCounter
import java.io.IOException
import kotlinx.coroutines.*
import ru.nsu.ccfit.zuev.osu.*

import ru.nsu.ccfit.zuev.osu.helper.StringTable
import ru.nsu.ccfit.zuev.osu.DifficultyAlgorithm.*
import java.util.LinkedList
import java.util.concurrent.CancellationException
import kotlin.math.*
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

object ModMenu : UIContainer() {


    /**
     * List of currently enabled mods.
     */
    val enabledMods = ModHashMap()

    /**
     * List of all mod sections.
     */
    val modSections = mutableListOf<ModMenuSection>()

    /**
     * List of all mod toggles.
     */
    val modToggles = mutableListOf<ModMenuToggle>()


    private val modChangeQueue = LinkedList<Mod>()
    private val modPresetsSection: ModMenuPresetsSection

    private val customizeButton: UITextButton
    private val customizationMenu: ModCustomizationMenu
    private val selectedModsIndicator: ModsIndicator
    private val searchInput: ModMenuSearchInput

    private val rankedBadge: UIBadge
    private val arBadge: StatisticsBadge
    private val odBadge: StatisticsBadge
    private val csBadge: StatisticsBadge
    private val hpBadge: StatisticsBadge
    private val bpmBadge: StatisticsBadge
    private val starRatingBadge: StarRatingBadge
    private val scoreMultiplierBadge: StatisticsBadge

    private var calculationJob: Job? = null


    init {
        width = Dimension.FillAvailable
        height = Dimension.FillAvailable

        +UILinearContainer().apply {
            width = Dimension.FillAvailable
            height = Dimension.FillAvailable
            orientation = UILinearContainer.Orientation.Vertical
            style = Style { theme ->
                backgroundColor = (theme.palette.accent * 0.1f).copy(alpha = 0.9f)
            }

            +UIContainer().apply {
                width = Dimension.FillAvailable
                height = Dimension.WrapContent
                style = Style {
                    padding = Verktex.graphics.window.insets.safeArea.toPadding().copy(top = 8f.dip, bottom = 8f.dip)
                }

                +UILinearContainer().apply {
                    orientation = UILinearContainer.Orientation.Horizontal
                    anchor = Anchor.CenterLeft
                    origin = Anchor.CenterLeft
                    spacing = 8f.dip

                    +UITextButton().apply {
                        text = "Back"
                        leadingIcon = faSolidIcon(FAIcon.ArrowLeft)
                        onActionUp = {
                            SkinSounds["click-short-confirm"].play()
                            back()
                        }
                        onActionCancel = { SkinSounds["click-short"].play() }
                    }

                    customizeButton = UITextButton().apply {
                        text = "Customize"
                        isEnabled = false
                        leadingIcon = faSolidIcon(FAIcon.Wrench)
                        onActionUp = {
                            SkinSounds["click-short-confirm"].play()
                            if (customizationMenu.isShown) {
                                customizationMenu.hide()
                            } else {
                                customizationMenu.show()
                            }
                        }
                        onActionCancel = { SkinSounds["click-short"].play() }
                    }

                    +customizeButton

                    +UITextButton().apply {
                        text = "Clear"
                        tintChildren = true
                        style += Style {
                            color = Color4(0xFFFFBFBF)
                            backgroundColor = Color4(0xFF342121)
                        }
                        leadingIcon = faSolidIcon(FAIcon.DeleteLeft)
                        onActionUp = {
                            SkinSounds["click-short-confirm"].play()
                            clear()
                        }
                        onActionCancel = { SkinSounds["click-short"].play() }
                    }

                    +UIScrollableContainer().apply {
                        width = 192f.dip
                        height = Dimension.WrapContent
                        anchor = Anchor.CenterLeft
                        origin = Anchor.CenterLeft
                        scrollAxes = Axis.X
                        clipping = ClipMode.Bounds

                        selectedModsIndicator = ModsIndicator()
                        +selectedModsIndicator
                    }
                }

                +UIContainer().apply {
                    anchor = Anchor.CenterRight
                    origin = Anchor.CenterRight

                    searchInput = ModMenuSearchInput().apply {
                        width = 288f.dip
                        onSearchTermUpdate = { searchTerm ->
                            modSections.forEach { it.onSearchTermUpdate(searchTerm) }

                            if (modPresetsSection.isVisible) {
                                modPresetsSection.onSearchTermUpdate(searchTerm)
                            }
                        }
                    }

                    +searchInput

                    container {
                        anchor = Anchor.CenterRight
                        origin = Anchor.CenterRight
                        padding = Padding(8f.dip, 0f.dip)

                        +UIIcon(FAIcon.MagnifyingGlass).apply {
                            style = Style { theme ->
                                color = theme.palette.accent
                            }
                        }
                    }
                }
            }

            +UIScrollableContainer().apply {
                width = Dimension.FillAvailable
                height = Dimension.FillAvailable
                scrollAxes = Axis.X

                +UILinearContainer().apply {
                    orientation = UILinearContainer.Orientation.Horizontal
                    width = Dimension.WrapContent
                    height = Dimension.FillAvailable
                    spacing = 8f.dip
                    style = Style {
                        padding = Verktex.graphics.window.insets.safeArea.toPadding()
                    }

                    modPresetsSection = ModMenuPresetsSection()
                    +modPresetsSection

                    val mods = allModsInstances

                    ModType.entries.forEach { type ->
                        val sectionName = StringTable.get(type.stringId)
                        val sectionToggles = mods.filter { it !is IMigratableMod && it.isUserPlayable && it.type == type }.map { ModMenuToggle(it) }

                        if (sectionToggles.isEmpty()) {
                            return@forEach
                        }

                        modToggles.addAll(sectionToggles)

                        val section = ModMenuSection(sectionName, sectionToggles)

                        +section
                        modSections.add(section)
                    }
                }
            }

            +UIContainer().apply {
                width = Dimension.FillAvailable
                height = Dimension.WrapContent
                style = Style {
                    padding = Verktex.graphics.window.insets.safeArea.toPadding().copy(
                        top = 8f.dip,
                        bottom = (8f.dip.toPixels() + (Multiplayer.roomScene?.chat?.buttonHeight ?: 0f)).px
                    )
                }

                +UILinearContainer().apply {
                    orientation = UILinearContainer.Orientation.Horizontal
                    anchor = Anchor.CenterLeft
                    origin = Anchor.CenterLeft
                    spacing = 8f.dip

                    arBadge = StatisticsBadge().apply {
                        label = "AR"
                        value = "0.00"
                    }

                    odBadge = StatisticsBadge().apply {
                        label = "OD"
                        value = "0.00"
                    }

                    csBadge = StatisticsBadge().apply {
                        label = "CS"
                        value = "0.00"
                    }

                    hpBadge = StatisticsBadge().apply {
                        label = "HP"
                        value = "0.00"
                    }

                    bpmBadge = StatisticsBadge { it.roundToInt().toString() }.apply {
                        label = "BPM"
                        value = "0.0"
                    }

                    +arBadge
                    +odBadge
                    +csBadge
                    +hpBadge
                    +bpmBadge
                }

                +UILinearContainer().apply {
                    orientation = UILinearContainer.Orientation.Horizontal
                    anchor = Anchor.CenterRight
                    origin = Anchor.CenterRight
                    spacing = 8f.dip

                    starRatingBadge = StarRatingBadge()
                    +starRatingBadge

                    rankedBadge = badge {
                        text = "Ranked"
                        style = Style {}
                        backgroundColor = Color4(0xFF342121)
                        color = Color4(0xFF161622)
                    }

                    scoreMultiplierBadge = StatisticsBadge { "%.2fx".format(it) }.apply {
                        label = "Score"
                        value = "1.00x"
                    }

                    +scoreMultiplierBadge
                }
            }
        }

        // Customizations menu
        customizationMenu = ModCustomizationMenu(customizeButton)
        +customizationMenu

        modPresetsSection.loadPresets()
    }


    //region Calculation

    fun cancelCalculationJob() {
        calculationJob?.cancel(CancellationException("Difficulty calculation has been cancelled."))
        calculationJob = null
    }

    private fun parseBeatmap() {
        cancelCalculationJob()

        val selectedBeatmap = GlobalManager.getInstance().selectedBeatmap ?: return

        calculationJob = async scope@{

            val difficultyAlgorithm = Config.getDifficultyAlgorithm()
            val gameMode = if (difficultyAlgorithm == droid) GameMode.Droid else GameMode.Standard

            val beatmap = try {
                BeatmapCache.getBeatmap(selectedBeatmap, true, gameMode, this)
            } catch (e: Exception) {
                when (e) {
                    is IOException, is IllegalArgumentException -> null
                    else -> throw e
                }
            }

            if (beatmap == null) {
                SongSelect.setStarRatingDisplay(0.0)
                return@scope
            }

            if (selectedBeatmap.needsDifficultyCalculation) {
                val newInfo = BeatmapInfo(beatmap, selectedBeatmap.dateImported, true)
                selectedBeatmap.apply(newInfo)
                DatabaseManager.beatmapInfoTable.update(newInfo)
            }

            // This is needed to give the mod a reference to the beatmap's default difficulty values.
            enabledMods.ofType<ModDifficultyAdjust>()?.setDefaultDifficulty(beatmap.difficulty)

            customizationMenu.updateComponents()

            // Copy the mods to avoid concurrent modification
            val mods = enabledMods.deepCopy().values
            val difficulty = beatmap.difficulty.clone()
            val rate = mods.calculateRate(Double.POSITIVE_INFINITY)

            difficulty.applyMods(gameMode, mods, true, this@scope)

            ensureActive()

            updateThread {
                arBadge.updateValue(selectedBeatmap.approachRate, difficulty.ar)
                odBadge.updateValue(selectedBeatmap.overallDifficulty, difficulty.od)
                csBadge.updateValue(selectedBeatmap.circleSize, difficulty.difficultyCS)
                hpBadge.updateValue(selectedBeatmap.hpDrainRate, difficulty.hp)
                bpmBadge.updateValue(selectedBeatmap.mostCommonBPM, selectedBeatmap.mostCommonBPM * rate)

                scoreMultiplierBadge.updateValue(1f, ScoreMultiplierCalculator(beatmap.difficulty).calculateFor(enabledMods.values).toFloat())
            }

            ensureActive()

            val attributes = when (difficultyAlgorithm) {
                droid -> calculateDroidDifficulty(beatmap, mods, this@scope)
                standard -> calculateStandardDifficulty(beatmap, mods, this@scope)
            }

            ensureActive()

            starRatingBadge.rating = attributes.starRating
            SongSelect.setDifficultyStatistics(selectedBeatmap)
            SongSelect.setStarRatingDisplay(attributes.starRating)
        }
    }

    //endregion

    //region Visibility

    /**
     * Whether this menu is currently shown.
     */
    val isShown
        get() = parent != null

    fun show() {
        // An overlay because the scene it was opened from stays visible behind it.
        if (!isShown) {
            Verktex.addOverlay(this)
        }

        // Do not show mod presets in multiplayer.
        modPresetsSection.isVisible = !Multiplayer.isMultiplayer

        if (modPresetsSection.isVisible) {
            modPresetsSection.onSearchTermUpdate(searchInput.value)
        }

        // Ensure mods and customizations that can be enabled by the user are displayed and enabled.
        updateModButtonVisibility()
        updateCustomizationMenuEnabledStates()

        // Only parsing to update mod's specific settings defaults, specially those which rely on the original beatmap data.
        parseBeatmap()
    }

    @JvmOverloads
    fun back(updatePlayerMods: Boolean = true) {

        if (Multiplayer.isConnected) {
            val gameScene = GlobalManager.getInstance().gameScene
            val isInGameplay = gameScene != null &&
                Verktex.scene == gameScene.scene

            if (!isInGameplay) {
                Multiplayer.roomScene?.chat?.show()
                Multiplayer.roomScene?.isWaitingForModsChange?.set(true)

                // The room mods are the same as the host mods
                if (Multiplayer.isRoomHost) {
                    setRoomMods(enabledMods.serializeMods())
                } else if (updatePlayerMods) {
                    setPlayerMods(enabledMods.serializeMods())
                } else {
                    Multiplayer.roomScene?.isWaitingForModsChange?.set(false)
                }
            }
        }

        Verktex.removeOverlay(this)
    }

    //endregion

    //region Mods

    fun setMods(mods: RoomMods, isFreeMod: Boolean) {
        if (isFreeMod) {
            for ((_, mod) in enabledMods.toList()) {
                if (mod.isValidForMultiplayerAsFreeMod) {
                    continue
                }

                if (mod !in mods) {
                    removeMod(mod)
                }
            }
        } else {
            clear()
        }

        for (mod in mods.values) {
            if (!mod.isValidForMultiplayer) {
                continue
            }

            if (!isFreeMod || !mod.isValidForMultiplayerAsFreeMod) {
                addMod(mod)
            }
        }

        if (!Multiplayer.isRoomHost) {
            val doubleTime = enabledMods.ofType<ModDoubleTime>()
            val nightCore = enabledMods.ofType<ModNightCore>()

            if (Config.isUseNightcoreOnMultiplayer() && doubleTime != null) {
                removeMod(doubleTime)
                addMod(ModNightCore())
            } else if (!Config.isUseNightcoreOnMultiplayer() && nightCore != null) {
                removeMod(nightCore)
                addMod(ModDoubleTime())
            }
        }

        if (ModScoreV2::class in mods) {
            addMod(ModScoreV2())
        } else {
            removeMod(ModScoreV2())
        }

        updateModButtonVisibility()
    }

    fun updateModButtonVisibility() {
        modToggles.forEach {
            it.updateVisibility(searchInput.value)
            it.applyCompatibilityState()
        }

        // Update section visibility after toggles have been updated since it may not need to be visible anymore.
        modSections.forEach { it.updateVisibility() }
    }

    fun updateCustomizationMenuEnabledStates() {
        customizationMenu.updateComponentEnabledStates()
    }

    fun clear() {
        cancelCalculationJob()

        val room = Multiplayer.room
        val isHost = Multiplayer.isRoomHost

        enabledMods.toList().forEach {
            val mod = it.second

            if (room != null) {
                // For non-host in multiplayer, we want to keep mods that are not allowed to be selected by the player
                // since only the host can change those mods.
                if (!isHost && room.gameplaySettings.isFreeMod && !mod.isValidForMultiplayerAsFreeMod) {
                    return@forEach
                }

                // Special handling for the ScoreV2 mod, which is used by the ScoreV2 win condition.
                if (mod is ModScoreV2) {
                    return@forEach
                }
            }

            removeMod(mod::class)
        }
    }

    fun queueModChange(mod: Mod) = synchronized(modChangeQueue) {
        // Adding to first place in case it is already queued.
        if (modChangeQueue.isEmpty() || modChangeQueue.first() != mod) {
            if (modChangeQueue.isNotEmpty()) {
                modChangeQueue.remove(mod)
            }
            modChangeQueue.addFirst(mod)
        }
    }

    override fun onUpdate(clock: FrameClock) {

        if (modChangeQueue.isNotEmpty()) {
            onModsChanged()
        }

        super.onUpdate(clock)
    }

    private fun onModsChanged() = synchronized(modChangeQueue) {
        val enabledMods = enabledMods.values.toList()
        val isRanked = enabledMods.isEmpty() || enabledMods.none { !it.isRanked }

        rankedBadge.apply {
            text = if (isRanked) "Ranked" else "Unranked"

            clearTransforms()
            colorTo(if (isRanked) Color4(0xFF161622) else Verktex.theme.palette.accent, 0.1f)

            backgroundColor = if (isRanked) Color4(0xFF83DF6B) else Verktex.theme.palette.accent * 0.15f
        }

        modToggles.forEach {
            it.hasIncompatibility =
                !it.isSelected && enabledMods.any { m -> !it.mod.isCompatibleWith(m) || !m.isCompatibleWith(it.mod) }
        }

        val selectedBeatmap = GlobalManager.getInstance().selectedBeatmap
        scoreMultiplierBadge.updateValue(1f, ScoreMultiplierCalculator(selectedBeatmap?.getBeatmapDifficulty()).calculateFor(enabledMods).toFloat())

        customizeButton.isEnabled = !customizationMenu.isEmpty()

        if (modChangeQueue.any { it is IModApplicableToTrackRate }) {
            SongSelect.updateMusicEffects()
        }
        modChangeQueue.clear()

        parseBeatmap()

        selectedModsIndicator.mods = enabledMods
        modPresetsSection.onModsChanged()
        SongSelect.onModsChanged()
    }

    fun addMod(mod: Mod) {

        if (mod in enabledMods) {
            return
        }
        enabledMods.put(mod)

        modToggles.forEach { button ->

            val wasSelected = button.isSelected
            button.isSelected = button.mod::class in enabledMods

            if (button.mod::class == mod::class) {
                button.mod = mod
            }

            // Handle incompatible mods with the selected mod.
            if (wasSelected && !button.isSelected) {
                customizationMenu.onModRemoved(button.mod)
                button.mod = button.mod::class.createInstance()
            }
        }

        customizationMenu.onModAdded(mod)
        queueModChange(mod)
    }

    fun removeMod(mod: Mod) = removeMod(mod::class)

    fun removeMod(modClass: KClass<out Mod>) {

        if (modClass !in enabledMods) {
            return
        }

        val toggle = modToggles.find { it.mod::class == modClass } ?: return

        enabledMods.remove(modClass)

        toggle.isSelected = false

        customizationMenu.onModRemoved(toggle.mod)
        queueModChange(toggle.mod)

        toggle.mod = modClass.createInstance()
    }

    //endregion

    //region Components

    private class StatisticsBadge(
        private val formatter: (Float) -> String = { "%.2f".format(it) }
    ) : UILabeledBadge() {
        private val counter = RollingFloatCounter(0f).apply {
            rollingEasing = Easing.OutQuint
            rollingDuration = 0.3f
        }

        fun updateValue(initialValue: Float, finalValue: Float) {
            if (counter.targetValue == finalValue) {
                return
            }

            counter.targetValue = finalValue

            valueComponent.clearTransforms()
            valueComponent.colorTo(
                Color4(
                    when {
                        initialValue < finalValue -> 0xFFF78383
                        initialValue > finalValue -> 0xFF40CF5D
                        else -> 0xFFFFFFFF
                    }
                ), counter.rollingDuration, counter.rollingEasing)
        }

        override fun onUpdate(clock: FrameClock) {
            counter.onUpdate(clock.elapsedFrameTime)

            if (counter.isRolling) {
                valueComponent.text = formatter(counter.currentValue)
            }

            super.onUpdate(clock)
        }
    }

    //endregion

}


