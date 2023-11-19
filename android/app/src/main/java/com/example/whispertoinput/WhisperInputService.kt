/*
 * This file is part of Whisper To Input, see <https://github.com/j3soon/whisper-to-input>.
 *
 * Copyright (c) 2023 Yan-Bin Diau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

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
import android.widget.Toast

private const val RECORDED_AUDIO_FILENAME = "recorded.m4a"

class WhisperInputService : InputMethodService() {
    private var whisperKeyboard: WhisperKeyboard = WhisperKeyboard()
    private var whisperJobManager: WhisperTranscriber = WhisperTranscriber()
    private var recorderManager: RecorderManager = RecorderManager()
    private var recordedAudioFilename: String = ""

    private fun transcriptionCallback(text: String?) {
        if (!text.isNullOrEmpty()) {
            currentInputConnection?.commitText(text, text.length)
        }

        whisperKeyboard.reset()
    }

    private fun transcriptionExceptionCallback(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onCreateInputView(): View {
        // Assigns the file name for recorded audio
        recordedAudioFilename = "${externalCacheDir?.absolutePath}/${RECORDED_AUDIO_FILENAME}"

        // Returns the keyboard after setting it up and inflating its layout
        return whisperKeyboard.setup(
            layoutInflater,
            { onStartRecording() },
            { onCancelRecording() },
            { onStartTranscription() },
            { onCancelTranscription() })
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
            { transcriptionCallback(it) },
            { transcriptionExceptionCallback(it) }
        )
    }

    private fun onCancelTranscription() {
        whisperJobManager.stop()
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
