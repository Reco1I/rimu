package com.osudroid.ui.v2.modmenu

import com.osudroid.OsuGame
import com.osudroid.BeatmapSelection
import com.osudroid.settings.OsuSettings
import com.reco1l.verktex.settings.Settings
import com.osudroid.GameMode
import com.osudroid.beatmaps.BeatmapCache
import com.osudroid.data.BeatmapInfo
import com.osudroid.data.DatabaseManager
import com.osudroid.difficulty.BeatmapDifficultyCalculator.calculateDroidDifficulty
import com.osudroid.difficulty.BeatmapDifficultyCalculator.calculateStandardDifficulty
import com.osudroid.mods.IMigratableMod
import com.osudroid.mods.IModApplicableToTrackRate
import com.osudroid.mods.Mod
import com.osudroid.mods.ModDifficultyAdjust
import com.osudroid.mods.ModDoubleTime
import com.osudroid.mods.ModNightCore
import com.osudroid.mods.ModScoreV2
import com.osudroid.mods.ModType
import com.osudroid.multiplayer.Multiplayer
import com.osudroid.multiplayer.api.RoomAPI.setPlayerMods
import com.osudroid.multiplayer.api.RoomAPI.setRoomMods
import com.osudroid.multiplayer.api.data.RoomMods
import com.osudroid.scoring.ScoreMultiplierCalculator
import com.osudroid.ui.v2.ModsIndicator
import com.osudroid.ui.v2.SkinSounds
import com.osudroid.ui.v2.StarRatingBadge
import com.osudroid.ui.v2.songselect.SongSelect
import com.osudroid.utils.ModHashMap
import com.osudroid.utils.allModsInstances
import com.osudroid.utils.applyMods
import com.osudroid.utils.async
import com.osudroid.utils.calculateRate
import com.osudroid.utils.updateThread
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Axis
import com.reco1l.verktex.data.ClipMode
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.data.fill
import com.reco1l.verktex.data.horizontalPadding
import com.reco1l.verktex.data.verticalPadding
import com.reco1l.verktex.data.wrap
import com.reco1l.verktex.math.Easing
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.ui.FAIcon
import com.reco1l.verktex.ui.UIBadge
import com.reco1l.verktex.ui.UILabeledBadge
import com.reco1l.verktex.ui.UITextButton
import com.reco1l.verktex.ui.badge
import com.reco1l.verktex.ui.clearTransforms
import com.reco1l.verktex.ui.colorTo
import com.reco1l.verktex.ui.container.UICaptureContainer
import com.reco1l.verktex.ui.container.UILinearContainer.Orientation
import com.reco1l.verktex.ui.container.container
import com.reco1l.verktex.ui.container.extendIntoSafeAreaContainer
import com.reco1l.verktex.ui.container.linearContainer
import com.reco1l.verktex.ui.container.scrollableContainer
import com.reco1l.verktex.ui.text.faSolidIcon
import com.reco1l.verktex.ui.textButton
import com.rian.framework.RollingFloatCounter
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import ru.nsu.ccfit.zuev.osu.DifficultyAlgorithm.droid
import ru.nsu.ccfit.zuev.osu.DifficultyAlgorithm.standard
import java.io.IOException
import java.util.LinkedList
import java.util.concurrent.CancellationException
import kotlin.math.roundToInt
import kotlin.reflect.KClass
import com.osudroid.utils.createModInstance
import com.reco1l.verktex.ui.themed

object ModMenu : UICaptureContainer() {


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

    private lateinit var modPresetsSection: ModMenuPresetsSection
    private lateinit var customizeButton: UITextButton
    private lateinit var customizationMenu: ModCustomizationMenu
    private lateinit var selectedModsIndicator: ModsIndicator
    private lateinit var searchInput: ModMenuSearchInput
    private lateinit var rankedBadge: UIBadge
    private lateinit var arBadge: StatisticsBadge
    private lateinit var odBadge: StatisticsBadge
    private lateinit var csBadge: StatisticsBadge
    private lateinit var hpBadge: StatisticsBadge
    private lateinit var bpmBadge: StatisticsBadge
    private lateinit var starRatingBadge: StarRatingBadge
    private lateinit var scoreMultiplierBadge: StatisticsBadge

    private var calculationJob: Job? = null


