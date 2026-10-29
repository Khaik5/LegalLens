package com.example.lagallens.data.repository

import android.content.Context
import com.example.lagallens.core.network.BackendNotConfiguredException
import com.example.lagallens.domain.model.payment.PaymentRecord
import com.example.lagallens.domain.model.payment.PaymentStatus
import com.example.lagallens.domain.repository.CreditRepository
import com.example.lagallens.domain.repository.PaymentRepository
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class FakePaymentRepository(context: Context, private val credits: CreditRepository, preferencesName: String = "payment_demo_records") : PaymentRepository {
    private val preferences = context.applicationContext.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    private val lock = Mutex()

    override suspend fun createOrGet(paymentId: String, packageId: String, amountVnd: Long, credits: Int): PaymentRecord = lock.withLock {
        require(paymentId.isNotBlank() && packageId.isNotBlank() && amountVnd >= 0 && credits > 0)
        val existing = read(paymentId)
        if (existing != null) {
            require(existing.packageId == packageId && existing.amountVnd == amountVnd && existing.credits == credits) {
                "paymentId is already associated with a different payment"
            }
            existing
        } else {
            PaymentRecord(paymentId, packageId, amountVnd, credits, PaymentStatus.PENDING, System.currentTimeMillis()).also(::save)
        }
    }

    override suspend fun updateStatus(paymentId: String, status: PaymentStatus): PaymentRecord = lock.withLock {
        val current = requireNotNull(read(paymentId)) { "Unknown paymentId" }
        require(status != PaymentStatus.SUCCESS || current.status == PaymentStatus.SUCCESS) {
            "Use completeDemoPayment to complete a DEMO payment"
        }
        val next = when {
            current.status == status -> current
            current.status == PaymentStatus.PENDING -> current.copy(status = status, updatedAtEpochMillis = System.currentTimeMillis())
            else -> current // terminal payments are immutable; use a new paymentId to retry
        }
        save(next)
        next
    }

    override suspend fun get(paymentId: String): PaymentRecord? = lock.withLock { read(paymentId) }

    override suspend fun completeDemoPayment(paymentId: String): PaymentRecord = lock.withLock {
        val current = requireNotNull(read(paymentId)) { "Unknown paymentId" }
        if (current.status != PaymentStatus.PENDING) return@withLock current
        // CreditRepository is idempotent by paymentId, so retry after process death cannot double-credit.
        credits.addPurchasedCredits(current.paymentId, current.packageId, current.amountVnd, current.credits)
        current.copy(status = PaymentStatus.SUCCESS, updatedAtEpochMillis = System.currentTimeMillis()).also(::save)
    }

    private fun read(paymentId: String): PaymentRecord? {
        val rows = JSONArray(preferences.getString(KEY_RECORDS, "[]"))
        for (index in 0 until rows.length()) {
            val row = rows.getJSONObject(index)
            if (row.optString("id") == paymentId) return row.toRecord()
        }
        return null
    }

    private fun save(record: PaymentRecord) {
        val rows = JSONArray(preferences.getString(KEY_RECORDS, "[]"))
        val updated = JSONArray()
        var replaced = false
        for (index in 0 until rows.length()) {
            val row = rows.getJSONObject(index)
            if (row.optString("id") == record.paymentId) {
                updated.put(record.toJson())
                replaced = true
            } else updated.put(row)
        }
        if (!replaced) updated.put(record.toJson())
        check(preferences.edit().putString(KEY_RECORDS, updated.toString()).commit())
    }

    private fun JSONObject.toRecord() = PaymentRecord(
        paymentId = getString("id"), packageId = getString("packageId"), amountVnd = getLong("amount"),
        credits = getInt("credits"), status = PaymentStatus.valueOf(getString("status")), updatedAtEpochMillis = getLong("updatedAt")
    )

    private fun PaymentRecord.toJson() = JSONObject().put("id", paymentId).put("packageId", packageId)
        .put("amount", amountVnd).put("credits", credits).put("status", status.name).put("updatedAt", updatedAtEpochMillis)

    private companion object { const val KEY_RECORDS = "records_v1" }
}

class UnconfiguredRemotePaymentRepository : PaymentRepository {
    override val isConfigured: Boolean = false
    private fun unavailable(): Nothing = throw BackendNotConfiguredException("Payment")
    override suspend fun createOrGet(paymentId: String, packageId: String, amountVnd: Long, credits: Int): PaymentRecord = unavailable()
    override suspend fun updateStatus(paymentId: String, status: PaymentStatus): PaymentRecord = unavailable()
    override suspend fun get(paymentId: String): PaymentRecord? = unavailable()
    override suspend fun completeDemoPayment(paymentId: String): PaymentRecord = unavailable()
}
