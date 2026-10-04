package dev.belalkhan.semanticsearch.search

import dev.belalkhan.semanticsearch.model.SearchDocument
import dev.belalkhan.semanticsearch.model.SearchResult
import android.os.SystemClock
import android.util.Log
import kotlin.math.sqrt

class SemanticSearchEngine(private val embeddings: EmbeddingGemmaEngine) : AutoCloseable {
    private var documentEmbeddings: List<Pair<SearchDocument, FloatArray>> = emptyList()

    fun prepare(documents: List<SearchDocument>) {
        val started = SystemClock.elapsedRealtime()
        Log.d(TAG, "Preparing ${documents.size} document embeddings")
        documentEmbeddings = documents.map { document ->
            val vector = embeddings.embedDocument(document)
            Log.d(TAG, "Prepared ${document.id}: dimensions=${vector.size}")
            document to vector
        }
        Log.d(TAG, "Preparation complete: ${SystemClock.elapsedRealtime() - started} ms")
    }

    fun search(query: String, topK: Int = 3, threshold: Double = -1.0): List<SearchResult> {
        Log.d(TAG, "Document embeddings reused=true; count=${documentEmbeddings.size}")

        val started = SystemClock.elapsedRealtime()
        val queryEmbedding = embeddings.embedQuery(query)
        Log.d(TAG, "Query embedding latency=${SystemClock.elapsedRealtime() - started} ms")

        val ranked = documentEmbeddings.map { (document, vector) ->
            SearchResult(document, cosineSimilarity(queryEmbedding, vector)).also {
                Log.d(TAG, "Candidate ${document.id}: cosine=${it.similarity}")
            }
        }.sortedByDescending { it.similarity }
        Log.d(TAG, "Ranking=${ranked.map { it.document.id }}")

        val top = ranked.take(topK)
        Log.d(TAG, "Top K=$topK: ${top.map { it.document.id }}")
        return top.filter { result ->
            val accepted = result.similarity >= threshold
            Log.d(TAG, "${result.document.id}: threshold=$threshold, rejected=${!accepted}")
            accepted
        }
    }

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Double {
        var dot = 0.0
        var normA = 0.0
        var normB = 0.0
        for (i in a.indices) {
            dot += a[i].toDouble() * b[i]
            normA += a[i].toDouble() * a[i]
            normB += b[i].toDouble() * b[i]
        }
        return dot / (sqrt(normA) * sqrt(normB))
    }

    override fun close() = embeddings.close()

    companion object { const val TAG = "SemanticSearch" }
}
