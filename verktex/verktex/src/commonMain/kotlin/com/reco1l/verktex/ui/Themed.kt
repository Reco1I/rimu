@file:OptIn(ExperimentalTypeInference::class)

package com.reco1l.verktex.ui

import com.reco1l.verktex.Verktex
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Border
import com.reco1l.verktex.data.Dimension
import com.reco1l.verktex.data.Padding
import com.reco1l.verktex.data.Shadow
import kotlin.experimental.ExperimentalTypeInference
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * A value that can depend on a [Theme], such as a [themed] dimension or color, or a value made of
 * them like a padding.
 *
 * Themeable properties of a component (see [UIComponent.themeable]) resolve these values with the
 * component's theme, and again whenever it changes.
 */
interface ThemeDependent<out T> {

    /**
     * Whether this value depends on the theme, i.e. it's or contains a [themed] value.
     */
    val isThemed: Boolean

    /**
     * Resolves this value with the given theme into one that doesn't depend on it.
     */
    fun resolve(theme: Theme): T
}

/**
 * Something applied with a theme, then again whenever the theme of its component changes.
 */
fun interface ThemeBinding {
    fun apply(theme: Theme)
}


/**
 * A dimension taken from the theme, e.g. `height = themed { buttonStyle.medium.height }`. Inside
 * the block, `this` is the theme of the component it's assigned to.
 *
 * Used outside a themeable property it resolves with [Verktex.theme].
 */
@OverloadResolutionByLambdaReturnType
fun themed(block: Theme.() -> Dimension.Fixed): Dimension.Fixed = Dimension.Fixed.Themed(block)

/**
 * A color taken from the theme, e.g. `color = themed { palette.accent }`. Inside the block, `this`
 * is the theme of the component it's assigned to.
 *
 * Used outside a themeable property it takes the channels of the color in [Verktex.theme] when
 * created.
 */
@OverloadResolutionByLambdaReturnType
fun themed(block: Theme.() -> Color4): Color4 = ThemedColor4(block)

/**
 * Font settings taken from the theme, e.g. `fontSettings = themed { typography.caption }`. Inside
 * the block, `this` is the theme of the component it's assigned to.
 */
@OverloadResolutionByLambdaReturnType
fun themed(block: Theme.() -> Theme.FontSettings): Theme.FontSettings = ThemedFontSettings(block)


/**
 * A padding taken from the theme, e.g. `padding = themed { badgeStyle.medium.padding }`, made of
 * [themed] dimensions.
 */
@OverloadResolutionByLambdaReturnType
fun themed(block: Theme.() -> Padding): Padding = Padding(
    left = themed { block().left },
    top = themed { block().top },
    right = themed { block().right },
    bottom = themed { block().bottom },
)

/**
 * A border taken from the theme, made of [themed] dimensions and colors.
 */
@OverloadResolutionByLambdaReturnType
fun themed(block: Theme.() -> Border): Border = Border(
    width = themed { block().width },
    color = themed { block().color },
    colorBottom = themed { block().colorBottom },
)

/**
 * A shadow taken from the theme, made of [themed] dimensions and colors. Whether it's a cutout is
 * taken from [Verktex.theme] when created.
 */
@OverloadResolutionByLambdaReturnType
fun themed(block: Theme.() -> Shadow): Shadow = Shadow(
    color = themed { block().color },
    offsetY = themed { block().offsetY },
    blur = themed { block().blur },
    cutout = Verktex.theme.block().cutout,
)


/**
 * Resolves [value] with [theme] if it depends on it, otherwise returns it as is.
 */
@Suppress("UNCHECKED_CAST")
fun <T> resolveThemed(value: T, theme: Theme): T {
    return if (value is ThemeDependent<*> && value.isThemed) value.resolve(theme) as T else value
}

/**
 * Whether this value depends on the theme.
 */
val Any?.isThemed: Boolean
    get() = this is ThemeDependent<*> && isThemed


/**
 * A [Color4] taken from the theme, see [themed]. The channels set through [copy] override the ones
 * of the resolved color, so changing the alpha of a component keeps its color themed.
 */
