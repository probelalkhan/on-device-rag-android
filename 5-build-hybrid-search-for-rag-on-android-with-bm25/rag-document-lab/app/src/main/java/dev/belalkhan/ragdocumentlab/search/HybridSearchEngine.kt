package dev.belalkhan.ragdocumentlab.search

import dev.belalkhan.ragdocumentlab.data.StoredChunk
import java.util.Locale
import javax.inject.Inject
import kotlin.math.ln
import kotlin.math.sqrt

data class SearchHit(
    val chunkIndex: Int,
    val text: String,
    val semanticRank: Int?,
    val bm25Rank: Int?
)

data class SearchResults(
    val semantic: List<SearchHit>,
    val bm25: List<SearchHit>,
    val hybrid: List<SearchHit>
)

/** Ranks the saved chunks of one PDF. There is no server or external search index. */
class HybridSearchEngine @Inject constructor() {
    private val tokenPattern = Regex("[\\p{L}\\p{N}]+")

    fun search(query: String, queryEmbedding: FloatArray, chunks: List<StoredChunk>): SearchResults {
        if (chunks.isEmpty()) return SearchResults(emptyList(), emptyList(), emptyList())
        require(queryEmbedding.isNotEmpty()) { "Query embedding is empty" }

        val terms = tokenize(query).distinct()
        val tokenizedChunks = chunks.map { tokenize(it.text) }
        val documentFrequency = terms.associateWith { term ->
            tokenizedChunks.count { term in it }
        }
        val averageLength = tokenizedChunks.map { it.size }.average().coerceAtLeast(1.0)

        val semantic = chunks.indices.sortedWith(
            compareByDescending<Int> { cosine(queryEmbedding, chunks[it].embedding) }
                .thenBy { chunks[it].chunkIndex }
        )
        val bm25 = chunks.indices.map { index ->
            index to bm25Score(terms, tokenizedChunks[index], documentFrequency,
                chunks.size, averageLength)
        }.filter { it.second > 0.0 }
            .sortedWith(compareByDescending<Pair<Int, Double>> { it.second }
                .thenBy { chunks[it.first].chunkIndex })
            .map { it.first }

        val semanticRanks = semantic.withIndex().associate { (rank, index) -> index to rank + 1 }
        val bm25Ranks = bm25.withIndex().associate { (rank, index) -> index to rank + 1 }

        fun hit(index: Int) = SearchHit(
            chunkIndex = chunks[index].chunkIndex,
            text = chunks[index].text,
            semanticRank = semanticRanks[index],
            bm25Rank = bm25Ranks[index]
        )

        // BM25 and cosine scores have different scales. Fuse their positions, not their scores.
        val hybrid = chunks.indices.sortedWith(
            compareByDescending<Int> { index ->
                listOfNotNull(semanticRanks[index], bm25Ranks[index])
                    .sumOf { rank -> 1.0 / (60 + rank) }
            }.thenBy { chunks[it].chunkIndex }
        )

        return SearchResults(semantic.map(::hit), bm25.map(::hit), hybrid.map(::hit))
    }

    private fun tokenize(text: String): List<String> = tokenPattern.findAll(text.lowercase(Locale.ROOT))
        .map { it.value }.toList()

    private fun bm25Score(
        queryTerms: List<String>,
        tokens: List<String>,
        documentFrequency: Map<String, Int>,
        corpusSize: Int,
        averageLength: Double
    ): Double {
        val counts = tokens.groupingBy { it }.eachCount()
        val k1 = 1.2
        val b = 0.75
        return queryTerms.sumOf { term ->
            val frequency = counts[term] ?: return@sumOf 0.0
            val df = documentFrequency.getValue(term)
            val idf = ln(1.0 + (corpusSize - df + 0.5) / (df + 0.5))
            val lengthCorrection = 1.0 - b + b * tokens.size / averageLength
            idf * frequency * (k1 + 1.0) / (frequency + k1 * lengthCorrection)
        }
    }

    private fun cosine(first: FloatArray, second: FloatArray): Double {
        if (first.size != second.size) return 0.0
        var dot = 0.0
        var firstNorm = 0.0
        var secondNorm = 0.0
        for (i in first.indices) {
            dot += first[i] * second[i]
            firstNorm += first[i] * first[i]
            secondNorm += second[i] * second[i]
        }
        return if (firstNorm == 0.0 || secondNorm == 0.0) 0.0
        else dot / sqrt(firstNorm * secondNorm)
    }
}
