package com.reco1l.verktex.android

import android.content.Context
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.testing.UITestScene
import com.reco1l.verktex.testing.UITestSuite
import com.reco1l.verktex.testing.VerktexUITests

/**
 * An activity to browse and try UI components in isolation.
 *
 * Projects extend it, return their suites from [createSuites] (usually with [VerktexUITests] first) and
 * register it in their manifest. As the engine is a singleton, the activity should run in a process of
 * its own (`android:process=":uitest"`) so it never shares it with the game.
 *
 * The test to open first can be passed with the [EXTRA_TEST] extra, as `suite/test` or just the test
 * name. Without it, the last opened test is shown again.
 */
abstract class UITestActivity : AndroidGameHost() {

    /**
     * Creates the suites shown in the browser. Called once the engine is initialized, so textures and
     * fonts can be loaded.
     */
    abstract fun createSuites(): List<UITestSuite>


    override fun onEngineInitialized() {}

    override fun onEngineStarted() {
        val preferences = getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val initialTest = intent.getStringExtra(EXTRA_TEST) ?: preferences.getString(KEY_LAST_TEST, null)

        Verktex.withTreeLock {
            Verktex.pushScene(UITestScene(createSuites(), initialTest) { id ->
                preferences.edit().putString(KEY_LAST_TEST, id).apply()
            })
        }
    }


    companion object {
        /**
         * The intent extra with the test to open first.
         */
        const val EXTRA_TEST = "test"

        private const val PREFERENCES_NAME = "verktex_ui_tests"
        private const val KEY_LAST_TEST = "last_test"
    }
}
