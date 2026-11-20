package com.example.lagallens.presentation.feature.compare.result.utils

import com.example.lagallens.domain.model.ContractKeyFact
import org.junit.Assert.assertEquals
import org.junit.Test

class CompareKeyFactPairingTest {
    private fun fact(label: String, value: String) = ContractKeyFact(label, value, null)

    @Test fun factsNamedDifferentlyArePairedByTopic() {
        val rows = pairKeyFacts(
            listOf(
                fact("Loại hợp đồng và thời hạn", "12 tháng"),
                fact("Lương cơ bản", "9,5 triệu"),
                fact("Phụ cấp ăn trưa", "730 nghìn"),
                fact("Nghỉ phép hằng năm", "12 ngày")
            ),
            listOf(
                fact("Thời hạn hợp đồng", "12 tháng"),
                fact("Mức lương chính", "9 triệu"),
                fact("Địa điểm làm việc", "Đà Nẵng")
            )
        )

        assertEquals(
            listOf(
                KeyFactRow("Loại hợp đồng và thời hạn", "12 tháng", "12 tháng"),
                KeyFactRow("Lương cơ bản", "9,5 triệu", "9 triệu"),
                KeyFactRow("Phụ cấp ăn trưa", "730 nghìn", null),
                KeyFactRow("Nghỉ phép hằng năm", "12 ngày", null),
                KeyFactRow("Địa điểm làm việc", null, "Đà Nẵng")
            ),
            rows
        )
    }

    @Test fun unknownTopicsPairOnlyByTheSameLabel() {
        val rows = pairKeyFacts(listOf(fact("Điều khoản bảo mật", "Có")), listOf(fact("điều khoản bảo mật", "Không"), fact("Khác", "x")))

        assertEquals(listOf(KeyFactRow("Điều khoản bảo mật", "Có", "Không"), KeyFactRow("Khác", null, "x")), rows)
    }
}
