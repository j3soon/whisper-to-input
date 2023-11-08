package com.example.whispertoinput

import android.inputmethodservice.InputMethodService
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.view.KeyboardShortcutInfo
import android.view.View

class WhisperInputService : InputMethodService(), KeyboardView.OnKeyboardActionListener {
    private var keyboardView : KeyboardView? = null
    private var keyboard : Keyboard? = null
    private var caps = false

    override fun onPress(primaryCode: Int) {
        TODO("Not yet implemented")
    }

    override fun onRelease(primaryCode: Int) {
        TODO("Not yet implemented")
    }

    override fun onKey(primaryCode: Int, keyCodes: IntArray?) {
        TODO("Not yet implemented")
    }

    override fun onText(text: CharSequence?) {
        TODO("Not yet implemented")
    }

    override fun swipeLeft() {
        TODO("Not yet implemented")
    }

    override fun swipeRight() {
        TODO("Not yet implemented")
    }

    override fun swipeDown() {
        TODO("Not yet implemented")
    }

    override fun swipeUp() {
        TODO("Not yet implemented")
    }

    override fun onCreateInputView(): View {
        keyboardView = layoutInflater.inflate(R.layout.keyboard_view, null) as KeyboardView
        keyboard = Keyboard(this, R.xml.key_layout)
        keyboardView!!.keyboard = keyboard
        keyboardView!!.setOnKeyboardActionListener(this)
        return keyboardView!!
    }
}
