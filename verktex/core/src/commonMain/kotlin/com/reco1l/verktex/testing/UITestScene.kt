package com.reco1l.verktex.testing

import com.reco1l.verktex.Logger
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.input.InputEvent
import com.reco1l.verktex.ui.container.container
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Axis
import com.reco1l.verktex.data.ClipMode
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.LayoutConstraints
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.Vec4
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.data.fill
import com.reco1l.verktex.ui.FAIcon
import com.reco1l.verktex.ui.UIButton
import com.reco1l.verktex.ui.UIComponent
import com.reco1l.verktex.graphics.GraphicsRenderer
import com.reco1l.verktex.ui.Theme
import com.reco1l.verktex.ui.UIScene
import com.reco1l.verktex.ui.UITextButton
import com.reco1l.verktex.ui.container.UIContainer
import com.reco1l.verktex.ui.container.UILinearContainer
import com.reco1l.verktex.ui.container.linearContainer
import com.reco1l.verktex.ui.container.scrollableContainer
import com.reco1l.verktex.ui.iconButton
import com.reco1l.verktex.ui.textButton
import com.reco1l.verktex.time.derive
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.ui.text.UIText
import com.reco1l.verktex.ui.text.text
import com.reco1l.verktex.data.oklch
import kotlin.random.Random

/**
 * A scene listing [UITestSuite]s on the left and showing the selected [UITest] on the right.
 *
 * @param initialTest The id (`suite/test`) or the name of the test to open first.
 * @param onTestSelected Called with the id of a test when it's opened.
 */
