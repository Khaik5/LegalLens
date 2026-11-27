package com.example.lagallens.presentation.feature.compare.result.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClauseHeadingTest {
    @Test fun keepsOnlyTheArticleNumberAndName() {
        assertEquals("Điều 3. Thời giờ làm việc", "Điều 3. Thời giờ làm việc: 48 giờ/tuần, từ thứ Hai đến thứ Bảy. Bên B làm thêm giờ khi".toClauseHeading())
        assertEquals("Điều 7. Bảo mật và không cạnh tranh", "Điều 7. Bảo mật và không cạnh tranh: Bên B không được làm việc cho doanh nghiệp cùng".toClauseHeading())
    }

    @Test fun usesOnlyTheFirstLine() {
        assertEquals("Điều 1. Loại hợp đồng", "Điều 1. Loại hợp đồng\nHợp đồng xác định thời hạn 12 tháng".toClauseHeading())
    }

    @Test fun shortensATitleWithoutArticleHeading() {
        val text = "Hai bên thỏa thuận ký kết hợp đồng lao động với các điều khoản sau đây và cam kết thực hiện đúng"
        val heading = text.toClauseHeading()
        assertTrue(heading.length <= 61)
        assertTrue(heading.endsWith("…"))
        assertTrue(text.startsWith(heading.dropLast(1)))
    }
}
