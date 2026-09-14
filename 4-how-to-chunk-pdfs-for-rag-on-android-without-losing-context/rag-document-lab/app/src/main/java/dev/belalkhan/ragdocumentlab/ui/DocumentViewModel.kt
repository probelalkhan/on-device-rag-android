package dev.belalkhan.ragdocumentlab.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.belalkhan.ragdocumentlab.data.DocumentRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
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

    override fun onCleared() = repository.close()
}
