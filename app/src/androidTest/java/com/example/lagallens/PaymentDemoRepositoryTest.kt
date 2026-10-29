package com.example.lagallens

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.lagallens.data.repository.FakePaymentRepository
import com.example.lagallens.data.repository.FakeCreditRepository
import com.example.lagallens.domain.model.payment.PaymentStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PaymentDemoRepositoryTest {
    @Test fun successAddsCreditsOnceAndCancelledPaymentAddsNone() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("credit_demo_wallet", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("payment_demo_records", Context.MODE_PRIVATE).edit().clear().commit()
        val credits = FakeCreditRepository(context)
        val repository = FakePaymentRepository(context, credits)

        repository.createOrGet("pay-success", "credits_5", 29000, 5)
        assertEquals(PaymentStatus.SUCCESS, repository.completeDemoPayment("pay-success").status)
        repository.completeDemoPayment("pay-success")
        assertEquals(10, credits.balance.value)

        repository.createOrGet("pay-cancelled", "credits_5", 29000, 5)
        repository.updateStatus("pay-cancelled", PaymentStatus.CANCELLED)
        assertEquals(PaymentStatus.CANCELLED, repository.completeDemoPayment("pay-cancelled").status)
        assertEquals(10, credits.balance.value)
    }
}
