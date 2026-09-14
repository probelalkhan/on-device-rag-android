package dev.belalkhan.ragdocumentlab.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chunks",
    foreignKeys = [ForeignKey(
        entity = StoredDocument::class,
        parentColumns = ["id"],
        childColumns = ["documentId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("documentId")]
)
data class StoredChunk(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val documentId: Long,
    val chunkIndex: Int,
    val text: String,
    val embedding: FloatArray
)

