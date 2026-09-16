package dev.belalkhan.ragdocumentlab.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "documents",
    indices = [Index(value = ["name"], unique = true)]
)
data class StoredDocument(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val chunkCount: Int,
    val indexedAt: Long = System.currentTimeMillis()
)

