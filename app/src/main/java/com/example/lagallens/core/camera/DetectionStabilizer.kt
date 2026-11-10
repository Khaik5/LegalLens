package com.example.lagallens.core.camera

import com.example.lagallens.domain.model.camera.DocumentDetection
import kotlin.math.abs

/**
 * Smooths per-frame document detection for the live preview.
 *
 * Detection on a single frame flickers: a bent page or a passing shadow can turn one frame Partial, and the
 * quadrilateral jitters a little even when the phone is held still. This keeps a detected page until it has
 * been missing for several frames in a row, and only reports movement when the page moves away from its
 * recent average position for more than one frame.
 */
class DetectionStabilizer(
    private val historySize: Int = 5,
    private val framesToLose: Int = 3,
    private val shakyFramesToReport: Int = 2,
    private val centerTolerance: Double = 0.06,
    private val areaTolerance: Double = 0.12
) {
    private val history = ArrayDeque<DocumentDetection.Ready>()
    private var missedFrames = 0
    private var shakyFrames = 0

    /** True while the page is visibly moving; used to warn about a possibly shaken photo. */
    var isShaking = false
        private set

    fun update(detection: DocumentDetection): DocumentDetection {
        if (detection !is DocumentDetection.Ready) {
            missedFrames += 1
            if (history.isNotEmpty() && missedFrames < framesToLose) return if (isShaking) DocumentDetection.Unstable else history.last()
            reset()
            return detection
        }
        missedFrames = 0
        val moved = history.isNotEmpty() && movedFromAverage(detection)
        shakyFrames = if (moved) shakyFrames + 1 else 0
        isShaking = shakyFrames >= shakyFramesToReport
        history.addLast(detection)
        while (history.size > historySize) history.removeFirst()
        return if (isShaking) DocumentDetection.Unstable else detection
    }

    fun reset() {
        history.clear()
        missedFrames = 0
        shakyFrames = 0
        isShaking = false
    }

    private fun movedFromAverage(detection: DocumentDetection.Ready): Boolean {
        val centerX = history.sumOf { it.centerX } / history.size
        val centerY = history.sumOf { it.centerY } / history.size
        val area = history.sumOf { it.areaRatio } / history.size
        return abs(detection.centerX - centerX) > centerTolerance ||
            abs(detection.centerY - centerY) > centerTolerance ||
            abs(detection.areaRatio - area) > areaTolerance
    }
}
