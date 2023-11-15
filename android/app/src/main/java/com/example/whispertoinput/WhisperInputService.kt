package com.example.whispertoinput

import android.inputmethodservice.InputMethodService
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import java.io.IOException
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager

private const val RECORDED_AUDIO_FILENAME = "recorded.3gp"
private const val MEDIA_RECORDER_CONSTRUCTOR_DEPRECATION_API_LEVEL = 31

class WhisperInputService : InputMethodService()
{
    private var whisperKeyboard : WhisperKeyboard = WhisperKeyboard()
    private var whisperJobManager : WhisperJobManager = WhisperJobManager()
    private var mediaRecorder : MediaRecorder? = null
    private var fileName : String = ""

    private fun transcriptionCallback(text : String?)
    {
        if (text == null)
        {
            return
        }

        currentInputConnection?.commitText(text, text.length)
        whisperKeyboard.reset()
    }

    override fun onCreateInputView(): View {
        // Assigns the file name for recorded audio
        fileName = "${externalCacheDir?.absolutePath}/${RECORDED_AUDIO_FILENAME}"
        return whisperKeyboard.setup(
            layoutInflater,
            { onStartRecording() },
            { onCancelRecording() },
            { onStartTranscription() },
            { onCancelTranscription() })
    }

    private fun onStartRecording()
    {
        // Upon starting recording, check whether audio permission is granted.
        if (!isPermissionGranted())
        {
            // If not, launch app MainActivity (for permission setup).
            launchMainActivity()
            whisperKeyboard.reset()
            return
        }

        startRecording()
    }

    private fun onCancelRecording()
    {

    }

    private fun onStartTranscription()
    {
        whisperJobManager.startTranscriptionJobAsync { transcriptionCallback(it) }
    }

    private fun onCancelTranscription()
    {
        whisperJobManager.clearTranscriptionJob()
    }

    // Starts the recorder (assumes granted permission or throws an exception)
    private fun startRecording()
    {
        mediaRecorder = if (Build.VERSION.SDK_INT >= MEDIA_RECORDER_CONSTRUCTOR_DEPRECATION_API_LEVEL) {
            MediaRecorder(this)
        } else {
            MediaRecorder()
        }

        mediaRecorder!!.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setOutputFile(fileName)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)

            try {
                prepare()
            } catch (e: IOException) {
                Log.e("whisper-input", "prepare() failed")
            }

            start()
        }
    }

    // Opens up app MainActivity
    private fun launchMainActivity()
    {
        val dialogIntent = Intent(this, MainActivity::class.java)
        dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(dialogIntent)
    }

    // Returns whether the permission RECORD_AUDIO is granted.
    private fun isPermissionGranted() : Boolean
    {
        val microphonePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
        return (microphonePermission == PackageManager.PERMISSION_GRANTED)
    }

    // Stops the recorder (the resulting file is there to stay).
    private fun stopRecording()
    {
        mediaRecorder?.apply {
            stop()
            release()
        }
        mediaRecorder = null
    }

    override fun onWindowShown() {
        super.onWindowShown()
        whisperJobManager.clearTranscriptionJob()
        whisperKeyboard.reset()
    }

    override fun onWindowHidden() {

        super.onWindowHidden()
        whisperJobManager.clearTranscriptionJob()
        whisperKeyboard.reset()
    }
}
