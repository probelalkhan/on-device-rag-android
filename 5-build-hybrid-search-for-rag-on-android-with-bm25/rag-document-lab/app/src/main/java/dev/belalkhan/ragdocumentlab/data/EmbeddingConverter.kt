package dev.belalkhan.ragdocumentlab.data

import androidx.room.TypeConverter
import java.nio.ByteBuffer
import java.nio.ByteOrder

class EmbeddingConverter {
    @TypeConverter
    fun toBytes(values: FloatArray): ByteArray =
        ByteBuffer.allocate(values.size * Float.SIZE_BYTES)
            .order(ByteOrder.LITTLE_ENDIAN)
            .apply { values.forEach(::putFloat) }
            .array()

    @TypeConverter
    fun toFloats(bytes: ByteArray): FloatArray {
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        return FloatArray(bytes.size / Float.SIZE_BYTES) { buffer.float }
    }
}