    init {
        blurRadius = 12f.dip

        extendIntoSafeAreaContainer {
            width = fill()
            height = fill()
            padding = horizontalPadding(8f.dip)
            backgroundColor = themed { (palette.accent.shade(0.1f)).alpha(0.75f) }

            linearContainer {
                width = fill()
                height = fill()
                orientation = Orientation.Vertical

                // TopBar
                linearContainer {
                    width = fill()
                    height = wrap()
                    padding = verticalPadding(8f.dip)
                    spacing = 4f.dip

                    // Left side
                    linearContainer {
                        width = fill()
                        orientation = Orientation.Horizontal
                        anchor = Anchor.CenterLeft
                        origin = Anchor.CenterLeft
                        spacing = 8f.dip

                        textButton {
                            text = "Back"
                            leadingIcon = faSolidIcon(FAIcon.ArrowLeft)
                            onActionUp = {
                                SkinSounds["click-short-confirm"].play()
                                back()
                            }
                            onActionCancel = { SkinSounds["click-short"].play() }
                        }

                        customizeButton = textButton {
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

                        textButton {
                            text = "Clear"
                            color = Color4(0xFFFFBFBF)
                            backgroundColor = Color4(0xFF342121)
                            leadingIcon = faSolidIcon(FAIcon.DeleteLeft)
                            onActionUp = {
                                SkinSounds["click-short-confirm"].play()
                                clear()
                            }
                            onActionCancel = { SkinSounds["click-short"].play() }
                        }

                        scrollableContainer {
                            width = fill()
                            anchor = Anchor.CenterLeft
                            origin = Anchor.CenterLeft
                            scrollAxes = Axis.X
                            clipping = ClipMode.Bounds

                            selectedModsIndicator = ModsIndicator()
                            +selectedModsIndicator
                        }
                    }

                    // Right side: Search box
                    searchInput = ModMenuSearchInput().apply {

                        onSearchTermUpdate = { searchTerm ->
                            modSections.forEach { it.onSearchTermUpdate(searchTerm) }

                            if (modPresetsSection.isVisible) {
                                modPresetsSection.onSearchTermUpdate(searchTerm)
                            }
                        }
                    }
                    +searchInput
                }

                // Sections container
                scrollableContainer {
                    width = fill()
                    height = fill()
                    scrollAxes = Axis.X

                    linearContainer {
                        orientation = Orientation.Horizontal
                        width = wrap()
                        height = fill()
                        spacing = 16f.dip

                        modPresetsSection = ModMenuPresetsSection()
                        +modPresetsSection

                        val mods = allModsInstances

                        ModType.entries.forEach { type ->
                            val sectionName = type.stringId.toString()
                            val sectionToggles = mods
                                .filter { it !is IMigratableMod && it.isUserPlayable && it.type == type }
                                .map { ModMenuToggle(it) }

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

                container {
                    width = fill()
                    height = wrap()
                    padding = Padding(8f.dip)

                    linearContainer {
                        orientation = Orientation.Horizontal
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

                    linearContainer {
                        orientation = Orientation.Horizontal
                        anchor = Anchor.CenterRight
                        origin = Anchor.CenterRight
                        spacing = 8f.dip

                        starRatingBadge = StarRatingBadge()
                        +starRatingBadge

                        rankedBadge = badge {
                            text = "Ranked"
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
        }

        modPresetsSection.loadPresets()
    }


    //region Calculation

    fun cancelCalculationJob() {
        calculationJob?.cancel(CancellationException("Difficulty calculation has been cancelled."))
        calculationJob = null
    }

    private fun parseBeatmap() {
        cancelCalculationJob()

        val selectedBeatmap = BeatmapSelection.beatmap ?: return

        calculationJob = async scope@{

            val difficultyAlgorithm = OsuSettings.difficultyAlgorithm
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
                bpmBadge.updateValue(
                    selectedBeatmap.mostCommonBPM,
                    selectedBeatmap.mostCommonBPM * rate
                )

                scoreMultiplierBadge.updateValue(
                    1f,
                    ScoreMultiplierCalculator(beatmap.difficulty).calculateFor(enabledMods.values)
                        .toFloat()
                )
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
            val gameScene = OsuGame.gameScene
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

            if (Settings[OsuSettings.UseNightcoreOnMultiplayer] && doubleTime != null) {
                removeMod(doubleTime)
                addMod(ModNightCore())
            } else if (!Settings[OsuSettings.UseNightcoreOnMultiplayer] && nightCore != null) {
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

            backgroundColor =
                if (isRanked) Color4(0xFF83DF6B) else Verktex.theme.palette.accent.shade(0.15f)
        }

        modToggles.forEach {
            it.hasIncompatibility =
                !it.isSelected && enabledMods.any { m ->
                    !it.mod.isCompatibleWith(m) || !m.isCompatibleWith(
                        it.mod
                    )
                }
        }

        val selectedBeatmap = BeatmapSelection.beatmap
        scoreMultiplierBadge.updateValue(
            1f,
            ScoreMultiplierCalculator(selectedBeatmap?.getBeatmapDifficulty()).calculateFor(
                enabledMods
            ).toFloat()
        )

        customizeButton.isEnabled = !customizationMenu.isEmpty()

        if (modChangeQueue.any { it is IModApplicableToTrackRate }) {
            SongSelect.updateMusicEffects()
        }
        modChangeQueue.clear()

        parseBeatmap()

        selectedModsIndicator.mods = enabledMods
        modPresetsSection.onModsChanged()
        SongSelect.onModsChanged()

        // The song select behind shows the statistics with the mods.
        //invalidateCapture()
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
                button.mod = button.mod::class.createModInstance()
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

        toggle.mod = modClass.createModInstance()
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
                ), counter.rollingDuration, counter.rollingEasing
            )
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


