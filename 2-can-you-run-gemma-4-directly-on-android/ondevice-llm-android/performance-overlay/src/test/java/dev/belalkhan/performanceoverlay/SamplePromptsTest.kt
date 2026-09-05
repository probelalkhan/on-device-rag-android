package dev.belalkhan.performanceoverlay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SamplePromptsTest {
    @Test
    fun everyPromptRequestsTheSameSustainedBenchmarkWorkload() {
        assertEquals(6, samplePrompts.size)
        samplePrompts.forEach { prompt ->
            assertTrue(prompt.contains("multiple explicit stages"))
            assertTrue(prompt.contains("2,000 to 2,500 words"))
        }
    }
}
