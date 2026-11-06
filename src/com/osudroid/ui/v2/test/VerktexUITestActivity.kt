package com.osudroid.ui.v2.test

import com.reco1l.verktex.android.UITestActivity
import com.reco1l.verktex.testing.VerktexUITests

/**
 * The UI test browser of Verktex's own components, without the game's. It lives in the app since
 * Verktex has no Android application of its own, see [OsuUITestActivity] for the game's.
 */
class VerktexUITestActivity : UITestActivity() {

    override fun createSuites() = listOf(VerktexUITests)
}
