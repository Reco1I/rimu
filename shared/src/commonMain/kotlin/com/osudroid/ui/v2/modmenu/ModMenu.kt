package com.osudroid.ui.v2.modmenu

import com.osudroid.OsuGame
import com.reco1l.verktex.data.Background
import com.osudroid.BeatmapSelection
import com.osudroid.settings.OsuSettings
import com.osudroid.GameMode
import com.osudroid.beatmaps.BeatmapCache
import com.osudroid.data.BeatmapInfo
import com.osudroid.data.DatabaseManager
import com.osudroid.difficulty.BeatmapDifficultyCalculator.calculateDroidDifficulty
import com.osudroid.difficulty.BeatmapDifficultyCalculator.calculateStandardDifficulty
import com.osudroid.mods.IModApplicableToTrackRate
import com.osudroid.mods.Mod
import com.osudroid.mods.ModDifficultyAdjust
import com.osudroid.multiplayer.Multiplayer
import com.osudroid.scoring.ScoreMultiplierCalculator
import com.osudroid.ui.v2.ModsIndicator
import com.osudroid.ui.v2.SkinSounds
import com.osudroid.ui.v2.StarRatingBadge
import com.osudroid.multiplayer.clearPlayerMods
import com.osudroid.utils.ModHashMap
import kotlin.concurrent.Volatile
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
import com.reco1l.verktex.ui.container.Container
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.data.px
import com.reco1l.verktex.data.fill
import com.reco1l.verktex.data.verticalPadding
import com.reco1l.verktex.data.wrap
import com.reco1l.verktex.math.Easing
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.ui.FAIcon
import com.reco1l.verktex.ui.Badge
import com.reco1l.verktex.ui.LabeledBadge
import com.reco1l.verktex.ui.TextButton
import com.reco1l.verktex.ui.badge
import com.reco1l.verktex.ui.clearTransforms
import com.reco1l.verktex.ui.colorTo
import com.reco1l.verktex.ui.fadeIn
import com.reco1l.verktex.ui.fadeOut
import com.reco1l.verktex.ui.moveToX
import com.reco1l.verktex.ui.control.TabSelector
import com.reco1l.verktex.ui.container.CaptureContainer
import com.reco1l.verktex.ui.container.LinearContainer.Orientation
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
import okio.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt
import kotlin.reflect.KClass
import com.osudroid.utils.createModInstance
import com.reco1l.verktex.data.Border
import com.reco1l.verktex.ui.container.IgnoreParentPaddingContainer
import com.reco1l.verktex.ui.container.captureContainer
import com.reco1l.verktex.ui.themed
import com.reco1l.verktex.localization.formatString

/**
 * The mod menu, shown over the scene it was opened from. It shows and changes [mods], which its
 * owner keeps: song select's for solo play, the room's in multiplayer.
 */
class ModMenu(val mods: ModHashMap) : CaptureContainer() {

    /**
     * Called by the back button. Hides the menu by default.
     */
    var onBack: () -> Unit = { hide() }

    /**
     * Called with the beatmap and its star rating with [mods] once calculated, on any thread.
     */
    var onDifficultyCalculated: (beatmapInfo: BeatmapInfo, starRating: Double) -> Unit = { _, _ -> }

    /**
     * Extra space below the content of the bottom bar, for what's shown over the menu at the
     * bottom of the screen, e.g. the bar of a multiplayer room's chat.
     */
    var bottomInset: Dimension.Fixed = 0f.dip
        set(value) {
            field = value
            bottomBarContent.padding = bottomBarPadding()
        }

    /**
     * The toggles of every mod the user can select.
     */
    val modToggles
        get() = sectionsView.modToggles


    /**
     * The [ModHashMap.version] of [mods] this menu shows.
     */
    private var shownVersion = -1

    @Volatile
    private var hasSettingsChanged = false

    private lateinit var bottomBarContent: Container
    private lateinit var tabs: TabSelector
    private lateinit var customizeTab: TextButton
    private lateinit var sectionsView: ModSectionsView
    private lateinit var customizationView: ModCustomizationView
    private var shownTab = Tab.Mods
    private lateinit var selectedModsIndicator: ModsIndicator
    private lateinit var searchInput: ModMenuSearchInput
    private lateinit var rankedBadge: Badge
    private lateinit var arBadge: StatisticsBadge
    private lateinit var odBadge: StatisticsBadge
    private lateinit var csBadge: StatisticsBadge
    private lateinit var hpBadge: StatisticsBadge
    private lateinit var bpmBadge: StatisticsBadge
    private lateinit var starRatingBadge: StarRatingBadge
    private lateinit var scoreMultiplierBadge: StatisticsBadge

    private var calculationJob: Job? = null

    /**
     * The mods whose settings [customizationView] shows, which follow [mods].
     */
    private val customizedMods = mutableMapOf<KClass<out Mod>, Mod>()


