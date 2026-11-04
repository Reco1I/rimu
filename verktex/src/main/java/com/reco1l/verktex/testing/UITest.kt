package com.reco1l.verktex.testing

import com.reco1l.verktex.Logger
import com.reco1l.verktex.SceneHost
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.data.Axis
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.data.fill
import com.reco1l.verktex.time.FrameClock
import com.reco1l.verktex.ui.UIScene
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
 *
 * While the test is shown, the stage is the [Verktex.sceneHost]: scenes pushed with
 * [Verktex.pushScene] are shown inside it, and [Verktex.popScene] goes back through them without
 * ever leaving the browser.
 */
class UITestStage internal constructor(private val logger: (String) -> Unit) : UIContainer(), SceneHost {

    private val frameListeners = mutableListOf<(FrameClock) -> Unit>()

    private val disposeListeners = mutableListOf<() -> Unit>()

    private val scenes = ArrayDeque<UIScene>()


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

    /**
     * Runs [block] when the test is closed or reloaded, after its scenes were popped. Used to restore
     * global state changed by the test. The blocks run in the reverse order they were added.
     */
    fun onDispose(block: () -> Unit) {
        disposeListeners += block
    }

    /**
     * Pushes [scene] into the stage, as [Verktex.pushScene] would do in the test.
     */
    fun <T : UIScene> scene(scene: T): T {
        pushScene(scene)
        return scene
    }


    override val currentScene: UIScene?
        get() = scenes.lastOrNull()

    override fun pushScene(scene: UIScene) {
        val previous = scenes.lastOrNull()

        // Singleton scenes keep the parent of a previous stage otherwise.
        scene.removeSelf()
        scenes.addLast(scene)
        this += scene

        if (previous != null) {
            previous.isVisible = false
            previous.onLeave()
        }
        scene.isVisible = true
        scene.onEnter()
    }

    override fun popScene(): UIScene? {
        val scene = scenes.removeLastOrNull() ?: run {
            log("Nothing to pop, the test is the root scene")
            return null
        }

        this -= scene
        scene.onLeave()

        scenes.lastOrNull()?.let {
            it.isVisible = true
            it.onEnter()
        }
        return scene
    }

    /**
     * Pops the scenes of the test and runs the [onDispose] blocks.
     */
    internal fun dispose() {
        while (scenes.isNotEmpty()) {
            val scene = scenes.removeLast()
            this -= scene
            runCatching { scene.onLeave() }.onFailure { Logger.e(TAG, "Failed to leave scene", it) }
        }

        for (index in disposeListeners.indices.reversed()) {
            runCatching { disposeListeners[index]() }.onFailure { Logger.e(TAG, "Failed to dispose test", it) }
        }
        disposeListeners.clear()
        frameListeners.clear()
        clearChildren()
    }

    override fun onUpdate(clock: FrameClock) {
        frameListeners.forEach { it(clock) }
        super.onUpdate(clock)
    }


    private companion object {
        const val TAG = "UITest"
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
