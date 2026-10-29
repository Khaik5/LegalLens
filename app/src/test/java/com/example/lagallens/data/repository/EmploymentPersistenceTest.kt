package com.example.lagallens.data.repository

import android.content.SharedPreferences
import com.example.lagallens.data.datasource.local.datastore.EmploymentDemoDataSource
import com.example.lagallens.domain.model.EmploymentContract
import com.example.lagallens.domain.model.EmploymentDocument
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.lang.reflect.Proxy

class EmploymentPersistenceTest {
    private fun preferences(): SharedPreferences {
        val values = mutableMapOf<String, Any?>()
        return Proxy.newProxyInstance(javaClass.classLoader, arrayOf(SharedPreferences::class.java)) { _, method, args ->
            when (method.name) {
                "getAll" -> values.toMap()
                "contains" -> values.containsKey(args!![0])
                "getString", "getStringSet", "getInt", "getLong", "getBoolean", "getFloat" -> values[args!![0]] ?: args[1]
                "edit" -> {
                    val changes = mutableMapOf<String, Any?>(); var clear = false
                    Proxy.newProxyInstance(javaClass.classLoader, arrayOf(SharedPreferences.Editor::class.java)) { editor, operation, inputs ->
                        when {
                            operation.name.startsWith("put") -> { changes[inputs!![0] as String] = inputs[1]; editor }
                            operation.name == "remove" -> { changes[inputs!![0] as String] = null; editor }
                            operation.name == "clear" -> { clear = true; editor }
                            operation.name == "commit" || operation.name == "apply" -> {
                                if (clear) values.clear()
                                changes.forEach { (key, value) -> if (value == null) values.remove(key) else values[key] = value }
                                if (operation.name == "commit") true else null
                            }
                            else -> null
                        }
                    }
                }
                else -> null
            }
        } as SharedPreferences
    }
    private fun source(prefs: SharedPreferences) = EmploymentDemoDataSource(prefs) { "Labor $it" }

    @Test fun actualRepositoriesKeepAccountsSeparateAndPersistLinks() = runBlocking {
        val a = preferences(); val b = preferences()
        val aSource = source(a); val bSource = source(b)
        val contractsA = FakeContractRepository(aSource); val documentsA = FakeDocumentRepository(aSource)
        val contractsB = FakeContractRepository(bSource); val documentsB = FakeDocumentRepository(bSource)
        contractsA.createContract(EmploymentContract("A", "docA", "A", "private A", 1, 1))
        assertNull(contractsB.getContract("A"))
        assertNull(documentsB.getDocument("docA"))
        documentsB.saveDocument(EmploymentDocument("docB", null, "B", null, "text/plain", "private B", 1))
        assertNull(documentsA.getDocument("docB"))
        val reopened = source(a)
        assertEquals("private A", FakeContractRepository(reopened).getContract("A")?.content)
        assertEquals("A", FakeDocumentRepository(reopened).getDocument("docA")?.contractId)
        try { documentsA.saveDocument(EmploymentDocument("wrong", "A", "bad", null, "text/plain", "bad", 1)); fail() }
        catch (_: IllegalArgumentException) { }
    }

    @Test fun deletingAllContractsDoesNotSeedOnRestart() = runBlocking {
        val prefs = preferences(); val first = source(prefs)
        first.contracts.value.toList().forEach { first.deleteContract(it.id) }
        assertTrue(source(prefs).contracts.value.isEmpty())
    }

    @Test fun corruptStorageFailsWithoutReplacingIt() {
        val prefs = preferences()
        prefs.edit().putString("employment_contracts_v2", "broken").commit()
        try { source(prefs); fail() } catch (_: org.json.JSONException) { }
        assertEquals("broken", prefs.getString("employment_contracts_v2", null))
    }
}
