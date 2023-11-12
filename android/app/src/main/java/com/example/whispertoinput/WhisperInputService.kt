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
    private enum class KeyboardStatus
    {
        Idle,       // Ready to start recording
        Recording,  // Currently recording
        Waiting,    // Waiting for speech-to-text results
    }

    private var keyboardView : ConstraintLayout? = null
    private var buttonMic : ImageButton? = null
    private var keyboardStatus : KeyboardStatus = KeyboardStatus.Idle

    private fun setupKeyboardView()
    {
        buttonMic = keyboardView!!.getViewById(R.id.btn_mic) as ImageButton
        buttonMic!!.setOnClickListener{ onButtonMicClick(it) }
    }

    private fun onButtonMicClick(it: View)
    {
        // Determine the next keyboard status upon mic button click.
        // Idle -> Start recording
        // Recording -> Cancel recording
        // Waiting -> Cancel waiting
        when (keyboardStatus)
        {
            KeyboardStatus.Idle -> setKeyboardStatus(KeyboardStatus.Recording)
            KeyboardStatus.Recording -> setKeyboardStatus(KeyboardStatus.Idle)
            KeyboardStatus.Waiting -> setKeyboardStatus(KeyboardStatus.Idle)
        }
    }

    private fun setKeyboardStatus(newStatus : KeyboardStatus)
    {
        // TODO: Different actions depending on different orig status
        keyboardStatus = newStatus
    }

    override fun onCreateInputView(): View
    {
        keyboardView = layoutInflater.inflate(R.layout.keyboard_view, null) as ConstraintLayout
        setupKeyboardView()
        return keyboardView!!
    }
}
