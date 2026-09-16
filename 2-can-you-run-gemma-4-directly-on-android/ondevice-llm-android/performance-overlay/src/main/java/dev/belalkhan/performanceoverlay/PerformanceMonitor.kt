package dev.belalkhan.performanceoverlay

import android.app.ActivityManager
import android.content.Context
import android.os.PowerManager
import android.os.Process
import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class PerformanceMonitor(context: Context) {
    private val activityManager = context.getSystemService(ActivityManager::class.java)
    private val powerManager = context.getSystemService(PowerManager::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _metrics = MutableStateFlow(PerformanceMetrics())
    val metrics = _metrics.asStateFlow()

    private var sampleJob: Job? = null
    private var startedAt = 0L
    private var firstOutputAt = 0L
    private var outputCharacters = 0
    private var previousSampleAt = 0L
    private var previousCpuTime = 0L

    fun onGenerationStarted() {
        startedAt = SystemClock.elapsedRealtime()
        firstOutputAt = 0L
        outputCharacters = 0
        previousSampleAt = startedAt
        previousCpuTime = Process.getElapsedCpuTime()
        _metrics.value = readDeviceMetrics(
            PerformanceMetrics(isGenerating = true)
        )
        startSampling()
    }

    fun onOutput(text: String) {
        if (startedAt == 0L) return
        if (firstOutputAt == 0L) firstOutputAt = SystemClock.elapsedRealtime()
        outputCharacters += text.length
        updateInferenceMetrics(SystemClock.elapsedRealtime())
    }

    fun onGenerationFinished() {
        if (startedAt == 0L) return
        val now = SystemClock.elapsedRealtime()
        updateInferenceMetrics(now)
        sampleJob?.cancel()
        sampleJob = null
        _metrics.update { readDeviceMetrics(it.copy(isGenerating = false), now) }
    }

    fun close() {
        scope.cancel()
    }

    private fun startSampling() {
        sampleJob?.cancel()
        sampleJob = scope.launch {
            while (isActive) {
                delay(SAMPLE_INTERVAL_MS)
                val now = SystemClock.elapsedRealtime()
                _metrics.update { readDeviceMetrics(it, now) }
                updateInferenceMetrics(now)
            }
        }
    }

    private fun updateInferenceMetrics(now: Long) {
        val elapsed = (now - startedAt).coerceAtLeast(0L)
        val generationElapsed = if (firstOutputAt == 0L) 0L else now - firstOutputAt
        val estimatedTokens = estimateTokenCount(outputCharacters)
        _metrics.update {
            it.copy(
                timeToFirstOutputMs = firstOutputAt.takeIf { time -> time > 0L }
                    ?.minus(startedAt),
                elapsedMs = elapsed,
                estimatedTokens = estimatedTokens,
                estimatedTokensPerSecond = if (generationElapsed > 0L) {
                    estimatedTokens * 1_000.0 / generationElapsed
                } else {
                    0.0
                },
            )
        }
    }

    private fun readDeviceMetrics(
        current: PerformanceMetrics,
        now: Long = SystemClock.elapsedRealtime(),
    ): PerformanceMetrics {
        val cpuTime = Process.getElapsedCpuTime()
        val sampleDuration = now - previousSampleAt
        val cpuDuration = cpuTime - previousCpuTime
        previousSampleAt = now
        previousCpuTime = cpuTime

        val cpuPercent = if (sampleDuration > 0L) {
            val cores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
            (cpuDuration * 100.0 / sampleDuration / cores).roundToInt().coerceIn(0, 100)
        } else {
            current.cpuPercent
        }

        val memoryMb = activityManager
            .getProcessMemoryInfo(intArrayOf(Process.myPid()))
            .firstOrNull()
            ?.totalPss
            ?.div(1_024)
            ?: current.memoryMb

        val headroom = runCatching { powerManager.getThermalHeadroom(0) }
            .getOrNull()
            ?.takeIf(Float::isFinite)

        return current.copy(
            cpuPercent = cpuPercent,
            memoryMb = memoryMb,
            peakMemoryMb = maxOf(current.peakMemoryMb, memoryMb),
            thermalStatus = powerManager.currentThermalStatus.toThermalStatus(),
            thermalHeadroom = headroom,
        )
    }

    private fun Int.toThermalStatus() = when (this) {
        PowerManager.THERMAL_STATUS_LIGHT -> ThermalStatus.Light
        PowerManager.THERMAL_STATUS_MODERATE -> ThermalStatus.Moderate
        PowerManager.THERMAL_STATUS_SEVERE -> ThermalStatus.Severe
        PowerManager.THERMAL_STATUS_CRITICAL -> ThermalStatus.Critical
        PowerManager.THERMAL_STATUS_EMERGENCY -> ThermalStatus.Emergency
        PowerManager.THERMAL_STATUS_SHUTDOWN -> ThermalStatus.Shutdown
        else -> ThermalStatus.None
    }

    private companion object {
        const val SAMPLE_INTERVAL_MS = 1_000L
    }
}

internal fun estimateTokenCount(characterCount: Int): Int =
    if (characterCount == 0) 0 else ((characterCount + 3) / 4)
