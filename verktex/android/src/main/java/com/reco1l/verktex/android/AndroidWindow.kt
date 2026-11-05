package com.reco1l.verktex.android

import android.os.Build
import android.view.RoundedCorner
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.reco1l.verktex.data.Vec2
import com.reco1l.verktex.data.Vec4
import com.reco1l.verktex.graphics.GraphicsWindow
import com.reco1l.verktex.Platform
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.settings.Settings
import com.reco1l.verktex.settings.VerktexSettings
import kotlin.math.max
import kotlin.math.roundToInt
import android.content.Context as AndroidContext


class AndroidWindow(androidContext: AndroidContext) : SurfaceView(androidContext), SurfaceHolder.Callback, GraphicsWindow {

    override var windowWidth: Int = 0
    override var windowHeight: Int = 0
    // The safe area in pixels of the view, the insets are in pixels of the window, which can
    // render at a lower resolution.
    private var viewSafeArea = Vec4.Zero

    private var cachedInsets = GraphicsWindow.Insets(Vec4.Zero, Vec2.Zero)
    private var cachedInsetsSafeArea = Vec4.Zero
    private var cachedInsetsScale = 1f
    private var cachedInsetsImeHeight = 0

    override var insets: GraphicsWindow.Insets
        // The keyboard doesn't dispatch insets to this view (the window pans instead of resizing),
        // so its height is read from the root insets, which are always up to date.
        get() {
            val imeHeight = imeHeight()
            val scale = Platform.resolutionScale
            val safeArea = viewSafeArea

            // Read every frame, they're only made again when something changed.
            if (imeHeight != cachedInsetsImeHeight || scale != cachedInsetsScale || safeArea !== cachedInsetsSafeArea) {
                cachedInsetsImeHeight = imeHeight
                cachedInsetsScale = scale
                cachedInsetsSafeArea = safeArea

                cachedInsets = GraphicsWindow.Insets(
                    safeArea = Vec4(safeArea.x * scale, safeArea.y * scale, safeArea.z * scale, safeArea.w * scale),
                    offsets = Vec2(0f, -imeHeight * scale)
                )
            }
            return cachedInsets
        }
        set(value) {
            viewSafeArea = value.safeArea
        }

    @Volatile
    private var cachedImeHeight = 0
    @Volatile
    private var imeHeightQueryNanos = 0L

    // Read every frame by the draw and update workers, querying the insets allocates, so they're
    // queried a few times per second only. The content follows the keyboard eased anyway.
    private fun imeHeight(): Int {
        val now = System.nanoTime()
        if (now - imeHeightQueryNanos >= IME_QUERY_INTERVAL_NANOS) {
            imeHeightQueryNanos = now
            cachedImeHeight = ViewCompat.getRootWindowInsets(this)?.getInsets(WindowInsetsCompat.Type.ime())?.bottom ?: 0
        }
        return cachedImeHeight
    }

    @Volatile
    private var cachedRefreshRate = 60f
    @Volatile
    private var refreshRateQueryNanos = 0L

    // Read by the performance overlay a few times per second, the display mode rarely changes.
    override val refreshRate: Float
        get() {
            val now = System.nanoTime()
            if (now - refreshRateQueryNanos >= REFRESH_RATE_QUERY_INTERVAL_NANOS) {
                refreshRateQueryNanos = now
                display?.refreshRate?.takeIf { it > 0f }?.let { cachedRefreshRate = it }
            }
            return cachedRefreshRate
        }

    @Volatile
    private var surfaceDestroyed = false
    @Volatile
    private var surfaceCreated = false

    /**
     * Whether the underlying Android surface is currently valid and can be rendered to.
     */
    val hasSurface
        get() = surfaceCreated && !surfaceDestroyed

    /**
     * Incremented every time a new Android surface is created, so the EGL surface can be recreated.
     */
    @Volatile
    var surfaceGeneration = 0
        private set


    init {
        holder.addCallback(this)

        Settings.on(VerktexSettings.ResolutionScale) { post { updateBufferSize() } }

        setOnApplyWindowInsetsListener { view, viewInsets ->

            val compatInsets = ViewCompat.getRootWindowInsets(view)

            var radiusTopLeft = 0
            var radiusTopRight = 0
            var radiusBottomLeft = 0
            var radiusBottomRight = 0

            // Unfortunately there's no backward compatible API to get corner radius.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val windowInsets = view.getRootWindowInsets()

                val topLeft = windowInsets.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)
                val topRight = windowInsets.getRoundedCorner(RoundedCorner.POSITION_TOP_RIGHT)
                val bottomLeft = windowInsets.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_LEFT)
                val bottomRight = windowInsets.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_RIGHT)

                if (topLeft != null) radiusTopLeft = topLeft.radius
                if (topRight != null) radiusTopRight = topRight.radius
                if (bottomLeft != null) radiusBottomLeft = bottomLeft.radius
                if (bottomRight != null) radiusBottomRight = bottomRight.radius
            }

            val leftRadius = max(radiusTopLeft, radiusBottomLeft)
            val rightRadius = max(radiusTopRight, radiusBottomRight)

            val bars = compatInsets?.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())

            val safeArea = Vec4(
                x = max(bars?.left ?: 0, leftRadius).toFloat(),
                y = 0f,
                z = max(bars?.right ?: 0, rightRadius).toFloat(),
                w = 0f
            )

            insets = GraphicsWindow.Insets(
                safeArea = safeArea,
                offsets = Vec2.Zero
            )

            viewInsets
        }
    }


    override fun shouldClose(): Boolean {
        return surfaceDestroyed
    }

    override fun pollEvents() {
        // no-op
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        surfaceGeneration++
        surfaceDestroyed = false
        surfaceCreated = true
    }

    override fun surfaceChanged(
        holder: SurfaceHolder,
        format: Int,
        surfaceWidth: Int,
        surfaceHeight: Int
    ) {
        windowWidth = surfaceWidth
        windowHeight = surfaceHeight

        // The scale of the buffer the surface got, which the display scales up to the view.
        val scale = if (width > 0) surfaceWidth.toFloat() / width else 1f

        if (scale != Platform.resolutionScale) {
            Platform.resolutionScale = scale

            // Density independent sizes changed, everything is measured and styled again.
            Verktex.onThemeChange()
        }
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        updateBufferSize()
    }

    /**
     * Sizes the buffer of the surface following [VerktexSettings.ResolutionScale], the display
     * scales it up to the view for free.
     */
    private fun updateBufferSize() {
        if (width <= 0 || height <= 0) {
            return
        }

        val scale = Settings[VerktexSettings.ResolutionScale, 1f].coerceIn(MIN_RESOLUTION_SCALE, 1f)

        if (scale >= 1f) {
            holder.setSizeFromLayout()
        } else {
            holder.setFixedSize(
                (width * scale).roundToInt().coerceAtLeast(1),
                (height * scale).roundToInt().coerceAtLeast(1)
            )
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        surfaceDestroyed = true
    }


    private companion object {
        const val IME_QUERY_INTERVAL_NANOS = 50_000_000L
        const val REFRESH_RATE_QUERY_INTERVAL_NANOS = 500_000_000L
        const val MIN_RESOLUTION_SCALE = 0.25f
    }
}
