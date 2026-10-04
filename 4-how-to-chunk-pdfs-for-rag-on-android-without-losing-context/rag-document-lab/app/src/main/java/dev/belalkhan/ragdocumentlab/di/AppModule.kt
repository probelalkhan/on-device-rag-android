package dev.belalkhan.ragdocumentlab.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.belalkhan.ragdocumentlab.data.DocumentDatabase
import dev.belalkhan.ragdocumentlab.embedding.EmbeddingGemmaEngine
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DocumentDatabase =
        Room.databaseBuilder(context, DocumentDatabase::class.java, "rag-documents.db").build()

    @Provides
    fun provideEmbeddingGemma(
        @ApplicationContext context: Context
    ): EmbeddingGemmaEngine = EmbeddingGemmaEngine(context)
}

