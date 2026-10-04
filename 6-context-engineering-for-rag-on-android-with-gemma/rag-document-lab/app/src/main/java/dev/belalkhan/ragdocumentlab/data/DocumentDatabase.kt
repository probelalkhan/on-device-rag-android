package dev.belalkhan.ragdocumentlab.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [StoredDocument::class, StoredChunk::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(EmbeddingConverter::class)
abstract class DocumentDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
}

