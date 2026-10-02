package com.example.lagallens.presentation.feature.analysis.parties.contract

import com.example.lagallens.R

data class AnalysisPartiesUiState(
    val documentName: String = "",
    val documentMetadata: String = "",
    val employer: AnalysisParty = AnalysisParty(
        titleRes = R.string.analysis_parties_employer_title,
        roleRes = R.string.analysis_parties_employer_role,
        firstLabelRes = R.string.analysis_parties_organization_label,
        firstValueRes = R.string.analysis_parties_organization_value,
        secondLabelRes = R.string.analysis_parties_business_id_label,
        secondValueRes = R.string.analysis_parties_business_id_value,
        thirdLabelRes = R.string.analysis_parties_headquarters_label,
        thirdValueRes = R.string.analysis_parties_headquarters_value,
        fourthLabelRes = R.string.analysis_parties_representative_label,
        fourthValueRes = R.string.analysis_parties_representative_value
    ),
    val employee: AnalysisParty = AnalysisParty(
        titleRes = R.string.analysis_parties_employee_title,
        roleRes = R.string.analysis_parties_employee_role,
        firstLabelRes = R.string.analysis_parties_full_name_label,
        firstValueRes = R.string.analysis_parties_full_name_value,
        secondLabelRes = R.string.analysis_parties_identity_label,
        secondValueRes = R.string.analysis_parties_identity_value,
        thirdLabelRes = R.string.analysis_parties_address_label,
        thirdValueRes = R.string.analysis_parties_address_value,
        fourthLabelRes = R.string.analysis_parties_contact_label,
        fourthValueRes = R.string.analysis_parties_contact_value
    )
)
