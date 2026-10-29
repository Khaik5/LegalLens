package com.example.lagallens.presentation.feature.upload

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UploadFileValidatorTest {
    @Test fun acceptsSupportedMimeTypes() {
        assertTrue(UploadFileValidator.isSupported(UploadFileValidator.PDF_MIME))
        assertTrue(UploadFileValidator.isSupported(UploadFileValidator.DOCX_MIME))
        assertTrue(UploadFileValidator.isSupported("image/jpeg"))
    }

    @Test fun rejectsUnsupportedMimeTypes() {
        assertFalse(UploadFileValidator.isSupported("application/zip"))
        assertFalse(UploadFileValidator.isSupported(""))
    }

    @Test fun demoLimitIsTwentyMiB() {
        assertTrue(UploadFileValidator.MAX_BYTES == 20L * 1024L * 1024L)
    }
}
