/*
 * This file is part of Whisper To Input, see <https://github.com/j3soon/whisper-to-input>.
 *
 * Copyright (c) 2023-2024 Yan-Bin Diau, Johnson Sun
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

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import com.github.liuyueyi.quick.transfer.ChineseUtils

class WhisperTranscriber {
    private data class Config(
        val endpoint: String,
        val languageCode: String,
        val speechToTextBackend: String,
        val apiKey: String,
        val model: String,
        val postprocessing: String
    )

    private val TAG = "WhisperTranscriber"
    private var currentTranscriptionJob: Job? = null

    fun startAsync(
        context: Context,
        filename: String,
        mediaType: String,
        attachToEnd: String,
        callback: (String?) -> Unit,
        exceptionCallback: (String) -> Unit
    ) {
        suspend fun makeWhisperRequest(): String {
            // Retrieve configs
            val (endpoint, languageCode, speechToTextBackend, apiKey, model, postprocessing) = context.dataStore.data.map { preferences: Preferences ->
                Config(
                    preferences[ENDPOINT] ?: "",
                    preferences[LANGUAGE_CODE] ?: "auto",
                    preferences[SPEECH_TO_TEXT_BACKEND] ?: context.getString(R.string.settings_option_openai_api),
                    preferences[API_KEY] ?: "",
                    preferences[MODEL] ?: "",
                    preferences[POSTPROCESSING] ?: context.getString(R.string.settings_option_no_conversion)
                )
            }.first()

            // Foolproof message
            if (endpoint == "") {
                throw Exception(context.getString(R.string.error_endpoint_unset))
            }

            // Make request
            val client = OkHttpClient()
            val request = buildWhisperRequest(
                context,
                filename,
                mediaType,
                speechToTextBackend,
                endpoint,
                languageCode,
                apiKey,
                model
            )
            val response = client.newCall(request).execute()

            // If request is not successful, or response code is weird
            if (!response.isSuccessful || response.code / 100 != 2) {
                throw Exception(response.body!!.string().replace('\n', ' '))
            }

            val rawText = response.body!!.string().trim()
            val processedText = when (postprocessing) {
                context.getString(R.string.settings_option_to_simplified) -> ChineseUtils.tw2s(rawText)
                context.getString(R.string.settings_option_to_traditional) -> ChineseUtils.s2tw(rawText)
                else -> rawText // No conversion
            }
            
            return processedText + attachToEnd
        }

        // Create a cancellable job in the main thread (for UI updating)
        val job = CoroutineScope(Dispatchers.Main).launch {

            // Within the job, make a suspend call at the I/O thread
            // It suspends before result is obtained.
            // Returns (transcribed string, exception message)
            val (transcribedText, exceptionMessage) = withContext(Dispatchers.IO) {
                try {
                    // Perform transcription here
                    val response = makeWhisperRequest()
                    // Clean up unused audio file after transcription
                    // Ref: https://developer.android.com/reference/android/media/MediaRecorder#setOutputFile(java.io.File)
                    File(filename).delete()
                    return@withContext Pair(response, null)
                } catch (e: CancellationException) {
                    // Task was canceled
                    return@withContext Pair(null, null)
                } catch (e: Exception) {
                    return@withContext Pair(null, e.message)
                }
            }

            // This callback is within the main thread.
            callback.invoke(transcribedText)

            // If exception message is not null
            if (!exceptionMessage.isNullOrEmpty()) {
                Log.e(TAG, exceptionMessage)
                exceptionCallback(exceptionMessage)
            }
        }

        registerTranscriptionJob(job)
    }

    fun stop() {
        registerTranscriptionJob(null)
    }

    private fun registerTranscriptionJob(job: Job?) {
        currentTranscriptionJob?.cancel()
        currentTranscriptionJob = job
    }

    private fun buildWhisperRequest(
        context: Context,
        filename: String,
        mediaType: String,
        speechToTextBackend: String,
        endpoint: String,
        languageCode: String,
        apiKey: String,
        model: String
    ): Request {
        // Please refer to the following for the endpoint/payload definitions:
        // - https://ahmetoner.com/whisper-asr-webservice/run/#usage
        // - https://platform.openai.com/docs/api-reference/audio/createTranscription
        // - https://platform.openai.com/docs/api-reference/making-requests
        // - NVIDIA NIM doesn't have documentation for HTTP-style requests.
        val file: File = File(filename)
        val fileBody: RequestBody = file.asRequestBody(mediaType.toMediaTypeOrNull())
        val requestBody: RequestBody = MultipartBody.Builder().apply {
            setType(MultipartBody.FORM)
            // Add file to payload
            if (speechToTextBackend == context.getString(R.string.settings_option_openai_api) || 
                speechToTextBackend == context.getString(R.string.settings_option_nvidia_nim)) {
                addFormDataPart("file", "@audio.m4a", fileBody)
            } else if (speechToTextBackend == context.getString(R.string.settings_option_whisper_asr_webservice)) {
                addFormDataPart("audio_file", "@audio.m4a", fileBody)
            }
            // Add backend-specific parameters to payload
            if (speechToTextBackend == context.getString(R.string.settings_option_openai_api)) {
                addFormDataPart("model", model)
                addFormDataPart("response_format", "text")
            }
            if (speechToTextBackend == context.getString(R.string.settings_option_nvidia_nim)) {
                addFormDataPart("language", languageCode)
            }
        }.build()

        val requestHeaders: Headers = Headers.Builder().apply {
            if (speechToTextBackend == context.getString(R.string.settings_option_openai_api)) {
                // Foolproof message
                if (apiKey == "") {
                    throw Exception(context.getString(R.string.error_apikey_unset))
                }
                add("Authorization", "Bearer $apiKey")
            }
            add("Content-Type", "multipart/form-data")
        }.build()

        // Build URL with endpoint-specific parameters
        val url = when (speechToTextBackend) {
            context.getString(R.string.settings_option_openai_api),
            context.getString(R.string.settings_option_whisper_asr_webservice) -> {
                "$endpoint?encode=true&task=transcribe&language=$languageCode&word_timestamps=false&output=txt"
            }
            else -> endpoint
        }

        return Request.Builder()
            .headers(requestHeaders)
            .url(url)
            .post(requestBody)
            .build()
    }
}
