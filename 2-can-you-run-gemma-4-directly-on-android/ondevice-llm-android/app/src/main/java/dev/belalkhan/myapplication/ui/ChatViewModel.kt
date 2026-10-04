package dev.belalkhan.myapplication.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.belalkhan.myapplication.ai.GemmaChat
import dev.belalkhan.myapplication.data.ModelDownloader
import dev.belalkhan.performanceoverlay.PerformanceMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val modelDownloader: ModelDownloader,
    private val gemmaChat: GemmaChat,
    val performanceMonitor: PerformanceMonitor,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        ChatUiState(isModelDownloaded = modelDownloader.isModelDownloaded())
    )
    val uiState = _uiState.asStateFlow()

    fun downloadModel() {
        if (_uiState.value.isBusy) return
        _uiState.update { it.copy(isBusy = true, downloadProgress = 0f, error = null) }

        viewModelScope.launch {
            runCatching { modelDownloader.download(::updateDownloadProgress) }
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isModelDownloaded = true,
                            downloadProgress = 1f,
                            isBusy = false,
                        )
                    }
                }
                .onFailure { showError(it) }
        }
    }

    fun sendMessage(prompt: String, imageUri: Uri?) {
        val text = prompt.trim()
        if ((text.isEmpty() && imageUri == null) || _uiState.value.isBusy) return

        val assistantId = System.nanoTime()
        _uiState.update {
            it.copy(
                messages = it.messages +
                    ChatMessage(assistantId - 1, text, isUser = true, imageUri = imageUri) +
                    ChatMessage(assistantId, "", isUser = false, isStreaming = true),
                isBusy = true,
                error = null,
            )
        }

        viewModelScope.launch(Dispatchers.Default) {
            runCatching {
                gemmaChat.sendMessage(text, imageUri) { token ->
                    updateAssistant(assistantId) { it.copy(text = it.text + token) }
                }
            }.onFailure { throwable ->
                _uiState.update { it.copy(error = throwable.message ?: "Could not generate a response.") }
                updateAssistant(assistantId) {
                    if (it.text.isBlank()) {
                        it.copy(text = "I couldn't generate a response. Please try again.")
                    } else {
                        it
                    }
                }
            }

            updateAssistant(assistantId) { it.copy(isStreaming = false) }
            _uiState.update { it.copy(isBusy = false) }
        }
    }

    fun clearChat() {
        if (_uiState.value.isBusy) return
        _uiState.update { it.copy(isBusy = true, error = null) }

        viewModelScope.launch(Dispatchers.Default) {
            gemmaChat.clearConversation()
            _uiState.update { it.copy(messages = emptyList(), isBusy = false) }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    override fun onCleared() {
        gemmaChat.release()
    }

    private fun updateDownloadProgress(progress: Float) {
        _uiState.update { it.copy(downloadProgress = progress) }
    }

    private fun showError(throwable: Throwable) {
        _uiState.update {
            it.copy(
                isBusy = false,
                error = throwable.message ?: "Something went wrong.",
            )
        }
    }

    private fun updateAssistant(id: Long, update: (ChatMessage) -> ChatMessage) {
        _uiState.update { state ->
            state.copy(
                messages = state.messages.map { if (it.id == id) update(it) else it }
            )
        }
    }
}
