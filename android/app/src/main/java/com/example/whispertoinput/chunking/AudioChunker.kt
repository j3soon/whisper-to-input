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

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

class AudioChunker(private val context: Context) {
    private val TAG = "AudioChunker"

    suspend fun splitAudio(sourceFile: File): List<AudioChunk> = withContext(Dispatchers.IO) {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(sourceFile.absolutePath)

            val audioTrackIndex = (0 until extractor.trackCount)
                .firstOrNull {
                    extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
                }
                ?: throw IllegalArgumentException("No audio track found")

            extractor.selectTrack(audioTrackIndex)
            val format = extractor.getTrackFormat(audioTrackIndex)
            val durationUs = format.getLong(MediaFormat.KEY_DURATION)
            val durationMs = durationUs / 1000

            Log.d(TAG, "Audio duration: ${durationMs}ms")

            val thresholdMs = ChunkConfig.CHUNKING_THRESHOLD_SECONDS * 1000L
            if (durationMs <= thresholdMs) {
                Log.d(TAG, "Audio is short enough, returning single chunk")
                return@withContext listOf(
                    AudioChunk(sourceFile, 0, 0, durationMs)
                )
            }

            val chunkDurationMs = ChunkConfig.CHUNK_DURATION_SECONDS * 1000L
            val overlapMs = ChunkConfig.CHUNK_OVERLAP_SECONDS * 1000L
            val chunks = mutableListOf<AudioChunk>()

            var currentStartMs = 0L
            var chunkIndex = 0

            while (currentStartMs < durationMs) {
                val mainEndMs = minOf(currentStartMs + chunkDurationMs, durationMs)
                val fileEndMs = minOf(mainEndMs + overlapMs, durationMs)

                Log.d(TAG, "Creating chunk $chunkIndex: ${currentStartMs}ms - ${fileEndMs}ms")

                val chunkFile = exportChunk(
                    sourceFile = sourceFile,
                    startMs = currentStartMs,
                    endMs = fileEndMs,
                    index = chunkIndex
                )

                chunks.add(AudioChunk(
                    file = chunkFile,
                    index = chunkIndex,
                    startTimeMs = currentStartMs,
                    endTimeMs = mainEndMs
                ))

                currentStartMs = mainEndMs
                chunkIndex++
            }

            Log.d(TAG, "Created ${chunks.size} chunks")
            chunks
        } finally {
            extractor.release()
        }
    }

    private suspend fun exportChunk(
        sourceFile: File,
        startMs: Long,
        endMs: Long,
        index: Int
    ): File = withContext(Dispatchers.IO) {
        val outputFile = File(context.cacheDir, "chunk_${index}_${System.currentTimeMillis()}.m4a")

        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null

        try {
            extractor.setDataSource(sourceFile.absolutePath)

            val audioTrackIndex = (0 until extractor.trackCount)
                .first {
                    extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
                }

            extractor.selectTrack(audioTrackIndex)
            val format = extractor.getTrackFormat(audioTrackIndex)

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val muxerTrackIndex = muxer.addTrack(format)
            muxer.start()

            extractor.seekTo(startMs * 1000, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

            val buffer = ByteBuffer.allocate(1024 * 1024)
            val bufferInfo = MediaCodec.BufferInfo()

            while (true) {
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) break

                val sampleTimeUs = extractor.sampleTime
                if (sampleTimeUs > endMs * 1000) break

                bufferInfo.offset = 0
                bufferInfo.size = sampleSize
                bufferInfo.presentationTimeUs = sampleTimeUs - (startMs * 1000)
                bufferInfo.flags = extractor.sampleFlags

                muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)
                extractor.advance()
            }

            Log.d(TAG, "Exported chunk $index to ${outputFile.absolutePath}")
            outputFile
        } finally {
            muxer?.stop()
            muxer?.release()
            extractor.release()
        }
    }

    fun cleanup(chunks: List<AudioChunk>) {
        chunks.forEach { chunk ->
            if (chunk.file.exists() && chunk.file.name.startsWith("chunk_")) {
                chunk.file.delete()
                Log.d(TAG, "Deleted chunk file: ${chunk.file.name}")
            }
        }
    }
}
