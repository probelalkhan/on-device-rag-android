package dev.belalkhan.ragdocumentlab.answer

import dev.belalkhan.ragdocumentlab.data.StoredDocument
import dev.belalkhan.ragdocumentlab.search.SearchHit
import dev.belalkhan.ragdocumentlab.ui.context.ContextLabUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContextBuilderTest {
    private fun hit(index: Int, text: String) = SearchHit(index, text, 1, 1)
    private fun budget(total: Int) = RequestBudget(total, 100, 100, 20)

    @Test fun keepsCompleteBlocksInHybridOrderAndPreservesSourceText() {
        val hits = listOf(hit(9, "Uric Acid\n7.7 H* mg/dL 3.5 - 7.2 mg/dL"),
            hit(2, "Patient NAME: Belal\nCollected Jul 08, 2026"))
        val request = ContextBuilder(budget(2000)).prepare("July Report.pdf", "What is uric acid?", hits)
        assertEquals(listOf(10, 3), request.selected.map { it.chunkNumber })
        assertEquals(listOf("S1", "S2"), request.selected.map { it.id })
        assertEquals(hits[0].text, request.selected[0].text)
        assertTrue(request.userMessage.contains("[S1] Document: July Report.pdf | Chunk: 10\n" + hits[0].text))
        assertTrue(request.userMessage.contains("[S2] Document: July Report.pdf | Chunk: 3\n" + hits[1].text))
        assertEquals(request.questionSection + request.evidenceHeading +
            request.selected.joinToString("") { it.block }, request.userMessage)
        assertEquals("System instructions:\n${request.instructions}\n\nUser message:\n${request.userMessage}",
            request.exportText)
    }

    @Test fun duplicateChunkIdentifierIsOmittedWithoutRemovingDifferentOverlappingChunks() {
        val request = ContextBuilder(budget(2000)).prepare("report", "question",
            listOf(hit(1, "same text"), hit(1, "same text"), hit(2, "same text")))
        assertEquals(listOf(2, 3), request.selected.map { it.chunkNumber })
        assertEquals(1, request.omitted.size)
        assertTrue(request.omitted.single().reason.contains("Duplicate"))
    }

    @Test fun exactBoundaryFitsAndOneTokenLessOmitsWholeBlock() {
        val hits = listOf(hit(0, "A complete passage."))
        val roomy = ContextBuilder(budget(2000)).prepare("report", "question", hits)
        val exactTotal = roomy.estimatedInputTokens + 200
        assertEquals(1, ContextBuilder(budget(exactTotal)).prepare("report", "question", hits).selected.size)
        val short = ContextBuilder(budget(exactTotal - 1)).prepare("report", "question", hits)
        assertTrue(short.selected.isEmpty())
        assertFalse(short.userMessage.contains("A complete passage."))
    }

    @Test fun oversizedStrongestSourceDoesNotSubstituteWeakerEvidence() {
        val hits = listOf(hit(0, "Long".repeat(500)), hit(1, "short"))
        val request = ContextBuilder(budget(500)).prepare("report", "question", hits)
        assertTrue(request.selected.isEmpty())
        assertEquals(2, request.omitted.size)
        assertTrue(request.omitted[1].reason.contains("Strongest"))
    }

    @Test fun emptyCandidatesProduceAnEvidenceFreePrompt() {
        val request = ContextBuilder().prepare("report", "question", emptyList())
        assertTrue(request.exportText.contains("Question:\nquestion"))
        assertTrue(request.selected.isEmpty())
        assertTrue(request.omitted.isEmpty())
    }

    @Test fun changingQuestionClearsPreparedContext() {
        val prepared = ContextBuilder().prepare("report", "first", listOf(hit(0, "value")))
        val state = ContextLabUiState(StoredDocument(id = 1, name = "report", chunkCount = 1),
            question = "first", prepared = prepared,
            retrievalCandidates = listOf(hit(0, "value")))
        val changed = state.withQuestion("second")
        assertEquals(null, changed.prepared)
        assertTrue(changed.retrievalCandidates.isEmpty())
    }
}
