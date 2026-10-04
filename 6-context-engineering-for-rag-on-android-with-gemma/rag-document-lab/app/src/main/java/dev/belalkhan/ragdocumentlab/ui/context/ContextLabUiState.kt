package dev.belalkhan.ragdocumentlab.ui.context

import dev.belalkhan.ragdocumentlab.answer.PreparedRequest
import dev.belalkhan.ragdocumentlab.data.StoredDocument
import dev.belalkhan.ragdocumentlab.search.SearchHit

data class ContextLabUiState(
    val document: StoredDocument,
    val question: String = "",
    val preparing: Boolean = false,
    val retrievalCandidates: List<SearchHit> = emptyList(),
    val prepared: PreparedRequest? = null,
    val error: String? = null
) {
    fun withQuestion(value: String): ContextLabUiState = copy(
        question = value, retrievalCandidates = emptyList(), prepared = null, error = null
    )
}
