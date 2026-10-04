package dev.belalkhan.semanticsearch.ui

import dev.belalkhan.semanticsearch.model.searchDocuments
import dev.belalkhan.semanticsearch.search.EmbeddingGemmaEngine
import dev.belalkhan.semanticsearch.search.SemanticSearchEngine
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.util.concurrent.Executors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SearchViewModel(application: Application) : AndroidViewModel(application) {
    private val inferenceExecutor = Executors.newSingleThreadExecutor()
    private val inferenceDispatcher = inferenceExecutor.asCoroutineDispatcher()
    private var searchEngine: SemanticSearchEngine? = null
    private val _state = MutableStateFlow(SearchUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch(inferenceDispatcher) {
            try {
                val engine = SemanticSearchEngine(EmbeddingGemmaEngine(application))
                searchEngine = engine
                engine.prepare(searchDocuments)
                _state.update { it.copy(preparing = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(SemanticSearchEngine.TAG, "Preparation failed", e)
                _state.update { it.copy(preparing = false,
                    error = "Model initialization failed. Check embedding_gemma.task in assets and Logcat, then restart the app.") }
            }
        }
    }

    fun setQuery(query: String) {
        _state.update { it.copy(query = query, searchType = null) }
    }

    fun setTopK(topK: Int) {
        _state.update { it.copy(topK = topK, searchType = null) }
    }

    fun setThreshold(threshold: Float) {
        _state.update { it.copy(threshold = threshold, searchType = null) }
    }

    fun literalSearch(query: String) {
        if (query.isBlank()) return
        _state.update { it.copy(
            query = query,
            searchType = SearchType.LITERAL,
            literalResults = searchDocuments.filter {
                it.text.contains(query.trim(), ignoreCase = true)
            }
        ) }
    }

    fun semanticSearch(query: String) {
        val input = state.value.copy(query = query)
        if (input.preparing || input.searching || input.error != null || query.isBlank()) return
        _state.value = input.copy(searching = true, searchType = null)
        viewModelScope.launch(inferenceDispatcher) {
            try {
                val results = checkNotNull(searchEngine).search(
                    query = input.query.trim(),
                    topK = input.topK,
                    threshold = input.threshold.toDouble()
                )
                _state.update { it.copy(
                    searching = false,
                    searchType = SearchType.SEMANTIC,
                    results = results
                ) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(SemanticSearchEngine.TAG, "Search failed", e)
                _state.update { it.copy(searching = false,
                    error = "Search failed. Check Logcat and restart the app.") }
            }
        }
    }

    override fun onCleared() {
        inferenceExecutor.execute { searchEngine?.close() }
        inferenceDispatcher.close()
    }
}
