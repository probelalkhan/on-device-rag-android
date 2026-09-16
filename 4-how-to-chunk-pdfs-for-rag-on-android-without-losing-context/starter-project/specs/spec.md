# RAG Document Lab engineering specification

## 1. Contract

- Application ID: `dev.belalkhan.ragdocumentlab`
- Product: RAG Document Lab

`MUST` and `MUST NOT` are normative. A change is complete when every applicable acceptance criterion passes.

The app turns a local, text-based PDF into persistent data for a later on-device RAG pipeline:

```text
PDF -> text extraction -> overlapping chunks -> EmbeddingGemma -> Room
```

Google EmbeddingGemma MUST create every chunk vector. Processing is local and MUST NOT require a network connection.

In scope: single PDF import, progress, errors, persistence, document listing, saved embedding inspection, and replacement by display name.

Out of scope: OCR, search, retrieval, generation, chat, batch import, deletion, cloud, accounts, background work, and automated tests.

## 2. User-visible behavior

### FR-1: Initial state

The app MUST show `RAG Document Lab`, one `Add a PDF` primary action, and documents ordered by `indexedAt` descending. An empty database MUST show an empty state.

### FR-2: PDF selection

`Add a PDF` MUST open the system document picker with MIME type `application/pdf`. Cancelling MUST leave state and storage unchanged.

### FR-3: Import lifecycle

For a selected URI, the app MUST execute these stages in order:

1. Extract PDF text.
2. Create chunks.
3. Load EmbeddingGemma if it is not already loaded.
4. Embed every chunk sequentially.
5. Persist the complete import in one Room transaction.

Only one import may run at a time. While it runs, the primary action MUST be disabled and progress MUST identify the active stage. Embedding progress MUST include the current chunk number and total chunk count.

### FR-4: Success

After success, the list MUST show the document name, chunk count, and indexed date. Progress MUST become `Document indexed and stored locally`, and the primary action MUST be enabled.

### FR-5: Failure

- A failed import MUST leave stored data unchanged.
- `CancellationException` MUST be rethrown.
- The UI MUST show a non-blank exception message, otherwise `Could not process this PDF`.
- Dismissing an error MUST clear only the error.
- After failure, progress MUST be `Ready to try another PDF` and the primary action MUST be enabled.

### FR-6: Saved embedding inspection

1. Tapping an indexed document MUST open its saved embeddings screen.
2. The screen MUST show the document name, chunk count, and vector dimension count.
3. Every chunk MUST be selectable in `chunkIndex` order.
4. The selected chunk MUST show its complete stored text and all stored float values with zero-based dimension indexes.
5. Values MUST use the stored `Float` representation without rounding.
6. Opening this screen MUST read Room data and MUST NOT run EmbeddingGemma.
7. Back MUST return to the document list.

## 3. Processing contracts

### ENG-1: PDF extraction

Contract: document-picker `Uri` -> `ExtractedPdf(name: String, text: String)`.

The extractor MUST:

1. Read the name from `OpenableColumns.DISPLAY_NAME`, falling back to `document.pdf`.
2. Open the URI through `ContentResolver` and load it with PDFBox Android.
3. Extract text with `PDFTextStripper`.
4. Close the input stream and PDF document for both success and failure.
5. Normalize `\r\n` and `\r` to `\n`.
6. Replace form-feed page separators with `\n\n`.
7. Collapse consecutive spaces or tabs to one space.
8. Remove horizontal whitespace adjacent to a newline.
9. Collapse three or more newlines to two.
10. Trim the result.

A blank result MUST fail with `No selectable text was found. Scanned PDFs need OCR.`

### ENG-2: Chunking

Constants:

| Name | Value |
| --- | ---: |
| Maximum chunk length | 800 characters |
| Overlap | 120 characters |

Given normalized text, the chunker MUST:

1. Return an empty list for blank input.
2. Begin the first candidate at offset `0`.
3. Set `candidateEnd` to `min(start + 800, text.length)`.
4. When `candidateEnd` is not the document end, search backward from it for a boundary at or after the candidate midpoint.
5. Search boundary types in this priority: `\n\n`, `. `, `? `, `! `, `\n`.
6. Select the last match of the first boundary type that has a valid match. Otherwise select `candidateEnd`.
7. Trim the selected substring and emit it when non-blank.
8. Assign zero-based indexes in emitted order.
9. Set the next start to `end - 120`.
10. If the next start is inside a word, move it forward to the next space before `end`.
11. Stop after reaching `text.length`.

Chunk text MUST retain the overlap. Character-based chunking MUST NOT be replaced by token-based chunking without changing this specification.

### ENG-3: EmbeddingGemma model

| Property | Required value |
| --- | --- |
| Model | Google EmbeddingGemma MediaPipe task bundle |
| Runtime | MediaPipe Tasks Text `TextEmbedder` |
| Asset path | `app/src/main/assets/embedding_gemma.task` |
| Output | Unquantized `FloatArray`, 768 values |

1. Every chunk MUST be embedded with this EmbeddingGemma model. No substitute model is permitted.
2. Document input MUST use task type `RETRIEVAL_DOCUMENT`, role `DOCUMENT`, and title `none`.
3. Returned vectors MUST be copied before storage.
4. Model creation MUST be lazy, occur off the main thread, and run only when an import reaches embedding.
5. Chunk inference MUST be sequential and off the main thread.
6. The engine MUST release MediaPipe resources when the ViewModel is cleared.
7. The model asset MUST remain excluded from Git and uncompressed in the APK.

### ENG-4: Persistence

Database name: `rag-documents.db`. Schema version: `1`.

`documents` contract:

| Field | Type | Constraint |
| --- | --- | --- |
| `id` | Long | Auto-generated primary key |
| `name` | String | Unique |
| `chunkCount` | Int | Equals persisted chunk rows |
| `indexedAt` | Long | Unix epoch milliseconds |

`chunks` contract:

| Field | Type | Constraint |
| --- | --- | --- |
| `id` | Long | Auto-generated primary key |
| `documentId` | Long | Foreign key to `documents.id`, cascade delete |
| `chunkIndex` | Int | Zero-based document order |
| `text` | String | Exact emitted chunk text |
| `embedding` | FloatArray | SQLite BLOB |

Float arrays MUST be encoded and decoded with little-endian byte order using exactly four bytes per float.

All embeddings MUST exist before persistence begins. Within one Room transaction, persistence MUST:

1. Find a document with the same display name.
2. Delete that document and its cascaded chunks when found.
3. Insert the new document.
4. Insert all chunks with the new document ID.

Reading a document's chunks MUST order them by `chunkIndex`.

## 4. Architecture boundaries

Dependency direction:

```text
Activity -> Compose screen -> ViewModel -> Repository
Repository -> PDF extractor, chunker, embedding engine, Room DAO
```

- The Activity owns the picker launcher.
- The Compose screen renders immutable state and emits callbacks. It MUST NOT access Room, PDFs, MediaPipe, or coroutine scopes.
- The ViewModel owns immutable UI state, concurrency control, and the import coroutine.
- The Repository owns pipeline order, dispatcher selection, and transaction initiation.
- Each Section 3 component owns only its named contract.
- Hilt MUST provide application-scoped infrastructure and construct the ViewModel.
- Unspecified layers, interfaces, modules, and screens MUST NOT be added.

## 5. UI design contract

### Visual system

Use Compose Material 3 with a light color scheme and Manrope. Shape radii MUST be 10 dp, 16 dp, and 24 dp for small, medium, and large roles.

| Role | Value |
| --- | --- |
| Background / surface | `#F7F7FA` / `#FFFFFF` |
| Primary / container | `#4355C5` / `#E2E6FF` |
| Secondary / container | `#006B5D` / `#9CF2DD` |
| Primary / secondary text | `#1B1B20` / `#62636C` |
| Hero gradient | `#26358D` to `#4355C5` |

