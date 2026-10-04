package dev.belalkhan.ragdocumentlab.ui.context

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private val steps = listOf("Question", "Retrieve", "Build Context", "Model Input")
private val julyReportDemoQuestions = listOf(
    "Uric acid" to "What was my uric acid result?",
    "Random glucose" to "What was my random blood glucose result?",
    "TSH" to "What was my TSH result?",
    "Vitamin D included?" to "Does this report include a vitamin D result?"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContextLabScreen(
    state: ContextLabUiState,
    onBack: () -> Unit,
    onQuestionChange: (String) -> Unit,
    onPrepareContext: () -> Unit,
    onExportPrompt: (String) -> Unit
) {
    val pager = rememberPagerState(pageCount = { steps.size })
    val scope = rememberCoroutineScope()
    var showIndexDetails by rememberSaveable { mutableStateOf(false) }
    var expandedCandidate by rememberSaveable { mutableIntStateOf(-1) }
    var showOmissions by rememberSaveable { mutableStateOf(false) }
    var showInstructions by rememberSaveable { mutableStateOf(false) }
    var showQuestion by rememberSaveable { mutableStateOf(false) }
    var showSources by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.question, state.document.id) {
        expandedCandidate = -1
        showOmissions = false
        showInstructions = false
        showQuestion = false
        showSources = false
        if (state.prepared == null) pager.scrollToPage(0)
    }
    LaunchedEffect(state.prepared) {
        if (state.prepared != null) pager.animateScrollToPage(1)
    }
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(Modifier.fillMaxHeight().widthIn(max = 720.dp)) {
            TopAppBar(
                title = {
                    Column {
                        Text("Context Lab")
                        Text(state.document.name, style = MaterialTheme.typography.bodySmall,
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
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } },
                    enabled = pager.currentPage > 0
                ) { Text("Previous step") }
                Text("${steps[pager.currentPage]} · ${pager.currentPage + 1} of 4",
                    style = MaterialTheme.typography.labelLarge)
                TextButton(
                    onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                    enabled = pager.currentPage < 3 && state.prepared != null
                ) { Text("Next step") }
            }
            HorizontalPager(
                state = pager,
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) { page ->
                when (page) {
                    0 -> QuestionPage(state, showIndexDetails,
                        onIndexDetailsToggle = { showIndexDetails = it },
                        onQuestionChange = onQuestionChange)
                    1 -> RetrievePage(state, expandedCandidate) { expandedCandidate = it }
                    2 -> BuildContextPage(state, showOmissions) { showOmissions = it }
                    else -> ModelInputPage(
                        state, showInstructions, showQuestion, showSources,
                        onInstructionsToggle = { showInstructions = it },
                        onQuestionToggle = { showQuestion = it },
                        onSourcesToggle = { showSources = it }
                    )
                }
            }
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (pager.currentPage) {
                    0 -> Button(
                        onClick = onPrepareContext,
                        enabled = state.question.isNotBlank() && !state.preparing,
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) { Text(if (state.preparing) "PREPARING CONTEXT…" else "PREPARE CONTEXT") }
                    1, 2 -> Button(
                        onClick = {
                            scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                        },
                        enabled = state.prepared != null,
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Text(if (pager.currentPage == 1) "NEXT · BUILD CONTEXT"
                            else "NEXT · MODEL INPUT")
                    }
                    else -> Button(
                        onClick = { state.prepared?.let { onExportPrompt(it.exportText) } },
                        enabled = state.prepared != null && !state.preparing,
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) { Text("EXPORT PROMPT") }
                }
            }
        }
    }
}

