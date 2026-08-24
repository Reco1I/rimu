package com.reco1l.verktex.android

import android.os.Bundle
import android.os.PersistableBundle
import android.text.InputType
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView.BufferType
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.widget.doOnTextChanged
import com.reco1l.verktex.GameHost
import com.reco1l.verktex.input.KeyboardEvent
import com.reco1l.verktex.input.PointerEvent
import com.reco1l.verktex.Verktex
import com.reco1l.verktex.input.Input
import com.reco1l.verktex.ui.control.UITextInput

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
    private val editTextView = AppCompatEditText(this).apply {
        layoutParams = ViewGroup.LayoutParams(1, 1)
        alpha = 0f

        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        imeOptions = EditorInfo.IME_FLAG_NO_FULLSCREEN or EditorInfo.IME_FLAG_NO_EXTRACT_UI

        isClickable = false
    }

    private var currentTextWatcher: TextWatcher? = null


    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)

        val windowView = Verktex.initializeAndroid(this)
        setContentView(windowView)
        onEngineInitialized()

        insetsController = WindowCompat.getInsetsController(window, windowView)

        // This allows the user to swipe from the edge of the screen to temporarily reveal the system
        // bars with a semi-transparent overlay. The bars will automatically hide after a short delay.
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        hideSystemBars()

        Verktex.start()
        onEngineStarted()
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


    override fun attachTextInput(textInput: UITextInput) {
        if (currentTextWatcher != null)
            editTextView.removeTextChangedListener(currentTextWatcher)

        editTextView.setText(textInput.value, BufferType.EDITABLE)

        currentTextWatcher = editTextView.doOnTextChanged { text, start, before, count ->
            val newText = text?.toString() ?: ""

            fun revertWithError() {
                editTextView.setText(textInput.value, BufferType.EDITABLE)
                editTextView.setSelection(textInput.value.length.coerceAtMost(start))
                textInput.notifyInputError()
            }

            if (textInput.maxCharacters > 0 && newText.codePointCount(0, newText.length) > textInput.maxCharacters) {
                revertWithError()
                return@doOnTextChanged
            }

            if (count > 0) {
                val addedText = newText.substring(start, start + count)
                var codePointIndex = 0
                while (codePointIndex < addedText.length) {
                    val codePoint = addedText.codePointAt(codePointIndex)
                    val charCount = Character.charCount(codePoint)
                    val character = addedText.substring(codePointIndex, codePointIndex + charCount)

                    if (!textInput.isCharacterAllowed(character)) {
                        revertWithError()
                        return@doOnTextChanged
                    }

                    codePointIndex += charCount
                }
            }

            if (!textInput.isTextValid(newText)) {
                revertWithError()
                return@doOnTextChanged
            }

            textInput.value = newText
        }
    }

    override fun detachTextInput() {
        if (currentTextWatcher != null)
            editTextView.removeTextChangedListener(currentTextWatcher)

        currentTextWatcher = null

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
            consumed = Input.dispatchPointerEvent(
                action = when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> PointerEvent.Action.Down
                    MotionEvent.ACTION_UP -> PointerEvent.Action.Up
                    MotionEvent.ACTION_MOVE -> PointerEvent.Action.Move
                    else -> PointerEvent.Action.Unknown
                },
                pointerId = event.getPointerId(event.actionIndex),
                x = event.x,
                y = event.y,
                button = when (event.buttonState) {
                    MotionEvent.BUTTON_PRIMARY -> PointerEvent.Button.Left
                    MotionEvent.BUTTON_SECONDARY -> PointerEvent.Button.Right
                    else -> PointerEvent.Button.Unknown
                }
            )
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