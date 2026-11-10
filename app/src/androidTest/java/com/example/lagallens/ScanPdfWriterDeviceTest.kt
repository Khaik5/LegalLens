package com.example.lagallens

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.lagallens.data.datasource.local.writeScanPdf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class ScanPdfWriterDeviceTest {
    @Test
    fun writesBothImagesInScanOrder() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val images = listOf(Color.RED, Color.BLUE).map { color ->
            File.createTempFile("scan-test-", ".png", context.cacheDir).apply {
                val bitmap = Bitmap.createBitmap(80, 120, Bitmap.Config.ARGB_8888)
                try {
                    bitmap.eraseColor(color)
                    outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                } finally {
                    bitmap.recycle()
                }
            }
        }
        val pdf = File.createTempFile("scan-test-", ".pdf", context.cacheDir)
        try {
            writeScanPdf(context.contentResolver, images.map { Uri.fromFile(it).toString() }, pdf)
            PdfRenderer(ParcelFileDescriptor.open(pdf, ParcelFileDescriptor.MODE_READ_ONLY)).use { renderer ->
                assertEquals(2, renderer.pageCount)
                listOf(Color.RED, Color.BLUE).forEachIndexed { index, color ->
                    renderer.openPage(index).use { page ->
                        val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                        try {
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            // Pages are JPEG-encoded, so allow small per-channel differences.
                            val pixel = bitmap.getPixel(page.width / 2, page.height / 2)
                            assertTrue(abs(Color.red(pixel) - Color.red(color)) <= 8)
                            assertTrue(abs(Color.green(pixel) - Color.green(color)) <= 8)
                            assertTrue(abs(Color.blue(pixel) - Color.blue(color)) <= 8)
                        } finally {
                            bitmap.recycle()
                        }
                    }
                }
            }
        } finally {
            images.forEach { it.delete() }
            pdf.delete()
        }
    }
}