@Composable
private fun QuestionPage(
    state: ContextLabUiState,
    showIndexDetails: Boolean,
    onIndexDetailsToggle: (Boolean) -> Unit,
    onQuestionChange: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            LabCard {
                Text("Question", style = MaterialTheme.typography.titleLarge)
                Text("This PDF is already indexed. Ask one question to retrieve saved chunks.",
                    style = MaterialTheme.typography.bodyMedium)
                Text(state.document.name, style = MaterialTheme.typography.labelLarge)
                Text("${state.document.chunkCount} saved chunks",
                    style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { onIndexDetailsToggle(!showIndexDetails) }) {
                    Text(if (showIndexDetails) "HIDE INDEX DETAILS" else "VIEW INDEX DETAILS")
                }
                if (showIndexDetails) {
                    Text("The PDF text and EmbeddingGemma vectors are already saved in Room. " +
                        "Preparing context reads those saved chunks; it does not reindex the PDF.",
                        style = MaterialTheme.typography.bodySmall)
                }
                OutlinedTextField(
                    value = state.question,
                    onValueChange = onQuestionChange,
                    label = { Text("Your question") },
                    placeholder = { Text("What was my uric acid result?") },
                    enabled = !state.preparing,
                    modifier = Modifier.fillMaxWidth()
                )
                if (state.document.name.equals("July Report.pdf", ignoreCase = true)) {
                    Text("TRY A QUESTION FROM THIS REPORT",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        julyReportDemoQuestions.forEach { (label, question) ->
                            AssistChip(
                                onClick = { onQuestionChange(question) },
                                label = { Text(label) },
                                enabled = !state.preparing
                            )
                        }
                    }
                    Text("Tap to fill the question, then prepare context.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (state.preparing) item {
            LabCard {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator()
                    Text("Retrieving and preparing context")
                }
            }
        }
        state.error?.let { message -> item { ErrorCard(message) } }
    }
}

