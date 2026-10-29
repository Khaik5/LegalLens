package com.example.lagallens

import android.os.Bundle
import androidx.test.runner.AndroidJUnitRunner

/** Fail before tests run if the test APK is pointed at the user's installed app. */
class SandboxTestRunner : AndroidJUnitRunner() {
    override fun onCreate(arguments: Bundle?) {
        check(targetContext.packageName == "com.example.lagallens.testsandbox") {
            "Instrumentation requires the isolated testSandbox application"
        }
        super.onCreate(arguments)
    }
}
