package com.example.whispertoinput

import android.view.LayoutInflater
import android.view.View
import android.view.View.inflate
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout

class WhisperKeyboard {
    private enum class KeyboardStatus {
        Idle,       // Ready to start recording
        Recording,  // Currently recording
        Waiting,    // Waiting for speech-to-text results
    }

    // Keyboard event listeners. Assignable custom behaviors upon certain UI events (user-operated).
    private var onStartRecording: () -> Unit = { }
    private var onCancelRecording: () -> Unit = { }
    private var onStartTranscribing: () -> Unit = { }
    private var onCancelTranscribing: () -> Unit = { }

    // Keyboard Status
    private var keyboardStatus: KeyboardStatus = KeyboardStatus.Idle

    // Views & Keyboard Layout
    private var keyboardView: ConstraintLayout? = null
    private var buttonMic: ImageButton? = null
    private var buttonRecordingDone: ImageButton? = null
    private var labelStatus: TextView? = null
    private var waitingIcon: ProgressBar? = null

    fun setup(
        layoutInflater: LayoutInflater,
        onStartRecording: () -> Unit,
        onCancelRecording: () -> Unit,
        onStartTranscribing: () -> Unit,
        onCancelTranscribing: () -> Unit
    ): View {
        // Inflate the keyboard layout & assign views
        keyboardView = layoutInflater.inflate(R.layout.keyboard_view, null) as ConstraintLayout
        buttonMic = keyboardView!!.findViewById(R.id.btn_mic) as ImageButton
        buttonRecordingDone = keyboardView!!.findViewById(R.id.btn_recording_done) as ImageButton
        labelStatus = keyboardView!!.findViewById(R.id.label_status) as TextView
        waitingIcon = keyboardView!!.findViewById(R.id.pb_waiting_icon) as ProgressBar

        // Set onClick listeners
        buttonMic!!.setOnClickListener { onButtonMicClick() }
        buttonRecordingDone!!.setOnClickListener { onButtonRecordingDoneClick() }

        // Set event listeners
        this.onStartRecording = onStartRecording
        this.onCancelRecording = onCancelRecording
        this.onStartTranscribing = onStartTranscribing
        this.onCancelTranscribing = onCancelTranscribing

        // Resets keyboard upon setup
        reset()

        // Returns the keyboard view (non-nullable)
        return keyboardView!!
    }

    fun reset() {
        setKeyboardStatus(KeyboardStatus.Idle)
    }

    private fun onButtonMicClick() {
        // Upon button mic click...
        // Idle -> Start Recording
        // Recording -> Cancel Recording
        // Waiting -> Cancel Transcribing
        when (keyboardStatus) {
            KeyboardStatus.Idle -> {
                setKeyboardStatus(KeyboardStatus.Recording)
                onStartRecording()
            }

            KeyboardStatus.Recording -> {
                setKeyboardStatus(KeyboardStatus.Idle)
                onCancelRecording()
            }

            KeyboardStatus.Waiting -> {
                setKeyboardStatus(KeyboardStatus.Idle)
                onCancelTranscribing()
            }
        }
    }

    private fun onButtonRecordingDoneClick() {
        // Upon button recording done click.
        // Recording -> Start transcribing
        // else -> nothing
        if (keyboardStatus == KeyboardStatus.Recording) {
            setKeyboardStatus(KeyboardStatus.Waiting)
            onStartTranscribing()
        }
    }

    private fun setKeyboardStatus(newStatus: KeyboardStatus) {
        if (keyboardStatus == newStatus) {
            return
        }

        when (newStatus) {
            KeyboardStatus.Idle -> {
                labelStatus!!.setText(R.string.whisper_to_input)
                buttonMic!!.setImageResource(R.drawable.mic_idle)
                waitingIcon!!.visibility = View.INVISIBLE
                buttonRecordingDone!!.visibility = View.GONE
            }

            KeyboardStatus.Recording -> {
                labelStatus!!.setText(R.string.recording)
                buttonMic!!.setImageResource(R.drawable.mic_pressed)
                waitingIcon!!.visibility = View.INVISIBLE
                buttonRecordingDone!!.visibility = View.VISIBLE
            }

            KeyboardStatus.Waiting -> {
                labelStatus!!.setText(R.string.transcribing)
                buttonMic!!.setImageResource(R.drawable.mic_transcribing)
                waitingIcon!!.visibility = View.VISIBLE
                buttonRecordingDone!!.visibility = View.GONE
            }
        }

        keyboardStatus = newStatus
    }
}
