package dev.belalkhan.ragdocumentlab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.belalkhan.ragdocumentlab.ui.DocumentScreen
import dev.belalkhan.ragdocumentlab.ui.DocumentTheme
import dev.belalkhan.ragdocumentlab.ui.DocumentViewModel
import dev.belalkhan.ragdocumentlab.ui.embedding.EmbeddingDetailsScreen
import dev.belalkhan.ragdocumentlab.ui.search.SearchScreen

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: DocumentViewModel = hiltViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            val pdfPicker = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocument()
            ) { uri -> uri?.let(viewModel::addDocument) }
            val embeddingDetails = state.embeddingDetails
            val search = state.search

            BackHandler(enabled = embeddingDetails != null || search != null) {
                if (search != null) viewModel.closeSearch() else viewModel.closeDocument()
            }

            DocumentTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    if (search != null) {
                        SearchScreen(
                            state = search,
                            onBack = viewModel::closeSearch,
                            onQueryChange = viewModel::updateSearchQuery,
                            onSearch = viewModel::searchDocument,
                            onModeChange = viewModel::selectSearchMode
                        )
                    } else if (embeddingDetails == null) {
                        DocumentScreen(
                            state = state,
                            onAddDocument = { pdfPicker.launch(arrayOf("application/pdf")) },
                            onDismissError = viewModel::dismissError,
                            onOpenDocument = viewModel::openDocument,
                            onSearchDocument = viewModel::openSearch
                        )
                    } else {
                        EmbeddingDetailsScreen(
                            state = embeddingDetails,
                            onBack = viewModel::closeDocument,
                            onSelectChunk = viewModel::selectChunk
                        )
                    }
                }
            }
        }
    }
}
