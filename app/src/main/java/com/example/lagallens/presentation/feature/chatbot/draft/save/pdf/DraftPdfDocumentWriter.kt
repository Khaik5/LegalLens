package com.example.lagallens.presentation.feature.chatbot.draft.save.pdf

import android.content.Context
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.res.ResourcesCompat
import com.example.lagallens.R
import java.io.File
import java.io.FileOutputStream

internal object DraftPdfDocumentWriter {
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val PAGE_MARGIN = 48f

    fun generate(context: Context, contractName: String, content: String): File {
        require(content.isNotBlank())
        val directory = File(context.cacheDir, "draft-pdfs")
        check(directory.isDirectory || directory.mkdirs())
        val output = File.createTempFile("draft-d2-", ".pdf", directory)
        try {
            val document = PdfDocument()
            try {
                val flow = PdfFlow(context, document)
                flow.startPage()
                flow.drawText(contractName, 21f, context.getColor(R.color.legal_lens_primary), true, 28f)
                flow.drawText(context.getString(R.string.draft_pdf_document_metadata), 11f, context.getColor(R.color.legal_lens_secondary_text), false, 16f)
                flow.drawDivider()
                flow.drawNotice(context.getString(R.string.draft_pdf_warning))
                flow.drawText(content, 11f, context.getColor(R.color.legal_lens_status_text), false, 16f)
                flow.drawNotice(context.getString(R.string.draft_save_legal_warning))
                flow.finishPage()
                FileOutputStream(output).use(document::writeTo)
            } finally {
                document.close()
            }
            return output
        } catch (exception: Exception) {
            output.delete()
            throw exception
        }
    }

    fun copyToUri(context: Context, source: File, destination: Uri) {
        check(source.isFile && source.length() > 0L)
        val output = context.contentResolver.openOutputStream(destination, "w") ?: error("Unable to open the selected PDF destination")
        output.use { destinationStream -> source.inputStream().use { input -> input.copyTo(destinationStream) } }
    }

    private class PdfFlow(private val context: Context, private val document: PdfDocument) {
        private val regular = ResourcesCompat.getFont(context, R.font.manrope) ?: Typeface.create("sans-serif", Typeface.NORMAL)
        private val bold = Typeface.create(regular, Typeface.BOLD)
        private var page: PdfDocument.Page? = null
        private var pageNumber = 0
        private var y = 0f

        fun startPage() {
            pageNumber += 1
            val startedPage = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
            page = startedPage
            startedPage.canvas.drawColor(Color.WHITE)
            val headerPaint = paint(9f, context.getColor(R.color.legal_lens_auth_accent), true)
            startedPage.canvas.drawText(context.getString(R.string.draft_pdf_page_header), PAGE_MARGIN, 36f, headerPaint)
            y = 72f
        }

        fun finishPage() {
            val canvas = page?.canvas ?: return
            val footerY = PAGE_HEIGHT - 32f
            canvas.drawLine(PAGE_MARGIN, footerY - 14f, PAGE_WIDTH - PAGE_MARGIN, footerY - 14f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = context.getColor(R.color.legal_lens_input_stroke)
                strokeWidth = 1f
            })
            canvas.drawText(context.getString(R.string.draft_pdf_page_number, pageNumber), PAGE_MARGIN, footerY, paint(9f, context.getColor(R.color.legal_lens_secondary_text), false))
            document.finishPage(page)
            page = null
        }

        fun drawText(value: String, size: Float, color: Int, isBold: Boolean, lineHeight: Float) {
            val activePage = page ?: return
            val textPaint = paint(size, color, isBold)
            value.split('\n').forEach { paragraph ->
                if (paragraph.isBlank()) {
                    ensureSpace(lineHeight)
                    y += lineHeight
                } else {
                    wrap(paragraph, textPaint, PAGE_WIDTH - 2f * PAGE_MARGIN).forEach { line ->
                        ensureSpace(lineHeight)
                        (page ?: activePage).canvas.drawText(line, PAGE_MARGIN, y, textPaint)
                        y += lineHeight
                    }
                }
            }
            y += 5f
        }

        fun drawDivider() {
            ensureSpace(20f)
            page?.canvas?.drawLine(PAGE_MARGIN, y, PAGE_WIDTH - PAGE_MARGIN, y, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = context.getColor(R.color.legal_lens_input_stroke)
                strokeWidth = 1f
            })
            y += 17f
        }

        fun drawNotice(value: String) {
            val textPaint = paint(10f, context.getColor(R.color.legal_lens_secondary_text), false)
            val lines = wrap(value, textPaint, PAGE_WIDTH - 2f * PAGE_MARGIN - 28f)
            val height = lines.size * 15f + 20f
            ensureSpace(height + 12f)
            val bounds = RectF(PAGE_MARGIN, y, PAGE_WIDTH - PAGE_MARGIN, y + height)
            page?.canvas?.drawRoundRect(bounds, 10f, 10f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = context.getColor(R.color.chatbot_draft_notice_background)
            })
            var baseline = y + 20f
            lines.forEach { line ->
                page?.canvas?.drawText(line, PAGE_MARGIN + 14f, baseline, textPaint)
                baseline += 15f
            }
            y += height + 12f
        }

        private fun ensureSpace(height: Float) {
            if (y + height <= PAGE_HEIGHT - 62f) return
            finishPage()
            startPage()
        }

        private fun wrap(value: String, paint: Paint, width: Float): List<String> {
            val result = mutableListOf<String>()
            var line = StringBuilder()
            value.trim().split(Regex("\\s+")).forEach { word ->
                val candidate = if (line.isEmpty()) word else "$line $word"
                if (line.isNotEmpty() && paint.measureText(candidate) > width) {
                    result += line.toString()
                    line = StringBuilder()
                }
                val chunks = splitLongWord(word, paint, width)
                chunks.forEachIndexed { index, chunk ->
                    if (line.isNotEmpty() && paint.measureText("$line$chunk") > width) {
                        result += line.toString()
                        line = StringBuilder()
                    }
                    if (index > 0 && line.isNotEmpty()) {
                        result += line.toString()
                        line = StringBuilder()
                    }
                    line.append(chunk)
                    if (index < chunks.lastIndex) {
                        result += line.toString()
                        line = StringBuilder()
                    } else if (line.isNotEmpty()) {
                        line.append(' ')
                    }
                }
            }
            if (line.isNotEmpty()) result += line.toString().trimEnd()
            return result.ifEmpty { listOf("") }
        }

        private fun splitLongWord(word: String, paint: Paint, width: Float): List<String> {
            if (paint.measureText(word) <= width) return listOf(word)
            val chunks = mutableListOf<String>()
            var start = 0
            var end = 0
            while (end < word.length) {
                val next = word.offsetByCodePoints(end, 1)
                if (end > start && paint.measureText(word.substring(start, next)) > width) {
                    chunks += word.substring(start, end)
                    start = end
                }
                end = next
            }
            if (start < word.length) chunks += word.substring(start)
            return chunks
        }

        private fun paint(size: Float, color: Int, isBold: Boolean) = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            textSize = size
            this.color = color
            typeface = if (isBold) bold else regular
        }
    }
}
