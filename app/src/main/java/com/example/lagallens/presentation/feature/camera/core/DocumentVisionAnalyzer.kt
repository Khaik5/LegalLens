package com.example.lagallens.presentation.feature.camera.core

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.camera.core.ImageProxy
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfDouble
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.imgproc.Imgproc
import java.io.File
import kotlin.math.max

object DocumentVisionAnalyzer {
    fun detect(image: ImageProxy): DocumentDetection {
        val gray = image.toGrayMat()
        return try {
            detect(gray)
        } finally {
            gray.release()
        }
    }

    fun checkQuality(file: File, shakeRisk: Boolean): DocumentQualityResult = runCatching {
        evaluateQuality(file, shakeRisk)
    }.getOrElse {
        DocumentQualityResult(QualityDecision.REJECTED, listOf(QualityIssue.DOCUMENT_NOT_FOUND))
    }

    private fun evaluateQuality(file: File, shakeRisk: Boolean): DocumentQualityResult {
        val bitmap = loadQualityBitmap(file) ?: return DocumentQualityResult(
            QualityDecision.REJECTED,
            listOf(QualityIssue.DOCUMENT_NOT_FOUND)
        )
        val rgba = Mat()
        val gray = Mat()
        return try {
            Utils.bitmapToMat(bitmap, rgba)
            Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGBA2GRAY)
            val issues = mutableListOf<QualityIssue>()
            val detection = detect(gray)
            if (blurScore(gray) < 90.0) issues += QualityIssue.BLUR
            if (isPoorlyExposed(gray)) issues += QualityIssue.EXPOSURE
            if (shakeRisk) issues += QualityIssue.SHAKE
            when (detection) {
                is DocumentDetection.Ready -> {
                    if (detection.areaRatio < 0.28) issues += QualityIssue.DOCUMENT_SMALL
                    if (isCropRisk(gray) || isTextCutOff(bitmap)) issues += QualityIssue.CROP_RISK
                }
                is DocumentDetection.Partial -> {
                    issues += QualityIssue.DOCUMENT_SMALL
                    issues += QualityIssue.CROP_RISK
                }
                DocumentDetection.Unstable -> issues += QualityIssue.SHAKE
                DocumentDetection.NotFound,
                DocumentDetection.Unavailable -> issues += QualityIssue.DOCUMENT_NOT_FOUND
            }
            val decision = when {
                QualityIssue.DOCUMENT_NOT_FOUND in issues || QualityIssue.CROP_RISK in issues -> {
                    QualityDecision.REJECTED
                }
                issues.isEmpty() -> QualityDecision.ACCEPTED
                else -> QualityDecision.WARNING
            }
            DocumentQualityResult(decision, issues.distinct())
        } finally {
            gray.release()
            rgba.release()
            bitmap.recycle()
        }
    }

    private fun detect(gray: Mat): DocumentDetection {
        val blurred = Mat()
        val edges = Mat()
        val contours = mutableListOf<MatOfPoint>()
        val hierarchy = Mat()
        return try {
            Imgproc.GaussianBlur(gray, blurred, org.opencv.core.Size(5.0, 5.0), 0.0)
            Imgproc.Canny(blurred, edges, 60.0, 160.0)
            Imgproc.findContours(edges, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)
            val imageArea = gray.rows().toDouble() * gray.cols().toDouble()
            val largest = contours.maxByOrNull { Imgproc.contourArea(it) }
                ?: return DocumentDetection.NotFound
            val contourArea = Imgproc.contourArea(largest)
            val ratio = contourArea / imageArea
            if (ratio < 0.08) return DocumentDetection.NotFound
            val curve = MatOfPoint2f(*largest.toArray())
            val approximation = MatOfPoint2f()
            try {
                Imgproc.approxPolyDP(curve, approximation, Imgproc.arcLength(curve, true) * 0.02, true)
                val points = approximation.toArray()
                if (points.size != 4 || !Imgproc.isContourConvex(MatOfPoint(*points))) {
                    return DocumentDetection.Partial(ratio)
                }
                val centerX = points.map { it.x }.average() / gray.cols()
                val centerY = points.map { it.y }.average() / gray.rows()
                val isInsidePreview = points.all {
                    it.x >= 0 && it.x <= gray.cols() && it.y >= 0 && it.y <= gray.rows()
                }
                if (!isInsidePreview) DocumentDetection.Partial(ratio) else {
                    DocumentDetection.Ready(ratio, centerX, centerY)
                }
            } finally {
                approximation.release()
                curve.release()
            }
        } catch (_: Throwable) {
            DocumentDetection.Unavailable
        } finally {
            contours.forEach { contour -> contour.release() }
            hierarchy.release()
            edges.release()
            blurred.release()
        }
    }

    private fun blurScore(gray: Mat): Double {
        val laplacian = Mat()
        val mean = MatOfDouble()
        val stdDev = MatOfDouble()
        return try {
            Imgproc.Laplacian(gray, laplacian, CvType.CV_64F)
            Core.meanStdDev(laplacian, mean, stdDev)
            val value = stdDev.get(0, 0)[0]
            value * value
        } finally {
            stdDev.release()
            mean.release()
            laplacian.release()
        }
    }

    private fun isPoorlyExposed(gray: Mat): Boolean {
        val mean = Core.mean(gray).`val`[0]
        val dark = Mat()
        val bright = Mat()
        return try {
            Imgproc.threshold(gray, dark, 30.0, 255.0, Imgproc.THRESH_BINARY_INV)
            Imgproc.threshold(gray, bright, 225.0, 255.0, Imgproc.THRESH_BINARY)
            val pixels = gray.rows().toDouble() * gray.cols().toDouble()
            val darkRatio = Core.countNonZero(dark) / pixels
            val brightRatio = Core.countNonZero(bright) / pixels
            mean < 55.0 || mean > 210.0 || max(darkRatio, brightRatio) > 0.72
        } finally {
            bright.release()
            dark.release()
        }
    }

    private fun isCropRisk(gray: Mat): Boolean {
        val marginX = (gray.cols() * 0.025).toInt()
        val marginY = (gray.rows() * 0.025).toInt()
        val border = Mat()
        return try {
            Imgproc.Canny(gray, border, 70.0, 180.0)
            val top = border.submat(0, marginY, 0, border.cols())
            val bottom = border.submat(border.rows() - marginY, border.rows(), 0, border.cols())
            val left = border.submat(0, border.rows(), 0, marginX)
            val right = border.submat(0, border.rows(), border.cols() - marginX, border.cols())
            try {
                val edgeDensity = listOf(top, bottom, left, right).sumOf { Core.countNonZero(it) }
                edgeDensity > border.rows() + border.cols()
            } finally {
                right.release()
                left.release()
                bottom.release()
                top.release()
            }
        } finally {
            border.release()
        }
    }

    private fun isTextCutOff(bitmap: Bitmap): Boolean {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        return try {
            val text = Tasks.await(recognizer.process(InputImage.fromBitmap(bitmap, 0)))
            val marginX = (bitmap.width * 0.025).toInt()
            val marginY = (bitmap.height * 0.025).toInt()
            text.textBlocks.any { block ->
                block.boundingBox?.let { bounds ->
                    bounds.left <= marginX ||
                        bounds.top <= marginY ||
                        bounds.right >= bitmap.width - marginX ||
                        bounds.bottom >= bitmap.height - marginY
                } == true
            }
        } catch (_: Throwable) {
            false
        } finally {
            recognizer.close()
        }
    }

    private fun loadQualityBitmap(file: File): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        val maxDimension = max(bounds.outWidth, bounds.outHeight)
        var sampleSize = 1
        while (maxDimension / sampleSize > 1920) sampleSize *= 2
        return BitmapFactory.decodeFile(
            file.path,
            BitmapFactory.Options().apply { inSampleSize = sampleSize }
        )
    }

    private fun ImageProxy.toGrayMat(): Mat {
        val plane = planes.first()
        val data = ByteArray(width * height)
        val buffer = plane.buffer.duplicate()
        for (row in 0 until height) {
            buffer.position(row * plane.rowStride)
            buffer.get(data, row * width, width)
        }
        return Mat(height, width, CvType.CV_8UC1).apply { put(0, 0, data) }
    }
}
