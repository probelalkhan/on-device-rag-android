package dev.belalkhan.semanticsearch.ui

import dev.belalkhan.semanticsearch.model.SearchDocument
import dev.belalkhan.semanticsearch.model.SearchResult

data class SearchUiState(
    val query: String = "dessert",
    val topK: Int = 1,
    val threshold: Float = -1f,
    val preparing: Boolean = true,
    val searching: Boolean = false,
    val searchType: SearchType? = null,
    val results: List<SearchResult> = emptyList(),
    val literalResults: List<SearchDocument> = emptyList(),
    val error: String? = null
)

enum class SearchType { LITERAL, SEMANTIC }
