package dev.belalkhan.ragdocumentlab.ui.embedding

import dev.belalkhan.ragdocumentlab.data.StoredChunk
import dev.belalkhan.ragdocumentlab.data.StoredDocument

data class EmbeddingDetailsUiState(
    val document: StoredDocument,
    val chunks: List<StoredChunk> = emptyList(),
    val selectedChunkIndex: Int = 0,
    val loading: Boolean = true,
    val error: String? = null
) {
    val selectedChunk: StoredChunk?
        get() = chunks.getOrNull(selectedChunkIndex)
}
