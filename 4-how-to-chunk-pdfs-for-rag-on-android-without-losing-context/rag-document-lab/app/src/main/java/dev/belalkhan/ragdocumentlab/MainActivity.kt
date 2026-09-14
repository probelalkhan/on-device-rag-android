package dev.belalkhan.ragdocumentlab

import android.os.Bundle
import androidx.activity.ComponentActivity
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

            DocumentTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    DocumentScreen(
                        state = state,
                        onAddDocument = { pdfPicker.launch(arrayOf("application/pdf")) },
                        onDismissError = viewModel::dismissError
                    )
                }
            }
        }
    }
}