class ThemedColor4 private constructor(
    val block: Theme.() -> Color4,
    private val redOverride: Float?,
    private val greenOverride: Float?,
    private val blueOverride: Float?,
    private val alphaOverride: Float?,
    snapshot: Color4,
) : Color4(
    redOverride ?: snapshot.red,
    greenOverride ?: snapshot.green,
    blueOverride ?: snapshot.blue,
    alphaOverride ?: snapshot.alpha,
) {

    constructor(block: Theme.() -> Color4) : this(block, null, null, null, null, Verktex.theme.block())

    private constructor(
        block: Theme.() -> Color4,
        redOverride: Float?,
        greenOverride: Float?,
        blueOverride: Float?,
        alphaOverride: Float?,
    ) : this(block, redOverride, greenOverride, blueOverride, alphaOverride, Verktex.theme.block())

    override val isThemed get() = true

    override fun resolve(theme: Theme): Color4 {
        val color = resolveThemed(theme.block(), theme)
        if (redOverride == null && greenOverride == null && blueOverride == null && alphaOverride == null) {
            return color
        }
        return Color4(
            redOverride ?: color.red,
            greenOverride ?: color.green,
            blueOverride ?: color.blue,
            alphaOverride ?: color.alpha,
        )
    }

    override fun copy(red: Float, green: Float, blue: Float, alpha: Float): Color4 {
        // Only the channels that change are overridden, the others keep following the theme.
        return ThemedColor4(
            block,
            if (red != this.red) red else redOverride,
            if (green != this.green) green else greenOverride,
            if (blue != this.blue) blue else blueOverride,
            if (alpha != this.alpha) alpha else alphaOverride,
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is ThemedColor4
            && block == other.block
            && redOverride == other.redOverride
            && greenOverride == other.greenOverride
            && blueOverride == other.blueOverride
            && alphaOverride == other.alphaOverride
    }

    override fun hashCode(): Int {
        var result = block.hashCode()
        result = 31 * result + redOverride.hashCode()
        result = 31 * result + greenOverride.hashCode()
        result = 31 * result + blueOverride.hashCode()
        result = 31 * result + alphaOverride.hashCode()
        return result
    }

    override fun toString() = "ThemedColor4(${super.toString()})"
}

/**
 * [Theme.FontSettings] taken from the theme, see [themed]. Its fields are the ones of the settings in
 * [Verktex.theme] when created.
 */
class ThemedFontSettings private constructor(
    val block: Theme.() -> Theme.FontSettings,
    snapshot: Theme.FontSettings,
) : Theme.FontSettings(
    size = snapshot.size,
    family = snapshot.family,
    weight = snapshot.weight,
    style = snapshot.style,
) {

    constructor(block: Theme.() -> Theme.FontSettings) : this(block, Verktex.theme.block())

    override val isThemed get() = true

    override fun resolve(theme: Theme): Theme.FontSettings = resolveThemed(theme.block(), theme)
}


/**
 * A property of a component that can be assigned [themed] values. It keeps the assigned value and
 * holds the one resolved with the component's theme, which is the one it returns.
 *
 * Assigning a value that doesn't depend on the theme replaces the binding.
 */
class ThemeableProperty<T> internal constructor(
    private val owner: UIComponent,
    initial: T,
    private val onChanged: (T) -> Unit,
) : ReadWriteProperty<UIComponent, T>, ThemeBinding {

    /**
     * The value as assigned, which might depend on the theme.
     */
    var raw: T = initial
        private set

    /**
     * The value resolved with the theme of the component.
     */
    var value: T = initial
        get() {
            // A themed initial value is resolved when first read rather than while the component is
            // constructed, which can happen while Verktex is.
            if (!isResolved) {
                isResolved = true
                field = resolveThemed(raw, owner.resolvedTheme)
            }
            return field
        }
        private set

    private var isResolved = !initial.isThemed


    init {
        if (initial.isThemed) {
            owner.setThemeBinding(this, this)
        }
    }


    override fun getValue(thisRef: UIComponent, property: KProperty<*>) = value

    override fun setValue(thisRef: UIComponent, property: KProperty<*>, value: T) {
        raw = value
        if (value.isThemed) {
            owner.setThemeBinding(this, this)
            update(resolveThemed(value, owner.resolvedTheme))
        } else {
            // Not reading the theme here, components are also constructed while Verktex is.
            owner.setThemeBinding(this, null)
            update(value)
        }
    }

    override fun apply(theme: Theme) {
        update(resolveThemed(raw, theme))
    }

    private fun update(resolved: T) {
        if (isResolved && value == resolved) return
        isResolved = true
        value = resolved
        onChanged(resolved)
    }
}
