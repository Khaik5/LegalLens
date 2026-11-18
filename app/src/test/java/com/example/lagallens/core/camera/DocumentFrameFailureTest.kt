package com.example.lagallens.core.camera

import androidx.camera.core.ImageProxy
import com.example.lagallens.domain.model.camera.DocumentDetection
import com.example.lagallens.presentation.feature.camera.capture.ui.analyzeAndCloseCameraFrame
import java.lang.reflect.Proxy
import org.junit.Assert.assertEquals
import org.junit.Test

class DocumentFrameFailureTest {
    @Test fun captureClosesAFrameExactlyOnceWhenDetectionThrows() {
        var closes = 0
        val image = frame { closes++ }
        val failure = IllegalStateException("conversion failed")
        val result = runCatching { analyzeAndCloseCameraFrame(image) { throw failure } }
        org.junit.Assert.assertSame(failure, result.exceptionOrNull())
        assertEquals(1, closes)
    }

    @Test fun captureClosesAFrameExactlyOnceWhenDetectionSucceeds() {
        var closes = 0
        val image = frame { closes++ }
        assertEquals(DocumentDetection.NotFound, analyzeAndCloseCameraFrame(image) { DocumentDetection.NotFound })
        assertEquals(1, closes)
    }

    private fun frame(onClose: () -> Unit): ImageProxy = Proxy.newProxyInstance(
        ImageProxy::class.java.classLoader, arrayOf(ImageProxy::class.java)
    ) { _, method, _ ->
        if (method.name == "close") { onClose(); null }
        else throw IllegalStateException("unreadable frame")
    } as ImageProxy

    @Test fun unreadableFrameProducesUnavailableInsteadOfBreakingTheAnalyzer() {
        var closes = 0
        val image = Proxy.newProxyInstance(ImageProxy::class.java.classLoader, arrayOf(ImageProxy::class.java)) { _, method, _ ->
            if (method.name == "close") { closes++; null }
            else throw IllegalStateException("unreadable frame")
        } as ImageProxy
        assertEquals(DocumentDetection.Unavailable, DocumentVisionAnalyzer.detect(image))
        // The capture Activity owns the ImageProxy; the detector only owns its Mats.
        assertEquals(0, closes)
    }
}
