package com.reco1l.verktex.testing

import com.reco1l.verktex.data.Padding
import kotlin.math.max
import kotlin.math.min
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import com.reco1l.verktex.ui.container.UIContainer
import com.reco1l.verktex.ui.UIPath
import com.reco1l.verktex.math.Path
import com.reco1l.verktex.data.Anchor
import com.reco1l.verktex.data.Border
import com.reco1l.verktex.data.Color4
import com.reco1l.verktex.data.Vec2
import com.reco1l.verktex.data.dip
import com.reco1l.verktex.data.fill
import com.reco1l.verktex.math.Easing
import com.reco1l.verktex.ui.FAIcon
import com.reco1l.verktex.ui.UIBadge
import com.reco1l.verktex.ui.UIButton
import com.reco1l.verktex.ui.UIScene
import com.reco1l.verktex.ui.shape.UIBox
import com.reco1l.verktex.ui.badge
import com.reco1l.verktex.ui.card
import com.reco1l.verktex.ui.container.UILinearContainer.Orientation
import com.reco1l.verktex.ui.container.container
import com.reco1l.verktex.ui.container.linearContainer
import com.reco1l.verktex.ui.control.UICheckbox
import com.reco1l.verktex.ui.control.UISelect
import com.reco1l.verktex.ui.control.UITabSelector
import com.reco1l.verktex.ui.control.slider
import com.reco1l.verktex.ui.control.textInput
import com.reco1l.verktex.ui.control.IntegerTextInput
import com.reco1l.verktex.ui.dialog.UIConfirmDialog
import com.reco1l.verktex.ui.dialog.UIMessageDialog
import com.reco1l.verktex.ui.dialog.UIPromptDialog
import com.reco1l.verktex.ui.form.FormCheckbox
import com.reco1l.verktex.ui.form.FormContainer
import com.reco1l.verktex.ui.form.FormInput
import com.reco1l.verktex.ui.form.FormSlider
import com.reco1l.verktex.ui.gradientBox
import com.reco1l.verktex.ui.icon
import com.reco1l.verktex.ui.iconButton
import com.reco1l.verktex.ui.labeledBadge
import com.reco1l.verktex.ui.loadingIndicator
import com.reco1l.verktex.ui.rotateTo
import com.reco1l.verktex.ui.shape.box
import com.reco1l.verktex.ui.shape.circle
import com.reco1l.verktex.ui.shape.line
import com.reco1l.verktex.ui.shape.triangle
import com.reco1l.verktex.ui.shape.wedge
import com.reco1l.verktex.ui.text.faSolidIcon
import com.reco1l.verktex.ui.text.text
import com.reco1l.verktex.ui.text.spans
import com.reco1l.verktex.ui.textButton
import com.reco1l.verktex.ui.transformSequence
import com.reco1l.verktex.ui.toast.Toasts
import kotlin.math.roundToInt

/**
 * Tests for the components built into Verktex.
 */
object VerktexUITests : UITestSuite("Verktex") {

