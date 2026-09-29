# Build Hybrid Search for RAG on Android with BM25

Episode 5 extends the Episode 4 RAG Document Lab project. Open [rag-document-lab](rag-document-lab) in Android Studio. The app still imports a text-based PDF, chunks it, embeds the chunks with EmbeddingGemma, and saves them in Room. The new `Search PDF` action compares three rankings of the same saved passages.

| Mode | What it does |
| --- | --- |
| Semantic | Embeds the question and sorts saved vectors by cosine similarity. |
| BM25 | Scores matching words in the saved chunk text, using length and word rarity. |
| Hybrid | Combines semantic and BM25 positions with reciprocal rank fusion. |

Select a saved PDF, tap `Search PDF`, enter a question, and switch the three modes after the results appear. The app shows the first five source chunks with their semantic and BM25 ranks. It does not generate answers. Results are scoped to the selected PDF.

Put the EmbeddingGemma task bundle at `rag-document-lab/app/src/main/assets/embedding_gemma.task` before building. The model is intentionally excluded from Git. See [the asset instructions](rag-document-lab/app/src/main/assets/README.md). Run `./gradlew testDebugUnitTest assembleDebug` from the Android project directory.

The application ID and Room schema match Episode 4. Installing this project over that app keeps already indexed documents. New indexing is only needed for new PDFs or a clean install. The search does not send the PDF, chunks, or question over the network.

This is an educational, single-document linear scan. BM25 statistics are calculated from that document's saved chunks for each submitted question. For very small PDFs, rarity estimates are weak. An exact match can help retrieve an identifier or lab term, but hybrid retrieval does not guarantee that the top passage is correct. Choose the recording example only after testing the real PDF and question on the device.
