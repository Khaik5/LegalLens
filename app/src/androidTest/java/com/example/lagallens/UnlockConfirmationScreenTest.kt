package com.example.lagallens

import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.lagallens.presentation.feature.creditstore.unlock.ui.UnlockConfirmationActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UnlockConfirmationScreenTest {
    @get:Rule val session = DemoSessionRule()

    @Test fun theResultCodeIsNotShownToTheUser() {
        val app = ApplicationProvider.getApplicationContext<LegalLensApplication>()
        val resultId = "8b0cb393-0851-4041-a974-ca9c10fd8a7f"
        val intent = UnlockConfirmationActivity.createIntent(app, resultId, availableCredits = 10, unlockCost = 2, contractName = "Contract")

        ActivityScenario.launch<UnlockConfirmationActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val meta = activity.findViewById<android.widget.TextView>(R.id.unlockConfirmationResultMeta).text.toString()
                assertEquals(activity.getString(R.string.unlock_confirmation_result_meta), meta)
                assertFalse("The technical result ID must stay hidden: $meta", meta.contains(resultId))
                assertFalse(meta.contains("%%"))
            }
        }
    }
}
