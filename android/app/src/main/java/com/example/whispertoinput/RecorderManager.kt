package com.example.whispertoinput

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import java.io.IOException

private const val MEDIA_RECORDER_CONSTRUCTOR_DEPRECATION_API_LEVEL = 31

class RecorderManager {
    companion object {
        fun requiredPermissions() = arrayOf(Manifest.permission.RECORD_AUDIO)
    }

    private var recorder: MediaRecorder? = null
    private var filename: String = ""

    fun setup(filename: String) {
        this.filename = filename
    }

    fun getFilename() = filename

    fun start(context: Context) {
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
            setAudioSource(MediaRecorder.AudioSource.MIC)
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
    }

    fun stop() {
        recorder?.apply {
            stop()
            release()
        }
        recorder = null
    }

    // Returns whether all of the permissions are granted.
    private fun isAllPermissionsGranted(context: Context): Boolean {
        for (permission in requiredPermissions()) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }

        return true
    }
}