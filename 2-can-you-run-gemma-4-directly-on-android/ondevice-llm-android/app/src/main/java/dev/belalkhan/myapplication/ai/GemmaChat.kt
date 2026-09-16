package dev.belalkhan.myapplication.ai

import android.content.Context
import android.net.Uri
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.MessageCallback
import com.google.ai.edge.litertlm.SamplerConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.belalkhan.myapplication.data.ModelDownloader
import dev.belalkhan.performanceoverlay.PerformanceMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class GemmaChat @Inject constructor(
    @ApplicationContext private val context: Context,
    private val modelDownloader: ModelDownloader,
    private val performanceMonitor: PerformanceMonitor,
) {
    private val mutex = Mutex()
    private val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var engine: Engine? = null
    private var conversation: Conversation? = null

    suspend fun sendMessage(
        prompt: String,
        imageUri: Uri?,
        onToken: (String) -> Unit,
    ) = mutex.withLock {
        performanceMonitor.onGenerationStarted()
        try {
            initialize()

            val content = buildList {
                imageUri?.let { add(Content.ImageBytes(readImage(it))) }
                add(Content.Text(prompt.ifBlank { "What do you see in this image?" }))
            }

            suspendCancellableCoroutine { continuation ->
                conversation?.sendMessageAsync(
                    Contents.of(content),
                    object : MessageCallback {
                        override fun onMessage(message: Message) {
                            message.contents.contents
                                .filterIsInstance<Content.Text>()
                                .forEach {
                                    performanceMonitor.onOutput(it.text)
                                    onToken(it.text)
                                }
                        }

                        override fun onDone() {
                            if (continuation.isActive) continuation.resume(Unit)
                        }

                        override fun onError(throwable: Throwable) {
                            if (continuation.isActive) continuation.resumeWithException(throwable)
                        }
                    },
                )
                continuation.invokeOnCancellation { conversation?.cancelProcess() }
            }
        } finally {
            performanceMonitor.onGenerationFinished()
        }
    }

    suspend fun clearConversation() = mutex.withLock {
        conversation?.close()
        conversation = null
    }

    fun release() {
        cleanupScope.launch {
            mutex.withLock {
                conversation?.close()
                conversation = null
                engine?.close()
                engine = null
            }
        }
    }

    private fun initialize() {
        if (conversation != null) return

        if (engine == null) {
            check(modelDownloader.isModelDownloaded()) { "Download the model before chatting." }
            engine = Engine(
                EngineConfig(
                    modelPath = modelDownloader.modelFile.absolutePath,
                    backend = Backend.CPU(),
                    visionBackend = Backend.CPU(),
                    maxNumImages = 1,
                    cacheDir = context.cacheDir.absolutePath,
                )
            ).also(Engine::initialize)
        }

        conversation = engine?.createConversation(
            ConversationConfig(
                samplerConfig = SamplerConfig(
                    topK = 20,
                    topP = 0.9,
                    temperature = 0.7,
                )
            )
        )
    }

    private fun readImage(uri: Uri): ByteArray =
        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("Could not read the selected image.")
}
