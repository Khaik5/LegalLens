package com.example.lagallens.data.datasource.local.datastore

import com.example.lagallens.domain.model.ProfileLanguage
import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileSettingsLanguageTest {
    @Test
    fun legacyUnsupportedLanguageFallsBackToVietnamese() {
        assertEquals(ProfileLanguage.VIETNAMESE, profileLanguageOrDefault("JAPANESE"))
    }

    @Test
    fun englishRemainsSupported() {
        assertEquals(ProfileLanguage.ENGLISH, profileLanguageOrDefault("ENGLISH"))
    }
}
