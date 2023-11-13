package com.example.whispertoinput

import kotlinx.coroutines.*

class WhisperJobManager
{
    private var currentTranscriptionJob : Job? = null

    fun startTranscriptionJobAsync(callback: (String?) -> Unit)
    {
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

    fun clearTranscriptionJob()
    {
        registerTranscriptionJob(null)
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
