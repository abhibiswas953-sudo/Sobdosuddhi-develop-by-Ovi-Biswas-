package com.example.core

import java.security.MessageDigest

data class TextMetrics(
    val wordCount: Int,
    val charCount: Int,
    val charCountNoSpaces: Int,
    val paragraphCount: Int,
    val lineCount: Int,
    val sentenceCount: Int,
    val uniqueWordCount: Int,
    val lexicalDiversityPercent: Int
)

data class ManuscriptChunk(
    val index: Int,
    val totalChunks: Int,
    val previousContext: String,
    val content: String,
    val nextContext: String
)

object BengaliTextUtils {
    private val BENGALI_DIGITS = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')

    fun toBengaliDigits(number: Number): String {
        val raw = number.toString()
        val sb = StringBuilder(raw.length)
        for (ch in raw) {
            if (ch in '0'..'9') {
                sb.append(BENGALI_DIGITS[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun computeMetrics(text: String): TextMetrics {
        if (text.isBlank()) {
            return TextMetrics(0, 0, 0, 0, 0, 0, 0, 0)
        }
        val words = text.trim()
            .split(Regex("[\\s\\u200C\\u200D,।?!;:\"'()\\[\\]{}—–-]+"))
            .filter { it.isNotBlank() }
        val uniqueWords = words.map { it.lowercase() }.toSet()
        val paragraphs = text.split(Regex("\\n\\s*\\n")).count { it.isNotBlank() }
            .coerceAtLeast(if (text.isNotBlank()) 1 else 0)
        val lines = text.lines().count { it.isNotBlank() }
        val sentences = text.split(Regex("[।?!\\n]+")).count { it.trim().isNotEmpty() }
            .coerceAtLeast(if (words.isNotEmpty()) 1 else 0)
        val diversity = if (words.isNotEmpty()) {
            ((uniqueWords.size.toDouble() / words.size.toDouble()) * 100.0).toInt().coerceIn(0, 100)
        } else {
            0
        }

        return TextMetrics(
            wordCount = words.size,
            charCount = text.length,
            charCountNoSpaces = text.count { !it.isWhitespace() },
            paragraphCount = paragraphs,
            lineCount = lines,
            sentenceCount = sentences,
            uniqueWordCount = uniqueWords.size,
            lexicalDiversityPercent = diversity
        )
    }

    fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Splits long manuscripts by paragraph boundaries into manageable chunks while
     * retaining brief leading/trailing context so narrative & character continuity stays intact.
     */
    fun chunkManuscript(text: String, maxCharsPerChunk: Int = 2200): List<ManuscriptChunk> {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return emptyList()
        if (trimmed.length <= maxCharsPerChunk) {
            return listOf(ManuscriptChunk(0, 1, "", trimmed, ""))
        }

        val paragraphs = trimmed.split(Regex("\\n\\s*\\n")).filter { it.isNotBlank() }
        val rawChunks = mutableListOf<String>()
        val current = StringBuilder()

        for (para in paragraphs) {
            if (current.isNotEmpty() && current.length + para.length + 2 > maxCharsPerChunk) {
                rawChunks.add(current.toString().trim())
                current.clear()
            }
            if (para.length > maxCharsPerChunk) {
                // Split oversized single paragraph by sentences
                val sentences = para.split(Regex("(?<=[।?!])\\s+"))
                for (sentence in sentences) {
                    if (current.isNotEmpty() && current.length + sentence.length + 1 > maxCharsPerChunk) {
                        rawChunks.add(current.toString().trim())
                        current.clear()
                    }
                    if (current.isNotEmpty()) current.append(' ')
                    current.append(sentence)
                }
            } else {
                if (current.isNotEmpty()) current.append("\n\n")
                current.append(para)
            }
        }
        if (current.isNotBlank()) {
            rawChunks.add(current.toString().trim())
        }

        return rawChunks.mapIndexed { idx, chunkText ->
            val prev = if (idx > 0) rawChunks[idx - 1].takeLast(240) else ""
            val next = if (idx < rawChunks.lastIndex) rawChunks[idx + 1].take(240) else ""
            ManuscriptChunk(
                index = idx,
                totalChunks = rawChunks.size,
                previousContext = prev,
                content = chunkText,
                nextContext = next
            )
        }
    }

    fun buildLineDiff(original: String, corrected: String): List<DiffLine> {
        val origLines = original.lines()
        val corrLines = corrected.lines()
        val maxLen = maxOf(origLines.size, corrLines.size)
        val result = mutableListOf<DiffLine>()
        for (i in 0 until maxLen) {
            val o = origLines.getOrNull(i) ?: ""
            val c = corrLines.getOrNull(i) ?: ""
            val changed = o != c
            result.add(DiffLine(lineNumber = i + 1, original = o, corrected = c, changed = changed))
        }
        return result
    }
}

data class DiffLine(
    val lineNumber: Int,
    val original: String,
    val corrected: String,
    val changed: Boolean
)
