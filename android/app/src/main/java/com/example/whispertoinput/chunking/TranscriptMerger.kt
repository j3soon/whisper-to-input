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

object TranscriptMerger {
    private const val MIN_OVERLAP_WORDS = 3
    private const val MAX_OVERLAP_WORDS = 15

    fun merge(transcripts: List<ChunkTranscript>): String {
        if (transcripts.isEmpty()) return ""
        if (transcripts.size == 1) return transcripts[0].text.trim()

        val sorted = transcripts.sortedBy { it.index }
        var result = sorted[0].text.trim()

        for (i in 1 until sorted.size) {
            val currentText = sorted[i].text.trim()
            if (currentText.isEmpty()) continue

            val deduplicated = removeOverlap(result, currentText)
            result = if (deduplicated != null && deduplicated.isNotEmpty()) {
                "$result $deduplicated"
            } else if (deduplicated == null) {
                "$result $currentText"
            } else {
                result
            }
        }

        return normalizeWhitespace(result)
    }

    private fun removeOverlap(previous: String, current: String): String? {
        val previousWords = previous.split(Regex("\\s+"))
        val currentWords = current.split(Regex("\\s+"))

        if (previousWords.size < MIN_OVERLAP_WORDS || currentWords.size < MIN_OVERLAP_WORDS) {
            return null
        }

        val checkWords = minOf(MAX_OVERLAP_WORDS, previousWords.size)
        val previousSuffix = previousWords.takeLast(checkWords)

        for (overlapLength in minOf(checkWords, currentWords.size) downTo MIN_OVERLAP_WORDS) {
            val previousEnd = previousSuffix.takeLast(overlapLength)
            val currentStart = currentWords.take(overlapLength)

            if (wordsMatch(previousEnd, currentStart)) {
                val remaining = currentWords.drop(overlapLength)
                return remaining.joinToString(" ")
            }
        }

        return null
    }

    private fun wordsMatch(words1: List<String>, words2: List<String>): Boolean {
        if (words1.size != words2.size) return false
        return words1.zip(words2).all { (w1, w2) ->
            cleanWord(w1).equals(cleanWord(w2), ignoreCase = true)
        }
    }

    private fun cleanWord(word: String): String {
        return word.replace(Regex("[^\\p{L}\\p{N}]"), "")
    }

    private fun normalizeWhitespace(text: String): String {
        return text.replace(Regex("\\s+"), " ").trim()
    }
}
