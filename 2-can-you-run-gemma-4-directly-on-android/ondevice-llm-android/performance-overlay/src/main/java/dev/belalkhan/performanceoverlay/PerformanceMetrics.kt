package dev.belalkhan.performanceoverlay

data class PerformanceMetrics(
    val isGenerating: Boolean = false,
    val timeToFirstOutputMs: Long? = null,
    val elapsedMs: Long = 0,
    val estimatedTokens: Int = 0,
    val estimatedTokensPerSecond: Double = 0.0,
    val cpuPercent: Int = 0,
    val memoryMb: Int = 0,
    val peakMemoryMb: Int = 0,
    val thermalStatus: ThermalStatus = ThermalStatus.None,
    val thermalHeadroom: Float? = null,
)

enum class ThermalStatus(val label: String) {
    None("Nominal"),
    Light("Light"),
    Moderate("Moderate"),
    Severe("Severe"),
    Critical("Critical"),
    Emergency("Emergency"),
    Shutdown("Shutdown"),
}
