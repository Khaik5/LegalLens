package com.example.lagallens.core.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextSearchTest {
    @Test fun aQueryWithoutDiacriticsFindsVietnameseText() {
        assertTrue("Theo quy định, mức lương thử việc".matchesSearch("luong thu viec"))
        assertTrue("Hợp đồng ký tại Đà Nẵng".matchesSearch("da nang"))
        assertTrue("Hợp đồng ký tại Đà Nẵng".matchesSearch("ĐÀ NẴNG"))
    }

    @Test fun aQueryWithDiacriticsStillMatchesAndCaseIsIgnored() {
        assertTrue("HOP_DONG_01".matchesSearch("hợp"))
        assertTrue("Tiền LƯƠNG".matchesSearch("lương"))
    }

    @Test fun aBlankQueryMatchesAndAnUnrelatedOneDoesNot() {
        assertTrue("Bất kỳ".matchesSearch("   "))
        assertFalse("Hợp đồng lao động".matchesSearch("zzzz"))
    }

    @Test fun foldingRemovesMarksAndMapsDToD() {
        assertEquals("duong luong ngay", "Đường lương ngày".foldForSearch())
    }
}
