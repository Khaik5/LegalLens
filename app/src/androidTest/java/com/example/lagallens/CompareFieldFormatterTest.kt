package com.example.lagallens

import android.content.Context
import android.content.res.Configuration
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.lagallens.domain.model.ContractFieldValue
import com.example.lagallens.domain.model.ContractPartyValue
import com.example.lagallens.presentation.feature.compare.result.utils.CompareFieldFormatter
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CompareFieldFormatterTest {
    private fun formatter(language: String): CompareFieldFormatter {
        val target: Context = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(target.resources.configuration).apply { setLocale(Locale(language)) }
        return CompareFieldFormatter(target.createConfigurationContext(configuration))
    }

    @Test fun labelsFollowTheAppLanguageAndAnUnknownFieldKeepsTheBackendLabel() {
        assertEquals("Loại hợp đồng", formatter("vi").label("contractType", "Loai"))
        assertEquals("Contract type", formatter("en").label("contractType", "Loại hợp đồng"))
        assertEquals("Nhãn của backend", formatter("en").label("someNewField", "Nhãn của backend"))
    }

    @Test fun contractTypeCodesBecomeNamesAndAnUnknownCodeStaysAsSent() {
        assertEquals("Hợp đồng xác định thời hạn", formatter("vi").value("contractType", ContractFieldValue.Text("XAC_DINH_THOI_HAN")))
        assertEquals("Fixed-term contract", formatter("en").value("contractType", ContractFieldValue.Text("XAC_DINH_THOI_HAN")))
        assertEquals("Other", formatter("en").value("contractType", ContractFieldValue.Text("KHAC")))
        assertEquals("MOI_LA", formatter("en").value("contractType", ContractFieldValue.Text("MOI_LA")))
    }

    @Test fun datesAreWrittenInTheAppLanguageAndAnInvalidDateStaysAsSent() {
        assertTrue(formatter("en").value("startDate", ContractFieldValue.Text("2026-11-18")).contains("Nov"))
        val vietnamese = formatter("vi").value("endDate", ContractFieldValue.Text("2026-11-18"))
        assertNotEquals("2026-11-18", vietnamese)
        assertTrue(vietnamese.contains("2026"))
        assertEquals("not-a-date", formatter("en").value("startDate", ContractFieldValue.Text("not-a-date")))
    }

    @Test fun partiesListEachNameWithItsRoleAndAnUnknownRoleStaysAsSent() {
        val parties = ContractFieldValue.Parties(listOf(
            ContractPartyValue("Công ty A", "EMPLOYER"),
            ContractPartyValue("Nguyễn B", "EMPLOYEE"),
            ContractPartyValue("Người C", "GUARANTOR"),
            ContractPartyValue("Người D", null)
        ))

        assertEquals(
            "Công ty A (Bên sử dụng lao động)\nNguyễn B (Người lao động)\nNgười C (GUARANTOR)\nNgười D",
            formatter("vi").value("parties", parties)
        )
        assertEquals("Công ty A (Employer)", formatter("en").value("parties", parties).lines().first())
    }

    @Test fun aRoleAlreadyWrittenInThePartyNameIsNotRepeated() {
        val parties = ContractFieldValue.Parties(listOf(
            ContractPartyValue("Bên A (Người sử dụng lao động)", "EMPLOYER"),
            ContractPartyValue("Người lao động (Bên B) - Nguyễn Văn An", "EMPLOYEE")
        ))

        assertEquals(
            "Bên A (Người sử dụng lao động)\nNgười lao động (Bên B) - Nguyễn Văn An",
            formatter("vi").value("parties", parties)
        )
    }

    @Test fun flagsAndOtherTextAreWordedForTheLanguage() {
        assertEquals("Có", formatter("vi").value("socialInsurance", ContractFieldValue.Flag(true)))
        assertEquals("No", formatter("en").value("socialInsurance", ContractFieldValue.Flag(false)))
        assertEquals("15000000", formatter("en").value("basicSalary", ContractFieldValue.Text("15000000")))
    }
}
