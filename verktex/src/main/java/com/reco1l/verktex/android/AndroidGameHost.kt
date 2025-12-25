package com.reco1l.verktex.android

import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView.BufferType
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.getSystemService
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.widget.doOnTextChanged
import com.reco1l.verktex.GameHost
import com.reco1l.verktex.input.KeyboardEvent
import com.reco1l.verktex.input.PointerEvent
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.input.Input
import com.reco1l.verktex.ui.ITextInput

/**
 * An abstract class that serves as a base for Android game hosts. It extends [AppCompatActivity]
 * and implements [GameHost].
 *
 * This class is responsible for initializing the Verktex engine, handling input events, and managing
 * the lifecycle of the game.
 */
abstract class AndroidGameHost : AppCompatActivity(), GameHost {

    private lateinit var insetsController: WindowInsetsControllerCompat


    // Using a hidden EditText is the only way to properly handle soft keyboard
    // input in Android allowing UTF-16 characters, selection and clipboard operations.
    private val editTextView by lazy { AppCompatEditText(this).apply {
        layoutParams = ViewGroup.LayoutParams(1, 1)
        alpha = 0f

        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        imeOptions = EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_FULLSCREEN or EditorInfo.IME_FLAG_NO_EXTRACT_UI

        isClickable = false
    } }

    private var currentTextWatcher: TextWatcher? = null

    /**
     * Whether the screen is kept on while the game is in the foreground.
     */
    var keepScreenOn = true
        set(value) {
            field = value
            runOnUiThread { applyKeepScreenOn() }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val windowView = Verktex.initializeAndroid(this)
        setContentView(windowView)
        onEngineInitialized()

        insetsController = WindowCompat.getInsetsController(window, windowView)

        // This allows the user to swipe from the edge of the screen to temporarily reveal the system
        // bars with a semi-transparent overlay. The bars will automatically hide after a short delay.
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        hideSystemBars()
        applyKeepScreenOn()

        Verktex.start()
        onEngineStarted()
    }


