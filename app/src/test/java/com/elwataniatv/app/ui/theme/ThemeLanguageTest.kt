package com.elwataniatv.app.ui.theme

import androidx.compose.ui.unit.LayoutDirection
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeLanguageTest {
    @Test
    fun arabicLanguageUsesRtl() {
        assertEquals(LayoutDirection.Rtl, layoutDirectionForLanguage("ar"))
        assertEquals(LayoutDirection.Rtl, layoutDirectionForLanguage("AR"))
    }

    @Test
    fun arabicRegionalLanguageTagsUseRtl() {
        assertEquals(LayoutDirection.Rtl, layoutDirectionForLanguage("ar-SA"))
        assertEquals(LayoutDirection.Rtl, layoutDirectionForLanguage(" ar_DZ "))
    }

    @Test
    fun englishAndUnknownLanguagesUseLtr() {
        assertEquals(LayoutDirection.Ltr, layoutDirectionForLanguage("en"))
        assertEquals(LayoutDirection.Ltr, layoutDirectionForLanguage("fr"))
        assertEquals(LayoutDirection.Ltr, layoutDirectionForLanguage(""))
    }
}
