package com.example.whispertoinput

import android.inputmethodservice.InputMethodService
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import kotlinx.coroutines.*


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
    private var buttonRecordingDone : ImageButton? = null
    private var labelStatus : TextView? = null
    private var waitingIcon : ProgressBar? = null
    private var keyboardStatus : KeyboardStatus = KeyboardStatus.Idle
    private var currentTranscriptionJob : Job? = null

    private fun setupKeyboardView()
    {
        buttonMic = keyboardView!!.findViewById(R.id.btn_mic) as ImageButton?
        buttonRecordingDone = keyboardView!!.findViewById(R.id.btn_recording_done) as ImageButton
        labelStatus = keyboardView!!.findViewById(R.id.label_status) as TextView
        waitingIcon = keyboardView!!.findViewById(R.id.pb_waiting_icon) as ProgressBar

        // Assign onClicks
        buttonMic!!.setOnClickListener{ onButtonMicClick(it) }
        buttonRecordingDone!!.setOnClickListener{ onButtonRecordingDoneClick(it) }
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

    private fun onButtonRecordingDoneClick(it: View)
    {
        // Determine the next keyboard status upon recording_done button click.
        // Recording -> End recording, start transcribing
        // else -> nothing
        if (keyboardStatus == KeyboardStatus.Recording)
        {
            setKeyboardStatus(KeyboardStatus.Waiting)
            transcribeAsync{ transcriptionCallback(it) }
        }
    }

    private fun registerTranscriptionJob(job : Job?)
    {
        if (currentTranscriptionJob != null)
        {
            currentTranscriptionJob!!.cancel()
        }

        currentTranscriptionJob = job
    }

    private fun transcriptionCallback(text : String?)
    {
        if (text == null)
        {
            return
        }

        currentInputConnection?.commitText(text, text.length)
        setKeyboardStatus(KeyboardStatus.Idle)
    }

    private fun transcribeAsync(callback: (String?) -> Unit) {
        suspend fun whisperTranscription(): String {
            // TODO: Make Whisper requests to transcribe
            // For now a text is returned after some predetermined time.
            delay(3000)
            return "Text 文字文字"
        }

        // Create a cancellable job in the main thread (for UI updating)
        val job = CoroutineScope(Dispatchers.Main).launch {

            // Within the job, make a suspend call at the I/O thread
            // It suspends before result is obtained.
            val result = withContext(Dispatchers.IO) {
                try {
                    // Perform transcription here
                    return@withContext whisperTranscription()
                } catch (e: CancellationException) {
                    // Task was canceled
                    return@withContext null
                }
            }

            // This callback is within the main thread.
            if (!result.isNullOrEmpty())
            {
                callback.invoke(result)
            }
        }

        registerTranscriptionJob(job)
    }



    private fun setKeyboardStatus(newStatus : KeyboardStatus)
    {
        if (keyboardStatus == newStatus)
        {
            return
        }

        // TODO: Different actions depending on different orig status
        when (newStatus)
        {
            KeyboardStatus.Idle ->
            {
                labelStatus!!.setText(R.string.whisper_to_input)
                buttonMic!!.setImageResource(R.drawable.mic_idle)
                buttonRecordingDone!!.visibility = View.GONE
                waitingIcon!!.visibility = View.INVISIBLE
                registerTranscriptionJob(null)
            }
            KeyboardStatus.Recording ->
            {
                labelStatus!!.setText(R.string.recording)
                buttonMic!!.setImageResource(R.drawable.mic_pressed)
                waitingIcon!!.visibility = View.INVISIBLE
                buttonRecordingDone!!.visibility = View.VISIBLE
                registerTranscriptionJob(null)
            }
            KeyboardStatus.Waiting ->
            {
                labelStatus!!.setText(R.string.transcribing)
                buttonMic!!.setImageResource(R.drawable.mic_transcribing)
                waitingIcon!!.visibility = View.VISIBLE
                buttonRecordingDone!!.visibility = View.GONE
            }
        }

        keyboardStatus = newStatus
    }

    override fun onCreateInputView(): View
    {
        keyboardView = layoutInflater.inflate(R.layout.keyboard_view, null) as ConstraintLayout
        setupKeyboardView()
        return keyboardView!!
    }

    override fun onWindowShown() {
        super.onWindowShown()
        setKeyboardStatus(KeyboardStatus.Idle)
    }

    override fun onWindowHidden() {

        super.onWindowHidden()
        setKeyboardStatus(KeyboardStatus.Idle)
        // TODO: Release some resources
    }
}
