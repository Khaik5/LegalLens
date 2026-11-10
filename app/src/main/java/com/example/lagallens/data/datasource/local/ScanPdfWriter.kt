package com.example.lagallens.data.datasource.local

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FilterOutputStream
import java.io.OutputStream
import java.util.Locale

/**
 * Writes scanned pages into one PDF, each page embedded as a JPEG (DCTDecode) image.
 * PdfDocument re-encodes bitmaps losslessly, which forced pages to be shrunk until text blurred;
 * embedding JPEG keeps roughly A4 at 300 dpi while staying well under [maxBytes].
 * Lower quality steps are used only when the document would otherwise exceed [maxBytes].
 */
internal suspend fun writeScanPdf(
    resolver: ContentResolver,
    pages: List<String>,
    target: File,
    maxBytes: Long = Long.MAX_VALUE
) {
    require(pages.isNotEmpty())
    for (step in QUALITY_STEPS) {
        val isLastStep = step == QUALITY_STEPS.last()
        val images = ArrayList<ScanImage>(pages.size)
        var totalBytes = 0L
        for (path in pages) {
            currentCoroutineContext().ensureActive()
            val image = encodePage(resolver, Uri.parse(path), step)
            images += image
            totalBytes += image.jpeg.size + PDF_OVERHEAD_PER_PAGE
            // Stop early so oversized steps do not keep every page in memory.
            if (totalBytes > maxBytes && !isLastStep) break
        }
        if (totalBytes <= maxBytes || isLastStep) {
            // The caller rejects a final result that is still above its limit.
            target.outputStream().buffered().use { writePdf(it, images) }
            return
        }
    }
}

private class ScanImage(val width: Int, val height: Int, val jpeg: ByteArray)

private class QualityStep(val maxEdge: Int, val jpegQuality: Int)

private fun encodePage(resolver: ContentResolver, uri: Uri, step: QualityStep): ScanImage {
    // ImageDecoder applies the EXIF orientation written by the camera.
    val bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri)) { decoder, info, _ ->
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        val longestSide = maxOf(info.size.width, info.size.height)
        if (longestSide > step.maxEdge) {
            val scale = step.maxEdge.toFloat() / longestSide
            decoder.setTargetSize(
                (info.size.width * scale).toInt().coerceAtLeast(1),
                (info.size.height * scale).toInt().coerceAtLeast(1)
            )
        }
    }
    try {
        val bytes = ByteArrayOutputStream()
        check(bitmap.compress(Bitmap.CompressFormat.JPEG, step.jpegQuality, bytes)) { "JPEG encoding failed" }
        return ScanImage(bitmap.width, bitmap.height, bytes.toByteArray())
    } finally {
        bitmap.recycle()
    }
}

private fun writePdf(output: OutputStream, images: List<ScanImage>) {
    val out = CountingOutputStream(output)
    val offsets = mutableListOf<Long>()
    fun beginObject(): Int {
        offsets += out.count
        val number = offsets.size
        out.ascii("$number 0 obj\n")
        return number
    }

    out.ascii("%PDF-1.4\n")
    out.write(byteArrayOf('%'.code.toByte(), 0xE2.toByte(), 0xE3.toByte(), 0xCF.toByte(), 0xD3.toByte(), '\n'.code.toByte()))

    // Objects: 1 catalog, 2 page tree, then (page, image, content) for each page.
    val pageObjects = images.indices.map { 3 + it * 3 }
    beginObject()
    out.ascii("<< /Type /Catalog /Pages 2 0 R >>\nendobj\n")
    beginObject()
    out.ascii("<< /Type /Pages /Kids [${pageObjects.joinToString(" ") { "$it 0 R" }}] /Count ${images.size} >>\nendobj\n")

    images.forEach { image ->
        val pageWidth = A4_WIDTH_POINTS
        val pageHeight = A4_WIDTH_POINTS * image.height / image.width
        val width = "%.2f".format(Locale.US, pageWidth)
        val height = "%.2f".format(Locale.US, pageHeight)
        val pageNumber = beginObject()
        out.ascii(
            "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 $width $height] " +
                "/Resources << /XObject << /Im0 ${pageNumber + 1} 0 R >> >> /Contents ${pageNumber + 2} 0 R >>\nendobj\n"
        )
        beginObject()
        out.ascii(
            "<< /Type /XObject /Subtype /Image /Width ${image.width} /Height ${image.height} " +
                "/ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode /Length ${image.jpeg.size} >>\nstream\n"
        )
        out.write(image.jpeg)
        out.ascii("\nendstream\nendobj\n")
        val content = "q $width 0 0 $height 0 0 cm /Im0 Do Q"
        beginObject()
        out.ascii("<< /Length ${content.length} >>\nstream\n$content\nendstream\nendobj\n")
    }

    val xrefOffset = out.count
    out.ascii("xref\n0 ${offsets.size + 1}\n0000000000 65535 f \n")
    offsets.forEach { out.ascii("%010d 00000 n \n".format(Locale.US, it)) }
    out.ascii("trailer\n<< /Size ${offsets.size + 1} /Root 1 0 R >>\nstartxref\n$xrefOffset\n%%EOF\n")
    out.flush()
}

private class CountingOutputStream(output: OutputStream) : FilterOutputStream(output) {
    var count = 0L
        private set

    override fun write(b: Int) {
        out.write(b)
        count++
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        out.write(b, off, len)
        count += len
    }

    fun ascii(text: String) = write(text.toByteArray(Charsets.US_ASCII))
}

/** A4 width in PDF points; the page height follows the photo's aspect ratio. */
private const val A4_WIDTH_POINTS = 595f
private const val PDF_OVERHEAD_PER_PAGE = 1024L

/** First step is about A4 at 300 dpi; later steps only apply when the scan would exceed the upload limit. */
private val QUALITY_STEPS = listOf(
    QualityStep(maxEdge = 3508, jpegQuality = 90),
    QualityStep(maxEdge = 2800, jpegQuality = 85),
    QualityStep(maxEdge = 2200, jpegQuality = 80),
    QualityStep(maxEdge = 1600, jpegQuality = 75)
)
