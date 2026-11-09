package com.example.lagallens.data.repository

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/** Capture localized DEMO text once, when a result is created. */
internal fun createDemoAnalysisSnapshot(context: Context): Map<String, String> {
    val names = listOf(
        "analysis_clause_source_salary_excerpt",
        "analysis_key_facts_compensation_description",
        "analysis_key_facts_compensation_label",
        "analysis_key_facts_compensation_reference",
        "analysis_key_facts_compensation_value",
        "analysis_key_facts_effective_date_description",
        "analysis_key_facts_effective_date_label",
        "analysis_key_facts_effective_date_reference",
        "analysis_key_facts_effective_date_value",
        "analysis_key_facts_payment_description",
        "analysis_key_facts_payment_label",
        "analysis_key_facts_payment_reference",
        "analysis_key_facts_payment_value",
        "analysis_key_facts_probation_description",
        "analysis_key_facts_probation_label",
        "analysis_key_facts_probation_reference",
        "analysis_key_facts_probation_value",
        "analysis_key_facts_salary_description",
        "analysis_key_facts_salary_label",
        "analysis_key_facts_salary_reference",
        "analysis_key_facts_salary_value",
        "analysis_key_facts_term_description",
        "analysis_key_facts_term_label",
        "analysis_key_facts_term_reference",
        "analysis_key_facts_term_value",
        "analysis_obligations_working_conditions_excerpt",
        "analysis_obligations_salary_excerpt",
        "analysis_obligations_insurance_excerpt",
        "analysis_obligations_professional_work_excerpt",
        "analysis_obligations_confidentiality_excerpt",
        "analysis_obligations_confidentiality_clause",
        "analysis_obligations_confidentiality_description",
        "analysis_obligations_confidentiality_title",
        "analysis_obligations_employee_heading",
        "analysis_obligations_employer_heading",
        "analysis_obligations_insurance_clause",
        "analysis_obligations_insurance_description",
        "analysis_obligations_insurance_title",
        "analysis_obligations_professional_work_clause",
        "analysis_obligations_professional_work_description",
        "analysis_obligations_professional_work_title",
        "analysis_obligations_salary_clause",
        "analysis_obligations_salary_description",
        "analysis_obligations_salary_title",
        "analysis_obligations_working_conditions_clause",
        "analysis_obligations_working_conditions_description",
        "analysis_obligations_working_conditions_title",
        "analysis_parties_address_label",
        "analysis_parties_address_value",
        "analysis_parties_business_id_label",
        "analysis_parties_business_id_value",
        "analysis_parties_contact_label",
        "analysis_parties_contact_value",
        "analysis_parties_employee_role",
        "analysis_parties_employee_title",
        "analysis_parties_employer_role",
        "analysis_parties_employer_title",
        "analysis_parties_full_name_label",
        "analysis_parties_full_name_value",
        "analysis_parties_headquarters_label",
        "analysis_parties_headquarters_value",
        "analysis_parties_identity_label",
        "analysis_parties_identity_value",
        "analysis_parties_organization_label",
        "analysis_parties_organization_value",
        "analysis_parties_representative_label",
        "analysis_parties_representative_value",
        "analysis_risk_detail_attention_reason",
        "analysis_risk_detail_clause_excerpt",
        "analysis_risk_detail_clause_reference",
        "analysis_risk_detail_legal_explanation",
        "analysis_risk_detail_legal_reference_subtitle",
        "analysis_risk_detail_legal_reference_title",
        "analysis_risk_detail_recommendation",
        "analysis_risk_detail_severity_high",
        "analysis_risk_findings_dispute_clause",
        "analysis_risk_findings_dispute_consequence",
        "analysis_risk_findings_dispute_description",
        "analysis_risk_findings_dispute_title",
        "analysis_risk_findings_penalty_clause",
        "analysis_risk_findings_penalty_consequence",
        "analysis_risk_findings_penalty_description",
        "analysis_risk_findings_penalty_title",
        "analysis_risk_findings_termination_clause",
        "analysis_risk_findings_termination_consequence",
        "analysis_risk_findings_termination_description",
        "analysis_risk_findings_termination_title",
        "analysis_summary_contract_type_description",
        "analysis_summary_contract_type_title",
        "analysis_summary_effective_term_description",
        "analysis_summary_effective_term_title",
        "analysis_summary_key_terms_description",
        "analysis_summary_key_terms_title",
        "analysis_summary_notable_points_description",
        "analysis_summary_notable_points_title",
        "analysis_summary_parties_description",
        "analysis_summary_parties_title",
    )
    return buildMap {
        put("schema:version", "3")
        put("translation:en", "complete-v1")
        put("layout:summary", """[["summary:analysis_summary_contract_type_title","analysis_summary_contract_type_title","analysis_summary_contract_type_description"],["summary:analysis_summary_parties_title","analysis_summary_parties_title","analysis_summary_parties_description"],["summary:analysis_summary_effective_term_title","analysis_summary_effective_term_title","analysis_summary_effective_term_description"],["summary:analysis_summary_key_terms_title","analysis_summary_key_terms_title","analysis_summary_key_terms_description"],["summary:analysis_summary_notable_points_title","analysis_summary_notable_points_title","analysis_summary_notable_points_description"]]""")
        put("layout:facts", """[["facts:analysis_key_facts_salary_label","analysis_key_facts_salary_label","analysis_key_facts_salary_reference","analysis_key_facts_salary_value","analysis_key_facts_salary_description","analysis_key_facts_salary_reference","analysis_key_facts_salary_label","analysis_clause_source_salary_excerpt"],["facts:analysis_key_facts_probation_label","analysis_key_facts_probation_label","analysis_key_facts_probation_reference","analysis_key_facts_probation_value","analysis_key_facts_probation_description"],["facts:analysis_key_facts_payment_label","analysis_key_facts_payment_label","analysis_key_facts_payment_reference","analysis_key_facts_payment_value","analysis_key_facts_payment_description"],["facts:analysis_key_facts_compensation_label","analysis_key_facts_compensation_label","analysis_key_facts_compensation_reference","analysis_key_facts_compensation_value","analysis_key_facts_compensation_description"],["facts:analysis_key_facts_term_label","analysis_key_facts_term_label","analysis_key_facts_term_reference","analysis_key_facts_term_value","analysis_key_facts_term_description"],["facts:analysis_key_facts_effective_date_label","analysis_key_facts_effective_date_label","analysis_key_facts_effective_date_reference","analysis_key_facts_effective_date_value","analysis_key_facts_effective_date_description"]]""")
        put("layout:parties", """[["parties:analysis_parties_employer_title","analysis_parties_employer_title","analysis_parties_employer_role",[["field:analysis_parties_organization_label","analysis_parties_organization_label","analysis_parties_organization_value"],["field:analysis_parties_business_id_label","analysis_parties_business_id_label","analysis_parties_business_id_value"],["field:analysis_parties_headquarters_label","analysis_parties_headquarters_label","analysis_parties_headquarters_value"],["field:analysis_parties_representative_label","analysis_parties_representative_label","analysis_parties_representative_value"]]],["parties:analysis_parties_employee_title","analysis_parties_employee_title","analysis_parties_employee_role",[["field:analysis_parties_full_name_label","analysis_parties_full_name_label","analysis_parties_full_name_value"],["field:analysis_parties_identity_label","analysis_parties_identity_label","analysis_parties_identity_value"],["field:analysis_parties_address_label","analysis_parties_address_label","analysis_parties_address_value"],["field:analysis_parties_contact_label","analysis_parties_contact_label","analysis_parties_contact_value"]]]]""")
        put("layout:obligations", """[["section","obligation:analysis_obligations_employer_heading","analysis_obligations_employer_heading","rights"],["obligation","obligation:analysis_obligations_working_conditions_title","analysis_obligations_working_conditions_title","analysis_obligations_working_conditions_clause","analysis_obligations_working_conditions_description","analysis_obligations_working_conditions_clause","analysis_obligations_working_conditions_title","analysis_obligations_working_conditions_excerpt"],["obligation","obligation:analysis_obligations_salary_title","analysis_obligations_salary_title","analysis_obligations_salary_clause","analysis_obligations_salary_description","analysis_obligations_salary_clause","analysis_obligations_salary_title","analysis_obligations_salary_excerpt"],["obligation","obligation:analysis_obligations_insurance_title","analysis_obligations_insurance_title","analysis_obligations_insurance_clause","analysis_obligations_insurance_description","analysis_obligations_insurance_clause","analysis_obligations_insurance_title","analysis_obligations_insurance_excerpt"],["section","obligation:analysis_obligations_employee_heading","analysis_obligations_employee_heading","duties"],["obligation","obligation:analysis_obligations_professional_work_title","analysis_obligations_professional_work_title","analysis_obligations_professional_work_clause","analysis_obligations_professional_work_description","analysis_obligations_professional_work_clause","analysis_obligations_professional_work_title","analysis_obligations_professional_work_excerpt"],["obligation","obligation:analysis_obligations_confidentiality_title","analysis_obligations_confidentiality_title","analysis_obligations_confidentiality_clause","analysis_obligations_confidentiality_description","analysis_obligations_confidentiality_clause","analysis_obligations_confidentiality_title","analysis_obligations_confidentiality_excerpt"]]""")
        put("layout:findings", """{"penalty-cap":["analysis_risk_findings_penalty_clause","analysis_risk_findings_penalty_title","analysis_risk_findings_penalty_description","analysis_risk_findings_penalty_consequence"],"termination-notice":["analysis_risk_findings_termination_clause","analysis_risk_findings_termination_title","analysis_risk_findings_termination_description","analysis_risk_findings_termination_consequence"],"dispute-resolution":["analysis_risk_findings_dispute_clause","analysis_risk_findings_dispute_title","analysis_risk_findings_dispute_description","analysis_risk_findings_dispute_consequence"]}""")

        put("risk:penalty-cap:severity", "HIGH")
        put("risk:penalty-cap:recommendation", "analysis_risk_detail_recommendation")
        put("risk:termination-notice:severity", "MEDIUM")
        put("risk:dispute-resolution:severity", "LOW")
        for (language in listOf("vi", "en")) {
            val config = Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag(language)) }
            val localized = context.createConfigurationContext(config)
            for (name in names) {
                val id = context.resources.getIdentifier(name, "string", context.packageName)
                check(id != 0) { "Missing DEMO fixture resource: $name" }
                put("$language:$name", if (language == "en") demoSnapshotEnglish[name] ?: localized.getString(id) else localized.getString(id))
            }
        }
    }
}
