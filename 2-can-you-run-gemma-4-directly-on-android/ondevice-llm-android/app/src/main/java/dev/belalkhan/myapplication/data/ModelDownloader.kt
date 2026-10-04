package dev.belalkhan.myapplication.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.belalkhan.myapplication.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelDownloader @Inject constructor(
    @ApplicationContext context: Context,
    private val client: OkHttpClient,
) {
    val modelFile = File(context.filesDir, MODEL_FILE_NAME)
    private val partFile = File(context.filesDir, "$MODEL_FILE_NAME.part")
    private val sizeFile = File(context.filesDir, "$MODEL_FILE_NAME.size")

    fun isModelDownloaded(): Boolean {
        val expectedSize = runCatching {
            sizeFile.takeIf(File::isFile)?.readText()?.trim()?.toLongOrNull()
        }.getOrNull()
        return expectedSize != null && expectedSize > 0 && modelFile.length() == expectedSize
    }

    suspend fun download(onProgress: (Float) -> Unit) = withContext(Dispatchers.IO) {
        if (isModelDownloaded()) return@withContext
        recoverUnverifiedModel()

        var existingBytes = partFile.takeIf(File::isFile)?.length() ?: 0L
        var restarted = false

        while (true) {
            openResponse(existingBytes).use { response ->
                when (response.code) {
                    200 -> {
                        val expectedTotal = response.body.contentLength()
                        check(expectedTotal > 0) { "The server did not provide the model size." }
                        writeResponse(response, expectedTotal, append = false, onProgress)
                        finishDownload(expectedTotal, onProgress)
                        return@withContext
                    }

                    206 -> {
                        val range = parseContentRange(response.header("Content-Range"))
                            ?: error("The server returned an invalid Content-Range header.")
                        if (existingBytes <= 0 || range.start != existingBytes) {
                            check(!restarted) {
                                "The server returned a different download range than requested."
                            }
                            deletePartFile()
                            existingBytes = 0L
                            restarted = true
                        } else {
                            val responseLength = response.body.contentLength()
                            val rangeLength = range.end - range.start + 1
                            check(responseLength == -1L || responseLength == rangeLength) {
                                "The server returned an incomplete download range."
                            }

                            writeResponse(response, range.total, append = true, onProgress)
                            finishDownload(range.total, onProgress)
                            return@withContext
                        }
                    }

                    416 -> {
                        val expectedTotal = parseUnsatisfiedRange(response.header("Content-Range"))
                            ?: error("The server rejected the saved download range.")

                        if (existingBytes == expectedTotal) {
                            finishDownload(expectedTotal, onProgress)
                            return@withContext
                        }

                        check(!restarted) { "The saved download could not be resumed." }
                        deletePartFile()
                        existingBytes = 0L
                        restarted = true
                    }

                    else -> error("Download failed with HTTP ${response.code}.")
                }
            }
        }
    }

    private fun writeResponse(
        response: Response,
        expectedTotal: Long,
        append: Boolean,
        onProgress: (Float) -> Unit,
    ) {
        var downloadedBytes = if (append) partFile.length() else 0L
        var lastReportedBytes = downloadedBytes
        reportProgress(downloadedBytes, expectedTotal, onProgress)

        response.body.byteStream().use { input ->
            FileOutputStream(partFile, append).use { output ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val count = input.read(buffer)
                    if (count == -1) break

                    output.write(buffer, 0, count)
                    downloadedBytes += count
                    check(downloadedBytes <= expectedTotal) {
                        "The server sent more data than expected."
                    }

                    if (downloadedBytes - lastReportedBytes >= PROGRESS_INTERVAL) {
                        lastReportedBytes = downloadedBytes
                        reportProgress(downloadedBytes, expectedTotal, onProgress)
                    }
                }
            }
        }
    }

    private fun finishDownload(expectedTotal: Long, onProgress: (Float) -> Unit) {
        check(partFile.length() == expectedTotal) {
            "The download is incomplete (${partFile.length()} of $expectedTotal bytes)."
        }
        Files.move(partFile.toPath(), modelFile.toPath(), ATOMIC_MOVE, REPLACE_EXISTING)
        sizeFile.writeText(expectedTotal.toString())
        onProgress(1f)
    }

    private fun recoverUnverifiedModel() {
        if (!modelFile.isFile) return

        sizeFile.delete()
        if (!partFile.isFile || modelFile.length() > partFile.length()) {
            Files.move(modelFile.toPath(), partFile.toPath(), ATOMIC_MOVE, REPLACE_EXISTING)
        } else {
            check(modelFile.delete()) { "Could not remove the invalid model file." }
        }
    }

    private fun deletePartFile() {
        check(!partFile.exists() || partFile.delete()) { "Could not restart the model download." }
    }

    private fun openResponse(existingBytes: Long): Response {
        var url = MODEL_URL

        repeat(MAX_REDIRECTS) {
            val requestUrl = url.toHttpUrl()
            val request = Request.Builder()
                .url(requestUrl)
                .header("Accept-Encoding", "identity")
                .apply {
                    if (requestUrl.host == HUGGING_FACE_HOST && BuildConfig.HF_TOKEN.isNotBlank()) {
                        header("Authorization", "Bearer ${BuildConfig.HF_TOKEN}")
                    }
                    if (existingBytes > 0) header("Range", "bytes=$existingBytes-")
                }
                .build()

            val response = client.newCall(request).execute()
            if (response.code !in REDIRECT_CODES) return response

            val location = response.header("Location")
            response.close()
            check(!location.isNullOrBlank()) { "Download redirect did not include a location." }
            url = request.url.resolve(location)?.toString()
                ?: error("Could not resolve the download redirect.")
        }

        error("Too many download redirects.")
    }

    private fun reportProgress(downloaded: Long, total: Long, callback: (Float) -> Unit) {
        val progress = (downloaded.toDouble() / total).toFloat().coerceIn(0f, 0.99f)
        callback(progress)
    }

    private companion object {
        const val MODEL_FILE_NAME = "gemma-4-e2b-it.litertlm"
        const val MODEL_URL =
            "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm"
        const val HUGGING_FACE_HOST = "huggingface.co"
        const val PROGRESS_INTERVAL = 512 * 1024L
        const val MAX_REDIRECTS = 8
        val REDIRECT_CODES = setOf(301, 302, 303, 307, 308)
    }
}

internal data class ContentRange(
    val start: Long,
    val end: Long,
    val total: Long,
)

internal fun parseContentRange(header: String?): ContentRange? {
    val match = CONTENT_RANGE_REGEX.matchEntire(header.orEmpty()) ?: return null
    val start = match.groupValues[1].toLongOrNull() ?: return null
    val end = match.groupValues[2].toLongOrNull() ?: return null
    val total = match.groupValues[3].toLongOrNull() ?: return null
    return ContentRange(start, end, total).takeIf {
        it.start >= 0 && it.end >= it.start && it.end < it.total
    }
}

internal fun parseUnsatisfiedRange(header: String?): Long? =
    UNSATISFIED_RANGE_REGEX.matchEntire(header.orEmpty())
        ?.groupValues
        ?.get(1)
        ?.toLongOrNull()
        ?.takeIf { it > 0 }

private val CONTENT_RANGE_REGEX = Regex("bytes (\\d+)-(\\d+)/(\\d+)")
private val UNSATISFIED_RANGE_REGEX = Regex("bytes \\*/(\\d+)")
