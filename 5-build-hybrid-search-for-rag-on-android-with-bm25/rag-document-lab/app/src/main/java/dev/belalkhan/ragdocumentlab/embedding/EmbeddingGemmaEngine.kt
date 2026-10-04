package dev.belalkhan.ragdocumentlab.embedding

import dev.belalkhan.ragdocumentlab.document.DocumentChunk
import android.content.Context
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder.EmbeddingType
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder.TextFormatContext
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder.TextRole

class EmbeddingGemmaEngine(context: Context) : AutoCloseable {
    private val embedder = TextEmbedder.createFromOptions(
        context,
        TextEmbedder.TextEmbedderOptions.builder()
            .setBaseOptions(BaseOptions.builder()
                .setModelAssetPath("embedding_gemma.task").build())
            .setQuantize(false)
            .build()
    )

    fun embedQuery(query: String): FloatArray = embed(
        query,
        TextFormatContext.builder()
            .setTaskType(EmbeddingType.RETRIEVAL_QUERY)
            .setRole(TextRole.QUERY)
            .build()
    )

    fun embedDocument(document: DocumentChunk): FloatArray = embed(
        document.text,
        TextFormatContext.builder()
            .setTaskType(EmbeddingType.RETRIEVAL_DOCUMENT)
            .setRole(TextRole.DOCUMENT)
            .setTitle("none")
            .build()
    )

    private fun embed(text: String, format: TextFormatContext): FloatArray =
        embedder.embed(text, format).embeddingResult().embeddings().first()
            .floatEmbedding().copyOf()

    override fun close() = embedder.close()
}

