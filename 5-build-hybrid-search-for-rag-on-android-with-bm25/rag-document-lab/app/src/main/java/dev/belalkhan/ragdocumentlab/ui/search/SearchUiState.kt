package dev.belalkhan.ragdocumentlab.ui.search

import dev.belalkhan.ragdocumentlab.data.StoredDocument
import dev.belalkhan.ragdocumentlab.search.SearchHit
import dev.belalkhan.ragdocumentlab.search.SearchResults

enum class SearchMode { SEMANTIC, BM25, HYBRID }

data class SearchUiState(
    val document: StoredDocument,
    val query: String = "",
    val searchedQuery: String? = null,
    val searching: Boolean = false,
    val mode: SearchMode = SearchMode.HYBRID,
    val results: SearchResults? = null,
    val error: String? = null
) {
    val visibleHits: List<SearchHit>
        get() = when (mode) {
            SearchMode.SEMANTIC -> results?.semantic
            SearchMode.BM25 -> results?.bm25
            SearchMode.HYBRID -> results?.hybrid
        }?.take(5).orEmpty()
}
