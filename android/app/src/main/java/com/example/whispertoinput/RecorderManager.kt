package com.example.whispertoinput

import android.Manifest
import android.media.MediaRecorder

class RecorderManager {
    companion object {
        fun requiredPermissions() = arrayOf(Manifest.permission.RECORD_AUDIO)
    }

    private var recorder: MediaRecorder? = null

    fun start() {
        TODO("Implement RecorderManager.start()")
    }

    fun stop() {
        TODO("Implement RecorderManager.stop()")
    }

}