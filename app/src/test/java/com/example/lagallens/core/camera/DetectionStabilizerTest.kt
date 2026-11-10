package com.example.lagallens.core.camera

import com.example.lagallens.domain.model.camera.DocumentDetection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DetectionStabilizerTest {
    private val stabilizer = DetectionStabilizer()
    private fun ready(x: Double = 0.5, y: Double = 0.5, area: Double = 0.5) = DocumentDetection.Ready(area, x, y)

    @Test fun smallJitterStaysReady() {
        listOf(0.50, 0.53, 0.48, 0.52, 0.49).forEach { x ->
            assertTrue(stabilizer.update(ready(x = x)) is DocumentDetection.Ready)
        }
        assertFalse(stabilizer.isShaking)
    }

    @Test fun briefMissKeepsTheLastReadyFrame() {
        stabilizer.update(ready())
        val first = stabilizer.update(DocumentDetection.Partial(0.4))
        val second = stabilizer.update(DocumentDetection.NotFound)

        assertEquals(ready(), first)
        assertEquals(ready(), second)
    }

    @Test fun pageLostForSeveralFramesIsReported() {
        stabilizer.update(ready())
        repeat(2) { stabilizer.update(DocumentDetection.Partial(0.4)) }

        assertEquals(DocumentDetection.Partial(0.4), stabilizer.update(DocumentDetection.Partial(0.4)))
    }

    @Test fun oneOutlierFrameIsNotShakeButSustainedMovementIs() {
        repeat(3) { stabilizer.update(ready()) }

        assertTrue(stabilizer.update(ready(x = 0.65)) is DocumentDetection.Ready)
        assertFalse(stabilizer.isShaking)

        assertEquals(DocumentDetection.Unstable, stabilizer.update(ready(x = 0.75)))
        assertTrue(stabilizer.isShaking)
    }
}
