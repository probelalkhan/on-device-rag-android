package dev.belalkhan.ragdocumentlab.search

import dev.belalkhan.ragdocumentlab.data.StoredChunk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HybridSearchEngineTest {
    private val engine = HybridSearchEngine()
    private val chunks = listOf(
        chunk(0, "TSH reference range and thyroid result", floatArrayOf(1f, 0f)),
        chunk(1, "Ferritin 12 ng ml flagged low", floatArrayOf(0f, 1f))
    )

    @Test fun exactTermCanRescueAnEvidenceChunk() {
        val results = engine.search("ferritin", floatArrayOf(1f, 0f), chunks)

        assertEquals(0, results.semantic.first().chunkIndex)
        assertEquals(1, results.bm25.first().chunkIndex)
        assertEquals(1, results.hybrid.first().chunkIndex)
        assertEquals(2, results.hybrid.first().semanticRank)
        assertEquals(1, results.hybrid.first().bm25Rank)
    }

    @Test fun aParaphraseStillUsesSemanticRetrieval() {
        val results = engine.search("iron stores", floatArrayOf(0f, 1f), chunks)

        assertTrue(results.bm25.isEmpty())
        assertEquals(1, results.semantic.first().chunkIndex)
        assertEquals(1, results.hybrid.first().chunkIndex)
    }

    @Test fun exactWordMatchingIsCaseInsensitive() {
        val results = engine.search("FERRITIN", floatArrayOf(0f, 1f), chunks)
        assertEquals(1, results.bm25.first().chunkIndex)
    }

    private fun chunk(index: Int, text: String, embedding: FloatArray) = StoredChunk(
        documentId = 1,
        chunkIndex = index,
        text = text,
        embedding = embedding
    )
}
