package dev.belalkhan.myapplication.ui

import android.net.Uri

data class ChatMessage(
    val id: Long,
    val text: String,
    val isUser: Boolean,
    val imageUri: Uri? = null,
    val isStreaming: Boolean = false,
)

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isModelDownloaded: Boolean = false,
    val downloadProgress: Float = 0f,
    val isBusy: Boolean = false,
    val error: String? = null,
)
