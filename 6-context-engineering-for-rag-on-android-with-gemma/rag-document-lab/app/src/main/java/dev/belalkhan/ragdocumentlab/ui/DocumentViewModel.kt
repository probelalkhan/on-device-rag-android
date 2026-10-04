package dev.belalkhan.ragdocumentlab.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.belalkhan.ragdocumentlab.data.DocumentRepository
import dev.belalkhan.ragdocumentlab.data.StoredDocument
import dev.belalkhan.ragdocumentlab.answer.ContextBuilder
import dev.belalkhan.ragdocumentlab.ui.embedding.EmbeddingDetailsUiState
import dev.belalkhan.ragdocumentlab.ui.context.ContextLabUiState
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
    private var prepareJob: Job? = null
    private var requestSerial = 0L
    private val contextBuilder = ContextBuilder()

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

    fun openContextLab(document: StoredDocument) {
        if (state.value.processing) return
        prepareJob?.cancel()
        _state.update { it.copy(contextLab = ContextLabUiState(document)) }
    }

    fun updateQuestion(question: String) {
        _state.update { current ->
            val lab = current.contextLab ?: return@update current
            if (lab.preparing) current
            else current.copy(contextLab = lab.withQuestion(question))
        }
    }

    fun prepareContext() {
        val lab = state.value.contextLab ?: return
        if (lab.preparing || lab.question.isBlank()) return
        val question = lab.question.trim()
        val documentId = lab.document.id
        val serial = ++requestSerial
        _state.update { it.copy(contextLab = lab.copy(preparing = true, error = null,
            retrievalCandidates = emptyList(), prepared = null)) }
        prepareJob = viewModelScope.launch {
            try {
                // Retrieval stays scoped to this PDF. The full Hybrid list, not a UI
                // preview, becomes the context builder's candidate list.
                val results = repository.search(documentId, question)
                val prepared = contextBuilder.prepare(lab.document.name, question, results.hybrid)
                _state.update { current ->
                    val active = current.contextLab
                    if (serial != requestSerial || active?.document?.id != documentId ||
                        active.question.trim() != question) current
                    else current.copy(contextLab = active.copy(preparing = false,
                        retrievalCandidates = results.hybrid, prepared = prepared))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { current ->
                    val active = current.contextLab
                    if (serial != requestSerial || active?.document?.id != documentId) current
                    else current.copy(contextLab = active.copy(
                        preparing = false, error = e.message ?: "Could not prepare context"
                    ))
                }
            }
        }
    }

    fun closeContextLab() {
        requestSerial++
        prepareJob?.cancel()
        _state.update { it.copy(contextLab = null) }
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

    override fun onCleared() {
        repository.close()
    }
}