@Composable
private fun RetrievePage(
    state: ContextLabUiState,
    expandedCandidate: Int,
    onExpand: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            LabCard {
                Text("Retrieve", style = MaterialTheme.typography.titleLarge)
                Text("Hybrid retrieval candidates, ordered by rank. A high rank does not prove " +
                    "a passage supports an answer.", style = MaterialTheme.typography.bodyMedium)
            }
        }
        when {
            state.preparing -> item { LabCard { Text("Retrieving saved chunks…") } }
            state.error != null && state.prepared == null ->
                item { ErrorCard(state.error) }
            state.prepared == null ->
                item { LabCard { Text("Prepare context from the Question page to see candidates.") } }
            state.retrievalCandidates.isEmpty() ->
                item { LabCard { Text("No retrieval candidates were found in this PDF.") } }
            else -> itemsIndexed(state.retrievalCandidates, key = { _, hit -> hit.chunkIndex }) {
                rank, hit ->
                LabCard {
                    Text("Candidate #${rank + 1} · Chunk ${hit.chunkIndex + 1}",
                        style = MaterialTheme.typography.titleMedium)
                    Text(hit.text.take(180) + if (hit.text.length > 180) "…" else "",
                        style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = {
                        onExpand(if (expandedCandidate == hit.chunkIndex) -1 else hit.chunkIndex)
                    }) {
                        Text(if (expandedCandidate == hit.chunkIndex) "HIDE DETAILS"
                            else "VIEW COMPLETE CHUNK")
                    }
                    if (expandedCandidate == hit.chunkIndex) {
                        Text("Semantic rank: ${hit.semanticRank ?: "none"} · " +
                            "BM25 rank: ${hit.bm25Rank ?: "no match"}",
                            style = MaterialTheme.typography.labelMedium)
                        Text("Raw scores are not retained by this search result.",
                            style = MaterialTheme.typography.bodySmall)
                        Text(hit.text, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun BuildContextPage(
    state: ContextLabUiState,
    showOmissions: Boolean,
    onOmissionsToggle: (Boolean) -> Unit
) {
    val request = state.prepared
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            LabCard {
                Text("Build Context", style = MaterialTheme.typography.titleLarge)
                Text("These complete blocks are the evidence included in the request. " +
                    "Retrieval candidates can be omitted by the budget.",
                    style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (request == null) {
            item { LabCard { Text("Prepare context from the Question page first.") } }
        } else {
            item {
                LabCard {
                    Text("Selected source blocks", style = MaterialTheme.typography.titleMedium)
                    Text(request.selected.joinToString { "[${it.id}] Chunk ${it.chunkNumber}" }
                        .ifEmpty { "None fit" })
                    Text("Estimated input: ${request.estimatedInputTokens} / " +
                        "${request.budget.inputLimit} token allowance",
                        style = MaterialTheme.typography.bodySmall)
                    Text("Omitted candidates: ${request.omitted.size}",
                        style = MaterialTheme.typography.bodySmall)
                    if (request.selected.isEmpty()) {
                        Text("No complete source block fits this budget. The exported prompt " +
                            "will have no source evidence.",
                            color = MaterialTheme.colorScheme.error)
                    }
                    if (request.omitted.isNotEmpty()) {
                        TextButton(onClick = { onOmissionsToggle(!showOmissions) }) {
                            Text(if (showOmissions) "HIDE OMITTED CANDIDATES"
                                else "VIEW OMITTED CANDIDATES")
                        }
                    }
                }
            }
            if (showOmissions) {
                itemsIndexed(request.omitted) { _, omitted ->
                    LabCard {
                        Text("Chunk ${omitted.chunkNumber}",
                            style = MaterialTheme.typography.titleSmall)
                        Text(omitted.reason, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun ModelInputPage(
    state: ContextLabUiState,
    showInstructions: Boolean,
    showQuestion: Boolean,
    showSources: Boolean,
    onInstructionsToggle: (Boolean) -> Unit,
    onQuestionToggle: (Boolean) -> Unit,
    onSourcesToggle: (Boolean) -> Unit
) {
    val request = state.prepared
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            LabCard {
                Text("Model Input", style = MaterialTheme.typography.titleLarge)
                Text("This is the application request prepared for Gemma.",
                    style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (request == null) {
            item { LabCard { Text("Prepare context from the Question page first.") } }
        } else {
            item {
                LabCard {
                    Text("${request.selected.size} source blocks · " +
                        "estimated ${request.estimatedInputTokens} input tokens",
                        style = MaterialTheme.typography.titleMedium)
                    Text("Input allowance: ${request.budget.inputLimit} · " +
                        "reserved output: ${request.budget.reservedOutputTokens} · " +
                        "safety margin: ${request.budget.safetyMarginTokens}",
                        style = MaterialTheme.typography.bodySmall)
                    Text("Estimated as one token per UTF-8 byte, plus " +
                        "${request.budget.chatFormattingTokens} estimated formatting tokens. " +
                        "This is an estimate, not a measured model token count.",
                        style = MaterialTheme.typography.bodySmall)
                    Text("The export keeps system instructions and the user message separate.",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
            item {
                LabCard {
                    TextButton(onClick = { onInstructionsToggle(!showInstructions) }) {
                        Text(if (showInstructions) "HIDE SYSTEM INSTRUCTIONS"
                            else "VIEW SYSTEM INSTRUCTIONS")
                    }
                    if (showInstructions) Text(request.instructions)
                }
            }
            item {
                LabCard {
                    TextButton(onClick = { onQuestionToggle(!showQuestion) }) {
                        Text(if (showQuestion) "HIDE QUESTION" else "VIEW QUESTION")
                    }
                    if (showQuestion) Text(request.questionSection)
                }
            }
            item {
                LabCard {
                    TextButton(onClick = { onSourcesToggle(!showSources) }) {
                        Text(if (showSources) "HIDE SOURCE BLOCKS" else "VIEW SOURCE BLOCKS")
                    }
                    if (showSources) {
                        Text(request.evidenceHeading)
                        request.selected.forEach { source -> Text(source.block) }
                    }
                }
            }
        }
    }
}

@Composable
private fun LabCard(content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

@Composable
private fun ErrorCard(message: String) {
    Card(colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.errorContainer
    )) { Text(message, Modifier.fillMaxWidth().padding(18.dp)) }
}
