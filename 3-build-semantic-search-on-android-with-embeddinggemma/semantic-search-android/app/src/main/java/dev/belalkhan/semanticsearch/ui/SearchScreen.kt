package dev.belalkhan.semanticsearch.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import dev.belalkhan.semanticsearch.model.searchDocuments
import java.util.Locale

@Composable
fun SearchScreen(
    state: SearchUiState,
    onQueryChange: (String) -> Unit,
    onLiteralSearch: (String) -> Unit,
    onSemanticSearch: (String) -> Unit,
    onTopKChange: (Int) -> Unit,
    onThresholdChange: (Float) -> Unit
) {
    val inputEnabled = !state.searching
    val semanticEnabled = !state.preparing && inputEnabled && state.error == null
    val keyboard = LocalSoftwareKeyboardController.current
    var showSettings by rememberSaveable { mutableStateOf(false) }
    val literalSearch: (String) -> Unit = { query -> keyboard?.hide(); onLiteralSearch(query) }
    val semanticSearch: (String) -> Unit = { query -> keyboard?.hide(); onSemanticSearch(query) }
    val colors = MaterialTheme.colorScheme

    LazyColumn(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().imePadding(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("recall", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                Badge("ON DEVICE")
            }
            Spacer(Modifier.height(28.dp))
            Text("Different words.\nSame meaning.", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(8.dp))
            Text("Find what you need in your saved examples.",
                color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }
        item {
            Surface(shape = RoundedCornerShape(24.dp), color = colors.surface) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("WHAT ARE YOU LOOKING FOR?", style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant)
                    OutlinedTextField(
                        value = state.query, onValueChange = onQueryChange,
                        modifier = Modifier.fillMaxWidth(), enabled = inputEnabled,
                        shape = RoundedCornerShape(14.dp), maxLines = 3,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            if (semanticEnabled) semanticSearch(state.query)
                        })
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { literalSearch(state.query) },
                            enabled = inputEnabled && state.query.isNotBlank(),
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) { Text("Literal search") }
                        Button(
                            onClick = { semanticSearch(state.query) },
                            enabled = semanticEnabled && state.query.isNotBlank(),
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(when {
                                state.preparing -> "Preparing..."
                                state.searching -> "Searching..."
                                else -> "Semantic search"
                            })
                        }
                    }
                    Text("TRY A MATCH", style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("dessert", "pets", "driving", "footwear", "flowers", "flying").forEach { query ->
                            SuggestionChip(onClick = { onQueryChange(query) }, enabled = inputEnabled,
                                label = { Text(query) })
                        }
                    }
                    Text("UNRELATED", style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("encryption", "photosynthesis").forEach { query ->
                            SuggestionChip(onClick = { onQueryChange(query) }, enabled = inputEnabled,
                                label = { Text(query) })
                        }
                    }
                    state.error?.let { Text(it, color = colors.error, style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
        if (state.searchType != null) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (state.searchType == SearchType.LITERAL) "Literal results" else "Semantic results",
                        style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    if (state.searchType == SearchType.SEMANTIC) {
                        Text("TOP ${state.topK}", style = MaterialTheme.typography.labelMedium,
                            color = colors.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(12.dp))
                if (state.searchType == SearchType.LITERAL) {
                    Surface(shape = RoundedCornerShape(16.dp), color = colors.surface,
                        border = BorderStroke(1.dp, colors.outlineVariant)) {
                        Text(state.literalResults.joinToString { it.text }.ifEmpty { "No literal match" },
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    }
                }
            }
            if (state.searchType == SearchType.SEMANTIC && state.results.isEmpty()) {
                item {
                    Surface(shape = RoundedCornerShape(20.dp), color = colors.primaryContainer) {
                        Column(Modifier.fillMaxWidth().padding(20.dp)) {
                            Text("No relevant result", style = MaterialTheme.typography.titleMedium)
                            Text("No example passed the similarity threshold.", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            if (state.searchType == SearchType.SEMANTIC) itemsIndexed(state.results) { index, result ->
                Surface(shape = RoundedCornerShape(20.dp), color = colors.primaryContainer) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("${index + 1}  /  SEMANTIC MATCH", style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.weight(1f))
                            Text("ITEM ${result.document.id}", style = MaterialTheme.typography.labelMedium)
                        }
                        Text(result.document.text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Text(String.format(Locale.US, "Cosine similarity: %.3f", result.similarity), style = MaterialTheme.typography.labelLarge,
                            color = colors.onSurfaceVariant)
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Your examples", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                Text("${searchDocuments.size} SAVED", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
            }
        }
        item {
            BoxWithConstraints {
                val cardWidth = (maxWidth - 12.dp) / 2
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    searchDocuments.forEach { document ->
                        Surface(
                            modifier = Modifier.width(cardWidth),
                            shape = RoundedCornerShape(18.dp),
                            color = colors.surface
                        ) {
                            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(document.id, style = MaterialTheme.typography.labelMedium, color = colors.primary)
                                Text(document.text, style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            }
        }
        item {
            TextButton(onClick = { showSettings = !showSettings }, modifier = Modifier.fillMaxWidth()) {
                Text(if (showSettings) "Hide search settings −" else "Search settings +")
            }
            if (showSettings) {
                Surface(shape = RoundedCornerShape(20.dp), color = colors.surface) {
                    Column(Modifier.padding(20.dp)) {
                        Text("Results to keep", style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(1, 3, searchDocuments.size).forEach { k ->
                                FilterChip(selected = state.topK == k, onClick = { onTopKChange(k) }, enabled = inputEnabled,
                                    label = { Text(if (k == searchDocuments.size) "All results" else "$k") })
                            }
                        }
                        Text("Minimum similarity: ${String.format(Locale.US, "%.2f", state.threshold)}",
                            style = MaterialTheme.typography.bodyMedium)
                        Slider(value = state.threshold, onValueChange = onThresholdChange,
                            valueRange = -1f..1f, enabled = inputEnabled)
                        Text("Change a setting, then search again.", style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Powered by EmbeddingGemma · Fully offline", modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
    }
}

@Composable
private fun Badge(text: String) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(8.dp)) {
        Text(text, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
    }
}
