package dev.belalkhan.ragdocumentlab.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.belalkhan.ragdocumentlab.data.StoredDocument
import java.text.DateFormat
import java.util.Date

@Composable
fun DocumentScreen(
    state: DocumentUiState,
    onAddDocument: () -> Unit,
    onDismissError: () -> Unit,
    onOpenDocument: (StoredDocument) -> Unit
) {
    Box(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.fillMaxHeight().widthIn(max = 720.dp),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item { Header() }
            item { HeroCard(state.processing, onAddDocument) }
            item {
                SectionTitle("How indexing works")
                Spacer(Modifier.height(12.dp))
                PipelineCard()
            }
            if (state.processing) item { ProgressCard(state.progress) }
            state.error?.let { message -> item { ErrorCard(message, onDismissError) } }
            item { DocumentHeader(state.documents.size) }
            if (state.documents.isEmpty()) {
                item { EmptyState() }
            } else {
                items(state.documents, key = { it.id }) {
                    DocumentCard(it, onOpenDocument)
                }
            }
            item {
                Text(
                    "800 character chunks  ·  120 character overlap  ·  768D vectors",
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun Header() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            modifier = Modifier.size(42.dp),
            shape = RoundedCornerShape(13.dp),
            color = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Rounded.Hub, null, Modifier.padding(10.dp), MaterialTheme.colorScheme.onPrimary)
        }
        Column(Modifier.padding(start = 12.dp).weight(1f)) {
            Text("RAG Document Lab", style = MaterialTheme.typography.titleMedium)
            Text("Local document indexing", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Lock, null, Modifier.size(14.dp))
                Text("PRIVATE", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun HeroCard(processing: Boolean, onAddDocument: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large)
            .background(Brush.linearGradient(listOf(Color(0xFF26358D), colors.primary)))
            .padding(24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.14f)) {
                Icon(Icons.Rounded.PictureAsPdf, null, Modifier.padding(10.dp).size(22.dp), Color.White)
            }
            Text("Turn PDFs into\nsearchable knowledge",
                style = MaterialTheme.typography.displaySmall, color = Color.White)
            Text("Extract, chunk, embed, and save, fully on device.",
                style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.78f))
            Button(
                onClick = onAddDocument,
                enabled = !processing,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = colors.primary,
                    disabledContainerColor = Color.White.copy(alpha = 0.55f)
                )
            ) {
                Icon(Icons.Rounded.Add, null)
                Text(if (processing) "Processing PDF…" else "Add a PDF",
                    modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun PipelineCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            PipelineStep(Icons.Rounded.Description, "Extract")
            PipelineStep(Icons.Rounded.ContentCut, "Chunk")
            PipelineStep(Icons.Rounded.Hub, "Embed")
            PipelineStep(Icons.Rounded.Storage, "Store")
        }
    }
}

@Composable
private fun PipelineStep(icon: ImageVector, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
            Icon(icon, null, Modifier.padding(10.dp).size(20.dp), MaterialTheme.colorScheme.primary)
        }
        Text(label, Modifier.padding(top = 8.dp), MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun ProgressCard(progress: String) {
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primaryContainer) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Building local index", style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f))
                Text("WORKING", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary)
            }
            LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical = 14.dp))
            Text(progress, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ErrorCard(message: String, onDismiss: () -> Unit) {
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.errorContainer) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Could not index PDF", style = MaterialTheme.typography.titleMedium)
            Text(message, style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(onClick = onDismiss) { Text("Dismiss") }
        }
    }
}

@Composable
private fun DocumentHeader(count: Int) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        SectionTitle("Indexed documents", Modifier.weight(1f))
        Text("$count SAVED", style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun EmptyState() {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainer) {
                Icon(Icons.Rounded.Description, null, Modifier.padding(14.dp).size(28.dp),
                    MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("Your library is empty", style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp))
            Text("Add a text-based PDF to create your first local index.",
                modifier = Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
private fun DocumentCard(document: StoredDocument, onClick: (StoredDocument) -> Unit) {
    Card(
        onClick = { onClick(document) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(Icons.Rounded.PictureAsPdf, null, Modifier.padding(12.dp).size(24.dp),
                    MaterialTheme.colorScheme.primary)
            }
            Column(Modifier.padding(horizontal = 14.dp).weight(1f)) {
                Text(document.name, style = MaterialTheme.typography.titleMedium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${document.chunkCount} chunks  ·  ${formatDate(document.indexedAt)}",
                    modifier = Modifier.padding(top = 4.dp), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    "Indexed",
                    tint = MaterialTheme.colorScheme.secondary
                )
                Icon(
                    Icons.Rounded.ChevronRight,
                    "View saved embeddings",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier, style = MaterialTheme.typography.titleLarge)
}

private fun formatDate(time: Long): String =
    DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(time))
