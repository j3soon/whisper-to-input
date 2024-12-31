package com.example.whisperkeyboard

import android.inputmethodservice.InputMethodService
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.Toast

class MyKeyboardService : InputMethodService(), KeyboardView.OnKeyboardActionListener {

    private lateinit var inputView: KeyboardView
    private lateinit var qwertyKeyboard: Keyboard

    override fun onCreate() {
        super.onCreate()
        // Initialize any STT or Whisper code here if desired
    }

    override fun onCreateInputView(): View {
        // Inflate the keyboard_view layout
        val view = layoutInflater.inflate(R.layout.keyboard_view, null)
        inputView = view.findViewById(R.id.keyboard_view)

        // Load our QWERTY layout
        qwertyKeyboard = Keyboard(this, R.xml.keyboard_layout)
        inputView.keyboard = qwertyKeyboard
        inputView.setOnKeyboardActionListener(this)
        return view
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        // If needed, reset states
    }

    override fun onKey(primaryCode: Int, keyCodes: IntArray?) {
        val ic: InputConnection? = currentInputConnection
        when (primaryCode) {
            Keyboard.KEYCODE_DELETE -> {
                ic?.deleteSurroundingText(1, 0)
            }
            Keyboard.KEYCODE_DONE -> {
                ic?.sendKeyEvent(
                    android.view.KeyEvent(
                        android.view.KeyEvent.ACTION_DOWN,
                        android.view.KeyEvent.KEYCODE_ENTER
                    )
                )
            }
            -999 -> {
                // Mic button
                triggerWhisperSTT()
            }
            else -> {
                // Normal character
                val charCode = primaryCode.toChar()
                ic?.commitText(charCode.toString(), 1)
            }
        }
    }

    private fun triggerWhisperSTT() {
        // Where you'd plug in your actual STT logic (e.g., Whisper)
        Toast.makeText(this, "Mic tapped - start Whisper STT here", Toast.LENGTH_SHORT).show()
    }

    override fun onPress(primaryCode: Int) {}
    override fun onRelease(primaryCode: Int) {}
    override fun onText(text: CharSequence?) {}
    override fun swipeLeft() {}
    override fun swipeRight() {}
    override fun swipeDown() {}
    override fun swipeUp() {}
}