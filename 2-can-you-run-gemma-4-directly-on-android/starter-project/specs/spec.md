# Pocket AI

## Goal

Build a small Android app called Pocket AI that runs Gemma 4 E2B directly on device with LiteRT LM.

The user can download the model once, send a text prompt or one optional image, and see the response stream into the chat. After the model is downloaded, inference must work offline without a cloud inference API.

## Architecture

Keep the existing Android project and package name.

Use MVVM with Hilt.

Use standard Hilt wiring end to end with an `@HiltAndroidApp` Application, an `@AndroidEntryPoint` launcher Activity, and an `@HiltViewModel` `ChatViewModel`. Do not replace Hilt with a custom ViewModel factory or manual service container.

- `ChatViewModel` owns UI state and actions.
- `GemmaChat` owns LiteRT LM `Engine` and `Conversation`.
- `ModelDownloader` owns model download and storage.
- Provide one shared `OkHttpClient` with Hilt.

Use current stable compatible Android libraries. Use LiteRT LM Android `0.14.0`.

## Model download

Use OkHttp.

Model file.

`gemma-4-e2b-it.litertlm`

Model URL.

`https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm`

Store the model in app internal storage. Do not hardcode the expected model size or treat a partial file as valid.

Read `HF_TOKEN` from root `local.properties` and expose it as `BuildConfig.HF_TOKEN` for debug builds.

The downloader must report progress, use a `.part` file, resume with HTTP `Range` when possible, and rename only after the complete model has been downloaded.

Determine the expected total size from the server response. Use `Content-Length` for a fresh download and the total size from `Content-Range` for a resumed download. Rename the `.part` file only when the downloaded byte count matches the expected total.

Handle Hugging Face redirects explicitly. Preserve the `Range` header across redirects and send the bearer token only to `huggingface.co`, never redirected CDN or storage hosts.

Add Internet permission.

## Local inference

Use one LiteRT LM `Engine` and one current `Conversation` inside `GemmaChat`.

Configure the Engine with the downloaded model, CPU backend, CPU vision backend, maximum one image, and app cache directory.

Do not initialize the model on the Android main thread or recreate the Engine for every message.

Use `topK = 20`, `topP = 0.9`, and `temperature = 0.7`.

Provide.

`sendMessage(prompt: String, imageUri: Uri?, onToken: (String) -> Unit)`

Use `sendMessageAsync` and stream generated text through `onToken`. Support one optional image with `Content.ImageBytes`. If an image is sent without text, use `What do you see in this image?`.

Keep the Conversation between messages. Clearing chat closes only the current Conversation. Release the Engine when `GemmaChat` is no longer needed.

## Chat state

Use a Hilt `ChatViewModel` with `StateFlow` for.

- `messages`
- `isModelDownloaded`
- `downloadProgress`
- `isBusy`
- `error`

Keep messages in memory only. Prevent overlapping download or inference work.

Run inference from `viewModelScope` on `Dispatchers.Default`. Add the user message immediately, then an empty streaming assistant message, and append generated text as it arrives.

## UI

Build the UI with Jetpack Compose.

Show a model download screen before the model is available, then a modern chat screen with Pocket AI and visible On-device status, separate user and assistant messages, streamed Markdown responses, text input, Android Photo Picker for one optional image, clear chat confirmation, and useful error states.

Use a sleek, polished and professional modern UI with strong spacing, clear hierarchy, and consistent Material 3 styling.

Support both Material 3 light and dark color schemes and select between them from the current system theme using `isSystemInDarkTheme()` or the equivalent system-aware mechanism. Never hardcode the app to a light color scheme.

Use consistent Material 3 spacing with comfortable screen padding and no excessive empty space. Handle status bar, navigation bar, and IME insets once at the appropriate container level so edge to edge content is correct without duplicated padding.

The keyboard must never cover the chat input. When the keyboard is open, the chat composer should sit directly above it without an extra bottom gap or duplicated IME padding. Keep the chat composer compact, rounded and modern Material 3 style.

## Done when

The project builds successfully and can download a complete model, stream Gemma responses, keep and clear conversation context, prevent overlapping inference, and run inference offline after download.

Before finishing, run a debug build and fix any compile errors. Do not launch or run the app.