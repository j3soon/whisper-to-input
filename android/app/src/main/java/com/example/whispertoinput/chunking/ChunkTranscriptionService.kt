/*
 * This file is part of Whisper To Input, see <https://github.com/j3soon/whisper-to-input>.
 *
 * Copyright (c) 2023-2025 Yan-Bin Diau, Johnson Sun
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

package com.example.whispertoinput.chunking

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

class ChunkTranscriptionService(
    private val transcriber: suspend (AudioChunk) -> String,
    private val progressListener: ChunkProgressListener? = null
) {
    private val TAG = "ChunkTranscriptionService"

    suspend fun transcribe(chunks: List<AudioChunk>): String = coroutineScope {
        val semaphore = Semaphore(ChunkConfig.MAX_CONCURRENT_CHUNKS)
        val totalChunks = chunks.size

        Log.d(TAG, "Starting transcription of $totalChunks chunks")
        progressListener?.onChunkingStarted(totalChunks)

        val results = chunks.map { chunk ->
            async {
                semaphore.withPermit {
                    progressListener?.onChunkStarted(chunk.index)
                    transcribeWithRetry(chunk)
                }
            }
        }

        val transcripts = mutableListOf<ChunkTranscript>()
        val errors = mutableListOf<Pair<Int, Exception>>()
        var completedCount = 0

        results.forEachIndexed { index, deferred ->
            try {
                val text = deferred.await()
                val chunk = chunks[index]
                transcripts.add(ChunkTranscript(
                    text = text,
                    index = chunk.index,
                    startTimeMs = chunk.startTimeMs,
                    endTimeMs = chunk.endTimeMs
                ))
                completedCount++
                Log.d(TAG, "Chunk ${chunk.index} completed: $completedCount/$totalChunks")
                progressListener?.onChunkCompleted(chunk.index, completedCount, totalChunks)
            } catch (e: Exception) {
                Log.e(TAG, "Chunk $index failed: ${e.message}")
                errors.add(index to e)
                progressListener?.onChunkFailed(index, e)
            }
        }

        if (transcripts.isEmpty()) {
            throw ChunkedTranscriptionException("All chunks failed", errors)
        }

        Log.d(TAG, "Merging ${transcripts.size} transcripts")
        progressListener?.onMergingStarted()
        val merged = TranscriptMerger.merge(transcripts.sortedBy { it.index })
        progressListener?.onTranscriptionComplete(merged)

        Log.d(TAG, "Transcription complete: ${merged.length} characters")
        merged
    }

    private suspend fun transcribeWithRetry(chunk: AudioChunk): String {
        var lastException: Exception? = null
        var delay = ChunkConfig.RETRY_DELAY_MS

        repeat(ChunkConfig.MAX_RETRIES) { attempt ->
            try {
                Log.d(TAG, "Transcribing chunk ${chunk.index}, attempt ${attempt + 1}")
                return transcriber(chunk)
            } catch (e: Exception) {
                lastException = e
                Log.w(TAG, "Chunk ${chunk.index} attempt ${attempt + 1} failed: ${e.message}")
                if (attempt < ChunkConfig.MAX_RETRIES - 1) {
                    delay(delay)
                    delay *= 2
                }
            }
        }

        throw lastException ?: Exception("Transcription failed after ${ChunkConfig.MAX_RETRIES} attempts")
    }
}

class ChunkedTranscriptionException(
    message: String,
    val errors: List<Pair<Int, Exception>>
) : Exception(message)
