package dev.belalkhan.ragdocumentlab.ui

import dev.belalkhan.ragdocumentlab.data.StoredDocument

data class DocumentUiState(
    val documents: List<StoredDocument> = emptyList(),
    val processing: Boolean = false,
    val progress: String = "Ready to index a PDF",
    val error: String? = null
)

