package com.osudroid.game

import com.osudroid.game.session.GameplaySession
import com.osudroid.notifications.Notifications
import com.osudroid.resources.Res
import com.osudroid.settings.OsuSettings
import com.osudroid.ui.v2.GameLoaderScene
import com.osudroid.ui.v2.LoaderScene
import com.osudroid.ui.v2.SplashScene
import com.osudroid.ui.v2.settings.SettingsMenu
import com.osudroid.ui.v2.settings.SettingsSection
import com.osudroid.ui.v2.settings.minResolutionScale
import com.osudroid.utils.async
import com.osudroid.utils.updateThread
import com.reco1l.verktex.Logger
import com.reco1l.verktex.PerformanceStats
import com.reco1l.verktex.Platform
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.localization.formatString
import com.reco1l.verktex.settings.Settings
import com.reco1l.verktex.ui.Scene
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

/**
 * Watches the frame rate, in gameplay and in the menus, and suggests lowering the resolution in a
 * notification when it's unstable, which opens the graphics settings.
 *
 * Each second is sampled from [PerformanceStats]: a second is unstable when it ran below
 * [MIN_STABLE_FPS_RATIO] of the display's refresh rate (e.g. 60 frames per second on a 120 Hz
 * display, which is what vsync falls to when frames take too long) or when more than
 * [MAX_SLOW_FRAME_RATIO] of its frames took much longer than its median one. The first seconds after a scene shows are left out, and so are the loaders, the splash,
 * a paused play and the game in the background.
 *
 * - A play was unstable when at least [MIN_UNSTABLE_SECONDS] of its seconds, and
 * [MIN_UNSTABLE_RATIO] of them, were. It's suggested once the play is left, not over gameplay.
 * - The menus are unstable when [MENU_UNSTABLE_SECONDS] of their last [MENU_WINDOW_SECONDS] seconds
 * were, suggested right away.
 *
 * When the device measures the GPU's time, the suggestion is left out if the GPU wasn't what limited
 * the frame rate (it took less than [GPU_BOUND_RATIO] of a refresh), as a lower resolution wouldn't help then. It's suggested once at a given resolution
 * ([OsuSettings.LowerResolutionSuggestedAt]): again only once the player lowered it and the frame rate
 * is still unstable, and never at the lowest resolution.
 */
object FrameRateMonitor {

    /**
     * Seconds below this ratio of the display's refresh rate are unstable.
     */
    const val MIN_STABLE_FPS_RATIO = 0.9f

    /**
     * Seconds with more than this ratio of slow frames are unstable.
     */
    const val MAX_SLOW_FRAME_RATIO = 0.1f

    /**
     * How many unstable seconds a play needs to be unstable.
     */
    const val MIN_UNSTABLE_SECONDS = 15

    /**
     * The ratio of a play's seconds that must be unstable for it to be unstable.
     */
    const val MIN_UNSTABLE_RATIO = 0.25f

    /**
     * How many of the menus' last seconds are looked at.
     */
    const val MENU_WINDOW_SECONDS = 30

    /**
     * How many of [MENU_WINDOW_SECONDS] must be unstable for the menus to be unstable.
     */
    const val MENU_UNSTABLE_SECONDS = 15

    // The first seconds after a scene shows are left out, as it may still be loading its textures.
    private const val WARMUP_SECONDS = 3

    // A frame is slow if it took this many times the median one, and at least SLOW_FRAME_MARGIN more.
    private const val SLOW_FRAME_FACTOR = 1.5f
    private const val SLOW_FRAME_MARGIN = 4f

    /**
     * The GPU limits the frame rate if it took at least this ratio of a refresh of the display:
     * compared to the frame time it would miss a GPU that drops 120 Hz to 60 by just a little.
     */
    const val GPU_BOUND_RATIO = 0.8f

    // When the display doesn't tell its refresh rate.
    private const val DEFAULT_REFRESH_RATE = 60f

    private const val SAMPLE_INTERVAL = 1000L

    private const val TAG = "FrameRateMonitor"

    // Each sample is logged in debug and profile builds, to tune the thresholds.
    private val isLogging = Platform.application.isDebug || Platform.application.buildType == "profile"


    private var job: Job? = null

    private val frameTimes = FloatArray(PerformanceStats.HISTORY_SIZE)

    // The newest frame times of the last sample, to tell when no frame was drawn since.
    private var lastNewestFrames = FloatArray(3)

    private var scene: Scene? = null
    private var sceneSeconds = 0

    // The attempt being sampled, and its seconds so far.
    private var session: GameplaySession? = null
    private val play = Seconds()

    // The menus' last seconds, oldest first once it wrapped around.
    private val menuWindow = arrayOfNulls<Second>(MENU_WINDOW_SECONDS)
    private var menuWindowIndex = 0


    /**
     * Starts watching, once the game started.
     */
    fun start() {
        if (job != null) {
            return
        }
        job = async {
            while (true) {
                delay(SAMPLE_INTERVAL)
                updateThread { update() }
            }
        }
    }