| Role | Size / line height | Weight |
| --- | --- | --- |
| Hero | 34 sp / 40 sp | 700 |
| Section | 20 sp / 28 sp | 700 |
| Card title | 16 sp / 22 sp | 700 |
| Body | 14 sp / 22 sp | 400 |
| Caption | 12 sp / 18 sp | 400 |
| Label | 12 sp / 18 sp | 700 |

### Composition

Use edge-to-edge screens with safe drawing insets. White rounded cards sit on the neutral background. Primary containers identify pipeline icons and PDF icons. Secondary color identifies privacy and successful indexing. The home screen hero uses white content over the indigo gradient and contains the only primary action.

The home screen MUST present, in this order:

1. App identity
2. PDF import card and primary action
3. `Extract`, `Chunk`, `Embed`, `Store` pipeline
4. Active progress or current error
5. Indexed-document count
6. Empty state or document rows
7. Chunk size, overlap, and vector size summary

The saved embeddings screen MUST present, in this order:

1. Back action and document name
2. Chunk count, dimension count, and local storage summary
3. Previous and next chunk controls
4. Complete selected chunk text
5. Complete selected embedding as indexed float values

Layout constraints:

- Horizontal padding: 24 dp
- Vertical padding: 20 dp
- Major-section spacing: 24 dp
- Maximum content width: 720 dp, centered when more width is available
- Hero padding: 24 dp
- Primary action: full width and 52 dp high
- PDF names: one line with ellipsis
- Touch targets: at least 48 dp

The layout MUST remain usable without horizontal scrolling at phone and tablet widths.

## 6. Build and dependency constraints

- Compile SDK: 37
- Target SDK: 37
- Minimum SDK: 26
- Java compatibility: 17
- APK output: universal
- Build command: `./gradlew assembleDebug`

Dependencies MUST use stable, fixed versions. The build files are the authority for exact versions.

## 7. Acceptance criteria

| ID | Given | When | Then |
| --- | --- | --- | --- |
| AC-1 | The model is at the required asset path | `./gradlew assembleDebug` runs | One universal debug APK is produced |
| AC-2 | The app process is started | No PDF has been selected | EmbeddingGemma is not loaded |
| AC-3 | No stored documents exist | The main screen opens | The empty state and enabled `Add a PDF` action are visible |
| AC-4 | The picker is open | The user cancels | UI state and database contents do not change |
| AC-5 | A text-based PDF is selected | Import completes | At least one chunk is stored and every stored vector has 768 values |
| AC-6 | Extracted text exceeds 800 characters | It is chunked | Chunks are ordered, no chunk exceeds 800 characters, and adjacent chunks repeat up to 120 source characters |
| AC-7 | A PDF has no selectable text | Extraction completes | No rows are written and the OCR message is shown |
| AC-8 | Any extraction, chunking, or embedding step fails | The error reaches the ViewModel | Existing rows remain unchanged and the action becomes enabled |
| AC-9 | A document with name N exists | A successful import with name N completes | Exactly one document named N exists and only its new chunks remain |
| AC-10 | An import succeeds | The app process restarts | The document remains listed with the same chunk count |
| AC-11 | An import is active | The user views the screen | The action is disabled and the current stage is visible |
| AC-12 | Phone and tablet widths are used | The screen renders | Section 5 tokens are applied, content has no horizontal scrolling, and width is capped at 720 dp |
| AC-13 | The repository is inspected | Git status is checked | `embedding_gemma.task` is not tracked |
| AC-14 | An indexed document exists | Its list item is tapped | Its chunks load in order and the selected chunk shows every stored value without running EmbeddingGemma |
| AC-15 | The saved embeddings screen is open | Back is pressed | The document list returns without changing stored data |

## 8. Change rules

1. A requested behavior change MUST update this specification before or with the implementation.
2. A schema change MUST define its Room migration before implementation.
3. A change to chunking, EmbeddingGemma, or vector encoding MUST update its contract and acceptance criteria.
