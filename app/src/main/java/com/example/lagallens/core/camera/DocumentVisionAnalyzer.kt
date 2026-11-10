package com.example.lagallens.core.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.camera.core.ImageProxy
import com.example.lagallens.domain.model.camera.DocumentDetection
import com.example.lagallens.domain.model.camera.DocumentQualityResult
import com.example.lagallens.domain.model.camera.QualityDecision
import com.example.lagallens.domain.model.camera.QualityIssue
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

    /** Detects the document in a decoded image, as used for still photos and gallery pages. */
    fun detect(bitmap: Bitmap): DocumentDetection {
        val rgba = Mat()
        val gray = Mat()
        return try {
            Utils.bitmapToMat(bitmap, rgba)
            Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGBA2GRAY)
            detect(gray)
        } catch (_: Throwable) {
            DocumentDetection.Unavailable
        } finally {
            gray.release()
            rgba.release()
        }
    }

    /**
     * Only an unreadable image is rejected. Blur, bad exposure or a missing document need the user's decision
     * ([QualityDecision.WARNING]); framing hints such as a small or cut-off page, or a shaky preview, are kept on
     * an accepted page so the user is not interrupted for a readable photo.
     *
     * [framedByPreview] is true when the live preview showed a stable document at the moment of capture; the
     * framing was then already confirmed, so only the photo's sharpness and exposure are checked.
     */
    fun checkQuality(file: File, shakeRisk: Boolean, framedByPreview: Boolean = false): DocumentQualityResult = runCatching {
        evaluateQuality(file, shakeRisk, framedByPreview)
    }.getOrElse {
        DocumentQualityResult(QualityDecision.WARNING, listOf(QualityIssue.DOCUMENT_NOT_FOUND))
    }

    private fun evaluateQuality(file: File, shakeRisk: Boolean, framedByPreview: Boolean): DocumentQualityResult {
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
            if (blurScore(gray) < BLUR_THRESHOLD) issues += QualityIssue.BLUR
            if (isPoorlyExposed(gray)) issues += QualityIssue.EXPOSURE
            if (shakeRisk) issues += QualityIssue.SHAKE
            if (!framedByPreview) {
                when (val detection = detect(gray)) {
                    // A quadrilateral clear of the image border is already fully inside the photo.
                    is DocumentDetection.Ready -> if (detection.areaRatio < MIN_DOCUMENT_RATIO) issues += QualityIssue.DOCUMENT_SMALL
                    is DocumentDetection.Partial -> {
                        if (detection.areaRatio < MIN_DOCUMENT_RATIO) issues += QualityIssue.DOCUMENT_SMALL
                        if (isCropRisk(gray) || isTextCutOff(bitmap)) issues += QualityIssue.CROP_RISK
                    }
                    DocumentDetection.Unstable -> issues += QualityIssue.SHAKE
                    DocumentDetection.NotFound,
                    DocumentDetection.Unavailable -> issues += QualityIssue.DOCUMENT_NOT_FOUND
                }
            }
            val decision = if (issues.any { it in BLOCKING_ISSUES }) QualityDecision.WARNING else QualityDecision.ACCEPTED
            DocumentQualityResult(decision, issues.distinct())
        } finally {
            gray.release()
            rgba.release()
            bitmap.recycle()
        }
    }

    /**
     * Finds the page as the largest convex quadrilateral. Two masks are tried, because each fails where the other
     * works: a brightness mask (a light page on a darker surface, even with ruled lines or handwriting inside) and an
     * edge mask with thresholds that follow the image brightness (a page on a light or patterned surface).
     */
    private fun detect(gray: Mat): DocumentDetection {
        val small = Mat()
        val blurred = Mat()
        return try {
            val scale = DETECTION_SIZE / max(gray.cols(), gray.rows()).toDouble()
            if (scale < 1.0) Imgproc.resize(gray, small, org.opencv.core.Size(), scale, scale, Imgproc.INTER_AREA) else gray.copyTo(small)
            Imgproc.GaussianBlur(small, blurred, org.opencv.core.Size(5.0, 5.0), 0.0)
            val candidates = listOfNotNull(bestQuad(brightnessMask(blurred), small), bestQuad(edgeMask(blurred), small))
            val ready = candidates.filterIsInstance<DocumentDetection.Ready>().maxByOrNull { it.areaRatio }
            ready ?: candidates.filterIsInstance<DocumentDetection.Partial>().maxByOrNull { it.areaRatio } ?: DocumentDetection.NotFound
        } catch (_: Throwable) {
            DocumentDetection.Unavailable
        } finally {
            blurred.release()
            small.release()
        }
    }

    private fun brightnessMask(blurred: Mat): Mat {
        val mask = Mat()
        Imgproc.threshold(blurred, mask, 0.0, 255.0, Imgproc.THRESH_BINARY or Imgproc.THRESH_OTSU)
        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, org.opencv.core.Size(9.0, 9.0))
        Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_CLOSE, kernel)
        kernel.release()
        return mask
    }

    private fun edgeMask(blurred: Mat): Mat {
        val median = medianIntensity(blurred)
        val edges = Mat()
        Imgproc.Canny(blurred, edges, max(0.0, 0.66 * median), kotlin.math.min(255.0, 1.33 * median))
        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, org.opencv.core.Size(3.0, 3.0))
        Imgproc.dilate(edges, edges, kernel, org.opencv.core.Point(-1.0, -1.0), 2)
        kernel.release()
        return edges
    }

    /** Releases [mask]. Returns null when no region is large enough to be a page. */
    private fun bestQuad(mask: Mat, image: Mat): DocumentDetection? {
        val contours = mutableListOf<MatOfPoint>()
        val hierarchy = Mat()
        return try {
            Imgproc.findContours(mask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
            val imageArea = image.rows().toDouble() * image.cols().toDouble()
            val margin = max(image.cols(), image.rows()) * BORDER_MARGIN
            var partial: DocumentDetection.Partial? = null
            for (contour in contours.sortedByDescending { Imgproc.contourArea(it) }.take(CANDIDATES)) {
                val ratio = Imgproc.contourArea(contour) / imageArea
                if (ratio < MIN_AREA_RATIO) break
                val points = quadrilateral(contour)
                if (points == null) {
                    if (partial == null) partial = DocumentDetection.Partial(ratio)
                    continue
                }
                val quad = MatOfPoint(*points)
                val quadRatio = Imgproc.contourArea(quad) / imageArea
                quad.release()
                val touchesBorder = points.any {
                    it.x <= margin || it.y <= margin || it.x >= image.cols() - 1 - margin || it.y >= image.rows() - 1 - margin
                }
                if (touchesBorder) {
                    if (partial == null) partial = DocumentDetection.Partial(quadRatio)
                    continue
                }
                return DocumentDetection.Ready(
                    quadRatio,
                    points.map { it.x }.average() / image.cols(),
                    points.map { it.y }.average() / image.rows()
                )
            }
            partial
        } finally {
            contours.forEach { it.release() }
            hierarchy.release()
            mask.release()
        }
    }

    /** The contour's convex hull simplified to four corners, or null when it is not a quadrilateral. */
    private fun quadrilateral(contour: MatOfPoint): Array<org.opencv.core.Point>? {
        val hullIndices = org.opencv.core.MatOfInt()
        val curve = MatOfPoint2f()
        val approximation = MatOfPoint2f()
        return try {
            Imgproc.convexHull(contour, hullIndices)
            val all = contour.toArray()
            curve.fromArray(*hullIndices.toArray().map { all[it] }.toTypedArray())
            val perimeter = Imgproc.arcLength(curve, true)
            QUAD_EPSILONS.firstNotNullOfOrNull { epsilon ->
                Imgproc.approxPolyDP(curve, approximation, perimeter * epsilon, true)
                approximation.toArray().takeIf { it.size == 4 }
            }
        } finally {
            approximation.release()
            curve.release()
            hullIndices.release()
        }
    }

    private fun medianIntensity(gray: Mat): Double {
        val bytes = ByteArray(gray.rows() * gray.cols())
        val continuous = if (gray.isContinuous) gray else gray.clone()
        continuous.get(0, 0, bytes)
        if (continuous !== gray) continuous.release()
        val histogram = IntArray(256)
        bytes.forEach { histogram[it.toInt() and 0xFF]++ }
        var seen = 0
        histogram.forEachIndexed { value, count ->
            seen += count
            if (seen * 2 >= bytes.size) return value.toDouble()
        }
        return 128.0
    }

    private const val DETECTION_SIZE = 640.0
    private const val MIN_AREA_RATIO = 0.08
    private const val BORDER_MARGIN = 0.01
    private const val CANDIDATES = 5
    private const val BLUR_THRESHOLD = 90.0
    private const val MIN_DOCUMENT_RATIO = 0.20

    /** Simplification tolerances tried in order; a bent page or a covered corner needs a looser one. */
    private val QUAD_EPSILONS = listOf(0.02, 0.03, 0.04, 0.05)

    /** Issues that make the photo hard to read; the rest are framing hints that do not interrupt the user. */
    private val BLOCKING_ISSUES = setOf(QualityIssue.BLUR, QualityIssue.EXPOSURE, QualityIssue.DOCUMENT_NOT_FOUND)

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
            // A white page filling the frame is legitimately bright; only a dark or washed-out photo is flagged.
            mean < 50.0 || darkRatio > 0.72 || brightRatio > 0.92
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

    /** The luminance plane limited to [ImageProxy.getCropRect], which matches the preview when a ViewPort is set. */
    private fun ImageProxy.toGrayMat(): Mat {
        val plane = planes.first()
        val crop = android.graphics.Rect(cropRect).apply {
            if (!intersect(0, 0, this@toGrayMat.width, this@toGrayMat.height) || isEmpty) {
                set(0, 0, this@toGrayMat.width, this@toGrayMat.height)
            }
        }
        val cropWidth = crop.width()
        val cropHeight = crop.height()
        val data = ByteArray(cropWidth * cropHeight)
        val buffer = plane.buffer.duplicate()
        for (row in 0 until cropHeight) {
            buffer.position((crop.top + row) * plane.rowStride + crop.left * plane.pixelStride)
            buffer.get(data, row * cropWidth, cropWidth)
        }
        return Mat(cropHeight, cropWidth, CvType.CV_8UC1).apply { put(0, 0, data) }
    }
}