    private fun update() {
        val current = GameplaySession.current

        if (current !== session) {
            // The attempt that was sampled was left, or retried.
            if (session != null && play.isUnstable(MIN_UNSTABLE_SECONDS, MIN_UNSTABLE_RATIO)) {
                suggest()
            }
            session = current
            play.clear()
            menuWindow.fill(null)
            menuWindowIndex = 0
        }

        val currentScene = Verktex.scene
        if (currentScene !== scene) {
            scene = currentScene
            sceneSeconds = 0
        }
        sceneSeconds++

        if (!Platform.isInForeground || sceneSeconds <= WARMUP_SECONDS) {
            return
        }

        if (current != null) {
            if (!current.isPaused) {
                play.add(sample() ?: return)
            }
            return
        }

        if (currentScene is SplashScene || currentScene is LoaderScene || currentScene is GameLoaderScene) {
            return
        }

        menuWindow[menuWindowIndex] = sample() ?: return
        menuWindowIndex = (menuWindowIndex + 1) % MENU_WINDOW_SECONDS

        val menus = Seconds()
        menuWindow.forEach { if (it != null) menus.add(it) }

        if (menus.isUnstable(MENU_UNSTABLE_SECONDS, 0f)) {
            menuWindow.fill(null)
            suggest()
        }
    }

    /**
     * Samples the last second, null if no frame was drawn since the last sample.
     */
    private fun sample(): Second? {
        // The frames of the last second, newest first.
        var count = 0
        var total = 0f
        for (i in PerformanceStats.HISTORY_SIZE - 1 downTo 0) {
            val time = PerformanceStats.frameTimeAt(i)
            if (time <= 0f) {
                break
            }
            frameTimes[count++] = time
            total += time
            if (total >= SAMPLE_INTERVAL) {
                break
            }
        }

        if (count < lastNewestFrames.size) {
            return null
        }

        // The same frames as the last sample, the game didn't draw since.
        var isSame = true
        for (i in lastNewestFrames.indices) {
            if (lastNewestFrames[i] != frameTimes[i]) {
                isSame = false
            }
            lastNewestFrames[i] = frameTimes[i]
        }
        if (isSame) {
            return null
        }

        val fps = count * 1000f / total
        frameTimes.sort(0, count)
        val median = frameTimes[count / 2]
        val slowThreshold = maxOf(median * SLOW_FRAME_FACTOR, median + SLOW_FRAME_MARGIN)

        var slowFrames = 0
        for (i in 0 until count) {
            if (frameTimes[i] > slowThreshold) {
                slowFrames++
            }
        }

        val refreshRate = Verktex.graphics.window.refreshRate.takeIf { it > 0f } ?: DEFAULT_REFRESH_RATE
        val refreshInterval = 1000f / refreshRate
        val gpuTime = PerformanceStats.gpuTime

        val second = when {
            fps >= refreshRate * MIN_STABLE_FPS_RATIO && slowFrames <= count * MAX_SLOW_FRAME_RATIO -> Second.Stable
            gpuTime < 0f -> Second.Unstable
            gpuTime >= refreshInterval * GPU_BOUND_RATIO -> Second.UnstableGpuBound
            else -> Second.UnstableNotGpuBound
        }

        if (isLogging) {
            Logger.i(TAG, formatString("%.1f fps at %.0f Hz, median %.2fms, %d/%d slow, gpu %.2fms: %s",
                fps, refreshRate, median, slowFrames, count, gpuTime, second))
        }
        return second
    }

    private fun suggest() {
        val scale = Settings[OsuSettings.ResolutionScale]
        val minScale = minResolutionScale() ?: return

        if (scale <= minScale || scale >= Settings[OsuSettings.LowerResolutionSuggestedAt]) {
            return
        }
        Settings[OsuSettings.LowerResolutionSuggestedAt] = scale

        Notifications.post(
            title = Res.string.unstable_frame_rate_title,
            text = Res.string.unstable_frame_rate_text,
            action = Res.string.unstable_frame_rate_action,
            onAction = {
                // Not over gameplay, the notification center is opened from the menus.
                if (GameplaySession.current == null) {
                    Verktex.pushScene(SettingsMenu(SettingsSection.Graphics))
                }
            }
        )
    }


    private enum class Second {
        Stable,

        /**
         * Unstable, and the device doesn't measure the GPU's time.
         */
        Unstable,
        UnstableGpuBound,
        UnstableNotGpuBound
    }

    private class Seconds {
        var total = 0
        var unstable = 0
        var gpuMeasured = 0
        var gpuBound = 0

        fun add(second: Second) {
            total++
            if (second == Second.Stable) {
                return
            }
            unstable++
            if (second != Second.Unstable) {
                gpuMeasured++
            }
            if (second == Second.UnstableGpuBound) {
                gpuBound++
            }
        }

        fun clear() {
            total = 0
            unstable = 0
            gpuMeasured = 0
            gpuBound = 0
        }

        /**
         * Whether at least [minSeconds] seconds, and [minRatio] of them, were unstable, most of them
         * because of the GPU when it's measured.
         */
        fun isUnstable(minSeconds: Int, minRatio: Float): Boolean {
            if (unstable < minSeconds || unstable < total * minRatio) {
                return false
            }
            return gpuMeasured == 0 || gpuBound * 2 >= gpuMeasured
        }
    }
}