    init {
        width = fill()
        height = fill()
        blurRadius = 8f.dip
        background = themed {
            Background.Color(palette.accent.shade(0.15f).alpha(0.5f))
        }

        linearContainer {
            width = fill()
            height = fill()
            orientation = Orientation.Vertical

            // TopBar
            extendIntoSafeAreaContainer {
                width = fill()
                background = Background.Color(themed { palette.accent.shade(0.5f).alpha(0.1f) })
                border = Border(1f.dip, colorBottom = Color4.White.alpha(0.15f))

                linearContainer {
                    width = fill()
                    height = wrap()
                    padding = Padding(16f.dip)
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
                                onBack()
                            }
                            onActionCancel = { SkinSounds["click-short"].play() }
                        }

                        tabs = TabSelector().apply {
                            width = 280f.dip
                            anchor = Anchor.CenterLeft
                            origin = Anchor.CenterLeft

                            addButton("Mods") {
                                SkinSounds["click-short-confirm"].play()
                                showTab(Tab.Mods)
                            }.apply {
                                leadingIcon = faSolidIcon(FAIcon.PuzzlePiece)
                                height = TAB_HEIGHT
                            }

                            customizeTab = addButton("Customize") {
                                SkinSounds["click-short-confirm"].play()
                                showTab(Tab.Customize)
                            }.apply {
                                leadingIcon = faSolidIcon(FAIcon.Wrench)
                                height = TAB_HEIGHT
                                isEnabled = false
                            }

                            selectedTab = 0
                        }
                        +tabs

                        textButton {
                            text = "Clear"
                            color = Color4(0xFFFFBFBF)
                            background = Background.Color(Color4(0xFF342121))
                            leadingIcon = faSolidIcon(FAIcon.DeleteLeft)
                            onActionUp = {
                                SkinSounds["click-short-confirm"].play()
                                mods.clearPlayerMods()
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

                        onSearchTermUpdate = { sectionsView.onSearchTermUpdate(it) }
                    }
                    +searchInput
                }
            }

            // Tabs: the mod sections or the customization
            extendIntoSafeAreaContainer {
                width = fill()
                height = fill()

                sectionsView = ModSectionsView(this@ModMenu)
                +sectionsView

                customizationView = ModCustomizationView(::onModSettingChanged).apply {
                    isVisible = false
                }
                +customizationView
            }

            // Bottom bar
            extendIntoSafeAreaContainer {
                width = fill()
                background = Background.Color(themed { palette.accent.shade(0.5f).alpha(0.1f) })
                border = Border(1f.dip, Color4.White.alpha(0.15f), Color4.Transparent)

                bottomBarContent = container {
                    width = fill()
                    height = wrap()
                    padding = bottomBarPadding()

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

                        starRatingBadge = StarRatingBadge().apply {
                            height = fill()
                        }
                        +starRatingBadge

                        rankedBadge = badge {
                            text = "Ranked"
                            background = Background.Color(Color4(0xFF342121))
                            color = Color4(0xFF161622)
                        }

                        scoreMultiplierBadge = StatisticsBadge { formatString("%.2fx", it) }.apply {
                            label = "Score"
                            value = "1.00x"
                        }

                        +scoreMultiplierBadge
                    }
                }
            }
        }

        sectionsView.presetsSection.loadPresets()
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
                onDifficultyCalculated(selectedBeatmap, 0.0)
                return@scope
            }

            if (selectedBeatmap.needsDifficultyCalculation) {
                val newInfo = BeatmapInfo(beatmap, selectedBeatmap.dateImported, true)
                selectedBeatmap.apply(newInfo)
                DatabaseManager.beatmapInfoTable.update(newInfo)
            }

            // This is needed to give the mod a reference to the beatmap's default difficulty values.
            this@ModMenu.mods.ofType<ModDifficultyAdjust>()?.setDefaultDifficulty(beatmap.difficulty)

            customizationView.updateComponents()

            // Copy the mods to avoid concurrent modification
            val mods = this@ModMenu.mods.deepCopy().values
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
                    ScoreMultiplierCalculator(beatmap.difficulty).calculateFor(mods)
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
            onDifficultyCalculated(selectedBeatmap, attributes.starRating)
        }
    }

    //endregion

    //region Visibility

    /**
     * Whether this menu is shown.
     */
    val isShown
        get() = parent != null

    /**
     * Shows this menu over the current scene.
     */
    private fun bottomBarPadding() =
        Padding(16f.dip, 16f.dip, 16f.dip, (16f.dip.toPixels() + bottomInset.toPixels()).px)

    fun show() {
        // An overlay because the scene it was opened from stays visible behind it.
        if (!isShown) {
            Verktex.addOverlay(this)
        }

        showTab(Tab.Mods, animate = false)

        // Do not show mod presets in multiplayer.
        sectionsView.presetsSection.isVisible = !Multiplayer.isMultiplayer

        if (sectionsView.presetsSection.isVisible) {
            sectionsView.presetsSection.onSearchTermUpdate(searchInput.value)
        }

        // Ensure mods and customizations that can be enabled by the user are displayed and enabled.
        updateModButtonVisibility()
        updateCustomizationEnabledStates()

        // The mods may have changed while it was hidden, and the beatmap is parsed again to update the
        // settings defaults that rely on the original beatmap data.
        shownVersion = -1
    }

    fun hide() {
        cancelCalculationJob()
        Verktex.removeOverlay(this)
    }

    //endregion

    //region Tabs

    private enum class Tab { Mods, Customize }

    /**
     * Shows the mod sections or the customization of the selected mods in place of the other.
     */
    private fun showTab(tab: Tab, animate: Boolean = true) {
        tabs.selectedTab = tab.ordinal

        if (shownTab == tab && animate) {
            return
        }
        shownTab = tab

        val shown = if (tab == Tab.Mods) sectionsView else customizationView
        val hidden = if (tab == Tab.Mods) customizationView else sectionsView

        if (!animate) {
            hidden.clearTransforms()
            hidden.isVisible = false

            shown.clearTransforms()
            shown.isVisible = true
            shown.alpha = 1f
            shown.translationX = 0f.px
        } else {
            // The customization is to the right of the mods: switching to it slides both to the left, and back.
            val offset = TAB_SWITCH_OFFSET.toPixels() * if (tab == Tab.Customize) 1f else -1f

            hidden.clearTransforms()
            hidden.fadeOut(TAB_SWITCH_DURATION / 2f, Easing.OutQuint)
            hidden.moveToX(-offset, TAB_SWITCH_DURATION, Easing.OutQuint).after { hidden.isVisible = false }

            shown.clearTransforms()
            shown.isVisible = true
            shown.alpha = 0f
            shown.translationX = offset.px
            shown.fadeIn(TAB_SWITCH_DURATION, Easing.OutQuint)
            shown.moveToX(0f, TAB_SWITCH_DURATION, Easing.OutQuint)
        }

        // Search only filters the mod sections.
        searchInput.isVisible = tab == Tab.Mods
    }

    //endregion

    //region Mods

    fun updateModButtonVisibility() {
        sectionsView.updateModButtonVisibility(searchInput.value)
    }

    fun updateCustomizationEnabledStates() {
        customizationView.updateComponentEnabledStates()
    }

    /**
     * Called when a setting of [mod], one of [mods], changed.
     */
    private fun onModSettingChanged(mod: Mod) {
        hasSettingsChanged = true

        if (mod is IModApplicableToTrackRate) {
            OsuGame.applyModsToMusic(mods)
        }
    }

    override fun onUpdate(clock: FrameClock) {

        // The mods can change on other threads (the room's socket), so they're shown here rather than
        // where they're changed.
        val version = mods.version

        if (version != shownVersion) {
            shownVersion = version
            hasSettingsChanged = false
            onModsChanged()
        } else if (hasSettingsChanged) {
            hasSettingsChanged = false
            parseBeatmap()
        }

        super.onUpdate(clock)
    }

    private fun onModsChanged() {
        val enabledMods = mods.values.toList()
        val isRanked = enabledMods.isEmpty() || enabledMods.none { !it.isRanked }

        updateToggles()
        updateCustomizations()

        rankedBadge.apply {
            text = if (isRanked) "Ranked" else "Unranked"

            clearTransforms()
            colorTo(if (isRanked) Color4(0xFF161622) else Verktex.theme.palette.accent, 0.1f)

            background = Background.Color(
                if (isRanked) Color4(0xFF83DF6B) else Verktex.theme.palette.accent.shade(0.15f)
            )
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

        customizeTab.isEnabled = !customizationView.isEmpty()

        if (shownTab == Tab.Customize && customizationView.isEmpty()) {
            showTab(Tab.Mods)
        }

        OsuGame.applyModsToMusic(mods)
        parseBeatmap()

        selectedModsIndicator.mods = enabledMods
        sectionsView.presetsSection.onModsChanged()
    }

    /**
     * Selects the toggles of the selected mods. A selected toggle holds the selected instance, so
     * that its settings are the ones changed, and an unselected one a new instance.
     */
    private fun updateToggles() {
        modToggles.forEach { toggle ->
            val selected = mods[toggle.mod::class]

            if (selected != null) {
                toggle.mod = selected
            } else if (toggle.isSelected) {
                toggle.mod = toggle.mod::class.createModInstance()
            }

            toggle.isSelected = selected != null
        }
    }

    /**
     * Shows the settings of the selected mods in [customizationView].
     */
    private fun updateCustomizations() {
        for ((modClass, mod) in customizedMods.toList()) {
            if (mods[modClass] !== mod) {
                customizationView.onModRemoved(mod)
                customizedMods.remove(modClass)
            }
        }

        for ((modClass, mod) in mods) {
            if (modClass !in customizedMods) {
                customizationView.onModAdded(mod)
                customizedMods[modClass] = mod
            }
        }
    }

    //endregion

    //region Components

    private class StatisticsBadge(
        private val formatter: (Float) -> String = { formatString("%.2f", it) }
    ) : LabeledBadge() {
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

    private companion object {
        const val TAB_SWITCH_DURATION = 0.3f
        val TAB_SWITCH_OFFSET = 48f.dip

        // The selector's padding around tabs of this height makes it as tall as the top bar's other buttons.
        val TAB_HEIGHT = themed { (buttonStyle.medium.height.toPixels() - gap.xs.toPixels() * 2f).px }
    }
}
