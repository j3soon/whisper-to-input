package com.example.whispertoinput

import android.content.Context
import android.util.Log
import com.aallam.openai.api.audio.TranscriptionRequest
import com.aallam.openai.api.file.FileSource
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import okio.FileSystem
import okio.Path.Companion.toPath

class WhisperJobManager {
    private var currentTranscriptionJob: Job? = null

    fun startTranscriptionJobAsync(
        context: Context,
        filename: String,
        callback: (String?) -> Unit
    ) {
        suspend fun whisperTranscription(): String {
            val apiKey = context.dataStore.data.map { preferences ->
                preferences[API_KEY]
            }.first()
            val openai = OpenAI(
                token = apiKey ?: "",
            )
            val request = TranscriptionRequest(
                audio = FileSource(
                    name = filename,
                    source = FileSystem.SYSTEM.source(filename.toPath())
                ),
                model = ModelId("whisper-1"),
                language = "zh"
            )
            val transcription = openai.transcription(request)

            return transcription.text
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
                } catch (e: Exception) {
                    return@withContext null
                }
            }

            // This callback is within the main thread.
            callback.invoke(result)
        }

        registerTranscriptionJob(job)
    }

    fun clearTranscriptionJob() {
        registerTranscriptionJob(null)
    }

    private fun registerTranscriptionJob(job: Job?) {
        if (currentTranscriptionJob != null) {
            currentTranscriptionJob!!.cancel()
        }

        currentTranscriptionJob = job
    }
}
