package com.example.whispertoinput

import android.inputmethodservice.InputMethodService
import android.text.TextUtils
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.ImageButton
import androidx.constraintlayout.widget.ConstraintLayout


class WhisperInputService : InputMethodService()
{
    private var keyboardView : ConstraintLayout? = null

    private fun setupKeyboardView()
    {
        val buttonMic = keyboardView!!.getViewById(R.id.btn_mic) as ImageButton
        buttonMic.setOnClickListener{ onButtonMicClick(it) }
    }

    private fun onButtonMicClick(it: View)
    {
        val buttonMic = it as ImageButton
    }

    override fun onCreateInputView(): View
    {
        keyboardView = layoutInflater.inflate(R.layout.keyboard_view, null) as ConstraintLayout
        setupKeyboardView()
        return keyboardView!!
    }
}
