package dev.belalkhan.ragdocumentlab.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents ORDER BY indexedAt DESC")
    fun observeDocuments(): Flow<List<StoredDocument>>

    @Query("SELECT * FROM documents WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): StoredDocument?

    @Insert
    suspend fun insertDocument(document: StoredDocument): Long

    @Insert
    suspend fun insertChunks(chunks: List<StoredChunk>)

    @Delete
    suspend fun deleteDocument(document: StoredDocument)
}

