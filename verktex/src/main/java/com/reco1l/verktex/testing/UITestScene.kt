package com.reco1l.verktex.testing

import com.reco1l.verktex.Logger
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.ui.container.container
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Axis
import com.reco1l.verktex.data.ClipMode
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.data.fill
import com.reco1l.verktex.ui.FAIcon
import com.reco1l.verktex.ui.UIButton
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
                        icon = FAIcon.ArrowRotateRight
                        variant = UIButton.Variant.Surface
                        onActionUp = { reload() }
                    }

                    iconButton {
                        icon = FAIcon.Palette
                        variant = UIButton.Variant.Surface
                        onActionUp = {
                            backgroundIndex = (backgroundIndex + 1) % BACKGROUNDS.size
                            stageHost.backgroundColor = BACKGROUNDS[backgroundIndex]
                        }
                    }
                }

                container {
                    width = fill()
                    height = fill()

                    // The tests run on a clock of their own, so they can be paused or sped up.
                    stageHost = object : UIContainer() {
                        override fun onUpdate(clock: FrameClock) {
                            stageClock.processFrame()
                            super.onUpdate(stageClock)
                        }
                    }.also { +it }

                    stageHost.apply {
                        width = fill()
                        height = fill()
                        clipping = ClipMode.Bounds
                        backgroundColor = BACKGROUNDS[0]
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

        val initial = initialTest?.let(::findTest) ?: suites.firstOrNull()?.let { s -> s.tests.firstOrNull()?.let { s to it } }
        initial?.let { (suite, test) -> select(suite, test) }
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

        stageHost.clearChildren()
        logLines.clear()
        logText.text = " "

        val stage = UITestStage(::log)
        stageHost += stage

        // Overlays opened by the test (e.g. modals) are kept inside the stage, above its content,
        // instead of covering the whole browser.
        val overlayLayer = UIContainer().apply {
            width = fill()
            height = fill()
        }
        stageHost += overlayLayer
        Verktex.overlayHost = overlayLayer

        try {
            test.build(stage)
        } catch (e: Throwable) {
            Logger.e(TAG, "Failed to build test ${test.name}", e)

            stage.clearChildren()
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
    }

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
