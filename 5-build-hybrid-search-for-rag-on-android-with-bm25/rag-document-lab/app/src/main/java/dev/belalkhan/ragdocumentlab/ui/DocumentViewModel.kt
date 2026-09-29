package dev.belalkhan.ragdocumentlab.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.belalkhan.ragdocumentlab.data.DocumentRepository
import dev.belalkhan.ragdocumentlab.data.StoredDocument
import dev.belalkhan.ragdocumentlab.ui.embedding.EmbeddingDetailsUiState
import dev.belalkhan.ragdocumentlab.ui.search.SearchMode
import dev.belalkhan.ragdocumentlab.ui.search.SearchUiState
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class DocumentViewModel @Inject constructor(
    private val repository: DocumentRepository
) : ViewModel() {
    private val _state = MutableStateFlow(DocumentUiState())
    val state = _state.asStateFlow()
    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            repository.observeDocuments().collect { documents ->
                _state.update { it.copy(documents = documents) }
            }
        }
    }

    fun addDocument(uri: Uri) {
        if (state.value.processing) return
        _state.update { it.copy(processing = true, error = null) }

        viewModelScope.launch {
            try {
                repository.addDocument(uri) { progress ->
                    _state.update { it.copy(progress = progress) }
                }
                _state.update { it.copy(
                    processing = false,
                    progress = "Document indexed and stored locally"
                ) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(
                    processing = false,
                    progress = "Ready to try another PDF",
                    error = e.message ?: "Could not process this PDF"
                ) }
            }
        }
    }

    fun dismissError() {
        _state.update { it.copy(error = null) }
    }

    fun openSearch(document: StoredDocument) {
        if (state.value.processing) return
        searchJob?.cancel()
        _state.update { it.copy(search = SearchUiState(document)) }
    }

    fun updateSearchQuery(query: String) {
        _state.update { current ->
            val search = current.search ?: return@update current
            if (search.searching) current else current.copy(
                search = search.copy(query = query, results = null, searchedQuery = null, error = null)
            )
        }
    }

    fun selectSearchMode(mode: SearchMode) {
        _state.update { current ->
            val search = current.search ?: return@update current
            current.copy(search = search.copy(mode = mode))
        }
    }

    fun searchDocument() {
        val search = state.value.search ?: return
        if (search.searching || search.query.isBlank()) return
        val query = search.query.trim()
        val documentId = search.document.id
        _state.update { it.copy(search = search.copy(searching = true, error = null)) }
        searchJob = viewModelScope.launch {
            try {
                val results = repository.search(documentId, query)
                _state.update { current ->
                    val active = current.search
                    if (active?.document?.id != documentId || active.query.trim() != query) current
                    else current.copy(search = active.copy(
                        searching = false, searchedQuery = query, results = results
                    ))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { current ->
                    val active = current.search
                    if (active?.document?.id != documentId) current
                    else current.copy(search = active.copy(
                        searching = false, error = e.message ?: "Could not search this PDF"
                    ))
                }
            }
        }
    }

    fun closeSearch() {
        searchJob?.cancel()
        _state.update { it.copy(search = null) }
    }

    fun openDocument(document: StoredDocument) {
        _state.update {
            it.copy(embeddingDetails = EmbeddingDetailsUiState(document = document))
        }

        viewModelScope.launch {
            try {
                val chunks = repository.getChunks(document.id)
                _state.update { current ->
                    if (current.embeddingDetails?.document?.id != document.id) current
                    else current.copy(
                        embeddingDetails = current.embeddingDetails.copy(
                            chunks = chunks,
                            loading = false
                        )
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { current ->
                    if (current.embeddingDetails?.document?.id != document.id) current
                    else current.copy(
                        embeddingDetails = current.embeddingDetails.copy(
                            loading = false,
                            error = e.message ?: "Could not load saved embeddings"
                        )
                    )
                }
            }
        }
    }

    fun selectChunk(index: Int) {
        _state.update { current ->
            val details = current.embeddingDetails ?: return@update current
            if (index !in details.chunks.indices) current
            else current.copy(embeddingDetails = details.copy(selectedChunkIndex = index))
        }
    }

    fun closeDocument() {
        _state.update { it.copy(embeddingDetails = null) }
    }

    override fun onCleared() = repository.close()
}
