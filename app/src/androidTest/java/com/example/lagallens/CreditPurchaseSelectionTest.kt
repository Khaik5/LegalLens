package com.example.lagallens

import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.lagallens.presentation.feature.creditstore.flow.ui.PurchaseConfirmationActivity
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CreditPurchaseSelectionTest {
    @Test fun confirmationShowsSelectedPackageAndPrice() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val samples = listOf(Triple("credits_5", "5 Credits", "29.000đ"), Triple("credits_15", "15 Credits", "79.000đ"), Triple("credits_30", "30 Credits", "149.000đ"), Triple("credits_100", "100 Credits", "449.000đ"))
        samples.forEach { (id, title, price) ->
            ActivityScenario.launch<PurchaseConfirmationActivity>(PurchaseConfirmationActivity.createIntent(context, id, 5)).use {
                onView(withId(R.id.purchasePackageTitle)).check(matches(withText(title)))
                onView(withId(R.id.purchaseSummaryPrice)).check(matches(withText(price)))
            }
        }
    }
}
