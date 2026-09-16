package dev.belalkhan.ragdocumentlab.document

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

data class ExtractedPdf(val name: String, val text: String)

class PdfTextExtractor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    init {
        PDFBoxResourceLoader.init(context)
    }

    fun extract(uri: Uri): ExtractedPdf {
        val name = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameColumn = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameColumn >= 0) cursor.getString(nameColumn) else null
        } ?: "document.pdf"

        val text = context.contentResolver.openInputStream(uri)?.use { input ->
            PDDocument.load(input).use { document -> PDFTextStripper().getText(document) }
        } ?: error("Could not open the selected PDF")

        return ExtractedPdf(name, clean(text))
    }

    private fun clean(text: String): String = text
        .replace("\r\n", "\n")
        .replace('\r', '\n')
        .replace("\u000C", "\n\n")
        .replace(Regex("[ \\t]+"), " ")
        .replace(Regex(" *\\n *"), "\n")
        .replace(Regex("\\n{3,}"), "\n\n")
        .trim()
}

