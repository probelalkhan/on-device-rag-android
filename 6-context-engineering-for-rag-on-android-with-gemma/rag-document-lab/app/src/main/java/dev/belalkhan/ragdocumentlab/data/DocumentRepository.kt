package dev.belalkhan.ragdocumentlab.data

import android.net.Uri
import androidx.room.withTransaction
import dev.belalkhan.ragdocumentlab.document.PdfTextExtractor
import dev.belalkhan.ragdocumentlab.document.TextChunker
import dev.belalkhan.ragdocumentlab.embedding.EmbeddingGemmaEngine
import dev.belalkhan.ragdocumentlab.search.HybridSearchEngine
import dev.belalkhan.ragdocumentlab.search.SearchResults
import javax.inject.Inject
import javax.inject.Provider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DocumentRepository @Inject constructor(
    private val database: DocumentDatabase,
    private val extractor: PdfTextExtractor,
    private val chunker: TextChunker,
    private val embeddingProvider: Provider<EmbeddingGemmaEngine>,
    private val searchEngine: HybridSearchEngine
) : AutoCloseable {
    private var embeddingEngine: EmbeddingGemmaEngine? = null
    private val embeddingMutex = Mutex()

    fun observeDocuments(): Flow<List<StoredDocument>> =
        database.documentDao().observeDocuments()

    suspend fun getChunks(documentId: Long): List<StoredChunk> =
        withContext(Dispatchers.IO) {
            database.documentDao().getChunks(documentId)
        }

    suspend fun search(documentId: Long, query: String): SearchResults {
        val cleanQuery = query.trim()
        require(cleanQuery.isNotEmpty()) { "Enter a question to search" }
        val chunks = getChunks(documentId)
        require(chunks.isNotEmpty()) { "This document has no saved chunks" }
        val queryEmbedding = withContext(Dispatchers.IO) {
            embeddingMutex.withLock {
                val engine = embeddingEngine ?: embeddingProvider.get().also { embeddingEngine = it }
                engine.embedQuery(cleanQuery)
            }
        }
        return withContext(Dispatchers.Default) {
            searchEngine.search(cleanQuery, queryEmbedding, chunks)
        }
    }

    suspend fun addDocument(uri: Uri, onProgress: (String) -> Unit) =
        withContext(Dispatchers.IO.limitedParallelism(1)) {
            onProgress("Extracting text from PDF")
            val pdf = extractor.extract(uri)
            require(pdf.text.isNotBlank()) {
                "No selectable text was found. Scanned PDFs need OCR."
            }

            onProgress("Splitting text into overlapping chunks")
            val chunks = chunker.chunk(pdf.text)

            val storedChunks = embeddingMutex.withLock {
                val embeddings = embeddingEngine ?: run {
                    onProgress("Loading EmbeddingGemma")
                    embeddingProvider.get().also { embeddingEngine = it }
                }
                chunks.mapIndexed { index, chunk ->
                    onProgress("Creating embedding ${index + 1} of ${chunks.size}")
                    chunk to embeddings.embedDocument(chunk)
                }
            }

            onProgress("Saving chunks and embeddings on device")
            database.withTransaction {
                val dao = database.documentDao()
                dao.findByName(pdf.name)?.let { dao.deleteDocument(it) }
                val documentId = dao.insertDocument(
                    StoredDocument(name = pdf.name, chunkCount = chunks.size)
                )
                dao.insertChunks(storedChunks.map { (chunk, embedding) ->
                    StoredChunk(
                        documentId = documentId,
                        chunkIndex = chunk.index,
                        text = chunk.text,
                        embedding = embedding
                    )
                })
            }
        }

    override fun close() {
        embeddingEngine?.close()
    }
}
