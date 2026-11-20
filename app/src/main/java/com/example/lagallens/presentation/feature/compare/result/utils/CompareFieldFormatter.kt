package com.example.lagallens.presentation.feature.compare.result.utils

import android.content.Context
import androidx.annotation.StringRes
import com.example.lagallens.R
import com.example.lagallens.core.utils.foldForSearch
import com.example.lagallens.domain.model.ContractFieldValue
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.FormatStyle

/**
 * Turns the backend's field keys and raw codes into text in the language of the app. The backend labels and
 * codes stay as the fallback, so a field or code this app does not know yet is still shown.
 */
class CompareFieldFormatter(private val context: Context) {
    fun label(field: String, backendLabel: String): String =
        labelRes(field)?.let(context::getString) ?: backendLabel

    fun value(field: String, value: ContractFieldValue): String = when (value) {
        is ContractFieldValue.Flag -> context.getString(if (value.isYes) R.string.compare_value_yes else R.string.compare_value_no)
        is ContractFieldValue.Parties -> value.entries.joinToString("\n") { party ->
            // The analysis often writes the role into the name already ("Người lao động (Bên B) - …").
            val name = party.name.foldForSearch()
            val role = party.role?.takeUnless { role -> roleKeywords(role).any(name::contains) }?.let(::roleText)
            role?.let { "${party.name} ($it)" } ?: party.name
        }
        is ContractFieldValue.Text -> when (field) {
            FIELD_CONTRACT_TYPE -> contractTypeRes(value.value)?.let(context::getString) ?: value.value
            FIELD_START_DATE, FIELD_END_DATE -> dateText(value.value)
            else -> value.value
        }
    }

    private fun dateText(isoDate: String): String = try {
        LocalDate.parse(isoDate).format(
            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(context.resources.configuration.locales[0])
        )
    } catch (_: DateTimeParseException) {
        isoDate
    }

    /** Words that already name the role inside a party name, in both languages, without marks. */
    private fun roleKeywords(role: String): List<String> = when (role.uppercase()) {
        "EMPLOYER" -> listOf("su dung lao dong", "employer")
        "EMPLOYEE" -> listOf("nguoi lao dong", "employee")
        else -> listOf(role.foldForSearch())
    }

    private fun roleText(role: String): String = when (role.uppercase()) {
        "EMPLOYER" -> context.getString(R.string.compare_party_role_employer)
        "EMPLOYEE" -> context.getString(R.string.compare_party_role_employee)
        else -> role
    }

    @StringRes
    private fun contractTypeRes(code: String): Int? = when (code.uppercase()) {
        "XAC_DINH_THOI_HAN" -> R.string.compare_contract_type_fixed_term
        "KHONG_XAC_DINH_THOI_HAN" -> R.string.compare_contract_type_indefinite
        "THOI_VU" -> R.string.compare_contract_type_seasonal
        "THU_VIEC" -> R.string.compare_contract_type_probation
        "KHAC" -> R.string.compare_contract_type_other
        else -> null
    }

    @StringRes
    private fun labelRes(field: String): Int? = when (field) {
        FIELD_CONTRACT_TYPE -> R.string.compare_field_contract_type
        FIELD_START_DATE -> R.string.compare_field_start_date
        FIELD_END_DATE -> R.string.compare_field_end_date
        "parties" -> R.string.compare_field_parties
        "jobTitle" -> R.string.compare_field_job_title
        "workLocation" -> R.string.compare_field_work_location
        "basicSalary" -> R.string.compare_field_basic_salary
        "salaryCurrency" -> R.string.compare_field_salary_currency
        "allowances" -> R.string.compare_field_allowances
        "workingHoursPerWeek" -> R.string.compare_field_working_hours_per_week
        "probationPeriodDays" -> R.string.compare_field_probation_period_days
        "probationSalary" -> R.string.compare_field_probation_salary
        "socialInsurance" -> R.string.compare_field_social_insurance
        "annualLeaveDays" -> R.string.compare_field_annual_leave_days
        "terminationNoticeDays" -> R.string.compare_field_termination_notice_days
        else -> null
    }

    private companion object {
        const val FIELD_CONTRACT_TYPE = "contractType"
        const val FIELD_START_DATE = "startDate"
        const val FIELD_END_DATE = "endDate"
    }
}
