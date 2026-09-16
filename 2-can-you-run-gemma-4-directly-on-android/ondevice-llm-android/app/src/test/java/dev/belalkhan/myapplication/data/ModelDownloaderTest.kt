package dev.belalkhan.myapplication.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ModelDownloaderTest {
    @Test
    fun parsesContentRange() {
        assertEquals(
            ContentRange(start = 100, end = 499, total = 500),
            parseContentRange("bytes 100-499/500"),
        )
    }

    @Test
    fun rejectsInvalidContentRange() {
        assertNull(parseContentRange("bytes 100-500/500"))
        assertNull(parseContentRange("invalid"))
        assertNull(parseContentRange(null))
    }

    @Test
    fun parsesUnsatisfiedRange() {
        assertEquals(500L, parseUnsatisfiedRange("bytes */500"))
        assertNull(parseUnsatisfiedRange("bytes */0"))
        assertNull(parseUnsatisfiedRange(null))
    }
}
