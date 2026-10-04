package dev.belalkhan.ragdocumentlab

import android.content.Intent
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
import dev.belalkhan.ragdocumentlab.ui.context.ContextLabScreen

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
            val contextLab = state.contextLab

            BackHandler(enabled = embeddingDetails != null || contextLab != null) {
                if (contextLab != null) viewModel.closeContextLab() else viewModel.closeDocument()
            }

            DocumentTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    if (contextLab != null) {
                        ContextLabScreen(
                            state = contextLab,
                            onBack = viewModel::closeContextLab,
                            onQuestionChange = viewModel::updateQuestion,
                            onPrepareContext = viewModel::prepareContext,
                            onExportPrompt = { prompt ->
                                val share = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, prompt)
                                }
                                startActivity(Intent.createChooser(share, "Export prepared prompt"))
                            }
                        )
                    } else if (embeddingDetails == null) {
                        DocumentScreen(
                            state = state,
                            onAddDocument = { pdfPicker.launch(arrayOf("application/pdf")) },
                            onDismissError = viewModel::dismissError,
                            onOpenDocument = viewModel::openDocument,
                            onOpenContextLab = viewModel::openContextLab
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