    init {
        test("Text") {
            column {
                val typography = com.reco1l.verktex.Verktex.theme.typography

                section("Typography") {
                    orientation = Orientation.Vertical
                    text { text = "Heading 1"; fontSettings = typography.h1 }
                    text { text = "Heading 2"; fontSettings = typography.h2 }
                    text { text = "Heading 3"; fontSettings = typography.h3 }
                    text { text = "Body text"; fontSettings = typography.body }
                    text { text = "Small body text"; fontSettings = typography.smallBody }
                    text { text = "Caption"; fontSettings = typography.caption }
                }

                section("Icons") {
                    text { text = "Leading"; leadingIcon = faSolidIcon(FAIcon.Star) }
                    text { text = "Trailing"; trailingIcon = faSolidIcon(FAIcon.Heart) }
                    icon { iconCode = FAIcon.Gear }
                    icon { iconCode = FAIcon.Music }
                }

                section("Wrapping") {
                    text {
                        width = 300f.dip
                        text = "A long paragraph that should wrap inside a fixed width of three hundred " +
                            "density independent pixels, breaking between words."
                    }
                    text {
                        width = 300f.dip
                        maxLines = 1
                        text = "A single line that is cut once it reaches the end of its width."
                    }
                }
            }
        }

        test("Spanned text") {
            column {
                section("Styles") {
                    orientation = Orientation.Vertical
                    text {
                        spans {
                            +"Plain, "
                            color(Color4.Red400) { +"colored, " }
                            bold { +"bold, " }
                            italic { +"italic, " }
                            bold { italic { +"both, " } }
                            size(24f.dip) { +"bigger, " }
                            background(Color4(0.3f, 0.3f, 0.6f)) { +" highlighted " }
                            +" and "
                            font(com.reco1l.verktex.Verktex.theme.typography.h3) { +"another font" }
                        }
                    }
                }

                section("Icons") {
                    orientation = Orientation.Vertical
                    text {
                        spans {
                            icon(FAIcon.Star)
                            space(6f.dip)
                            bold { +"5.00" }
                        }
                    }
                    text {
                        spans {
                            +"Icons "
                            color(Color4.Red400) { icon(FAIcon.Heart) }
                            +" in the middle "
                            icon(FAIcon.Music)
                            +" of a text"
                        }
                    }
                    text {
                        spans {
                            size(32f.dip) { icon(FAIcon.CircleInfo) }
                            space(12f.dip)
                            +"A big icon centered with the text"
                        }
                    }
                    text {
                        text = "Leading and trailing icons"
                        leadingIcon = faSolidIcon(FAIcon.ArrowLeft)
                        trailingIcon = faSolidIcon(FAIcon.ArrowRight)
                        iconSpacing = 8f.dip
                    }
                }

                section("Mixed sizes share a baseline") {
                    text {
                        spans {
                            size(32f.dip) { bold { +"128" } }
                            size(14f.dip) { color(Color4(1f, 1f, 1f, 0.6f)) { +" ms" } }
                        }
                    }
                }

                section("Wrapping across spans") {
                    orientation = Orientation.Vertical
                    text {
                        width = 300f.dip
                        spans {
                            +"A paragraph with "
                            bold { +"bold words" }
                            +" and "
                            color(Color4.Red400) { +"colored ones" }
                            +" that wraps between words, even "
                            icon(FAIcon.Star)
                            +" around icons and "
                            size(22f.dip) { +"bigger text" }
                            +" inside a fixed width."
                        }
                    }
                    text {
                        width = 300f.dip
                        maxLines = 2
                        spans {
                            bold { +"Truncated: " }
                            +"a long text limited to two lines that ends with an ellipsis once it doesn't fit anymore."
                        }
                    }
                }
            }
        }

        test("Buttons") {
            column {
                for (density in UIButton.Density.entries) {
                    section("Density: $density") {
                        for (variant in UIButton.Variant.entries) {
                            textButton {
                                text = variant.name
                                this.variant = variant
                                this.density = density
                                onActionUp = { log("Clicked $variant ($density)") }
                            }
                        }
                    }
                }

                section("States") {
                    textButton { text = "Disabled"; isEnabled = false }
                    textButton { text = "Selected"; isSelected = true }
                    textButton { text = "Leading icon"; leadingIcon = faSolidIcon(FAIcon.Play) }
                    textButton { text = "Trailing icon"; trailingIcon = faSolidIcon(FAIcon.Plus) }
                    textButton {
                        text = "Long press"
                        onActionLongPress = { log("Long pressed") }
                    }
                }

                section("Icon buttons") {
                    for (variant in UIButton.Variant.entries) {
                        iconButton {
                            icon = FAIcon.Gear
                            this.variant = variant
                            onActionUp = { log("Clicked icon $variant") }
                        }
                    }
                }
            }
        }

        test("Badges") {
            column {
                for (density in UIBadge.Density.entries) {
                    section("Density: $density") {
                        for (variant in UIBadge.Variant.entries) {
                            badge {
                                text = variant.name
                                this.variant = variant
                                this.density = density
                            }
                        }
                        badge {
                            text = "Icon"
                            leadingIcon = faSolidIcon(FAIcon.Star)
                            this.density = density
                        }
                    }
                }

                section("Labeled") {
                    for (variant in UIBadge.Variant.entries) {
                        labeledBadge {
                            label = "Label"
                            value = variant.name
                            this.variant = variant
                        }
                    }
                }
            }
        }

        test("Controls") {
            column {
                section("Checkbox") {
                    +UICheckbox().apply { onValueChange = { log("Checkbox: $it") } }
                    +UICheckbox(true).apply { onValueChange = { log("Checkbox: $it") } }
                }

                section("Slider") {
                    orientation = Orientation.Vertical
                    slider {
                        width = 300f.dip
                        value = 0.5f
                        onValueChange = { log("Slider: $it") }
                    }
                    slider {
                        width = 300f.dip
                        min = -10f
                        max = 10f
                        step = 1f
                        onValueChange = { log("Stepped slider: $it") }
                    }
                }

                section("Text input") {
                    orientation = Orientation.Vertical
                    textInput {
                        width = 300f.dip
                        onValueChange = { log("Input: $it") }
                    }
                    textInput {
                        width = 300f.dip
                        placeholder = "Search..."
                        trailingIcon = faSolidIcon(FAIcon.MagnifyingGlass)
                        onValueChange = { log("Search: $it") }
                    }
                    textInput {
                        width = 300f.dip
                        placeholder = "Username"
                        leadingIcon = faSolidIcon(FAIcon.User)
                        trailingIcon = faSolidIcon(FAIcon.Xmark)
                    }
                }

                section("Select") {
                    +UISelect<String>().apply {
                        width = 220f.dip
                        options = listOf(
                            UISelect.Option("easy", "Easy"),
                            UISelect.Option("normal", "Normal"),
                            UISelect.Option("hard", "Hard", leadingIcon = faSolidIcon(FAIcon.Star)),
                        )
                        onValueChange = { log("Select: $it") }
                    }
                    +UISelect<String>().apply {
                        width = 220f.dip
                        selectionMode = UISelect.SelectionMode.Multiple
                        placeholder = "Multiple"
                        options = listOf("A", "B", "C", "D").map { UISelect.Option(it, it) }
                        onValueChange = { log("Multi select: $it") }
                    }
                }

                section("Tab selector") {
                    +UITabSelector().apply {
                        for (name in listOf("General", "Audio", "Graphics")) {
                            addButton(name) { log("Tab: $name") }
                        }
                        selectedTab = 0
                    }
                }
            }
        }

        test("Forms") {
            column {
                +FormContainer().apply {
                    width = 400f.dip
                    orientation = Orientation.Vertical

                    +FormCheckbox(true).apply { label = "Checkbox"; key = "checkbox" }
                    +FormSlider(0.5f).apply { label = "Slider"; key = "slider" }
                    +FormInput("Text").apply { label = "Input"; key = "input" }

                    onSubmit = { log("Submitted: $it") }
                }
            }
        }

        test("Cards") {
            column {
                card {
                    width = 400f.dip
                    title = "Card"
                    content { text { text = "The content of a card." } }
                }
                card {
                    width = 400f.dip
                    title = "Collapsible card"
                    isCollapsible = true
                    onExpandStatusChange = { log("Expanded: $it") }
                    content {
                        text { text = "Tap the title to collapse it." }
                        textButton { text = "A button" }
                    }
                }
            }
        }

        test("Dialogs") {
            column {
                section("Dialogs") {
                    textButton {
                        text = "Message"
                        onActionUp = {
                            UIMessageDialog().apply {
                                title = "Message"
                                text = "A message dialog with a single button."
                                addButton { text = "OK"; onActionUp = { hide() } }
                            }.show()
                        }
                    }
                    textButton {
                        text = "Confirm"
                        onActionUp = {
                            UIConfirmDialog().apply {
                                title = "Confirm"
                                text = "Do you want to continue?"
                                onConfirm = { log("Confirmed") }
                                onCancel = { log("Cancelled") }
                            }.show()
                        }
                    }
                    textButton {
                        text = "Prompt"
                        onActionUp = {
                            UIPromptDialog(IntegerTextInput(120, 30, 300)).apply {
                                title = "BPM"
                                text = "A prompt dialog asking for a number."
                                addButton { text = "OK"; onActionUp = { log("Entered: $value"); hide() } }
                                addButton { text = "Cancel"; onActionUp = { hide() } }
                            }.show()
                        }
                    }
                }
            }
        }

        test("Toasts") {
            var task: Toasts.Toast? = null
            var elapsed = 0f

            // A task that shows its progress after a second of loading.
            onEachFrame { clock ->
                val toast = task ?: return@onEachFrame
                elapsed += clock.elapsedFrameTime

                if (elapsed >= 1f) {
                    val progress = ((elapsed - 1f) / 3f).coerceAtMost(1f)
                    toast.isIndeterminate = false
                    toast.progress = progress
                    toast.title = "A task (${(progress * 100).roundToInt()}%)"

                    if (progress >= 1f) {
                        toast.dismiss()
                        task = null
                    }
                }
            }
            onDispose { task?.dismiss() }

            fun startTask() {
                task?.dismiss()
                elapsed = 0f
                task = Toasts.showPersistent("Working on something...", title = "A task", isIndeterminate = true)
            }

            // Shown right away, so a screenshot of the test has a toast.
            startTask()
            Toasts.show("A short message.")

            column {
                section("Toasts") {
                    textButton {
                        text = "Short"
                        onActionUp = { Toasts.show("A short message.") }
                    }
                    textButton {
                        text = "Long"
                        onActionUp = { Toasts.show("A longer message that stays on screen a bit more, and wraps when it doesn't fit in one line.", Toasts.Long) }
                    }
                    textButton {
                        text = "Progress"
                        onActionUp = { startTask() }
                    }
                }
            }
        }

        test("Shapes") {
            column {
                section("Shapes") {
                    box { width = 80f.dip; height = 80f.dip; color = Color4.Red400 }
                    box { width = 80f.dip; height = 80f.dip; radius = 16f.dip; color = Color4.Green400 }
                    circle { width = 80f.dip; height = 80f.dip; color = Color4.Blue400 }
                    circle { width = 80f.dip; height = 80f.dip; endAngle = 270f; color = Color4.Yellow400 }
                    triangle { width = 80f.dip; height = 80f.dip; color = Color4.Purple400 }
                    wedge { width = 160f.dip; height = 80f.dip; color = Color4.Orange400 }
                }

                section("Lines") {
                    container {
                        width = 200f.dip
                        height = 80f.dip
                        line { fromPoint = Vec2(0f, 0f); toPoint = Vec2(200f, 80f); lineWidth = 2f.dip }
                        line { fromPoint = Vec2(0f, 80f); toPoint = Vec2(200f, 0f); lineWidth = 6f.dip }
                    }
                }

                section("Gradients") {
                    for (angle in listOf(0f, 45f, 90f)) {
                        gradientBox {
                            width = 120f.dip
                            height = 80f.dip
                            gradientAngle = angle
                            colorStart = Color4.Pink400
                            colorEnd = Color4.Blue600
                        }
                    }
                }

                section("Borders") {
                    container {
                        width = 120f.dip
                        height = 80f.dip
                        radius = 12f.dip
                        border = Border(2f.dip, Color4.White)
                    }
                }
            }
        }

        test("Paths") {
            // A wave and a loop crossing itself, in the colours of a slider body.
            fun wave() = Path.Builder().apply {
                for (i in 0..40) add(i * 6f, 40f + sin(i / 40f * 2f * PI.toFloat()) * 30f)
            }.build()

            fun loop() = Path.Builder().apply {
                for (i in 0..60) {
                    val t = i / 60f * 2f * PI.toFloat()
                    add(110f + cos(t) * 70f - t * 12f, 50f + sin(t) * 40f)
                }
            }.build()

            fun UIContainer.path(path: Path, block: UIPath.() -> Unit = {}) = UIPath().apply {
                this.path = path
                halfWidth = 14f
                borderWidth = 2.5f
                hintWidth = 4f
                bodyColor = Color4.Pink400.alpha(0.7f)
                borderColor = Color4.White
                hintColor = Color4.Pink200.alpha(0.5f)
                block()
            }.also { this += it }

            fun UIContainer.pathCell(block: UIContainer.() -> Unit) = container {
                width = 260f.dip
                height = 110f.dip
                padding = Padding(12f.dip)
                block()
            }

            val snakingIn = mutableListOf<UIPath>()
            val snakingOut = mutableListOf<UIPath>()
            val snakingBoth = mutableListOf<UIPath>()

            column {
                section("Static") {
                    pathCell { path(wave()) }
                    pathCell { path(loop()) { hintWidth = 0f } }
                    pathCell { path(Path.of(20f, 40f)) }
                    pathCell { path(Path.of(0f, 40f, 200f, 40f)) { borderWidth = 14f } }
                }

                section("Snaking") {
                    pathCell { snakingIn += path(wave()) }
                    pathCell { snakingOut += path(wave()) }
                    pathCell { snakingBoth += path(loop()) }
                }
            }

            var time = 0f
            onEachFrame { clock ->
                time = (time + clock.elapsedFrameTime) % 2f
                val progress = min(1f, time)

                snakingIn.forEach { it.endLength = it.length * progress }
                snakingOut.forEach { it.startLength = it.length * progress }
                snakingBoth.forEach {
                    it.startLength = it.length * max(0f, progress - 0.4f)
                    it.endLength = it.length * min(1f, progress + 0.2f)
                }
            }
        }

        test("Loading indicators") {
            column {
                section("Sizes") {
                    loadingIndicator { width = 24f.dip; height = 24f.dip }
                    loadingIndicator { width = 48f.dip; height = 48f.dip; thickness = 5f.dip }
                    loadingIndicator { width = 96f.dip; height = 96f.dip; thickness = 8f.dip; arcColor = Color4.Pink400 }
                }
            }
        }

        test("Transforms") {
            val target = container {
                anchor = Anchor.Center
                origin = Anchor.Center
                width = 100f.dip
                height = 100f.dip
                radius = 12f.dip
                backgroundColor = Color4.Pink400
            }

            linearContainer {
                anchor = Anchor.BottomCenter
                origin = Anchor.BottomCenter
                y = (-16f).dip
                spacing = 8f.dip

                textButton {
                    text = "Scale"
                    onActionUp = {
                        target.transformSequence {
                            scaleTo(1.5f, 0.3f, Easing.OutBack)
                            scaleTo(1f, 0.3f, Easing.OutQuad)
                        }
                    }
                }
                textButton {
                    text = "Rotate"
                    onActionUp = { target.rotateTo(target.rotationZ + 90f, 0.4f, Easing.OutQuint) }
                }
                textButton {
                    text = "Move"
                    onActionUp = {
                        target.transformSequence {
                            moveToX(-100f, 0.3f, Easing.OutQuad)
                            moveToX(100f, 0.6f, Easing.InOutQuad)
                            moveToX(0f, 0.3f, Easing.InQuad)
                        }
                    }
                }
            }
        }

        test("Embedded scenes") {
            allowsNavigation = true
            var depth = 0

            fun sceneAt(level: Int): UIScene = object : UIScene() {
                init {
                    // Drawn over the whole scene and not affected by its layout.
                    background = UIBox().apply {
                        color = BACKGROUND_COLORS[level % BACKGROUND_COLORS.size]
                    }

                    linearContainer {
                        orientation = Orientation.Vertical
                        anchor = Anchor.Center
                        origin = Anchor.Center
                        spacing = 8f.dip

                        text { text = "Scene $level" }
                        textButton {
                            text = "Push"
                            onActionUp = { com.reco1l.verktex.Verktex.pushScene(sceneAt(++depth)) }
                        }
                        textButton {
                            text = "Pop"
                            onActionUp = { com.reco1l.verktex.Verktex.popScene()?.let { depth-- } }
                        }
                    }
                }

                override fun onEnter() = log("Scene $level entered")
                override fun onLeave() = log("Scene $level left")
            }

            scene(sceneAt(0))
        }

        addContainerTests()
    }

    private val BACKGROUND_COLORS = arrayOf(
        Color4(0.2f, 0.25f, 0.4f),
        Color4(0.35f, 0.2f, 0.3f),
        Color4(0.2f, 0.35f, 0.25f),
    )
}
