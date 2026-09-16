package dev.belalkhan.semanticsearch

import dev.belalkhan.semanticsearch.ui.SearchScreen
import dev.belalkhan.semanticsearch.ui.SearchViewModel
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import dev.belalkhan.semanticsearch.ui.SearchTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val model: SearchViewModel = viewModel()
            val state by model.state.collectAsStateWithLifecycle()
            SearchTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    SearchScreen(state, model::setQuery, model::literalSearch,
                        model::semanticSearch, model::setTopK, model::setThreshold)
                }
            }
        }
    }
}
