package com.example.lagallens.presentation.feature.analysis

import com.example.lagallens.data.mapper.DemoAnalysisSnapshotMapper
import com.example.lagallens.domain.model.analysis.AnalysisIdentity
import com.example.lagallens.domain.repository.AnalysisIdentityRepository
import com.example.lagallens.presentation.feature.analysis.summary.viewmodel.AnalysisSummaryViewModel
import com.example.lagallens.presentation.feature.analysis.keyfacts.viewmodel.AnalysisKeyFactsViewModel
import com.example.lagallens.presentation.feature.analysis.parties.viewmodel.AnalysisPartiesViewModel
import com.example.lagallens.presentation.feature.analysis.obligations.viewmodel.AnalysisObligationsViewModel
import com.example.lagallens.presentation.feature.analysis.risk.detail.viewmodel.AnalysisRiskDetailViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class AnalysisSnapshotViewModelsTest {
    private fun fixture(text: String, resultId: String, contractId: String, documentId: String): Map<String, String> {
        val keys = listOf("analysis_summary_contract_type_title", "analysis_summary_contract_type_description",
            "analysis_key_facts_salary_label", "analysis_key_facts_salary_reference", "analysis_key_facts_salary_value", "analysis_key_facts_salary_description",
            "analysis_parties_employer_title", "analysis_obligations_employer_heading",
            "analysis_risk_findings_penalty_clause", "analysis_risk_findings_penalty_title", "analysis_risk_findings_penalty_description", "analysis_risk_findings_penalty_consequence",
            "analysis_risk_findings_termination_clause", "analysis_risk_findings_termination_title", "analysis_risk_findings_termination_description", "analysis_risk_findings_termination_consequence")
        val s = mutableMapOf("identity:resultId" to resultId, "identity:contractId" to contractId,
            "identity:documentId" to documentId, "identity:accountId" to "account")
        keys.forEach { s["vi:$it"] = text; s["en:$it"] = text }
        s["vi:test"] = text
        s["layout:summary"] = """[["analysis_summary_contract_type_title","analysis_summary_contract_type_description"]]"""
        s["layout:facts"] = """[["analysis_key_facts_salary_label","analysis_key_facts_salary_reference","analysis_key_facts_salary_value","analysis_key_facts_salary_description"]]"""
        s["layout:parties"] = """[["analysis_parties_employer_title","analysis_parties_employer_title"]]"""
        s["layout:obligations"] = """[["section","analysis_obligations_employer_heading"]]"""
        s["layout:findings"] = """{"penalty-cap":["analysis_risk_findings_penalty_clause","analysis_risk_findings_penalty_title","analysis_risk_findings_penalty_description","analysis_risk_findings_penalty_consequence"],"termination-notice":["analysis_risk_findings_termination_clause","analysis_risk_findings_termination_title","analysis_risk_findings_termination_description","analysis_risk_findings_termination_consequence"]}"""
        s["risk:penalty-cap:severity"] = "HIGH"; s["risk:termination-notice:severity"] = "MEDIUM"
        return s
    }
    private inner class Repository : AnalysisIdentityRepository {
        val first = AnalysisIdentity("one", "op1", "c1", "d1", listOf("penalty-cap"), snapshot = fixture("first", "one", "c1", "d1"))
        val second = AnalysisIdentity("two", "op2", "c2", "d2", listOf("termination-notice"), snapshot = fixture("second", "two", "c2", "d2"))
        override fun sessionKey() = "account:session"
        override suspend fun create(operationId: String, contractId: String, documentId: String, sessionKey: String): AnalysisIdentity = error("Must not create on read")
        override suspend fun get(resultId: String, sessionKey: String): AnalysisIdentity? =
            if (sessionKey != sessionKey()) null else listOf(first, second).firstOrNull { it.resultId == resultId }
        override suspend fun latest(contractId: String, sessionKey: String): AnalysisIdentity? = null
    }
    private fun detail(identity: AnalysisIdentity) = DemoAnalysisSnapshotMapper("vi", "unavailable").toDetail(identity)
    private fun isValid(identity: AnalysisIdentity) = runCatching { detail(identity) }.isSuccess

    @Test fun screensShowTheSelectedResultAndOtherSessionsCannotReadIt() = runTest {
        val repository = Repository()
        val first = requireNotNull(repository.get("one", "account:session"))
        val second = requireNotNull(repository.get("two", "account:session"))
        assertTrue(isValid(first))
        assertTrue(isValid(second))
        assertFalse(isValid(first.copy(snapshot = first.snapshot!! - "layout:summary")))
        val summary = AnalysisSummaryViewModel(); val facts = AnalysisKeyFactsViewModel()
        val parties = AnalysisPartiesViewModel(); val obligations = AnalysisObligationsViewModel()
        summary.setResult(first, detail(first), isUnlocked = true); facts.setResult(first, detail(first), isUnlocked = true)
        parties.setResult(first, detail(first), isUnlocked = true); obligations.setResult(first, detail(first), isUnlocked = true)
        assertEquals("first", summary.uiState.value.sections.single().title)
        assertEquals("first", facts.uiState.value.facts.single().label)
        assertEquals("first", parties.uiState.value.parties.single().title)
        summary.setResult(second, detail(second), isUnlocked = true); facts.setResult(second, detail(second), isUnlocked = true)
        parties.setResult(second, detail(second), isUnlocked = true); obligations.setResult(second, detail(second), isUnlocked = true)
        assertEquals("two", summary.uiState.value.resultId)
        assertEquals("second", facts.uiState.value.facts.single().label)
        assertEquals("second", parties.uiState.value.parties.single().title)
        assertNull(repository.get("one", "other:session"))
    }

    @Test fun findingMustBelongToTheResult() = runTest {
        val repository = Repository(); val first = requireNotNull(repository.get("one", "account:session"))
        val riskDetail = AnalysisRiskDetailViewModel()
        assertTrue(riskDetail.loadFinding("penalty-cap", first, detail(first)))
        assertEquals("first", riskDetail.uiState.value.title)
        assertFalse(riskDetail.loadFinding("termination-notice", first, detail(first)))
        assertFalse(isValid(AnalysisIdentity("old", "op", "c", "d")))
    }

    @Test fun optionalFindingDetailStaysWithItsResultAndRequiresBothLanguages() {
        val first = Repository().first
        val original = requireNotNull(first.snapshot)
        val enriched = first.copy(snapshot = original + mapOf(
            "risk:penalty-cap:recommendation" to "advice",
            "vi:advice" to "Lời khuyên A", "en:advice" to "Advice A"
        ))
        val riskDetail = AnalysisRiskDetailViewModel()
        assertTrue(riskDetail.loadFinding("penalty-cap", enriched, detail(enriched)))
        assertEquals("Lời khuyên A", riskDetail.uiState.value.recommendation)
        assertTrue(riskDetail.loadFinding("penalty-cap", first, detail(first)))
        assertNull(riskDetail.uiState.value.recommendation)
        assertFalse(isValid(enriched.copy(snapshot = enriched.snapshot!! - "en:advice")))
        assertFalse(riskDetail.loadFinding("not-a-finding", enriched, detail(enriched)))
    }

    @Test fun versionedStructuresPreserveOrderAndRejectIncompleteContent() {
        val identity = Repository().first
        val v1 = requireNotNull(identity.snapshot)
        assertTrue(isValid(identity))
        val v2 = v1 + mapOf(
            "schema:version" to "2",
            "layout:summary" to """[["s","analysis_summary_contract_type_title","analysis_summary_contract_type_description"]]""",
            "layout:facts" to """[["f","analysis_key_facts_salary_label","analysis_key_facts_salary_reference","analysis_key_facts_salary_value","analysis_key_facts_salary_description"]]""",
            "layout:parties" to """[["p","analysis_parties_employer_title","analysis_parties_employer_title"]]""",
            "layout:obligations" to """[["section","o","analysis_obligations_employer_heading"]]"""
        )
        assertTrue(isValid(identity.copy(snapshot = v2)))
        val v3 = v2 + mapOf("schema:version" to "3", "layout:parties" to """[["p","analysis_parties_employer_title","analysis_parties_employer_title",[]]]""")
        assertTrue(isValid(identity.copy(snapshot = v3)))
        assertEquals(listOf("s"), detail(identity.copy(snapshot = v3)).summary.map { it.id })
        assertFalse(isValid(identity.copy(snapshot = v3 - "en:analysis_summary_contract_type_title")))
        assertFalse(isValid(identity.copy(snapshot = v3 + ("schema:version" to "99"))))
        assertFalse(isValid(identity.copy(snapshot = v3 + ("layout:parties" to "[["))))
    }

    @Test fun oldAndNewObligationRowsRemainReadable() {
        val identity = Repository().first
        val legacy = identity.snapshot!! + mapOf(
            "layout:obligations" to """[["section","analysis_obligations_employer_heading"],["obligation","analysis_key_facts_salary_label","analysis_key_facts_salary_reference","analysis_key_facts_salary_description"]]"""
        )
        val old = detail(identity.copy(snapshot = legacy)).obligations
        assertNull((old[1] as com.example.lagallens.domain.model.analysis.AnalysisObligationEntry.Item).source)
        val updated = legacy + mapOf(
            "layout:obligations" to """[["section","analysis_obligations_employer_heading","duties"],["obligation","analysis_key_facts_salary_label","analysis_key_facts_salary_reference","analysis_key_facts_salary_description","analysis_key_facts_salary_reference","analysis_key_facts_salary_label","analysis_key_facts_salary_description"]]"""
        )
        val current = identity.copy(snapshot = updated)
        assertTrue(com.example.lagallens.data.repository.AnalysisSnapshotPayloadValidator.isValid(current, "account"))
        val mapped = detail(current).obligations
        assertEquals(com.example.lagallens.domain.model.analysis.AnalysisObligationTab.DUTIES, (mapped[0] as com.example.lagallens.domain.model.analysis.AnalysisObligationEntry.Section).tab)
        assertEquals("first", (mapped[1] as com.example.lagallens.domain.model.analysis.AnalysisObligationEntry.Item).source?.excerpt)
    }
}