class UITestScene(
    private val suites: List<UITestSuite>,
    initialTest: String? = null,
    private val onTestSelected: (id: String) -> Unit = {}
) : UIScene() {

    private val testButtons = mutableMapOf<UITest, UITextButton>()

    // Assigned from the builders in init.
    private lateinit var titleText: UIText
    private lateinit var logText: UIText
    private lateinit var stageHost: UIContainer

    // Holds the stage and its overlays, scaled down when the tests are shown at the window size.
    private val viewport = object : UIContainer() {
        override fun drawContent(renderer: GraphicsRenderer) {
            if (!outlinesComponents) {
                return super.drawContent(renderer)
            }

            // Only what the test shows is outlined, not the browser.
            UIComponent.isOutliningBoundaries = true
            try {
                super.drawContent(renderer)
            } finally {
                UIComponent.isOutliningBoundaries = false
            }
        }


        // Scaled to fit, it's bigger than the area it's shown in.
        override fun adjustParentConstraints(constraints: LayoutConstraints): LayoutConstraints {
            if (!isFittingWindow || virtualWidth <= 0f || virtualHeight <= 0f) {
                return constraints
            }
            return LayoutConstraints(minWidth = virtualWidth, maxWidth = virtualWidth, minHeight = virtualHeight, maxHeight = virtualHeight)
        }
    }.apply {
        backgroundColor = BACKGROUNDS[0]
        anchor = Anchor.Center
        origin = Anchor.Center
        scaleOrigin = Anchor.Center
        // What goes past the window in the game, e.g. the beatmap carrousel, isn't shown either.
        clipping = ClipMode.Bounds
    }

    /**
     * Whether the stage is laid out as wide as the window and scaled down to fit, so scenes look as
     * they do in the game. Its height fills the area, scaled the same. Otherwise it's laid out at
     * the size it has, as components are.
     */
    private var isFittingWindow = true

    /**
     * Whether the scenes of the stage keep the safe area of the window, as they do in the game.
     */
    private var hasSafeArea = true

    /**
     * Whether the components of the test are outlined with their bounds.
     */
    private var outlinesComponents = false

    // The size the viewport is laid out at while fitting the window, before it's scaled.
    private var virtualWidth = 0f
    private var virtualHeight = 0f

    private val logLines = ArrayDeque<String>()

    private val stageClock = Verktex.clock.derive()

    // The browser is drawn flat, with square corners and filled buttons, so it can be told apart
    // from the components under test, which use the global theme.
    private val flatTheme = Theme(
        palette = Verktex.theme.palette,
        radius = Theme.FixedSizeBreakpoints(0f.dip, 0f.dip, 0f.dip, 0f.dip, 0f.dip),
    ).let { theme ->
        // Without the bezel and shadow of buttons and cards.
        Theme(
            palette = theme.palette,
            radius = theme.radius,
            buttonStyle = theme.buttonStyle.withoutElevation(),
            cardStyle = theme.cardStyle.withoutElevation(),
        )
    }

    private var currentTest: UITest? = null

    private var currentStage: UITestStage? = null

    private var pendingTest: Pair<UITestSuite, UITest>? = null

    private var currentOverlayLayer: UIScene? = null
    private var backgroundIndex = 0


    init {
        backgroundColor = BACKGROUNDS[0]

        linearContainer {
            orientation = UILinearContainer.Orientation.Horizontal
            width = fill()
            height = fill()

            scrollableContainer {
                theme = flatTheme
                scrollAxes = Axis.Y
                width = 220f.dip
                height = fill()
                backgroundColor = Verktex.theme.palette.surface

                linearContainer {
                    orientation = UILinearContainer.Orientation.Vertical
                    width = fill()
                    padding = Padding(8f.dip)
                    spacing = 2f.dip

                    for (suite in suites) {
                        text {
                            text = suite.name
                            fontSize = Verktex.theme.typography.caption.size
                            color = Color4(1f, 1f, 1f, 0.5f)
                            padding = Padding(8f.dip, 12f.dip, 8f.dip, 4f.dip)
                        }

                        for (test in suite.tests) {
                            val button = UITextButton().apply {
                                text = test.name
                                width = fill()
                                alignment = Anchor.CenterLeft
                                variant = UIButton.Variant.Surface
                                density = UIButton.Density.Small
                                onActionUp = { select(suite, test) }
                            }
                            testButtons[test] = button
                            +button
                        }
                    }
                }
            }

            linearContainer {
                orientation = UILinearContainer.Orientation.Vertical
                width = fill()
                height = fill()

                linearContainer {
                    theme = flatTheme
                    orientation = UILinearContainer.Orientation.Horizontal
                    width = fill()
                    padding = Padding(12f.dip, 6f.dip)
                    spacing = 4f.dip

                    titleText = text {
                        width = fill()
                        height = fill()
                        alignment = Anchor.CenterLeft
                        text = "Select a test"
                        fontSettings = Verktex.theme.typography.h3
                    }

                    iconButton {
                        icon = FAIcon.Pause
                        variant = UIButton.Variant.Surface
                        onActionUp = {
                            if (stageClock.isRunning) stageClock.pause() else stageClock.start()
                            icon = if (stageClock.isRunning) FAIcon.Pause else FAIcon.Play
                        }
                    }

                    textButton {
                        text = formatRate(stageClock.rate)
                        variant = UIButton.Variant.Surface
                        onActionUp = {
                            val index = (RATES.indexOf(stageClock.rate) + 1) % RATES.size
                            stageClock.rate = RATES[index]
                            text = formatRate(stageClock.rate)
                        }
                    }

                    iconButton {
                        icon = viewportIcon()
                        variant = UIButton.Variant.Surface
                        onActionUp = {
                            isFittingWindow = !isFittingWindow
                            // Measured again with the constraints of the new mode.
                            viewport.invalidateMeasure()
                            icon = viewportIcon()
                            log(if (isFittingWindow) "Laid out at the window size" else "Laid out at the stage size")
                        }
                    }

                    iconButton {
                        icon = safeAreaIcon()
                        variant = UIButton.Variant.Surface
                        onActionUp = {
                            hasSafeArea = !hasSafeArea
                            icon = safeAreaIcon()
                            log(if (hasSafeArea) "Scenes keep the safe area" else "Scenes ignore the safe area")
                        }
                    }

                    iconButton {
                        icon = FAIcon.ObjectGroup
                        variant = UIButton.Variant.Surface
                        onActionUp = {
                            outlinesComponents = !outlinesComponents
                            variant = if (outlinesComponents) UIButton.Variant.Soft else UIButton.Variant.Surface
                            log(if (outlinesComponents) "Showing the bounds of components" else "Hiding the bounds of components")
                        }
                    }

                    iconButton {
                        icon = FAIcon.ArrowRotateRight
                        variant = UIButton.Variant.Surface
                        onActionUp = { reload() }
                    }

                    iconButton {
                        icon = FAIcon.Palette
                        variant = UIButton.Variant.Surface
                        onActionUp = {
                            backgroundIndex = (backgroundIndex + 1) % BACKGROUNDS.size
                            viewport.backgroundColor = BACKGROUNDS[backgroundIndex]
                        }
                    }

                    // Gives the stage a random accent, to see the components follow theme changes.
                    iconButton {
                        icon = FAIcon.Dice
                        variant = UIButton.Variant.Surface
                        onActionUp = { randomizeAccent() }
                        onActionLongPress = {
                            viewport.theme = null
                            log("Accent reset to the global theme")
                        }
                    }
                }

                container {
                    width = fill()
                    height = fill()

                    // The tests run on a clock of their own, so they can be paused or sped up.
                    stageHost = object : UIContainer() {
                        override fun onInputEvent(event: InputEvent): Boolean {
                            return try {
                                super.onInputEvent(event)
                            } catch (e: Exception) {
                                Logger.e(TAG, "Test ${currentTest?.name} failed handling input", e)
                                currentStage?.let { showError(it, e) }
                                true
                            }
                        }

                        override fun onUpdate(clock: FrameClock) {
                            stageClock.processFrame()
                            updateViewport(bounds.width, bounds.height)
                            try {
                                super.onUpdate(stageClock)
                            } catch (e: Exception) {
                                Logger.e(TAG, "Test ${currentTest?.name} failed while updating", e)
                                currentStage?.let { showError(it, e) }
                            }
                        }
                    }.also { +it }

                    stageHost.apply {
                        width = fill()
                        height = fill()
                        clipping = ClipMode.Bounds
                        +viewport
                    }

                    // Over the stage, text doesn't take input so the test below stays usable.
                    logText = text {
                        theme = flatTheme
                        width = fill()
                        anchor = Anchor.BottomLeft
                        origin = Anchor.BottomLeft
                        padding = Padding(12f.dip, 6f.dip)
                        fontSize = Verktex.theme.typography.caption.size
                        color = Color4(1f, 1f, 1f, 0.7f)
                        text = " "
                    }
                }
            }
        }

        // Opened once the browser is shown, the scenes pushed by the test go into its stage then.
        pendingTest = initialTest?.let(::findTest) ?: suites.firstOrNull()?.let { s -> s.tests.firstOrNull()?.let { s to it } }
    }


    override fun onEnter() {
        super.onEnter()

        val pending = pendingTest
        if (pending != null) {
            pendingTest = null
            select(pending.first, pending.second)
        } else {
            Verktex.sceneHost = currentStage
        }
    }

    override fun onLeave() {
        super.onLeave()

        // Scenes pushed while the browser isn't shown (e.g. by another activity of the engine) are
        // the engine's own.
        if (Verktex.sceneHost === currentStage) {
            Verktex.sceneHost = null
        }
    }


    private fun findTest(query: String): Pair<UITestSuite, UITest>? {
        for (suite in suites) {
            for (test in suite.tests) {
                if (idOf(suite, test).equals(query, true) || test.name.equals(query, true)) {
                    return suite to test
                }
            }
        }
        return null
    }

    private fun select(suite: UITestSuite, test: UITest) {
        currentTest?.let { testButtons[it]?.isSelected = false }
        currentTest = test
        testButtons[test]?.isSelected = true

        titleText.text = "${suite.name} / ${test.name}"
        onTestSelected(idOf(suite, test))
        reload()
    }

    /**
     * Builds the current test again on a new stage.
     */
    fun reload() {
        val test = currentTest ?: return

        disposeStage()
        viewport.clearChildren()
        logLines.clear()
        logText.text = " "

        val stage = UITestStage(::log)
        viewport += stage
        currentStage = stage

        // Overlays opened by the test (e.g. modals) are kept inside the stage, above its content,
        // instead of covering the whole browser. A scene, as in the game, so they keep its safe area.
        val overlayLayer = UIScene()
        viewport += overlayLayer
        currentOverlayLayer = overlayLayer
        Verktex.overlayHost = overlayLayer
        // Likewise for scenes, they are shown inside the stage.
        Verktex.sceneHost = stage

        try {
            test.build(stage)
        } catch (e: Throwable) {
            Logger.e(TAG, "Failed to build test ${test.name}", e)
            showError(stage, e)
        }
    }

    /**
     * Closes the current test, so global state it changed is restored before the next one is built.
     */
    private fun disposeStage() {
        currentStage?.dispose()
        currentStage = null

        // Detached before the next test, so singleton overlays (e.g. menus shown with addOverlay)
        // don't keep the parent of the old stage.
        currentOverlayLayer?.clearChildren()
        currentOverlayLayer = null

        Verktex.sceneHost = null
        Verktex.overlayHost = null
    }

    private fun showError(stage: UITestStage, e: Throwable) {
        stage.dispose()
        currentOverlayLayer?.clearChildren()

        stage.scrollableContainer {
            width = fill()
            height = fill()

            text {
                width = fill()
                padding = Padding(16f.dip)
                color = Color4.Red400
                text = e.stackTraceToString().take(4000)
            }
        }
    }

    private fun updateViewport(hostWidth: Float, hostHeight: Float) {
        val safeArea = if (hasSafeArea) Verktex.graphics.window.insets.safeArea else Vec4.Zero
        currentStage?.safeArea = safeArea
        currentOverlayLayer?.embeddedSafeArea = safeArea

        if (!isFittingWindow) {
            viewport.width = fill()
            viewport.height = fill()
            viewport.scale = 1f
            return
        }

        val windowWidth = Verktex.graphics.window.windowWidth.toFloat()

        // Before the first layout, a scale of 0 would turn transformations into NaN.
        if (windowWidth <= 0f || hostWidth <= 0f || hostHeight <= 0f) {
            return
        }

        // As wide as the window, and as high as the area once scaled, so it fills it.
        val scale = minOf(hostWidth / windowWidth, 1f)
        virtualWidth = hostWidth / scale
        virtualHeight = hostHeight / scale

        viewport.setSize(virtualWidth, virtualHeight)
        viewport.scale = scale
    }

    private fun randomizeAccent() {
        val accent = oklch(0.85f, 0.09f, Random.nextFloat() * 360f)
        val base = Verktex.theme
        viewport.theme = Theme(
            palette = Theme.ColorPalette(accent),
            radius = base.radius,
            gap = base.gap,
            typography = base.typography,
            controlStyle = base.controlStyle,
            buttonStyle = base.buttonStyle,
            cardStyle = base.cardStyle,
            badgeStyle = base.badgeStyle,
        )
        log("Accent #${(accent.toHex() and 0xFFFFFF).toString(16).padStart(6, '0').uppercase()}")
    }

    private fun viewportIcon() = if (isFittingWindow) FAIcon.Mobile else FAIcon.Expand

    private fun safeAreaIcon() = if (hasSafeArea) FAIcon.BorderAll else FAIcon.BorderNone

    private fun log(message: String) {
        Logger.i(TAG, message)

        logLines.addLast(message)
        while (logLines.size > MAX_LOG_LINES) {
            logLines.removeFirst()
        }
        logText.text = logLines.joinToString("\n")
    }


    companion object {
        private const val TAG = "UITest"
        private const val MAX_LOG_LINES = 3

        private val BACKGROUNDS = arrayOf(
            Color4(0.08f, 0.08f, 0.1f),
            Color4.Black,
            Color4(0.5f, 0.5f, 0.5f),
            Color4.White,
        )

        private val RATES = listOf(0.25f, 0.5f, 1f, 2f, 4f)

        private fun formatRate(rate: Float) = "${rate.toString().removeSuffix(".0")}x"

        fun idOf(suite: UITestSuite, test: UITest) = "${suite.name}/${test.name}"
    }
}
