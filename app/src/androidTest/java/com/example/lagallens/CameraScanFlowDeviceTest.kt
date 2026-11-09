package com.example.lagallens

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import com.example.lagallens.presentation.feature.camera.capture.ui.CameraCaptureActivity
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Scan flow without a physical document. On the emulator the camera sees a virtual scene, so no page is detected:
 * a manual shot must still be possible, warn about the missing document, and continue through pages and OCR.
 */
class CameraScanFlowDeviceTest {
    @get:Rule val demoSession = DemoSessionRule()
    private val app = ApplicationProvider.getApplicationContext<Context>().applicationContext as LegalLensApplication
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val directory = File(app.getExternalFilesDir(null), "camera-scan").apply { deleteRecursively(); mkdirs() }
    private var index = 0

    @Test fun manualShotWithoutDetectedDocumentReachesOcr() {
        instrumentation.uiAutomation.grantRuntimePermission(app.packageName, Manifest.permission.CAMERA)

        ActivityScenario.launch<CameraCaptureActivity>(CameraCaptureActivity.createIntent(app)).use { scenario ->
            waitFor(R.id.camera_capture_shutter)
            Thread.sleep(3_000)
            var instruction = ""
            var shutterEnabled = false
            scenario.onActivity {
                instruction = it.findViewById<TextView>(R.id.camera_capture_instruction).text.toString()
                shutterEnabled = it.findViewById<android.view.View>(R.id.camera_capture_shutter).isEnabled
            }
            Log.i(TAG, "camera instruction='$instruction' shutterEnabled=$shutterEnabled")
            shot("camera-preview")
            assertTrue("Manual capture must stay available without a detected document", shutterEnabled)

            onView(withId(R.id.camera_capture_shutter)).perform(click())
            val keepText = app.getString(R.string.camera_quality_keep)
            val deadline = SystemClock.uptimeMillis() + 20_000
            var warned = false
            while (SystemClock.uptimeMillis() < deadline && !warned) {
                warned = runCatching { onView(withText(keepText)).inRoot(isDialog()).check(matches(isDisplayed())) }.isSuccess
                if (!warned) Thread.sleep(500)
            }
            if (warned) {
                shot("quality-warning")
                onView(withText(keepText)).inRoot(isDialog()).perform(click())
            }
            Thread.sleep(800)
            var pageCount = ""
            scenario.onActivity { pageCount = it.findViewById<TextView>(R.id.camera_capture_count).text.toString() }
            Log.i(TAG, "quality warning shown=$warned pages=$pageCount")
            assertTrue("The captured page was not kept (pages=$pageCount)", pageCount == "1")

            onView(withId(R.id.camera_capture_continue)).perform(click())
            waitFor(R.id.camera_page_complete); Thread.sleep(800); shot("page-manager")
            onView(withId(R.id.camera_page_complete)).perform(click())
            Thread.sleep(1_000); shot("ocr-processing")
            waitFor(R.id.ocr_result_review, 30_000); Thread.sleep(800); shot("ocr-result")
        }
    }

    /** A scanned page goes through OCR and its result stays readable, without falling into the error state. */
    @Test fun scannedPageOcrResultStaysLoaded() {
        val directory = File(app.cacheDir, "scan-session").apply { mkdirs() }
        val page = File(directory, "scan_test_${System.nanoTime()}.jpg")
        android.graphics.Bitmap.createBitmap(1200, 1600, android.graphics.Bitmap.Config.ARGB_8888).apply {
            eraseColor(android.graphics.Color.WHITE)
            page.outputStream().use { compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, it) }
            recycle()
        }
        val intent = com.example.lagallens.presentation.feature.camera.ocr.ui.OcrProcessingActivity.createIntent(
            app, listOf(com.example.lagallens.domain.model.camera.ScanPage(page.path))
        )
        ActivityScenario.launch<android.app.Activity>(intent).use {
            waitFor(R.id.ocr_result_review, 30_000)
            val errorText = app.getString(R.string.auth_backend_not_configured)
            fun documentText(): String {
                var text = ""
                instrumentation.runOnMainSync {
                    androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
                        .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED).firstOrNull()
                        ?.findViewById<TextView>(R.id.ocr_result_document)?.let { text = it.text.toString() }
                }
                return text
            }
            val first = documentText()
            Thread.sleep(5_000)
            val later = documentText()
            shot("scanned-ocr-result")
            Log.i(TAG, "ocr result first='${first.take(60)}' later='${later.take(60)}'")
            assertTrue("OCR result fell into the error state: '$later'", !later.contains(errorText) && later.isNotBlank())

            onView(withId(R.id.ocr_result_review)).perform(click())
            waitFor(R.id.ocr_review_confirm); shot("ocr-review")
            onView(withId(R.id.ocr_review_confirm)).perform(click())
            Thread.sleep(2_500)
            shot("after-review-confirm")
            val afterReview = documentText()
            Log.i(TAG, "after review confirm='${afterReview.take(80)}'")
            assertTrue("Review confirmation put the OCR result in the error state", !afterReview.contains(errorText))
        }
        page.delete()
    }

    /** Diagnostic: logs the detection for every real photo in `/sdcard/Pictures/scan-samples`; skipped when absent. */
    @Test fun logsDetectionForSamplePhotos() {
        val listing = android.os.ParcelFileDescriptor.AutoCloseInputStream(
            instrumentation.uiAutomation.executeShellCommand("ls /sdcard/Pictures/scan-samples")
        ).bufferedReader().readText()
        val names = listing.lines().map { it.trim() }.filter { it.endsWith(".jpg", true) || it.endsWith(".png", true) }
        if (names.isEmpty()) return
        assertTrue(org.opencv.android.OpenCVLoader.initLocal())
        names.forEach { name ->
            val local = File(app.cacheDir, "sample-$name")
            android.os.ParcelFileDescriptor.AutoCloseInputStream(
                instrumentation.uiAutomation.executeShellCommand("cat /sdcard/Pictures/scan-samples/$name")
            ).use { input -> local.outputStream().use { input.copyTo(it) } }
            val bitmap = android.graphics.BitmapFactory.decodeFile(local.path)
            val detection = if (bitmap == null) "unreadable" else com.example.lagallens.core.camera.DocumentVisionAnalyzer.detect(bitmap).toString()
            val quality = com.example.lagallens.core.camera.DocumentVisionAnalyzer.checkQuality(local, false)
            Log.i(TAG, "sample $name -> $detection | ${quality.decision} ${quality.issues}")
            bitmap?.recycle()
            local.delete()
        }
    }

    private fun waitFor(viewId: Int, timeoutMillis: Long = 15_000) {
        val deadline = SystemClock.uptimeMillis() + timeoutMillis
        while (true) {
            try {
                onView(withId(viewId)).check(matches(isDisplayed()))
                return
            } catch (failure: Throwable) {
                if (SystemClock.uptimeMillis() > deadline) throw failure
                Thread.sleep(250)
            }
        }
    }

    private fun shot(name: String) {
        instrumentation.waitForIdleSync()
        val bitmap = instrumentation.uiAutomation.takeScreenshot() ?: return
        index++
        File(directory, "%02d-%s.png".format(index, name)).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    private companion object {
        const val TAG = "CAMERA_SCAN_TEST"
    }
}
