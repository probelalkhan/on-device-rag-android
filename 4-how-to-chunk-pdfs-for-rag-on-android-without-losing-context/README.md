# HOW TO CHUNK PDFS FOR RAG ON ANDROID WITHOUT LOSING CONTEXT

This is the source code for Episode 4 of the On Device RAG on Android course.

The final project is **RAG Document Lab**, a small offline Android app that demonstrates the document-ingestion side of RAG:

```text
PDF → extract text → create overlapping chunks → create embeddings → store locally
```

## What the app demonstrates

- Selecting a PDF with Android's system document picker
- Extracting its text on the device
- Splitting the text into readable chunks with overlap
- Creating a 768-dimensional vector for every chunk with EmbeddingGemma
- Saving the document, chunks, and embeddings in a local Room database
- Restoring the indexed-document list after the app restarts

The app deliberately stops after indexing. Retrieval and generation are covered separately so the ingestion pipeline remains easy to see and explain.

## Project

Open [`rag-document-lab`](rag-document-lab) in Android Studio.

Before running the app, download the EmbeddingGemma model bundle and place it here:

```text
rag-document-lab/app/src/main/assets/embedding_gemma.task
```

The model file is not committed to this repository because of its size and license terms. See the assets README inside the Android project for the expected model configuration.

## Architecture

The app uses a deliberately small MVVM structure:

```text
MainActivity / DocumentScreen
              ↓
       DocumentViewModel
              ↓
       DocumentRepository
        ↙      ↓       ↘
PDF extractor  chunker  EmbeddingGemma
                         ↓
                    Room database
```

Hilt creates the ViewModel dependencies. Each ingestion component has one responsibility, while `DocumentRepository` keeps the complete pipeline readable in one place.

## Chunking strategy

`TextChunker` uses a maximum chunk size of 800 characters and carries 120 characters into the next chunk. It prefers ending at a paragraph or sentence boundary when one is available.

The overlap repeats a small part of the previous chunk so a sentence or idea near a boundary does not lose all of its surrounding context. Character counts keep the lesson independent from a model-specific tokenizer; production apps should measure the final token count for their chosen embedding and generation models.

## Local storage

Room stores two tables:

- `documents` stores one row per imported PDF.
- `chunks` stores the text and embedding vector for each chunk.

The `FloatArray` embedding is encoded as a byte array before SQLite stores it as a BLOB. Importing a PDF with the same display name replaces the earlier copy and its chunks.

## Notes

- Processing is fully local after the model has been added to the project.
- PDF text extraction works with text-based PDFs. Scanned image PDFs need OCR, which is outside this episode.
- This is an educational demo, not a production document pipeline.
- The project intentionally contains no unit or instrumented tests.

