package dev.belalkhan.ragdocumentlab.ui.embedding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DataArray
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmbeddingDetailsScreen(
    state: EmbeddingDetailsUiState,
    onBack: () -> Unit,
    onSelectChunk: (Int) -> Unit
) {
    val listState = rememberLazyListState()
    val selectedChunk = state.selectedChunk
    LaunchedEffect(state.selectedChunkIndex) { listState.scrollToItem(0) }

    Box(
        modifier = Modifier.fillMaxSize().safeDrawingPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(Modifier.fillMaxHeight().widthIn(max = 720.dp)) {
            TopAppBar(
                title = {
                    Column {
                        Text("Saved embeddings")
                        Text(
                            text = state.document.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )

            LazyColumn(
                modifier = Modifier.fillMaxHeight(),
                state = listState,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 24.dp,
                    end = 24.dp,
                    bottom = 24.dp
                )
            ) {
                item {
                    Box(Modifier.padding(bottom = 20.dp)) { SummaryCard(state) }
                }

                when {
                    state.loading -> item { LoadingState() }
                    state.error != null -> item { ErrorState(state.error) }
                    selectedChunk == null -> item { EmptyState() }
                    else -> {
                        val chunk = selectedChunk
                        item {
                            Box(Modifier.padding(bottom = 20.dp)) {
                                ChunkNavigator(
                                    index = state.selectedChunkIndex,
                                    count = state.chunks.size,
                                    onSelectChunk = onSelectChunk
                                )
                            }
                        }
                        item {
                            Box(Modifier.padding(bottom = 20.dp)) {
                                ChunkTextCard(chunk.text)
                            }
                        }
                        item {
                            Column(Modifier.padding(bottom = 12.dp)) {
                                Text(
                                    "Embedding vector",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    "${chunk.embedding.size} stored float values",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        items(
                            count = chunk.embedding.size,
                            key = { it }
                        ) { dimension ->
                            EmbeddingValue(
                                dimension = dimension,
                                value = chunk.embedding[dimension]
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(state: EmbeddingDetailsUiState) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface) {
                Icon(
                    Icons.Rounded.DataArray,
                    contentDescription = null,
                    modifier = Modifier.padding(10.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            SummaryValue("${state.document.chunkCount}", "CHUNKS")
            SummaryValue("768", "DIMENSIONS")
            SummaryValue("LOCAL", "STORAGE")
        }
    }
}

@Composable
private fun SummaryValue(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium)
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ChunkNavigator(index: Int, count: Int, onSelectChunk: (Int) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onSelectChunk(index - 1) }, enabled = index > 0) {
                Icon(Icons.Rounded.ChevronLeft, "Previous chunk")
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Chunk ${index + 1} of $count", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Saved source text and vector",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { onSelectChunk(index + 1) }, enabled = index < count - 1) {
                Icon(Icons.Rounded.ChevronRight, "Next chunk")
            }
        }
    }
}

@Composable
private fun ChunkTextCard(text: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Text("Chunk text", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmbeddingValue(dimension: Int, value: Float) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "[$dimension]",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun LoadingState() {
    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorState(message: String) {
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.errorContainer) {
        Text(message, Modifier.fillMaxWidth().padding(18.dp))
    }
}

@Composable
private fun EmptyState() {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Text("No saved chunks found", Modifier.fillMaxWidth().padding(24.dp))
    }
}