    private fun applyKeepScreenOn() {
        if (keepScreenOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun hideSystemBars() {
        insetsController.hide(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.navigationBars())
    }


    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        Verktex.onGameWindowFocusChange(hasFocus)
        if (hasFocus) {
            hideSystemBars()
        }
    }


    override fun attachTextInput(textInput: ITextInput) = runOnUiThread {
        if (currentTextWatcher != null)
            editTextView.removeTextChangedListener(currentTextWatcher)

        // The view must be part of the hierarchy to receive focus and IME input.
        if (editTextView.parent == null) {
            (window.decorView as ViewGroup).addView(editTextView)
        }

        editTextView.setText(textInput.value, BufferType.EDITABLE)
        editTextView.setSelection(editTextView.length())

        editTextView.setOnEditorActionListener { _, _, _ ->
            Verktex.withTreeLock { textInput.onImeAction() }
            true
        }

        // Rejected input is filtered before it reaches the text, reverting it afterwards would break
        // the IME's composing state.
        editTextView.filters = arrayOf(InputFilter { source, sourceStart, sourceEnd, dest, destStart, destEnd ->
            val inserted = source.subSequence(sourceStart, sourceEnd).toString()
            val result = dest.substring(0, destStart) + inserted + dest.substring(destEnd)

            if (isTextAccepted(textInput, inserted, result)) {
                null
            } else {
                Verktex.withTreeLock { textInput.notifyInputError() }
                // Keeps the replaced range as it was.
                dest.subSequence(destStart, destEnd)
            }
        })

        currentTextWatcher = editTextView.doOnTextChanged { text, _, _, _ ->
            // The text watcher runs on the UI thread, while the component tree belongs to the engine workers.
            val newText = text?.toString() ?: ""
            Verktex.withTreeLock { textInput.value = newText }
        }

        editTextView.requestFocus()
        getSystemService<InputMethodManager>()?.showSoftInput(editTextView, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun isTextAccepted(textInput: ITextInput, inserted: String, result: String): Boolean {
        if (textInput.maxCharacters > 0 && result.codePointCount(0, result.length) > textInput.maxCharacters) {
            return false
        }

        var index = 0
        while (index < inserted.length) {
            val charCount = Character.charCount(inserted.codePointAt(index))
            if (!textInput.isCharacterAllowed(inserted.substring(index, index + charCount))) {
                return false
            }
            index += charCount
        }

        return textInput.isTextValid(result)
    }

    override fun detachTextInput() = runOnUiThread {
        if (currentTextWatcher != null)
            editTextView.removeTextChangedListener(currentTextWatcher)

        currentTextWatcher = null
        editTextView.setOnEditorActionListener(null)
        editTextView.filters = arrayOf()

        getSystemService<InputMethodManager>()?.hideSoftInputFromWindow(editTextView.windowToken, 0)
        editTextView.clearFocus()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val consumed: Boolean

        if (event.action == MotionEvent.ACTION_SCROLL) {
            consumed = Input.dispatchScrollEvent(
                pointerId = event.getPointerId(event.actionIndex),
                deltaX = event.getAxisValue(MotionEvent.AXIS_HSCROLL),
                deltaY = event.getAxisValue(MotionEvent.AXIS_VSCROLL)
            )
        } else {
            val button = when (event.buttonState) {
                MotionEvent.BUTTON_PRIMARY -> PointerEvent.Button.Left
                MotionEvent.BUTTON_SECONDARY -> PointerEvent.Button.Right
                else -> PointerEvent.Button.Unknown
            }

            fun dispatch(action: PointerEvent.Action, index: Int) = Input.dispatchPointerEvent(
                action = action,
                button = button,
                pointerId = event.getPointerId(index),
                x = event.getX(index),
                y = event.getY(index),
                time = event.eventTime
            )

            // Move and cancel events carry every pointer at once, each one is dispatched.
            fun dispatchAll(action: PointerEvent.Action) =
                (0 until event.pointerCount).fold(false) { consumed, index -> dispatch(action, index) or consumed }

            // The samples batched into a move since the last one, oldest first.
            fun dispatchHistory(): Boolean {
                var consumed = false
                for (i in 0 until event.historySize) {
                    for (index in 0 until event.pointerCount) {
                        consumed = Input.dispatchPointerEvent(
                            action = PointerEvent.Action.Move,
                            button = button,
                            pointerId = event.getPointerId(index),
                            x = event.getHistoricalX(index, i),
                            y = event.getHistoricalY(index, i),
                            time = event.getHistoricalEventTime(i)
                        ) or consumed
                    }
                }
                return consumed
            }

            consumed = when (event.actionMasked) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> dispatch(PointerEvent.Action.Down, event.actionIndex)
                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> dispatch(PointerEvent.Action.Up, event.actionIndex)
                MotionEvent.ACTION_MOVE -> {
                    val historyConsumed = Input.dispatchHistoricalMoves && dispatchHistory()
                    dispatchAll(PointerEvent.Action.Move) or historyConsumed
                }
                MotionEvent.ACTION_CANCEL -> dispatchAll(PointerEvent.Action.Cancel)
                else -> dispatchAll(PointerEvent.Action.Unknown)
            }
        }

        return consumed || super.onTouchEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val consumed = Input.dispatchKeyboardEvent(
            action = KeyboardEvent.Action.Down,
            key = event.toInputKey(),
            shiftPressed = event.isShiftPressed,
            ctrlPressed = event.isCtrlPressed,
            altPressed = event.isAltPressed,
            metaPressed = event.isMetaPressed
        )
        return consumed || super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        val consumed = Input.dispatchKeyboardEvent(
            action = KeyboardEvent.Action.Up,
            key = event.toInputKey(),
            shiftPressed = event.isShiftPressed,
            ctrlPressed = event.isCtrlPressed,
            altPressed = event.isAltPressed,
            metaPressed = event.isMetaPressed
        )
        return consumed || super.onKeyUp(keyCode, event)
    }

    override fun onResume() {
        super.onResume()
        Verktex.resume()
    }

    override fun onPause() {
        super.onPause()
        Verktex.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        Verktex.dispose()
    }
}