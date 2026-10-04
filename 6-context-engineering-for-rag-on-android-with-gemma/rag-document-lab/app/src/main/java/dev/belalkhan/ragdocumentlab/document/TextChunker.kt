package dev.belalkhan.ragdocumentlab.document

import javax.inject.Inject

class TextChunker @Inject constructor() {
    fun chunk(
        text: String,
        maxCharacters: Int = 800,
        overlapCharacters: Int = 120
    ): List<DocumentChunk> {
        require(overlapCharacters < maxCharacters)
        if (text.isBlank()) return emptyList()

        val chunks = mutableListOf<DocumentChunk>()
        var start = 0

        while (start < text.length) {
            val proposedEnd = (start + maxCharacters).coerceAtMost(text.length)
            val end = findNaturalBoundary(text, start, proposedEnd)
            val chunkText = text.substring(start, end).trim()

            if (chunkText.isNotEmpty()) chunks += DocumentChunk(chunks.size, chunkText)
            if (end == text.length) break

            start = (end - overlapCharacters).coerceAtLeast(start + 1)
            start = moveToWordStart(text, start, end)
        }

        return chunks
    }

    private fun findNaturalBoundary(text: String, start: Int, proposedEnd: Int): Int {
        if (proposedEnd == text.length) return proposedEnd

        val earliestUsefulBoundary = start + (proposedEnd - start) / 2
        val boundaries = listOf("\n\n", ". ", "? ", "! ", "\n")
        return boundaries
            .map { boundary -> text.lastIndexOf(boundary, proposedEnd - 1) + boundary.length }
            .firstOrNull { it >= earliestUsefulBoundary }
            ?: proposedEnd
    }

    private fun moveToWordStart(text: String, start: Int, end: Int): Int {
        if (start == 0 || text[start - 1].isWhitespace()) return start
        val nextSpace = text.indexOf(' ', start)
        return if (nextSpace in start until end) nextSpace + 1 else start
    }
}

