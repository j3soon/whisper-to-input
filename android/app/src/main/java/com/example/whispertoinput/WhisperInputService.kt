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
import android.os.IBinder
import android.text.TextUtils
import android.view.inputmethod.InputMethodManager
import android.widget.Toast

private const val RECORDED_AUDIO_FILENAME = "recorded.m4a"
private const val AUDIO_MEDIA_TYPE = "audio/mp4"
private const val IME_SWITCH_OPTION_AVAILABILITY_API_LEVEL = 28

class WhisperInputService : InputMethodService() {
    private var whisperKeyboard: WhisperKeyboard = WhisperKeyboard()
    private var whisperJobManager: WhisperTranscriber = WhisperTranscriber()
    private var recorderManager: RecorderManager = RecorderManager()
    private var recordedAudioFilename: String = ""

    private fun transcriptionCallback(text: String?) {
        if (!text.isNullOrEmpty()) {
            currentInputConnection?.commitText(text, 1)
        }

        whisperKeyboard.reset()
    }

    private fun transcriptionExceptionCallback(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onCreateInputView(): View {
        // Assigns the file name for recorded audio
        recordedAudioFilename = "${externalCacheDir?.absolutePath}/${RECORDED_AUDIO_FILENAME}"

        // Should offer ime switch?
        val shouldOfferImeSwitch: Boolean =
            if (Build.VERSION.SDK_INT >= IME_SWITCH_OPTION_AVAILABILITY_API_LEVEL) {
                shouldOfferSwitchingToNextInputMethod()
            } else {
                val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                val token: IBinder? = window?.window?.attributes?.token
                inputMethodManager.shouldOfferSwitchingToNextInputMethod(token)
            }

        // Returns the keyboard after setting it up and inflating its layout
        return whisperKeyboard.setup(
            layoutInflater,
            shouldOfferImeSwitch,
            { onStartRecording() },
            { onCancelRecording() },
            { onStartTranscription() },
            { onCancelTranscription() },
            { onDeleteText() },
            { onSwitchIme() },
            { onOpenSettings() })
    }

    private fun onStartRecording() {
        // Upon starting recording, check whether audio permission is granted.
        if (!recorderManager.allPermissionsGranted(this)) {
            // If not, launch app MainActivity (for permission setup).
            launchMainActivity()
            whisperKeyboard.reset()
            return
        }

        recorderManager.start(this, recordedAudioFilename)
    }

    private fun onCancelRecording() {
        recorderManager.stop()
    }

    private fun onStartTranscription() {
        recorderManager.stop()
        whisperJobManager.startAsync(
            this,
            recordedAudioFilename,
            AUDIO_MEDIA_TYPE,
            { transcriptionCallback(it) },
            { transcriptionExceptionCallback(it) }
        )
    }

    private fun onCancelTranscription() {
        whisperJobManager.stop()
    }

    private fun onDeleteText() {
        val inputConnection = currentInputConnection ?: return
        val selectedText = inputConnection.getSelectedText(0)

        // Deletes cursor pointed text, or all selected texts
        if (TextUtils.isEmpty(selectedText)) {
            inputConnection.deleteSurroundingText(1, 0)
        } else {
            inputConnection.commitText("", 1)
        }
    }

    private fun onSwitchIme() {
        // Before API Level 28, switchToPreviousInputMethod() was not available
        if (Build.VERSION.SDK_INT >= IME_SWITCH_OPTION_AVAILABILITY_API_LEVEL) {
            switchToPreviousInputMethod()
        } else {
            val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            val token: IBinder? = window?.window?.attributes?.token
            inputMethodManager.switchToLastInputMethod(token)
        }

    }

    private fun onOpenSettings() {
        launchMainActivity()
    }

    // Opens up app MainActivity
    private fun launchMainActivity() {
        val dialogIntent = Intent(this, MainActivity::class.java)
        dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(dialogIntent)
    }

    override fun onWindowShown() {
        super.onWindowShown()
        whisperJobManager.stop()
        whisperKeyboard.reset()
        recorderManager.stop()
    }

    override fun onWindowHidden() {

        super.onWindowHidden()
        whisperJobManager.stop()
        whisperKeyboard.reset()
        recorderManager.stop()
    }
}
