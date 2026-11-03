package com.reco1l.verktex.testing

import com.reco1l.verktex.Verktex
import com.reco1l.verktex.data.Axis
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.data.fill
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.ui.container.UIContainer
import com.reco1l.verktex.ui.container.UILinearContainer
import com.reco1l.verktex.ui.container.linearContainer
import com.reco1l.verktex.ui.container.scrollableContainer
import com.reco1l.verktex.ui.text.text

/**
 * A visual test shown in the UI test browser.
 *
 * @param name The name shown in the list of tests.
 * @param build Fills the [UITestStage] with the components under test. It runs again every time the
 * test is reloaded, on a new stage.
 */
class UITest(val name: String, val build: UITestStage.() -> Unit)

/**
 * A named group of [UITest]s.
 */
open class UITestSuite(val name: String) {

    private val mutableTests = mutableListOf<UITest>()

    /**
     * The tests of this suite, in the order they were added.
     */
    val tests: List<UITest>
        get() = mutableTests

    /**
     * Adds a test to this suite.
     */
    fun test(name: String, build: UITestStage.() -> Unit) {
        mutableTests += UITest(name, build)
    }
}

/**
 * The container a [UITest] builds its components into. It fills the area of the browser next to the
 * list of tests.
 */
class UITestStage internal constructor(private val logger: (String) -> Unit) : UIContainer() {

    private val frameListeners = mutableListOf<(FrameClock) -> Unit>()


    init {
        width = fill()
        height = fill()
    }


    /**
     * Shows a message in the log bar of the browser, useful to check callbacks.
     */
    fun log(message: String) = logger(message)

    /**
     * Runs [block] every frame while this test is shown.
     */
    fun onEachFrame(block: (FrameClock) -> Unit) {
        frameListeners += block
    }

    override fun onUpdate(clock: FrameClock) {
        frameListeners.forEach { it(clock) }
        super.onUpdate(clock)
    }
}


/**
 * Adds a vertically scrollable column filling the stage, the usual layout to show a set of
 * components.
 */
inline fun UITestStage.column(block: UILinearContainer.() -> Unit) = scrollableContainer {
    scrollAxes = Axis.Y
    width = fill()
    height = fill()

    linearContainer {
        orientation = UILinearContainer.Orientation.Vertical
        width = fill()
        padding = Padding(16f.dip)
        spacing = 20f.dip
        block()
    }
}

/**
 * Adds a titled row of components, meant to be used inside a [column].
 */
inline fun UIContainer.section(title: String, block: UILinearContainer.() -> Unit) = linearContainer {
    orientation = UILinearContainer.Orientation.Vertical
    width = fill()
    spacing = 8f.dip

    text {
        text = title
        fontSize = Verktex.theme.typography.caption.size
        color = Color4(1f, 1f, 1f, 0.6f)
    }

    linearContainer {
        orientation = UILinearContainer.Orientation.Horizontal
        spacing = 8f.dip
        block()
    }
}
