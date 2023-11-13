package com.example.whispertoinput

import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout

class WhisperKeyboard
{
    private enum class KeyboardStatus
    {
        Idle,       // Ready to start recording
        Recording,  // Currently recording
        Waiting,    // Waiting for speech-to-text results
    }

    // Keyboard event listeners. Assignable custom behaviors upon certain UI events (user-operated).
    private var onStartRecording : () -> Unit = { }
    private var onCancelRecording : () -> Unit = { }
    private var onStartTranscribing : () -> Unit = { }
    private var onCancelTranscribing : () -> Unit = { }

    // Keyboard Status
    private var keyboardStatus : KeyboardStatus = KeyboardStatus.Idle

    // Views & Keyboard Layout
    private var keyboardView : ConstraintLayout? = null
    private var buttonMic : ImageButton? = null
    private var buttonRecordingDone : ImageButton? = null
    private var labelStatus : TextView? = null
    private var waitingIcon : ProgressBar? = null

    fun Setup(
        onStartRecording : () -> Unit,
        onCancelRecording : () -> Unit,
        onStartTranscribing : () -> Unit,
        onCancelTranscribing: () -> Unit) : View
    {
        // TODO: Inflate the keyboard layout & assign views

        // TODO: Set onClick listeners

        // Set event listeners
        this.onStartRecording = onStartRecording
        this.onCancelRecording = onCancelRecording
        this.onStartTranscribing = onStartTranscribing
        this.onCancelTranscribing = onCancelTranscribing

        // Resets keyboard upon setup
        Reset()

        // Returns the keyboard view (non-nullable)
        return keyboardView!!
    }

    fun Reset()
    {
        // TODO: Reset Keyboard
    }
}
