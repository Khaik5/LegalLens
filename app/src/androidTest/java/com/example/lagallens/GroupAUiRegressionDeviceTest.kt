package com.example.lagallens

import android.content.Context
import android.Manifest
import android.app.Activity
import android.app.Instrumentation
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.PatternMatcher
import android.provider.Settings
import android.view.View
import android.widget.EditText
import androidx.navigation.fragment.NavHostFragment
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.lagallens.domain.model.auth.OtpChallenge
import com.example.lagallens.domain.model.auth.OtpPurpose
import com.example.lagallens.presentation.feature.main.ui.MainActivity
import com.example.lagallens.presentation.feature.camera.permission.ui.CameraPermissionActivity
import com.example.lagallens.presentation.feature.otp.ui.OtpVerificationActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.Assume.assumeTrue
import java.util.Locale

class GroupAUiRegressionDeviceTest {
    @get:Rule val demoSession = DemoSessionRule()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val digitIds = listOf(R.id.otpInput1, R.id.otpInput2, R.id.otpInput3, R.id.otpInput4, R.id.otpInput5, R.id.otpInput6)

    @Test fun sixDigitPastePassesXmlFilterAndFillsEveryBox() {
        otpScenario().use { scenario ->
            scenario.onActivity { activity ->
                activity.findViewById<EditText>(R.id.otpInput1).apply {
                    requestFocus()
                    setText("012345")
                }
                assertEquals("012345", digits(activity))
                assertTrue(activity.findViewById<EditText>(R.id.otpInput6).hasFocus())
            }
        }
    }

    @Test fun recreationRestoresDigitsWithoutMovingFocus() {
        otpScenario().use { scenario ->
            scenario.onActivity { activity ->
                activity.findViewById<EditText>(R.id.otpInput1).setText("012345")
                activity.findViewById<EditText>(R.id.otpInput3).requestFocus()
            }
            scenario.recreate()
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                assertEquals("012345", digits(activity))
                assertTrue("Rendering restored digits must keep the third box focused", activity.findViewById<EditText>(R.id.otpInput3).hasFocus())
            }
        }
    }

    @Test fun repeatedProfileActionsNavigateOnceForAllThreeDestinations() {
        for ((row, destination) in listOf(
            R.id.actionEditProfile to R.id.profileEditFragment,
            R.id.actionNotificationSettings to R.id.profileSettingsFragment,
            R.id.actionChangePassword to R.id.profilePasswordFragment
        )) {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity { activity ->
                    val host = activity.supportFragmentManager.findFragmentById(R.id.mainNavHost) as NavHostFragment
                    host.navController.navigate(R.id.profileFragment)
                    host.childFragmentManager.executePendingTransactions()
                    val source = requireNotNull(activity.findViewById<View>(row))
                    source.performClick()
                    source.performClick()
                }
                instrumentation.waitForIdleSync()
                scenario.onActivity { activity ->
                    val host = activity.supportFragmentManager.findFragmentById(R.id.mainNavHost) as NavHostFragment
                    assertEquals(destination, host.navController.currentDestination?.id)
                }
            }
        }
    }

    @Test fun historyPluralsAndOcrFallbackResolveInVietnameseAndEnglish() {
        fun resources(language: String) = context.createConfigurationContext(Configuration(context.resources.configuration).apply {
            setLocale(Locale.forLanguageTag(language))
        }).resources
        val english = resources("en")
        assertEquals("1 message", english.getQuantityString(R.plurals.chatbot_history_message_count, 1, 1))
        assertEquals("2 messages", english.getQuantityString(R.plurals.chatbot_history_message_count, 2, 2))
        assertEquals("New employment contract", english.getString(R.string.ocr_result_default_contract_name))
        val vietnamese = resources("vi")
        assertEquals("1 tin nhắn", vietnamese.getQuantityString(R.plurals.chatbot_history_message_count, 1, 1))
        assertEquals("2 tin nhắn", vietnamese.getQuantityString(R.plurals.chatbot_history_message_count, 2, 2))
        assertEquals("Hợp đồng lao động mới", vietnamese.getString(R.string.ocr_result_default_contract_name))
    }

    @Test fun deniedCameraPermissionAfterAnEarlierRequestOpensAppSettings() {
        assumeTrue("Run with the sandbox camera permission revoked", context.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED)
        val filter = IntentFilter(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            addDataScheme("package")
            addDataSchemeSpecificPart(context.packageName, PatternMatcher.PATTERN_LITERAL)
        }
        val monitor = instrumentation.addMonitor(filter, Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null), true)
        try {
            ActivityScenario.launch<CameraPermissionActivity>(CameraPermissionActivity.createIntent(context)).use { scenario ->
                var preferences: android.content.SharedPreferences? = null
                var hadValue = false
                var originalValue = false
                try {
                    scenario.onActivity { activity ->
                        assertTrue(!activity.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA))
                        val prefs = activity.getPreferences(Context.MODE_PRIVATE)
                        preferences = prefs
                        hadValue = prefs.contains("cameraPermissionRequested")
                        originalValue = prefs.getBoolean("cameraPermissionRequested", false)
                        prefs.edit().putBoolean("cameraPermissionRequested", true).commit()
                        activity.findViewById<View>(R.id.camera_permission_allow).performClick()
                    }
                    instrumentation.waitForIdleSync()
                    assertEquals(1, monitor.hits)
                } finally {
                    preferences?.edit()?.apply {
                        if (hadValue) putBoolean("cameraPermissionRequested", originalValue) else remove("cameraPermissionRequested")
                    }?.commit()
                }
            }
        } finally {
            instrumentation.removeMonitor(monitor)
        }
    }

    private fun digits(activity: OtpVerificationActivity) = digitIds.joinToString("") {
        activity.findViewById<EditText>(it).text.toString()
    }

    private fun otpScenario() = ActivityScenario.launch<OtpVerificationActivity>(OtpVerificationActivity.createIntent(
        context, "ui-test@example.invalid", OtpPurpose.REGISTRATION, OtpChallenge("test-challenge", 600, 30)
    ))
}
