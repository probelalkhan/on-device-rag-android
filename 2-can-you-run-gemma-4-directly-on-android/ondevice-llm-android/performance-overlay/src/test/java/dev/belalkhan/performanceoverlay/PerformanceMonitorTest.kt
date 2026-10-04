package dev.belalkhan.performanceoverlay

import org.junit.Assert.assertEquals
import org.junit.Test

class PerformanceMonitorTest {
    @Test
    fun estimateTokens_usesTransparentFourCharacterEstimate() {
        assertEquals(0, estimateTokenCount(0))
        assertEquals(1, estimateTokenCount(1))
        assertEquals(1, estimateTokenCount(4))
        assertEquals(2, estimateTokenCount(5))
        assertEquals(25, estimateTokenCount(100))
    }
}
