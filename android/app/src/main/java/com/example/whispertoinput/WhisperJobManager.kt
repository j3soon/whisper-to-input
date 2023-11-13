package com.example.whispertoinput

import kotlinx.coroutines.*

class WhisperJobManager
{
    private var currentTranscriptionJob : Job? = null

    fun startTranscriptionJobAsync(callback: (String?) -> Unit)
    {
        // TODO: Start a transcription job
    }

    fun clearTranscriptionJob()
    {
        // TODO: clear current transcription job
    }

    private fun registerTranscriptionJob(job : Job?)
    {
        if (currentTranscriptionJob != null)
        {
            currentTranscriptionJob!!.cancel()
        }

        currentTranscriptionJob = job
    }
}
