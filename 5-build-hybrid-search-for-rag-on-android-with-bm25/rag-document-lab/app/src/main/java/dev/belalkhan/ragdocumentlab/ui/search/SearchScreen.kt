package dev.belalkhan.ragdocumentlab.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import dev.belalkhan.ragdocumentlab.search.SearchHit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    state: SearchUiState,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onModeChange: (SearchMode) -> Unit
) {
    Box(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.fillMaxHeight().widthIn(max = 720.dp)) {
            TopAppBar(
                title = {
                    Column {
                        Text("Search saved PDF")
                        Text(state.document.name, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Compare evidence rankings", style = MaterialTheme.typography.titleLarge)
                        Text("One question. The same saved chunks. Three ways to rank them.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedTextField(
                            value = state.query,
                            onValueChange = onQueryChange,
                            label = { Text("Question or exact medical term") },
                            placeholder = { Text("Search this PDF") },
                            singleLine = true,
                            enabled = !state.searching,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = onSearch,
                            enabled = state.query.isNotBlank() && !state.searching,
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) {
                            Text(if (state.searching) "Searching on device…" else "Compare search results")
                        }
                    }
                }
                if (state.searching) item {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator()
                        Text("Embedding the question and ranking saved chunks")
                    }
                }
                state.error?.let { message -> item {
                    Card(colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )) { Text(message, Modifier.fillMaxWidth().padding(16.dp)) }
                } }
                if (state.results != null) {
                    item {
                        Column {
                            Text("Results for ${state.searchedQuery}",
                                style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SearchMode.entries.forEach { mode ->
                                    FilterChip(
                                        selected = state.mode == mode,
                                        onClick = { onModeChange(mode) },
                                        label = { Text(mode.label) }
                                    )
                                }
                            }
                            Text(state.mode.explanation,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (state.visibleHits.isEmpty()) item {
                        Text("No exact word match in this PDF. Try Semantic or Hybrid.",
                            style = MaterialTheme.typography.bodyMedium)
                    }
                    itemsIndexed(state.visibleHits, key = { _, hit -> hit.chunkIndex }) { rank, hit ->
                        ResultCard(rank + 1, hit)
                    }
                    item {
                        Text("These are source passages, not a generated medical answer. " +
                            "Read the passage before trusting a ranking.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

private val SearchMode.label: String
    get() = when (this) {
        SearchMode.SEMANTIC -> "Semantic"
        SearchMode.BM25 -> "BM25"
        SearchMode.HYBRID -> "Hybrid"
    }

private val SearchMode.explanation: String
    get() = when (this) {
        SearchMode.SEMANTIC -> "Meaning similarity from EmbeddingGemma."
        SearchMode.BM25 -> "Exact word matches, weighted by frequency and rarity."
        SearchMode.HYBRID -> "Semantic and BM25 ranks combined with RRF."
    }

@Composable
private fun ResultCard(rank: Int, hit: SearchHit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("#$rank  ·  Chunk ${hit.chunkIndex + 1}",
                style = MaterialTheme.typography.titleMedium)
            Text("Semantic #${hit.semanticRank}  ·  " +
                (hit.bm25Rank?.let { "BM25 #$it" } ?: "No BM25 match"),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary)
            Text(hit.text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
