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

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.File

private const val MEDIA_RECORDER_CONSTRUCTOR_DEPRECATION_API_LEVEL = 31
private const val AMPLITUDE_UPDATE_PERIOD: Long = 150

class RecorderManager {
    companion object {
        fun requiredPermissions() = arrayOf(Manifest.permission.RECORD_AUDIO)
    }

    private var recorder: MediaRecorder? = null
    private var onUpdateMicrophoneAmplitude: (Int) -> Unit = { }
    private var microhphoneAmplitudeUpdateJob: Job? = null

    fun start(context: Context, filename: String) {
        recorder?.apply {
            stop()
            release()
        }

        recorder =
            if (Build.VERSION.SDK_INT >= MEDIA_RECORDER_CONSTRUCTOR_DEPRECATION_API_LEVEL) {
                MediaRecorder(context)
            } else {
                MediaRecorder()
            }

        recorder!!.apply {
            setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(filename)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)

            try {
                prepare()
            } catch (e: IOException) {
                Log.e("whisper-input", "prepare() failed")
            }

            start()
        }

        // Start a job to periodically report current amplitude
        microhphoneAmplitudeUpdateJob?.cancel()
        microhphoneAmplitudeUpdateJob = CoroutineScope(Dispatchers.Main).launch {
            while (recorder != null) {
                val amplitude = recorder?.maxAmplitude ?: 0
                onUpdateMicrophoneAmplitude(amplitude)
                delay(AMPLITUDE_UPDATE_PERIOD)
            }
        }
    }

    fun stop() {
        recorder?.apply {
            stop()
            release()
        }
        recorder = null

        microhphoneAmplitudeUpdateJob?.cancel()
        microhphoneAmplitudeUpdateJob = null
    }

    // Assign onUpdateMicrophoneAmplitude callback
    fun setOnUpdateMicrophoneAmplitude(onUpdateMicrophoneAmplitude: (Int) -> Unit) {
        this.onUpdateMicrophoneAmplitude = onUpdateMicrophoneAmplitude
    }

    // Returns whether all of the permissions are granted.
    fun allPermissionsGranted(context: Context): Boolean {
        for (permission in requiredPermissions()) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }

        return true
    }
}