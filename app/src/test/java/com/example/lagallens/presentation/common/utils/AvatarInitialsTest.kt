package com.example.lagallens.presentation.common.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class AvatarInitialsTest {
    @Test fun `uses the first and the last word`() = assertEquals("EU", "E2E Renamed User".avatarInitials())
    @Test fun `a Vietnamese three word name uses family and given name`() = assertEquals("NA", "Nguyễn Văn An".avatarInitials())
    @Test fun `accents and Đ are removed`() = assertEquals("DA", "Đặng Ánh".avatarInitials())
    @Test fun `a single word gives one letter`() = assertEquals("L", "linh".avatarInitials())
    @Test fun `blank gives nothing`() = assertEquals("", "   ".avatarInitials())
}
