package com.example.whispertoinput

import android.inputmethodservice.InputMethodService
import android.text.TextUtils
import android.view.KeyEvent
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout


class WhisperInputService : InputMethodService() {
    private var keyboardView : ConstraintLayout? = null

    override fun onCreateInputView(): View {
        keyboardView = layoutInflater.inflate(R.layout.keyboard_view, null) as ConstraintLayout
        return keyboardView!!
    }
}
