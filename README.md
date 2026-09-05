# Building an on-device RAG app on Android with Gemma

Build a private, offline Retrieval-Augmented Generation (RAG) system for Android with Gemma and LiteRT LM. This free course covers local inference, document indexing, embeddings, retrieval, grounded answers, evaluation, and real-device constraints.

**[Explore the free course](https://simplifiedcoding.net/courses/on-device-rag-android-gemma)** · **[Watch the course promo](https://www.youtube.com/watch?v=8j_KOA8Y4bU)**

[![Building an on-device RAG app on Android with Gemma course promo](images/on-device-rag-android-gemma-promo.jpg)](https://www.youtube.com/watch?v=8j_KOA8Y4bU)

## About the course

This course follows the engineering of a complete on-device RAG app, from private files to answers backed by retrieved evidence. The local path can work without an internet connection after the required model assets are available and the files have been indexed.

The core pipeline is:

> Private files → text chunks → embeddings → retrieval → context → Gemma → grounded answer

The goal is not another cloud API wrapper. You will learn how the system works, where it fails, how to measure it, and what must change before an on-device AI feature is ready for real users.

## What you will learn

- Run Gemma locally on Android with LiteRT LM.
- Manage model initialization, inference, concurrency, cancellation, and storage.
- Extract and chunk text for effective retrieval.
- Create embeddings and search a local vector index.
- Improve retrieval with lexical signals, metadata, thresholds, hybrid search, and reranking.
- Build compact prompts for an on-device model with a limited context window.
- Generate answers grounded in evidence and link them back to their sources.
- Separate retrieval failures from generation failures.
- Evaluate relevance, groundedness, refusal behavior, latency, and regressions.
- Measure memory pressure, battery use, thermals, storage, and performance across Android devices.
- Plan model delivery, integrity checks, compatibility, offline UX, and optional cloud fallback.

## Why on-device RAG matters

- **Privacy:** Files and retrieval data can remain on the Android device.
- **Offline access:** The local RAG path can answer questions without a network connection.
- **Grounded output:** Gemma receives selected evidence instead of being asked to guess from memory.
- **Engineering control:** You decide how data is indexed, retrieved, ranked, cited, and evaluated.

## Who this course is for

This course is for Android and mobile engineers who already know how to build applications and want to move beyond cloud AI wrappers. You should be comfortable with Kotlin, coroutines, application architecture, and local persistence. No machine learning background is required.

## About the instructor

The course is created by [Belal Khan](https://github.com/probelalkhan), a Google Developer Expert for Android and the creator of Simplified Coding. It is based on the practical decisions, failures, tradeoffs, and device limitations found while building an on-device RAG application.

## Repository contents

This repository contains companion assets for each course episode, including slides, demos, references, and source code when an episode includes a project.

- Episode directories use the format `N-episode-title` so they stay in chronological order.
- Assets live at the top level of each episode directory.
- A source-code repository is the only allowed subdirectory inside an episode directory.

## Episodes
