package com.example.lagallens.presentation.feature.chatbot.draft.save.pdf

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DraftPdfDocumentWriterDeviceTest {
    @Test
    fun writesSavedVietnameseContentAcrossPagesAndRejectsBlankContent() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val body = buildString {
            repeat(180) { index ->
                append("ĐIỀU ${index + 1}. TIỀN LƯƠNG VÀ QUYỀN LỢI\n")
                append("Người lao động nhận mức lương thỏa thuận, phụ cấp và quyền lợi theo nội dung đã lưu. ")
                append("Đây là văn bản tiếng Việt dài để kiểm tra xuống dòng và phân trang.\n\n")
            }
        }
        val file = DraftPdfDocumentWriter.generate(context, "Hợp đồng lao động DEMO", body)
        try {
            assertTrue(file.isFile)
            assertTrue(file.length() > 0L)
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                PdfRenderer(descriptor).use { pdf ->
                    assertTrue(pdf.pageCount > 1)
                    val page = pdf.openPage(0)
                    val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    assertFalse(bitmap.isRecycled)
                    bitmap.recycle()
                    page.close()
                }
            }
        } finally {
            file.delete()
        }

        var rejected = false
        try {
            DraftPdfDocumentWriter.generate(context, "Bản nháp rỗng", "  \n ")
        } catch (_: IllegalArgumentException) {
            rejected = true
        }
        assertTrue(rejected)
    }
}
